package com.example.quotepicker.vm

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.quotepicker.data.CharacterEntity
import com.example.quotepicker.data.Repository
import com.example.quotepicker.data.ResourceWithTagsCharacters
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class RandomUiState(
    val characters: List<CharacterEntity> = emptyList(),
    val resources: List<ResourceWithTagsCharacters> = emptyList(),
    val selectedCharacter: CharacterEntity? = null,
    val selectedResource: ResourceWithTagsCharacters? = null
)

class RandomViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = Repository.get(app)
    private val selectedCharacterId = MutableStateFlow<Long?>(null)
    private val selectedResourceId = MutableStateFlow<Long?>(null)

    val uiState: StateFlow<RandomUiState> = combine(
        repo.observeCharacters(),
        repo.observeResourcesWithRelations(),
        selectedCharacterId,
        selectedResourceId
    ) { characters, resources, charId, resId ->
        val selectedCharacter = characters.firstOrNull { it.id == charId }
        val matchingResources = if (selectedCharacter == null) {
            emptyList()
        } else {
            resources.filter { res -> res.characters.any { it.id == selectedCharacter.id } }
        }
        val selectedResource = matchingResources.firstOrNull { it.resource.id == resId }
        RandomUiState(
            characters = characters,
            resources = matchingResources,
            selectedCharacter = selectedCharacter,
            selectedResource = selectedResource
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, RandomUiState())

    fun randomCharacter() = viewModelScope.launch {
        val characters = repo.observeCharacters()
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList()).value
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
    }
}
