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
import com.example.quotepicker.data.TagCategoryType
import com.example.quotepicker.data.TagEntity
import com.example.quotepicker.data.CharacterEntity
import com.example.quotepicker.util.ImageCompression
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

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

data class ImageUpdateItem(
    val base64: String? = null,
    val uri: Uri? = null
)

data class VideoUpdateItem(
    val path: String? = null,
    val uri: Uri? = null
)

data class SceneMessageDraft(
    val speaker: String,
    val content: String
)

data class FlowUpdateItem(
    val type: ResourceType,
    val title: String? = null,
    val resourceId: Long? = null,
    val text: String? = null,
    val sceneMessages: List<SceneMessageDraft> = emptyList(),
    val images: List<ImageUpdateItem> = emptyList(),
    val videos: List<VideoUpdateItem> = emptyList()
)

class ResourceViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = Repository.get(app)

    private val filters = MutableStateFlow(ResourceFilterState())

    val allResources: StateFlow<List<ResourceWithTagsCharacters>> = repo.observeResourcesWithRelations()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val uiState: StateFlow<ResourceUiState> = combine(
        allResources,
        repo.observeCategories(),
        repo.observeAllTags(),
        repo.observeCharacters(),
        filters
    ) { resources, categories, tags, characters, filter ->
        val resourceCategories = categories.filter { it.type == TagCategoryType.RESOURCE }
        val resourceCategoryIds = resourceCategories.map { it.id }.toSet()
        val resourceTags = tags.filter { it.categoryId in resourceCategoryIds }
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
            categories = resourceCategories,
            tags = resourceTags,
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

    fun addTextResource(title: String, text: String, tagIds: List<Long>, characterIds: List<Long>) =
        viewModelScope.launch {
            if (characterIds.isEmpty()) return@launch
            repo.addResource(
                ResourceEntity(type = ResourceType.TEXT, title = title, quoteText = text),
                tagIds,
                characterIds
            )
        }

    fun addImageGroup(title: String, imageUris: List<Uri>, tagIds: List<Long>, characterIds: List<Long>) =
        viewModelScope.launch(Dispatchers.IO) {
            if (characterIds.isEmpty()) return@launch
            val base64List = imageUris.mapNotNull { uri ->
                ImageCompression.encodeToBase64(getApplication(), uri).ifBlank { null }
            }
            val payload = org.json.JSONArray(base64List).toString()
            repo.addResource(
                ResourceEntity(type = ResourceType.IMAGE, title = title, quoteImageBase64 = payload),
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

    fun addVideoGroup(title: String, uris: List<Uri>, tagIds: List<Long>, characterIds: List<Long>) =
        viewModelScope.launch(Dispatchers.IO) {
            if (characterIds.isEmpty()) return@launch
            val storedUris = uris.mapNotNull { uri -> storeVideoToInternal(uri) }
            if (storedUris.isEmpty()) return@launch
            val payload = org.json.JSONArray(storedUris).toString()
            repo.addResource(
                ResourceEntity(
                    type = ResourceType.VIDEO,
                    title = title,
                    contentUriOrPath = payload
                ),
                tagIds,
                characterIds
            )
        }

    fun addFlow(
        title: String,
        items: List<FlowUpdateItem>,
        tagIds: List<Long>,
        characterIds: List<Long>
    ) = viewModelScope.launch(Dispatchers.IO) {
        if (characterIds.isEmpty()) return@launch
        val payload = buildFlowPayloadWithVideos(items)
        repo.addResource(
            ResourceEntity(
                type = ResourceType.FLOW,
                title = title,
                sceneJson = payload
            ),
            tagIds,
            characterIds
        )
    }

    fun updateResource(resource: ResourceEntity) =
        viewModelScope.launch { repo.updateResource(resource) }

    fun updateTextResource(
        resource: ResourceEntity,
        title: String,
        text: String,
        tagIds: List<Long>,
        characterIds: List<Long>
    ) = viewModelScope.launch {
        if (characterIds.isEmpty()) return@launch
        repo.updateResource(resource.copy(title = title, quoteText = text))
        updateResourceTags(resource.id, tagIds)
        updateResourceCharacters(resource.id, characterIds)
    }

    fun updateSceneResource(
        resource: ResourceEntity,
        title: String,
        description: String?,
        sceneJson: String,
        tagIds: List<Long>,
        characterIds: List<Long>
    ) = viewModelScope.launch {
        if (characterIds.isEmpty()) return@launch
        repo.updateResource(
            resource.copy(title = title, quoteText = description, sceneJson = sceneJson)
        )
        updateResourceTags(resource.id, tagIds)
        updateResourceCharacters(resource.id, characterIds)
    }

    fun updateImageGroup(
        resource: ResourceEntity,
        title: String,
        items: List<ImageUpdateItem>,
        tagIds: List<Long>,
        characterIds: List<Long>
    ) = viewModelScope.launch(Dispatchers.IO) {
        if (characterIds.isEmpty()) return@launch
        val payload = org.json.JSONArray(encodeImageItems(items)).toString()
        repo.updateResource(resource.copy(title = title, quoteImageBase64 = payload))
        updateResourceTags(resource.id, tagIds)
        updateResourceCharacters(resource.id, characterIds)
    }

    fun updateVideoGroup(
        resource: ResourceEntity,
        title: String,
        items: List<VideoUpdateItem>,
        tagIds: List<Long>,
        characterIds: List<Long>
    ) = viewModelScope.launch(Dispatchers.IO) {
        if (characterIds.isEmpty()) return@launch
        val newUris = resolveVideoItems(items)
        if (newUris.isEmpty()) return@launch
        val payload = org.json.JSONArray(newUris).toString()
        repo.updateResource(resource.copy(title = title, contentUriOrPath = payload))
        updateResourceTags(resource.id, tagIds)
        updateResourceCharacters(resource.id, characterIds)
    }

    fun updateFlow(
        resource: ResourceEntity,
        title: String,
        items: List<FlowUpdateItem>,
        tagIds: List<Long>,
        characterIds: List<Long>
    ) = viewModelScope.launch(Dispatchers.IO) {
        if (characterIds.isEmpty()) return@launch
        val payload = buildFlowPayloadWithVideos(items)
        repo.updateResource(resource.copy(title = title, sceneJson = payload))
        updateResourceTags(resource.id, tagIds)
        updateResourceCharacters(resource.id, characterIds)
    }

    fun updateResourceTags(resourceId: Long, tagIds: List<Long>) =
        viewModelScope.launch { repo.updateResourceTags(resourceId, tagIds) }

    fun updateResourceCharacters(resourceId: Long, characterIds: List<Long>) =
        viewModelScope.launch { repo.updateResourceCharacters(resourceId, characterIds) }

    fun deleteResource(resource: ResourceEntity) = viewModelScope.launch(Dispatchers.IO) {
        repo.deleteResource(resource)
    }

    private suspend fun resolveVideoItems(items: List<VideoUpdateItem>): List<String> {
        return items.mapNotNull { item ->
            item.path?.ifBlank { null } ?: item.uri?.let { uri -> storeVideoToInternal(uri) }
        }.filter { it.isNotBlank() }
    }

    private suspend fun encodeImageItems(items: List<ImageUpdateItem>): List<String> {
        return items.mapNotNull { item ->
            item.base64 ?: item.uri?.let { uri ->
                ImageCompression.encodeToBase64(getApplication(), uri).ifBlank { null }
            }
        }
    }

    private suspend fun buildFlowPayloadWithVideos(items: List<FlowUpdateItem>): String {
        val array = org.json.JSONArray()
        items.forEach { item ->
            val obj = org.json.JSONObject()
            obj.put("type", item.type.name)
            item.title?.takeIf { it.isNotBlank() }?.let { obj.put("title", it) }
            item.resourceId?.let { obj.put("resourceId", it) }
            when (item.type) {
                ResourceType.TEXT -> obj.put("text", item.text.orEmpty())
                ResourceType.SCENE -> {
                    val messages = org.json.JSONArray()
                    item.sceneMessages.forEach { message ->
                        val msg = org.json.JSONObject()
                        msg.put("speaker", message.speaker)
                        msg.put("text", message.content)
                        messages.put(msg)
                    }
                    obj.put("messages", messages)
                }
                ResourceType.IMAGE -> obj.put("images", org.json.JSONArray(encodeImageItems(item.images)))
                ResourceType.VIDEO -> obj.put("videos", org.json.JSONArray(resolveVideoItems(item.videos)))
                ResourceType.FLOW -> obj.put("text", item.text.orEmpty())
            }
            array.put(obj)
        }
        return array.toString()
    }

    fun decodeBase64ToBitmap(b64: String): Bitmap? {
        return runCatching {
            val bytes = Base64.decode(b64, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        }.getOrNull()
    }

    private fun storeVideoToInternal(uri: Uri): String? {
        return runCatching {
            val dir = File(getApplication<Application>().filesDir, "videos").apply { mkdirs() }
            val target = File(dir, "video_${System.currentTimeMillis()}_${UUID.randomUUID()}.bin")
            getApplication<Application>().contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(target).use { output ->
                    input.copyTo(output)
                }
            } ?: return null
            Uri.fromFile(target).toString()
        }.getOrNull()
    }
}
