package com.example.quotepicker.util

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.io.File
import java.io.InputStream
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.CipherInputStream
import javax.crypto.CipherOutputStream
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import kotlin.random.Random

class EncryptedFileManager(private val context: Context) {
    private val keyAlias = "resource_aes_key"

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        val existing = keyStore.getKey(keyAlias, null) as? SecretKey
        if (existing != null) return existing

        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        val spec = KeyGenParameterSpec.Builder(
            keyAlias,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setKeySize(256)
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .build()
        keyGenerator.init(spec)
        return keyGenerator.generateKey()
    }

    fun encryptedDir(): File {
        val dir = File(context.filesDir, "encrypted_resources")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun encryptToFile(input: InputStream, targetName: String): File {
        val key = getOrCreateKey()
        val iv = Random.nextBytes(12)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply {
            init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(128, iv))
        }
        val outFile = File(encryptedDir(), targetName)
        outFile.outputStream().use { fileOut ->
            fileOut.write(iv)
            CipherOutputStream(fileOut, cipher).use { cipherOut ->
                input.copyTo(cipherOut)
            }
        }
        return outFile
    }

    fun openDecryptedStream(path: String): InputStream {
        val key = getOrCreateKey()
        val file = File(path)
        val input = file.inputStream()
        val iv = ByteArray(12)
        val read = input.read(iv)
        require(read == iv.size) { "Invalid encrypted file header" }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply {
            init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, iv))
        }
        return CipherInputStream(input, cipher)
    }

    fun deleteEncryptedFile(path: String?) {
        if (path.isNullOrBlank()) return
        runCatching { File(path).delete() }
    }
}
