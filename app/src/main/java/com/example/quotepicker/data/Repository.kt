package com.example.quotepicker.data

import android.content.Context
import android.net.Uri
import android.util.Base64
import androidx.room.withTransaction
import java.io.File
import java.io.InputStream
import java.time.LocalDate
import org.json.JSONArray
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class Repository private constructor(context: Context) {
    private val appContext = context.applicationContext
    private val db = AppDatabase.get(context)
    private val categoryDao = db.tagCategoryDao()
    private val tagDao = db.tagDao()
    private val characterDao = db.characterDao()
    private val resourceDao = db.resourceDao()
    private val crossRefDao = db.crossRefDao()
    private val responseRecordDao = db.responseRecordDao()
    private val executionSettingsDao = db.executionSettingsDao()
    private val executionResourceDao = db.executionResourceDao()

    fun observeCategories(): Flow<List<TagCategoryEntity>> = categoryDao.observeCategories()
    fun observeTagsByCategory(categoryId: Long): Flow<List<TagEntity>> = tagDao.observeTagsByCategory(categoryId)
    fun observeCategoriesByType(type: TagCategoryType): Flow<List<TagCategoryEntity>> =
        categoryDao.observeCategoriesByType(type)
    fun observeAllTags(): Flow<List<TagEntity>> = tagDao.observeAllTags()
    fun observeCharacters(): Flow<List<CharacterEntity>> = characterDao.observeCharacters()
    fun observeCharactersWithTags(): Flow<List<CharacterWithTags>> = characterDao.observeCharactersWithTags()
    fun observeResources(): Flow<List<ResourceEntity>> = resourceDao.observeResources()
    fun observeResourcesWithRelations(): Flow<List<ResourceWithTagsCharacters>> = resourceDao.observeResourcesWithRelations()
    fun observeResponseRecords(): Flow<List<ResponseRecordEntity>> = responseRecordDao.observeRecords()
    fun observeUsedTagIds(): Flow<Set<Long>> = combine(
        crossRefDao.observeUsedResourceTagIds(),
        crossRefDao.observeUsedCharacterTagIds(),
        responseRecordDao.observeUsedTagIds()
    ) { resourceTagIds, characterTagIds, responseTagIds ->
        (resourceTagIds + characterTagIds + responseTagIds).toSet()
    }
    fun observeExecutionSettings(): Flow<ExecutionSettingsEntity?> = executionSettingsDao.observeSettings()
    fun observeExecutionResources(): Flow<List<ExecutionResourceEntity>> = executionResourceDao.observeItems()

    data class ExportPackage(
        val snapshot: BackupSnapshot,
        val mediaSources: List<MediaExportSource>
    )

    suspend fun exportSnapshot(): BackupSnapshot = exportSnapshotPackage().snapshot

    suspend fun exportSnapshotPackage(): ExportPackage {
        val resources = resourceDao.listAll()
        val mediaSources = mutableListOf<MediaExportSource>()
        val mediaItems = collectMediaPaths(resources).mapNotNull { (path, type) ->
            val stream = openMediaStream(path) ?: return@mapNotNull null
            stream.close()
            val fileName = "media_${mediaSources.size}_${path.hashCode()}.dat"
            mediaSources.add(MediaExportSource(fileName = fileName, originalPath = path, type = type))
            MediaBackupItem(originalPath = path, type = type, fileName = fileName)
        }
        val snapshot = BackupSnapshot(
            categories = categoryDao.listAll(),
            tags = tagDao.listAll(),
            characters = characterDao.listAll(),
            resources = resources,
            resourceTagRefs = crossRefDao.listResourceTags(),
            characterTagRefs = crossRefDao.listCharacterTags(),
            resourceCharacterRefs = crossRefDao.listResourceCharacters(),
            responseRecords = responseRecordDao.listAll(),
            executionSettings = executionSettingsDao.getSettings(),
            executionResources = executionResourceDao.listAll(),
            media = mediaItems
        )
        return ExportPackage(snapshot = snapshot, mediaSources = mediaSources)
    }

    suspend fun replaceSnapshot(snapshot: BackupSnapshot, mediaPayloads: Map<String, MediaPayload> = emptyMap()) {
        clearManagedMediaDirectories()
        val mediaMapping = restoreMedia(snapshot.media, mediaPayloads)
        val restoredResources = snapshot.resources.map { resource ->
            when (resource.type) {
                ResourceType.IMAGE,
                ResourceType.VIDEO,
                ResourceType.SOUND -> resource.copy(
                    contentUriOrPath = replacePathsInPayload(resource.contentUriOrPath, mediaMapping)
                )
                ResourceType.FLOW -> resource.copy(
                    sceneJson = replacePathsInSceneJson(resource.sceneJson, mediaMapping)
                )
                else -> resource
            }
        }
        responseRecordDao.deleteAll()
        executionSettingsDao.deleteAll()
        executionResourceDao.deleteAll()
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
        if (snapshot.responseRecords.isNotEmpty()) {
            snapshot.responseRecords.forEach { responseRecordDao.upsert(it) }
        }
        snapshot.executionSettings?.let { executionSettingsDao.upsert(it) }
        if (snapshot.executionResources.isNotEmpty()) {
            snapshot.executionResources.forEach { executionResourceDao.insert(it) }
        }
    }

    private fun clearManagedMediaDirectories() {
        listOf("images", "videos", "audio").forEach { folder ->
            val dir = File(appContext.filesDir, folder)
            dir.listFiles()?.forEach { file ->
                runCatching {
                    if (file.isDirectory) file.deleteRecursively() else file.delete()
                }
            }
        }
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

    suspend fun addCharacter(name: String) = characterDao.insert(
        CharacterEntity(
            name = name,
            points = 30,
            probability = (1..10).random(),
            probabilityDate = currentDateString()
        )
    )
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

    suspend fun ensureSeedResources(minCount: Int = 5) {
        if (minCount <= 0) return
        val existingCount = resourceDao.listAll().size
        if (existingCount >= minCount) return
        val now = System.currentTimeMillis()
        val seeds = listOf(
            ResourceEntity(
                type = ResourceType.TEXT,
                title = "新手引导",
                quoteText = "欢迎使用资源库，这是默认的示例文本资源。",
                createdAt = now,
                updatedAt = now
            ),
            ResourceEntity(
                type = ResourceType.TEXT,
                title = "每日提示",
                quoteText = "坚持记录灵感，资源会越积越多。",
                createdAt = now + 1,
                updatedAt = now + 1
            ),
            ResourceEntity(
                type = ResourceType.SCENE,
                title = "场景模板",
                quoteText = "可在此基础上继续编辑剧情。",
                sceneJson = "[]",
                createdAt = now + 2,
                updatedAt = now + 2
            ),
            ResourceEntity(
                type = ResourceType.TEXT,
                title = "执行口令",
                quoteText = "执行资源 5 条记录入库完成。",
                createdAt = now + 3,
                updatedAt = now + 3
            ),
            ResourceEntity(
                type = ResourceType.TEXT,
                title = "持久保留",
                quoteText = "这些记录已保存在本地数据库中，可长期保留。",
                createdAt = now + 4,
                updatedAt = now + 4
            )
        )
        val toInsert = seeds.take(minCount - existingCount)
        if (toInsert.isNotEmpty()) {
            resourceDao.insertAll(toInsert)
        }
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


    suspend fun addExecutionResource(
        resourceId: Long,
        characterId: Long,
        tagId: Long,
        characterName: String,
        tagName: String
    ) {
        executionResourceDao.insert(
            ExecutionResourceEntity(
                resourceId = resourceId,
                characterId = characterId,
                tagId = tagId,
                characterName = characterName,
                tagName = tagName
            )
        )
    }

    suspend fun removeExecutionResource(id: Long) {
        executionResourceDao.deleteById(id)
    }

    suspend fun clearExecutionResources() {
        executionResourceDao.deleteAll()
    }

    suspend fun updateCharacterPoints(characterId: Long, points: Int) {
        val characters = characterDao.listAll()
        val target = characters.firstOrNull { it.id == characterId } ?: return
        characterDao.update(target.copy(points = points.coerceIn(0, 30), updatedAt = System.currentTimeMillis()))
    }

    suspend fun applyExecutionCompletion(characterId: Long, completionScoreSum: Int) {
        db.withTransaction {
            val characters = characterDao.listAll()
            val target = characters.firstOrNull { it.id == characterId } ?: return@withTransaction
            val nextPoints = ((target.points + completionScoreSum) / 2.0).roundToInt()
            characterDao.update(
                target.copy(
                    points = nextPoints.coerceIn(0, 30),
                    familiarity = target.familiarity + 1,
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
    }

    suspend fun incrementCharacterFamiliarity(characterId: Long) {
        val characters = characterDao.listAll()
        val target = characters.firstOrNull { it.id == characterId } ?: return
        characterDao.update(target.copy(familiarity = target.familiarity + 1, updatedAt = System.currentTimeMillis()))
    }

    suspend fun updateCharacterFamiliarity(characterId: Long, familiarity: Int) {
        val characters = characterDao.listAll()
        val target = characters.firstOrNull { it.id == characterId } ?: return
        characterDao.update(target.copy(familiarity = familiarity.coerceAtLeast(0), updatedAt = System.currentTimeMillis()))
    }

    suspend fun updateCharacterProbability(characterId: Long, probability: Int, date: String) {
        val characters = characterDao.listAll()
        val target = characters.firstOrNull { it.id == characterId } ?: return
        characterDao.update(
            target.copy(
                probability = probability.coerceIn(1, 10),
                probabilityDate = date,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun refreshDailyProbabilities() {
        val today = currentDateString()
        val characters = characterDao.listAll()
        characters.forEach { character ->
            if (character.probabilityDate != today) {
                characterDao.update(
                    character.copy(
                        probability = (1..10).random(),
                        probabilityDate = today,
                        updatedAt = System.currentTimeMillis()
                    )
                )
            }
        }
    }

    suspend fun ensureExecutionSettings(): ExecutionSettingsEntity {
        val existing = executionSettingsDao.getSettings()
        if (existing != null) return existing
        val settings = ExecutionSettingsEntity()
        executionSettingsDao.upsert(settings)
        return settings
    }

    suspend fun updateExecutionSettings(settings: ExecutionSettingsEntity) {
        executionSettingsDao.upsert(settings.copy(updatedAt = System.currentTimeMillis()))
    }

    suspend fun addResponseRecord(characterId: Long, tagId: Long, count: Int = 1) {
        val existing = responseRecordDao.findRecord(characterId, tagId)
        val nextCount = (existing?.count ?: 0) + count
        responseRecordDao.upsert(
            ResponseRecordEntity(
                characterId = characterId,
                tagId = tagId,
                count = nextCount,
                createdAt = existing?.createdAt ?: System.currentTimeMillis()
            )
        )
    }

    suspend fun consumeResponseRecord(characterId: Long, tagId: Long) {
        val existing = responseRecordDao.findRecord(characterId, tagId) ?: return
        val nextCount = existing.count - 1
        if (nextCount <= 0) {
            responseRecordDao.deleteRecord(characterId, tagId)
        } else {
            responseRecordDao.upsert(existing.copy(count = nextCount))
        }
    }

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
                ResourceType.SOUND -> {
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

    private fun restoreMedia(
        items: List<MediaBackupItem>,
        mediaPayloads: Map<String, MediaPayload>
    ): Map<String, String> {
        if (items.isEmpty()) return emptyMap()
        val mapping = mutableMapOf<String, String>()
        items.forEach { item ->
            val payload = when {
                item.base64 != null -> MediaPayload(bytes = runCatching { Base64.decode(item.base64, Base64.DEFAULT) }.getOrNull())
                item.fileName != null -> mediaPayloads[item.fileName]
                else -> null
            } ?: return@forEach
            val folder = when (item.type) {
                ResourceType.IMAGE -> "images"
                ResourceType.VIDEO -> "videos"
                ResourceType.SOUND -> "audio"
                else -> "media"
            }
            val dir = File(appContext.filesDir, folder).apply { mkdirs() }
            val target = File(dir, "media_import_${System.currentTimeMillis()}_${item.originalPath.hashCode()}.dat")
            runCatching {
                when {
                    payload.bytes != null -> target.writeBytes(payload.bytes)
                    payload.file != null -> payload.file.inputStream().use { input ->
                        target.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                }
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

    private fun currentDateString(): String = LocalDate.now().toString()

    fun openMediaStream(path: String): InputStream? {
        val uri = Uri.parse(path)
        return runCatching {
            if (uri.scheme == "file") {
                val filePath = uri.path ?: return@runCatching null
                File(filePath).takeIf { it.exists() }?.inputStream()
            } else {
                appContext.contentResolver.openInputStream(uri)
            }
        }.getOrNull()
    }

    fun mediaSize(path: String): Long? {
        val uri = Uri.parse(path)
        return runCatching {
            if (uri.scheme == "file") {
                val filePath = uri.path ?: return@runCatching null
                File(filePath).takeIf { it.exists() }?.length()?.takeIf { it > 0 }
            } else {
                appContext.contentResolver.openAssetFileDescriptor(uri, "r")?.use { it.length }
                    ?.takeIf { it > 0 }
            }
        }.getOrNull()
    }

    data class MediaExportSource(
        val fileName: String,
        val originalPath: String,
        val type: ResourceType
    )

    companion object {
        const val ORPHAN_CATEGORY_NAME = "未分类"
        @Volatile private var INSTANCE: Repository? = null
        fun get(context: Context): Repository =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Repository(context.applicationContext).also { INSTANCE = it }
            }
    }
}
