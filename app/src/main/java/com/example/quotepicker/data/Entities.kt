package com.example.quotepicker.data

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

enum class ResourceType { FLOW, TEXT, IMAGE, VIDEO, SOUND, SCENE }

enum class TagCategoryType { CHARACTER, RESOURCE }

@Entity(
    tableName = "tag_categories",
    indices = [Index(value = ["type", "name"], unique = true)]
)
data class TagCategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: TagCategoryType = TagCategoryType.RESOURCE,
    val name: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "tags",
    indices = [Index(value = ["categoryId", "name"], unique = true)]
)
data class TagEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val categoryId: Long,
    val name: String,
    val colorArgb: Int,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "characters",
    indices = [Index(value = ["name"], unique = true)]
)
data class CharacterEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val description: String? = null,
    val points: Int = 30,
    val familiarity: Int = 0,
    val probability: Int = 1,
    val probabilityDate: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "response_records",
    primaryKeys = ["characterId", "tagId"]
)
data class ResponseRecordEntity(
    val characterId: Long,
    val tagId: Long,
    val count: Int = 1,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "execution_settings")
data class ExecutionSettingsEntity(
    @PrimaryKey val id: Long = 1,
    val buttonLabel: String = "祈求",
    val successToast: String = "[]赐予了你[]",
    val failureToast: String = "[]无视了你",
    val pastAverage: Int = 100,
    val lastInputValue: Int = 0,
    val dailyAverage: Int = 100,
    val remainingValue: Int = 0,
    val lastExecutionDate: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "execution_resources")
data class ExecutionResourceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val resourceId: Long,
    val characterId: Long,
    val tagId: Long,
    val characterName: String,
    val tagName: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "resources")
data class ResourceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: ResourceType,
    val title: String,
    val contentUriOrPath: String? = null,
    val quoteText: String? = null,
    val quoteImageBase64: String? = null,
    val sceneJson: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "resource_tag_cross_ref",
    primaryKeys = ["resourceId", "tagId"]
)
data class ResourceTagCrossRef(
    val resourceId: Long,
    val tagId: Long
)

@Entity(
    tableName = "character_tag_cross_ref",
    primaryKeys = ["characterId", "tagId"]
)
data class CharacterTagCrossRef(
    val characterId: Long,
    val tagId: Long
)

@Entity(
    tableName = "resource_character_cross_ref",
    primaryKeys = ["resourceId", "characterId"]
)
data class ResourceCharacterCrossRef(
    val resourceId: Long,
    val characterId: Long
)

data class CharacterWithTags(
    @Embedded val character: CharacterEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = androidx.room.Junction(
            value = CharacterTagCrossRef::class,
            parentColumn = "characterId",
            entityColumn = "tagId"
        )
    )
    val tags: List<TagEntity>
)

data class ResourceWithTagsCharacters(
    @Embedded val resource: ResourceEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = androidx.room.Junction(
            value = ResourceTagCrossRef::class,
            parentColumn = "resourceId",
            entityColumn = "tagId"
        )
    )
    val tags: List<TagEntity>,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = androidx.room.Junction(
            value = ResourceCharacterCrossRef::class,
            parentColumn = "resourceId",
            entityColumn = "characterId"
        )
    )
    val characters: List<CharacterEntity>
)
