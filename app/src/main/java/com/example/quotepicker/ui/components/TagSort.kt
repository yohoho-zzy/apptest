package com.example.quotepicker.ui.components

import com.example.quotepicker.data.TagCategoryEntity
import com.example.quotepicker.data.TagEntity

private val tagColorOrder = listOf(
    0xFFFF8A80.toInt(), // 红
    0xFFFFF59D.toInt(), // 黄
    0xFFB388FF.toInt(), // 紫
    0xFF82B1FF.toInt(), // 蓝
    0xFFA5D6A7.toInt(), // 绿
    0xFFE0E0E0.toInt() // 灰
)

fun tagColorSortIndex(colorArgb: Int): Int {
    val index = tagColorOrder.indexOf(colorArgb)
    return if (index >= 0) index else tagColorOrder.size
}

fun sortTagsForDisplay(tags: List<TagEntity>, categories: List<TagCategoryEntity>): List<TagEntity> {
    val categoryOrder = categories.mapIndexed { index, category -> category.id to index }.toMap()
    return tags.sortedWith(
        compareBy<TagEntity> { categoryOrder[it.categoryId] ?: Int.MAX_VALUE }
            .thenBy { tagColorSortIndex(it.colorArgb) }
            .thenBy { it.name.lowercase() }
    )
}
