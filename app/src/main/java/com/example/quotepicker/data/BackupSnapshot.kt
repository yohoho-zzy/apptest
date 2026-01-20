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
    val media: List<MediaBackupItem> = emptyList()
) {
    fun toJsonString(): String = toJson().toString()

    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("version", 2)
            put("categories", JSONArray(categories.map(::categoryToJson)))
            put("tags", JSONArray(tags.map(::tagToJson)))
            put("characters", JSONArray(characters.map(::characterToJson)))
            put("resources", JSONArray(resources.map(::resourceToJson)))
            put("resourceTagRefs", JSONArray(resourceTagRefs.map(::resourceTagToJson)))
            put("characterTagRefs", JSONArray(characterTagRefs.map(::characterTagToJson)))
            put("resourceCharacterRefs", JSONArray(resourceCharacterRefs.map(::resourceCharacterToJson)))
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
                media = parseArray(obj, "media", ::mediaFromJson)
            )
        }
    }
}

data class MediaBackupItem(
    val originalPath: String,
    val type: ResourceType,
    val base64: String
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

private fun mediaToJson(item: MediaBackupItem): JSONObject =
    JSONObject()
        .put("path", item.originalPath)
        .put("type", item.type.name)
        .put("base64", item.base64)

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

private fun mediaFromJson(obj: JSONObject): MediaBackupItem =
    MediaBackupItem(
        originalPath = obj.getString("path"),
        type = ResourceType.valueOf(obj.getString("type")),
        base64 = obj.getString("base64")
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
