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
import java.io.ByteArrayInputStream
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
    val progress: Float? = null
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

    fun exportSnapshot(uri: Uri) = viewModelScope.launch(Dispatchers.IO) {
        _transferState.update { it.copy(inProgress = true, mode = TransferMode.EXPORT, progress = 0f) }
        try {
            val resolver = getApplication<Application>().contentResolver
            val exportPackage = repo.exportSnapshotPackage()
            val totalEntries = exportPackage.mediaPayloads.size + 1
            var completedEntries = 0
            resolver.openOutputStream(uri)?.use { output ->
                ZipOutputStream(output).use { zip ->
                    zip.putNextEntry(ZipEntry(BACKUP_JSON_NAME))
                    zip.write(exportPackage.snapshot.toJsonString().toByteArray(Charset.forName("UTF-8")))
                    zip.closeEntry()
                    completedEntries += 1
                    _transferState.update { it.copy(progress = completedEntries.toFloat() / totalEntries) }
                    exportPackage.mediaPayloads.forEach { (fileName, bytes) ->
                        zip.putNextEntry(ZipEntry("media/$fileName"))
                        zip.write(bytes)
                        zip.closeEntry()
                        completedEntries += 1
                        _transferState.update { current ->
                            current.copy(progress = completedEntries.toFloat() / totalEntries)
                        }
                    }
                }
            }
        } finally {
            _transferState.update { it.copy(inProgress = false, mode = null, progress = null) }
        }
    }

    fun importSnapshot(uri: Uri) = viewModelScope.launch(Dispatchers.IO) {
        _transferState.update { it.copy(inProgress = true, mode = TransferMode.IMPORT, progress = 0f) }
        try {
            val resolver = getApplication<Application>().contentResolver
            val bytes = resolver.openInputStream(uri)?.use { it.readBytes() } ?: return@launch
            var snapshot: BackupSnapshot? = null
            val mediaPayloads = mutableMapOf<String, ByteArray>()
            if (bytes.size >= 2 && bytes[0] == ZIP_MAGIC_P && bytes[1] == ZIP_MAGIC_K) {
                val totalEntries = ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
                    var entryCount = 0
                    var entry = zip.nextEntry
                    while (entry != null) {
                        entryCount += 1
                        zip.closeEntry()
                        entry = zip.nextEntry
                    }
                    entryCount.coerceAtLeast(1)
                }
                var completedEntries = 0
                ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
                    var entry = zip.nextEntry
                    var payload: String? = null
                    while (entry != null) {
                        when {
                            entry.name == BACKUP_JSON_NAME -> {
                                payload = zip.readBytes().toString(Charset.forName("UTF-8"))
                            }
                            entry.name.startsWith("media/") -> {
                                val fileName = entry.name.removePrefix("media/")
                                mediaPayloads[fileName] = zip.readBytes()
                            }
                        }
                        zip.closeEntry()
                        completedEntries += 1
                        _transferState.update { current ->
                            current.copy(progress = completedEntries.toFloat() / totalEntries)
                        }
                        entry = zip.nextEntry
                    }
                    if (!payload.isNullOrBlank()) {
                        snapshot = BackupSnapshot.fromJson(payload)
                    }
                }
            } else {
                val payload = bytes.toString(Charset.forName("UTF-8"))
                snapshot = BackupSnapshot.fromJson(payload)
                _transferState.update { it.copy(progress = 1f) }
            }
            snapshot?.let { repo.replaceSnapshot(it, mediaPayloads) }
        } finally {
            _transferState.update { it.copy(inProgress = false, mode = null, progress = null) }
        }
    }

    private companion object {
        const val BACKUP_JSON_NAME = "backup.json"
        const val ZIP_MAGIC_P: Byte = 0x50
        const val ZIP_MAGIC_K: Byte = 0x4B
    }
}
