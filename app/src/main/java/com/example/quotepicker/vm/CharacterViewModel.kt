package com.example.quotepicker.vm

import android.app.Application
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.quotepicker.data.BackupSnapshot
import com.example.quotepicker.data.CharacterEntity
import com.example.quotepicker.data.CharacterWithTags
import com.example.quotepicker.data.Repository
import com.example.quotepicker.data.TagCategoryEntity
import com.example.quotepicker.data.TagCategoryType
import com.example.quotepicker.data.TagEntity
import java.io.BufferedInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.io.IOException
import java.io.OutputStream
import java.nio.charset.Charset
import java.security.SecureRandom
import java.security.MessageDigest
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.crypto.Cipher
import javax.crypto.CipherInputStream
import javax.crypto.CipherOutputStream
import javax.crypto.Mac
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.PushbackInputStream

data class CharacterUiState(
    val characters: List<CharacterWithTags> = emptyList(),
    val categories: List<TagCategoryEntity> = emptyList(),
    val tags: List<TagEntity> = emptyList(),
    val executionSettings: com.example.quotepicker.data.ExecutionSettingsEntity = com.example.quotepicker.data.ExecutionSettingsEntity()
)

data class TransferState(
    val inProgress: Boolean = false,
    val mode: TransferMode? = null,
    val progress: Float? = null,
    val processedBytes: Long? = null,
    val totalBytes: Long? = null,
    val outputBytes: Long? = null
)

enum class TransferMode {
    IMPORT,
    EXPORT
}

enum class ExportFormat {
    ENCRYPTED,
    ZIP
}

class CharacterViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = Repository.get(app)
    private val _transferState = MutableStateFlow(TransferState())
    val transferState: StateFlow<TransferState> = _transferState

    init {
        viewModelScope.launch {
            repo.ensureExecutionSettings()
            repo.refreshDailyProbabilities()
        }
    }

    val uiState: StateFlow<CharacterUiState> = combine(
        repo.observeCharactersWithTags(),
        repo.observeCategories(),
        repo.observeAllTags(),
        repo.observeExecutionSettings()
    ) { characters, categories, tags, settings ->
        val roleCategories = categories.filter { it.type == TagCategoryType.CHARACTER }
        val roleCategoryIds = roleCategories.map { it.id }.toSet()
        val roleTags = tags.filter { it.categoryId in roleCategoryIds }
        CharacterUiState(
            characters = characters,
            categories = roleCategories,
            tags = roleTags,
            executionSettings = settings ?: com.example.quotepicker.data.ExecutionSettingsEntity()
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, CharacterUiState())

    fun addCharacter(name: String) = viewModelScope.launch { repo.addCharacter(name) }
    fun updateCharacter(character: CharacterEntity) = viewModelScope.launch { repo.updateCharacter(character) }
    fun deleteCharacter(character: CharacterEntity) = viewModelScope.launch { repo.deleteCharacter(character) }
    fun updateCharacterTags(characterId: Long, tagIds: List<Long>) =
        viewModelScope.launch { repo.updateCharacterTags(characterId, tagIds) }

    fun updateCharacterPoints(characterId: Long, points: Int) =
        viewModelScope.launch { repo.updateCharacterPoints(characterId, points) }

    fun updateCharacterProbability(characterId: Long, probability: Int, date: String) =
        viewModelScope.launch { repo.updateCharacterProbability(characterId, probability, date) }

    fun addResponseRecord(characterId: Long, tagId: Long, count: Int = 1) =
        viewModelScope.launch { repo.addResponseRecord(characterId, tagId, count) }

    fun consumeExecutionRemaining() = viewModelScope.launch {
        val current = uiState.value.executionSettings
        if (current.remainingValue <= 0) return@launch
        repo.updateExecutionSettings(current.copy(remainingValue = current.remainingValue - 1))
    }

    fun exportSnapshot(uri: Uri, format: ExportFormat) = viewModelScope.launch(Dispatchers.IO) {
        _transferState.update {
            it.copy(
                inProgress = true,
                mode = TransferMode.EXPORT,
                progress = 0f,
                processedBytes = 0L,
                totalBytes = 0L,
                outputBytes = 0L
            )
        }
        try {
            val resolver = getApplication<Application>().contentResolver
            val exportPackage = repo.exportSnapshotPackage()
            val snapshotBytes = exportPackage.snapshot.toJsonString().toByteArray(Charset.forName("UTF-8"))
            val totalBytes = exportPackage.mediaSources.sumOf { source ->
                repo.mediaSize(source.originalPath) ?: 0L
            } + snapshotBytes.size.toLong()
            var processedBytes = 0L
            var outputBytes = 0L
            var lastProgressBytes = 0L
            var lastOutputBytes = 0L
            val safeTotalBytes = totalBytes.coerceAtLeast(1L)
            fun updateProgress(force: Boolean = false) {
                val progress = (processedBytes.toFloat() / safeTotalBytes).coerceIn(0f, 1f)
                if (force || processedBytes - lastProgressBytes >= PROGRESS_UPDATE_BYTES || outputBytes - lastOutputBytes >= PROGRESS_UPDATE_BYTES) {
                    lastProgressBytes = processedBytes
                    lastOutputBytes = outputBytes
                    _transferState.update { current ->
                        current.copy(
                            progress = progress,
                            processedBytes = processedBytes,
                            totalBytes = totalBytes,
                            outputBytes = outputBytes
                        )
                    }
                }
            }
            _transferState.update { it.copy(totalBytes = totalBytes) }
            val exportOutput = resolver.openOutputStream(uri)
                ?: throw IOException("Unable to open output stream for export uri: $uri")
            exportOutput.use { output ->
                val countingOutput = CountingOutputStream(output) { bytesWritten ->
                    outputBytes = bytesWritten
                    updateProgress()
                }
                when (format) {
                    ExportFormat.ENCRYPTED -> writeEncryptedZipBackup(
                        output = countingOutput,
                        snapshotBytes = snapshotBytes,
                        mediaSources = exportPackage.mediaSources,
                        onChunkWritten = { written ->
                            processedBytes += written
                            updateProgress()
                        }
                    )
                    ExportFormat.ZIP -> writePlainZipBackup(
                        output = countingOutput,
                        snapshotBytes = snapshotBytes,
                        mediaSources = exportPackage.mediaSources,
                        onChunkWritten = { written ->
                            processedBytes += written
                            updateProgress()
                        }
                    )
                }
            }
            updateProgress(force = true)
        } catch (t: Throwable) {
            if (t is CancellationException) throw t
            t.printStackTrace()
        } finally {
            _transferState.update {
                it.copy(
                    inProgress = false,
                    mode = null,
                    progress = null,
                    processedBytes = null,
                    totalBytes = null,
                    outputBytes = null
                )
            }
        }
    }

    fun importSnapshot(uri: Uri) = viewModelScope.launch(Dispatchers.IO) {
        _transferState.update {
            it.copy(
                inProgress = true,
                mode = TransferMode.IMPORT,
                progress = 0f,
                processedBytes = 0L,
                totalBytes = 0L,
                outputBytes = null
            )
        }
        val tempFiles = mutableListOf<File>()
        try {
            val resolver = getApplication<Application>().contentResolver
            val totalBytes = resolver.openAssetFileDescriptor(uri, "r")?.use { it.length }?.takeIf { it > 0 }
                ?: 0L
            val safeTotalBytes = totalBytes.coerceAtLeast(1L)
            var processedBytes = 0L
            var lastProgressBytes = 0L
            fun updateProgress(force: Boolean = false) {
                val progress = (processedBytes.toFloat() / safeTotalBytes).coerceIn(0f, 1f)
                if (force || processedBytes - lastProgressBytes >= PROGRESS_UPDATE_BYTES) {
                    lastProgressBytes = processedBytes
                    _transferState.update { current ->
                        current.copy(
                            progress = progress,
                            processedBytes = processedBytes,
                            totalBytes = totalBytes
                        )
                    }
                }
            }
            _transferState.update { it.copy(totalBytes = totalBytes) }
            var snapshot: BackupSnapshot? = null
            val mediaPayloads = mutableMapOf<String, com.example.quotepicker.data.MediaPayload>()
            resolver.openInputStream(uri)?.use { input ->
                val countingInput = CountingInputStream(BufferedInputStream(input)) { bytesRead ->
                    processedBytes = bytesRead
                    updateProgress()
                }
                val backupInput = openBackupInput(uri, countingInput)
                val (detectedInput, isZip) = detectZipInput(backupInput)
                if (isZip) {
                    ZipInputStream(detectedInput).use { zip ->
                        var entry = zip.nextEntry
                        var payload: String? = null
                        val buffer = ByteArray(PROGRESS_UPDATE_BYTES)
                        while (entry != null) {
                            when {
                                entry.name == BACKUP_JSON_NAME -> {
                                    payload = readEntryString(zip, buffer)
                                }
                                entry.name.startsWith("media/") -> {
                                    val fileName = entry.name.removePrefix("media/")
                                    val temp = File.createTempFile("media_import_", ".dat", getApplication<Application>().cacheDir)
                                    tempFiles.add(temp)
                                    readEntryToFile(zip, temp, buffer)
                                    mediaPayloads[fileName] = com.example.quotepicker.data.MediaPayload(file = temp)
                                }
                            }
                            zip.closeEntry()
                            entry = zip.nextEntry
                        }
                        if (!payload.isNullOrBlank()) {
                            snapshot = BackupSnapshot.fromJson(payload)
                        }
                    }
                } else {
                    val payload = readStreamBytes(detectedInput).toString(Charset.forName("UTF-8"))
                    snapshot = BackupSnapshot.fromJson(payload)
                }
                processedBytes = countingInput.bytesRead
                updateProgress(force = true)
            }
            snapshot?.let { repo.replaceSnapshot(it, mediaPayloads) }
        } catch (t: Throwable) {
            if (t is CancellationException) throw t
            t.printStackTrace()
        } finally {
            tempFiles.forEach { file -> file.delete() }
            _transferState.update {
                it.copy(
                    inProgress = false,
                    mode = null,
                    progress = null,
                    processedBytes = null,
                    totalBytes = null,
                    outputBytes = null
                )
            }
        }
    }

    private companion object {
        const val BACKUP_JSON_NAME = "backup.json"
        val BACKUP_MAGIC = byteArrayOf('B'.code.toByte(), 'K'.code.toByte(), 'P'.code.toByte(), '1'.code.toByte())
        const val BACKUP_VERSION: Byte = 2
        const val CTR_IV_LENGTH = 16
        const val HMAC_SHA256_LENGTH = 32
        const val ZIP_MAGIC_P: Byte = 0x50
        const val ZIP_MAGIC_K: Byte = 0x4B
        const val PROGRESS_UPDATE_BYTES = 256 * 1024
        const val MAX_ZERO_READ_RETRIES = 1024
        val BACKUP_AES_KEY = "QuotePickerBackupAES256KeyMaterial!".toByteArray(Charset.forName("UTF-8")).copyOf(32)
        val BACKUP_HMAC_KEY = "QuotePickerBackupHmac256KeyMaterial".toByteArray(Charset.forName("UTF-8")).copyOf(32)
    }

    private class CountingOutputStream(
        private val delegate: OutputStream,
        private val onBytesWritten: (Long) -> Unit
    ) : OutputStream() {
        var bytesWritten: Long = 0
            private set

        override fun write(b: Int) {
            delegate.write(b)
            bytesWritten += 1
            onBytesWritten(bytesWritten)
        }

        override fun write(b: ByteArray, off: Int, len: Int) {
            delegate.write(b, off, len)
            bytesWritten += len
            onBytesWritten(bytesWritten)
        }

        override fun flush() {
            delegate.flush()
        }

        override fun close() {
            delegate.close()
        }
    }

    private class CountingInputStream(
        private val delegate: InputStream,
        private val onBytesRead: (Long) -> Unit
    ) : InputStream() {
        var bytesRead: Long = 0
            private set

        override fun read(): Int {
            val value = delegate.read()
            if (value >= 0) {
                bytesRead += 1
                onBytesRead(bytesRead)
            }
            return value
        }

        override fun read(b: ByteArray, off: Int, len: Int): Int {
            val read = delegate.read(b, off, len)
            if (read > 0) {
                bytesRead += read
                onBytesRead(bytesRead)
            }
            return read
        }

        override fun close() {
            delegate.close()
        }
    }

    private fun writeChunked(output: OutputStream, data: ByteArray, onChunk: (Int) -> Unit) {
        var offset = 0
        while (offset < data.size) {
            val size = minOf(PROGRESS_UPDATE_BYTES, data.size - offset)
            output.write(data, offset, size)
            onChunk(size)
            offset += size
        }
    }

    private fun readEntryBytes(zip: ZipInputStream, buffer: ByteArray): ByteArray {
        val output = ByteArrayOutputStream()
        copyStreamChunked(zip, output, buffer) { }
        return output.toByteArray()
    }

    private fun readEntryToFile(zip: ZipInputStream, target: File, buffer: ByteArray) {
        target.outputStream().use { output ->
            copyStreamChunked(zip, output, buffer) { }
        }
    }

    private fun readEntryString(zip: ZipInputStream, buffer: ByteArray): String {
        val output = readEntryBytes(zip, buffer)
        return output.toString(Charset.forName("UTF-8"))
    }

    private fun readStreamBytes(input: InputStream): ByteArray {
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        copyStreamChunked(input, output, buffer) { }
        return output.toByteArray()
    }

    private fun writeStreamChunked(input: InputStream, output: OutputStream, onChunk: (Int) -> Unit) {
        val buffer = ByteArray(PROGRESS_UPDATE_BYTES)
        copyStreamChunked(input, output, buffer, onChunk)
    }

    private fun copyStreamChunked(
        input: InputStream,
        output: OutputStream,
        buffer: ByteArray,
        onChunk: (Int) -> Unit
    ) {
        var zeroReadCount = 0
        while (true) {
            val read = input.read(buffer)
            if (read < 0) break
            if (read == 0) {
                zeroReadCount += 1
                if (zeroReadCount >= MAX_ZERO_READ_RETRIES) {
                    throw IOException("Input stream returned 0 bytes repeatedly")
                }
                val singleByte = input.read()
                if (singleByte < 0) break
                output.write(singleByte)
                onChunk(1)
                continue
            }
            zeroReadCount = 0
            output.write(buffer, 0, read)
            onChunk(read)
        }
    }


    private fun writeEncryptedZipBackup(
        output: OutputStream,
        snapshotBytes: ByteArray,
        mediaSources: List<Repository.MediaExportSource>,
        onChunkWritten: (Int) -> Unit
    ) {
        openEncryptedBackupOutput(output).use { encryptedOutput ->
            ZipOutputStream(encryptedOutput).use { zip ->
                zip.putNextEntry(ZipEntry(BACKUP_JSON_NAME))
                writeChunked(zip, snapshotBytes, onChunkWritten)
                zip.closeEntry()
                mediaSources.forEach { source ->
                    val stream = repo.openMediaStream(source.originalPath)
                        ?: throw IOException("Failed to open media stream: ${source.originalPath}")
                    stream.use { input ->
                        zip.putNextEntry(ZipEntry("media/${source.fileName}"))
                        try {
                            writeStreamChunked(input, zip, onChunkWritten)
                        } finally {
                            zip.closeEntry()
                        }
                    }
                }
            }
        }
    }
    private fun writePlainZipBackup(
        output: OutputStream,
        snapshotBytes: ByteArray,
        mediaSources: List<Repository.MediaExportSource>,
        onChunkWritten: (Int) -> Unit
    ) {
        ZipOutputStream(output).use { zip ->
            zip.putNextEntry(ZipEntry(BACKUP_JSON_NAME))
            writeChunked(zip, snapshotBytes, onChunkWritten)
            zip.closeEntry()
            mediaSources.forEach { source ->
                val stream = repo.openMediaStream(source.originalPath)
                    ?: throw IOException("Failed to open media stream: ${source.originalPath}")
                stream.use { input ->
                    zip.putNextEntry(ZipEntry("media/${source.fileName}"))
                    try {
                        writeStreamChunked(input, zip, onChunkWritten)
                    } finally {
                        zip.closeEntry()
                    }
                }
            }
        }
    }

    private fun openEncryptedBackupOutput(output: OutputStream): OutputStream {
        val iv = ByteArray(CTR_IV_LENGTH)
        SecureRandom().nextBytes(iv)
        output.write(BACKUP_MAGIC)
        output.write(BACKUP_VERSION.toInt())
        output.write(iv)

        val mac = createHmac()
        val macOutput = HmacOutputStream(output, mac)
        val cipher = createCipher(Cipher.ENCRYPT_MODE, iv)
        val nonClosingMacOutput = NonClosingOutputStream(macOutput)
        return FinalizingOutputStream(CipherOutputStream(nonClosingMacOutput, cipher)) {
            output.write(mac.doFinal())
            output.flush()
        }
    }

    private fun createCipher(mode: Int, iv: ByteArray): Cipher {
        val key = SecretKeySpec(BACKUP_AES_KEY, "AES")
        return Cipher.getInstance("AES/CTR/NoPadding").apply {
            init(mode, key, IvParameterSpec(iv))
        }
    }

    private fun createHmac(): Mac {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(BACKUP_HMAC_KEY, "HmacSHA256"))
        return mac
    }

    private fun openBackupInput(uri: Uri, input: InputStream): InputStream {
        return if (isEncryptedBackup(uri)) {
            openEncryptedBackupInput(input)
        } else {
            input
        }
    }

    private fun isEncryptedBackup(uri: Uri): Boolean {
        val name = queryDisplayName(uri) ?: uri.lastPathSegment ?: return true
        val lower = name.lowercase()
        return lower.endsWith(".backup.bin")
    }

    private fun queryDisplayName(uri: Uri): String? {
        val resolver = getApplication<Application>().contentResolver
        return resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (nameIndex >= 0 && cursor.moveToFirst()) cursor.getString(nameIndex) else null
        }
    }

    private fun openEncryptedBackupInput(input: InputStream): InputStream {
        val prefixLength = BACKUP_MAGIC.size + 1
        val pushback = PushbackInputStream(input, prefixLength)
        val prefix = ByteArray(prefixLength)
        val read = pushback.read(prefix)
        require(read == prefixLength) { "Invalid encrypted backup header" }
        if (!prefix.copyOf(BACKUP_MAGIC.size).contentEquals(BACKUP_MAGIC)) {
            throw IllegalArgumentException("Invalid encrypted backup magic")
        }
        val version = prefix[BACKUP_MAGIC.size]
        require(version == BACKUP_VERSION) { "Unsupported backup version: $version" }

        val iv = ByteArray(CTR_IV_LENGTH)
        var offset = 0
        while (offset < iv.size) {
            val count = pushback.read(iv, offset, iv.size - offset)
            require(count > 0) { "Invalid encrypted backup header" }
            offset += count
        }

        val tempCipherFile = File.createTempFile("backup_cipher_", ".bin", getApplication<Application>().cacheDir)
        val mac = createHmac()
        val buffer = ByteArray(PROGRESS_UPDATE_BYTES)
        var pendingTail = ByteArray(0)
        tempCipherFile.outputStream().use { cipherOutput ->
            while (true) {
                val count = pushback.read(buffer)
                if (count < 0) break
                val combined = ByteArray(pendingTail.size + count)
                pendingTail.copyInto(combined, 0)
                buffer.copyInto(combined, pendingTail.size, endIndex = count)
                if (combined.size <= HMAC_SHA256_LENGTH) {
                    pendingTail = combined
                } else {
                    val cipherLength = combined.size - HMAC_SHA256_LENGTH
                    cipherOutput.write(combined, 0, cipherLength)
                    mac.update(combined, 0, cipherLength)
                    pendingTail = combined.copyOfRange(cipherLength, combined.size)
                }
            }
        }
        require(pendingTail.size == HMAC_SHA256_LENGTH) { "Encrypted backup is truncated" }
        val computedHmac = mac.doFinal()
        require(MessageDigest.isEqual(pendingTail, computedHmac)) { "Encrypted backup integrity check failed" }
        val cipherStream = CipherInputStream(tempCipherFile.inputStream(), createCipher(Cipher.DECRYPT_MODE, iv))
        return DeletingInputStream(cipherStream, tempCipherFile)
    }

    private fun detectZipInput(input: InputStream): Pair<InputStream, Boolean> {
        val pushback = PushbackInputStream(input, 2)
        val header = ByteArray(2)
        val read = pushback.read(header)
        if (read > 0) {
            pushback.unread(header, 0, read)
        }
        val isZip = read == 2 && header[0] == ZIP_MAGIC_P && header[1] == ZIP_MAGIC_K
        return pushback to isZip
    }
}


private class NonClosingOutputStream(
    private val delegate: OutputStream
) : OutputStream() {
    override fun write(b: Int) {
        delegate.write(b)
    }

    override fun write(b: ByteArray, off: Int, len: Int) {
        delegate.write(b, off, len)
    }

    override fun flush() {
        delegate.flush()
    }

    override fun close() {
        flush()
    }
}

private class DeletingInputStream(
    private val delegate: InputStream,
    private val tempFile: File
) : InputStream() {
    override fun read(): Int = delegate.read()

    override fun read(b: ByteArray, off: Int, len: Int): Int = delegate.read(b, off, len)

    override fun close() {
        try {
            delegate.close()
        } finally {
            tempFile.delete()
        }
    }
}
private class HmacOutputStream(
    private val delegate: OutputStream,
    private val mac: Mac
) : OutputStream() {
    override fun write(b: Int) {
        mac.update(b.toByte())
        delegate.write(b)
    }

    override fun write(b: ByteArray, off: Int, len: Int) {
        mac.update(b, off, len)
        delegate.write(b, off, len)
    }

    override fun flush() {
        delegate.flush()
    }

    override fun close() {
        delegate.close()
    }
}

private class FinalizingOutputStream(
    private val delegate: OutputStream,
    private val onClose: () -> Unit
) : OutputStream() {
    private var closed = false

    override fun write(b: Int) {
        delegate.write(b)
    }

    override fun write(b: ByteArray, off: Int, len: Int) {
        delegate.write(b, off, len)
    }

    override fun flush() {
        delegate.flush()
    }

    override fun close() {
        if (closed) return
        closed = true
        delegate.close()
        onClose()
    }
}
