package com.example.quotepicker.ui

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
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
import com.example.quotepicker.ui.components.NameDialog
import com.example.quotepicker.ui.components.ResourcePreviewScreen
import com.example.quotepicker.ui.components.SquareGridItem
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
    var filterTagDialog by remember { mutableStateOf(false) }
    var selectedTagIds by remember { mutableStateOf(setOf<Long>()) }
    var selectedType by remember { mutableStateOf<ResourceType?>(null) }
    var previewTarget by remember { mutableStateOf<com.example.quotepicker.data.ResourceWithTagsCharacters?>(null) }

    if (previewTarget != null) {
        ResourcePreviewScreen(
            resource = previewTarget!!,
            vm = resourceVm,
            onBack = { previewTarget = null }
        )
        return
    }

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
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(5),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(ui.characters, key = { it.character.id }) { character ->
                            SquareGridItem(
                                title = character.character.name,
                                onClick = { selected = character },
                                onLongClick = { bottomSheetTarget = character }
                            )
                        }
                    }
                }
            } else {
                val char = selected!!.character
                TagSummarySection(
                    categories = ui.categories,
                    tags = selected!!.tags,
                    onEdit = { showTagPicker = true }
                )
                Spacer(Modifier.height(12.dp))
                CharacterResourceFilterBar(
                    selectedType = selectedType,
                    selectedTagIds = selectedTagIds,
                    onTypeChange = { selectedType = it },
                    onTagDialog = { filterTagDialog = true }
                )
                val filteredResources = resources.resources.filter {
                    it.characters.any { c -> c.id == char.id }
                }.filter { res ->
                    val typeMatch = selectedType?.let { it == res.resource.type } ?: true
                    val tagMatch = if (selectedTagIds.isEmpty()) true else res.tags.any { selectedTagIds.contains(it.id) }
                    typeMatch && tagMatch
                }
                if (filteredResources.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("暂无资源")
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(filteredResources, key = { it.resource.id }) { resource ->
                            val tagLabel = resource.tags.joinToString("、") { it.name }.ifBlank { "无标签" }
                            val subtitle = "${typeLabel(resource.resource.type)}\n标签：$tagLabel"
                            SquareGridItem(
                                title = resource.resource.title,
                                subtitle = subtitle,
                                onClick = { previewTarget = resource },
                                onLongClick = {}
                            )
                        }
                    }
                }
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
    if (filterTagDialog) {
        TagFilterDialog(
            categories = ui.categories,
            tags = ui.tags,
            selectedIds = selectedTagIds,
            onConfirm = { selectedTagIds = it },
            onDismiss = { filterTagDialog = false }
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
private fun CharacterEditDialog(
    character: CharacterEntity,
    onConfirm: (CharacterEntity) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(character.name) }
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
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (name.isNotBlank()) onConfirm(character.copy(name = name.trim()))
                onDismiss()
            }) { Text("确定") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TagSummarySection(
    categories: List<TagCategoryEntity>,
    tags: List<TagEntity>,
    onEdit: () -> Unit
) {
    val grouped = tags.groupBy { it.categoryId }
    val categoryMap = categories.associateBy { it.id }
    Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("标签", style = MaterialTheme.typography.titleMedium)
            TextButton(onClick = onEdit) { Text("编辑标签") }
        }
        categories.forEach { category ->
            val items = grouped[category.id].orEmpty()
            if (items.isNotEmpty()) {
                Text(category.name, style = MaterialTheme.typography.labelMedium)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items.forEach { tag ->
                        TagBadge(tag = tag)
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
        }
        val uncategorized = tags.filter { it.categoryId !in categoryMap.keys }
        if (uncategorized.isNotEmpty()) {
            Text("其他", style = MaterialTheme.typography.labelMedium)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                uncategorized.forEach { tag ->
                    TagBadge(tag = tag)
                }
            }
        }
    }
}

@Composable
private fun TagBadge(tag: TagEntity) {
    val bg = Color(tag.colorArgb)
    val textColor = if (bg.luminance() < 0.5f) Color.White else Color.Black
    Box(
        modifier = Modifier
            .background(bg, shape = MaterialTheme.shapes.small)
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(text = tag.name, color = textColor, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun CharacterResourceFilterBar(
    selectedType: ResourceType?,
    selectedTagIds: Set<Long>,
    onTypeChange: (ResourceType?) -> Unit,
    onTagDialog: () -> Unit
) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ResourceType.values().forEach { type ->
                FilterChip(
                    selected = selectedType == type,
                    onClick = { onTypeChange(if (selectedType == type) null else type) },
                    label = { Text(typeLabel(type)) }
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        AssistChip(onClick = onTagDialog, label = { Text("标签筛选(${selectedTagIds.size})") })
    }
}

@Composable
private fun TagFilterDialog(
    categories: List<TagCategoryEntity>,
    tags: List<TagEntity>,
    selectedIds: Set<Long>,
    onConfirm: (Set<Long>) -> Unit,
    onDismiss: () -> Unit
) {
    var selected by remember { mutableStateOf(selectedIds) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("筛选标签") },
        text = {
            TagSelectionSection(
                label = "标签",
                categories = categories,
                tags = tags,
                selected = selected,
                onChange = { selected = it }
            )
        },
        confirmButton = {
            TextButton(onClick = {
                onConfirm(selected)
                onDismiss()
            }) { Text("确定") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

private fun typeLabel(type: ResourceType): String = when (type) {
    ResourceType.IMAGE -> "图片"
    ResourceType.VIDEO -> "视频"
    ResourceType.AUDIO -> "声音"
    ResourceType.QUOTE -> "文本"
    ResourceType.SCENE -> "情景"
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
