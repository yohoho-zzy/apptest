package com.example.quotepicker.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.quotepicker.data.TagCategoryEntity
import com.example.quotepicker.data.TagCategoryType
import com.example.quotepicker.data.TagEntity
import com.example.quotepicker.ui.components.NameDialog
import com.example.quotepicker.ui.components.SquareGridItem
import com.example.quotepicker.ui.components.tagColorSortIndex
import com.example.quotepicker.ui.components.formatTagLabel
import com.example.quotepicker.ui.components.isPrefixGroupingCategory
import com.example.quotepicker.ui.components.splitTagsByPrefix
import com.example.quotepicker.vm.TagViewModel
import androidx.compose.runtime.collectAsState
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun TagScreen(modifier: Modifier = Modifier, vm: TagViewModel = viewModel()) {
    val ui by vm.uiState.collectAsState()
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current
    var showCategoryDialog by remember { mutableStateOf(false) }
    var showTagDialog by remember { mutableStateOf(false) }
    var editCategory by remember { mutableStateOf<TagCategoryEntity?>(null) }
    var editTag by remember { mutableStateOf<TagEntity?>(null) }
    var bottomSheetTarget by remember { mutableStateOf<Any?>(null) }
    var deleteCategory by remember { mutableStateOf<TagCategoryEntity?>(null) }
    var deleteTag by remember { mutableStateOf<TagEntity?>(null) }

    val isInCategory = ui.currentCategory != null
    val categoryTagCounts = remember(ui.allTags) {
        ui.allTags.groupingBy { it.categoryId }.eachCount()
    }
    val characterCategories = remember(ui.categories) {
        ui.categories.filter { it.type == TagCategoryType.CHARACTER }
    }
    val resourceCategories = remember(ui.categories) {
        ui.categories.filter { it.type == TagCategoryType.RESOURCE }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(if (isInCategory) ui.currentCategory?.name ?: "" else "标签类别") },
                navigationIcon = {
                    if (isInCategory) {
                        IconButton(onClick = { vm.selectCategory(null) }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                if (isInCategory) showTagDialog = true else showCategoryDialog = true
            }) {
                Icon(Icons.Default.Add, contentDescription = null)
            }
        }
    ) { inner ->
        Column(Modifier.padding(inner).fillMaxSize()) {
            if (!isInCategory) {
                if (characterCategories.isEmpty() && resourceCategories.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("暂无标签类别，点击右下角添加")
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(5),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            Text("角色标签", style = MaterialTheme.typography.titleMedium)
                        }
                        if (characterCategories.isEmpty()) {
                            item(span = { GridItemSpan(maxLineSpan) }) {
                                Text("暂无角色标签类别", style = MaterialTheme.typography.labelMedium)
                            }
                        } else {
                            items(characterCategories.sortedBy { it.name.lowercase() }, key = { it.id }) { category ->
                                val count = categoryTagCounts[category.id] ?: 0
                                SquareGridItem(
                                    title = category.name,
                                    subtitle = "标签 $count",
                                    onClick = { vm.selectCategory(category.id) },
                                    onLongClick = { bottomSheetTarget = category }
                                )
                            }
                        }
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            Spacer(Modifier.height(8.dp))
                        }
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            Text("资源标签", style = MaterialTheme.typography.titleMedium)
                        }
                        if (resourceCategories.isEmpty()) {
                            item(span = { GridItemSpan(maxLineSpan) }) {
                                Text("暂无资源标签类别", style = MaterialTheme.typography.labelMedium)
                            }
                        } else {
                            items(resourceCategories.sortedBy { it.name.lowercase() }, key = { it.id }) { category ->
                                val count = categoryTagCounts[category.id] ?: 0
                                SquareGridItem(
                                    title = category.name,
                                    subtitle = "标签 $count",
                                    onClick = { vm.selectCategory(category.id) },
                                    onLongClick = { bottomSheetTarget = category }
                                )
                            }
                        }
                    }
                }
            } else {
                if (ui.tags.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("暂无标签，点击右下角添加")
                    }
                } else {
                    val tags = ui.tags.sortedWith(
                        compareBy<TagEntity> { tagColorSortIndex(it.colorArgb) }
                            .thenBy { it.name.lowercase() }
                    )
                    val usePrefixGrouping = isPrefixGroupingCategory(ui.currentCategory?.name)
                    val groupingResult = remember(tags) { splitTagsByPrefix(tags) }
                    val expandedMap = remember(tags) {
                        mutableStateOf(groupingResult.groups.associate { it.name to true }.toMutableMap())
                    }
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(5),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (!usePrefixGrouping) {
                            items(tags, key = { it.id }) { tag ->
                                val bg = Color(tag.colorArgb)
                                val textColor = if (bg.luminance() < 0.5f) Color.White else Color.Black
                                val isUsed = tag.id in ui.usedTagIds
                                SquareGridItem(
                                    title = formatTagLabel(tag.name),
                                    backgroundColor = bg,
                                    contentColor = textColor,
                                    borderColor = if (isUsed) Color.Black else null,
                                    itemAspectRatio = 1.55f,
                                    titleTextStyle = MaterialTheme.typography.labelMedium,
                                    onClick = {},
                                    onLongClick = { bottomSheetTarget = tag }
                                )
                            }
                        } else {
                            groupingResult.groups.forEach { group ->
                                item(span = { GridItemSpan(maxLineSpan) }) {
                                    val isExpanded = expandedMap.value[group.name] ?: false
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .combinedClickable(
                                                onClick = {
                                                    expandedMap.value = expandedMap.value.toMutableMap().apply {
                                                        put(group.name, !isExpanded)
                                                    }
                                                },
                                                onLongClick = {}
                                            )
                                            .padding(vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = group.name,
                                            color = Color(0xFF795548),
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.titleSmall
                                        )
                                        Spacer(Modifier.width(4.dp))
                                        Icon(
                                            imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                            contentDescription = null,
                                            tint = Color(0xFF795548)
                                        )
                                    }
                                }
                                if (expandedMap.value[group.name] == true) {
                                    items(group.items, key = { it.tag.id }) { groupedTag ->
                                        val tag = groupedTag.tag
                                        val bg = Color(tag.colorArgb)
                                        val textColor = if (bg.luminance() < 0.5f) Color.White else Color.Black
                                        val isUsed = tag.id in ui.usedTagIds
                                        SquareGridItem(
                                            title = formatTagLabel(groupedTag.displayName),
                                            backgroundColor = bg,
                                            contentColor = textColor,
                                            borderColor = if (isUsed) Color.Black else null,
                                            itemAspectRatio = 1.55f,
                                            titleTextStyle = MaterialTheme.typography.labelMedium.copy(fontSize = 12.sp),
                                            onClick = {},
                                            onLongClick = { bottomSheetTarget = tag }
                                        )
                                    }
                                }
                            }
                            if (groupingResult.ungrouped.isNotEmpty()) {
                                item(span = { GridItemSpan(maxLineSpan) }) {
                                    Text(
                                        text = "未分组",
                                        color = Color(0xFF795548),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(vertical = 4.dp)
                                    )
                                }
                                items(groupingResult.ungrouped, key = { it.tag.id }) { groupedTag ->
                                    val tag = groupedTag.tag
                                    val bg = Color(tag.colorArgb)
                                    val textColor = if (bg.luminance() < 0.5f) Color.White else Color.Black
                                    val isUsed = tag.id in ui.usedTagIds
                                    SquareGridItem(
                                        title = formatTagLabel(groupedTag.displayName),
                                        backgroundColor = bg,
                                        contentColor = textColor,
                                        borderColor = if (isUsed) Color.Black else null,
                                        itemAspectRatio = 1.55f,
                                        titleTextStyle = MaterialTheme.typography.labelMedium.copy(fontSize = 12.sp),
                                        onClick = {},
                                        onLongClick = { bottomSheetTarget = tag }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCategoryDialog) {
        CategoryDialog(
            title = "新增类别",
            onConfirm = { name, type -> vm.addCategory(name, type) },
            onDismiss = { showCategoryDialog = false }
        )
    }
    if (showTagDialog) {
        TagDialog(
            title = "新增标签",
            initialName = "",
            initialColor = 0xFFB388FF.toInt(),
            onConfirm = { name, color ->
                ui.currentCategory?.let { category ->
                    val names = name
                        .split("+")
                        .map { it.trim() }
                        .filter { it.isNotBlank() }
                    vm.addTags(category.id, names, color)
                }
            },
            onDismiss = { showTagDialog = false }
        )
    }

    editCategory?.let { category ->
        NameDialog(
            title = "编辑类别",
            initial = category.name,
            onConfirm = { vm.updateCategory(category.copy(name = it)) },
            onDismiss = { editCategory = null }
        )
    }
    editTag?.let { tag ->
        TagDialog(
            title = "编辑标签",
            initialName = tag.name,
            initialColor = tag.colorArgb,
            onConfirm = { name, color -> vm.updateTag(tag.copy(name = name, colorArgb = color)) },
            onDismiss = { editTag = null }
        )
    }

    bottomSheetTarget?.let { target ->
        ModalBottomSheet(onDismissRequest = { bottomSheetTarget = null }) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                TextButton(onClick = {
                    when (target) {
                        is TagCategoryEntity -> editCategory = target
                        is TagEntity -> editTag = target
                    }
                    bottomSheetTarget = null
                }) {
                    Icon(Icons.Default.Edit, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("编辑")
                }
                TextButton(onClick = {
                    when (target) {
                        is TagCategoryEntity -> deleteCategory = target
                        is TagEntity -> deleteTag = target
                    }
                    bottomSheetTarget = null
                }) {
                    Icon(Icons.Default.Delete, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("删除")
                }
                if (target is TagCategoryEntity) {
                    TextButton(onClick = {
                        val tagsText = ui.allTags
                            .filter { it.categoryId == target.id }
                            .sortedBy { it.name.lowercase() }
                            .joinToString("\n") { formatTagLabel(it.name) }
                        clipboard.setText(AnnotatedString(tagsText))
                        Toast.makeText(context, "已复制${target.name}全部标签", Toast.LENGTH_SHORT).show()
                        bottomSheetTarget = null
                    }) {
                        Text("复制标签")
                    }
                }
                TextButton(onClick = { bottomSheetTarget = null }) {
                    Text("关闭")
                }
            }
            Spacer(Modifier.height(12.dp))
        }
    }

    deleteCategory?.let { category ->
        ConfirmDeleteDialog(
            title = "删除类别",
            message = "确定删除“${category.name}”吗？",
            onConfirm = { vm.deleteCategory(category) },
            onDismiss = { deleteCategory = null }
        )
    }
    deleteTag?.let { tag ->
        ConfirmDeleteDialog(
            title = "删除标签",
            message = "确定删除“${formatTagLabel(tag.name)}”吗？",
            onConfirm = { vm.deleteTag(tag) },
            onDismiss = { deleteTag = null }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TagDialog(
    title: String,
    initialName: String,
    initialColor: Int,
    onConfirm: (String, Int) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    val palette = listOf(
        0xFFFF8A80.toInt(), // 红
        0xFFFFF59D.toInt(), // 黄
        0xFFB388FF.toInt(), // 紫
        0xFF82B1FF.toInt(), // 蓝
        0xFFA5D6A7.toInt(), // 绿
        0xFFE0E0E0.toInt() // 灰
    )
    var colorIndex by remember {
        mutableIntStateOf(palette.indexOf(initialColor).takeIf { it >= 0 } ?: 0)
    }
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
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("标签名") },
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    palette.forEachIndexed { idx, color ->
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(Color(color))
                                .combinedClickable(
                                    onClick = { colorIndex = idx },
                                    onLongClick = {}
                                )
                        )
                    }
                }
                Text("当前颜色：${String.format("#%08X", palette[colorIndex])}")
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (name.isNotBlank()) onConfirm(name.trim(), palette[colorIndex])
                onDismiss()
            }) { Text("确定") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

@Composable
private fun CategoryDialog(
    title: String,
    onConfirm: (String, TagCategoryType) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(TagCategoryType.CHARACTER) }
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
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("类别名称") },
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = selectedType == TagCategoryType.CHARACTER,
                        onClick = { selectedType = TagCategoryType.CHARACTER },
                        label = { Text("角色标签") },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                    FilterChip(
                        selected = selectedType == TagCategoryType.RESOURCE,
                        onClick = { selectedType = TagCategoryType.RESOURCE },
                        label = { Text("资源标签") },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (name.isNotBlank()) onConfirm(name.trim(), selectedType)
                onDismiss()
            }) { Text("确定") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

@Composable
private fun ConfirmDeleteDialog(
    title: String,
    message: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = {
                onConfirm()
                onDismiss()
            }) { Text("删除") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}
