package com.example.quotepicker.ui

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.quotepicker.data.GroupEntity
import com.example.quotepicker.data.QuoteEntity
import com.example.quotepicker.data.QuoteType
import com.example.quotepicker.vm.MainViewModel
import com.example.quotepicker.ui.components.AddQuoteDialog
import com.example.quotepicker.ui.components.EditQuoteDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(vm: MainViewModel = viewModel()) {
    val ui by vm.uiState.collectAsState()

    var showAddGroup by remember { mutableStateOf(false) }
    var showAddQuote by remember { mutableStateOf(false) }
    var showResult by remember { mutableStateOf(false) }
    var currentTab by remember { mutableStateOf(0) } // 0: draw, 1: manage
    var editGroup by remember { mutableStateOf<GroupEntity?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (currentTab == 0) "抽取" else "数据管理") },
                actions = {
                    if (currentTab == 1) {
                        IconButton(onClick = { showAddGroup = true }) {
                            Icon(Icons.Default.Add, contentDescription = "添加分组")
                        }
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = currentTab == 0,
                    onClick = { currentTab = 0 },
                    icon = { Icon(Icons.Default.Casino, contentDescription = "抽取") },
                    label = { Text("抽取") }
                )
                NavigationBarItem(
                    selected = currentTab == 1,
                    onClick = { currentTab = 1 },
                    icon = { Icon(Icons.Default.List, contentDescription = "管理") },
                    label = { Text("管理") }
                )
            }
        },
        floatingActionButton = {
            if (currentTab == 0) {
                FloatingActionButton(onClick = {
                    vm.pickRandom()
                    showResult = true
                }) { Icon(Icons.Default.Casino, contentDescription = null) }
            } else {
                FloatingActionButton(onClick = { showAddQuote = true }) {
                    Icon(Icons.Default.Add, contentDescription = null)
                }
            }
        }
    ) { inner ->
        when (currentTab) {
            0 -> {
                Column(Modifier.padding(inner)) {
                    GroupTabs(
                        groups = ui.groups,
                        current = ui.currentGroupId,
                        onSelect = vm::setGroup,
                        onDelete = vm::deleteGroup,
                        onEdit = { editGroup = it }
                    )
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("点击右下角按钮抽取语录")
                    }
                }
            }
            else -> {
                Column(Modifier.padding(inner)) {
                    GroupTabs(
                        groups = ui.groups,
                        current = ui.currentGroupId,
                        onSelect = vm::setGroup,
                        onDelete = vm::deleteGroup,
                        onEdit = { editGroup = it }
                    )
                    if (ui.groups.isEmpty()) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("暂无分组，点击右上角添加")
                        }
                    } else {
                        QuoteList(
                            quotes = ui.quotes,
                            decodeImage = { vm.decodeBase64ToBitmap(it) },
                            onDelete = vm::deleteQuote,
                            onUpdate = vm::updateQuote
                        )
                    }
                }
            }
        }
    }

    if (showAddGroup) {
        AddGroupDialog(onDismiss = { showAddGroup = false }, onConfirm = { name -> vm.addGroup(name); showAddGroup = false })
    }
    if (showAddQuote) {
        AddQuoteDialog(
            groups = ui.groups,
            onDismiss = { showAddQuote = false },
            onAddText = { gid, text, w -> vm.addTextQuote(gid, text, w) },
            onAddImage = { gid, b64, text, w -> vm.addImageQuote(gid, b64, text, w) },
            vm = vm
        )
    }

    editGroup?.let { g ->
        EditGroupDialog(
            group = g,
            onDismiss = { editGroup = null },
            onConfirm = { name -> vm.updateGroup(g.copy(name = name)); editGroup = null }
        )
    }

    if (showResult) {
        AlertDialog(
            onDismissRequest = { showResult = false; vm.clearRandom() },
            confirmButton = { TextButton(onClick = { showResult = false; vm.clearRandom() }) { Text("关闭") } },
            title = { Text("抽取结果") },
            text = {
                val q = ui.randomResult
                if (q == null) {
                    Text("没有可抽取的内容")
                } else if (q.type == QuoteType.TEXT) {
                    Text(q.text ?: "")
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (!q.text.isNullOrBlank()) {
                            Text(q.text)
                        }
                        val bmp: Bitmap = vm.decodeBase64ToBitmap(q.imageBase64 ?: "")
                        Image(bitmap = bmp.asImageBitmap(), contentDescription = null, modifier = Modifier.fillMaxWidth())
                    }
                }
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun GroupTabs(
    groups: List<GroupEntity>,
    current: Long?,
    onSelect: (Long?) -> Unit,
    onDelete: (GroupEntity) -> Unit,
    onEdit: (GroupEntity) -> Unit
) {
    FlowRow(
        Modifier.fillMaxWidth().padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterChip(
            selected = current == null,
            onClick = { onSelect(null) },
            label = { Text("全部") }
        )
        groups.forEach { g ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                FilterChip(
                    selected = current == g.id,
                    onClick = { onSelect(g.id) },
                    label = { Text(g.name) }
                )
                IconButton(onClick = { onEdit(g) }) {
                    Icon(Icons.Default.Edit, contentDescription = "编辑分组")
                }
                IconButton(onClick = { onDelete(g); if (current == g.id) onSelect(null) }) {
                    Icon(Icons.Default.Close, contentDescription = "删除分组")
                }
            }
        }
    }
}

@Composable
private fun QuoteList(
    quotes: List<QuoteEntity>,
    decodeImage: (String)->android.graphics.Bitmap,
    onDelete: (QuoteEntity)->Unit,
    onUpdate: (QuoteEntity)->Unit
) {
    var editQuote by remember { mutableStateOf<QuoteEntity?>(null) }
    if (quotes.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("该分组下暂无语录，点击下方 + 号添加")
        }
    } else {
        LazyColumn(
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(quotes, key = { it.id }) { q ->
                ElevatedCard(Modifier.fillMaxWidth().clickable { editQuote = q }) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        var preview by remember { mutableStateOf(false) }
                        if (q.type == QuoteType.TEXT) {
                            Text(q.text.orEmpty(), style = MaterialTheme.typography.titleMedium)
                        } else {
                            Text(q.text.orEmpty(), style = MaterialTheme.typography.titleMedium)
                            if (preview) {
                                val bmp = remember(q.imageBase64) { decodeImage(q.imageBase64.orEmpty()) }
                                Image(bitmap = bmp.asImageBitmap(), contentDescription = null, modifier = Modifier.fillMaxWidth())
                            }
                        }
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AssistChip(onClick = {}, label = { Text("权重: ${q.weight}") })
                            if (q.type == QuoteType.IMAGE) {
                                TextButton(onClick = { preview = !preview }) { Text(if (preview) "隐藏" else "预览") }
                            }
                            Spacer(Modifier.weight(1f))
                            TextButton(onClick = { onDelete(q) }) { Text("删除") }
                        }
                    }
                }
            }
        }
    }
    editQuote?.let { q ->
        EditQuoteDialog(
            quote = q,
            onDismiss = { editQuote = null },
            onConfirm = { text, w -> onUpdate(q.copy(text = text, weight = w)); editQuote = null }
        )
    }
}

@Composable
private fun AddGroupDialog(onDismiss: ()->Unit, onConfirm: (String)->Unit) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("添加分组") },
        text = {
            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("分组名") }, modifier = Modifier.fillMaxWidth())
        },
        confirmButton = {
            Button(onClick = { if (name.isNotBlank()) onConfirm(name.trim()) }, enabled = name.isNotBlank()) { Text("确定") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

@Composable
private fun EditGroupDialog(group: GroupEntity, onDismiss: ()->Unit, onConfirm: (String)->Unit) {
    var name by remember { mutableStateOf(group.name) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("编辑分组") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("分组名") },
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(onClick = { if (name.isNotBlank()) onConfirm(name.trim()) }, enabled = name.isNotBlank()) {
                Text("确定")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}