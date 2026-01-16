package com.example.quotepicker.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.quotepicker.data.CharacterEntity
import com.example.quotepicker.data.ResourceType
import com.example.quotepicker.data.ResourceWithTagsCharacters
import com.example.quotepicker.data.TagCategoryEntity
import com.example.quotepicker.data.TagEntity
import com.example.quotepicker.ui.components.ResourceGridCard
import com.example.quotepicker.ui.components.ResourcePreviewScreen
import com.example.quotepicker.ui.components.TagBadge
import com.example.quotepicker.ui.components.tagTextColor
import com.example.quotepicker.vm.ResourceViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResourceScreen(modifier: Modifier = Modifier, vm: ResourceViewModel = viewModel()) {
    val ui by vm.uiState.collectAsState()
    var showAddMenu by remember { mutableStateOf(false) }
    var createMode by remember { mutableStateOf<CreateMode?>(null) }
    var previewTarget by remember { mutableStateOf<ResourceWithTagsCharacters?>(null) }
    var editTarget by remember { mutableStateOf<ResourceWithTagsCharacters?>(null) }
    var bottomSheetTarget by remember { mutableStateOf<ResourceWithTagsCharacters?>(null) }
    var deleteTarget by remember { mutableStateOf<ResourceWithTagsCharacters?>(null) }
    var filterTagDialog by remember { mutableStateOf(false) }
    var filterCharacterDialog by remember { mutableStateOf(false) }

    createMode?.let { mode ->
        ResourceCreateScreen(
            mode = mode,
            categories = ui.categories,
            tags = ui.tags,
            characters = ui.characters,
            vm = vm,
            onBack = { createMode = null }
        )
        return
    }

    editTarget?.let { target ->
        ResourceEditScreen(
            resource = target,
            categories = ui.categories,
            tags = ui.tags,
            characters = ui.characters,
            vm = vm,
            onBack = { editTarget = null }
        )
        return
    }

    if (previewTarget != null) {
        ResourcePreviewScreen(
            resource = previewTarget!!,
            vm = vm,
            onBack = { previewTarget = null }
        )
        return
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("资源") },
                actions = {
                    IconButton(onClick = { filterTagDialog = true }) {
                        Icon(Icons.Default.Edit, contentDescription = "筛选标签")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddMenu = true }) {
                Icon(Icons.Default.Add, contentDescription = null)
            }
        }
    ) { inner ->
        Column(Modifier.padding(inner).fillMaxSize()) {
            FilterBar(
                selectedType = ui.filters.selectedType,
                selectedTagIds = ui.filters.selectedTagIds,
                characters = ui.characters,
                selectedCharacterId = ui.filters.selectedCharacterId,
                onTypeChange = vm::updateTypeFilter,
                onCharacterDialog = { filterCharacterDialog = true },
                onTagDialog = { filterTagDialog = true }
            )
            if (ui.resources.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("暂无资源，点击右下角添加")
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(ui.resources, key = { it.resource.id }) { res ->
                        ResourceGridCard(
                            title = res.resource.title,
                            typeLabel = typeLabel(res.resource.type),
                            tags = res.tags,
                            onClick = { previewTarget = res },
                            onLongClick = { bottomSheetTarget = res }
                        )
                    }
                }
            }
        }
    }

    if (showAddMenu) {
        ModalBottomSheet(onDismissRequest = { showAddMenu = false }) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                TextButton(onClick = { createMode = CreateMode.Text; showAddMenu = false }) { Text("创建文本") }
                TextButton(onClick = { createMode = CreateMode.ImageText; showAddMenu = false }) { Text("创建图片文本") }
                TextButton(onClick = { createMode = CreateMode.Scene; showAddMenu = false }) { Text("创建聊天情景") }
                TextButton(onClick = { createMode = CreateMode.Media(ResourceType.VIDEO); showAddMenu = false }) {
                    Text("创建视频文本")
                }
                TextButton(onClick = { createMode = CreateMode.Media(ResourceType.AUDIO); showAddMenu = false }) {
                    Text("创建声音文本")
                }
            }
        }
    }

    if (filterTagDialog) {
        FilterTagDialog(
            categories = ui.categories,
            tags = ui.tags,
            selectedIds = ui.filters.selectedTagIds,
            onConfirm = { vm.updateTagFilter(it) },
            onDismiss = { filterTagDialog = false }
        )
    }
    if (filterCharacterDialog) {
        FilterCharacterDialog(
            characters = ui.characters,
            selectedId = ui.filters.selectedCharacterId,
            onConfirm = { vm.updateCharacterFilter(it) },
            onDismiss = { filterCharacterDialog = false }
        )
    }

    bottomSheetTarget?.let { target ->
        ModalBottomSheet(onDismissRequest = { bottomSheetTarget = null }) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                TextButton(onClick = {
                    editTarget = target
                    bottomSheetTarget = null
                }) {
                    Icon(Icons.Default.Edit, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("编辑")
                }
                TextButton(onClick = {
                    deleteTarget = target
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

    deleteTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("删除资源") },
            text = { Text("确定删除“${target.resource.title}”吗？") },
            confirmButton = {
                TextButton(onClick = {
                    vm.deleteResource(target.resource)
                    deleteTarget = null
                }) { Text("删除") }
            },
            dismissButton = { TextButton(onClick = { deleteTarget = null }) { Text("取消") } }
        )
    }
}

private sealed class CreateMode {
    data object Text : CreateMode()
    data object ImageText : CreateMode()
    data object Scene : CreateMode()
    data class Media(val type: ResourceType) : CreateMode()
}

@Composable
private fun FilterBar(
    selectedType: ResourceType?,
    selectedTagIds: Set<Long>,
    characters: List<CharacterEntity>,
    selectedCharacterId: Long?,
    onTypeChange: (ResourceType?) -> Unit,
    onCharacterDialog: () -> Unit,
    onTagDialog: () -> Unit
) {
    Column(Modifier.fillMaxWidth().padding(12.dp)) {
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
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            AssistChip(onClick = onTagDialog, label = { Text("标签筛选(${selectedTagIds.size})") })
            val selectedCharacter = characters.firstOrNull { it.id == selectedCharacterId }
            AssistChip(
                onClick = onCharacterDialog,
                label = { Text(selectedCharacter?.name ?: "全部角色") }
            )
        }
    }
}

private fun typeLabel(type: ResourceType): String = when (type) {
    ResourceType.IMAGE -> "图片"
    ResourceType.VIDEO -> "视频"
    ResourceType.AUDIO -> "声音"
    ResourceType.QUOTE -> "文本"
    ResourceType.SCENE -> "情景"
}

@Composable
private fun FilterTagDialog(
    categories: List<TagCategoryEntity>,
    tags: List<TagEntity>,
    selectedIds: Set<Long>,
    onConfirm: (Set<Long>) -> Unit,
    onDismiss: () -> Unit
) {
    var selected by remember { mutableStateOf(selectedIds.toMutableSet()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择标签筛选") },
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
            TextButton(onClick = {
                onConfirm(selected)
                onDismiss()
            }) { Text("确定") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FilterCharacterDialog(
    characters: List<CharacterEntity>,
    selectedId: Long?,
    onConfirm: (Long?) -> Unit,
    onDismiss: () -> Unit
) {
    var current by remember { mutableStateOf(selectedId) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择角色筛选") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("角色")
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = current == null,
                        onClick = { current = null },
                        label = { Text("全部角色") },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = Color.White,
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                    characters.forEach { character ->
                        FilterChip(
                            selected = current == character.id,
                            onClick = { current = character.id },
                            label = { Text(character.name) },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = Color.White,
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onConfirm(current)
                onDismiss()
            }) { Text("确定") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun ResourceCreateScreen(
    mode: CreateMode,
    categories: List<TagCategoryEntity>,
    tags: List<TagEntity>,
    characters: List<CharacterEntity>,
    vm: ResourceViewModel,
    onBack: () -> Unit
) {
    var title by remember { mutableStateOf("") }
    var quoteText by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var sceneJson by remember { mutableStateOf("") }
    var selectedTags by remember { mutableStateOf(setOf<Long>()) }
    var selectedCharacters by remember { mutableStateOf(setOf<Long>()) }
    var imageUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var mediaUri by remember { mutableStateOf<Uri?>(null) }
    var showTagPicker by remember { mutableStateOf(false) }
    var showCharacterPicker by remember { mutableStateOf(false) }

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) {
        imageUris = it
    }
    val mediaPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        mediaUri = uri
    }

    val screenTitle = when (mode) {
        CreateMode.Text -> "创建文本"
        CreateMode.ImageText -> "创建图片文本"
        CreateMode.Scene -> "创建聊天情景"
        is CreateMode.Media -> "创建${typeLabel(mode.type)}文本"
    }

    val canSubmit = title.isNotBlank() && selectedCharacters.isNotEmpty() && when (mode) {
        CreateMode.Text -> true
        CreateMode.ImageText -> imageUris.isNotEmpty()
        CreateMode.Scene -> true
        is CreateMode.Media -> mediaUri != null
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(screenTitle) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    TextButton(onClick = {
                        when (mode) {
                            CreateMode.Text -> vm.addTextQuote(
                                title.trim(),
                                quoteText.trim(),
                                selectedTags.toList(),
                                selectedCharacters.toList()
                            )
                            CreateMode.ImageText -> vm.addImageQuote(
                                title.trim(),
                                imageUris,
                                selectedTags.toList(),
                                selectedCharacters.toList()
                            )
                            CreateMode.Scene -> vm.addScene(
                                title.trim(),
                                description.ifBlank { null },
                                sceneJson,
                                selectedTags.toList(),
                                selectedCharacters.toList()
                            )
                            is CreateMode.Media -> vm.addEncryptedMedia(
                                mode.type,
                                title.trim(),
                                mediaUri ?: return@TextButton,
                                selectedTags.toList(),
                                selectedCharacters.toList()
                            )
                        }
                        onBack()
                    }, enabled = canSubmit) { Text("创建") }
                }
            )
        }
    ) { inner ->
        Column(
            modifier = Modifier
                .padding(inner)
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("标题") },
                modifier = Modifier.fillMaxWidth()
            )
            when (mode) {
                CreateMode.Text -> {
                    OutlinedTextField(
                        value = quoteText,
                        onValueChange = { quoteText = it },
                        label = { Text("文本内容") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                CreateMode.ImageText -> {
                    TextButton(onClick = { imagePicker.launch("image/*") }) { Text("选择图片") }
                    Text(if (imageUris.isEmpty()) "未选择图片" else "已选择 ${imageUris.size} 张图片")
                }
                CreateMode.Scene -> {
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("描述(可选)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = sceneJson,
                        onValueChange = { sceneJson = it },
                        label = { Text("对话JSON") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = "示例：[{\"speaker\":\"小明\",\"text\":\"你好\"},{\"speaker\":\"小红\",\"text\":\"你好呀\"}]",
                        style = MaterialTheme.typography.labelSmall
                    )
                }
                is CreateMode.Media -> {
                    val label = if (mode.type == ResourceType.VIDEO) "选择视频" else "选择声音"
                    TextButton(onClick = { mediaPicker.launch(if (mode.type == ResourceType.VIDEO) "video/*" else "audio/*") }) {
                        Text(label)
                    }
                    Text(mediaUri?.toString() ?: "未选择文件")
                }
            }
            ResourceTagPickerRow(
                label = "标签",
                allTags = tags,
                selected = selectedTags,
                onPick = { showTagPicker = true }
            )
            ResourceCharacterPickerRow(
                label = "角色",
                characters = characters,
                selected = selectedCharacters,
                onPick = { showCharacterPicker = true }
            )
        }
    }

    if (showTagPicker) {
        TagPickerDialog(
            categories = categories,
            tags = tags,
            selectedIds = selectedTags,
            onConfirm = { selectedTags = it },
            onDismiss = { showTagPicker = false }
        )
    }
    if (showCharacterPicker) {
        CharacterPickerDialog(
            characters = characters,
            selectedIds = selectedCharacters,
            onConfirm = { selectedCharacters = it },
            onDismiss = { showCharacterPicker = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ResourceEditScreen(
    resource: ResourceWithTagsCharacters,
    categories: List<TagCategoryEntity>,
    tags: List<TagEntity>,
    characters: List<CharacterEntity>,
    vm: ResourceViewModel,
    onBack: () -> Unit
) {
    var title by remember { mutableStateOf(resource.resource.title) }
    var selectedTags by remember { mutableStateOf(resource.tags.map { it.id }.toSet()) }
    var selectedCharacters by remember { mutableStateOf(resource.characters.map { it.id }.toSet()) }
    var showTagPicker by remember { mutableStateOf(false) }
    var showCharacterPicker by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("编辑资源") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    TextButton(onClick = {
                        if (title.isNotBlank() && selectedCharacters.isNotEmpty()) {
                            vm.updateResource(resource.resource.copy(title = title.trim()))
                            vm.updateResourceTags(resource.resource.id, selectedTags.toList())
                            vm.updateResourceCharacters(resource.resource.id, selectedCharacters.toList())
                            onBack()
                        }
                    }) { Text("保存") }
                }
            )
        }
    ) { inner ->
        Column(
            modifier = Modifier
                .padding(inner)
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(text = typeLabel(resource.resource.type), style = MaterialTheme.typography.labelMedium)
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("标题") },
                modifier = Modifier.fillMaxWidth()
            )
            ResourceTagPickerRow(
                label = "标签",
                allTags = tags,
                selected = selectedTags,
                onPick = { showTagPicker = true }
            )
            ResourceCharacterPickerRow(
                label = "角色",
                characters = characters,
                selected = selectedCharacters,
                onPick = { showCharacterPicker = true }
            )
        }
    }

    if (showTagPicker) {
        TagPickerDialog(
            categories = categories,
            tags = tags,
            selectedIds = selectedTags,
            onConfirm = { selectedTags = it },
            onDismiss = { showTagPicker = false }
        )
    }
    if (showCharacterPicker) {
        CharacterPickerDialog(
            characters = characters,
            selectedIds = selectedCharacters,
            onConfirm = { selectedCharacters = it },
            onDismiss = { showCharacterPicker = false }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ResourceTagPickerRow(
    label: String,
    allTags: List<TagEntity>,
    selected: Set<Long>,
    onPick: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(label, style = MaterialTheme.typography.titleMedium)
            TextButton(onClick = onPick) { Text("选择") }
        }
        val selectedTags = allTags.filter { selected.contains(it.id) }
        if (selectedTags.isEmpty()) {
            Text("未选择标签", style = MaterialTheme.typography.labelMedium)
        } else {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                selectedTags.forEach { tag ->
                    TagBadge(tag = tag)
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ResourceCharacterPickerRow(
    label: String,
    characters: List<CharacterEntity>,
    selected: Set<Long>,
    onPick: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(label, style = MaterialTheme.typography.titleMedium)
            TextButton(onClick = onPick) { Text("选择") }
        }
        val selectedCharacters = characters.filter { selected.contains(it.id) }
        if (selectedCharacters.isEmpty()) {
            Text("未选择角色", style = MaterialTheme.typography.labelMedium)
        } else {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                selectedCharacters.forEach { character ->
                    AssistChip(onClick = {}, label = { Text(character.name) })
                }
            }
        }
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
        confirmButton = { TextButton(onClick = { onConfirm(selected); onDismiss() }) { Text("确定") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CharacterPickerDialog(
    characters: List<CharacterEntity>,
    selectedIds: Set<Long>,
    onConfirm: (Set<Long>) -> Unit,
    onDismiss: () -> Unit
) {
    var selected by remember { mutableStateOf(selectedIds.toMutableSet()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择角色") },
        text = {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                characters.forEach { character ->
                    val isSelected = selected.contains(character.id)
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            if (isSelected) selected.remove(character.id) else selected.add(character.id)
                            selected = selected.toMutableSet()
                        },
                        label = { Text(character.name) },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(selected); onDismiss() }) { Text("确定") } },
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
            Text("其他", style = MaterialTheme.typography.labelMedium)
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
    val tagColor = Color(tag.colorArgb)
    val selectedTextColor = tagTextColor(tagColor)
    FilterChip(
        selected = isSelected,
        onClick = {
            val newSet = selected.toMutableSet()
            if (isSelected) newSet.remove(tag.id) else newSet.add(tag.id)
            onChange(newSet)
        },
        label = { Text(tag.name) },
        colors = FilterChipDefaults.filterChipColors(
            containerColor = tagColor.copy(alpha = 0.2f),
            selectedContainerColor = tagColor,
            labelColor = MaterialTheme.colorScheme.onSurface,
            selectedLabelColor = selectedTextColor
        )
    )
}
