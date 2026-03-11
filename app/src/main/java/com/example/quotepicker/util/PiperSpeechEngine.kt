package com.example.quotepicker.util

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.util.Log
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
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.TimeUnit
import java.util.UUID
import kotlin.math.abs
import kotlin.math.roundToInt

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
        private const val PREVIEW_TEMP_DIR = "voice-preview-audio"
        private val PREVIEW_TEMP_MAX_AGE_MS = TimeUnit.HOURS.toMillis(12)
        private const val PREVIEW_TEMP_MAX_FILE_COUNT = 5
    }

    private val mutex = Mutex()

    @Volatile
    private var tts: OfflineTts? = null

    @Volatile
    private var audioTrack: AudioTrack? = null

    @Volatile
    private var preparedModelDir: File? = null

    @Volatile
    private var configSignature: String? = null

    suspend fun speak(text: String, profile: VoiceProfile, speechRate: Float): Boolean {
        if (text.isBlank()) return false

        return withContext(Dispatchers.Default) {
            runCatching {
                ensureInit(profile)

                val engine = tts ?: error("TTS engine not initialized")
                val sid = normalizeSpeakerId(engine, profile.speakerId)
                val speed = speechRate.coerceIn(0.5f, 2.0f)

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

                stopInternal()

                val pcm16 = floatToPcm16WithGain(samples)
                playPcm16(pcm16, sampleRate)

                true
            }.onFailure { e ->
                Log.e("TTS_TAG", "speak failed", e)
            }.getOrDefault(false)
        }
    }

    suspend fun stop() = withContext(Dispatchers.IO) {
        stopInternal()
    }

    suspend fun synthesizeToTempWav(text: String, profile: VoiceProfile, speechRate: Float): File? {
        if (text.isBlank()) return null
        return withContext(Dispatchers.IO) {
            runCatching {
                ensureInit(profile)
                cleanupPreviewTempFiles()
                val engine = tts ?: error("TTS engine not initialized")
                val sid = normalizeSpeakerId(engine, profile.speakerId)
                val speed = speechRate.coerceIn(0.5f, 2.0f)
                val audio = engine.generate(text = text, sid = sid, speed = speed)
                val pcm = floatToPcm16WithGain(audio.samples)
                val tempDir = File(context.cacheDir, PREVIEW_TEMP_DIR).apply { mkdirs() }
                val out = File(tempDir, "preview_${System.currentTimeMillis()}_${UUID.randomUUID()}.wav")
                writePcmWav(out, pcm, audio.sampleRate)
                out
            }.getOrNull()
        }
    }

    suspend fun cleanupPreviewTempFiles() = withContext(Dispatchers.IO) {
        val tempDir = File(context.cacheDir, PREVIEW_TEMP_DIR)
        if (!tempDir.exists()) return@withContext
        val now = System.currentTimeMillis()
        val files = tempDir.listFiles()
            ?.filter { it.isFile }
            ?.sortedByDescending { it.lastModified() }
            .orEmpty()

        files.forEachIndexed { index, file ->
            val isExpired = now - file.lastModified() > PREVIEW_TEMP_MAX_AGE_MS
            val exceedsCountLimit = index >= PREVIEW_TEMP_MAX_FILE_COUNT
            if (isExpired || exceedsCountLimit) {
                runCatching { file.delete() }
            }
        }
    }

    suspend fun release() = withContext(Dispatchers.IO) {
        stopInternal()
        mutex.withLock {
            tts?.release()
            tts = null
            preparedModelDir = null
            configSignature = null
        }
    }

    private suspend fun ensureInit(profile: VoiceProfile) = withContext(Dispatchers.IO) {
        val effectiveNoiseScale = (profile.noiseScale ?: 0.2f).coerceIn(0.1f, 2.0f)
        val effectiveNoiseScaleW = (profile.noiseScaleW ?: 0.2f).coerceIn(0.1f, 2.0f)
        val effectiveLengthScale = (profile.lengthScale ?: 1.0f).coerceIn(0.5f, 2.0f)
        val effectiveMaxNumSentences = (profile.maxNumSentences ?: 1).coerceIn(1, 10)
        val effectiveSilenceScale = (profile.silenceScale ?: 0.2f).coerceIn(0f, 1f)
        val targetSignature = listOf(
            effectiveNoiseScale,
            effectiveNoiseScaleW,
            effectiveLengthScale,
            effectiveMaxNumSentences,
            effectiveSilenceScale
        ).joinToString("|")

        mutex.withLock {
            if (tts != null && configSignature == targetSignature) return@withLock
            tts?.release()
            tts = null

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

            val vits = OfflineTtsVitsModelConfig(
                model = modelPath,
                lexicon = lexiconPath,
                tokens = tokensPath,
                dictDir = dictDirPath,
                noiseScale = effectiveNoiseScale,
                noiseScaleW = effectiveNoiseScaleW,
                lengthScale = effectiveLengthScale
            )

            val config = OfflineTtsConfig(
                model = OfflineTtsModelConfig(
                    vits = vits,
                    numThreads = 2,
                    debug = false,
                    provider = "cpu"
                ),
                ruleFsts = ruleFsts,
                ruleFars = ruleFars,
                maxNumSentences = effectiveMaxNumSentences,
                silenceScale = effectiveSilenceScale
            )

            tts = OfflineTts(config = config)
            configSignature = targetSignature
        }
    }

    private fun prepareModelDir(): File {
        val outDir = File(context.filesDir, BASE_DIR)
        if (isModelPrepared(outDir)) {
            return outDir
        }

        copyAssetDirRecursively(BASE_DIR, outDir)

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

    private fun writePcmWav(target: File, pcm: ShortArray, sampleRate: Int) {
        val dataSize = pcm.size * 2
        val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN).apply {
            put("RIFF".toByteArray())
            putInt(36 + dataSize)
            put("WAVE".toByteArray())
            put("fmt ".toByteArray())
            putInt(16)
            putShort(1)
            putShort(1)
            putInt(sampleRate)
            putInt(sampleRate * 2)
            putShort(2)
            putShort(16)
            put("data".toByteArray())
            putInt(dataSize)
        }.array()
        FileOutputStream(target).use { output ->
            output.write(header)
            val pcmBytes = ByteBuffer.allocate(dataSize).order(ByteOrder.LITTLE_ENDIAN)
            pcm.forEach { pcmBytes.putShort(it) }
            output.write(pcmBytes.array())
            output.flush()
        }
    }
}
