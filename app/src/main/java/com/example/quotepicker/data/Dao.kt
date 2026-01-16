package com.example.quotepicker.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TagCategoryDao {
    @Query("SELECT * FROM tag_categories ORDER BY name COLLATE NOCASE ASC")
    fun observeCategories(): Flow<List<TagCategoryEntity>>

    @Insert
    suspend fun insert(category: TagCategoryEntity): Long

    @Update
    suspend fun update(category: TagCategoryEntity)

    @Delete
    suspend fun delete(category: TagCategoryEntity)
}

@Dao
interface TagDao {
    @Query("SELECT * FROM tags WHERE categoryId = :categoryId ORDER BY name COLLATE NOCASE ASC")
    fun observeTagsByCategory(categoryId: Long): Flow<List<TagEntity>>

    @Query("SELECT * FROM tags ORDER BY name COLLATE NOCASE ASC")
    fun observeAllTags(): Flow<List<TagEntity>>

    @Insert
    suspend fun insert(tag: TagEntity): Long

    @Update
    suspend fun update(tag: TagEntity)

    @Delete
    suspend fun delete(tag: TagEntity)
}

@Dao
interface CharacterDao {
    @Query("SELECT * FROM characters ORDER BY name COLLATE NOCASE ASC")
    fun observeCharacters(): Flow<List<CharacterEntity>>

    @Transaction
    @Query("SELECT * FROM characters ORDER BY name COLLATE NOCASE ASC")
    fun observeCharactersWithTags(): Flow<List<CharacterWithTags>>

    @Insert
    suspend fun insert(character: CharacterEntity): Long

    @Update
    suspend fun update(character: CharacterEntity)

    @Delete
    suspend fun delete(character: CharacterEntity)
}

@Dao
interface ResourceDao {
    @Query("SELECT * FROM resources ORDER BY createdAt DESC")
    fun observeResources(): Flow<List<ResourceEntity>>

    @Transaction
    @Query("SELECT * FROM resources ORDER BY createdAt DESC")
    fun observeResourcesWithRelations(): Flow<List<ResourceWithTagsCharacters>>

    @Insert
    suspend fun insert(resource: ResourceEntity): Long

    @Update
    suspend fun update(resource: ResourceEntity)

    @Delete
    suspend fun delete(resource: ResourceEntity)
}

@Dao
interface CrossRefDao {
    @Insert
    suspend fun insertResourceTags(refs: List<ResourceTagCrossRef>)

    @Insert
    suspend fun insertCharacterTags(refs: List<CharacterTagCrossRef>)

    @Insert
    suspend fun insertResourceCharacters(refs: List<ResourceCharacterCrossRef>)

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
}
