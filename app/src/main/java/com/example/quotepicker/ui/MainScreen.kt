package com.example.quotepicker.ui

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Casino
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(vm: MainViewModel = viewModel()) {
    val ui by vm.uiState.collectAsState()

    var showAddGroup by remember { mutableStateOf(false) }
    var showAddQuote by remember { mutableStateOf(false) }
    var showResult by remember { mutableStateOf(false) }
    var result: QuoteEntity? by remember { mutableStateOf(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("语录随机器") },
                actions = {
                    IconButton(onClick = { showAddGroup = true }) {
                        Icon(Icons.Default.Add, contentDescription = "添加分组")
                    }
                }
            )
        },
        floatingActionButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(bottom = 12.dp)) {
                ExtendedFloatingActionButton(onClick = { showAddQuote = true }, icon = { Icon(Icons.Default.Add, null) }, text = { Text("添加") })
                ExtendedFloatingActionButton(onClick = {
                    vm.pickRandom()
                    result = vm.uiState.value.randomResult
                    showResult = true
                }, icon = { Icon(Icons.Default.Casino, null) }, text = { Text("抽一句") })
            }
        }
    ) { inner ->
        Column(Modifier.padding(inner)) {
            GroupTabs(groups = ui.groups, current = ui.currentGroupId, onSelect = vm::setGroup)

            if (ui.groups.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("暂无分组，点击右上角添加")
                }
            } else {
                QuoteList(
                    quotes = ui.quotes,
                    decodeImage = { vm.decodeBase64ToBitmap(it) },
                    onDelete = vm::deleteQuote
                )
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
            onAddImage = { gid, b64, w -> vm.addImageQuote(gid, b64, w) },
            vm = vm
        )
    }

    if (showResult) {
        AlertDialog(
            onDismissRequest = { showResult = false; vm.clearRandom() },
            confirmButton = { TextButton(onClick = { showResult = false; vm.clearRandom() }) { Text("关闭") } },
            title = { Text("抽取结果") },
            text = {
                val q = vm.uiState.value.randomResult ?: result
                if (q == null) {
                    Text("没有可抽取的内容")
                } else if (q.type == QuoteType.TEXT) {
                    Text(q.text ?: "")
                } else {
                    val bmp: Bitmap = vm.decodeBase64ToBitmap(q.imageBase64 ?: "")
                    Image(bitmap = bmp.asImageBitmap(), contentDescription = null, modifier = Modifier.fillMaxWidth())
                }
            }
        )
    }
}

@Composable
private fun GroupTabs(groups: List<GroupEntity>, current: Long?, onSelect: (Long?) -> Unit) {
    val selectedIndex = if (current == null) 0 else groups.indexOfFirst { it.id == current } + 1
    ScrollableTabRow(selectedTabIndex = selectedIndex) {
        Tab(
            selected = selectedIndex == 0,
            onClick = { onSelect(null) },
            text = { Text("全部") }
        )
        groups.forEachIndexed { i, g ->
            Tab(
                selected = selectedIndex == i + 1,
                onClick = { onSelect(g.id) },
                text = { Text(g.name) }
            )
        }
    }
}

@Composable
private fun QuoteList(
    quotes: List<QuoteEntity>,
    decodeImage: (String)->android.graphics.Bitmap,
    onDelete: (QuoteEntity)->Unit
) {
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
                ElevatedCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (q.type == com.example.quotepicker.data.QuoteType.TEXT) {
                            Text(q.text.orEmpty(), style = MaterialTheme.typography.titleMedium)
                        } else {
                            val bmp = remember(q.imageBase64) { decodeImage(q.imageBase64.orEmpty()) }
                            Image(bitmap = bmp.asImageBitmap(), contentDescription = null, modifier = Modifier.fillMaxWidth())
                        }
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AssistChip(onClick = { /* 未来：编辑权重 */ }, label = { Text("权重: ${q.weight}") })
                            Spacer(Modifier.weight(1f))
                            TextButton(onClick = { onDelete(q) }) { Text("删除") }
                        }
                    }
                }
            }
        }
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