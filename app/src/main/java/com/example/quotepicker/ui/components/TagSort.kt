package com.example.quotepicker.ui.components

import com.example.quotepicker.data.TagCategoryEntity
import com.example.quotepicker.data.TagEntity

fun sortTagsForDisplay(tags: List<TagEntity>, categories: List<TagCategoryEntity>): List<TagEntity> {
    val categoryOrder = categories.mapIndexed { index, category -> category.id to index }.toMap()
    return tags.sortedWith(
        compareBy<TagEntity> { categoryOrder[it.categoryId] ?: Int.MAX_VALUE }
            .thenBy { it.colorArgb }
            .thenBy { it.name.lowercase() }
    )
}
