@file:OptIn(ExperimentalLayoutApi::class)

package com.example.quotepicker.ui

import androidx.compose.foundation.layout.WindowInsets
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DriveFileMove
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.MusicNote
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import android.widget.Toast
import androidx.compose.ui.text.TextStyle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.quotepicker.data.CharacterEntity
import com.example.quotepicker.data.ResourceMarkState
import com.example.quotepicker.data.ResourceType
import com.example.quotepicker.data.ResourceWithTagsCharacters
import com.example.quotepicker.data.TagCategoryEntity
import com.example.quotepicker.data.TagEntity
import com.example.quotepicker.ui.components.CharacterBadge
import com.example.quotepicker.ui.components.encodeResourceFileInfo
import com.example.quotepicker.ui.components.ResourceListRow
import com.example.quotepicker.ui.components.ResourcePreviewScreen
import com.example.quotepicker.ui.components.TagBadge
import com.example.quotepicker.ui.components.sortTagsForDisplay
import com.example.quotepicker.ui.components.tagTextColor
import com.example.quotepicker.vm.FlowUpdateItem
import com.example.quotepicker.vm.ImageUpdateItem
import com.example.quotepicker.vm.ResourceViewModel
import com.example.quotepicker.vm.SceneMessageDraft
import com.example.quotepicker.vm.StoredMediaItem
import com.example.quotepicker.vm.VideoUpdateItem
import com.example.quotepicker.vm.SoundUpdateItem
import java.io.File
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
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
    var regroupTarget by remember { mutableStateOf<ResourceWithTagsCharacters?>(null) }
    var filterTagDialog by remember { mutableStateOf(false) }
    var filterCharacterDialog by remember { mutableStateOf(false) }
    var filterGroupDialog by remember { mutableStateOf(false) }
    var manageScreen by remember { mutableStateOf(false) }
    var manageItems by remember { mutableStateOf<List<StoredMediaItem>>(emptyList()) }
    var restoreTarget by remember { mutableStateOf<StoredMediaItem?>(null) }
    var groupLevel1Selected by remember { mutableStateOf(true) }
    var groupLevel2Selected by remember { mutableStateOf(false) }
    var groupLevel3Selected by remember { mutableStateOf(false) }
    var groupByTitleSelected by remember { mutableStateOf(false) }
    var hierarchicalGroupMode by remember { mutableStateOf(false) }
    var openedGroupPath by remember { mutableStateOf<List<String>>(emptyList()) }
    var renameGroupTarget by remember { mutableStateOf<ResourceTitleGroup?>(null) }
    var renameGroupValue by remember { mutableStateOf(TextFieldValue("")) }
    var selectedGroupLevel1 by remember { mutableStateOf<Set<String>>(emptySet()) }
    var selectedGroupPairs by remember { mutableStateOf<Set<Pair<String, String>>>(emptySet()) }
    var groupKeyword by remember { mutableStateOf("") }
    var groupKeywordDialog by remember { mutableStateOf(false) }
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
            modifier = modifier,
            items = manageItems,
            resources = allResources,
            vm = vm,
            characters = ui.characters,
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

    val keywordFilteredResources = remember(ui.resources, groupKeyword) {
        if (groupKeyword.isBlank()) ui.resources
        else ui.resources.filter { it.resource.title.contains(groupKeyword, ignoreCase = true) }
    }

    val groupedFilterResources = remember(keywordFilteredResources, selectedGroupLevel1, selectedGroupPairs) {
        applyGroupFilter(
            resources = keywordFilteredResources,
            selectedLevel1 = selectedGroupLevel1,
            selectedPairs = selectedGroupPairs
        )
    }

    val groupedResources = remember(groupedFilterResources, groupLevel1Selected, groupLevel2Selected, groupLevel3Selected, groupByTitleSelected, hierarchicalGroupMode, openedGroupPath) {
        if (hierarchicalGroupMode) {
            buildNestedResourceGroups(
                resources = groupedFilterResources,
                level1 = groupLevel1Selected,
                level2 = groupLevel2Selected,
                level3 = groupLevel3Selected,
                groupByTitle = groupByTitleSelected,
                path = openedGroupPath
            )
        } else {
            buildResourceGroups(groupedFilterResources, groupLevel1Selected, groupLevel2Selected, groupLevel3Selected, groupByTitleSelected)
        }
    }

    val openedGroup = remember(groupedResources, hierarchicalGroupMode) {
        groupedResources.takeIf { !hierarchicalGroupMode && it.size == 1 }?.firstOrNull()
    }

    LaunchedEffect(groupedResources, groupLevel1Selected, groupLevel2Selected, groupLevel3Selected, hierarchicalGroupMode, openedGroupPath) {
        if (!groupLevel1Selected && !groupLevel2Selected && !groupLevel3Selected && !groupByTitleSelected) {
            openedGroupPath = emptyList()
            return@LaunchedEffect
        }
        if (hierarchicalGroupMode && groupedResources.isEmpty() && openedGroupPath.isNotEmpty()) {
            openedGroupPath = openedGroupPath.dropLast(1)
        }
    }

    createMode?.let { mode ->
        ResourceCreateScreen(
            mode = mode,
            categories = ui.categories,
            tags = ui.tags,
            characters = ui.characters,
            availableResources = allResources,
            initialTitle = buildCreateTitlePreset(
                openedGroupTitle = if (hierarchicalGroupMode) openedGroupPath.lastOrNull() else openedGroup?.title,
                level1 = groupLevel1Selected,
                level2 = groupLevel2Selected,
                level3 = groupLevel3Selected
            ),
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
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
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
                selectedMarkState = ui.filters.selectedMarkState,
                selectedGroupLevel1 = selectedGroupLevel1,
                selectedGroupPairs = selectedGroupPairs,
                groupLevel1Selected = groupLevel1Selected,
                groupLevel2Selected = groupLevel2Selected,
                groupLevel3Selected = groupLevel3Selected,
                hierarchicalGroupMode = hierarchicalGroupMode,
                onTypeChange = vm::updateTypeFilter,
                onCharacterDialog = { filterCharacterDialog = true },
                onTagDialog = { filterTagDialog = true },
                onGroupDialog = { filterGroupDialog = true },
                onKeywordDialog = { groupKeywordDialog = true },
                groupKeyword = groupKeyword,
                onToggleLevel1 = { groupLevel1Selected = !groupLevel1Selected },
                onToggleLevel2 = { groupLevel2Selected = !groupLevel2Selected },
                onToggleLevel3 = { groupLevel3Selected = !groupLevel3Selected },
                onToggleHierarchicalMode = { hierarchicalGroupMode = !hierarchicalGroupMode; openedGroupPath = emptyList() },
                onMarkStateChange = vm::updateMarkStateFilter
            )
            when {
                groupedFilterResources.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("暂无资源，点击右下角添加")
                    }
                }
                groupLevel1Selected || groupLevel2Selected || groupLevel3Selected || groupByTitleSelected -> {
                    val selectedFlatGroup = if (!hierarchicalGroupMode) groupedResources.firstOrNull { it.title == openedGroupPath.firstOrNull() } else null
                    if (selectedFlatGroup != null) {
                        ResourceGroupedPage(
                            group = selectedFlatGroup,
                            categories = ui.categories,
                            onBack = { openedGroupPath = emptyList() },
                            onPreview = { previewTarget = it },
                            onLongClick = { bottomSheetTarget = it }
                        )
                    } else if (hierarchicalGroupMode && groupedResources.size == 1 && groupedResources.first().resources.isNotEmpty() && groupedResources.first().childNames.isBlank()) {
                        ResourceGroupedPage(
                            group = groupedResources.first(),
                            categories = ui.categories,
                            onBack = { if (openedGroupPath.isNotEmpty()) openedGroupPath = openedGroupPath.dropLast(1) },
                            backLabel = openedGroupPath.dropLast(1).lastOrNull() ?: "根分组",
                            onPreview = { previewTarget = it },
                            onLongClick = { bottomSheetTarget = it }
                        )
                    } else {
                        LazyColumn(
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (hierarchicalGroupMode && openedGroupPath.isNotEmpty()) {
                                item {
                                    TextButton(onClick = { openedGroupPath = openedGroupPath.dropLast(1) }) {
                                        Text("返回 ${openedGroupPath.dropLast(1).lastOrNull() ?: "根分组"}")
                                    }
                                }
                            }
                            items(groupedResources, key = { it.key }) { group ->
                                GroupListRow(
                                    name = group.title,
                                    childNames = group.childNames,
                                    count = group.resources.size,
                                    onClick = {
                                        if (hierarchicalGroupMode && group.childNames.isNotBlank()) {
                                            openedGroupPath = openedGroupPath + group.title
                                        } else {
                                            previewTarget = null
                                            if (hierarchicalGroupMode) {
                                                openedGroupPath = openedGroupPath + group.title
                                            } else {
                                                openedGroupPath = listOf(group.title)
                                            }
                                        }
                                    },
                                    onLongClick = {
                                        renameGroupTarget = group
                                        renameGroupValue = TextFieldValue(group.title)
                                    }
                                )
                            }
                        }
                    }
                }
                else -> {
                    LazyColumn(
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(groupedFilterResources, key = { it.resource.id }) { res ->
                            ResourceListRow(
                                resource = res,
                                categories = ui.categories,
                                roleText = res.characters.joinToString("/") { it.name }.ifBlank { "无角色" },
                                onClick = { previewTarget = res },
                                onLongClick = { bottomSheetTarget = res }
                            )
                        }
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
                TextButton(onClick = { createMode = CreateMode.SoundGroup; showAddMenu = false }) {
                    Icon(Icons.Default.MusicNote, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("上传声音组")
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
    if (filterGroupDialog) {
        FilterGroupDialog(
            resources = allResources,
            selectedLevel1 = selectedGroupLevel1,
            onConfirm = { level1, pairs ->
                selectedGroupLevel1 = level1
                selectedGroupPairs = pairs
            },
            onDismiss = { filterGroupDialog = false }
        )
    }
    if (groupKeywordDialog) {
        GroupKeywordDialog(
            initialKeyword = groupKeyword,
            onConfirm = {
                groupKeyword = it
                groupKeywordDialog = false
            },
            onClear = {
                groupKeyword = ""
                groupKeywordDialog = false
            },
            onDismiss = { groupKeywordDialog = false }
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
                    regroupTarget = target
                    bottomSheetTarget = null
                }) {
                    Icon(Icons.Default.DriveFileMove, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("改组")
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

    regroupTarget?.let { target ->
        val selectedIndexes = remember(groupLevel1Selected, groupLevel2Selected, groupLevel3Selected) {
            buildList {
                if (groupLevel1Selected) add(0)
                if (groupLevel2Selected) add(1)
                if (groupLevel3Selected) add(2)
            }
        }
        val candidateGroups = remember(allResources, target, selectedIndexes) {
            if (selectedIndexes.isEmpty()) {
                emptyList()
            } else {
                allResources
                    .asSequence()
                    .filter { it.resource.type == target.resource.type }
                    .mapNotNull { candidate ->
                        groupedPartsForIndexes(candidate.resource.title, selectedIndexes)
                            ?.takeIf { it.isNotBlank() }
                    }
                    .distinct()
                    .sorted()
                    .toList()
            }
        }
        var selectedGroup by remember(target, candidateGroups) { mutableStateOf(candidateGroups.firstOrNull()) }
        val canConfirm = selectedGroup != null && selectedIndexes.isNotEmpty()

        AlertDialog(
            onDismissRequest = { regroupTarget = null },
            title = { Text("改组") },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 360.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (selectedIndexes.isEmpty()) {
                        Text("请先选择 1/2/3 分组按钮", style = MaterialTheme.typography.labelMedium)
                    } else if (candidateGroups.isEmpty()) {
                        Text("暂无可用组名", style = MaterialTheme.typography.labelMedium)
                    } else {
                        Text("选择目标组名", style = MaterialTheme.typography.labelMedium)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            candidateGroups.forEach { title ->
                                FilterChip(
                                    selected = selectedGroup == title,
                                    onClick = { selectedGroup = title },
                                    label = { Text(title) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                    )
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val nextTitle = buildRegroupedTitle(
                            originalTitle = target.resource.title,
                            selectedIndexes = selectedIndexes,
                            selectedGroupName = selectedGroup
                        )
                        if (nextTitle != null && nextTitle != target.resource.title) {
                            vm.updateResource(target.resource.copy(title = nextTitle))
                        }
                        regroupTarget = null
                    },
                    enabled = canConfirm
                ) { Text("确定") }
            },
            dismissButton = { TextButton(onClick = { regroupTarget = null }) { Text("取消") } }
        )
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

    if (renameGroupTarget != null) {
        AlertDialog(
            onDismissRequest = { renameGroupTarget = null },
            title = { Text("编辑组名") },
            text = {
                OutlinedTextField(
                    value = renameGroupValue,
                    onValueChange = { renameGroupValue = it },
                    label = { Text("组名") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val target = renameGroupTarget ?: return@TextButton
                        val updates = buildGroupRenameUpdates(
                            resources = target.resources,
                            newGroupName = renameGroupValue.text,
                            level1 = groupLevel1Selected,
                            level2 = groupLevel2Selected,
                            level3 = groupLevel3Selected
                        )
                        if (updates.isNotEmpty()) {
                            vm.updateResourceTitles(updates)
                        }
                        renameGroupTarget = null
                    }
                ) {
                    Text("保存")
                }
            },
            dismissButton = {
                TextButton(onClick = { renameGroupTarget = null }) {
                    Text("取消")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun ManageStorageScreen(
    modifier: Modifier = Modifier,
    items: List<StoredMediaItem>,
    resources: List<ResourceWithTagsCharacters>,
    vm: ResourceViewModel,
    characters: List<CharacterEntity>,
    onBack: () -> Unit,
    onRestore: (StoredMediaItem) -> Unit,
    onRefresh: () -> Unit
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    var actionTarget by remember { mutableStateOf<StoredMediaItem?>(null) }
    var deleteTarget by remember { mutableStateOf<StoredMediaItem?>(null) }
    var selectedType by remember { mutableStateOf(ResourceType.IMAGE) }
    var selectedCharacterId by remember { mutableStateOf<Long?>(null) }
    var showCharacterDialog by remember { mutableStateOf(false) }
    val groupExpandedState = remember { mutableStateMapOf<String, Boolean>() }
    val coroutineScope = rememberCoroutineScope()
    val groupedItems = remember(items, resources, selectedCharacterId) {
        buildStoredMediaGroups(items, resources, selectedCharacterId)
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("资源存储") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
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
            Text("长按文件进行管理", style = MaterialTheme.typography.labelSmall)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(ResourceType.IMAGE, ResourceType.VIDEO, ResourceType.SOUND).forEach { type ->
                    FilterChip(
                        selected = selectedType == type,
                        onClick = { selectedType = type },
                        label = { Text(typeLabel(type)) }
                    )
                }
                val selectedCharacter = characters.firstOrNull { it.id == selectedCharacterId }
                AssistChip(
                    onClick = { showCharacterDialog = true },
                    label = { Text(selectedCharacter?.name ?: "全部角色") }
                )
            }
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 28.dp)
            ) {
                val groups = groupedItems[selectedType].orEmpty()
                if (groups.isEmpty()) {
                    item {
                        Text(
                            "暂无${typeLabel(selectedType)}文件",
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                } else {
                    groups.forEach { group ->
                        val expanded = groupExpandedState.getOrPut("${selectedType.name}-${group.title}") { false }
                        item {
                            StorageSectionHeader(
                                title = group.title,
                                expanded = expanded,
                                onToggle = {
                                    groupExpandedState["${selectedType.name}-${group.title}"] = !expanded
                                }
                            )
                        }
                        if (expanded) {
                            items(group.items, key = { it.path }) { item ->
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
        }
    }

    if (showCharacterDialog) {
        FilterCharacterDialog(
            characters = characters,
            selectedId = selectedCharacterId,
            onConfirm = { selectedCharacterId = it },
            onDismiss = { showCharacterDialog = false }
        )
    }

    actionTarget?.let { target ->
        ModalBottomSheet(onDismissRequest = { actionTarget = null }) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TextButton(onClick = {
                    copyFileInfo(target, clipboard, context)
                    actionTarget = null
                }) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("复制文件信息")
                }
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
        value = withContext(Dispatchers.IO) {
            when (item.type) {
                ResourceType.IMAGE -> vm.decodeUriToBitmap(Uri.parse(item.path))
                ResourceType.VIDEO -> vm.decodeVideoFrame(Uri.parse(item.path))
                ResourceType.SOUND -> null
                else -> null
            }
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
        if (thumbnail != null) {
            Image(
                bitmap = thumbnail!!.asImageBitmap(),
                contentDescription = if (item.type == ResourceType.IMAGE) "图片预览" else "视频预览",
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
                    imageVector = when (item.type) {
                        ResourceType.IMAGE -> Icons.Default.Image
                        ResourceType.VIDEO -> Icons.Default.Videocam
                        ResourceType.SOUND -> Icons.Default.MusicNote
                        else -> Icons.Default.Videocam
                    },
                    contentDescription = null
                )
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = when (item.type) {
                    ResourceType.IMAGE -> "图片"
                    ResourceType.VIDEO -> "视频"
                    ResourceType.SOUND -> "音频"
                    else -> "资源"
                },
                style = MaterialTheme.typography.labelMedium
            )
            Text(
                text = item.path,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "大小: ${formatFileSizeInMb(item.path)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

}

private data class StoredMediaGroup(
    val title: String,
    val items: List<StoredMediaItem>
)

private fun formatFileSizeInMb(path: String): String {
    val sizeBytes = runCatching {
        val uri = Uri.parse(path)
        val filePath = uri.path ?: return@runCatching null
        File(filePath).takeIf { it.exists() }?.length()
    }.getOrNull()
    val sizeMb = (sizeBytes ?: 0L).toDouble() / (1024 * 1024)
    return String.format(Locale.US, "%.1fM", sizeMb)
}

@Composable
private fun StorageSectionHeader(
    title: String,
    expanded: Boolean,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        Icon(
            imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
            contentDescription = if (expanded) "折叠" else "展开"
        )
    }
}

private fun buildStoredMediaGroups(
    items: List<StoredMediaItem>,
    resources: List<ResourceWithTagsCharacters>,
    selectedCharacterId: Long?
): Map<ResourceType, List<StoredMediaGroup>> {
    val imageTitleMap = mutableMapOf<String, String>()
    val videoTitleMap = mutableMapOf<String, String>()
    val soundTitleMap = mutableMapOf<String, String>()

    val filteredResources = resources.filter { resource ->
        selectedCharacterId == null || resource.characters.any { it.id == selectedCharacterId }
    }

    filteredResources.forEach { resource ->
        when (resource.resource.type) {
            ResourceType.IMAGE -> {
                parseImageItems(resource.resource.contentUriOrPath, resource.resource.quoteImageBase64)
                    .forEach { item ->
                        item.path?.let { imageTitleMap[it] = resource.resource.title }
                    }
            }
            ResourceType.VIDEO -> {
                parseVideoItems(resource.resource.contentUriOrPath)
                    .forEach { item ->
                        item.path?.let { videoTitleMap[it] = resource.resource.title }
                    }
            }
            ResourceType.SOUND -> {
                parseSoundItems(resource.resource.contentUriOrPath)
                    .forEach { item ->
                        item.path?.let { soundTitleMap[it] = resource.resource.title }
                    }
            }
            else -> Unit
        }
    }

    fun groupItems(type: ResourceType, titleMap: Map<String, String>): List<StoredMediaGroup> {
        val groups = items.filter { it.type == type }
            .groupBy { titleMap[it.path] ?: "未归档" }
        return groups.entries.sortedBy { it.key }.map { (title, groupItems) ->
            StoredMediaGroup(title = title, items = groupItems.sortedBy { it.path })
        }
    }

    return mapOf(
        ResourceType.IMAGE to groupItems(ResourceType.IMAGE, imageTitleMap),
        ResourceType.VIDEO to groupItems(ResourceType.VIDEO, videoTitleMap),
        ResourceType.SOUND to groupItems(ResourceType.SOUND, soundTitleMap)
    )
}

private fun copyFileInfo(
    item: StoredMediaItem,
    clipboard: androidx.compose.ui.platform.ClipboardManager,
    context: android.content.Context
) {
    val info = encodeResourceFileInfo(item.type, Uri.parse(item.path))
    clipboard.setText(AnnotatedString(info))
    Toast.makeText(context, "文件信息已复制", Toast.LENGTH_SHORT).show()
}

private sealed class CreateMode {
    data object Flow : CreateMode()
    data object Text : CreateMode()
    data object ImageGroup : CreateMode()
    data object Scene : CreateMode()
    data object VideoGroup : CreateMode()
    data object SoundGroup : CreateMode()
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FilterBar(
    selectedType: ResourceType?,
    selectedTagIds: Set<Long>,
    characters: List<CharacterEntity>,
    selectedCharacterId: Long?,
    selectedMarkState: ResourceMarkState?,
    selectedGroupLevel1: Set<String>,
    selectedGroupPairs: Set<Pair<String, String>>,
    groupLevel1Selected: Boolean,
    groupLevel2Selected: Boolean,
    groupLevel3Selected: Boolean,
    hierarchicalGroupMode: Boolean,
    onTypeChange: (ResourceType?) -> Unit,
    onCharacterDialog: () -> Unit,
    onTagDialog: () -> Unit,
    onGroupDialog: () -> Unit,
    onKeywordDialog: () -> Unit,
    groupKeyword: String,
    onToggleLevel1: () -> Unit,
    onToggleLevel2: () -> Unit,
    onToggleLevel3: () -> Unit,
    onToggleHierarchicalMode: () -> Unit,
    onMarkStateChange: (ResourceMarkState?) -> Unit
) {
    // ---- 紧凑参数（≈80%高度）----
    val chipH = 30.dp
    val hGap = 8.dp
    val vGap = 6.dp
    val textStyle = TextStyle(fontSize = 12.sp)

    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(vGap)
    ) {
        // ---- Row 1: 资源类型 ----
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(hGap),
            verticalArrangement = Arrangement.spacedBy(vGap)
        ) {
            val orderedTypes = listOf(
                ResourceType.FLOW,
                ResourceType.TEXT,
                ResourceType.IMAGE,
                ResourceType.VIDEO,
                ResourceType.SOUND,
                ResourceType.SCENE
            )
            orderedTypes.forEach { type ->
                FilterChip(
                    modifier = Modifier.height(chipH),
                    selected = selectedType == type,
                    onClick = { onTypeChange(if (selectedType == type) null else type) },
                    label = { Text(typeLabel(type), style = textStyle, maxLines = 1) }
                )
            }
        }

        // ---- Row 2: Tag / Character / Group ----
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(hGap),
            verticalArrangement = Arrangement.spacedBy(vGap)
        ) {
            AssistChip(
                modifier = Modifier.height(chipH),
                onClick = onTagDialog,
                label = { Text("标签筛选(${selectedTagIds.size})", style = textStyle, maxLines = 1) }
            )

            val selectedCharacter = characters.firstOrNull { it.id == selectedCharacterId }
            AssistChip(
                modifier = Modifier.height(chipH),
                onClick = onCharacterDialog,
                label = { Text(selectedCharacter?.name ?: "全部角色", style = textStyle, maxLines = 1) }
            )

            val groupCount = selectedGroupPairs.size.takeIf { it > 0 } ?: selectedGroupLevel1.size
            AssistChip(
                modifier = Modifier.height(chipH),
                onClick = onGroupDialog,
                label = { Text("分组筛选($groupCount)", style = textStyle, maxLines = 1) }
            )
        }

        // ---- Row 3: Level + Mode + Mark ----
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(hGap),
            verticalArrangement = Arrangement.spacedBy(vGap)
        ) {
            FilterChip(
                modifier = Modifier.height(chipH),
                selected = selectedMarkState == ResourceMarkState.NONE,
                onClick = {
                    onMarkStateChange(
                        if (selectedMarkState == ResourceMarkState.NONE) null else ResourceMarkState.NONE
                    )
                },
                label = { Text("待理", color = Color.Gray, style = textStyle, maxLines = 1) }
            )

            FilterChip(
                modifier = Modifier.height(chipH),
                selected = selectedMarkState == ResourceMarkState.CHECKED,
                onClick = {
                    onMarkStateChange(
                        if (selectedMarkState == ResourceMarkState.CHECKED) null else ResourceMarkState.CHECKED
                    )
                },
                label = { Text("通过", color = Color(0xFF22C55E), style = textStyle, maxLines = 1) }
            )

            FilterChip(
                modifier = Modifier.height(chipH),
                selected = selectedMarkState == ResourceMarkState.FAVORITE,
                onClick = {
                    onMarkStateChange(
                        if (selectedMarkState == ResourceMarkState.FAVORITE) null else ResourceMarkState.FAVORITE
                    )
                },
                label = { Text("标记", color = Color(0xFFC62828), style = textStyle, maxLines = 1) }
            )

            val keywordLabel = if (groupKeyword.isBlank()) "关键词" else "[$groupKeyword]"
            AssistChip(
                modifier = Modifier.height(chipH),
                onClick = onKeywordDialog,
                label = { Text(keywordLabel, style = textStyle, maxLines = 1) }
            )
        }

        // ---- Row 3: Level + Mode + Mark ----
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(hGap),
            verticalArrangement = Arrangement.spacedBy(vGap)
        ) {
            FilterChip(
                modifier = Modifier.height(chipH),
                selected = groupLevel1Selected,
                onClick = onToggleLevel1,
                label = { Text("一层", style = textStyle, maxLines = 1) }
            )
            FilterChip(
                modifier = Modifier.height(chipH),
                selected = groupLevel2Selected,
                onClick = onToggleLevel2,
                label = { Text("二层", style = textStyle, maxLines = 1) }
            )
            FilterChip(
                modifier = Modifier.height(chipH),
                selected = groupLevel3Selected,
                onClick = onToggleLevel3,
                label = { Text("三层", style = textStyle, maxLines = 1) }
            )
            FilterChip(
                modifier = Modifier.height(chipH),
                selected = hierarchicalGroupMode,
                onClick = onToggleHierarchicalMode,
                label = { Text("折叠", style = textStyle, maxLines = 1) }
            )
        }
    }
}


private data class ResourceTitleGroup(
    val key: String,
    val title: String,
    val childNames: String,
    val resources: List<ResourceWithTagsCharacters>
)

private fun buildResourceGroups(
    resources: List<ResourceWithTagsCharacters>,
    level1: Boolean,
    level2: Boolean,
    level3: Boolean,
    groupByTitle: Boolean
): List<ResourceTitleGroup> {
    if (!level1 && !level2 && !level3 && !groupByTitle) return emptyList()
    val grouped = resources.groupBy { if (groupByTitle) it.resource.title else resourceTitleGroupKey(it.resource.title, level1, level2, level3).first }
    return grouped.entries
        .sortedWith(compareByDescending<Map.Entry<String, List<ResourceWithTagsCharacters>>> { it.value.size }.thenBy { it.key })
        .map { entry ->
            val childNames = entry.value
                .mapNotNull { if (groupByTitle) null else resourceTitleGroupKey(it.resource.title, level1, level2, level3).second }
                .distinct()
                .joinToString("・")
            ResourceTitleGroup(
                key = entry.key,
                title = entry.key,
                childNames = childNames,
                resources = entry.value
            )
        }
}

private fun buildNestedResourceGroups(
    resources: List<ResourceWithTagsCharacters>,
    level1: Boolean,
    level2: Boolean,
    level3: Boolean,
    groupByTitle: Boolean,
    path: List<String>
): List<ResourceTitleGroup> {
    val indexes = if (groupByTitle) listOf(-1) else buildList {
        if (level1) add(0)
        if (level2) add(1)
        if (level3) add(2)
    }
    if (indexes.isEmpty()) return emptyList()
    val depth = path.size
    val filtered = resources.filter { res ->
        val parts = res.resource.title.split("-").map { it.trim() }.filter { it.isNotEmpty() }
        path.indices.all { i ->
            val key = if (indexes[i] == -1) res.resource.title else parts.getOrNull(indexes[i]).orEmpty().ifBlank { "未分组" }
            key == path[i]
        }
    }
    if (depth >= indexes.size) {
        return listOf(ResourceTitleGroup(path.lastOrNull().orEmpty(), path.lastOrNull().orEmpty(), "", filtered))
    }
    val idxPart = indexes[depth]
    val grouped = filtered.groupBy { res ->
        if (idxPart == -1) res.resource.title else {
            res.resource.title.split("-").map { it.trim() }.filter { it.isNotEmpty() }.getOrNull(idxPart).orEmpty().ifBlank { "未分组" }
        }
    }
    return grouped.entries.map { (k,v) ->
        val childNames = if (depth + 1 < indexes.size) {
            val childIndex = indexes[depth + 1]
            v.mapNotNull { child ->
                if (childIndex == -1) {
                    child.resource.title
                } else {
                    child.resource.title.split("-").map { it.trim() }.filter { it.isNotEmpty() }.getOrNull(childIndex)
                }
            }.distinct().sorted().joinToString("・")
        } else {
            ""
        }
        ResourceTitleGroup(k, k, childNames, v)
    }.sortedBy { it.title }
}

private fun resourceTitleGroupKey(title: String, level1: Boolean, level2: Boolean, level3: Boolean): Pair<String, String?> {
    val parts = title.split("-").map { it.trim() }.filter { it.isNotEmpty() }
    val p1 = parts.getOrNull(0)
    val p2 = parts.getOrNull(1)
    val p3 = parts.getOrNull(2)
    val p4 = parts.getOrNull(3)
    val selectedParts = buildList {
        if (level1) add(p1)
        if (level2) add(p2)
        if (level3) add(p3)
    }.filterNotNull()
    val child = when {
        level1 && level2 && level3 -> p4
        level1 && level2 -> p3
        level1 && level3 -> p2
        level2 && level3 -> p1
        level1 -> p2
        level2 -> p3
        level3 -> p4
        else -> null
    }
    return if (selectedParts.isNotEmpty()) {
        selectedParts.joinToString("-").ifBlank { "未分组" } to child
    } else {
        title to null
    }
}

private fun buildCreateTitlePreset(
    openedGroupTitle: String?,
    level1: Boolean,
    level2: Boolean,
    level3: Boolean
): String {
    if (openedGroupTitle.isNullOrBlank()) return ""
    val selectedIndexes = buildList {
        if (level1) add(0)
        if (level2) add(1)
        if (level3) add(2)
    }
    if (selectedIndexes.isEmpty()) return ""
    val selectedParts = openedGroupTitle.split("-").map { it.trim() }.filter { it.isNotEmpty() }
    if (selectedParts.isEmpty()) return ""
    val maxIndex = selectedIndexes.maxOrNull() ?: return ""
    val baseParts = MutableList(maxIndex + 1) { "" }
    selectedIndexes.forEachIndexed { position, index ->
        selectedParts.getOrNull(position)?.let { baseParts[index] = it }
    }
    return baseParts.joinToString("-") + "-"
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun GroupListRow(
    name: String,
    childNames: String,
    count: Int,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant)
            .clip(MaterialTheme.shapes.small)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(horizontal = 8.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(name, style = MaterialTheme.typography.bodyMedium, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
            Text("${count}个资源", color = Color(0xFF1565C0), style = MaterialTheme.typography.labelMedium)
        }
        if (childNames.isNotBlank()) {
            Text(childNames, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun buildGroupRenameUpdates(
    resources: List<ResourceWithTagsCharacters>,
    newGroupName: String,
    level1: Boolean,
    level2: Boolean,
    level3: Boolean
): List<Pair<com.example.quotepicker.data.ResourceEntity, String>> {
    val renameParts = newGroupName.split("-").map { it.trim() }.filter { it.isNotEmpty() }
    val selectedIndexes = buildList {
        if (level1) add(0)
        if (level2) add(1)
        if (level3) add(2)
    }
    if (selectedIndexes.isEmpty() || renameParts.isEmpty()) return emptyList()
    return resources.mapNotNull { item ->
        val oldParts = item.resource.title.split("-").map { it.trim() }.toMutableList()
        val nextParts = oldParts.toMutableList()
        val requiredSize = (selectedIndexes.maxOrNull() ?: 0) + 1
        while (nextParts.size < requiredSize) {
            nextParts.add("")
        }
        selectedIndexes.forEachIndexed { index, partIndex ->
            val renamed = renameParts.getOrNull(index) ?: return@forEachIndexed
            nextParts[partIndex] = renamed
        }
        val nextTitle = nextParts.joinToString("-").trim('-').ifBlank { item.resource.title }
        if (nextTitle != item.resource.title) item.resource to nextTitle else null
    }
}

private fun groupedPartsForIndexes(title: String, selectedIndexes: List<Int>): String? {
    if (selectedIndexes.isEmpty()) return null
    val parts = title.split("-").map { it.trim() }.filter { it.isNotEmpty() }
    val selected = selectedIndexes.mapNotNull { index -> parts.getOrNull(index) }
    return selected.takeIf { it.isNotEmpty() }?.joinToString("-")
}

private fun buildRegroupedTitle(
    originalTitle: String,
    selectedIndexes: List<Int>,
    selectedGroupName: String?
): String? {
    if (selectedIndexes.isEmpty() || selectedGroupName.isNullOrBlank()) return null
    val originalParts = originalTitle.split("-").map { it.trim() }.filter { it.isNotEmpty() }.toMutableList()
    val replacementParts = selectedGroupName.split("-").map { it.trim() }.filter { it.isNotEmpty() }
    if (replacementParts.isEmpty()) return null
    val requiredSize = (selectedIndexes.maxOrNull() ?: 0) + 1
    while (originalParts.size < requiredSize) {
        originalParts.add("")
    }
    selectedIndexes.forEachIndexed { position, partIndex ->
        replacementParts.getOrNull(position)?.let { originalParts[partIndex] = it }
    }
    return originalParts.filter { it.isNotBlank() }.joinToString("-").ifBlank { null }
}

@Composable
private fun ResourceGroupedPage(
    group: ResourceTitleGroup,
    categories: List<TagCategoryEntity>,
    onBack: () -> Unit,
    onPreview: (ResourceWithTagsCharacters) -> Unit,
    onLongClick: (ResourceWithTagsCharacters) -> Unit,
    backLabel: String = group.title
) {
    Column(modifier = Modifier.fillMaxSize()) {
        TextButton(onClick = onBack, modifier = Modifier.padding(horizontal = 8.dp)) {
            Text("返回 $backLabel")
        }
        LazyColumn(
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(group.resources, key = { it.resource.id }) { resource ->
                ResourceListRow(
                    resource = resource,
                    categories = categories,
                    roleText = resource.characters.joinToString("/") { it.name }.ifBlank { "无角色" },
                    onClick = { onPreview(resource) },
                    onLongClick = { onLongClick(resource) }
                )
            }
        }
    }
}


private fun titleParts(title: String): List<String> = title.split("-").map { it.trim() }.filter { it.isNotEmpty() }

private fun applyGroupFilter(
    resources: List<ResourceWithTagsCharacters>,
    selectedLevel1: Set<String>,
    selectedPairs: Set<Pair<String, String>>
): List<ResourceWithTagsCharacters> {
    if (selectedLevel1.isEmpty() && selectedPairs.isEmpty()) return resources
    return resources.filter { item ->
        val parts = titleParts(item.resource.title)
        val first = parts.getOrNull(0)
        val second = parts.getOrNull(1)
        val matchLevel1 = first != null && selectedLevel1.contains(first)
        val matchPair = first != null && second != null && selectedPairs.contains(first to second)
        matchLevel1 || matchPair
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FilterGroupDialog(
    resources: List<ResourceWithTagsCharacters>,
    selectedLevel1: Set<String>,
    onConfirm: (Set<String>, Set<Pair<String, String>>) -> Unit,
    onDismiss: () -> Unit
) {
    val allLevel1 = remember(resources) {
        resources.mapNotNull { titleParts(it.resource.title).getOrNull(0) }.distinct().sorted()
    }
    var level1Selected by remember(selectedLevel1) { mutableStateOf(selectedLevel1) }
    val compactTextStyle = TextStyle(fontSize = 12.sp)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("分组筛选") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("第一层分组")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    allLevel1.forEach { first ->
                        FilterChip(
                            modifier = Modifier.height(28.dp),
                            selected = level1Selected.contains(first),
                            onClick = {
                                level1Selected = if (level1Selected.contains(first)) {
                                    level1Selected - first
                                } else {
                                    level1Selected + first
                                }
                            },
                            label = { Text(first, style = compactTextStyle, maxLines = 1) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onConfirm(level1Selected.toSet(), emptySet())
                onDismiss()
            }) { Text("确定") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

@Composable
private fun GroupKeywordDialog(
    initialKeyword: String,
    onConfirm: (String) -> Unit,
    onClear: () -> Unit,
    onDismiss: () -> Unit
) {
    var keyword by remember(initialKeyword) { mutableStateOf(initialKeyword) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("分组关键字") },
        text = {
            OutlinedTextField(
                value = keyword,
                onValueChange = { keyword = it },
                singleLine = true,
                label = { Text("输入关键字") },
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(keyword.trim()) }) { Text("确定") }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                TextButton(onClick = onClear) { Text("取消关键字") }
                TextButton(onClick = onDismiss) { Text("关闭") }
            }
        }
    )
}

private fun typeLabel(type: ResourceType): String = when (type) {
    ResourceType.FLOW -> "流程"
    ResourceType.IMAGE -> "图片"
    ResourceType.VIDEO -> "视频"
    ResourceType.SOUND -> "声音"
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
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 360.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                TagSelectionSection(
                    label = "标签",
                    categories = categories,
                    tags = tags,
                    selected = selected,
                    onChange = { selected = it.toMutableSet() }
                )
            }
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
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 360.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text("角色")
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    FilterChip(
                        selected = current == null,
                        onClick = { current = null },
                        label = {
                            Text(
                                text = "全部角色",
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        modifier = Modifier.height(30.dp),
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
                            label = {
                                Text(
                                    text = character.name,
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            modifier = Modifier.height(30.dp),
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
    initialTitle: String,
    vm: ResourceViewModel,
    onBack: () -> Unit
) {
    var title by remember(mode, initialTitle) { mutableStateOf(initialTitle) }
    var textContent by remember { mutableStateOf("") }
    var magicScriptAutoFilled by remember(mode) { mutableStateOf(false) }
    var description by remember { mutableStateOf("") }
    var sceneMessages by remember { mutableStateOf<List<SceneMessageDraft>>(emptyList()) }
    var flowItems by remember { mutableStateOf<List<FlowUpdateItem>>(emptyList()) }
    var selectedTags by remember { mutableStateOf(setOf<Long>()) }
    var selectedCharacters by remember { mutableStateOf(setOf<Long>()) }
    var imageUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var videoUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var soundUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
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
    val soundPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        soundUris = uris
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
        CreateMode.SoundGroup -> "上传声音组"
    }

    val titleLabel = when (mode) {
        CreateMode.Flow -> "流程名"
        CreateMode.Text -> "文本名"
        CreateMode.Scene -> "情景名"
        CreateMode.ImageGroup -> "名称"
        CreateMode.VideoGroup -> "名称"
        CreateMode.SoundGroup -> "名称"
    }

    val canSubmit = title.isNotBlank() && selectedCharacters.isNotEmpty() && when (mode) {
        CreateMode.Flow -> flowItems.isNotEmpty()
        CreateMode.Text -> textContent.isNotBlank()
        CreateMode.ImageGroup -> true
        CreateMode.Scene -> sceneMessages.isNotEmpty()
        CreateMode.VideoGroup -> true
        CreateMode.SoundGroup -> true
    }

    val selectedSpeakerNames = remember(selectedCharacters, characters) {
        characters.filter { selectedCharacters.contains(it.id) }.map { it.name }
    }
    val flowResources = remember(availableResources) {
        availableResources.filter { it.resource.type != ResourceType.FLOW }
    }
    val magicDramaDefaultScript = remember(availableResources, characters) {
        buildMagicDramaDefaultScript(availableResources, characters)
    }

    LaunchedEffect(mode, title) {
        if (
            mode == CreateMode.Text &&
            title.contains("魔剧") &&
            !magicScriptAutoFilled &&
            textContent.isBlank()
        ) {
            textContent = magicDramaDefaultScript
            magicScriptAutoFilled = true
        }
    }

    LaunchedEffect(availableResources) {
        if (mode == CreateMode.Flow) {
            flowItems = refreshFlowItemsFromResources(flowItems, availableResources)
        }
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
                            CreateMode.SoundGroup -> vm.addSoundGroup(
                                title.trim(),
                                soundUris,
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
                .padding(16.dp)
                .padding(bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = {
                    title = it
                    if (mode == CreateMode.Text && !it.contains("魔剧")) {
                        magicScriptAutoFilled = false
                    }
                },
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
                                                ResourceType.TEXT -> item.text.orEmpty()
                                                ResourceType.SCENE -> "对话 ${item.sceneMessages.size} 条"
                                                ResourceType.IMAGE -> "图片 ${item.images.size} 张"
                                                ResourceType.VIDEO -> "视频 ${item.videos.size} 个"
                                                else -> ""
                                            }
                                            if (summary.isNotBlank()) {
                                                Text(
                                                    text = summary,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            IconButton(onClick = {
                                                if (index > 0) {
                                                    flowItems = flowItems.toMutableList().also {
                                                        it.add(index - 1, it.removeAt(index))
                                                    }
                                                }
                                            }) {
                                                Icon(Icons.Default.KeyboardArrowUp, contentDescription = "上移")
                                            }
                                            IconButton(onClick = {
                                                if (index < flowItems.lastIndex) {
                                                    flowItems = flowItems.toMutableList().also {
                                                        it.add(index + 1, it.removeAt(index))
                                                    }
                                                }
                                            }) {
                                                Icon(Icons.Default.KeyboardArrowDown, contentDescription = "下移")
                                            }
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
                    Text(if (imageUris.isEmpty()) "未选择图片（将创建空图片组）" else "已选择 ${imageUris.size} 张图片")
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
                                            Text(
                                                text = message.content,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            IconButton(onClick = {
                                                if (index > 0) {
                                                    sceneMessages = sceneMessages.toMutableList().also {
                                                        it.add(index - 1, it.removeAt(index))
                                                    }
                                                }
                                            }) {
                                                Icon(Icons.Default.KeyboardArrowUp, contentDescription = "上移")
                                            }
                                            IconButton(onClick = {
                                                if (index < sceneMessages.lastIndex) {
                                                    sceneMessages = sceneMessages.toMutableList().also {
                                                        it.add(index + 1, it.removeAt(index))
                                                    }
                                                }
                                            }) {
                                                Icon(Icons.Default.KeyboardArrowDown, contentDescription = "下移")
                                            }
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
                    Text(if (videoUris.isEmpty()) "未选择视频（将创建空视频组）" else "已选择 ${videoUris.size} 个视频")
                }
                CreateMode.SoundGroup -> {
                    TextButton(onClick = { soundPicker.launch(arrayOf("audio/*")) }) {
                        Icon(Icons.Default.MusicNote, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("选择音频")
                    }
                    Text(if (soundUris.isEmpty()) "未选择音频（将创建空声音组）" else "已选择 ${soundUris.size} 个音频")
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
    var soundItems by remember { mutableStateOf(parseSoundItems(resource.resource.contentUriOrPath)) }
    var flowItems by remember { mutableStateOf(parseFlowItems(resource.resource.sceneJson)) }
    var showSceneDialog by remember { mutableStateOf(false) }
    var editSceneIndex by remember { mutableStateOf<Int?>(null) }
    var showFlowDialog by remember { mutableStateOf(false) }
    var editFlowIndex by remember { mutableStateOf<Int?>(null) }
    var transferTarget by remember { mutableStateOf<MediaTransferTarget?>(null) }
    val pendingTransfers = remember { mutableStateListOf<PendingMediaTransfer>() }
    val scrollState = rememberScrollState()

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
    val soundPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        val updated = soundItems.toMutableList()
        uris.forEach { uri ->
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
            updated.add(SoundUpdateItem(uri = uri))
        }
        soundItems = updated
    }

    val selectedSpeakerNames = remember(selectedCharacters, characters) {
        characters.filter { selectedCharacters.contains(it.id) }.map { it.name }
    }
    val flowResources = remember(availableResources) {
        availableResources.filter { it.resource.type != ResourceType.FLOW }
    }

    LaunchedEffect(availableResources) {
        if (resource.resource.type == ResourceType.FLOW) {
            flowItems = refreshFlowItemsFromResources(flowItems, availableResources)
        }
    }

    val canSave = title.isNotBlank() && selectedCharacters.isNotEmpty() && when (resource.resource.type) {
        ResourceType.FLOW -> flowItems.isNotEmpty()
        ResourceType.TEXT -> textContent.isNotBlank()
        ResourceType.IMAGE -> imageItems.isNotEmpty()
        ResourceType.VIDEO -> videoItems.isNotEmpty()
        ResourceType.SOUND -> soundItems.isNotEmpty()
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
                        applyPendingTransfers(
                            pendingTransfers = pendingTransfers,
                            availableResources = availableResources,
                            vm = vm
                        )
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
                            ResourceType.SOUND -> vm.updateSoundGroup(
                                resource.resource,
                                title.trim(),
                                soundItems,
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
                .padding(16.dp)
                .padding(bottom = 96.dp),
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
                                            transferTarget = MediaTransferTarget(
                                                type = ResourceType.IMAGE,
                                                index = index,
                                                item = MediaTransferItem.Image(item)
                                            )
                                        }) {
                                            Icon(Icons.Default.DriveFileMove, contentDescription = "移动或复制")
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
                                val previewUri = item.uri ?: item.path?.let { Uri.parse(it) }
                                val thumbnail by produceState<android.graphics.Bitmap?>(initialValue = null, item) {
                                    value = previewUri?.let { uri ->
                                        withContext(Dispatchers.IO) { vm.decodeVideoFrame(uri) }
                                    }
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (thumbnail != null) {
                                        Image(
                                            bitmap = thumbnail!!.asImageBitmap(),
                                            contentDescription = "视频预览",
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
                                            Icon(Icons.Default.Videocam, contentDescription = null)
                                        }
                                    }
                                    Spacer(Modifier.width(12.dp))
                                    val label = item.path?.let { "视频 ${index + 1}" } ?: "新视频 ${index + 1}"
                                    Text(text = label, modifier = Modifier.weight(1f))
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
                                            transferTarget = MediaTransferTarget(
                                                type = ResourceType.VIDEO,
                                                index = index,
                                                item = MediaTransferItem.Video(item)
                                            )
                                        }) {
                                            Icon(Icons.Default.DriveFileMove, contentDescription = "移动或复制")
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
                ResourceType.SOUND -> {
                    TextButton(onClick = { soundPicker.launch(arrayOf("audio/*")) }) {
                        Icon(Icons.Default.MusicNote, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("追加音频")
                    }
                    if (soundItems.isEmpty()) {
                        Text("暂无音频", style = MaterialTheme.typography.labelSmall)
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            soundItems.forEachIndexed { index, item ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.MusicNote, contentDescription = null)
                                    Spacer(Modifier.width(12.dp))
                                    val label = item.path?.let { "音频 ${index + 1}" } ?: "新音频 ${index + 1}"
                                    Text(text = label, modifier = Modifier.weight(1f))
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        IconButton(onClick = {
                                            if (index > 0) {
                                                soundItems = soundItems.toMutableList().also {
                                                    it.add(index - 1, it.removeAt(index))
                                                }
                                            }
                                        }) {
                                            Icon(Icons.Default.KeyboardArrowUp, contentDescription = "上移")
                                        }
                                        IconButton(onClick = {
                                            if (index < soundItems.lastIndex) {
                                                soundItems = soundItems.toMutableList().also {
                                                    it.add(index + 1, it.removeAt(index))
                                                }
                                            }
                                        }) {
                                            Icon(Icons.Default.KeyboardArrowDown, contentDescription = "下移")
                                        }
                                        IconButton(onClick = {
                                            transferTarget = MediaTransferTarget(
                                                type = ResourceType.SOUND,
                                                index = index,
                                                item = MediaTransferItem.Sound(item)
                                            )
                                        }) {
                                            Icon(Icons.Default.DriveFileMove, contentDescription = "移动或复制")
                                        }
                                        IconButton(onClick = {
                                            soundItems = soundItems.toMutableList().also { it.removeAt(index) }
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
                                            Text(
                                                text = message.content,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            IconButton(onClick = {
                                                if (index > 0) {
                                                    sceneMessages = sceneMessages.toMutableList().also {
                                                        it.add(index - 1, it.removeAt(index))
                                                    }
                                                }
                                            }) {
                                                Icon(Icons.Default.KeyboardArrowUp, contentDescription = "上移")
                                            }
                                            IconButton(onClick = {
                                                if (index < sceneMessages.lastIndex) {
                                                    sceneMessages = sceneMessages.toMutableList().also {
                                                        it.add(index + 1, it.removeAt(index))
                                                    }
                                                }
                                            }) {
                                                Icon(Icons.Default.KeyboardArrowDown, contentDescription = "下移")
                                            }
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
                                            val title = item.title?.ifBlank { null }
                                            if (title != null) {
                                                Text(
                                                    text = title,
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                                Text(
                                                    text = typeLabel(item.type),
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            } else {
                                                Text(
                                                    text = typeLabel(item.type),
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                            val summary = when (item.type) {
                                                ResourceType.TEXT -> item.text.orEmpty()
                                                ResourceType.SCENE -> "对话 ${item.sceneMessages.size} 条"
                                                ResourceType.IMAGE -> "图片 ${item.images.size} 张"
                                                ResourceType.VIDEO -> "视频 ${item.videos.size} 个"
                                                else -> ""
                                            }
                                            if (summary.isNotBlank()) {
                                                Text(
                                                    text = summary,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            IconButton(onClick = {
                                                if (index > 0) {
                                                    flowItems = flowItems.toMutableList().also {
                                                        it.add(index - 1, it.removeAt(index))
                                                    }
                                                }
                                            }) {
                                                Icon(Icons.Default.KeyboardArrowUp, contentDescription = "上移")
                                            }
                                            IconButton(onClick = {
                                                if (index < flowItems.lastIndex) {
                                                    flowItems = flowItems.toMutableList().also {
                                                        it.add(index + 1, it.removeAt(index))
                                                    }
                                                }
                                            }) {
                                                Icon(Icons.Default.KeyboardArrowDown, contentDescription = "下移")
                                            }
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

    transferTarget?.let { target ->
        val targetCharacterIds = remember(resource) { resource.characters.map { it.id }.toSet() }
        val candidateGroups = remember(availableResources, targetCharacterIds, resource) {
            availableResources.filter {
                it.resource.type == resource.resource.type &&
                    it.resource.id != resource.resource.id &&
                    it.characters.map { character -> character.id }.toSet() == targetCharacterIds
            }
        }
        var selectedGroupId by remember(target) { mutableStateOf(candidateGroups.firstOrNull()?.resource?.id) }
        val canConfirm = selectedGroupId != null

        AlertDialog(
            onDismissRequest = { transferTarget = null },
            title = { Text("移动或复制") },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 320.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (candidateGroups.isEmpty()) {
                        Text("暂无可用的同角色资源组", style = MaterialTheme.typography.labelMedium)
                    } else {
                        Text("选择目标组", style = MaterialTheme.typography.labelMedium)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            candidateGroups.forEach { candidate ->
                                FilterChip(
                                    selected = selectedGroupId == candidate.resource.id,
                                    onClick = { selectedGroupId = candidate.resource.id },
                                    label = { Text(candidate.resource.title) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                    )
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(
                        onClick = {
                            val destinationId = selectedGroupId
                            if (destinationId != null) {
                                pendingTransfers.add(
                                    PendingMediaTransfer(
                                        destinationId = destinationId,
                                        item = target.item,
                                        mode = TransferMode.Copy
                                    )
                                )
                            }
                            transferTarget = null
                        },
                        enabled = canConfirm
                    ) { Text("复制") }
                    TextButton(
                        onClick = {
                            val destinationId = selectedGroupId
                            if (destinationId != null) {
                                pendingTransfers.add(
                                    PendingMediaTransfer(
                                        destinationId = destinationId,
                                        item = target.item,
                                        mode = TransferMode.Move
                                    )
                                )
                                when (target.type) {
                                    ResourceType.IMAGE -> {
                                        imageItems = imageItems.toMutableList().also { list ->
                                            if (target.index in list.indices) {
                                                list.removeAt(target.index)
                                            }
                                        }
                                    }
                                    ResourceType.VIDEO -> {
                                        videoItems = videoItems.toMutableList().also { list ->
                                            if (target.index in list.indices) {
                                                list.removeAt(target.index)
                                            }
                                        }
                                    }
                                    ResourceType.SOUND -> {
                                        soundItems = soundItems.toMutableList().also { list ->
                                            if (target.index in list.indices) {
                                                list.removeAt(target.index)
                                            }
                                        }
                                    }
                                    else -> Unit
                                }
                            }
                            transferTarget = null
                        },
                        enabled = canConfirm
                    ) { Text("移动") }
                }
            },
            dismissButton = { TextButton(onClick = { transferTarget = null }) { Text("取消") } }
        )
    }
}

private enum class TransferMode {
    Move,
    Copy
}

private sealed class MediaTransferItem {
    data class Image(val item: ImageUpdateItem) : MediaTransferItem()
    data class Video(val item: VideoUpdateItem) : MediaTransferItem()
    data class Sound(val item: SoundUpdateItem) : MediaTransferItem()
}

private data class MediaTransferTarget(
    val type: ResourceType,
    val index: Int,
    val item: MediaTransferItem
)

private data class PendingMediaTransfer(
    val destinationId: Long,
    val item: MediaTransferItem,
    val mode: TransferMode
)

private fun applyPendingTransfers(
    pendingTransfers: MutableList<PendingMediaTransfer>,
    availableResources: List<ResourceWithTagsCharacters>,
    vm: ResourceViewModel
) {
    if (pendingTransfers.isEmpty()) return
    val grouped = pendingTransfers.groupBy { it.destinationId }
    grouped.forEach { (destinationId, transfers) ->
        val destination = availableResources.firstOrNull { it.resource.id == destinationId } ?: return@forEach
        when (destination.resource.type) {
            ResourceType.IMAGE -> {
                val items = parseImageItems(
                    destination.resource.contentUriOrPath,
                    destination.resource.quoteImageBase64
                ).toMutableList()
                transfers.forEach { transfer ->
                    val item = (transfer.item as? MediaTransferItem.Image)?.item ?: return@forEach
                    items.add(item)
                }
                vm.updateImageGroup(
                    destination.resource,
                    destination.resource.title,
                    items,
                    destination.tags.map { it.id },
                    destination.characters.map { it.id }
                )
            }
            ResourceType.VIDEO -> {
                val items = parseVideoItems(destination.resource.contentUriOrPath).toMutableList()
                transfers.forEach { transfer ->
                    val item = (transfer.item as? MediaTransferItem.Video)?.item ?: return@forEach
                    items.add(item)
                }
                vm.updateVideoGroup(
                    destination.resource,
                    destination.resource.title,
                    items,
                    destination.tags.map { it.id },
                    destination.characters.map { it.id }
                )
            }
            ResourceType.SOUND -> {
                val items = parseSoundItems(destination.resource.contentUriOrPath).toMutableList()
                transfers.forEach { transfer ->
                    val item = (transfer.item as? MediaTransferItem.Sound)?.item ?: return@forEach
                    items.add(item)
                }
                vm.updateSoundGroup(
                    destination.resource,
                    destination.resource.title,
                    items,
                    destination.tags.map { it.id },
                    destination.characters.map { it.id }
                )
            }
            else -> Unit
        }
    }
    pendingTransfers.clear()
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
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 360.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                TagSelectionSection(
                    label = "标签",
                    categories = categories,
                    tags = tags,
                    selected = selected,
                    onChange = { selected = it.toMutableSet() }
                )
            }
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
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 360.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    characters.forEach { character ->
                        val isSelected = selected.contains(character.id)
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                if (isSelected) selected.remove(character.id) else selected.add(character.id)
                                selected = selected.toMutableSet()
                            },
                            label = {
                                Text(
                                    text = character.name,
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            modifier = Modifier.height(30.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    }
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
    val sortedTags = sortTagsForDisplay(tags, categories)
    val grouped = sortedTags.groupBy { it.categoryId }
    val knownCategoryIds = categories.map { it.id }.toSet()
    val uncategorized = sortedTags.filter { it.categoryId !in knownCategoryIds }
    val expandedState = remember(categories) {
        mutableStateOf(
            categories.associate { it.id to true }.toMutableMap()
        )
    }
    var uncategorizedExpanded by remember { mutableStateOf(true) }
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, style = MaterialTheme.typography.labelSmall)
        categories.forEach { category ->
            val items = grouped[category.id].orEmpty()
            if (items.isNotEmpty()) {
                val isExpanded = expandedState.value[category.id] ?: true
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            expandedState.value = expandedState.value.toMutableMap().apply {
                                put(category.id, !isExpanded)
                            }
                        },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(category.name, style = MaterialTheme.typography.labelSmall)
                    Spacer(Modifier.width(6.dp))
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = null
                    )
                }
                if (isExpanded) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items.forEach { tag ->
                            TagFilterChip(
                                tag = tag,
                                selected = selected,
                                onChange = onChange,
                                compact = true
                            )
                        }
                    }
                }
            }
        }
        if (uncategorized.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { uncategorizedExpanded = !uncategorizedExpanded },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("未分类", style = MaterialTheme.typography.labelSmall)
                Spacer(Modifier.width(6.dp))
                Icon(
                    imageVector = if (uncategorizedExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null
                )
            }
            if (uncategorizedExpanded) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    uncategorized.forEach { tag ->
                        TagFilterChip(
                            tag = tag,
                            selected = selected,
                            onChange = onChange,
                            compact = true
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TagFilterChip(
    tag: TagEntity,
    selected: Set<Long>,
    onChange: (Set<Long>) -> Unit,
    compact: Boolean = false
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
        label = {
            Text(
                text = tag.name,
                fontSize = if (compact) 12.sp else 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        modifier = if (compact) Modifier.height(30.dp) else Modifier,
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
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 360.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
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
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
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
                                        Text(
                                            text = summary,
                                            style = MaterialTheme.typography.bodySmall,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
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


private fun buildMagicDramaDefaultScript(
    availableResources: List<ResourceWithTagsCharacters>,
    characters: List<CharacterEntity>
): String {
    val roleA = characters.getOrNull(0)?.name ?: "角色A"
    val roleB = characters.getOrNull(1)?.name ?: "角色B"
    val imageSource = pickFirstImageSource(availableResources)
    val imageGroupSource = pickFirstImageGroupSource(availableResources)
    val videoSource = pickFirstVideoSource(availableResources)
    val videoGroupSource = pickFirstVideoGroupSource(availableResources)

    return buildString {
        appendLine("+旁白:魔剧自动样例开始，包含所有常用case。-1200")
        appendLine("+重要:[[注意,提示]]：你可以直接编辑这些行。-1200")
        imageSource?.let { appendLine("+图片:$it") }
        appendLine("+$roleA:收到，我先展示图片。nn如果你看到这句说明角色台词正常。-1600")
        videoSource?.let { appendLine("+视频:$it") }
        appendLine("+$roleB:现在切到视频，准备倒计时。-1600")
        imageGroupSource?.let { appendLine("+图片组:$it") }
        appendLine("+$roleA:这里是图片组轮播。-1200")
        videoGroupSource?.let { appendLine("+视频组:$it") }
        appendLine("+$roleB:这里是视频组轮播。-1200")
        appendLine("+倒计时:5")
        appendLine("+按钮:继续%go-结束%end")
        appendLine("+%go:旁白:你选择了继续分支。-1000")
        appendLine("+%go:$roleA:继续剧情测试通过。-1200")
        appendLine("+%end:旁白:你选择了结束分支，魔剧测试完成。-1000")
    }.trim()
}

private fun pickFirstImageGroupSource(resources: List<ResourceWithTagsCharacters>): String? {
    val imageResource = resources.firstOrNull { it.resource.type == ResourceType.IMAGE }?.resource ?: return null
    val source = parseImageItems(imageResource.contentUriOrPath, imageResource.quoteImageBase64)
        .mapNotNull { it.path }
        .firstOrNull()
        ?: return null
    return encodeResourceFileInfo(ResourceType.IMAGE, Uri.parse(source), imageResource.id)
}

private fun pickFirstImageSource(resources: List<ResourceWithTagsCharacters>): String? {
    val imageResource = resources.firstOrNull { it.resource.type == ResourceType.IMAGE }?.resource ?: return null
    val source = parseImageItems(imageResource.contentUriOrPath, imageResource.quoteImageBase64)
        .mapNotNull { it.path }
        .firstOrNull()
        ?: return null
    return encodeResourceFileInfo(ResourceType.IMAGE, Uri.parse(source), imageResource.id)
}

private fun pickFirstVideoSource(resources: List<ResourceWithTagsCharacters>): String? {
    val videoResource = resources.firstOrNull { it.resource.type == ResourceType.VIDEO }?.resource ?: return null
    val source = parseVideoItems(videoResource.contentUriOrPath).firstOrNull()?.path ?: return null
    return encodeResourceFileInfo(ResourceType.VIDEO, Uri.parse(source), videoResource.id)
}

private fun pickFirstVideoGroupSource(resources: List<ResourceWithTagsCharacters>): String? {
    val videoResource = resources.firstOrNull { it.resource.type == ResourceType.VIDEO }?.resource ?: return null
    val source = parseVideoItems(videoResource.contentUriOrPath).firstOrNull()?.path ?: return null
    return encodeResourceFileInfo(ResourceType.VIDEO, Uri.parse(source), videoResource.id)
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

private fun parseSoundItems(raw: String?): List<SoundUpdateItem> {
    if (raw.isNullOrBlank()) return emptyList()
    val list = parsePathList(raw)
    return list.map { SoundUpdateItem(path = it) }
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

private fun refreshFlowItemsFromResources(
    items: List<FlowUpdateItem>,
    resources: List<ResourceWithTagsCharacters>
): List<FlowUpdateItem> {
    if (items.isEmpty()) return items
    val resourceMap = resources.associateBy { it.resource.id }
    return items.map { item ->
        val resourceId = item.resourceId ?: return@map item
        val resource = resourceMap[resourceId] ?: return@map item
        flowItemFromResource(resource)
    }
}

@Composable
private fun resourceSummary(resource: ResourceWithTagsCharacters): String {
    val data = resource.resource
    return when (data.type) {
        ResourceType.TEXT -> data.quoteText.orEmpty()
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
        ResourceType.SOUND -> {
            val sounds = parseSoundItems(data.contentUriOrPath)
            if (sounds.isEmpty()) "" else "音频 ${sounds.size} 个"
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
