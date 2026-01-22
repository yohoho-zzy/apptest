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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class CharacterUiState(
    val characters: List<CharacterWithTags> = emptyList(),
    val categories: List<TagCategoryEntity> = emptyList(),
    val tags: List<TagEntity> = emptyList(),
    val executionSettings: com.example.quotepicker.data.ExecutionSettingsEntity = com.example.quotepicker.data.ExecutionSettingsEntity()
)

class CharacterViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = Repository.get(app)

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
        val resolver = getApplication<Application>().contentResolver
        val exportPackage = repo.exportSnapshotPackage()
        resolver.openOutputStream(uri)?.use { output ->
            ZipOutputStream(output).use { zip ->
                zip.putNextEntry(ZipEntry(BACKUP_JSON_NAME))
                zip.write(exportPackage.snapshot.toJsonString().toByteArray(Charset.forName("UTF-8")))
                zip.closeEntry()
                exportPackage.mediaPayloads.forEach { (fileName, bytes) ->
                    zip.putNextEntry(ZipEntry("media/$fileName"))
                    zip.write(bytes)
                    zip.closeEntry()
                }
            }
        }
    }

    fun importSnapshot(uri: Uri) = viewModelScope.launch(Dispatchers.IO) {
        val resolver = getApplication<Application>().contentResolver
        val bytes = resolver.openInputStream(uri)?.use { it.readBytes() } ?: return@launch
        var snapshot: BackupSnapshot? = null
        val mediaPayloads = mutableMapOf<String, ByteArray>()
        if (bytes.size >= 2 && bytes[0] == ZIP_MAGIC_P && bytes[1] == ZIP_MAGIC_K) {
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
                    entry = zip.nextEntry
                }
                if (!payload.isNullOrBlank()) {
                    snapshot = BackupSnapshot.fromJson(payload)
                }
            }
        } else {
            val payload = bytes.toString(Charset.forName("UTF-8"))
            snapshot = BackupSnapshot.fromJson(payload)
        }
        snapshot?.let { repo.replaceSnapshot(it, mediaPayloads) }
    }

    private companion object {
        const val BACKUP_JSON_NAME = "backup.json"
        const val ZIP_MAGIC_P: Byte = 0x50
        const val ZIP_MAGIC_K: Byte = 0x4B
    }
}
