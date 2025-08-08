package com.example.quotepicker.data

import android.content.Context
import kotlinx.coroutines.flow.Flow

class Repository private constructor(context: Context) {
    private val db = AppDatabase.get(context)
    private val groupDao = db.groupDao()
    private val quoteDao = db.quoteDao()

    fun observeGroups(): Flow<List<GroupEntity>> = groupDao.observeGroups()
    fun observeQuotes(groupId: Long?): Flow<List<QuoteEntity>> =
        if (groupId == null) quoteDao.observeAllQuotes() else quoteDao.observeQuotesByGroup(groupId)

    suspend fun addGroup(name: String) = groupDao.insert(GroupEntity(name = name))
    suspend fun deleteGroup(group: GroupEntity) = groupDao.delete(group)

    suspend fun addTextQuote(groupId: Long, text: String, weight: Int) =
        quoteDao.insert(QuoteEntity(groupId = groupId, type = QuoteType.TEXT, text = text, weight = weight))

    suspend fun addImageQuote(groupId: Long, base64: String, weight: Int) =
        quoteDao.insert(QuoteEntity(groupId = groupId, type = QuoteType.IMAGE, imageBase64 = base64, weight = weight))

    suspend fun updateQuote(q: QuoteEntity) = quoteDao.update(q)
    suspend fun deleteQuote(q: QuoteEntity) = quoteDao.delete(q)

    companion object {
        @Volatile private var INSTANCE: Repository? = null
        fun get(context: Context): Repository =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Repository(context.applicationContext).also { INSTANCE = it }
            }
    }
}