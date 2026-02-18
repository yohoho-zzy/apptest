package com.example.quotepicker.vm

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.quotepicker.data.CharacterEntity
import com.example.quotepicker.data.ExecutionSettingsEntity
import com.example.quotepicker.data.ExecutionResourceEntity
import com.example.quotepicker.data.Repository
import com.example.quotepicker.data.ResourceWithTagsCharacters
import com.example.quotepicker.data.TagEntity
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.roundToInt
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
    val executionItems: List<ExecutionResourceDisplay> = emptyList(),
    val resources: List<ResourceWithTagsCharacters> = emptyList(),
    val characters: List<CharacterEntity> = emptyList(),
    val tags: List<TagEntity> = emptyList()
)

data class ExecutionResourceDisplay(
    val id: Long,
    val resource: ResourceWithTagsCharacters,
    val characterId: Long,
    val characterName: String,
    val tagName: String
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
        repo.observeExecutionSettings(),
        repo.observeExecutionResources()
    ) { records, characters, tags, resources, settings, executionItems ->
        val characterMap = characters.associateBy { it.id }
        val tagMap = tags.associateBy { it.id }
        val resourcesById = resources.associateBy { it.resource.id }
        val displayRecords = records.map { record ->
            ResponseRecordDisplay(
                characterId = record.characterId,
                tagId = record.tagId,
                count = record.count,
                characterName = characterMap[record.characterId]?.name ?: "角色",
                tagName = tagMap[record.tagId]?.name ?: "标签"
            )
        }
        val displayExecutionItems = executionItems.mapNotNull { item ->
            val resource = resourcesById[item.resourceId] ?: return@mapNotNull null
            item.toDisplay(resource)
        }
        ExecutionUiState(
            records = displayRecords,
            settings = settings ?: ExecutionSettingsEntity(),
            executionItems = displayExecutionItems,
            resources = resources,
            characters = characters,
            tags = tags
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, ExecutionUiState())

    fun consumeRecord(characterId: Long, tagId: Long) = viewModelScope.launch {
        repo.consumeResponseRecord(characterId, tagId)
    }

    fun consumeRecordAndAddExecutionResource(
        record: ResponseRecordDisplay,
        resource: ResourceWithTagsCharacters
    ) = viewModelScope.launch {
        repo.addExecutionResource(
            resourceId = resource.resource.id,
            characterId = record.characterId,
            tagId = record.tagId,
            characterName = record.characterName,
            tagName = record.tagName
        )
        repo.consumeResponseRecord(record.characterId, record.tagId)
    }

    fun removeExecutionResource(id: Long) = viewModelScope.launch {
        repo.removeExecutionResource(id)
    }

    fun updateSettings(settings: ExecutionSettingsEntity) = viewModelScope.launch {
        repo.updateExecutionSettings(settings)
    }

    fun updateCharacterPoints(characterId: Long, points: Int) = viewModelScope.launch {
        repo.updateCharacterPoints(characterId, points)
    }

    fun incrementCharacterFamiliarity(characterId: Long) = viewModelScope.launch {
        repo.incrementCharacterFamiliarity(characterId)
    }

    fun applyDailyInput(input: Int) = viewModelScope.launch {
        val current = uiState.value.settings
        val today = LocalDate.now()
        val lastDate = current.lastExecutionDate?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
        var pastAverage = current.dailyAverage
        var remaining = current.remainingValue
        if (lastDate != null) {
            val daysBetween = ChronoUnit.DAYS.between(lastDate, today).toInt()
            if (daysBetween > 1) {
                repeat(daysBetween - 1) {
                    val dailyAverage = ((pastAverage + 0) / 2.0).roundToInt()
                    remaining += dailyAverage
                    pastAverage = dailyAverage
                }
            } else if (daysBetween <= 0) {
                pastAverage = current.dailyAverage
            }
        } else {
            pastAverage = current.dailyAverage
        }
        val newDailyAverage = ((pastAverage + input) / 2.0).roundToInt()
        remaining += newDailyAverage
        repo.updateExecutionSettings(
            current.copy(
                pastAverage = pastAverage,
                lastInputValue = input,
                dailyAverage = newDailyAverage,
                remainingValue = remaining,
                lastExecutionDate = today.toString()
            )
        )
    }

    fun consumeExecutionRemaining() = viewModelScope.launch {
        val current = uiState.value.settings
        if (current.remainingValue <= 0) return@launch
        repo.updateExecutionSettings(current.copy(remainingValue = current.remainingValue - 1))
    }
}

private fun ExecutionResourceEntity.toDisplay(resource: ResourceWithTagsCharacters): ExecutionResourceDisplay =
    ExecutionResourceDisplay(
        id = id,
        resource = resource,
        characterId = characterId,
        characterName = characterName,
        tagName = tagName
    )
