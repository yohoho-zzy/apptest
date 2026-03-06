package com.example.quotepicker.ui

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector

private enum class MainTab(val title: String, val icon: ImageVector) {
    TAG("标签", Icons.Default.LocalOffer),
    CHARACTER("角色", Icons.Default.Person),
    RESOURCE("资源", Icons.Default.Folder),
    EXECUTION("执行", Icons.Default.Casino)
}

data class MagicSettings(
    val rounds: Int,
    val intervalMs: Long,
    val speechRate: Float,
    val speechPitch: Float
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    var currentTab by remember { mutableStateOf(MainTab.TAG) }
    val tabs = remember { MainTab.values().toList() }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            NavigationBar {
                tabs.forEach { tab ->
                    NavigationBarItem(
                        selected = currentTab == tab,
                        onClick = { currentTab = tab },
                        icon = { Icon(tab.icon, contentDescription = null) },
                        label = { Text(tab.title) }
                    )
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
            onAddImage = { gid, b64, w -> vm.addImageQuote(gid, b64, w) },
            vm = vm
        )
    }

    if (showPreview) {
        MagicPreviewDialog(
            title = previewTitle,
            quote = currentMagicQuote,
            decodeImage = vm::decodeBase64ToBitmap,
            onDismiss = {
                showPreview = false
                currentMagicQuote = null
                previewTitle = "魔剧进行中"
                tts.stop()
            }
        )
    }
}

private fun weightedPick(items: List<QuoteEntity>): QuoteEntity? {
    val enabledItems = items.filter { it.enabled && it.weight > 0 }
    if (enabledItems.isEmpty()) return null
    val totalWeight = enabledItems.sumOf { it.weight }
    val lucky = (1..totalWeight).random()
    var acc = 0
    enabledItems.forEach { item ->
        acc += item.weight
        if (lucky <= acc) return item
    }
    return enabledItems.first()
}

@Composable
private fun MagicPanel(
    roundsText: String,
    intervalText: String,
    speechRateText: String,
    speechPitchText: String,
    onRoundsChange: (String) -> Unit,
    onIntervalChange: (String) -> Unit,
    onSpeechRateChange: (String) -> Unit,
    onSpeechPitchChange: (String) -> Unit,
    onStart: () -> Unit
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("魔剧播放器", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Button(onClick = onStart, modifier = Modifier.fillMaxWidth()) { Text("开始魔剧") }
            Text("可编辑参数（默认值）", style = MaterialTheme.typography.labelLarge)
            OutlinedTextField(value = roundsText, onValueChange = onRoundsChange, label = { Text("播放条数(1-30)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            OutlinedTextField(value = intervalText, onValueChange = onIntervalChange, label = { Text("每条间隔毫秒(500-8000)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = speechRateText, onValueChange = onSpeechRateChange, label = { Text("语速0.5-2") }, modifier = Modifier.weight(1f), singleLine = true)
                OutlinedTextField(value = speechPitchText, onValueChange = onSpeechPitchChange, label = { Text("音调0.5-2") }, modifier = Modifier.weight(1f), singleLine = true)
            }
        }
    }
}

@Composable
private fun MagicPreviewDialog(
    title: String,
    quote: QuoteEntity?,
    decodeImage: (String) -> Bitmap,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("关闭") } },
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Brush.horizontalGradient(listOf(Color(0xFF8EC5FC), Color(0xFFE0C3FC)))),
                    contentAlignment = Alignment.Center
                ) {
                    Text("魔剧预览 · 固定舞台", color = Color(0xFF28304A), fontWeight = FontWeight.SemiBold)
                }

                Surface(
                    modifier = Modifier.fillMaxWidth().heightIn(min = 180.dp),
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFFF8F4FF)
                ) {
                    Box(modifier = Modifier.padding(14.dp), contentAlignment = Alignment.Center) {
                        if (quote == null) {
                            Text("等待内容...", color = Color(0xFF6B6B7A))
                        } else if (quote.type == QuoteType.TEXT) {
                            Text(quote.text.orEmpty(), style = MaterialTheme.typography.titleMedium, color = Color(0xFF2F2A3D))
                        } else {
                            val bmp = remember(quote.imageBase64) { decodeImage(quote.imageBase64.orEmpty()) }
                            Image(bitmap = bmp.asImageBitmap(), contentDescription = null, modifier = Modifier.fillMaxWidth())
                        }
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxWidth().height(42.dp),
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFFFF1DB)
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("固定字幕区：文本展示时自动语音朗读", style = MaterialTheme.typography.bodySmall, color = Color(0xFF6A4A11))
                    }
                }
            }
        }
    )
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
        Box(Modifier.fillMaxWidth().height(220.dp), contentAlignment = Alignment.Center) {
            Text("该分组下暂无语录，点击下方 + 号添加")
        }
    } else {
        LazyColumn(
            modifier = Modifier.heightIn(min = 260.dp, max = 520.dp),
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            when (currentTab) {
                MainTab.TAG -> TagScreen()
                MainTab.CHARACTER -> CharacterScreen()
                MainTab.RESOURCE -> ResourceScreen()
                MainTab.EXECUTION -> ExecutionScreen()
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
