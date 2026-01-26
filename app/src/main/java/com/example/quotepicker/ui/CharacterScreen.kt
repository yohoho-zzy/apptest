package com.example.quotepicker.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.clickable
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.quotepicker.data.CharacterEntity
import com.example.quotepicker.data.CharacterWithTags
import com.example.quotepicker.data.ResourceType
import com.example.quotepicker.data.TagCategoryEntity
import com.example.quotepicker.data.TagEntity
import com.example.quotepicker.ui.components.PreviewTextBlock
import com.example.quotepicker.ui.components.NameDialog
import com.example.quotepicker.ui.components.TagBadge
import com.example.quotepicker.ui.components.tagTextColor
import com.example.quotepicker.ui.components.ResourceGridCard
import com.example.quotepicker.ui.components.ResourcePreviewScreen
import com.example.quotepicker.ui.components.SquareGridItem
import com.example.quotepicker.ui.components.sortTagsForDisplay
import com.example.quotepicker.vm.CharacterViewModel
import com.example.quotepicker.vm.TransferMode
import com.example.quotepicker.vm.ResourceViewModel
import kotlinx.coroutines.launch
import android.widget.Toast
import java.util.Locale

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CharacterScreen(
    modifier: Modifier = Modifier,
    vm: CharacterViewModel = viewModel(),
    resourceVm: ResourceViewModel = viewModel()
) {
    val ui by vm.uiState.collectAsState()
    val transferState by vm.transferState.collectAsState()
    val resources by resourceVm.allResources.collectAsState()
    val resourceUi by resourceVm.uiState.collectAsState()
    var selectedId by remember { mutableStateOf<Long?>(null) }
    val selected = ui.characters.firstOrNull { it.character.id == selectedId }
    var showAddDialog by remember { mutableStateOf(false) }
    var editCharacter by remember { mutableStateOf<CharacterEntity?>(null) }
    var showTagPicker by remember { mutableStateOf(false) }
    var bottomSheetTarget by remember { mutableStateOf<CharacterWithTags?>(null) }
    var deleteTarget by remember { mutableStateOf<CharacterEntity?>(null) }
    var filterTagDialog by remember { mutableStateOf(false) }
    var selectedTagIds by remember { mutableStateOf(setOf<Long>()) }
    var selectedType by remember { mutableStateOf<ResourceType?>(null) }
    var previewTarget by remember { mutableStateOf<com.example.quotepicker.data.ResourceWithTagsCharacters?>(null) }
    val pagerState = rememberPagerState(pageCount = { 3 })
    val pagerScope = rememberCoroutineScope()
    val context = LocalContext.current
    val importPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            vm.importSnapshot(uri)
        }
    }
    val exportPicker = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        if (uri != null) {
            vm.exportSnapshot(uri)
        }
    }

    LaunchedEffect(selectedId) {
        selectedTagIds = emptySet()
        selectedType = null
    }

    if (previewTarget != null) {
        ResourcePreviewScreen(
            resource = previewTarget!!,
            vm = resourceVm,
            onBack = { previewTarget = null }
        )
        return
    }

    if (transferState.inProgress) {
        val label = when (transferState.mode) {
            TransferMode.EXPORT -> "正在导出..."
            TransferMode.IMPORT -> "正在导入..."
            null -> "处理中..."
        }
        val processedBytes = transferState.processedBytes
        val totalBytes = transferState.totalBytes
        val outputBytes = transferState.outputBytes
        val percent = if (processedBytes != null && totalBytes != null && totalBytes > 0) {
            ((processedBytes.toDouble() / totalBytes.toDouble()) * 100).coerceIn(0.0, 100.0)
        } else {
            null
        }
        AlertDialog(
            onDismissRequest = {},
            title = { Text(label) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("请保持应用在前台以完成任务")
                    if (percent != null && processedBytes != null && totalBytes != null) {
                        Text(
                            "进度：${percent.toInt()}% (${formatBytes(processedBytes)} / ${formatBytes(totalBytes)})"
                        )
                    }
                    if (transferState.mode == TransferMode.EXPORT && outputBytes != null) {
                        Text("文件大小：${formatBytes(outputBytes)}")
                    }
                    transferState.progress?.let { progress ->
                        LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
                    } ?: LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {}
        )
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(selected?.character?.name ?: "角色") },
                navigationIcon = {
                    if (selected != null) {
                        IconButton(onClick = { selectedId = null }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                        }
                    }
                },
                actions = {
                    if (selected == null) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(onClick = { importPicker.launch(arrayOf("application/zip", "application/json", "application/octet-stream")) }) {
                                Text("导入")
                            }
                            TextButton(onClick = { exportPicker.launch("quote_backup.zip") }) {
                                Text("导出")
                            }
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
                    val teamCategoryId = ui.categories.firstOrNull { it.name == "队伍体系" }?.id
                    val teamEntries = ui.characters.map { character ->
                        val teamTag = teamCategoryId?.let { categoryId ->
                            character.tags.firstOrNull { it.categoryId == categoryId }
                        }
                        val info = parseTeamInfo(teamTag)
                        CharacterTeamEntry(
                            character = character,
                            teamName = info.teamName,
                            levelLabel = info.levelLabel,
                            levelOrder = info.levelOrder,
                            borderColor = info.borderColor
                        )
                    }
                    val groupedTeams = teamEntries.groupBy { it.teamName }
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        groupedTeams.forEach { (teamName, members) ->
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text(text = teamName, style = MaterialTheme.typography.titleMedium)
                                val groupedLevels = members
                                    .groupBy { it.levelLabel }
                                    .toList()
                                    .sortedWith(compareBy({ it.second.first().levelOrder }, { it.first }))
                                groupedLevels.forEach { (_, levelMembers) ->
                                    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                                        val itemSpacing = 8.dp
                                        val itemSize = (maxWidth - itemSpacing * 4) / 5
                                        FlowRow(
                                            modifier = Modifier.fillMaxWidth(),
                                            maxItemsInEachRow = 5,
                                            horizontalArrangement = Arrangement.spacedBy(itemSpacing),
                                            verticalArrangement = Arrangement.spacedBy(itemSpacing)
                                        ) {
                                            levelMembers.forEach { entry ->
                                                SquareGridItem(
                                                    title = entry.character.character.name,
                                                    subtitle = entry.levelLabel,
                                                    borderColor = entry.borderColor,
                                                    subtitleOnTop = true,
                                                    subtitleColor = entry.borderColor,
                                                    subtitleFontWeight = FontWeight.Bold,
                                                    modifier = Modifier.width(itemSize),
                                                    bottomContent = {
                                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                            Text(
                                                                text = entry.character.character.points.toString(),
                                                                style = MaterialTheme.typography.labelSmall,
                                                                color = Color(0xFF2E7D32)
                                                            )
                                                            Text(
                                                                text = "${entry.character.character.probability}%",
                                                                style = MaterialTheme.typography.labelSmall,
                                                                color = Color(0xFF1565C0)
                                                            )
                                                        }
                                                    },
                                                    onClick = { selectedId = entry.character.character.id },
                                                    onLongClick = { bottomSheetTarget = entry.character }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                val char = selected!!.character
                val filteredResources = resources.filter {
                    it.characters.any { c -> c.id == char.id }
                }.filter { res ->
                    val typeMatch = selectedType?.let { it == res.resource.type } ?: true
                    val tagMatch = if (selectedTagIds.isEmpty()) true else res.tags.any { selectedTagIds.contains(it.id) }
                    typeMatch && tagMatch
                }
                val narrativeCategory = resourceUi.categories.firstOrNull { it.name == "叙事类别" }
                val narrativeTags = narrativeCategory?.let { category ->
                    resourceUi.tags.filter { it.categoryId == category.id }
                }.orEmpty()
                val introCandidates = resources.filter { res ->
                    res.characters.any { c -> c.id == char.id } &&
                        res.resource.type == ResourceType.TEXT &&
                        res.resource.title.take(2) == "要点"
                }
                val introText = introCandidates.firstOrNull()?.resource?.quoteText.orEmpty()
                TabRow(selectedTabIndex = pagerState.currentPage) {
                    listOf("标签", "要点", "资源").forEachIndexed { index, title ->
                        Tab(
                            selected = pagerState.currentPage == index,
                            onClick = { pagerScope.launch { pagerState.animateScrollToPage(index) } },
                            text = { Text(title) }
                        )
                    }
                }
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize()
                ) { page ->
                    when (page) {
                        0 -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState())
                            ) {
                                TagSummarySection(
                                    categories = ui.categories,
                                    tags = selected!!.tags,
                                    onEdit = { showTagPicker = true }
                                )
                            }
                        }
                        1 -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState())
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Button(onClick = {
                                        val current = char
                                        if (current.points <= 0) {
                                            vm.updateCharacterPoints(current.id, 30)
                                            val fallbackTag = narrativeTags.firstOrNull()
                                            if (fallbackTag != null) {
                                                vm.addResponseRecord(current.id, fallbackTag.id, count = 3)
                                                Toast.makeText(
                                                    context,
                                                    fillTemplate(
                                                        ui.executionSettings.successToast,
                                                        listOf(current.name, fallbackTag.name)
                                                    ),
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            } else {
                                                Toast.makeText(context, "未找到叙事类别标签", Toast.LENGTH_SHORT).show()
                                            }
                                        } else {
                                            vm.updateCharacterPoints(current.id, current.points - 1)
                                            val hit = (1..100).random() <= current.probability
                                            if (hit) {
                                                val pick = narrativeTags.randomOrNull()
                                                if (pick != null) {
                                                    vm.addResponseRecord(current.id, pick.id)
                                                    Toast.makeText(
                                                        context,
                                                        fillTemplate(
                                                            ui.executionSettings.successToast,
                                                            listOf(current.name, pick.name)
                                                        ),
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                } else {
                                                    Toast.makeText(context, "未找到叙事类别标签", Toast.LENGTH_SHORT).show()
                                                }
                                            } else {
                                                Toast.makeText(
                                                    context,
                                                    fillTemplate(ui.executionSettings.failureToast, listOf(current.name)),
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        }
                                    }) {
                                        Text(ui.executionSettings.buttonLabel)
                                    }
                                    Text(
                                        text = char.points.toString(),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = Color(0xFF2E7D32)
                                    )
                                    Text(
                                        text = "${char.probability}%",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = Color(0xFF1565C0)
                                    )
                                }
                                if (introText.isBlank()) {
                                    Text("暂无要点", style = MaterialTheme.typography.labelMedium)
                                } else {
                                    PreviewTextBlock(text = introText, onEventSequence = {})
                                }
                            }
                        }
                        2 -> {
                            Column(Modifier.fillMaxSize()) {
                                Spacer(Modifier.height(12.dp))
                                CharacterResourceFilterBar(
                                    selectedType = selectedType,
                                    selectedTagIds = selectedTagIds,
                                    onTypeChange = { selectedType = it },
                                    onTagDialog = { filterTagDialog = true }
                                )
                                if (filteredResources.isEmpty()) {
                                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        Text("暂无资源")
                                    }
                                } else {
                                    LazyVerticalGrid(
                                        columns = GridCells.Fixed(3),
                                        contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(12.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        items(filteredResources, key = { it.resource.id }) { resource ->
                                            ResourceGridCard(
                                                title = resource.resource.title,
                                                typeLabel = typeLabel(resource.resource.type),
                                                tags = sortTagsForDisplay(resource.tags, resourceUi.categories),
                                                onClick = { previewTarget = resource },
                                                onLongClick = {}
                                            )
                                        }
                                    }
                                }
                            }
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
            categories = resourceUi.categories,
            tags = resourceUi.tags,
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

private data class CharacterTeamEntry(
    val character: CharacterWithTags,
    val teamName: String,
    val levelLabel: String,
    val levelOrder: Int,
    val borderColor: Color?
)

private data class TeamTagInfo(
    val teamName: String,
    val levelLabel: String,
    val levelOrder: Int,
    val borderColor: Color?
)

private fun parseTeamInfo(tag: TagEntity?): TeamTagInfo {
    val defaultTeam = "未分队"
    val defaultLevel = "未设等级"
    if (tag == null) {
        return TeamTagInfo(defaultTeam, defaultLevel, Int.MAX_VALUE, null)
    }
    val parts = tag.name.split("|", limit = 2)
    val teamName = parts.firstOrNull()?.ifBlank { defaultTeam } ?: defaultTeam
    val levelLabel = parts.getOrNull(1)?.ifBlank { defaultLevel } ?: defaultLevel
    val levelOrder = Regex("\\d+").find(levelLabel)?.value?.toIntOrNull() ?: Int.MAX_VALUE
    return TeamTagInfo(teamName, levelLabel, levelOrder, Color(tag.colorArgb))
}

private fun formatBytes(bytes: Long): String {
    val units = listOf("B", "KB", "MB", "GB", "TB")
    var value = bytes.toDouble()
    var unitIndex = 0
    while (value >= 1024 && unitIndex < units.lastIndex) {
        value /= 1024
        unitIndex += 1
    }
    val format = when {
        value >= 100 || unitIndex == 0 -> "%.0f"
        value >= 10 -> "%.1f"
        else -> "%.2f"
    }
    return "${String.format(Locale.getDefault(), format, value)} ${units[unitIndex]}"
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
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 240.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
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
    val sortedTags = sortTagsForDisplay(tags, categories)
    val grouped = sortedTags.groupBy { it.categoryId }
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
        val uncategorized = sortedTags.filter { it.categoryId !in categoryMap.keys }
        if (uncategorized.isNotEmpty()) {
            Text("未分类", style = MaterialTheme.typography.labelMedium)
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
private fun CharacterResourceFilterBar(
    selectedType: ResourceType?,
    selectedTagIds: Set<Long>,
    onTypeChange: (ResourceType?) -> Unit,
    onTagDialog: () -> Unit
) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
    ResourceType.FLOW -> "流程"
    ResourceType.IMAGE -> "图片"
    ResourceType.VIDEO -> "视频"
    ResourceType.SOUND -> "声音"
    ResourceType.TEXT -> "文本"
    ResourceType.SCENE -> "情景"
}

private fun fillTemplate(template: String, values: List<String>): String {
    var result = template
    values.forEach { value ->
        result = result.replaceFirst("[]", value)
    }
    return result
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
    val sortedTags = sortTagsForDisplay(tags, categories)
    val grouped = sortedTags.groupBy { it.categoryId }
    val knownCategoryIds = categories.map { it.id }.toSet()
    val uncategorized = sortedTags.filter { it.categoryId !in knownCategoryIds }
    val expandedState = remember(categories) {
        mutableStateOf(categories.associate { it.id to true }.toMutableMap())
    }
    var uncategorizedExpanded by remember { mutableStateOf(true) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 360.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(label)
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
                    Text(category.name, style = MaterialTheme.typography.labelMedium)
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
                                        maxLines = 1,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    containerColor = tagColor.copy(alpha = 0.2f),
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
        if (uncategorized.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { uncategorizedExpanded = !uncategorizedExpanded },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("未分类", style = MaterialTheme.typography.labelMedium)
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
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = tagColor.copy(alpha = 0.2f),
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
}
