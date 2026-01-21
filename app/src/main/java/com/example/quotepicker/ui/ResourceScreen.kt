package com.example.quotepicker.ui

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Videocam
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
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.draw.clip
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.quotepicker.data.CharacterEntity
import com.example.quotepicker.data.ResourceType
import com.example.quotepicker.data.ResourceWithTagsCharacters
import com.example.quotepicker.data.TagCategoryEntity
import com.example.quotepicker.data.TagEntity
import com.example.quotepicker.ui.components.CharacterBadge
import com.example.quotepicker.ui.components.ResourceGridCard
import com.example.quotepicker.ui.components.ResourcePreviewScreen
import com.example.quotepicker.ui.components.TagBadge
import com.example.quotepicker.ui.components.rememberFormattedText
import com.example.quotepicker.ui.components.tagTextColor
import com.example.quotepicker.vm.FlowUpdateItem
import com.example.quotepicker.vm.ImageUpdateItem
import com.example.quotepicker.vm.ResourceViewModel
import com.example.quotepicker.vm.SceneMessageDraft
import com.example.quotepicker.vm.StoredMediaItem
import com.example.quotepicker.vm.VideoUpdateItem
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ResourceScreen(modifier: Modifier = Modifier, vm: ResourceViewModel = viewModel()) {
    val ui by vm.uiState.collectAsState()
    val allResources by vm.allResources.collectAsState()
    var showAddMenu by remember { mutableStateOf(false) }
    var createMode by remember { mutableStateOf<CreateMode?>(null) }
    var previewTarget by remember { mutableStateOf<ResourceWithTagsCharacters?>(null) }
    var editTarget by remember { mutableStateOf<ResourceWithTagsCharacters?>(null) }
    var bottomSheetTarget by remember { mutableStateOf<ResourceWithTagsCharacters?>(null) }
    var deleteTarget by remember { mutableStateOf<ResourceWithTagsCharacters?>(null) }
    var filterTagDialog by remember { mutableStateOf(false) }
    var filterCharacterDialog by remember { mutableStateOf(false) }
    var manageScreen by remember { mutableStateOf(false) }
    var manageItems by remember { mutableStateOf<List<StoredMediaItem>>(emptyList()) }
    var restoreTarget by remember { mutableStateOf<StoredMediaItem?>(null) }
    val coroutineScope = rememberCoroutineScope()
    val restorePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        val target = restoreTarget
        if (uri != null && target != null) {
            coroutineScope.launch {
                vm.restoreMediaToDirectory(target.path, target.type, uri)
            }
        }
        restoreTarget = null
    }

    if (manageScreen) {
        ManageStorageScreen(
            items = manageItems,
            vm = vm,
            onBack = { manageScreen = false },
            onRestore = { item ->
                restoreTarget = item
                restorePicker.launch(null)
            },
            onRefresh = {
                manageItems = vm.listStoredMedia()
            }
        )
        return
    }

    createMode?.let { mode ->
        ResourceCreateScreen(
            mode = mode,
            categories = ui.categories,
            tags = ui.tags,
            characters = ui.characters,
            availableResources = allResources,
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
            availableResources = allResources,
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
                    IconButton(onClick = {
                        manageItems = vm.listStoredMedia()
                        manageScreen = true
                    }) {
                        Icon(Icons.Default.Settings, contentDescription = "管理资源")
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
                    gridItems(ui.resources, key = { it.resource.id }) { res ->
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
                TextButton(onClick = { createMode = CreateMode.Flow; showAddMenu = false }) {
                    Icon(Icons.Default.Description, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("创建流程")
                }
                TextButton(onClick = { createMode = CreateMode.Text; showAddMenu = false }) {
                    Icon(Icons.Default.Description, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("创建文本")
                }
                TextButton(onClick = { createMode = CreateMode.Scene; showAddMenu = false }) {
                    Icon(Icons.Default.Chat, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("创建情景")
                }
                TextButton(onClick = { createMode = CreateMode.ImageGroup; showAddMenu = false }) {
                    Icon(Icons.Default.Image, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("上传图片组")
                }
                TextButton(onClick = { createMode = CreateMode.VideoGroup; showAddMenu = false }) {
                    Icon(Icons.Default.Videocam, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("上传视频组")
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ManageStorageScreen(
    items: List<StoredMediaItem>,
    vm: ResourceViewModel,
    onBack: () -> Unit,
    onRestore: (StoredMediaItem) -> Unit,
    onRefresh: () -> Unit
) {
    val context = LocalContext.current
    val imagesDir = remember { File(context.filesDir, "images").absolutePath }
    val videosDir = remember { File(context.filesDir, "videos").absolutePath }
    var displayType by remember { mutableStateOf(ResourceType.IMAGE) }
    var actionTarget by remember { mutableStateOf<StoredMediaItem?>(null) }
    var deleteTarget by remember { mutableStateOf<StoredMediaItem?>(null) }
    val coroutineScope = rememberCoroutineScope()
    val filteredItems = remember(items, displayType) { items.filter { it.type == displayType } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("资源存储") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    TextButton(onClick = {
                        displayType = if (displayType == ResourceType.IMAGE) {
                            ResourceType.VIDEO
                        } else {
                            ResourceType.IMAGE
                        }
                    }) {
                        Text(if (displayType == ResourceType.IMAGE) "显示视频" else "显示图片")
                    }
                    TextButton(onClick = onRefresh) { Text("刷新") }
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
            Text("App 资源存储路径", style = MaterialTheme.typography.titleMedium)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("图片目录：$imagesDir", style = MaterialTheme.typography.labelSmall)
                Text("视频目录：$videosDir", style = MaterialTheme.typography.labelSmall)
            }
            Text("长按文件可选择恢复或删除", style = MaterialTheme.typography.labelSmall)
            if (filteredItems.isEmpty()) {
                Text("暂无文件", style = MaterialTheme.typography.labelMedium)
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(filteredItems, key = { it.path }) { item ->
                        StorageMediaRow(
                            item = item,
                            vm = vm,
                            onLongPress = { actionTarget = item }
                        )
                    }
                }
            }
        }
    }

    actionTarget?.let { target ->
        ModalBottomSheet(onDismissRequest = { actionTarget = null }) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TextButton(onClick = {
                    actionTarget = null
                    onRestore(target)
                }) {
                    Icon(Icons.Default.Settings, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("恢复到目录")
                }
                TextButton(onClick = {
                    actionTarget = null
                    deleteTarget = target
                }) {
                    Icon(Icons.Default.Delete, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("删除")
                }
            }
            Spacer(Modifier.height(12.dp))
        }
    }

    deleteTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("删除文件") },
            text = { Text("确定删除该文件吗？") },
            confirmButton = {
                TextButton(onClick = {
                    deleteTarget = null
                    coroutineScope.launch(Dispatchers.IO) {
                        vm.deleteStoredMedia(target)
                        withContext(Dispatchers.Main) {
                            onRefresh()
                        }
                    }
                }) { Text("删除") }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text("取消") }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun StorageMediaRow(
    item: StoredMediaItem,
    vm: ResourceViewModel,
    onLongPress: (StoredMediaItem) -> Unit
) {
    val thumbnail by produceState<android.graphics.Bitmap?>(initialValue = null, item.path) {
        value = if (item.type == ResourceType.IMAGE) {
            withContext(Dispatchers.IO) { vm.decodeUriToBitmap(Uri.parse(item.path)) }
        } else {
            null
        }
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = {},
                onLongClick = { onLongPress(item) }
            )
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (item.type == ResourceType.IMAGE && thumbnail != null) {
            Image(
                bitmap = thumbnail!!.asImageBitmap(),
                contentDescription = "图片预览",
                modifier = Modifier
                    .size(56.dp)
                    .clip(MaterialTheme.shapes.small)
            )
        } else {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(MaterialTheme.shapes.small),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (item.type == ResourceType.IMAGE) Icons.Default.Image else Icons.Default.Videocam,
                    contentDescription = null
                )
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = if (item.type == ResourceType.IMAGE) "图片" else "视频",
                style = MaterialTheme.typography.labelMedium
            )
            Text(
                text = item.path,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private sealed class CreateMode {
    data object Flow : CreateMode()
    data object Text : CreateMode()
    data object ImageGroup : CreateMode()
    data object Scene : CreateMode()
    data object VideoGroup : CreateMode()
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
            val orderedTypes = listOf(
                ResourceType.FLOW,
                ResourceType.TEXT,
                ResourceType.IMAGE,
                ResourceType.VIDEO,
                ResourceType.SCENE
            )
            orderedTypes.forEach { type ->
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
    ResourceType.FLOW -> "流程"
    ResourceType.IMAGE -> "图片"
    ResourceType.VIDEO -> "视频"
    ResourceType.TEXT -> "文本"
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
    availableResources: List<ResourceWithTagsCharacters>,
    vm: ResourceViewModel,
    onBack: () -> Unit
) {
    var title by remember { mutableStateOf("") }
    var textContent by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var sceneMessages by remember { mutableStateOf<List<SceneMessageDraft>>(emptyList()) }
    var flowItems by remember { mutableStateOf<List<FlowUpdateItem>>(emptyList()) }
    var selectedTags by remember { mutableStateOf(setOf<Long>()) }
    var selectedCharacters by remember { mutableStateOf(setOf<Long>()) }
    var imageUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var videoUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var showTagPicker by remember { mutableStateOf(false) }
    var showCharacterPicker by remember { mutableStateOf(false) }
    var showSceneDialog by remember { mutableStateOf(false) }
    var editSceneIndex by remember { mutableStateOf<Int?>(null) }
    var showFlowDialog by remember { mutableStateOf(false) }
    var editFlowIndex by remember { mutableStateOf<Int?>(null) }
    val scrollState = rememberScrollState()

    val context = LocalContext.current
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        imageUris = uris
        uris.forEach { uri ->
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
        }
    }
    val videoPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        videoUris = uris
        uris.forEach { uri ->
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
        }
    }

    val screenTitle = when (mode) {
        CreateMode.Flow -> "创建流程"
        CreateMode.Text -> "创建文本"
        CreateMode.ImageGroup -> "上传图片组"
        CreateMode.Scene -> "创建情景"
        CreateMode.VideoGroup -> "上传视频组"
    }

    val titleLabel = when (mode) {
        CreateMode.Flow -> "流程名"
        CreateMode.Text -> "文本名"
        CreateMode.Scene -> "情景名"
        CreateMode.ImageGroup -> "名称"
        CreateMode.VideoGroup -> "名称"
    }

    val canSubmit = title.isNotBlank() && selectedCharacters.isNotEmpty() && when (mode) {
        CreateMode.Flow -> flowItems.isNotEmpty()
        CreateMode.Text -> textContent.isNotBlank()
        CreateMode.ImageGroup -> imageUris.isNotEmpty()
        CreateMode.Scene -> sceneMessages.isNotEmpty()
        CreateMode.VideoGroup -> videoUris.isNotEmpty()
    }

    val selectedSpeakerNames = remember(selectedCharacters, characters) {
        characters.filter { selectedCharacters.contains(it.id) }.map { it.name }
    }
    val flowResources = remember(availableResources) {
        availableResources.filter { it.resource.type != ResourceType.FLOW }
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
                            CreateMode.Flow -> vm.addFlow(
                                title.trim(),
                                flowItems,
                                selectedTags.toList(),
                                selectedCharacters.toList()
                            )
                            CreateMode.Text -> vm.addTextResource(
                                title.trim(),
                                textContent.trim(),
                                selectedTags.toList(),
                                selectedCharacters.toList()
                            )
                            CreateMode.ImageGroup -> vm.addImageGroup(
                                title.trim(),
                                imageUris,
                                selectedTags.toList(),
                                selectedCharacters.toList()
                            )
                            CreateMode.Scene -> vm.addScene(
                                title.trim(),
                                description.ifBlank { null },
                                buildSceneJson(sceneMessages),
                                selectedTags.toList(),
                                selectedCharacters.toList()
                            )
                            CreateMode.VideoGroup -> vm.addVideoGroup(
                                title.trim(),
                                videoUris,
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
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text(titleLabel) },
                modifier = Modifier.fillMaxWidth()
            )
            when (mode) {
                CreateMode.Flow -> {
                    TextButton(onClick = { showFlowDialog = true }) {
                        Icon(Icons.Default.Description, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("添加步骤")
                    }
                    if (flowItems.isEmpty()) {
                        Text("暂无步骤", style = MaterialTheme.typography.labelSmall)
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            flowItems.forEachIndexed { index, item ->
                                androidx.compose.material3.OutlinedCard {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(
                                            modifier = Modifier.weight(1f),
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text(
                                                text = item.title?.ifBlank { null } ?: typeLabel(item.type),
                                                style = MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            val summary = when (item.type) {
                                                ResourceType.TEXT -> rememberFormattedText(item.text.orEmpty())
                                                ResourceType.SCENE -> "对话 ${item.sceneMessages.size} 条"
                                                ResourceType.IMAGE -> "图片 ${item.images.size} 张"
                                                ResourceType.VIDEO -> "视频 ${item.videos.size} 个"
                                                else -> ""
                                            }
                                            if (summary.isNotBlank()) {
                                                Text(text = summary, style = MaterialTheme.typography.bodySmall)
                                            }
                                        }
                                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            IconButton(onClick = {
                                                editFlowIndex = index
                                                showFlowDialog = true
                                            }) {
                                                Icon(Icons.Default.Edit, contentDescription = "编辑")
                                            }
                                            IconButton(onClick = {
                                                flowItems = flowItems.toMutableList().also { it.removeAt(index) }
                                            }) {
                                                Icon(Icons.Default.Delete, contentDescription = "删除")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                CreateMode.Text -> {
                    OutlinedTextField(
                        value = textContent,
                        onValueChange = { textContent = it },
                        label = { Text("文本内容") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                CreateMode.ImageGroup -> {
                    TextButton(onClick = { imagePicker.launch(arrayOf("image/*")) }) {
                        Icon(Icons.Default.Image, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("选择图片")
                    }
                    Text(if (imageUris.isEmpty()) "未选择图片" else "已选择 ${imageUris.size} 张图片")
                }
                CreateMode.Scene -> {
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("描述(可选)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    TextButton(onClick = { showSceneDialog = true }, enabled = selectedSpeakerNames.isNotEmpty()) {
                        Icon(Icons.Default.Chat, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("添加对话")
                    }
                    if (sceneMessages.isEmpty()) {
                        Text("暂无对话", style = MaterialTheme.typography.labelSmall)
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            sceneMessages.forEachIndexed { index, message ->
                                androidx.compose.material3.OutlinedCard {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(
                                            modifier = Modifier.weight(1f),
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text(
                                                text = message.speaker.ifBlank { "角色" },
                                                style = MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            Text(text = message.content)
                                        }
                                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            IconButton(onClick = {
                                                editSceneIndex = index
                                                showSceneDialog = true
                                            }) {
                                                Icon(Icons.Default.Edit, contentDescription = "编辑")
                                            }
                                            IconButton(onClick = {
                                                sceneMessages = sceneMessages.toMutableList().also { it.removeAt(index) }
                                            }) {
                                                Icon(Icons.Default.Delete, contentDescription = "删除")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                CreateMode.VideoGroup -> {
                    TextButton(onClick = { videoPicker.launch(arrayOf("video/*")) }) {
                        Icon(Icons.Default.Videocam, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("选择视频")
                    }
                    Text(if (videoUris.isEmpty()) "未选择视频" else "已选择 ${videoUris.size} 个视频")
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
    if (showSceneDialog) {
        SceneMessageDialog(
            title = if (editSceneIndex == null) "添加对话" else "编辑对话",
            allowedSpeakers = selectedSpeakerNames,
            initialSpeaker = editSceneIndex?.let { sceneMessages.getOrNull(it)?.speaker }.orEmpty(),
            initialContent = editSceneIndex?.let { sceneMessages.getOrNull(it)?.content }.orEmpty(),
            onConfirm = { speaker, content ->
                if (speaker.isBlank() && content.isBlank()) {
                    showSceneDialog = false
                    editSceneIndex = null
                } else {
                    val updated = sceneMessages.toMutableList()
                    if (editSceneIndex != null) {
                        updated[editSceneIndex!!] = SceneMessageDraft(speaker.trim(), content.trim())
                    } else {
                        updated.add(SceneMessageDraft(speaker.trim(), content.trim()))
                    }
                    sceneMessages = updated
                    showSceneDialog = false
                    editSceneIndex = null
                }
            },
            onAddAnother = if (editSceneIndex == null) {
                { speaker, content ->
                    if (speaker.isNotBlank() || content.isNotBlank()) {
                        val updated = sceneMessages.toMutableList()
                        updated.add(SceneMessageDraft(speaker.trim(), content.trim()))
                        sceneMessages = updated
                    }
                }
            } else {
                null
            },
            onDismiss = {
                showSceneDialog = false
                editSceneIndex = null
            }
        )
    }
    if (showFlowDialog) {
        FlowStepDialog(
            initialItem = editFlowIndex?.let { flowItems.getOrNull(it) },
            availableResources = flowResources,
            onConfirm = { item ->
                val updated = flowItems.toMutableList()
                if (editFlowIndex != null) {
                    updated[editFlowIndex!!] = item
                } else {
                    updated.add(item)
                }
                flowItems = updated
                showFlowDialog = false
                editFlowIndex = null
            },
            onDismiss = {
                showFlowDialog = false
                editFlowIndex = null
            }
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
    availableResources: List<ResourceWithTagsCharacters>,
    vm: ResourceViewModel,
    onBack: () -> Unit
) {
    var title by remember { mutableStateOf(resource.resource.title) }
    var selectedTags by remember { mutableStateOf(resource.tags.map { it.id }.toSet()) }
    var selectedCharacters by remember { mutableStateOf(resource.characters.map { it.id }.toSet()) }
    var showTagPicker by remember { mutableStateOf(false) }
    var showCharacterPicker by remember { mutableStateOf(false) }
    var textContent by remember { mutableStateOf(resource.resource.quoteText.orEmpty()) }
    var description by remember { mutableStateOf(resource.resource.quoteText.orEmpty()) }
    var sceneMessages by remember { mutableStateOf(parseSceneMessages(resource.resource.sceneJson)) }
    var imageItems by remember {
        mutableStateOf(parseImageItems(resource.resource.contentUriOrPath, resource.resource.quoteImageBase64))
    }
    var videoItems by remember { mutableStateOf(parseVideoItems(resource.resource.contentUriOrPath)) }
    var flowItems by remember { mutableStateOf(parseFlowItems(resource.resource.sceneJson)) }
    var showSceneDialog by remember { mutableStateOf(false) }
    var editSceneIndex by remember { mutableStateOf<Int?>(null) }
    var showFlowDialog by remember { mutableStateOf(false) }
    var editFlowIndex by remember { mutableStateOf<Int?>(null) }

    val context = LocalContext.current
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        val updated = imageItems.toMutableList()
        uris.forEach { uri ->
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
            updated.add(ImageUpdateItem(uri = uri))
        }
        imageItems = updated
    }
    val videoPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        val updated = videoItems.toMutableList()
        uris.forEach { uri ->
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
            updated.add(VideoUpdateItem(uri = uri))
        }
        videoItems = updated
    }

    val selectedSpeakerNames = remember(selectedCharacters, characters) {
        characters.filter { selectedCharacters.contains(it.id) }.map { it.name }
    }
    val flowResources = remember(availableResources) {
        availableResources.filter { it.resource.type != ResourceType.FLOW }
    }

    val canSave = title.isNotBlank() && selectedCharacters.isNotEmpty() && when (resource.resource.type) {
        ResourceType.FLOW -> flowItems.isNotEmpty()
        ResourceType.TEXT -> textContent.isNotBlank()
        ResourceType.IMAGE -> imageItems.isNotEmpty()
        ResourceType.VIDEO -> videoItems.isNotEmpty()
        ResourceType.SCENE -> sceneMessages.isNotEmpty()
    }

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
                        when (resource.resource.type) {
                            ResourceType.FLOW -> vm.updateFlow(
                                resource.resource,
                                title.trim(),
                                flowItems,
                                selectedTags.toList(),
                                selectedCharacters.toList()
                            )
                            ResourceType.TEXT -> vm.updateTextResource(
                                resource.resource,
                                title.trim(),
                                textContent.trim(),
                                selectedTags.toList(),
                                selectedCharacters.toList()
                            )
                            ResourceType.IMAGE -> vm.updateImageGroup(
                                resource.resource,
                                title.trim(),
                                imageItems,
                                selectedTags.toList(),
                                selectedCharacters.toList()
                            )
                            ResourceType.VIDEO -> vm.updateVideoGroup(
                                resource.resource,
                                title.trim(),
                                videoItems,
                                selectedTags.toList(),
                                selectedCharacters.toList()
                            )
                            ResourceType.SCENE -> vm.updateSceneResource(
                                resource.resource,
                                title.trim(),
                                description.ifBlank { null },
                                buildSceneJson(sceneMessages),
                                selectedTags.toList(),
                                selectedCharacters.toList()
                            )
                        }
                        onBack()
                    }, enabled = canSave) { Text("保存") }
                }
            )
        }
    ) { inner ->
        Column(
            modifier = Modifier
                .padding(inner)
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(text = typeLabel(resource.resource.type), style = MaterialTheme.typography.labelMedium)
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("名称") },
                modifier = Modifier.fillMaxWidth()
            )
            when (resource.resource.type) {
                ResourceType.TEXT -> {
                    OutlinedTextField(
                        value = textContent,
                        onValueChange = { textContent = it },
                        label = { Text("文本内容") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                ResourceType.IMAGE -> {
                    TextButton(onClick = { imagePicker.launch(arrayOf("image/*")) }) {
                        Icon(Icons.Default.Image, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("追加图片")
                    }
                    if (imageItems.isEmpty()) {
                        Text("暂无图片", style = MaterialTheme.typography.labelSmall)
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            imageItems.forEachIndexed { index, item ->
                                val thumbnail by produceState<android.graphics.Bitmap?>(initialValue = null, item) {
                                    value = withContext(Dispatchers.IO) {
                                        when {
                                            item.base64 != null -> vm.decodeBase64ToBitmap(item.base64)
                                            item.uri != null -> vm.decodeUriToBitmap(item.uri)
                                            item.path != null -> vm.decodeUriToBitmap(Uri.parse(item.path))
                                            else -> null
                                        }
                                    }
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (thumbnail != null) {
                                        Image(
                                            bitmap = thumbnail!!.asImageBitmap(),
                                            contentDescription = "图片预览",
                                            modifier = Modifier
                                                .size(48.dp)
                                                .clip(MaterialTheme.shapes.small)
                                        )
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .size(48.dp)
                                                .clip(MaterialTheme.shapes.small),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(Icons.Default.Image, contentDescription = null)
                                        }
                                    }
                                    Spacer(Modifier.width(12.dp))
                                    Text(
                                        text = "图片 ${index + 1}",
                                        modifier = Modifier.weight(1f)
                                    )
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        IconButton(onClick = {
                                            if (index > 0) {
                                                imageItems = imageItems.toMutableList().also {
                                                    it.add(index - 1, it.removeAt(index))
                                                }
                                            }
                                        }) {
                                            Icon(Icons.Default.KeyboardArrowUp, contentDescription = "上移")
                                        }
                                        IconButton(onClick = {
                                            if (index < imageItems.lastIndex) {
                                                imageItems = imageItems.toMutableList().also {
                                                    it.add(index + 1, it.removeAt(index))
                                                }
                                            }
                                        }) {
                                            Icon(Icons.Default.KeyboardArrowDown, contentDescription = "下移")
                                        }
                                        IconButton(onClick = {
                                            imageItems = imageItems.toMutableList().also { it.removeAt(index) }
                                        }) {
                                            Icon(Icons.Default.Delete, contentDescription = "删除")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                ResourceType.VIDEO -> {
                    TextButton(onClick = { videoPicker.launch(arrayOf("video/*")) }) {
                        Icon(Icons.Default.Videocam, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("追加视频")
                    }
                    if (videoItems.isEmpty()) {
                        Text("暂无视频", style = MaterialTheme.typography.labelSmall)
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            videoItems.forEachIndexed { index, item ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val label = item.path?.let { "视频 ${index + 1}" } ?: "新视频 ${index + 1}"
                                    Text(text = label)
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        IconButton(onClick = {
                                            if (index > 0) {
                                                videoItems = videoItems.toMutableList().also {
                                                    it.add(index - 1, it.removeAt(index))
                                                }
                                            }
                                        }) {
                                            Icon(Icons.Default.KeyboardArrowUp, contentDescription = "上移")
                                        }
                                        IconButton(onClick = {
                                            if (index < videoItems.lastIndex) {
                                                videoItems = videoItems.toMutableList().also {
                                                    it.add(index + 1, it.removeAt(index))
                                                }
                                            }
                                        }) {
                                            Icon(Icons.Default.KeyboardArrowDown, contentDescription = "下移")
                                        }
                                        IconButton(onClick = {
                                            videoItems = videoItems.toMutableList().also { it.removeAt(index) }
                                        }) {
                                            Icon(Icons.Default.Delete, contentDescription = "删除")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                ResourceType.SCENE -> {
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("描述(可选)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    TextButton(onClick = { showSceneDialog = true }, enabled = selectedSpeakerNames.isNotEmpty()) {
                        Icon(Icons.Default.Chat, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("追加对话")
                    }
                    if (sceneMessages.isEmpty()) {
                        Text("暂无对话", style = MaterialTheme.typography.labelSmall)
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            sceneMessages.forEachIndexed { index, message ->
                                androidx.compose.material3.OutlinedCard {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(
                                            modifier = Modifier.weight(1f),
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text(
                                                text = message.speaker.ifBlank { "角色" },
                                                style = MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            Text(text = message.content)
                                        }
                                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            IconButton(onClick = {
                                                editSceneIndex = index
                                                showSceneDialog = true
                                            }) {
                                                Icon(Icons.Default.Edit, contentDescription = "编辑")
                                            }
                                            IconButton(onClick = {
                                                sceneMessages = sceneMessages.toMutableList().also { it.removeAt(index) }
                                            }) {
                                                Icon(Icons.Default.Delete, contentDescription = "删除")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                ResourceType.FLOW -> {
                    TextButton(onClick = { showFlowDialog = true }) {
                        Icon(Icons.Default.Description, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("追加步骤")
                    }
                    if (flowItems.isEmpty()) {
                        Text("暂无步骤", style = MaterialTheme.typography.labelSmall)
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            flowItems.forEachIndexed { index, item ->
                                androidx.compose.material3.OutlinedCard {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(
                                            modifier = Modifier.weight(1f),
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text(
                                                text = typeLabel(item.type),
                                                style = MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            val summary = when (item.type) {
                                                ResourceType.TEXT -> rememberFormattedText(item.text.orEmpty())
                                                ResourceType.SCENE -> "对话 ${item.sceneMessages.size} 条"
                                                ResourceType.IMAGE -> "图片 ${item.images.size} 张"
                                                ResourceType.VIDEO -> "视频 ${item.videos.size} 个"
                                                else -> ""
                                            }
                                            if (summary.isNotBlank()) {
                                                Text(text = summary, style = MaterialTheme.typography.bodySmall)
                                            }
                                        }
                                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            IconButton(onClick = {
                                                editFlowIndex = index
                                                showFlowDialog = true
                                            }) {
                                                Icon(Icons.Default.Edit, contentDescription = "编辑")
                                            }
                                            IconButton(onClick = {
                                                flowItems = flowItems.toMutableList().also { it.removeAt(index) }
                                            }) {
                                                Icon(Icons.Default.Delete, contentDescription = "删除")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
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
    if (showSceneDialog) {
        SceneMessageDialog(
            title = if (editSceneIndex == null) "添加对话" else "编辑对话",
            allowedSpeakers = selectedSpeakerNames,
            initialSpeaker = editSceneIndex?.let { sceneMessages.getOrNull(it)?.speaker }.orEmpty(),
            initialContent = editSceneIndex?.let { sceneMessages.getOrNull(it)?.content }.orEmpty(),
            onConfirm = { speaker, content ->
                if (speaker.isBlank() && content.isBlank()) {
                    showSceneDialog = false
                    editSceneIndex = null
                } else {
                    val updated = sceneMessages.toMutableList()
                    if (editSceneIndex != null) {
                        updated[editSceneIndex!!] = SceneMessageDraft(speaker.trim(), content.trim())
                    } else {
                        updated.add(SceneMessageDraft(speaker.trim(), content.trim()))
                    }
                    sceneMessages = updated
                    showSceneDialog = false
                    editSceneIndex = null
                }
            },
            onAddAnother = if (editSceneIndex == null) {
                { speaker, content ->
                    if (speaker.isNotBlank() || content.isNotBlank()) {
                        val updated = sceneMessages.toMutableList()
                        updated.add(SceneMessageDraft(speaker.trim(), content.trim()))
                        sceneMessages = updated
                    }
                }
            } else {
                null
            },
            onDismiss = {
                showSceneDialog = false
                editSceneIndex = null
            }
        )
    }
    if (showFlowDialog) {
        FlowStepDialog(
            initialItem = editFlowIndex?.let { flowItems.getOrNull(it) },
            availableResources = flowResources,
            onConfirm = { item ->
                val updated = flowItems.toMutableList()
                if (editFlowIndex != null) {
                    updated[editFlowIndex!!] = item
                } else {
                    updated.add(item)
                }
                flowItems = updated
                showFlowDialog = false
                editFlowIndex = null
            },
            onDismiss = {
                showFlowDialog = false
                editFlowIndex = null
            }
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
                    CharacterBadge(name = character.name)
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
            Text("未分类", style = MaterialTheme.typography.labelMedium)
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SceneMessageDialog(
    title: String,
    allowedSpeakers: List<String>,
    initialSpeaker: String,
    initialContent: String,
    onConfirm: (String, String) -> Unit,
    onAddAnother: ((String, String) -> Unit)?,
    onDismiss: () -> Unit
) {
    var speaker by remember(initialSpeaker, allowedSpeakers) {
        mutableStateOf(if (initialSpeaker in allowedSpeakers) initialSpeaker else allowedSpeakers.firstOrNull().orEmpty())
    }
    var content by remember(initialContent) { mutableStateOf(initialContent) }
    val canPickSpeaker = allowedSpeakers.isNotEmpty()
    val hasContent = speaker.isNotBlank() && content.isNotBlank()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (allowedSpeakers.isEmpty()) {
                    Text("请先选择角色再添加对话", style = MaterialTheme.typography.labelMedium)
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("角色")
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            allowedSpeakers.forEach { name ->
                                FilterChip(
                                    selected = speaker == name,
                                    onClick = { speaker = name },
                                    label = { Text(name) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                )
                            }
                        }
                    }
                }
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("说话内容") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (onAddAnother != null) {
                    TextButton(
                        onClick = {
                            onAddAnother(speaker, content)
                            speaker = ""
                            content = ""
                        },
                        enabled = hasContent && canPickSpeaker
                    ) { Text("继续添加") }
                }
                TextButton(onClick = { onConfirm(speaker, content) }, enabled = hasContent && canPickSpeaker) { Text("确定") }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun FlowStepDialog(
    initialItem: FlowUpdateItem?,
    availableResources: List<ResourceWithTagsCharacters>,
    onConfirm: (FlowUpdateItem) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedType by remember { mutableStateOf(initialItem?.type ?: ResourceType.TEXT) }
    val initialResourceId = remember(initialItem, availableResources) {
        initialItem?.resourceId
            ?: availableResources.firstOrNull {
                it.resource.title == initialItem?.title && it.resource.type == initialItem?.type
            }?.resource?.id
    }
    var selectedResourceId by remember(initialResourceId) { mutableStateOf(initialResourceId) }

    val resourcesOfType = remember(selectedType, availableResources) {
        availableResources.filter { it.resource.type == selectedType }
    }
    val selectedResource = resourcesOfType.firstOrNull { it.resource.id == selectedResourceId }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("流程步骤") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("步骤类型")
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(ResourceType.TEXT, ResourceType.SCENE, ResourceType.IMAGE, ResourceType.VIDEO).forEach { type ->
                        FilterChip(
                            selected = selectedType == type,
                            onClick = {
                                selectedType = type
                                selectedResourceId = null
                            },
                            label = { Text(typeLabel(type)) }
                        )
                    }
                }
                Text("选择资源")
                if (resourcesOfType.isEmpty()) {
                    Text("暂无可用资源", style = MaterialTheme.typography.labelSmall)
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        resourcesOfType.forEach { res ->
                            val isSelected = selectedResourceId == res.resource.id
                            androidx.compose.material3.OutlinedCard(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedResourceId = res.resource.id }
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = res.resource.title,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                    val summary = resourceSummary(res)
                                    if (summary.isNotBlank()) {
                                        Text(summary, style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { selectedResource?.let { onConfirm(flowItemFromResource(it)) } },
                enabled = selectedResource != null
            ) { Text("确定") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

private fun parseSceneMessages(raw: String?): List<SceneMessageDraft> {
    if (raw.isNullOrBlank()) return emptyList()
    return runCatching {
        val array = org.json.JSONArray(raw)
        buildList {
            for (i in 0 until array.length()) {
                val obj = array.optJSONObject(i) ?: continue
                val speaker = obj.optString("speaker")
                val content = obj.optString("text")
                if (speaker.isNotBlank() || content.isNotBlank()) {
                    add(SceneMessageDraft(speaker.ifBlank { "角色" }, content))
                }
            }
        }
    }.getOrDefault(emptyList())
}

private fun parseImageItems(payload: String?): List<ImageUpdateItem> {
    return parseImageItems(payload, null)
}

private fun parseImageItems(pathPayload: String?, base64Payload: String?): List<ImageUpdateItem> {
    val payload = pathPayload?.takeIf { it.isNotBlank() } ?: base64Payload
    if (payload.isNullOrBlank()) return emptyList()
    val trimmed = payload.trim()
    val parsedList = if (trimmed.startsWith("[")) {
        runCatching {
            val array = org.json.JSONArray(trimmed)
            buildList {
                for (i in 0 until array.length()) {
                    when (val entry = array.get(i)) {
                        is org.json.JSONObject -> {
                            val image = entry.optString("image")
                            val motion = entry.optString("motionVideo")
                            if (image.isNotBlank()) {
                                add(ImageUpdateItem(path = image, motionVideoPath = motion.ifBlank { null }))
                            }
                        }
                        else -> {
                            val item = entry.toString()
                            if (item.isNotBlank()) add(ImageUpdateItem(path = item))
                        }
                    }
                }
            }
        }.getOrDefault(emptyList())
    } else if (trimmed.startsWith("{")) {
        runCatching {
            val obj = org.json.JSONObject(trimmed)
            val image = obj.optString("image")
            val motion = obj.optString("motionVideo")
            if (image.isNotBlank()) {
                listOf(ImageUpdateItem(path = image, motionVideoPath = motion.ifBlank { null }))
            } else {
                emptyList()
            }
        }.getOrDefault(emptyList())
    } else {
        listOf(ImageUpdateItem(path = payload))
    }
    return parsedList.map { item ->
        val raw = item.path ?: item.base64 ?: return@map item
        val uri = Uri.parse(raw)
        if (uri.scheme != null) {
            item.copy(path = raw, base64 = null)
        } else {
            item.copy(base64 = raw, path = null)
        }
    }
}

private fun parseVideoItems(raw: String?): List<VideoUpdateItem> {
    if (raw.isNullOrBlank()) return emptyList()
    val list = parsePathList(raw)
    return list.map { VideoUpdateItem(path = it) }
}

private fun parsePathList(raw: String): List<String> {
    return if (raw.trim().startsWith("[")) {
        runCatching {
            val array = org.json.JSONArray(raw)
            List(array.length()) { index -> array.getString(index) }
        }.getOrDefault(listOf(raw))
    } else {
        listOf(raw)
    }
}

private fun flowItemFromResource(resource: ResourceWithTagsCharacters): FlowUpdateItem {
    val data = resource.resource
    return when (data.type) {
        ResourceType.TEXT -> FlowUpdateItem(
            type = data.type,
            title = data.title,
            resourceId = data.id,
            text = data.quoteText.orEmpty()
        )
        ResourceType.SCENE -> FlowUpdateItem(
            type = data.type,
            title = data.title,
            resourceId = data.id,
            sceneMessages = parseSceneMessages(data.sceneJson)
        )
        ResourceType.IMAGE -> FlowUpdateItem(
            type = data.type,
            title = data.title,
            resourceId = data.id,
            images = parseImageItems(data.contentUriOrPath, data.quoteImageBase64)
        )
        ResourceType.VIDEO -> FlowUpdateItem(
            type = data.type,
            title = data.title,
            resourceId = data.id,
            videos = parseVideoItems(data.contentUriOrPath)
        )
        else -> FlowUpdateItem(type = data.type, title = data.title, resourceId = data.id)
    }
}

@Composable
private fun resourceSummary(resource: ResourceWithTagsCharacters): String {
    val data = resource.resource
    return when (data.type) {
        ResourceType.TEXT -> rememberFormattedText(data.quoteText.orEmpty())
        ResourceType.SCENE -> {
            val messages = parseSceneMessages(data.sceneJson)
            if (messages.isEmpty()) "" else "对话 ${messages.size} 条"
        }
        ResourceType.IMAGE -> {
            val images = parseImageItems(data.contentUriOrPath, data.quoteImageBase64)
            if (images.isEmpty()) "" else "图片 ${images.size} 张"
        }
        ResourceType.VIDEO -> {
            val videos = parseVideoItems(data.contentUriOrPath)
            if (videos.isEmpty()) "" else "视频 ${videos.size} 个"
        }
        else -> ""
    }
}

private fun parseFlowItems(raw: String?): List<FlowUpdateItem> {
    if (raw.isNullOrBlank()) return emptyList()
    return runCatching {
        val array = org.json.JSONArray(raw)
        buildList {
            for (i in 0 until array.length()) {
                val obj = array.optJSONObject(i) ?: continue
                val type = runCatching { ResourceType.valueOf(obj.optString("type")) }.getOrNull() ?: continue
                val title = obj.optString("title").ifBlank { null }
                val resourceId = obj.optLong("resourceId", -1L).takeIf { it > 0 }
                when (type) {
                    ResourceType.TEXT -> add(
                        FlowUpdateItem(
                            type = type,
                            title = title,
                            resourceId = resourceId,
                            text = obj.optString("text")
                        )
                    )
                    ResourceType.SCENE -> {
                        val messages = obj.optJSONArray("messages")
                        val parsed = buildList {
                            if (messages != null) {
                                for (j in 0 until messages.length()) {
                                    val msg = messages.optJSONObject(j) ?: continue
                                    val speaker = msg.optString("speaker")
                                    val content = msg.optString("text")
                                    if (speaker.isNotBlank() || content.isNotBlank()) {
                                        add(SceneMessageDraft(speaker.ifBlank { "角色" }, content))
                                    }
                                }
                            }
                        }
                        add(
                            FlowUpdateItem(
                                type = type,
                                title = title,
                                resourceId = resourceId,
                                sceneMessages = parsed
                            )
                        )
                    }
                    ResourceType.IMAGE -> {
                        val images = obj.optJSONArray("images")
                        val parsed = buildList {
                            if (images != null) {
                                for (j in 0 until images.length()) {
                                    when (val entry = images.get(j)) {
                                        is org.json.JSONObject -> {
                                            val image = entry.optString("image")
                                            val motion = entry.optString("motionVideo")
                                            if (image.isNotBlank()) {
                                                val uri = Uri.parse(image)
                                                if (uri.scheme != null) {
                                                    add(ImageUpdateItem(path = image, motionVideoPath = motion.ifBlank { null }))
                                                } else {
                                                    add(ImageUpdateItem(base64 = image, motionVideoPath = motion.ifBlank { null }))
                                                }
                                            }
                                        }
                                        else -> {
                                            val item = entry.toString()
                                            if (item.isNotBlank()) {
                                                val uri = Uri.parse(item)
                                                if (uri.scheme != null) {
                                                    add(ImageUpdateItem(path = item))
                                                } else {
                                                    add(ImageUpdateItem(base64 = item))
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        add(
                            FlowUpdateItem(
                                type = type,
                                title = title,
                                resourceId = resourceId,
                                images = parsed
                            )
                        )
                    }
                    ResourceType.VIDEO -> {
                        val videos = obj.optJSONArray("videos")
                        val parsed = buildList {
                            if (videos != null) {
                                for (j in 0 until videos.length()) {
                                    val item = videos.optString(j)
                                    if (item.isNotBlank()) add(VideoUpdateItem(path = item))
                                }
                            }
                        }
                        add(
                            FlowUpdateItem(
                                type = type,
                                title = title,
                                resourceId = resourceId,
                                videos = parsed
                            )
                        )
                    }
                    else -> {}
                }
            }
        }
    }.getOrDefault(emptyList())
}

private fun buildSceneJson(messages: List<SceneMessageDraft>): String {
    val array = org.json.JSONArray()
    messages.forEach { message ->
        val obj = org.json.JSONObject()
        obj.put("speaker", message.speaker)
        obj.put("text", message.content)
        array.put(obj)
    }
    return array.toString()
}
