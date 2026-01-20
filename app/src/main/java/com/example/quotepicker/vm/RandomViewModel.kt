package com.example.quotepicker.vm

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.quotepicker.data.CharacterEntity
import com.example.quotepicker.data.Repository
import com.example.quotepicker.data.ResourceWithTagsCharacters
import com.example.quotepicker.data.TagCategoryEntity
import com.example.quotepicker.data.TagCategoryType
import com.example.quotepicker.data.TagEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class RandomUiState(
    val characters: List<CharacterEntity> = emptyList(),
    val resources: List<ResourceWithTagsCharacters> = emptyList(),
    val characterResources: List<ResourceWithTagsCharacters> = emptyList(),
    val categories: List<TagCategoryEntity> = emptyList(),
    val tags: List<TagEntity> = emptyList(),
    val selectedTagIds: Set<Long> = emptySet(),
    val selectedCharacter: CharacterEntity? = null,
    val selectedResource: ResourceWithTagsCharacters? = null
)

private data class RandomInputs(
    val characters: List<CharacterEntity>,
    val resources: List<ResourceWithTagsCharacters>,
    val categories: List<TagCategoryEntity>,
    val tags: List<TagEntity>,
    val selectedCharacterId: Long?
)

class RandomViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = Repository.get(app)
    private val selectedCharacterId = MutableStateFlow<Long?>(null)
    private val selectedResourceId = MutableStateFlow<Long?>(null)
    private val selectedTagIds = MutableStateFlow<Set<Long>>(emptySet())

    private val baseInputs = combine(
        repo.observeCharacters(),
        repo.observeResourcesWithRelations(),
        repo.observeCategories(),
        repo.observeAllTags(),
        selectedCharacterId
    ) { characters, resources, categories, tags, charId ->
        RandomInputs(
            characters = characters,
            resources = resources,
            categories = categories,
            tags = tags,
            selectedCharacterId = charId
        )
    }

    val uiState: StateFlow<RandomUiState> = combine(
        baseInputs,
        selectedResourceId,
        selectedTagIds
    ) { inputs, resId, tagIds ->
        val selectedCharacter = inputs.characters.firstOrNull { it.id == inputs.selectedCharacterId }
        val matchingResources = if (selectedCharacter == null) {
            emptyList()
        } else {
            inputs.resources.filter { res -> res.characters.any { it.id == selectedCharacter.id } }
        }
        val filteredResources = if (tagIds.isEmpty()) {
            matchingResources
        } else {
            matchingResources.filter { res -> res.tags.any { tagIds.contains(it.id) } }
        }
        val selectedResource = filteredResources.firstOrNull { it.resource.id == resId }
        val resourceCategories = inputs.categories.filter { it.type == TagCategoryType.RESOURCE }
        val categoryIds = resourceCategories.map { it.id }.toSet()
        val resourceTags = inputs.tags.filter { it.categoryId in categoryIds }
        RandomUiState(
            characters = inputs.characters,
            resources = filteredResources,
            characterResources = matchingResources,
            categories = resourceCategories,
            tags = resourceTags,
            selectedTagIds = tagIds,
            selectedCharacter = selectedCharacter,
            selectedResource = selectedResource
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, RandomUiState())

    fun randomCharacter() = viewModelScope.launch {
        val characters = uiState.value.characters
        if (characters.isNotEmpty()) {
            val pick = characters.random()
            selectedCharacterId.value = pick.id
            selectedResourceId.value = null
        }
    }

    fun randomResource() = viewModelScope.launch {
        val state = uiState.value
        if (state.resources.isNotEmpty()) {
            selectedResourceId.value = state.resources.random().resource.id
        }
    }

    fun nextResource() = viewModelScope.launch {
        val state = uiState.value
        if (state.resources.isEmpty()) return@launch
        val currentId = selectedResourceId.value
        val candidates = state.resources.filter { it.resource.id != currentId }
        selectedResourceId.value = (candidates.ifEmpty { state.resources }).random().resource.id
    }

    fun reset() {
        selectedCharacterId.value = null
        selectedResourceId.value = null
        selectedTagIds.value = emptySet()
    }

    fun updateTagFilter(tagIds: Set<Long>) {
        selectedTagIds.value = tagIds
    }
}
