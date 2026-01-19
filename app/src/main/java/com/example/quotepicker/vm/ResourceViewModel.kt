package com.example.quotepicker.vm

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.security.KeyStoreException
import android.util.Base64
import android.util.Log
import android.webkit.MimeTypeMap
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
import com.example.quotepicker.util.EncryptedFileManager
import com.example.quotepicker.util.ImageCompression
import java.io.File
import javax.crypto.AEADBadTagException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

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
    val text: String? = null,
    val sceneMessages: List<SceneMessageDraft> = emptyList(),
    val images: List<ImageUpdateItem> = emptyList(),
    val videos: List<VideoUpdateItem> = emptyList()
)

class ResourceViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = Repository.get(app)
    private val fileManager = EncryptedFileManager(app)

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
            val encryptedPaths = encryptMediaGroup(ResourceType.VIDEO, uris)
            if (encryptedPaths.isEmpty()) return@launch
            val payload = org.json.JSONArray(encryptedPaths).toString()
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
        val payload = buildFlowPayloadWithVideos(items).first
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

    fun addEncryptedMedia(
        type: ResourceType,
        title: String,
        uri: Uri,
        tagIds: List<Long>,
        characterIds: List<Long>
    ) = viewModelScope.launch(Dispatchers.IO) {
        if (characterIds.isEmpty()) return@launch
        val extension = resolveMediaExtension(uri)
        val targetName = buildEncryptedName(type, extension)
        val resolver = getApplication<Application>().contentResolver
        val input = runCatching { resolver.openInputStream(uri) }
            .onFailure { error ->
                Log.e("ResourceViewModel", "Failed to open media uri=$uri type=$type", error)
            }
            .getOrNull()
            ?: return@launch
        val encryptedFile = runCatching { input.use { fileManager.encryptToFile(it, targetName) } }
            .onFailure { error ->
                Log.e("ResourceViewModel", "Failed to encrypt media uri=$uri type=$type", error)
            }
            .getOrNull()
            ?: return@launch
        Log.d(
            "ResourceViewModel",
            "Encrypted media saved type=$type path=${encryptedFile.absolutePath} size=${encryptedFile.length()}"
        )
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

    fun addEncryptedMediaGroup(
        type: ResourceType,
        title: String,
        uris: List<Uri>,
        tagIds: List<Long>,
        characterIds: List<Long>
    ) = viewModelScope.launch(Dispatchers.IO) {
        if (characterIds.isEmpty()) return@launch
        if (uris.isEmpty()) return@launch
        val resolver = getApplication<Application>().contentResolver
        val encryptedPaths = mutableListOf<String>()
        uris.forEachIndexed { index, uri ->
            val extension = resolveMediaExtension(uri)
            val targetName = buildEncryptedName(type, extension, index)
            val input = runCatching { resolver.openInputStream(uri) }
                .onFailure { error ->
                    Log.e("ResourceViewModel", "Failed to open media uri=$uri type=$type", error)
                }
                .getOrNull()
            if (input == null) {
                Log.w("ResourceViewModel", "Skip media uri=$uri type=$type due to open failure")
                return@forEachIndexed
            }
            val encryptedFile = runCatching { input.use { fileManager.encryptToFile(it, targetName) } }
                .onFailure { error ->
                    Log.e("ResourceViewModel", "Failed to encrypt media uri=$uri type=$type", error)
                }
                .getOrNull()
            if (encryptedFile != null) {
                encryptedPaths.add(encryptedFile.absolutePath)
                Log.d(
                    "ResourceViewModel",
                    "Encrypted media saved type=$type path=${encryptedFile.absolutePath} size=${encryptedFile.length()}"
                )
            } else {
                Log.w("ResourceViewModel", "Skip media uri=$uri type=$type due to encryption failure")
            }
        }
        if (encryptedPaths.isEmpty()) {
            Log.e("ResourceViewModel", "No media saved for group type=$type title=$title")
            return@launch
        }
        val payload = org.json.JSONArray(encryptedPaths).toString()
        repo.addResource(
            ResourceEntity(
                type = type,
                title = title,
                contentUriOrPath = payload
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
        val oldPaths = parseMediaPaths(resource.contentUriOrPath)
        val newPaths = resolveVideoItems(items)
        if (newPaths.isEmpty()) return@launch
        val payload = org.json.JSONArray(newPaths).toString()
        repo.updateResource(resource.copy(title = title, contentUriOrPath = payload))
        deleteEncryptedPaths(oldPaths.filterNot { it in newPaths })
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
        val (payload, newPaths) = buildFlowPayloadWithVideos(items)
        val oldPaths = extractFlowVideoPaths(resource.sceneJson)
        repo.updateResource(resource.copy(title = title, sceneJson = payload))
        deleteEncryptedPaths(oldPaths.filterNot { it in newPaths })
        updateResourceTags(resource.id, tagIds)
        updateResourceCharacters(resource.id, characterIds)
    }

    fun updateResourceTags(resourceId: Long, tagIds: List<Long>) =
        viewModelScope.launch { repo.updateResourceTags(resourceId, tagIds) }

    fun updateResourceCharacters(resourceId: Long, characterIds: List<Long>) =
        viewModelScope.launch { repo.updateResourceCharacters(resourceId, characterIds) }

    fun deleteResource(resource: ResourceEntity) = viewModelScope.launch(Dispatchers.IO) {
        val paths = when (resource.type) {
            ResourceType.VIDEO -> parseMediaPaths(resource.contentUriOrPath)
            ResourceType.FLOW -> extractFlowVideoPaths(resource.sceneJson)
            else -> emptyList()
        }
        deleteEncryptedPaths(paths)
        repo.deleteResource(resource)
    }

    suspend fun loadDecryptedBytes(path: String): ByteArray = withContext(Dispatchers.IO) {
        runCatching {
            fileManager.openDecryptedStream(path).use { it.readBytes() }
        }
            .onFailure { error -> handleDecryptionFailure(path, error) }
            .getOrDefault(ByteArray(0))
    }

    suspend fun writeDecryptedToCache(
        path: String,
        extension: String? = null,
        timeoutMs: Long = 60_000
    ): File? = withContext(Dispatchers.IO) {
        val cacheDir = getApplication<Application>().cacheDir
        val normalizedExtension = extension?.let { if (it.startsWith(".")) it else ".$it" } ?: ".media"
        val temp = File(cacheDir, "preview_${System.currentTimeMillis()}$normalizedExtension")
        val result = withTimeoutOrNull(timeoutMs) {
            runCatching {
                fileManager.openDecryptedStream(path).use { input ->
                    temp.outputStream().use { output ->
                        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                        while (true) {
                            ensureActive()
                            val read = input.read(buffer)
                            if (read <= 0) break
                            output.write(buffer, 0, read)
                        }
                        output.flush()
                    }
                }
                temp
            }
                .onSuccess {
                    Log.d(
                        "ResourceViewModel",
                        "Decrypted media cached path=$path temp=${temp.absolutePath} size=${temp.length()}"
                    )
                }
                .onFailure { error ->
                    handleDecryptionFailure(path, error)
                    runCatching { if (temp.exists()) temp.delete() }
                    Log.e("ResourceViewModel", "Failed to decrypt media path=$path", error)
                }
                .getOrNull()
        }
        if (result == null) {
            Log.e("ResourceViewModel", "Decrypt media timeout path=$path after ${timeoutMs}ms")
            runCatching { if (temp.exists()) temp.delete() }
        }
        result
    }

    private fun deleteEncryptedPaths(paths: List<String>) {
        paths.forEach { fileManager.deleteEncryptedFile(it) }
    }

    private fun parseMediaPaths(raw: String?): List<String> {
        if (raw.isNullOrBlank()) return emptyList()
        val trimmed = raw.trim()
        if (trimmed.startsWith("[")) {
            return runCatching {
                val arr = org.json.JSONArray(trimmed)
                List(arr.length()) { index -> arr.getString(index) }
            }.getOrDefault(listOf(raw))
        }
        return listOf(raw)
    }

    private fun extractFlowVideoPaths(flowJson: String?): List<String> {
        if (flowJson.isNullOrBlank()) return emptyList()
        return runCatching {
            val array = org.json.JSONArray(flowJson)
            buildList {
                for (i in 0 until array.length()) {
                    val obj = array.optJSONObject(i) ?: continue
                    if (obj.optString("type") != ResourceType.VIDEO.name) continue
                    val videos = obj.optJSONArray("videos") ?: continue
                    for (j in 0 until videos.length()) {
                        val path = videos.optString(j)
                        if (path.isNotBlank()) add(path)
                    }
                }
            }
        }.getOrDefault(emptyList())
    }

    private suspend fun resolveVideoItems(items: List<VideoUpdateItem>): List<String> {
        val resolver = getApplication<Application>().contentResolver
        val result = mutableListOf<String>()
        items.forEachIndexed { index, item ->
            item.path?.let {
                result.add(it)
                return@forEachIndexed
            }
            val uri = item.uri ?: return@forEachIndexed
            val extension = resolveMediaExtension(uri)
            val targetName = buildEncryptedName(ResourceType.VIDEO, extension, index)
            val input = runCatching { resolver.openInputStream(uri) }
                .getOrNull()
                ?: return@forEachIndexed
            val encryptedFile = runCatching { input.use { fileManager.encryptToFile(it, targetName) } }
                .getOrNull()
                ?: return@forEachIndexed
            result.add(encryptedFile.absolutePath)
        }
        return result
    }

    private suspend fun encryptMediaGroup(type: ResourceType, uris: List<Uri>): List<String> {
        if (uris.isEmpty()) return emptyList()
        val resolver = getApplication<Application>().contentResolver
        val encryptedPaths = mutableListOf<String>()
        uris.forEachIndexed { index, uri ->
            val extension = resolveMediaExtension(uri)
            val targetName = buildEncryptedName(type, extension, index)
            val input = runCatching { resolver.openInputStream(uri) }.getOrNull() ?: return@forEachIndexed
            val encryptedFile = runCatching { input.use { fileManager.encryptToFile(it, targetName) } }
                .getOrNull()
                ?: return@forEachIndexed
            encryptedPaths.add(encryptedFile.absolutePath)
        }
        return encryptedPaths
    }

    private suspend fun encodeImageItems(items: List<ImageUpdateItem>): List<String> {
        return items.mapNotNull { item ->
            item.base64 ?: item.uri?.let { uri ->
                ImageCompression.encodeToBase64(getApplication(), uri).ifBlank { null }
            }
        }
    }

    private suspend fun buildFlowPayloadWithVideos(items: List<FlowUpdateItem>): Pair<String, List<String>> {
        val array = org.json.JSONArray()
        val videoPaths = mutableListOf<String>()
        items.forEach { item ->
            val obj = org.json.JSONObject()
            obj.put("type", item.type.name)
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
                ResourceType.VIDEO -> {
                    val paths = resolveVideoItems(item.videos)
                    videoPaths.addAll(paths)
                    obj.put("videos", org.json.JSONArray(paths))
                }
                ResourceType.FLOW -> obj.put("text", item.text.orEmpty())
            }
            array.put(obj)
        }
        return array.toString() to videoPaths
    }

    private fun handleDecryptionFailure(path: String, error: Throwable) {
        if (!isDecryptionFailure(error)) return
        Log.w("ResourceViewModel", "Deleting corrupted encrypted media path=$path", error)
        fileManager.deleteEncryptedFile(path)
    }

    private fun isDecryptionFailure(error: Throwable): Boolean {
        return generateSequence(error) { it.cause }
            .any { cause -> cause is AEADBadTagException || cause is KeyStoreException }
    }

    private fun buildEncryptedName(type: ResourceType, extension: String?, index: Int? = null): String {
        val suffix = index?.let { "_$it" }.orEmpty()
        val ext = extension?.let { ".$it" }.orEmpty()
        return "${type.name.lowercase()}_${System.currentTimeMillis()}$suffix$ext.enc"
    }

    private fun resolveMediaExtension(uri: Uri): String? {
        val resolver = getApplication<Application>().contentResolver
        val mime = resolver.getType(uri)
        return mime?.let { MimeTypeMap.getSingleton().getExtensionFromMimeType(it) }
    }

    fun decodeBase64ToBitmap(b64: String): Bitmap? {
        return runCatching {
            val bytes = Base64.decode(b64, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        }.getOrNull()
    }
}
