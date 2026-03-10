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
                val bufferSize = maxOf(minBufferSize, pcm16.size * 2)

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
                val written = track.write(pcm16, 0, pcm16.size)
                if (written <= 0) {
                    track.release()
                    audioTrack = null
                    error("AudioTrack write failed: $written")
                }

                track.play()
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
        return ShortArray(samples.size) { i ->
            val v = samples[i].coerceIn(-1f, 1f)
            (v * Short.MAX_VALUE).roundToInt().toShort()
        }
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
