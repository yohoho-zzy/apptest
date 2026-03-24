package com.example.quotepicker.ui

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Language
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
import kotlinx.coroutines.delay

enum class Corner { LEFT_TOP, RIGHT_TOP, LEFT_BOTTOM, RIGHT_BOTTOM, NONE }

data class GateWebItem(
    val name: String,
    val url: String
)

private fun normalizeUrl(rawUrl: String): String {
    val trimmed = rawUrl.trim()
    if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
        return trimmed
    }
    return "https://$trimmed"
}

@Composable
fun GateScreen(onPassed: () -> Unit) {
    var step by remember { mutableStateOf(0) }
    var count by remember { mutableStateOf(0) }
    var startTime by remember { mutableStateOf<Long?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }
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

    Box(modifier = Modifier.fillMaxSize()) {
        if (selectedItem == null) {
            GateWebListScreen(
                webItems = webItems,
                onAddClick = { showAddDialog = true },
                onItemClick = { selectedIndex = it }
            )
        } else {
            GateWebDetailScreen(
                item = selectedItem,
                onBack = { selectedIndex = null }
            )
        }

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
}

@Composable
private fun GateWebListScreen(
    webItems: List<GateWebItem>,
    onAddClick: () -> Unit,
    onItemClick: (Int) -> Unit
) {
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
            Spacer(modifier = Modifier.height(16.dp))
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
