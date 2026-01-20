package com.example.quotepicker.data

import android.content.Context
import kotlinx.coroutines.flow.Flow

class Repository private constructor(context: Context) {
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

    suspend fun exportSnapshot(): BackupSnapshot =
        BackupSnapshot(
            categories = categoryDao.listAll(),
            tags = tagDao.listAll(),
            characters = characterDao.listAll(),
            resources = resourceDao.listAll(),
            resourceTagRefs = crossRefDao.listResourceTags(),
            characterTagRefs = crossRefDao.listCharacterTags(),
            resourceCharacterRefs = crossRefDao.listResourceCharacters()
        )

    suspend fun replaceSnapshot(snapshot: BackupSnapshot) {
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
        if (snapshot.resources.isNotEmpty()) resourceDao.insertAll(snapshot.resources)
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

    companion object {
        const val ORPHAN_CATEGORY_NAME = "未分类"
        @Volatile private var INSTANCE: Repository? = null
        fun get(context: Context): Repository =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Repository(context.applicationContext).also { INSTANCE = it }
            }
    }
}
