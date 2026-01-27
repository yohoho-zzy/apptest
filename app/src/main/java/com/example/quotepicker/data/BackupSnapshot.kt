package com.example.quotepicker.data

import org.json.JSONArray
import org.json.JSONObject

data class BackupSnapshot(
    val categories: List<TagCategoryEntity>,
    val tags: List<TagEntity>,
    val characters: List<CharacterEntity>,
    val resources: List<ResourceEntity>,
    val resourceTagRefs: List<ResourceTagCrossRef>,
    val characterTagRefs: List<CharacterTagCrossRef>,
    val resourceCharacterRefs: List<ResourceCharacterCrossRef>,
    val responseRecords: List<ResponseRecordEntity> = emptyList(),
    val executionSettings: ExecutionSettingsEntity? = null,
    val media: List<MediaBackupItem> = emptyList()
) {
    fun toJsonString(): String = toJson().toString()

    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("version", 4)
            put("categories", JSONArray(categories.map(::categoryToJson)))
            put("tags", JSONArray(tags.map(::tagToJson)))
            put("characters", JSONArray(characters.map(::characterToJson)))
            put("resources", JSONArray(resources.map(::resourceToJson)))
            put("resourceTagRefs", JSONArray(resourceTagRefs.map(::resourceTagToJson)))
            put("characterTagRefs", JSONArray(characterTagRefs.map(::characterTagToJson)))
            put("resourceCharacterRefs", JSONArray(resourceCharacterRefs.map(::resourceCharacterToJson)))
            put("responseRecords", JSONArray(responseRecords.map(::responseRecordToJson)))
            executionSettings?.let { put("executionSettings", executionSettingsToJson(it)) }
            put("media", JSONArray(media.map(::mediaToJson)))
        }
    }

    companion object {
        fun fromJson(json: String): BackupSnapshot {
            val obj = JSONObject(json)
            return BackupSnapshot(
                categories = parseArray(obj, "categories", ::categoryFromJson),
                tags = parseArray(obj, "tags", ::tagFromJson),
                characters = parseArray(obj, "characters", ::characterFromJson),
                resources = parseArray(obj, "resources", ::resourceFromJson),
                resourceTagRefs = parseArray(obj, "resourceTagRefs", ::resourceTagFromJson),
                characterTagRefs = parseArray(obj, "characterTagRefs", ::characterTagFromJson),
                resourceCharacterRefs = parseArray(obj, "resourceCharacterRefs", ::resourceCharacterFromJson),
                responseRecords = parseArray(obj, "responseRecords", ::responseRecordFromJson),
                executionSettings = obj.optJSONObject("executionSettings")?.let(::executionSettingsFromJson),
                media = parseArray(obj, "media", ::mediaFromJson)
            )
        }
    }
}

data class MediaBackupItem(
    val originalPath: String,
    val type: ResourceType,
    val base64: String? = null,
    val fileName: String? = null
)

private fun categoryToJson(category: TagCategoryEntity): JSONObject =
    JSONObject()
        .put("id", category.id)
        .put("type", category.type.name)
        .put("name", category.name)
        .put("createdAt", category.createdAt)
        .put("updatedAt", category.updatedAt)

private fun tagToJson(tag: TagEntity): JSONObject =
    JSONObject()
        .put("id", tag.id)
        .put("categoryId", tag.categoryId)
        .put("name", tag.name)
        .put("colorArgb", tag.colorArgb)
        .put("createdAt", tag.createdAt)
        .put("updatedAt", tag.updatedAt)

private fun characterToJson(character: CharacterEntity): JSONObject =
    JSONObject()
        .put("id", character.id)
        .put("name", character.name)
        .put("description", character.description)
        .put("points", character.points)
        .put("probability", character.probability)
        .put("probabilityDate", character.probabilityDate)
        .put("createdAt", character.createdAt)
        .put("updatedAt", character.updatedAt)

private fun resourceToJson(resource: ResourceEntity): JSONObject =
    JSONObject()
        .put("id", resource.id)
        .put("type", resource.type.name)
        .put("title", resource.title)
        .put("contentUriOrPath", resource.contentUriOrPath)
        .put("quoteText", resource.quoteText)
        .put("quoteImageBase64", resource.quoteImageBase64)
        .put("sceneJson", resource.sceneJson)
        .put("createdAt", resource.createdAt)
        .put("updatedAt", resource.updatedAt)

private fun resourceTagToJson(ref: ResourceTagCrossRef): JSONObject =
    JSONObject()
        .put("resourceId", ref.resourceId)
        .put("tagId", ref.tagId)

private fun characterTagToJson(ref: CharacterTagCrossRef): JSONObject =
    JSONObject()
        .put("characterId", ref.characterId)
        .put("tagId", ref.tagId)

private fun resourceCharacterToJson(ref: ResourceCharacterCrossRef): JSONObject =
    JSONObject()
        .put("resourceId", ref.resourceId)
        .put("characterId", ref.characterId)

private fun responseRecordToJson(record: ResponseRecordEntity): JSONObject =
    JSONObject()
        .put("characterId", record.characterId)
        .put("tagId", record.tagId)
        .put("count", record.count)
        .put("createdAt", record.createdAt)

private fun executionSettingsToJson(settings: ExecutionSettingsEntity): JSONObject =
    JSONObject()
        .put("id", settings.id)
        .put("buttonLabel", settings.buttonLabel)
        .put("successToast", settings.successToast)
        .put("failureToast", settings.failureToast)
        .put("pastAverage", settings.pastAverage)
        .put("lastInputValue", settings.lastInputValue)
        .put("dailyAverage", settings.dailyAverage)
        .put("remainingValue", settings.remainingValue)
        .put("lastExecutionDate", settings.lastExecutionDate)
        .put("createdAt", settings.createdAt)
        .put("updatedAt", settings.updatedAt)

private fun mediaToJson(item: MediaBackupItem): JSONObject =
    JSONObject().apply {
        put("path", item.originalPath)
        put("type", item.type.name)
        item.base64?.let { put("base64", it) }
        item.fileName?.let { put("fileName", it) }
    }

private fun categoryFromJson(obj: JSONObject): TagCategoryEntity =
    TagCategoryEntity(
        id = obj.getLong("id"),
        type = TagCategoryType.valueOf(obj.getString("type")),
        name = obj.getString("name"),
        createdAt = obj.getLong("createdAt"),
        updatedAt = obj.getLong("updatedAt")
    )

private fun tagFromJson(obj: JSONObject): TagEntity =
    TagEntity(
        id = obj.getLong("id"),
        categoryId = obj.getLong("categoryId"),
        name = obj.getString("name"),
        colorArgb = obj.getInt("colorArgb"),
        createdAt = obj.getLong("createdAt"),
        updatedAt = obj.getLong("updatedAt")
    )

private fun characterFromJson(obj: JSONObject): CharacterEntity =
    CharacterEntity(
        id = obj.getLong("id"),
        name = obj.getString("name"),
        description = readNullableString(obj, "description"),
        points = obj.optInt("points", 30),
        probability = obj.optInt("probability", 1),
        probabilityDate = readNullableString(obj, "probabilityDate"),
        createdAt = obj.getLong("createdAt"),
        updatedAt = obj.getLong("updatedAt")
    )

private fun resourceFromJson(obj: JSONObject): ResourceEntity =
    ResourceEntity(
        id = obj.getLong("id"),
        type = ResourceType.valueOf(obj.getString("type")),
        title = obj.getString("title"),
        contentUriOrPath = readNullableString(obj, "contentUriOrPath"),
        quoteText = readNullableString(obj, "quoteText"),
        quoteImageBase64 = readNullableString(obj, "quoteImageBase64"),
        sceneJson = readNullableString(obj, "sceneJson"),
        createdAt = obj.getLong("createdAt"),
        updatedAt = obj.getLong("updatedAt")
    )

private fun resourceTagFromJson(obj: JSONObject): ResourceTagCrossRef =
    ResourceTagCrossRef(
        resourceId = obj.getLong("resourceId"),
        tagId = obj.getLong("tagId")
    )

private fun characterTagFromJson(obj: JSONObject): CharacterTagCrossRef =
    CharacterTagCrossRef(
        characterId = obj.getLong("characterId"),
        tagId = obj.getLong("tagId")
    )

private fun resourceCharacterFromJson(obj: JSONObject): ResourceCharacterCrossRef =
    ResourceCharacterCrossRef(
        resourceId = obj.getLong("resourceId"),
        characterId = obj.getLong("characterId")
    )

private fun responseRecordFromJson(obj: JSONObject): ResponseRecordEntity =
    ResponseRecordEntity(
        characterId = obj.getLong("characterId"),
        tagId = obj.getLong("tagId"),
        count = obj.optInt("count", 1),
        createdAt = obj.optLong("createdAt", System.currentTimeMillis())
    )

private fun executionSettingsFromJson(obj: JSONObject): ExecutionSettingsEntity =
    ExecutionSettingsEntity(
        id = obj.optLong("id", 1),
        buttonLabel = obj.optString("buttonLabel", "祈求"),
        successToast = obj.optString("successToast", "[]赐予了你[]"),
        failureToast = obj.optString("failureToast", "[]无视了你"),
        pastAverage = obj.optInt("pastAverage", 100),
        lastInputValue = obj.optInt("lastInputValue", 0),
        dailyAverage = obj.optInt("dailyAverage", 100),
        remainingValue = obj.optInt("remainingValue", 0),
        lastExecutionDate = readNullableString(obj, "lastExecutionDate"),
        createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
        updatedAt = obj.optLong("updatedAt", System.currentTimeMillis())
    )

private fun mediaFromJson(obj: JSONObject): MediaBackupItem =
    MediaBackupItem(
        originalPath = obj.getString("path"),
        type = ResourceType.valueOf(obj.getString("type")),
        base64 = readNullableString(obj, "base64"),
        fileName = readNullableString(obj, "fileName")
    )

private fun <T> parseArray(
    obj: JSONObject,
    key: String,
    mapper: (JSONObject) -> T
): List<T> {
    val array = obj.optJSONArray(key) ?: JSONArray()
    return List(array.length()) { index ->
        mapper(array.getJSONObject(index))
    }
}

private fun readNullableString(obj: JSONObject, key: String): String? {
    return if (obj.has(key) && !obj.isNull(key)) obj.getString(key) else null
}
