package com.example.quotepicker.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.quotepicker.ui.components.ResourcePreviewScreen
import com.example.quotepicker.ui.components.CharacterBadge
import com.example.quotepicker.ui.components.TagBadge
import com.example.quotepicker.data.TagCategoryEntity
import com.example.quotepicker.data.TagEntity
import com.example.quotepicker.vm.RandomViewModel
import com.example.quotepicker.vm.ResourceViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RandomScreen(modifier: Modifier = Modifier, vm: RandomViewModel = viewModel(), resourceVm: ResourceViewModel = viewModel()) {
    val ui by vm.uiState.collectAsState()
    var showPreview by remember { mutableStateOf(false) }
    var showTagDialog by remember { mutableStateOf(false) }

    if (showPreview && ui.selectedResource != null) {
        ResourcePreviewScreen(
            resource = ui.selectedResource!!,
            vm = resourceVm,
            onBack = { showPreview = false }
        )
        return
    }

    Column(modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Button(onClick = { vm.randomCharacter() }) {
            Icon(Icons.Default.Casino, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("随机角色")
        }
        if (ui.selectedCharacter == null) {
            Text("未选择角色")
        } else {
            CharacterBadge(name = ui.selectedCharacter!!.name)
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = { showTagDialog = true }) {
                    Text("资源标签筛选(${ui.selectedTagIds.size})")
                }
            }
            if (ui.selectedTagIds.isEmpty()) {
                Text("未选择标签")
            } else {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ui.tags.filter { ui.selectedTagIds.contains(it.id) }.forEach { tag ->
                        TagBadge(tag = tag)
                    }
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = { vm.randomResource() }, enabled = ui.selectedCharacter != null) {
                Icon(Icons.Default.Casino, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("随机资源")
            }
            Button(onClick = { vm.nextResource() }, enabled = ui.selectedCharacter != null) {
                Icon(Icons.Default.SkipNext, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("下一个")
            }
            Button(onClick = { vm.reset() }) {
                Icon(Icons.Default.Refresh, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("重置")
            }
        }
        Spacer(Modifier.height(8.dp))
        val res = ui.selectedResource
        if (res == null) {
            Text("暂无资源")
        } else {
            Text("当前资源：${res.resource.title}")
            Button(onClick = { showPreview = true }) {
                Icon(Icons.Default.Visibility, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("预览")
            }
        }
    }

    if (showTagDialog) {
        TagPickerDialog(
            categories = ui.categories,
            tags = ui.tags,
            selectedIds = ui.selectedTagIds,
            onConfirm = vm::updateTagFilter,
            onDismiss = { showTagDialog = false }
        )
    }
}

@Composable
private fun TagPickerDialog(
    categories: List<TagCategoryEntity>,
    tags: List<TagEntity>,
    selectedIds: Set<Long>,
    onConfirm: (Set<Long>) -> Unit,
    onDismiss: () -> Unit
) {
    var selected by remember { mutableStateOf(selectedIds.toMutableSet()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("资源标签筛选") },
        text = {
            TagSelectionSection(
                categories = categories,
                tags = tags,
                selected = selected,
                onChange = { selected = it.toMutableSet() }
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(selected); onDismiss() }) { Text("确定") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TagSelectionSection(
    categories: List<TagCategoryEntity>,
    tags: List<TagEntity>,
    selected: Set<Long>,
    onChange: (Set<Long>) -> Unit
) {
    val grouped = tags.groupBy { it.categoryId }
    val knownCategoryIds = categories.map { it.id }.toSet()
    val uncategorized = tags.filter { it.categoryId !in knownCategoryIds }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        categories.forEach { category ->
            val items = grouped[category.id].orEmpty()
            if (items.isNotEmpty()) {
                Text(category.name)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items.forEach { tag ->
                        TagFilterChip(
                            tag = tag,
                            selected = selected,
                            onChange = onChange
                        )
                    }
                }
            }
        }
        if (uncategorized.isNotEmpty()) {
            Text("未分类")
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                uncategorized.forEach { tag ->
                    TagFilterChip(
                        tag = tag,
                        selected = selected,
                        onChange = onChange
                    )
                }
            }
        }
    }
}

@Composable
private fun TagFilterChip(
    tag: TagEntity,
    selected: Set<Long>,
    onChange: (Set<Long>) -> Unit
) {
    val isSelected = selected.contains(tag.id)
    FilterChip(
        selected = isSelected,
        onClick = {
            val updated = selected.toMutableSet()
            if (isSelected) updated.remove(tag.id) else updated.add(tag.id)
            onChange(updated)
        },
        label = { Text(tag.name) },
        colors = FilterChipDefaults.filterChipColors()
    )
}
