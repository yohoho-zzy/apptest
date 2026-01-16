package com.example.quotepicker.vm

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.quotepicker.data.Repository
import com.example.quotepicker.data.TagCategoryEntity
import com.example.quotepicker.data.TagEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class TagUiState(
    val categories: List<TagCategoryEntity> = emptyList(),
    val currentCategory: TagCategoryEntity? = null,
    val tags: List<TagEntity> = emptyList()
)

class TagViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = Repository.get(app)
    private val selectedCategoryId = MutableStateFlow<Long?>(null)

    private val tagsFlow = selectedCategoryId.flatMapLatest { categoryId ->
        if (categoryId == null) {
            kotlinx.coroutines.flow.flowOf(emptyList())
        } else {
            repo.observeTagsByCategory(categoryId)
        }
    }

    val uiState: StateFlow<TagUiState> = combine(
        repo.observeCategories(),
        selectedCategoryId,
        tagsFlow
    ) { categories, selectedId, tags ->
        val current = categories.firstOrNull { it.id == selectedId }
        TagUiState(categories = categories, currentCategory = current, tags = tags)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, TagUiState())

    fun selectCategory(id: Long?) { selectedCategoryId.value = id }

    fun addCategory(name: String) = viewModelScope.launch { repo.addCategory(name) }
    fun updateCategory(category: TagCategoryEntity) = viewModelScope.launch { repo.updateCategory(category) }
    fun deleteCategory(category: TagCategoryEntity) = viewModelScope.launch { repo.deleteCategory(category) }

    fun addTag(categoryId: Long, name: String, colorArgb: Int) =
        viewModelScope.launch { repo.addTag(categoryId, name, colorArgb) }

    fun updateTag(tag: TagEntity) = viewModelScope.launch { repo.updateTag(tag) }
    fun deleteTag(tag: TagEntity) = viewModelScope.launch { repo.deleteTag(tag) }
}
