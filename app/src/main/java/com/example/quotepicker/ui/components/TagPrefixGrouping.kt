package com.example.quotepicker.ui.components

import com.example.quotepicker.data.TagEntity

private const val SPECIAL_CATEGORY_SUFFIX = "+"

data class GroupedTagItem(
    val tag: TagEntity,
    val displayName: String
)

data class TagPrefixGroup(
    val name: String,
    val items: List<GroupedTagItem>
)

data class TagPrefixGroupingResult(
    val groups: List<TagPrefixGroup>,
    val ungrouped: List<GroupedTagItem>
)

fun isPrefixGroupingCategory(categoryName: String?): Boolean {
    return categoryName?.trim()?.endsWith(SPECIAL_CATEGORY_SUFFIX) == true
}

fun splitTagsByPrefix(tags: List<TagEntity>): TagPrefixGroupingResult {
    val groupedMap = linkedMapOf<String, MutableList<GroupedTagItem>>()
    val ungrouped = mutableListOf<GroupedTagItem>()
    tags.forEach { tag ->
        val parts = tag.name.split("-", limit = 2).map { it.trim() }
        if (parts.size == 2 && parts[0].isNotBlank() && parts[1].isNotBlank()) {
            groupedMap.getOrPut(parts[0]) { mutableListOf() }
                .add(GroupedTagItem(tag = tag, displayName = parts[1]))
        } else {
            ungrouped.add(GroupedTagItem(tag = tag, displayName = tag.name))
        }
    }
    val groups = groupedMap
        .toList()
        .sortedBy { it.first.lowercase() }
        .map { TagPrefixGroup(name = it.first, items = it.second) }
    return TagPrefixGroupingResult(groups = groups, ungrouped = ungrouped)
}
