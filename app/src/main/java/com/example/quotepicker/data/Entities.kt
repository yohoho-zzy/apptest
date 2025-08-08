package com.example.quotepicker.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "groups")
data class GroupEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val createdAt: Long = System.currentTimeMillis()
)

enum class QuoteType { TEXT, IMAGE }

@Entity(tableName = "quotes")
data class QuoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val groupId: Long,
    val type: QuoteType,
    val text: String? = null,
    val imageBase64: String? = null,
    val weight: Int = 1,
    val enabled: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)