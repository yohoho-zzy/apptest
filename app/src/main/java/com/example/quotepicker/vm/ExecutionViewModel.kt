package com.example.quotepicker.vm

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.quotepicker.data.CharacterEntity
import com.example.quotepicker.data.ExecutionSettingsEntity
import com.example.quotepicker.data.ExecutionResourceEntity
import com.example.quotepicker.data.Repository
import com.example.quotepicker.data.ResourceWithTagsCharacters
import com.example.quotepicker.data.ResponseRecordEntity
import com.example.quotepicker.data.TagEntity
import com.example.quotepicker.ui.components.plainTagName
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
    val tagName: String,
    val isTriggerCategory: Boolean = false
)

data class ExecutionUiState(
    val records: List<ResponseRecordDisplay> = emptyList(),
    val settings: ExecutionSettingsEntity = ExecutionSettingsEntity(),
    val executionItems: List<ExecutionResourceDisplay> = emptyList(),
    val resources: List<ResourceWithTagsCharacters> = emptyList(),
    val characters: List<CharacterEntity> = emptyList(),
    val tags: List<TagEntity> = emptyList(),
    val categories: List<com.example.quotepicker.data.TagCategoryEntity> = emptyList()
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

    private val baseExecutionFlow = combine(
        repo.observeResponseRecords(),
        repo.observeCharacters(),
        repo.observeAllTags(),
        repo.observeResourcesWithRelations(),
        repo.observeExecutionSettings(),
        repo.observeCategories()
    ) { records, characters, tags, resources, settings, categories ->
        BaseExecutionData(
            records = records,
            characters = characters,
            tags = tags,
            resources = resources,
            settings = settings,
            categories = categories
        )
    }

    val uiState: StateFlow<ExecutionUiState> = combine(
        baseExecutionFlow,
        repo.observeExecutionResources()
    ) { baseData, executionItems ->
        val characterMap = baseData.characters.associateBy { it.id }
        val tagMap = baseData.tags.associateBy { it.id }
        val categoryMap = baseData.categories.associateBy { it.id }
        val resourcesById = baseData.resources.associateBy { it.resource.id }
        val displayRecords = baseData.records.map { record ->
            ResponseRecordDisplay(
                characterId = record.characterId,
                tagId = record.tagId,
                count = record.count,
                characterName = characterMap[record.characterId]?.name ?: "角色",
                tagName = tagMap[record.tagId]?.name?.let(::plainTagName) ?: "标签",
                isTriggerCategory = categoryMap[tagMap[record.tagId]?.categoryId]?.name == "触发类别"
            )
        }
        val displayExecutionItems = executionItems.mapNotNull { item ->
            val resource = resourcesById[item.resourceId] ?: return@mapNotNull null
            item.toDisplay(resource)
        }
        ExecutionUiState(
            records = displayRecords,
            settings = baseData.settings ?: ExecutionSettingsEntity(),
            executionItems = displayExecutionItems,
            resources = baseData.resources,
            characters = baseData.characters,
            tags = baseData.tags,
            categories = baseData.categories
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

    fun applyExecutionCompletion(characterId: Long, completionScoreSum: Int) = viewModelScope.launch {
        repo.applyExecutionCompletion(characterId, completionScoreSum)
    }

    fun applyExecutionCompletionWithTrigger(characterId: Long, currentPoints: Int) = viewModelScope.launch {
        repo.applyExecutionCompletion(characterId, 30)
        when {
            currentPoints < 10 -> repo.addResponseRecordByCategoryPrefix(characterId, "触发类别", "E")
            currentPoints < 20 -> repo.addResponseRecordByCategoryPrefix(characterId, "触发类别", "D")
        }
    }

    fun addTriggerRecordByPrefix(characterId: Long, prefix: String) = viewModelScope.launch {
        repo.addResponseRecordByCategoryPrefix(characterId, "触发类别", prefix)
    }

    fun incrementCharacterFamiliarity(characterId: Long) = viewModelScope.launch {
        repo.incrementCharacterFamiliarity(characterId)
    }

    fun updateCharacterFamiliarity(characterId: Long, familiarity: Int) = viewModelScope.launch {
        repo.updateCharacterFamiliarity(characterId, familiarity)
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

private data class BaseExecutionData(
    val records: List<ResponseRecordEntity>,
    val characters: List<CharacterEntity>,
    val tags: List<TagEntity>,
    val resources: List<ResourceWithTagsCharacters>,
    val settings: ExecutionSettingsEntity?,
    val categories: List<com.example.quotepicker.data.TagCategoryEntity>
)

private fun ExecutionResourceEntity.toDisplay(resource: ResourceWithTagsCharacters): ExecutionResourceDisplay =
    ExecutionResourceDisplay(
        id = id,
        resource = resource,
        characterId = characterId,
        characterName = characterName,
        tagName = tagName
    )
