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
        if (text.isBlank() || profile.modelUri.isBlank() || profile.configUri.isBlank()) return false
        val output = withContext(Dispatchers.IO) {
            val modelFile = copyUriToCache(profile.modelUri, "model_${profile.id}.onnx") ?: return@withContext null
            val configFile = copyUriToCache(profile.configUri, "config_${profile.id}.json") ?: return@withContext null
            val outFile = File(context.cacheDir, "piper_${System.currentTimeMillis()}.wav")
            val lengthScale = (1f / speechRate.coerceIn(0.6f, 1.8f)).toString()
            val process = ProcessBuilder(
                "piper",
                "--model", modelFile.absolutePath,
                "--config", configFile.absolutePath,
                "--output_file", outFile.absolutePath,
                "--length_scale", lengthScale
            ).redirectErrorStream(true).start()
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
            context.contentResolver.openInputStream(uri)?.use { input ->
                out.outputStream().use { output -> input.copyTo(output) }
            } ?: return null
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
