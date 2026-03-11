package com.example.quotepicker.util

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.util.Log
import com.k2fsa.sherpa.onnx.GeneratedAudio
import com.k2fsa.sherpa.onnx.OfflineTts
import com.k2fsa.sherpa.onnx.OfflineTtsConfig
import com.k2fsa.sherpa.onnx.OfflineTtsModelConfig
import com.k2fsa.sherpa.onnx.OfflineTtsVitsModelConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.math.abs
import kotlin.math.roundToInt

private const val TTS_TAG = "PiperSpeech"

class PiperSpeechEngine(private val context: Context) {
    companion object {
        private const val BASE_DIR = "tts/vits-zh-hf-fanchen-C"
        private const val MODEL_FILE_NAME = "vits-zh-hf-fanchen-C.onnx"
        private const val LEXICON_FILE_NAME = "lexicon.txt"
        private const val TOKENS_FILE_NAME = "tokens.txt"
        private const val PHONE_FST_FILE_NAME = "phone.fst"
        private const val NUMBER_FST_FILE_NAME = "number.fst"
        private const val RULE_FAR_FILE_NAME = "rule.far"
        private const val DICT_DIR_NAME = "dict"

        private const val MAX_GAIN = 8.0f
        private const val TARGET_PEAK = 0.75f
    }

    private val mutex = Mutex()

    @Volatile
    private var tts: OfflineTts? = null

    @Volatile
    private var audioTrack: AudioTrack? = null

    @Volatile
    private var preparedModelDir: File? = null

    suspend fun speak(text: String, profile: VoiceProfile, speechRate: Float): Boolean {
        if (text.isBlank()) return false

        return withContext(Dispatchers.Default) {
            runCatching {
                ensureInit()

                val engine = tts ?: error("TTS engine not initialized")
                val sid = normalizeSpeakerId(engine, profile.speakerId)
                val speed = speechRate.coerceIn(0.5f, 2.0f)

                Log.d(
                    TTS_TAG,
                    "input text=[$text], length=${text.length}, hash=${text.hashCode()}, sid=$sid, speed=$speed"
                )
                Log.d(
                    TTS_TAG,
                    "input codePoints=" + text.map { "U+%04X".format(it.code) }.joinToString(" ")
                )

                val audio = engine.generate(
                    text = text,
                    sid = sid,
                    speed = speed
                )

                val samples = audio.samples
                val sampleRate = audio.sampleRate

                if (samples.isEmpty()) {
                    error("Generated audio is empty")
                }

                val peak = samples.maxOfOrNull { abs(it) } ?: 0f
                Log.d(
                    TTS_TAG,
                    "generate ok. sid=$sid speed=$speed sampleRate=$sampleRate size=${samples.size} peak=$peak"
                )

                stopInternal()

                val pcm16 = floatToPcm16WithGain(samples)
                playPcm16(pcm16, sampleRate)

                true
            }.onFailure { e ->
                Log.e(TTS_TAG, "speak failed", e)
            }.getOrDefault(false)
        }
    }

    suspend fun stop() = withContext(Dispatchers.IO) {
        stopInternal()
    }

    suspend fun release() = withContext(Dispatchers.IO) {
        stopInternal()
        mutex.withLock {
            tts?.release()
            tts = null
            preparedModelDir = null
        }
    }

    private suspend fun ensureInit() = withContext(Dispatchers.IO) {
        mutex.withLock {
            if (tts != null) return@withLock

            val modelDir = prepareModelDir()
            preparedModelDir = modelDir

            val modelPath = File(modelDir, MODEL_FILE_NAME).absolutePath
            val lexiconPath = File(modelDir, LEXICON_FILE_NAME).absolutePath
            val tokensPath = File(modelDir, TOKENS_FILE_NAME).absolutePath
            val phoneFstPath = File(modelDir, PHONE_FST_FILE_NAME).absolutePath
            val numberFstPath = File(modelDir, NUMBER_FST_FILE_NAME).absolutePath
            val ruleFarPath = File(modelDir, RULE_FAR_FILE_NAME).absolutePath
            val dictDirPath = File(modelDir, DICT_DIR_NAME).absolutePath

            val ruleFsts = buildRuleFsts(phoneFstPath, numberFstPath)
            val ruleFars = if (File(ruleFarPath).exists()) ruleFarPath else ""

            Log.d(TTS_TAG, "init config (no dataDir):")
            Log.d(TTS_TAG, "modelDir=${modelDir.absolutePath}")
            Log.d(TTS_TAG, "model=$modelPath exists=${File(modelPath).exists()}")
            Log.d(TTS_TAG, "lexicon=$lexiconPath exists=${File(lexiconPath).exists()}")
            Log.d(TTS_TAG, "tokens=$tokensPath exists=${File(tokensPath).exists()}")
            Log.d(TTS_TAG, "ruleFsts=$ruleFsts")
            Log.d(
                TTS_TAG,
                "ruleFars=$ruleFars exists=${if (ruleFars.isNotEmpty()) File(ruleFars).exists() else false}"
            )
            Log.d(TTS_TAG, "dictExists=${File(dictDirPath).exists()}")

            val vits = OfflineTtsVitsModelConfig(
                model = modelPath,
                lexicon = lexiconPath,
                tokens = tokensPath,
                dictDir = dictDirPath,
                noiseScale = 0.667f,
                noiseScaleW = 0.8f,
                lengthScale = 1.0f
            )

            val config = OfflineTtsConfig(
                model = OfflineTtsModelConfig(
                    vits = vits,
                    numThreads = 2,
                    debug = true,
                    provider = "cpu"
                ),
                ruleFsts = ruleFsts,
                ruleFars = ruleFars,
                maxNumSentences = 1,
                silenceScale = 0.2f
            )

            tts = OfflineTts(config = config)

            Log.d(
                TTS_TAG,
                "TTS initialized. speakers=${tts?.numSpeakers()}, sampleRate=${tts?.sampleRate()}"
            )
        }
    }

    private fun prepareModelDir(): File {
        val outDir = File(context.filesDir, BASE_DIR)
        if (isModelPrepared(outDir)) {
            Log.d(TTS_TAG, "model dir already prepared: ${outDir.absolutePath}")
            return outDir
        }

        Log.d(TTS_TAG, "preparing model dir: ${outDir.absolutePath}")
        copyAssetDirRecursively(BASE_DIR, outDir)

        Log.d(
            TTS_TAG,
            "prepared files: ${outDir.walkTopDown().joinToString { it.absolutePath }}"
        )

        check(isModelPrepared(outDir)) {
            "Model files are not prepared correctly under ${outDir.absolutePath}"
        }

        return outDir
    }

    private fun isModelPrepared(dir: File): Boolean {
        return File(dir, MODEL_FILE_NAME).exists() &&
                File(dir, LEXICON_FILE_NAME).exists() &&
                File(dir, TOKENS_FILE_NAME).exists()
    }

    private fun copyAssetDirRecursively(assetPath: String, outDir: File) {
        if (!outDir.exists()) {
            outDir.mkdirs()
        }

        val children = context.assets.list(assetPath).orEmpty()

        if (children.isEmpty()) {
            copySingleAsset(assetPath, outDir)
            return
        }

        for (name in children) {
            val childAssetPath = "$assetPath/$name"
            val nestedChildren = context.assets.list(childAssetPath).orEmpty()
            if (nestedChildren.isEmpty()) {
                copySingleAsset(childAssetPath, outDir)
            } else {
                val childDir = File(outDir, name)
                copyAssetDirRecursively(childAssetPath, childDir)
            }
        }
    }

    private fun copySingleAsset(assetPath: String, outDir: File) {
        if (!outDir.exists()) {
            outDir.mkdirs()
        }

        val fileName = assetPath.substringAfterLast('/')
        val outFile = File(outDir, fileName)

        if (outFile.exists() && outFile.length() > 0L) {
            return
        }

        context.assets.open(assetPath).use { input ->
            FileOutputStream(outFile).use { output ->
                input.copyTo(output)
                output.flush()
            }
        }

        Log.d(
            TTS_TAG,
            "copied asset: $assetPath -> ${outFile.absolutePath} (${outFile.length()} bytes)"
        )
    }

    private fun buildRuleFsts(phoneFstPath: String, numberFstPath: String): String {
        val list = mutableListOf<String>()
        if (File(phoneFstPath).exists()) list += phoneFstPath
        if (File(numberFstPath).exists()) list += numberFstPath
        return list.joinToString(",")
    }

    private fun normalizeSpeakerId(engine: OfflineTts, speakerId: Int?): Int {
        val count = engine.numSpeakers()
        if (count <= 0) return 0
        return (speakerId ?: 0).coerceIn(0, count - 1)
    }

    private fun floatToPcm16WithGain(samples: FloatArray): ShortArray {
        val peak = samples.maxOfOrNull { abs(it) } ?: 0f

        val gain = when {
            peak <= 0f -> 1f
            else -> (TARGET_PEAK / peak).coerceIn(1f, MAX_GAIN)
        }

        Log.d(TTS_TAG, "pcm16 convert. inputPeak=$peak gain=$gain")

        return ShortArray(samples.size) { i ->
            val normalized = (samples[i] * gain).coerceIn(-1f, 1f)
            (normalized * Short.MAX_VALUE).roundToInt().toShort()
        }
    }

    private fun playPcm16(samples: ShortArray, sampleRate: Int) {
        val minBufferSize = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )

        if (minBufferSize <= 0) {
            error("Invalid minBufferSize for PCM_16BIT: $minBufferSize")
        }

        val bufferSize = maxOf(minBufferSize, samples.size * 2)

        val track = AudioTrack(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build(),
            AudioFormat.Builder()
                .setSampleRate(sampleRate)
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                .build(),
            bufferSize,
            AudioTrack.MODE_STATIC,
            AudioManager.AUDIO_SESSION_ID_GENERATE
        )

        audioTrack = track

        val written = track.write(samples, 0, samples.size, AudioTrack.WRITE_BLOCKING)
        if (written <= 0) {
            track.release()
            audioTrack = null
            error("AudioTrack short write failed: $written")
        }

        track.play()

        Log.d(
            TTS_TAG,
            "PCM_16BIT play ok. sampleRate=$sampleRate, samples=${samples.size}, written=$written"
        )
    }

    private fun stopInternal() {
        try {
            audioTrack?.let { track ->
                try {
                    if (track.state == AudioTrack.STATE_INITIALIZED) {
                        try {
                            track.pause()
                        } catch (_: Throwable) {
                        }
                        try {
                            track.flush()
                        } catch (_: Throwable) {
                        }
                        try {
                            track.stop()
                        } catch (_: Throwable) {
                        }
                    }
                } finally {
                    track.release()
                }
            }
        } finally {
            audioTrack = null
        }
    }
}