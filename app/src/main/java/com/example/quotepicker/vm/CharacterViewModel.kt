package com.example.quotepicker.vm

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.quotepicker.data.BackupSnapshot
import com.example.quotepicker.data.CharacterEntity
import com.example.quotepicker.data.CharacterWithTags
import com.example.quotepicker.data.Repository
import com.example.quotepicker.data.TagCategoryEntity
import com.example.quotepicker.data.TagCategoryType
import com.example.quotepicker.data.TagEntity
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.io.PushbackInputStream
import java.nio.charset.Charset
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

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

    fun exportSnapshot(uri: Uri) = viewModelScope.launch(Dispatchers.IO) {
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
            resolver.openOutputStream(uri)?.use { output ->
                val countingOutput = CountingOutputStream(output) { bytesWritten ->
                    outputBytes = bytesWritten
                    updateProgress()
                }
                ZipOutputStream(countingOutput).use { zip ->
                    zip.putNextEntry(ZipEntry(BACKUP_JSON_NAME))
                    writeChunked(zip, snapshotBytes) { written ->
                        processedBytes += written
                        updateProgress()
                    }
                    zip.closeEntry()
                    exportPackage.mediaSources.forEach { source ->
                        val stream = repo.openMediaStream(source.originalPath) ?: return@forEach
                        stream.use { input ->
                            zip.putNextEntry(ZipEntry("media/${source.fileName}"))
                            writeStreamChunked(input, zip) { written ->
                                processedBytes += written
                                updateProgress()
                            }
                            zip.closeEntry()
                        }
                    }
                }
            }
            updateProgress(force = true)
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
                val pushbackInput = PushbackInputStream(input, 2)
                val header = ByteArray(2)
                val headerSize = pushbackInput.read(header)
                if (headerSize > 0) {
                    pushbackInput.unread(header, 0, headerSize)
                }
                val countingInput = CountingInputStream(pushbackInput) { bytesRead ->
                    processedBytes = bytesRead
                    updateProgress()
                }
                val isZip = headerSize == 2 && header[0] == ZIP_MAGIC_P && header[1] == ZIP_MAGIC_K
                if (isZip) {
                    ZipInputStream(countingInput).use { zip ->
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
                        processedBytes = countingInput.bytesRead
                        updateProgress(force = true)
                    }
                } else {
                    val payload = readStreamString(countingInput)
                    snapshot = BackupSnapshot.fromJson(payload)
                    processedBytes = countingInput.bytesRead
                    updateProgress(force = true)
                }
            }
            snapshot?.let { repo.replaceSnapshot(it, mediaPayloads) }
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
        const val ZIP_MAGIC_P: Byte = 0x50
        const val ZIP_MAGIC_K: Byte = 0x4B
        const val PROGRESS_UPDATE_BYTES = 256 * 1024
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
        var read = zip.read(buffer)
        while (read > 0) {
            output.write(buffer, 0, read)
            read = zip.read(buffer)
        }
        return output.toByteArray()
    }

    private fun readEntryToFile(zip: ZipInputStream, target: File, buffer: ByteArray) {
        target.outputStream().use { output ->
            var read = zip.read(buffer)
            while (read > 0) {
                output.write(buffer, 0, read)
                read = zip.read(buffer)
            }
        }
    }

    private fun readEntryString(zip: ZipInputStream, buffer: ByteArray): String {
        val output = readEntryBytes(zip, buffer)
        return output.toString(Charset.forName("UTF-8"))
    }

    private fun readStreamString(input: InputStream): String {
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        var read = input.read(buffer)
        while (read > 0) {
            output.write(buffer, 0, read)
            read = input.read(buffer)
        }
        return output.toString(Charset.forName("UTF-8"))
    }

    private fun writeStreamChunked(input: InputStream, output: OutputStream, onChunk: (Int) -> Unit) {
        val buffer = ByteArray(PROGRESS_UPDATE_BYTES)
        var read = input.read(buffer)
        while (read > 0) {
            output.write(buffer, 0, read)
            onChunk(read)
            read = input.read(buffer)
        }
    }
}
