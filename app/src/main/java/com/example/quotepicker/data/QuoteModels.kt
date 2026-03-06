package com.example.quotepicker.data

enum class QuoteType { TEXT, IMAGE }

data class GroupEntity(
    val id: Long,
    val name: String
)

data class QuoteEntity(
    val id: Long,
    val groupId: Long,
    val type: QuoteType,
    val text: String? = null,
    val imageBase64: String? = null,
    val weight: Int = 1,
    val enabled: Boolean = true
)
