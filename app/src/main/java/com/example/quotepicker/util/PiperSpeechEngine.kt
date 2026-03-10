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
import kotlin.math.abs
import kotlin.math.roundToInt

private const val TTS_TAG = "PiperSpeech"

class PiperSpeechEngine(private val context: Context) {
    companion object {
        private const val BASE_DIR = "tts/vits-zh-hf-fanchen-C"
        private const val MODEL_PATH = "$BASE_DIR/vits-zh-hf-fanchen-C.onnx"
        private const val LEXICON_PATH = "$BASE_DIR/lexicon.txt"
        private const val TOKENS_PATH = "$BASE_DIR/tokens.txt"
        private const val RULE_FSTS = "$BASE_DIR/phone.fst,$BASE_DIR/number.fst"
        private const val RULE_FARS = "$BASE_DIR/rule.far"
        private const val DICT_DIR = "$BASE_DIR/dict"
    }

    private val mutex = Mutex()

    @Volatile
    private var tts: OfflineTts? = null

    @Volatile
    private var audioTrack: AudioTrack? = null

    suspend fun speak(text: String, profile: VoiceProfile, speechRate: Float): Boolean {
        if (text.isBlank()) return false
        return withContext(Dispatchers.Default) {
            runCatching {
                ensureInit()
                val engine = tts ?: error("TTS engine not initialized")
                val sid = normalizeSpeakerId(engine, profile.speakerId)
                val speed = speechRate.coerceIn(0.5f, 2.0f)
                val audio = engine.generate(text = text, sid = sid, speed = speed)

                stopInternal()

                val pcm16 = floatToPcm16(audio.samples)
                val sampleRate = engine.sampleRate()
                val minBufferSize = AudioTrack.getMinBufferSize(
                    sampleRate,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT
                )
                val pcmBytes = shortArrayToBytes(pcm16)
                val bufferSize = maxOf(minBufferSize, pcmBytes.size)

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
                    AudioTrack.MODE_STREAM,
                    AudioManager.AUDIO_SESSION_ID_GENERATE
                )

                audioTrack = track
                track.play()
                val written = track.write(pcmBytes, 0, pcmBytes.size, AudioTrack.WRITE_BLOCKING)
                if (written <= 0) {
                    track.release()
                    audioTrack = null
                    error("AudioTrack write failed: $written")
                }
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
        }
    }

    private suspend fun ensureInit() = withContext(Dispatchers.IO) {
        mutex.withLock {
            if (tts != null) return@withLock

            val vits = OfflineTtsVitsModelConfig(
                model = MODEL_PATH,
                lexicon = LEXICON_PATH,
                tokens = TOKENS_PATH,
                dataDir = BASE_DIR,
                dictDir = DICT_DIR,
                noiseScale = 0.667f,
                noiseScaleW = 0.8f,
                lengthScale = 1.0f
            )


            val ruleFars = if (assetExists(RULE_FARS)) RULE_FARS else ""

            val config = OfflineTtsConfig(
                model = OfflineTtsModelConfig(
                    vits = vits,
                    numThreads = 2,
                    debug = true,
                    provider = "cpu"
                ),
                ruleFsts = RULE_FSTS,
                ruleFars = ruleFars,
                maxNumSentences = 1,
                silenceScale = 0.2f
            )

            tts = OfflineTts(
                assetManager = context.assets,
                config = config
            )

            Log.d(TTS_TAG, "TTS initialized. speakers=${tts?.numSpeakers()}, sampleRate=${tts?.sampleRate()}")
        }
    }

    private fun assetExists(path: String): Boolean {
        return runCatching {
            context.assets.open(path).use { }
            true
        }.getOrDefault(false)
    }

    private fun normalizeSpeakerId(engine: OfflineTts, speakerId: Int?): Int {
        val count = engine.numSpeakers()
        if (count <= 0) return 0
        return (speakerId ?: 0).coerceIn(0, count - 1)
    }

    private fun floatToPcm16(samples: FloatArray): ShortArray {
        // sherpa-onnx models may output normalized samples [-1, 1], but some builds return
        // float values in the int16 range. Auto-detect to avoid heavy distortion.
        val peak = samples.maxOfOrNull { abs(it) } ?: 1f
        val scale = if (peak > 1.5f) 1f else Short.MAX_VALUE.toFloat()

        return ShortArray(samples.size) { i ->
            val normalized = (samples[i] / scale).coerceIn(-1f, 1f)
            (normalized * Short.MAX_VALUE).roundToInt().toShort()
        }
    }

    private fun shortArrayToBytes(samples: ShortArray): ByteArray {
        val out = ByteArray(samples.size * 2)
        var j = 0
        for (sample in samples) {
            out[j++] = (sample.toInt() and 0xFF).toByte()
            out[j++] = ((sample.toInt() shr 8) and 0xFF).toByte()
        }
        return out
    }

    private fun stopInternal() {
        try {
            audioTrack?.let { track ->
                try {
                    track.pause()
                    track.flush()
                    track.stop()
                } catch (_: Throwable) {
                }
                track.release()
            }
        } finally {
            audioTrack = null
        }
    }
}
