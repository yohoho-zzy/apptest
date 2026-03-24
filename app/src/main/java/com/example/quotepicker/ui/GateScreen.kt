package com.example.quotepicker.ui

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.quotepicker.util.StoragePaths
import java.io.File
import kotlinx.coroutines.delay
import org.json.JSONArray
import org.json.JSONObject

enum class Corner { LEFT_TOP, RIGHT_TOP, LEFT_BOTTOM, RIGHT_BOTTOM, NONE }

data class GateWebItem(
    val name: String,
    val url: String
)

private const val GATE_WEB_EXPORT_FILENAME = "gate_websites.json"

private fun normalizeUrl(rawUrl: String): String {
    val trimmed = rawUrl.trim()
    if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
        return trimmed
    }
    return "https://$trimmed"
}

private fun serializeGateWebItems(items: List<GateWebItem>): String {
    val root = JSONObject()
    root.put("version", 1)
    root.put("items", JSONArray().apply {
        items.forEach { item ->
            put(
                JSONObject()
                    .put("name", item.name)
                    .put("url", item.url)
            )
        }
    })
    return root.toString(2)
}

private fun parseGateWebItems(jsonText: String): List<GateWebItem> {
    val trimmed = jsonText.trim()
    if (trimmed.isBlank()) return emptyList()

    fun parseArray(array: JSONArray): List<GateWebItem> {
        return buildList {
            for (i in 0 until array.length()) {
                val obj = array.optJSONObject(i) ?: continue
                val url = obj.optString("url").trim()
                if (url.isBlank()) continue
                val normalizedUrl = normalizeUrl(url)
                val name = obj.optString("name").trim().ifBlank { normalizedUrl }
                add(GateWebItem(name = name, url = normalizedUrl))
            }
        }
    }

    return if (trimmed.startsWith("[")) {
        parseArray(JSONArray(trimmed))
    } else {
        val root = JSONObject(trimmed)
        parseArray(root.optJSONArray("items") ?: JSONArray())
    }
    return "https://$trimmed"
}

@Composable
fun GateScreen(onPassed: () -> Unit) {
    var step by remember { mutableStateOf(0) }
    var count by remember { mutableStateOf(0) }
    var startTime by remember { mutableStateOf<Long?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }
    val webItems = remember {
        mutableStateListOf(
            GateWebItem(name = "Notion 表单", url = "https://www.notion.so")
        )
    }
    var selectedIndex by remember { mutableStateOf<Int?>(null) }

    val targets = listOf(
        Corner.RIGHT_TOP to 3,
        Corner.LEFT_TOP to 1,
        Corner.LEFT_BOTTOM to 2
    )

    LaunchedEffect(startTime) {
        while (true) {
            delay(500)
            val started = startTime ?: continue
            if (System.currentTimeMillis() - started > 10_000) {
                step = 0
                count = 0
                startTime = null
            }
        }
    }

    fun handleCornerTap(corner: Corner) {
        if (corner == Corner.NONE || step >= targets.size) return
        if (startTime == null) startTime = System.currentTimeMillis()
        val (expectCorner, expectTimes) = targets[step]
        if (corner == expectCorner) {
            count += 1
            if (count >= expectTimes) {
                step += 1
                count = 0
                if (step >= targets.size) {
                    onPassed()
                }
            }
        } else {
            step = 0
            count = 0
            startTime = null
        }
    }

    val selectedItem = selectedIndex?.let { webItems.getOrNull(it) }

    if (selectedItem == null) {
        Box(modifier = Modifier.fillMaxSize()) {
            GateWebListScreen(
                webItems = webItems,
                onAddClick = { showAddDialog = true },
                onImportClick = { showImportDialog = true },
                onExportClick = {
                    val context = it
                    runCatching {
                        val dir = StoragePaths.sysDir().apply { mkdirs() }
                        val target = File(dir, GATE_WEB_EXPORT_FILENAME)
                        target.writeText(serializeGateWebItems(webItems))
                        target
                    }.onSuccess { file ->
                        Toast.makeText(context, "已导出到 ${file.absolutePath}", Toast.LENGTH_SHORT).show()
                    }.onFailure { e ->
                        Toast.makeText(context, "导出失败: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                },
                onItemClick = { selectedIndex = it }
            )

            CornerHotspot(
                modifier = Modifier.align(Alignment.TopStart),
                onTap = { handleCornerTap(Corner.LEFT_TOP) }
            )
            CornerHotspot(
                modifier = Modifier.align(Alignment.TopEnd),
                onTap = { handleCornerTap(Corner.RIGHT_TOP) }
            )
            CornerHotspot(
                modifier = Modifier.align(Alignment.BottomStart),
                onTap = { handleCornerTap(Corner.LEFT_BOTTOM) }
            )
            CornerHotspot(
                modifier = Modifier.align(Alignment.BottomEnd),
                onTap = { handleCornerTap(Corner.RIGHT_BOTTOM) }
            )
        }
    } else {
        GateWebDetailScreen(
            item = selectedItem,
            onBack = { selectedIndex = null }
        )
    }

    if (showAddDialog) {
        var name by remember { mutableStateOf("") }
        var url by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("添加网页") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("名称") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = url,
                        onValueChange = { url = it },
                        label = { Text("URL") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val normalizedUrl = normalizeUrl(url)
                        webItems.add(
                            GateWebItem(
                                name = name.trim().ifBlank { normalizedUrl },
                                url = normalizedUrl
                            )
                        )
                        selectedIndex = webItems.lastIndex
                        showAddDialog = false
                    },
                    enabled = url.trim().isNotBlank()
                ) {
                    Text("追加")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("取消")
                }
            }
        )
    }

    if (showImportDialog) {
        val context = LocalContext.current
        val importFiles = remember(showImportDialog) {
            StoragePaths.sysDir().apply { mkdirs() }
                .listFiles { file -> file.isFile && file.extension.equals("json", ignoreCase = true) }
                ?.sortedByDescending { it.lastModified() }
                .orEmpty()
        }
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            title = { Text("导入网站记录（${StoragePaths.sysDir().absolutePath}）") },
            text = {
                if (importFiles.isEmpty()) {
                    Text("未找到 json 文件，先导出一次再导入。")
                } else {
                    LazyColumn {
                        items(importFiles) { file ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        runCatching {
                                            parseGateWebItems(file.readText())
                                        }.onSuccess { imported ->
                                            if (imported.isEmpty()) {
                                                Toast.makeText(context, "导入文件为空", Toast.LENGTH_SHORT).show()
                                            } else {
                                                webItems.clear()
                                                webItems.addAll(imported)
                                                selectedIndex = null
                                                showImportDialog = false
                                                Toast.makeText(context, "已导入 ${imported.size} 条", Toast.LENGTH_SHORT).show()
                                            }
                                        }.onFailure { e ->
                                            Toast.makeText(context, "导入失败: ${e.message}", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                    .padding(vertical = 10.dp)
                            ) {
                                Text(file.name, fontWeight = FontWeight.SemiBold)
                                Text(
                                    text = "${file.length()} bytes",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Divider()
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showImportDialog = false }) {
                    Text("关闭")
                }
            }
        )
    }
}

@Composable
private fun GateWebListScreen(
    webItems: List<GateWebItem>,
    onAddClick: () -> Unit,
    onImportClick: () -> Unit,
    onExportClick: (android.content.Context) -> Unit,
    onItemClick: (Int) -> Unit
) {
    val context = LocalContext.current
    Scaffold(
        containerColor = Color.Transparent,
        floatingActionButton = {
            FloatingActionButton(onClick = onAddClick) {
                Icon(Icons.Default.Add, contentDescription = "添加网页")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFFF7FAFF), Color(0xFFEFF4FF))
                    )
                )
                .padding(innerPadding)
                .padding(horizontal = 18.dp, vertical = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "N表单",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1D2A57)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "纯列表入口，点击项目后进入新页面查看网页内容。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF5B6585)
                    )
                }
                IconButton(onClick = onImportClick) {
                    Icon(Icons.Default.Download, contentDescription = "导入 JSON")
                }
                IconButton(onClick = { onExportClick(context) }) {
                    Icon(Icons.Default.Upload, contentDescription = "导出 JSON")
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Surface(
                modifier = Modifier.fillMaxSize(),
                shape = RoundedCornerShape(24.dp),
                tonalElevation = 2.dp,
                shadowElevation = 8.dp,
                color = Color.White.copy(alpha = 0.92f)
            ) {
                if (webItems.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "点击右下角 + 添加网页",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        itemsIndexed(webItems) { index, item ->
                            GateWebListItem(
                                item = item,
                                onClick = { onItemClick(index) }
                            )
                            if (index != webItems.lastIndex) {
                                Divider(modifier = Modifier.padding(start = 64.dp, end = 16.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GateWebListItem(
    item: GateWebItem,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Color(0xFFE8F0FF)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Language,
                contentDescription = null,
                tint = Color(0xFF315DDB)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = Color(0xFF1A2340)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = item.url,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF6B7491),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "打开",
            style = MaterialTheme.typography.labelMedium,
            color = Color(0xFF315DDB),
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun GateWebDetailScreen(
    item: GateWebItem,
    onBack: () -> Unit
) {
    Scaffold(
        containerColor = Color(0xFFF3F6FC),
        topBar = {
            Surface(shadowElevation = 4.dp, color = Color.White) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = item.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = item.url,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        ElevatedCard(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(12.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.elevatedCardColors(containerColor = Color.White)
        ) {
            GateWebPreview(url = item.url)
        }
    }
}

@Composable
private fun CornerHotspot(
    modifier: Modifier = Modifier,
    onTap: () -> Unit
) {
    Box(
        modifier = modifier
            .size(72.dp)
            .clickable(onClick = onTap)
    )
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun GateWebPreview(url: String) {
    val context = LocalContext.current
    var loading by remember(url) { mutableStateOf(true) }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = {
                WebView(context).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.loadsImagesAutomatically = true
                    settings.allowFileAccess = true
                    settings.useWideViewPort = true
                    settings.loadWithOverviewMode = true
                    settings.mediaPlaybackRequiresUserGesture = false
                    webChromeClient = WebChromeClient()
                    webViewClient = object : WebViewClient() {
                        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                            loading = true
                        }

                        override fun onPageFinished(view: WebView?, url: String?) {
                            loading = false
                        }

                        override fun shouldOverrideUrlLoading(
                            view: WebView?,
                            request: WebResourceRequest?
                        ): Boolean = false
                    }
                    loadUrl(url)
                }
            },
            update = { webView ->
                if (webView.url != url) {
                    webView.loadUrl(url)
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        if (loading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White.copy(alpha = 0.78f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color(0xFF315DDB))
            }
        }
    }
}
