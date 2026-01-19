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
    val categories: List<TagCategoryEntity> = emptyList(),
    val tags: List<TagEntity> = emptyList(),
    val selectedTagIds: Set<Long> = emptySet(),
    val selectedCharacter: CharacterEntity? = null,
    val selectedResource: ResourceWithTagsCharacters? = null
)

class RandomViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = Repository.get(app)
    private val selectedCharacterId = MutableStateFlow<Long?>(null)
    private val selectedResourceId = MutableStateFlow<Long?>(null)
    private val selectedTagIds = MutableStateFlow<Set<Long>>(emptySet())

    val uiState: StateFlow<RandomUiState> = combine(
        repo.observeCharacters(),
        repo.observeResourcesWithRelations(),
        repo.observeCategories(),
        repo.observeAllTags(),
        selectedCharacterId,
        selectedResourceId,
        selectedTagIds
    ) { characters, resources, categories, tags, charId, resId, tagIds ->
        val selectedCharacter = characters.firstOrNull { it.id == charId }
        val matchingResources = if (selectedCharacter == null) {
            emptyList()
        } else {
            resources.filter { res -> res.characters.any { it.id == selectedCharacter.id } }
        }
        val filteredResources = if (tagIds.isEmpty()) {
            matchingResources
        } else {
            matchingResources.filter { res -> res.tags.any { tagIds.contains(it.id) } }
        }
        val selectedResource = filteredResources.firstOrNull { it.resource.id == resId }
        val resourceCategories = categories.filter { it.type == TagCategoryType.RESOURCE }
        val categoryIds = resourceCategories.map { it.id }.toSet()
        val resourceTags = tags.filter { it.categoryId in categoryIds }
        RandomUiState(
            characters = characters,
            resources = filteredResources,
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
