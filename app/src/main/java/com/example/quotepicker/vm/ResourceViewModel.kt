package com.example.quotepicker.vm

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Base64
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.documentfile.provider.DocumentFile
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
    val path: String? = null,
    val base64: String? = null,
    val uri: Uri? = null,
    val motionVideoPath: String? = null,
    val motionVideoUri: Uri? = null
)

data class VideoUpdateItem(
    val path: String? = null,
    val uri: Uri? = null
)

data class SoundUpdateItem(
    val path: String? = null,
    val uri: Uri? = null
)

data class StoredImageResult(
    val imagePath: String,
    val motionVideoPath: String? = null
)

data class StoredMediaItem(
    val path: String,
    val type: ResourceType
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
            val storedPaths = imageUris.mapNotNull { uri -> storeImageToInternal(uri) }
                .map { stored ->
                    if (stored.motionVideoPath != null) {
                        org.json.JSONObject()
                            .put("image", stored.imagePath)
                            .put("motionVideo", stored.motionVideoPath)
                    } else {
                        stored.imagePath
                    }
                }
            if (storedPaths.isEmpty()) return@launch
            val payload = org.json.JSONArray(storedPaths).toString()
            repo.addResource(
                ResourceEntity(type = ResourceType.IMAGE, title = title, contentUriOrPath = payload),
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

    fun addSoundGroup(title: String, uris: List<Uri>, tagIds: List<Long>, characterIds: List<Long>) =
        viewModelScope.launch(Dispatchers.IO) {
            if (characterIds.isEmpty()) return@launch
            val storedUris = uris.mapNotNull { uri -> storeAudioToInternal(uri) }
            if (storedUris.isEmpty()) return@launch
            val payload = org.json.JSONArray(storedUris).toString()
            repo.addResource(
                ResourceEntity(
                    type = ResourceType.SOUND,
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
        val resolved = resolveImageItems(items)
        if (resolved.isEmpty()) return@launch
        val payload = org.json.JSONArray(resolved).toString()
        repo.updateResource(resource.copy(title = title, contentUriOrPath = payload, quoteImageBase64 = null))
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

    fun updateSoundGroup(
        resource: ResourceEntity,
        title: String,
        items: List<SoundUpdateItem>,
        tagIds: List<Long>,
        characterIds: List<Long>
    ) = viewModelScope.launch(Dispatchers.IO) {
        if (characterIds.isEmpty()) return@launch
        val newUris = resolveSoundItems(items)
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

    private suspend fun resolveSoundItems(items: List<SoundUpdateItem>): List<String> {
        return items.mapNotNull { item ->
            item.path?.ifBlank { null } ?: item.uri?.let { uri -> storeAudioToInternal(uri) }
        }.filter { it.isNotBlank() }
    }

    private suspend fun resolveImageItems(items: List<ImageUpdateItem>): List<Any> {
        return items.mapNotNull { item ->
            val stored = item.path?.ifBlank { null }?.let { StoredImageResult(it, item.motionVideoPath) }
                ?: item.uri?.let { uri -> storeImageToInternal(uri) }
                ?: item.base64?.let { base64 -> storeBase64ImageToInternal(base64) }?.let { StoredImageResult(it) }
            val imagePath = stored?.imagePath ?: return@mapNotNull null
            val motionVideoPath = stored.motionVideoPath
                ?: item.motionVideoPath?.ifBlank { null }
                ?: item.motionVideoUri?.let { uri -> storeVideoToInternal(uri, deleteSource = false) }
            if (motionVideoPath != null) {
                org.json.JSONObject()
                    .put("image", imagePath)
                    .put("motionVideo", motionVideoPath)
            } else {
                imagePath
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
                ResourceType.IMAGE -> obj.put("images", org.json.JSONArray(resolveImageItems(item.images)))
                ResourceType.VIDEO -> obj.put("videos", org.json.JSONArray(resolveVideoItems(item.videos)))
                ResourceType.SOUND -> obj.put("text", item.text.orEmpty())
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

    fun decodeUriToBitmap(uri: Uri): Bitmap? {
        return runCatching {
            val resolver = getApplication<Application>().contentResolver
            resolver.openInputStream(uri)?.use { input ->
                BitmapFactory.decodeStream(input)
            }
        }.getOrNull()
    }

    fun decodeVideoFrame(uri: Uri): Bitmap? {
        val app = getApplication<Application>()
        return runCatching {
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(app, uri)
                retriever.getFrameAtTime(0)
            } finally {
                retriever.release()
            }
        }.getOrNull()
    }

    fun listStoredMedia(): List<StoredMediaItem> {
        val app = getApplication<Application>()
        val images = File(app.filesDir, "images")
            .listFiles()
            ?.sortedByDescending { it.lastModified() }
            ?.map { StoredMediaItem(path = Uri.fromFile(it).toString(), type = ResourceType.IMAGE) }
            .orEmpty()
        val videos = File(app.filesDir, "videos")
            .listFiles()
            ?.sortedByDescending { it.lastModified() }
            ?.map { StoredMediaItem(path = Uri.fromFile(it).toString(), type = ResourceType.VIDEO) }
            .orEmpty()
        val sounds = File(app.filesDir, "audio")
            .listFiles()
            ?.sortedByDescending { it.lastModified() }
            ?.map { StoredMediaItem(path = Uri.fromFile(it).toString(), type = ResourceType.SOUND) }
            .orEmpty()
        return images + videos + sounds
    }

    fun restoreMediaToDirectory(path: String, type: ResourceType, directoryUri: Uri): Boolean {
        val app = getApplication<Application>()
        val tree = androidx.documentfile.provider.DocumentFile.fromTreeUri(app, directoryUri) ?: return false
        val sourceUri = Uri.parse(path)
        val sourcePath = sourceUri.path ?: return false
        val sourceFile = File(sourcePath)
        if (!sourceFile.exists()) return false
        val mime = when (type) {
            ResourceType.IMAGE -> "image/*"
            ResourceType.VIDEO -> "video/*"
            ResourceType.SOUND -> "audio/*"
            else -> "*/*"
        }
        val target = tree.createFile(mime, sourceFile.name) ?: return false
        val output = app.contentResolver.openOutputStream(target.uri) ?: return false
        return runCatching {
            output.use { out ->
                sourceFile.inputStream().use { input -> input.copyTo(out) }
            }
            true
        }.getOrDefault(false)
    }

    fun deleteStoredMedia(item: StoredMediaItem): Boolean {
        val uri = Uri.parse(item.path)
        val path = uri.path ?: return false
        val target = File(path)
        return target.exists() && target.delete()
    }

    private fun storeBase64ImageToInternal(base64: String): String? {
        val bitmap = decodeBase64ToBitmap(base64) ?: return null
        return storeBitmapToInternal(bitmap)
    }

    private fun storeImageToInternal(uri: Uri): StoredImageResult? {
        val bitmap = ImageCompression.decodeToBitmap(getApplication(), uri) ?: return null
        val stored = storeBitmapToInternal(bitmap) ?: return null
        val motionVideoPath = queryMotionVideoUri(uri)?.let { motionUri ->
            storeVideoToInternal(motionUri, deleteSource = false)
        }
        deleteSourceUri(uri)
        return StoredImageResult(imagePath = stored, motionVideoPath = motionVideoPath)
    }

    private fun storeBitmapToInternal(bitmap: Bitmap): String? {
        return runCatching {
            val dir = File(getApplication<Application>().filesDir, "images").apply { mkdirs() }
            val target = File(dir, "media_${System.currentTimeMillis()}_${UUID.randomUUID()}.dat")
            FileOutputStream(target).use { output ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 85, output)
            }
            Uri.fromFile(target).toString()
        }.getOrNull()
    }

    private fun deleteSourceUri(uri: Uri) {
        val app = getApplication<Application>()
        val deleted = runCatching {
            val doc = DocumentFile.fromSingleUri(app, uri)
            doc?.delete() ?: false
        }.getOrDefault(false)
        if (!deleted) {
            runCatching { app.contentResolver.delete(uri, null, null) }
        }
    }

    private fun storeVideoToInternal(uri: Uri, deleteSource: Boolean = true): String? {
        return runCatching {
            val dir = File(getApplication<Application>().filesDir, "videos").apply { mkdirs() }
            val target = File(dir, "media_${System.currentTimeMillis()}_${UUID.randomUUID()}.dat")
            getApplication<Application>().contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(target).use { output ->
                    input.copyTo(output)
                }
            } ?: return null
            if (deleteSource) {
                deleteSourceUri(uri)
            }
            Uri.fromFile(target).toString()
        }.getOrNull()
    }

    private fun storeAudioToInternal(uri: Uri, deleteSource: Boolean = true): String? {
        return runCatching {
            val dir = File(getApplication<Application>().filesDir, "audio").apply { mkdirs() }
            val target = File(dir, "media_${System.currentTimeMillis()}_${UUID.randomUUID()}.dat")
            getApplication<Application>().contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(target).use { output ->
                    input.copyTo(output)
                }
            } ?: return null
            if (deleteSource) {
                deleteSourceUri(uri)
            }
            Uri.fromFile(target).toString()
        }.getOrNull()
    }

    private fun queryMotionVideoUri(uri: Uri): Uri? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return null
        val resolver = getApplication<Application>().contentResolver
        val projection = arrayOf(
            COLUMN_IS_MOTION_PHOTO,
            COLUMN_MOTION_PHOTO_ASSOCIATED_VIDEO
        )
        return resolver.query(uri, projection, null, null, null)?.use { cursor ->
            if (!cursor.moveToFirst()) return@use null
            val isMotionIndex = cursor.getColumnIndex(COLUMN_IS_MOTION_PHOTO)
            val videoIndex = cursor.getColumnIndex(COLUMN_MOTION_PHOTO_ASSOCIATED_VIDEO)
            val isMotion = isMotionIndex >= 0 && cursor.getInt(isMotionIndex) == 1
            val videoUri = if (videoIndex >= 0) cursor.getString(videoIndex) else null
            if (isMotion && !videoUri.isNullOrBlank()) Uri.parse(videoUri) else null
        }
    }

    private companion object {
        const val COLUMN_IS_MOTION_PHOTO = "is_motion_photo"
        const val COLUMN_MOTION_PHOTO_ASSOCIATED_VIDEO = "motion_photo_associated_video"
    }
}
