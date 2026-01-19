package com.example.quotepicker.vm

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.security.KeyStoreException
import android.util.Base64
import android.util.Log
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
import java.io.File
import javax.crypto.AEADBadTagException
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

    val allResources: StateFlow<List<ResourceWithTagsCharacters>> = repo.observeResourcesWithRelations()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val uiState: StateFlow<ResourceUiState> = combine(
        allResources,
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

    fun addImageQuote(title: String, imageUris: List<Uri>, tagIds: List<Long>, characterIds: List<Long>) =
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

    fun addEncryptedMedia(
        type: ResourceType,
        title: String,
        uri: Uri,
        tagIds: List<Long>,
        characterIds: List<Long>
    ) = viewModelScope.launch(Dispatchers.IO) {
        if (characterIds.isEmpty()) return@launch
        val targetName = "${type.name.lowercase()}_${System.currentTimeMillis()}.enc"
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
            val targetName = "${type.name.lowercase()}_${System.currentTimeMillis()}_$index.enc"
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

    fun updateResourceTags(resourceId: Long, tagIds: List<Long>) =
        viewModelScope.launch { repo.updateResourceTags(resourceId, tagIds) }

    fun updateResourceCharacters(resourceId: Long, characterIds: List<Long>) =
        viewModelScope.launch { repo.updateResourceCharacters(resourceId, characterIds) }

    fun deleteResource(resource: ResourceEntity) = viewModelScope.launch(Dispatchers.IO) {
        fileManager.deleteEncryptedFile(resource.contentUriOrPath)
        repo.deleteResource(resource)
    }

    suspend fun loadDecryptedBytes(path: String): ByteArray = withContext(Dispatchers.IO) {
        runCatching {
            fileManager.openDecryptedStream(path).use { it.readBytes() }
        }
            .onFailure { error -> handleDecryptionFailure(path, error) }
            .getOrDefault(ByteArray(0))
    }

    suspend fun writeDecryptedToCache(path: String, extension: String? = null): File? = withContext(Dispatchers.IO) {
        val cacheDir = getApplication<Application>().cacheDir
        val normalizedExtension = extension?.let { if (it.startsWith(".")) it else ".$it" } ?: ".media"
        val temp = File(cacheDir, "preview_${System.currentTimeMillis()}$normalizedExtension")
        runCatching {
            fileManager.openDecryptedStream(path).use { input ->
                temp.outputStream().use { output -> input.copyTo(output) }
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

    private fun handleDecryptionFailure(path: String, error: Throwable) {
        if (!isDecryptionFailure(error)) return
        Log.w("ResourceViewModel", "Deleting corrupted encrypted media path=$path", error)
        fileManager.deleteEncryptedFile(path)
    }

    private fun isDecryptionFailure(error: Throwable): Boolean {
        return generateSequence(error) { it.cause }
            .any { cause -> cause is AEADBadTagException || cause is KeyStoreException }
    }

    fun decodeBase64ToBitmap(b64: String): Bitmap? {
        return runCatching {
            val bytes = Base64.decode(b64, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        }.getOrNull()
    }
}
