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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier

data class MagicSettings(
    val rounds: Int,
    val intervalMs: Long,
    val speechRate: Float,
    val speechPitch: Float
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(vm: MainViewModel = viewModel()) {
    val ui by vm.uiState.collectAsState()

    var showAddGroup by remember { mutableStateOf(false) }
    var showAddQuote by remember { mutableStateOf(false) }
    var showPreview by remember { mutableStateOf(false) }
    var currentMagicQuote by remember { mutableStateOf<QuoteEntity?>(null) }
    var previewTitle by remember { mutableStateOf("魔剧进行中") }
    var playSessionId by remember { mutableIntStateOf(0) }

    var roundsText by remember { mutableStateOf("8") }
    var intervalText by remember { mutableStateOf("1500") }
    var speechRateText by remember { mutableStateOf("1.0") }
    var speechPitchText by remember { mutableStateOf("1.0") }

    val context = LocalContext.current
    var ttsReady by remember { mutableStateOf(false) }
    val tts = remember {
        TextToSpeech(context) { status ->
            ttsReady = status == TextToSpeech.SUCCESS
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            tts.stop()
            tts.shutdown()
        }
    }

    val effectiveSettings = remember(roundsText, intervalText, speechRateText, speechPitchText) {
        MagicSettings(
            rounds = roundsText.toIntOrNull()?.coerceIn(1, 30) ?: 8,
            intervalMs = intervalText.toLongOrNull()?.coerceIn(500L, 8000L) ?: 1500L,
            speechRate = speechRateText.toFloatOrNull()?.coerceIn(0.5f, 2f) ?: 1.0f,
            speechPitch = speechPitchText.toFloatOrNull()?.coerceIn(0.5f, 2f) ?: 1.0f
        )
    }

    LaunchedEffect(playSessionId) {
        if (playSessionId == 0 || ui.quotes.isEmpty()) return@LaunchedEffect

        tts.language = Locale.CHINA
        tts.setSpeechRate(effectiveSettings.speechRate)
        tts.setPitch(effectiveSettings.speechPitch)

        repeat(effectiveSettings.rounds) { index ->
            val picked = weightedPick(ui.quotes)
            currentMagicQuote = picked
            if (picked?.type == QuoteType.TEXT && ttsReady) {
                tts.speak(picked.text.orEmpty(), TextToSpeech.QUEUE_FLUSH, null, "magic_$index")
            }
            delay(effectiveSettings.intervalMs)
        }
        previewTitle = "魔剧播放完成"
    }

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
            ExtendedFloatingActionButton(
                onClick = { showAddQuote = true },
                icon = { Icon(Icons.Default.Add, null) },
                text = { Text("添加") }
            )
        }
    ) { inner ->
        Column(
            modifier = Modifier
                .padding(inner)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            GroupTabs(groups = ui.groups, current = ui.currentGroupId, onSelect = vm::setGroup)

            MagicPanel(
                roundsText = roundsText,
                intervalText = intervalText,
                speechRateText = speechRateText,
                speechPitchText = speechPitchText,
                onRoundsChange = { roundsText = it },
                onIntervalChange = { intervalText = it },
                onSpeechRateChange = { speechRateText = it },
                onSpeechPitchChange = { speechPitchText = it },
                onStart = {
                    if (ui.quotes.isEmpty()) {
                        previewTitle = "暂无可播放语录"
                        currentMagicQuote = null
                        showPreview = true
                    } else {
                        previewTitle = "魔剧进行中"
                        showPreview = true
                        playSessionId += 1
                    }
                }
            )

            if (ui.groups.isEmpty()) {
                Box(Modifier.fillMaxWidth().height(220.dp), contentAlignment = Alignment.Center) {
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
                            AssistChip(onClick = { }, label = { Text("权重: ${q.weight}") })
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
