package com.example.quotepicker.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TagCategoryDao {
    @Query("SELECT * FROM tag_categories ORDER BY name COLLATE NOCASE ASC")
    fun observeCategories(): Flow<List<TagCategoryEntity>>

    @Query("SELECT * FROM tag_categories ORDER BY name COLLATE NOCASE ASC")
    suspend fun listAll(): List<TagCategoryEntity>

    @Query("SELECT * FROM tag_categories WHERE type = :type ORDER BY name COLLATE NOCASE ASC")
    fun observeCategoriesByType(type: TagCategoryType): Flow<List<TagCategoryEntity>>

    @Query("SELECT * FROM tag_categories WHERE name = :name AND type = :type LIMIT 1")
    suspend fun findByName(name: String, type: TagCategoryType): TagCategoryEntity?

    @Insert
    suspend fun insert(category: TagCategoryEntity): Long

    @Insert
    suspend fun insertAll(categories: List<TagCategoryEntity>)

    @Update
    suspend fun update(category: TagCategoryEntity)

    @Delete
    suspend fun delete(category: TagCategoryEntity)

    @Query("DELETE FROM tag_categories")
    suspend fun deleteAll()
}

@Dao
interface TagDao {
    @Query("SELECT * FROM tags WHERE categoryId = :categoryId ORDER BY name COLLATE NOCASE ASC")
    fun observeTagsByCategory(categoryId: Long): Flow<List<TagEntity>>

    @Query("SELECT * FROM tags ORDER BY name COLLATE NOCASE ASC")
    fun observeAllTags(): Flow<List<TagEntity>>

    @Query("SELECT * FROM tags ORDER BY name COLLATE NOCASE ASC")
    suspend fun listAll(): List<TagEntity>

    @Insert
    suspend fun insert(tag: TagEntity): Long

    @Insert
    suspend fun insertAll(tags: List<TagEntity>)

    @Update
    suspend fun update(tag: TagEntity)

    @Delete
    suspend fun delete(tag: TagEntity)

    @Query("UPDATE tags SET categoryId = :newCategoryId WHERE categoryId = :oldCategoryId")
    suspend fun reassignCategory(oldCategoryId: Long, newCategoryId: Long)

    @Query("DELETE FROM tags")
    suspend fun deleteAll()
}

@Dao
interface CharacterDao {
    @Query("SELECT * FROM characters ORDER BY name COLLATE NOCASE ASC")
    fun observeCharacters(): Flow<List<CharacterEntity>>

    @Transaction
    @Query("SELECT * FROM characters ORDER BY name COLLATE NOCASE ASC")
    fun observeCharactersWithTags(): Flow<List<CharacterWithTags>>

    @Query("SELECT * FROM characters ORDER BY name COLLATE NOCASE ASC")
    suspend fun listAll(): List<CharacterEntity>

    @Insert
    suspend fun insert(character: CharacterEntity): Long

    @Insert
    suspend fun insertAll(characters: List<CharacterEntity>)

    @Update
    suspend fun update(character: CharacterEntity)

    @Delete
    suspend fun delete(character: CharacterEntity)

    @Query("DELETE FROM characters")
    suspend fun deleteAll()
}

@Dao
interface ResourceDao {
    @Query("SELECT * FROM resources ORDER BY createdAt DESC")
    fun observeResources(): Flow<List<ResourceEntity>>

    @Transaction
    @Query("SELECT * FROM resources ORDER BY createdAt DESC")
    fun observeResourcesWithRelations(): Flow<List<ResourceWithTagsCharacters>>

    @Query("SELECT * FROM resources ORDER BY createdAt DESC")
    suspend fun listAll(): List<ResourceEntity>

    @Insert
    suspend fun insert(resource: ResourceEntity): Long

    @Insert
    suspend fun insertAll(resources: List<ResourceEntity>)

    @Update
    suspend fun update(resource: ResourceEntity)

    @Delete
    suspend fun delete(resource: ResourceEntity)

    @Query("DELETE FROM resources")
    suspend fun deleteAll()
}

@Dao
interface CrossRefDao {
    @Insert
    suspend fun insertResourceTags(refs: List<ResourceTagCrossRef>)

    @Insert
    suspend fun insertCharacterTags(refs: List<CharacterTagCrossRef>)

    @Insert
    suspend fun insertResourceCharacters(refs: List<ResourceCharacterCrossRef>)

    @Query("SELECT * FROM resource_tag_cross_ref")
    suspend fun listResourceTags(): List<ResourceTagCrossRef>

    @Query("SELECT * FROM character_tag_cross_ref")
    suspend fun listCharacterTags(): List<CharacterTagCrossRef>

    @Query("SELECT * FROM resource_character_cross_ref")
    suspend fun listResourceCharacters(): List<ResourceCharacterCrossRef>

    @Query("DELETE FROM resource_tag_cross_ref WHERE resourceId = :resourceId")
    suspend fun clearResourceTags(resourceId: Long)

    @Query("DELETE FROM character_tag_cross_ref WHERE characterId = :characterId")
    suspend fun clearCharacterTags(characterId: Long)

    @Query("DELETE FROM resource_character_cross_ref WHERE resourceId = :resourceId")
    suspend fun clearResourceCharacters(resourceId: Long)

    @Query("SELECT resourceId FROM resource_character_cross_ref WHERE characterId = :characterId")
    suspend fun resourceIdsForCharacter(characterId: Long): List<Long>

    @Query("SELECT resourceId FROM resource_tag_cross_ref WHERE tagId IN (:tagIds)")
    suspend fun resourceIdsForTags(tagIds: List<Long>): List<Long>

    @Query("DELETE FROM resource_tag_cross_ref")
    suspend fun deleteAllResourceTags()

    @Query("DELETE FROM character_tag_cross_ref")
    suspend fun deleteAllCharacterTags()

    @Query("DELETE FROM resource_character_cross_ref")
    suspend fun deleteAllResourceCharacters()
}

@Dao
interface ResponseRecordDao {
    @Query("SELECT * FROM response_records")
    fun observeRecords(): Flow<List<ResponseRecordEntity>>

    @Query("SELECT * FROM response_records")
    suspend fun listAll(): List<ResponseRecordEntity>

    @Query("SELECT * FROM response_records WHERE characterId = :characterId AND tagId = :tagId LIMIT 1")
    suspend fun findRecord(characterId: Long, tagId: Long): ResponseRecordEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(record: ResponseRecordEntity)

    @Query("DELETE FROM response_records WHERE characterId = :characterId AND tagId = :tagId")
    suspend fun deleteRecord(characterId: Long, tagId: Long)

    @Query("DELETE FROM response_records")
    suspend fun deleteAll()
}

@Dao
interface ExecutionSettingsDao {
    @Query("SELECT * FROM execution_settings WHERE id = 1 LIMIT 1")
    fun observeSettings(): Flow<ExecutionSettingsEntity?>

    @Query("SELECT * FROM execution_settings WHERE id = 1 LIMIT 1")
    suspend fun getSettings(): ExecutionSettingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(settings: ExecutionSettingsEntity)

    @Query("DELETE FROM execution_settings")
    suspend fun deleteAll()
}
