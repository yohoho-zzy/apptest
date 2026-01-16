package com.example.quotepicker.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.quotepicker.data.TagCategoryEntity
import com.example.quotepicker.data.TagEntity
import com.example.quotepicker.ui.components.AvatarListItem
import com.example.quotepicker.ui.components.NameDialog
import com.example.quotepicker.vm.TagViewModel
import androidx.compose.runtime.collectAsState
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TagScreen(modifier: Modifier = Modifier, vm: TagViewModel = viewModel()) {
    val ui by vm.uiState.collectAsState()
    var showCategoryDialog by remember { mutableStateOf(false) }
    var showTagDialog by remember { mutableStateOf(false) }
    var editCategory by remember { mutableStateOf<TagCategoryEntity?>(null) }
    var editTag by remember { mutableStateOf<TagEntity?>(null) }
    var bottomSheetTarget by remember { mutableStateOf<Any?>(null) }
    var deleteCategory by remember { mutableStateOf<TagCategoryEntity?>(null) }
    var deleteTag by remember { mutableStateOf<TagEntity?>(null) }

    val isInCategory = ui.currentCategory != null

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
                if (ui.categories.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("暂无标签类别，点击右下角添加")
                    }
                } else {
                    LazyColumn(
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(ui.categories, key = { it.id }) { category ->
                            AvatarListItem(
                                title = category.name,
                                onClick = { vm.selectCategory(category.id) },
                                onLongClick = { bottomSheetTarget = category }
                            )
                        }
                    }
                }
            } else {
                if (ui.tags.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("暂无标签，点击右下角添加")
                    }
                } else {
                    LazyColumn(
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(ui.tags, key = { it.id }) { tag ->
                            AvatarListItem(
                                title = tag.name,
                                avatarColor = Color(tag.colorArgb),
                                avatarTextColor = if (Color(tag.colorArgb).luminance() < 0.5f) Color.White else Color.Black,
                                onClick = {},
                                onLongClick = { bottomSheetTarget = tag }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showCategoryDialog) {
        NameDialog(
            title = "新增类别",
            initial = "",
            onConfirm = { vm.addCategory(it) },
            onDismiss = { showCategoryDialog = false }
        )
    }
    if (showTagDialog) {
        TagDialog(
            title = "新增标签",
            initialName = "",
            initialColor = 0xFFB388FF.toInt(),
            onConfirm = { name, color ->
                ui.currentCategory?.let { vm.addTag(it.id, name, color) }
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
            message = "确定删除“${tag.name}”吗？",
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
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
