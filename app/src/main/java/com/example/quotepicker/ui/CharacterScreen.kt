package com.example.quotepicker.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.AssistChip
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.quotepicker.data.CharacterEntity
import com.example.quotepicker.data.CharacterWithTags
import com.example.quotepicker.data.ResourceType
import com.example.quotepicker.data.TagCategoryEntity
import com.example.quotepicker.data.TagEntity
import com.example.quotepicker.ui.components.AvatarListItem
import com.example.quotepicker.ui.components.NameDialog
import com.example.quotepicker.vm.CharacterViewModel
import com.example.quotepicker.vm.ResourceViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CharacterScreen(
    modifier: Modifier = Modifier,
    vm: CharacterViewModel = viewModel(),
    resourceVm: ResourceViewModel = viewModel()
) {
    val ui by vm.uiState.collectAsState()
    val resources by resourceVm.uiState.collectAsState()
    var selected by remember { mutableStateOf<CharacterWithTags?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }
    var editCharacter by remember { mutableStateOf<CharacterEntity?>(null) }
    var showTagPicker by remember { mutableStateOf(false) }
    var bottomSheetTarget by remember { mutableStateOf<CharacterWithTags?>(null) }
    var deleteTarget by remember { mutableStateOf<CharacterEntity?>(null) }
    var showImages by remember { mutableStateOf(true) }
    var showVideos by remember { mutableStateOf(true) }
    var showAudios by remember { mutableStateOf(true) }
    var showQuotes by remember { mutableStateOf(true) }
    var showScenes by remember { mutableStateOf(true) }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(selected?.character?.name ?: "角色") },
                navigationIcon = {
                    if (selected != null) {
                        IconButton(onClick = { selected = null }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            if (selected == null) {
                FloatingActionButton(onClick = { showAddDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = null)
                }
            }
        }
    ) { inner ->
        Column(Modifier.padding(inner).fillMaxSize()) {
            if (selected == null) {
                if (ui.characters.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("暂无角色，点击右下角添加")
                    }
                } else {
                    LazyColumn(
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(ui.characters, key = { it.character.id }) { character ->
                            AvatarListItem(
                                title = character.character.name,
                                onClick = { selected = character },
                                onLongClick = { bottomSheetTarget = character }
                            )
                        }
                    }
                }
            } else {
                val char = selected!!.character
                OutlinedTextField(
                    value = char.description.orEmpty(),
                    onValueChange = { vm.updateCharacter(char.copy(description = it)) },
                    label = { Text("简介") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                )
                Row(Modifier.padding(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    selected!!.tags.forEach { tag ->
                        AssistChip(onClick = {}, label = { Text(tag.name) })
                    }
                    TextButton(onClick = { showTagPicker = true }) { Text("编辑标签") }
                }
                Spacer(Modifier.height(12.dp))
                val grouped = resources.resources.filter {
                    it.characters.any { c -> c.id == char.id }
                }.groupBy { it.resource.type }
                ResourceGroup(
                    title = "图片",
                    items = grouped[ResourceType.IMAGE].orEmpty(),
                    expanded = showImages,
                    onToggle = { showImages = !showImages }
                )
                ResourceGroup(
                    title = "视频",
                    items = grouped[ResourceType.VIDEO].orEmpty(),
                    expanded = showVideos,
                    onToggle = { showVideos = !showVideos }
                )
                ResourceGroup(
                    title = "声音",
                    items = grouped[ResourceType.AUDIO].orEmpty(),
                    expanded = showAudios,
                    onToggle = { showAudios = !showAudios }
                )
                ResourceGroup(
                    title = "语录",
                    items = grouped[ResourceType.QUOTE].orEmpty(),
                    expanded = showQuotes,
                    onToggle = { showQuotes = !showQuotes }
                )
                ResourceGroup(
                    title = "情景",
                    items = grouped[ResourceType.SCENE].orEmpty(),
                    expanded = showScenes,
                    onToggle = { showScenes = !showScenes }
                )
            }
        }
    }

    if (showAddDialog) {
        NameDialog(
            title = "新增角色",
            initial = "",
            onConfirm = { vm.addCharacter(it) },
            onDismiss = { showAddDialog = false }
        )
    }

    editCharacter?.let { character ->
        CharacterEditDialog(
            character = character,
            onConfirm = { vm.updateCharacter(it) },
            onDismiss = { editCharacter = null }
        )
    }

    if (showTagPicker && selected != null) {
        TagPickerDialog(
            categories = ui.categories,
            tags = ui.tags,
            selectedIds = selected!!.tags.map { it.id }.toSet(),
            onConfirm = { ids ->
                vm.updateCharacterTags(selected!!.character.id, ids.toList())
                showTagPicker = false
            },
            onDismiss = { showTagPicker = false }
        )
    }

    bottomSheetTarget?.let { target ->
        ModalBottomSheet(onDismissRequest = { bottomSheetTarget = null }) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                TextButton(onClick = {
                    editCharacter = target.character
                    bottomSheetTarget = null
                }) {
                    Icon(Icons.Default.Edit, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("编辑")
                }
                TextButton(onClick = {
                    deleteTarget = target.character
                    bottomSheetTarget = null
                }) {
                    Icon(Icons.Default.Delete, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("删除")
                }
                TextButton(onClick = { bottomSheetTarget = null }) { Text("关闭") }
            }
            Spacer(Modifier.height(12.dp))
        }
    }

    deleteTarget?.let { character ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("删除角色") },
            text = { Text("确定删除“${character.name}”吗？") },
            confirmButton = {
                TextButton(onClick = {
                    vm.deleteCharacter(character)
                    deleteTarget = null
                }) { Text("删除") }
            },
            dismissButton = { TextButton(onClick = { deleteTarget = null }) { Text("取消") } }
        )
    }
}

@Composable
private fun ResourceGroup(
    title: String,
    items: List<com.example.quotepicker.data.ResourceWithTagsCharacters>,
    expanded: Boolean,
    onToggle: () -> Unit
) {
    Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(title, modifier = Modifier.weight(1f))
            IconButton(onClick = onToggle) {
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null
                )
            }
        }
        if (expanded) {
            if (items.isEmpty()) {
                Text("暂无${title}资源")
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items.forEach { resource ->
                        AvatarListItem(
                            title = resource.resource.title,
                            onClick = {},
                            onLongClick = {}
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CharacterEditDialog(
    character: CharacterEntity,
    onConfirm: (CharacterEntity) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(character.name) }
    var description by remember { mutableStateOf(character.description.orEmpty()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("编辑角色") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("角色名") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("简介") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (name.isNotBlank()) onConfirm(character.copy(name = name.trim(), description = description))
                onDismiss()
            }) { Text("确定") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

@Composable
private fun TagPickerDialog(
    categories: List<TagCategoryEntity>,
    tags: List<com.example.quotepicker.data.TagEntity>,
    selectedIds: Set<Long>,
    onConfirm: (Set<Long>) -> Unit,
    onDismiss: () -> Unit
) {
    var selected by remember { mutableStateOf(selectedIds.toMutableSet()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择标签") },
        text = {
            TagSelectionSection(
                label = "标签",
                categories = categories,
                tags = tags,
                selected = selected,
                onChange = { selected = it.toMutableSet() }
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(selected) }) { Text("确定") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TagSelectionSection(
    label: String,
    categories: List<TagCategoryEntity>,
    tags: List<TagEntity>,
    selected: Set<Long>,
    onChange: (Set<Long>) -> Unit
) {
    val grouped = tags.groupBy { it.categoryId }
    val knownCategoryIds = categories.map { it.id }.toSet()
    val uncategorized = tags.filter { it.categoryId !in knownCategoryIds }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label)
        categories.forEach { category ->
            val items = grouped[category.id].orEmpty()
            if (items.isNotEmpty()) {
                Text(category.name, style = MaterialTheme.typography.labelMedium)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items.forEach { tag ->
                        val isSelected = selected.contains(tag.id)
                        val tagColor = Color(tag.colorArgb)
                        val selectedTextColor = if (tagColor.luminance() < 0.5f) Color.White else Color.Black
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                val newSet = selected.toMutableSet()
                                if (isSelected) newSet.remove(tag.id) else newSet.add(tag.id)
                                onChange(newSet)
                            },
                            label = { Text(tag.name) },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = Color.White,
                                selectedContainerColor = tagColor,
                                labelColor = MaterialTheme.colorScheme.onSurface,
                                selectedLabelColor = selectedTextColor
                            )
                        )
                    }
                }
            }
        }
        if (uncategorized.isNotEmpty()) {
            Text("其他", style = MaterialTheme.typography.labelMedium)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                uncategorized.forEach { tag ->
                    val isSelected = selected.contains(tag.id)
                    val tagColor = Color(tag.colorArgb)
                    val selectedTextColor = if (tagColor.luminance() < 0.5f) Color.White else Color.Black
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            val newSet = selected.toMutableSet()
                            if (isSelected) newSet.remove(tag.id) else newSet.add(tag.id)
                            onChange(newSet)
                        },
                        label = { Text(tag.name) },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = Color.White,
                            selectedContainerColor = tagColor,
                            labelColor = MaterialTheme.colorScheme.onSurface,
                            selectedLabelColor = selectedTextColor
                        )
                    )
                }
            }
        }
    }
}
