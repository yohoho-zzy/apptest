package com.example.quotepicker.util

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.coroutines.resume

private const val PIPER_TAG = "PiperSpeech"

class PiperSpeechEngine(private val context: Context) {
    suspend fun speak(text: String, profile: VoiceProfile, speechRate: Float): Boolean {
        if (text.isBlank() || profile.modelUri.isBlank()) return false
        val output = withContext(Dispatchers.IO) {
            val modelFile = copyUriToCache(profile.modelUri, "model_${profile.id}.onnx") ?: return@withContext null
            val configFile = profile.configUri.takeIf { it.isNotBlank() }?.let {
                copyUriToCache(it, "config_${profile.id}.json")
            }
            val outFile = File(context.cacheDir, "piper_${System.currentTimeMillis()}.wav")
            val lengthScale = (1f / speechRate.coerceIn(0.6f, 1.8f)).toString()
            val command = mutableListOf(
                "piper",
                "--model", modelFile.absolutePath,
                "--output_file", outFile.absolutePath,
                "--length_scale", lengthScale
            )
            configFile?.let {
                command += listOf("--config", it.absolutePath)
            }
            profile.speakerId?.let {
                if (it >= 0) command += listOf("--speaker", it.toString())
            }
            profile.noiseScale?.let {
                command += listOf("--noise_scale", it.toString())
            }
            profile.noiseW?.let {
                command += listOf("--noise_w", it.toString())
            }
            profile.sentenceSilence?.let {
                command += listOf("--sentence_silence", it.toString())
            }
            val process = ProcessBuilder(command).redirectErrorStream(true).start()
            process.outputStream.bufferedWriter().use {
                it.write(text)
                it.newLine()
            }
            val exit = process.waitFor()
            if (exit != 0 || !outFile.exists()) {
                Log.e(PIPER_TAG, "piper execute failed exit=$exit")
                null
            } else {
                outFile
            }
        } ?: return false

        return playFile(output)
    }

    private fun copyUriToCache(rawUri: String, fileName: String): File? {
        val uri = Uri.parse(rawUri)
        return runCatching {
            val out = File(context.cacheDir, fileName)
            val inputStream = when (uri.scheme) {
                "asset" -> context.assets.open(uri.schemeSpecificPart.removePrefix("/"))
                else -> context.contentResolver.openInputStream(uri)
            } ?: return null
            inputStream.use { input ->
                out.outputStream().use { output -> input.copyTo(output) }
            }
            out
        }.getOrNull()
    }

    private suspend fun playFile(file: File): Boolean = suspendCancellableCoroutine { cont ->
        val player = MediaPlayer()
        runCatching {
            player.setDataSource(file.absolutePath)
            player.setOnCompletionListener {
                it.release()
                if (cont.isActive) cont.resume(true)
            }
            player.setOnErrorListener { mp, _, _ ->
                mp.release()
                if (cont.isActive) cont.resume(false)
                true
            }
            player.prepare()
            player.start()
            cont.invokeOnCancellation { player.release() }
        }.onFailure {
            player.release()
            if (cont.isActive) cont.resume(false)
        }
    }
}
