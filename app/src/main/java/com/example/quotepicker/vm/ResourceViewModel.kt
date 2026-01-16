package com.example.quotepicker.vm

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.quotepicker.data.Repository
import com.example.quotepicker.data.ResourceEntity
import com.example.quotepicker.data.ResourceType
import com.example.quotepicker.data.ResourceWithTagsCharacters
import com.example.quotepicker.data.TagCategoryEntity
import com.example.quotepicker.data.TagEntity
import com.example.quotepicker.data.CharacterEntity
import com.example.quotepicker.util.EncryptedFileManager
import com.example.quotepicker.util.ImageCompression
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class ResourceFilterState(
    val selectedType: ResourceType? = null,
    val selectedTagIds: Set<Long> = emptySet(),
    val selectedCharacterId: Long? = null
)

data class ResourceUiState(
    val resources: List<ResourceWithTagsCharacters> = emptyList(),
    val categories: List<TagCategoryEntity> = emptyList(),
    val tags: List<TagEntity> = emptyList(),
    val characters: List<CharacterEntity> = emptyList(),
    val filters: ResourceFilterState = ResourceFilterState()
)

class ResourceViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = Repository.get(app)
    private val fileManager = EncryptedFileManager(app)

    private val filters = MutableStateFlow(ResourceFilterState())

    val uiState: StateFlow<ResourceUiState> = combine(
        repo.observeResourcesWithRelations(),
        repo.observeCategories(),
        repo.observeAllTags(),
        repo.observeCharacters(),
        filters
    ) { resources, categories, tags, characters, filter ->
        val filtered = resources.filter { res ->
            val typeMatch = filter.selectedType?.let { it == res.resource.type } ?: true
            val charMatch = filter.selectedCharacterId?.let { id ->
                res.characters.any { it.id == id }
            } ?: true
            val tagMatch = if (filter.selectedTagIds.isEmpty()) {
                true
            } else {
                res.tags.any { filter.selectedTagIds.contains(it.id) }
            }
            typeMatch && charMatch && tagMatch
        }
        ResourceUiState(
            resources = filtered,
            categories = categories,
            tags = tags,
            characters = characters,
            filters = filter
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, ResourceUiState())

    fun updateTypeFilter(type: ResourceType?) {
        filters.value = filters.value.copy(selectedType = type)
    }

    fun updateTagFilter(tagIds: Set<Long>) {
        filters.value = filters.value.copy(selectedTagIds = tagIds)
    }

    fun updateCharacterFilter(characterId: Long?) {
        filters.value = filters.value.copy(selectedCharacterId = characterId)
    }

    fun addTextQuote(title: String, text: String, tagIds: List<Long>, characterIds: List<Long>) =
        viewModelScope.launch {
            if (characterIds.isEmpty()) return@launch
            repo.addResource(
                ResourceEntity(type = ResourceType.QUOTE, title = title, quoteText = text),
                tagIds,
                characterIds
            )
        }

    fun addImageQuote(title: String, imageUri: Uri, tagIds: List<Long>, characterIds: List<Long>) =
        viewModelScope.launch(Dispatchers.IO) {
            if (characterIds.isEmpty()) return@launch
            val base64 = ImageCompression.encodeToBase64(getApplication(), imageUri)
            repo.addResource(
                ResourceEntity(type = ResourceType.QUOTE, title = title, quoteImageBase64 = base64),
                tagIds,
                characterIds
            )
        }

    fun addScene(title: String, description: String?, sceneJson: String, tagIds: List<Long>, characterIds: List<Long>) =
        viewModelScope.launch {
            if (characterIds.isEmpty()) return@launch
            repo.addResource(
                ResourceEntity(
                    type = ResourceType.SCENE,
                    title = title,
                    quoteText = description,
                    sceneJson = sceneJson
                ),
                tagIds,
                characterIds
            )
        }

    fun addEncryptedMedia(
        type: ResourceType,
        title: String,
        uri: Uri,
        tagIds: List<Long>,
        characterIds: List<Long>
    ) = viewModelScope.launch(Dispatchers.IO) {
        if (characterIds.isEmpty()) return@launch
        val targetName = "${type.name.lowercase()}_${System.currentTimeMillis()}.enc"
        val input = getApplication<Application>().contentResolver.openInputStream(uri) ?: return@launch
        val encryptedFile = fileManager.encryptToFile(input, targetName)
        repo.addResource(
            ResourceEntity(
                type = type,
                title = title,
                contentUriOrPath = encryptedFile.absolutePath
            ),
            tagIds,
            characterIds
        )
    }

    fun updateResource(resource: ResourceEntity) =
        viewModelScope.launch { repo.updateResource(resource) }

    fun updateResourceTags(resourceId: Long, tagIds: List<Long>) =
        viewModelScope.launch { repo.updateResourceTags(resourceId, tagIds) }

    fun updateResourceCharacters(resourceId: Long, characterIds: List<Long>) =
        viewModelScope.launch { repo.updateResourceCharacters(resourceId, characterIds) }

    fun deleteResource(resource: ResourceEntity) = viewModelScope.launch(Dispatchers.IO) {
        fileManager.deleteEncryptedFile(resource.contentUriOrPath)
        repo.deleteResource(resource)
    }

    suspend fun loadDecryptedBytes(path: String): ByteArray = withContext(Dispatchers.IO) {
        fileManager.openDecryptedStream(path).use { it.readBytes() }
    }

    fun decodeBase64ToBitmap(b64: String): Bitmap? {
        return runCatching {
            val bytes = Base64.decode(b64, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        }.getOrNull()
    }
}
