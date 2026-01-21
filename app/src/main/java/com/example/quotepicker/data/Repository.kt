package com.example.quotepicker.data

import android.content.Context
import android.net.Uri
import android.util.Base64
import java.io.File
import org.json.JSONArray
import kotlinx.coroutines.flow.Flow

class Repository private constructor(context: Context) {
    private val appContext = context.applicationContext
    private val db = AppDatabase.get(context)
    private val categoryDao = db.tagCategoryDao()
    private val tagDao = db.tagDao()
    private val characterDao = db.characterDao()
    private val resourceDao = db.resourceDao()
    private val crossRefDao = db.crossRefDao()

    fun observeCategories(): Flow<List<TagCategoryEntity>> = categoryDao.observeCategories()
    fun observeTagsByCategory(categoryId: Long): Flow<List<TagEntity>> = tagDao.observeTagsByCategory(categoryId)
    fun observeCategoriesByType(type: TagCategoryType): Flow<List<TagCategoryEntity>> =
        categoryDao.observeCategoriesByType(type)
    fun observeAllTags(): Flow<List<TagEntity>> = tagDao.observeAllTags()
    fun observeCharacters(): Flow<List<CharacterEntity>> = characterDao.observeCharacters()
    fun observeCharactersWithTags(): Flow<List<CharacterWithTags>> = characterDao.observeCharactersWithTags()
    fun observeResources(): Flow<List<ResourceEntity>> = resourceDao.observeResources()
    fun observeResourcesWithRelations(): Flow<List<ResourceWithTagsCharacters>> = resourceDao.observeResourcesWithRelations()

    suspend fun exportSnapshot(): BackupSnapshot {
        val resources = resourceDao.listAll()
        val mediaItems = collectMediaPaths(resources).mapNotNull { (path, type) ->
            readMedia(path)?.let { bytes ->
                MediaBackupItem(
                    originalPath = path,
                    type = type,
                    base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
                )
            }
        }
        return BackupSnapshot(
            categories = categoryDao.listAll(),
            tags = tagDao.listAll(),
            characters = characterDao.listAll(),
            resources = resources,
            resourceTagRefs = crossRefDao.listResourceTags(),
            characterTagRefs = crossRefDao.listCharacterTags(),
            resourceCharacterRefs = crossRefDao.listResourceCharacters(),
            media = mediaItems
        )
    }

    suspend fun replaceSnapshot(snapshot: BackupSnapshot) {
        val mediaMapping = restoreMedia(snapshot.media)
        val restoredResources = snapshot.resources.map { resource ->
            when (resource.type) {
                ResourceType.IMAGE,
                ResourceType.VIDEO -> resource.copy(
                    contentUriOrPath = replacePathsInPayload(resource.contentUriOrPath, mediaMapping)
                )
                ResourceType.FLOW -> resource.copy(
                    sceneJson = replacePathsInSceneJson(resource.sceneJson, mediaMapping)
                )
                else -> resource
            }
        }
        crossRefDao.deleteAllResourceTags()
        crossRefDao.deleteAllCharacterTags()
        crossRefDao.deleteAllResourceCharacters()
        resourceDao.deleteAll()
        characterDao.deleteAll()
        tagDao.deleteAll()
        categoryDao.deleteAll()
        if (snapshot.categories.isNotEmpty()) categoryDao.insertAll(snapshot.categories)
        if (snapshot.tags.isNotEmpty()) tagDao.insertAll(snapshot.tags)
        if (snapshot.characters.isNotEmpty()) characterDao.insertAll(snapshot.characters)
        if (restoredResources.isNotEmpty()) resourceDao.insertAll(restoredResources)
        if (snapshot.resourceTagRefs.isNotEmpty()) crossRefDao.insertResourceTags(snapshot.resourceTagRefs)
        if (snapshot.characterTagRefs.isNotEmpty()) crossRefDao.insertCharacterTags(snapshot.characterTagRefs)
        if (snapshot.resourceCharacterRefs.isNotEmpty()) crossRefDao.insertResourceCharacters(snapshot.resourceCharacterRefs)
    }

    suspend fun addCategory(name: String, type: TagCategoryType) =
        categoryDao.insert(TagCategoryEntity(name = name, type = type))

    suspend fun updateCategory(category: TagCategoryEntity) =
        categoryDao.update(category.copy(updatedAt = System.currentTimeMillis()))
    suspend fun deleteCategory(category: TagCategoryEntity) {
        if (category.name == ORPHAN_CATEGORY_NAME) return
        val orphan = ensureOrphanCategory(category.type)
        tagDao.reassignCategory(category.id, orphan.id)
        categoryDao.delete(category)
    }

    suspend fun ensureOrphanCategory(type: TagCategoryType): TagCategoryEntity {
        val existing = categoryDao.findByName(ORPHAN_CATEGORY_NAME, type)
        if (existing != null) return existing
        val id = categoryDao.insert(TagCategoryEntity(name = ORPHAN_CATEGORY_NAME, type = type))
        return TagCategoryEntity(id = id, name = ORPHAN_CATEGORY_NAME, type = type)
    }

    suspend fun addTag(categoryId: Long, name: String, colorArgb: Int) =
        tagDao.insert(TagEntity(categoryId = categoryId, name = name, colorArgb = colorArgb))

    suspend fun updateTag(tag: TagEntity) = tagDao.update(tag.copy(updatedAt = System.currentTimeMillis()))
    suspend fun deleteTag(tag: TagEntity) = tagDao.delete(tag)

    suspend fun addCharacter(name: String) = characterDao.insert(CharacterEntity(name = name))
    suspend fun updateCharacter(character: CharacterEntity) =
        characterDao.update(character.copy(updatedAt = System.currentTimeMillis()))

    suspend fun deleteCharacter(character: CharacterEntity) = characterDao.delete(character)

    suspend fun updateCharacterTags(characterId: Long, tagIds: List<Long>) {
        crossRefDao.clearCharacterTags(characterId)
        if (tagIds.isNotEmpty()) {
            crossRefDao.insertCharacterTags(tagIds.distinct().map { CharacterTagCrossRef(characterId, it) })
        }
    }

    suspend fun addResource(resource: ResourceEntity, tagIds: List<Long>, characterIds: List<Long>): Long {
        val id = resourceDao.insert(resource)
        updateResourceTags(id, tagIds)
        updateResourceCharacters(id, characterIds)
        return id
    }

    suspend fun updateResource(resource: ResourceEntity) =
        resourceDao.update(resource.copy(updatedAt = System.currentTimeMillis()))

    suspend fun deleteResource(resource: ResourceEntity) = resourceDao.delete(resource)

    suspend fun updateResourceTags(resourceId: Long, tagIds: List<Long>) {
        crossRefDao.clearResourceTags(resourceId)
        if (tagIds.isNotEmpty()) {
            crossRefDao.insertResourceTags(tagIds.distinct().map { ResourceTagCrossRef(resourceId, it) })
        }
    }

    suspend fun updateResourceCharacters(resourceId: Long, characterIds: List<Long>) {
        crossRefDao.clearResourceCharacters(resourceId)
        if (characterIds.isNotEmpty()) {
            crossRefDao.insertResourceCharacters(characterIds.distinct().map { ResourceCharacterCrossRef(resourceId, it) })
        }
    }

    suspend fun resourceIdsForCharacter(characterId: Long) = crossRefDao.resourceIdsForCharacter(characterId)
    suspend fun resourceIdsForTags(tagIds: List<Long>) = crossRefDao.resourceIdsForTags(tagIds)

    private fun collectMediaPaths(resources: List<ResourceEntity>): List<Pair<String, ResourceType>> {
        val unique = linkedSetOf<Pair<String, ResourceType>>()
        resources.forEach { resource ->
            when (resource.type) {
                ResourceType.IMAGE -> {
                    parseImageMediaPaths(resource.contentUriOrPath).forEach { (path, type) ->
                        unique.add(path to type)
                    }
                }
                ResourceType.VIDEO -> {
                    parseMediaPaths(resource.contentUriOrPath).forEach { path ->
                        unique.add(path to resource.type)
                    }
                }
                ResourceType.FLOW -> {
                    parseFlowMediaPaths(resource.sceneJson).forEach { (path, type) ->
                        unique.add(path to type)
                    }
                }
                else -> Unit
            }
        }
        return unique.toList()
    }

    private fun parseMediaPaths(payload: String?): List<String> {
        if (payload.isNullOrBlank()) return emptyList()
        val trimmed = payload.trim()
        if (trimmed.startsWith("[")) {
            return runCatching {
                val array = JSONArray(trimmed)
                List(array.length()) { index -> array.getString(index) }
            }.getOrDefault(emptyList())
        }
        return listOf(trimmed)
    }

    private fun parseImageMediaPaths(payload: String?): List<Pair<String, ResourceType>> {
        if (payload.isNullOrBlank()) return emptyList()
        val trimmed = payload.trim()
        return if (trimmed.startsWith("[")) {
            runCatching {
                val array = JSONArray(trimmed)
                buildList {
                    for (i in 0 until array.length()) {
                        when (val entry = array.get(i)) {
                            is org.json.JSONObject -> {
                                val image = entry.optString("image")
                                val motionVideo = entry.optString("motionVideo")
                                if (image.isNotBlank()) add(image to ResourceType.IMAGE)
                                if (motionVideo.isNotBlank()) add(motionVideo to ResourceType.VIDEO)
                            }
                            else -> {
                                val item = entry.toString()
                                if (item.isNotBlank()) add(item to ResourceType.IMAGE)
                            }
                        }
                    }
                }
            }.getOrDefault(emptyList())
        } else if (trimmed.startsWith("{")) {
            runCatching {
                val obj = org.json.JSONObject(trimmed)
                val image = obj.optString("image")
                val motionVideo = obj.optString("motionVideo")
                buildList {
                    if (image.isNotBlank()) add(image to ResourceType.IMAGE)
                    if (motionVideo.isNotBlank()) add(motionVideo to ResourceType.VIDEO)
                }
            }.getOrDefault(emptyList())
        } else {
            listOf(trimmed to ResourceType.IMAGE)
        }
    }

    private fun parseFlowMediaPaths(payload: String?): List<Pair<String, ResourceType>> {
        if (payload.isNullOrBlank()) return emptyList()
        return runCatching {
            val array = JSONArray(payload)
            buildList {
                for (i in 0 until array.length()) {
                    val obj = array.optJSONObject(i) ?: continue
                    val type = runCatching {
                        ResourceType.valueOf(obj.getString("type"))
                    }.getOrNull() ?: continue
                    when (type) {
                        ResourceType.IMAGE -> {
                            val images = obj.optJSONArray("images") ?: JSONArray()
                            for (j in 0 until images.length()) {
                                when (val entry = images.get(j)) {
                                    is org.json.JSONObject -> {
                                        val image = entry.optString("image")
                                        val motionVideo = entry.optString("motionVideo")
                                        if (image.isNotBlank()) add(image to ResourceType.IMAGE)
                                        if (motionVideo.isNotBlank()) add(motionVideo to ResourceType.VIDEO)
                                    }
                                    else -> {
                                        val item = entry.toString()
                                        if (item.isNotBlank()) add(item to type)
                                    }
                                }
                            }
                        }
                        ResourceType.VIDEO -> {
                            val videos = obj.optJSONArray("videos") ?: JSONArray()
                            for (j in 0 until videos.length()) {
                                add(videos.getString(j) to type)
                            }
                        }
                        else -> Unit
                    }
                }
            }
        }.getOrDefault(emptyList())
    }

    private fun readMedia(path: String): ByteArray? {
        val uri = Uri.parse(path)
        return runCatching {
            if (uri.scheme == "file") {
                val filePath = uri.path ?: return@runCatching null
                File(filePath).takeIf { it.exists() }?.readBytes()
            } else {
                appContext.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            }
        }.getOrNull()
    }

    private fun restoreMedia(items: List<MediaBackupItem>): Map<String, String> {
        if (items.isEmpty()) return emptyMap()
        val mapping = mutableMapOf<String, String>()
        items.forEach { item ->
            val bytes = runCatching { Base64.decode(item.base64, Base64.DEFAULT) }.getOrNull() ?: return@forEach
            val folder = when (item.type) {
                ResourceType.IMAGE -> "images"
                ResourceType.VIDEO -> "videos"
                else -> "media"
            }
            val dir = File(appContext.filesDir, folder).apply { mkdirs() }
            val target = File(dir, "media_import_${System.currentTimeMillis()}_${item.originalPath.hashCode()}.dat")
            runCatching {
                target.writeBytes(bytes)
                mapping[item.originalPath] = Uri.fromFile(target).toString()
            }
        }
        return mapping
    }

    private fun replacePathsInPayload(payload: String?, mapping: Map<String, String>): String? {
        if (payload.isNullOrBlank() || mapping.isEmpty()) return payload
        val trimmed = payload.trim()
        if (trimmed.startsWith("[")) {
            return runCatching {
                val array = JSONArray(trimmed)
                val updated = JSONArray()
                for (i in 0 until array.length()) {
                    when (val entry = array.get(i)) {
                        is org.json.JSONObject -> {
                            val image = entry.optString("image")
                            val motionVideo = entry.optString("motionVideo")
                            if (image.isNotBlank()) {
                                entry.put("image", mapping[image] ?: image)
                            }
                            if (motionVideo.isNotBlank()) {
                                entry.put("motionVideo", mapping[motionVideo] ?: motionVideo)
                            }
                            updated.put(entry)
                        }
                        else -> {
                            val raw = entry.toString()
                            updated.put(mapping[raw] ?: raw)
                        }
                    }
                }
                updated.toString()
            }.getOrDefault(payload)
        }
        if (trimmed.startsWith("{")) {
            return runCatching {
                val obj = org.json.JSONObject(trimmed)
                val image = obj.optString("image")
                val motionVideo = obj.optString("motionVideo")
                if (image.isNotBlank()) obj.put("image", mapping[image] ?: image)
                if (motionVideo.isNotBlank()) obj.put("motionVideo", mapping[motionVideo] ?: motionVideo)
                obj.toString()
            }.getOrDefault(payload)
        }
        return mapping[trimmed] ?: payload
    }

    private fun replacePathsInSceneJson(payload: String?, mapping: Map<String, String>): String? {
        if (payload.isNullOrBlank() || mapping.isEmpty()) return payload
        return runCatching {
            val array = JSONArray(payload)
            for (i in 0 until array.length()) {
                val obj = array.optJSONObject(i) ?: continue
                listOf("images", "videos").forEach { key ->
                    val items = obj.optJSONArray(key) ?: return@forEach
                    val updated = JSONArray()
                    for (j in 0 until items.length()) {
                        when (val entry = items.get(j)) {
                            is org.json.JSONObject -> {
                                val image = entry.optString("image")
                                val motionVideo = entry.optString("motionVideo")
                                if (image.isNotBlank()) {
                                    entry.put("image", mapping[image] ?: image)
                                }
                                if (motionVideo.isNotBlank()) {
                                    entry.put("motionVideo", mapping[motionVideo] ?: motionVideo)
                                }
                                updated.put(entry)
                            }
                            else -> {
                                val raw = entry.toString()
                                updated.put(mapping[raw] ?: raw)
                            }
                        }
                    }
                    obj.put(key, updated)
                }
            }
            array.toString()
        }.getOrDefault(payload)
    }

    companion object {
        const val ORPHAN_CATEGORY_NAME = "未分类"
        @Volatile private var INSTANCE: Repository? = null
        fun get(context: Context): Repository =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Repository(context.applicationContext).also { INSTANCE = it }
            }
    }
}
