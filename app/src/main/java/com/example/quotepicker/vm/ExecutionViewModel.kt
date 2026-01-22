package com.example.quotepicker.vm

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.quotepicker.data.CharacterEntity
import com.example.quotepicker.data.ExecutionSettingsEntity
import com.example.quotepicker.data.Repository
import com.example.quotepicker.data.ResourceWithTagsCharacters
import com.example.quotepicker.data.TagEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch


data class ResponseRecordDisplay(
    val characterId: Long,
    val tagId: Long,
    val count: Int,
    val characterName: String,
    val tagName: String
)

data class ExecutionUiState(
    val records: List<ResponseRecordDisplay> = emptyList(),
    val settings: ExecutionSettingsEntity = ExecutionSettingsEntity(),
    val resources: List<ResourceWithTagsCharacters> = emptyList(),
    val characters: List<CharacterEntity> = emptyList(),
    val tags: List<TagEntity> = emptyList()
)

class ExecutionViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = Repository.get(app)

    init {
        viewModelScope.launch {
            repo.ensureExecutionSettings()
        }
    }

    val uiState: StateFlow<ExecutionUiState> = combine(
        repo.observeResponseRecords(),
        repo.observeCharacters(),
        repo.observeAllTags(),
        repo.observeResourcesWithRelations(),
        repo.observeExecutionSettings()
    ) { records, characters, tags, resources, settings ->
        val characterMap = characters.associateBy { it.id }
        val tagMap = tags.associateBy { it.id }
        val displayRecords = records.map { record ->
            ResponseRecordDisplay(
                characterId = record.characterId,
                tagId = record.tagId,
                count = record.count,
                characterName = characterMap[record.characterId]?.name ?: "角色",
                tagName = tagMap[record.tagId]?.name ?: "标签"
            )
        }
        ExecutionUiState(
            records = displayRecords,
            settings = settings ?: ExecutionSettingsEntity(),
            resources = resources,
            characters = characters,
            tags = tags
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, ExecutionUiState())

    fun consumeRecord(characterId: Long, tagId: Long) = viewModelScope.launch {
        repo.consumeResponseRecord(characterId, tagId)
    }

    fun updateSettings(settings: ExecutionSettingsEntity) = viewModelScope.launch {
        repo.updateExecutionSettings(settings)
    }

    fun updateCharacterPoints(characterId: Long, points: Int) = viewModelScope.launch {
        repo.updateCharacterPoints(characterId, points)
    }
}
