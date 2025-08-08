package com.example.quotepicker.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface GroupDao {
    @Query("SELECT * FROM groups ORDER BY name COLLATE NOCASE ASC")
    fun observeGroups(): Flow<List<GroupEntity>>

    @Insert
    suspend fun insert(group: GroupEntity): Long

    @Delete
    suspend fun delete(group: GroupEntity)

    @Update
    suspend fun update(group: GroupEntity)
}

@Dao
interface QuoteDao {
    @Query("SELECT * FROM quotes WHERE groupId = :groupId ORDER BY createdAt DESC")
    fun observeQuotesByGroup(groupId: Long): Flow<List<QuoteEntity>>

    @Query("SELECT * FROM quotes ORDER BY createdAt DESC")
    fun observeAllQuotes(): Flow<List<QuoteEntity>>

    @Insert
    suspend fun insert(quote: QuoteEntity): Long

    @Update
    suspend fun update(quote: QuoteEntity)

    @Delete
    suspend fun delete(quote: QuoteEntity)
}