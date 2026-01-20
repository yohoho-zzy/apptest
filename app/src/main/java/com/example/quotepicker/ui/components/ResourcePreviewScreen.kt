package com.example.quotepicker.ui.components

import android.net.Uri
import android.util.Log
import android.widget.MediaController
import android.widget.VideoView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.quotepicker.data.ResourceType
import com.example.quotepicker.data.ResourceWithTagsCharacters
import com.example.quotepicker.ui.components.TagBadge
import com.example.quotepicker.vm.ResourceViewModel
import org.json.JSONArray

private data class SceneMessage(val speaker: String, val content: String)

private data class FlowPreviewItem(
    val type: ResourceType,
    val title: String? = null,
    val text: String = "",
    val sceneMessages: List<SceneMessage> = emptyList(),
    val images: List<android.graphics.Bitmap> = emptyList(),
    val mediaUris: List<Uri> = emptyList(),
    val mediaFailed: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
fun ResourcePreviewScreen(
    resource: ResourceWithTagsCharacters,
    vm: ResourceViewModel,
    onBack: () -> Unit
) {
    var quoteImages by remember { mutableStateOf<List<android.graphics.Bitmap>>(emptyList()) }
    var mediaUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var mediaLoadFailed by remember { mutableStateOf(false) }
    var mediaReloadKey by remember { mutableStateOf(0) }
    var sceneMessages by remember { mutableStateOf<List<SceneMessage>>(emptyList()) }
    var flowItems by remember { mutableStateOf<List<FlowPreviewItem>>(emptyList()) }
    val scrollState = rememberScrollState()

    LaunchedEffect(resource.resource.id, mediaReloadKey) {
        val res = resource.resource
        quoteImages = emptyList()
        mediaLoadFailed = false
        flowItems = emptyList()
        if (res.type == ResourceType.IMAGE) {
            quoteImages = decodeImageSources(res.contentUriOrPath ?: res.quoteImageBase64, vm)
        } else if (res.type == ResourceType.TEXT) {
            quoteImages = decodeQuoteImages(res.quoteImageBase64, vm)
        } else if (res.type == ResourceType.FLOW) {
            flowItems = parseFlowItems(res.sceneJson.orEmpty(), vm)
        }
        mediaUris = when (res.type) {
            ResourceType.VIDEO -> {
                val raw = res.contentUriOrPath
                if (raw.isNullOrBlank()) {
                    Log.e("ResourcePreview", "Missing media uri for type=${res.type} id=${res.id}")
                    mediaLoadFailed = true
                    return@LaunchedEffect
                }
                val paths = parseMediaPaths(raw)
                val uriList = paths.mapNotNull { path ->
                    path.takeIf { it.isNotBlank() }?.let { Uri.parse(it) }
                }
                if (uriList.isEmpty()) {
                    Log.e("ResourcePreview", "Empty media uri list for type=${res.type} id=${res.id}")
                    mediaLoadFailed = true
                }
                uriList
            }
            else -> emptyList()
        }
        sceneMessages = if (res.type == ResourceType.SCENE) parseSceneMessages(res.sceneJson.orEmpty()) else emptyList()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("资源预览") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { inner ->
        Column(
            modifier = Modifier
                .padding(inner)
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 16.dp)
                .padding(bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = resource.resource.title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            if (resource.resource.type != ResourceType.FLOW) {
                ResourceMetaRow(label = "标签") {
                    if (resource.tags.isNotEmpty()) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            resource.tags.forEach { tag ->
                                TagBadge(tag = tag)
                            }
                        }
                    } else {
                        Text("无标签", style = MaterialTheme.typography.labelSmall)
                    }
                }
                ResourceMetaRow(label = "角色") {
                    if (resource.characters.isNotEmpty()) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            resource.characters.forEach { character ->
                                CharacterBadge(name = character.name)
                            }
                        }
                    } else {
                        Text("未选择角色", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    when (resource.resource.type) {
                        ResourceType.TEXT -> {
                            val quoteText = resource.resource.quoteText.orEmpty()
                            if (quoteText.isNotBlank()) {
                                val displayText = rememberFormattedText(quoteText)
                                Text(displayText)
                            }
                            QuoteImagePager(images = quoteImages)
                        }
                        ResourceType.IMAGE -> {
                            if (quoteImages.isNotEmpty()) {
                                QuoteImagePager(images = quoteImages)
                            } else {
                                Text("图片加载中…")
                            }
                        }
                        ResourceType.VIDEO -> {
                            if (mediaUris.isNotEmpty()) {
                                MediaPreviewPager(uris = mediaUris)
                            } else if (mediaLoadFailed) {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("视频加载失败")
                                    AssistChip(
                                        onClick = { mediaReloadKey += 1 },
                                        label = { Text("重试") }
                                    )
                                }
                            } else {
                                Text("视频加载中…")
                            }
                        }
                        ResourceType.SCENE -> {
                            if (sceneMessages.isNotEmpty()) {
                                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    sceneMessages.forEach { message ->
                                        SceneBubble(message = message)
                                    }
                                }
                            } else {
                                Text(resource.resource.sceneJson.orEmpty())
                            }
                        }
                        ResourceType.FLOW -> {
                            if (flowItems.isEmpty()) {
                                Text("流程内容为空")
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    flowItems.forEach { item ->
                                        Card(modifier = Modifier.fillMaxWidth()) {
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(12.dp),
                                                verticalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                val title = item.title?.ifBlank { null } ?: typeLabel(item.type)
                                                Text(title, style = MaterialTheme.typography.titleMedium)
                                                when (item.type) {
                                                    ResourceType.TEXT -> {
                                                        val displayText = rememberFormattedText(item.text)
                                                        Text(displayText)
                                                    }
                                                    ResourceType.SCENE -> {
                                                        item.sceneMessages.forEach { message ->
                                                            SceneBubble(message = message)
                                                        }
                                                    }
                                                    ResourceType.IMAGE -> QuoteImagePager(images = item.images)
                                                    ResourceType.VIDEO -> {
                                                        if (item.mediaUris.isNotEmpty()) {
                                                            MediaPreviewPager(uris = item.mediaUris)
                                                        } else if (item.mediaFailed) {
                                                            Text("视频加载失败")
                                                        } else {
                                                            Text("视频加载中…")
                                                        }
                                                    }
                                                    else -> {}
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
        }
    }
}

@Composable
private fun MediaPreview(uri: Uri) {
    val viewHolder = remember { mutableStateOf<VideoView?>(null) }
    DisposableEffect(Unit) {
        onDispose {
            viewHolder.value?.stopPlayback()
        }
    }
    AndroidView(
        factory = { context ->
            VideoView(context).apply {
                val controller = MediaController(context)
                controller.setAnchorView(this)
                setMediaController(controller)
                setVideoURI(uri)
                tag = uri
                setOnPreparedListener { mediaPlayer ->
                    Log.d("MediaPreview", "Video prepared uri=$uri duration=${mediaPlayer.duration}")
                    mediaPlayer.start()
                }
                setOnErrorListener { _, what, extra ->
                    Log.e("MediaPreview", "Video error uri=$uri what=$what extra=$extra")
                    false
                }
                viewHolder.value = this
            }
        },
        update = { view ->
            if (view.tag != uri) {
                view.tag = uri
                view.setVideoURI(uri)
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .height(420.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant)
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MediaPreviewPager(uris: List<Uri>) {
    val pagerState = rememberPagerState(pageCount = { uris.size })
    var fullScreenUri by remember { mutableStateOf<Uri?>(null) }
    if (fullScreenUri != null) {
        FullScreenVideoDialog(uri = fullScreenUri!!, onDismiss = { fullScreenUri = null })
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        HorizontalPager(state = pagerState) { page ->
            MediaPreview(uri = uris[page])
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.weight(1f)) {
                if (uris.size > 1) {
                    Text(
                        text = "视频 ${pagerState.currentPage + 1}/${uris.size}",
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
            TextButton(onClick = { fullScreenUri = uris[pagerState.currentPage] }) {
                Text("全屏播放")
            }
        }
    }
}

private fun parseMediaPaths(raw: String): List<String> {
    val trimmed = raw.trim()
    if (trimmed.startsWith("[")) {
        return runCatching {
            val arr = JSONArray(trimmed)
            List(arr.length()) { index -> arr.getString(index) }
        }.getOrDefault(listOf(raw))
    }
    return listOf(raw)
}

private fun decodeImageSources(payload: String?, vm: ResourceViewModel): List<android.graphics.Bitmap> {
    if (payload.isNullOrBlank()) return emptyList()
    val items = parseMediaPaths(payload)
    return items.mapNotNull { decodeImageSource(it, vm) }
}

private fun decodeImageSource(item: String, vm: ResourceViewModel): android.graphics.Bitmap? {
    val uri = Uri.parse(item)
    return if (uri.scheme != null) {
        vm.decodeUriToBitmap(uri)
    } else {
        vm.decodeBase64ToBitmap(item)
    }
}

@Composable
private fun SceneBubble(message: SceneMessage) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start
    ) {
        Column(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.shapes.medium)
                .padding(12.dp)
        ) {
            Text(
                text = message.speaker,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(Modifier.height(4.dp))
            val content = rememberFormattedText(message.content)
            Text(text = content)
        }
    }
}

private fun parseSceneMessages(raw: String): List<SceneMessage> {
    return runCatching {
        val array = JSONArray(raw)
        buildList {
            for (i in 0 until array.length()) {
                val obj = array.optJSONObject(i) ?: continue
                val speaker = obj.optString("speaker")
                val content = obj.optString("text")
                if (speaker.isNotBlank() || content.isNotBlank()) {
                    add(SceneMessage(speaker.ifBlank { "角色" }, content))
                }
            }
        }
    }.getOrDefault(emptyList())
}

private suspend fun parseFlowItems(raw: String, vm: ResourceViewModel): List<FlowPreviewItem> {
    if (raw.isBlank()) return emptyList()
    return runCatching {
        val array = JSONArray(raw)
        buildList {
            for (i in 0 until array.length()) {
                val obj = array.optJSONObject(i) ?: continue
                val type = runCatching { ResourceType.valueOf(obj.optString("type")) }.getOrNull() ?: continue
                val title = obj.optString("title").ifBlank { null }
                when (type) {
                    ResourceType.TEXT -> add(
                        FlowPreviewItem(type = type, title = title, text = obj.optString("text"))
                    )
                    ResourceType.SCENE -> {
                        val messages = obj.optJSONArray("messages") ?: JSONArray()
                        val parsed = buildList {
                            for (j in 0 until messages.length()) {
                                val msg = messages.optJSONObject(j) ?: continue
                                val speaker = msg.optString("speaker")
                                val content = msg.optString("text")
                                if (speaker.isNotBlank() || content.isNotBlank()) {
                                    add(SceneMessage(speaker.ifBlank { "角色" }, content))
                                }
                            }
                        }
                        add(FlowPreviewItem(type = type, title = title, sceneMessages = parsed))
                    }
                    ResourceType.IMAGE -> {
                        val images = obj.optJSONArray("images") ?: JSONArray()
                        val bitmaps = buildList {
                            for (j in 0 until images.length()) {
                                val item = images.optString(j)
                                if (item.isNotBlank()) {
                                    decodeImageSource(item, vm)?.let { add(it) }
                                }
                            }
                        }
                        add(FlowPreviewItem(type = type, title = title, images = bitmaps))
                    }
                    ResourceType.VIDEO -> {
                        val videos = obj.optJSONArray("videos") ?: JSONArray()
                        val uriList = mutableListOf<Uri>()
                        var failed = false
                        for (j in 0 until videos.length()) {
                            val path = videos.optString(j)
                            if (path.isBlank()) {
                                failed = true
                                continue
                            }
                            uriList.add(Uri.parse(path))
                        }
                        if (uriList.isEmpty() && videos.length() > 0) {
                            failed = true
                        }
                        add(FlowPreviewItem(type = type, title = title, mediaUris = uriList, mediaFailed = failed))
                    }
                    else -> {}
                }
            }
        }
    }.getOrDefault(emptyList())
}

private fun typeLabel(type: ResourceType): String = when (type) {
    ResourceType.FLOW -> "流程"
    ResourceType.TEXT -> "文本"
    ResourceType.IMAGE -> "图片"
    ResourceType.VIDEO -> "视频"
    ResourceType.SCENE -> "情景"
}

@Composable
private fun ResourceMetaRow(
    label: String,
    content: @Composable () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Box(modifier = Modifier.weight(1f)) {
            content()
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun QuoteImagePager(images: List<android.graphics.Bitmap>) {
    if (images.isEmpty()) return
    var fullScreenImage by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    if (fullScreenImage != null) {
        FullScreenImageDialog(bitmap = fullScreenImage!!, onDismiss = { fullScreenImage = null })
    }
    if (images.size == 1) {
        Image(
            bitmap = images.first().asImageBitmap(),
            contentDescription = null,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { fullScreenImage = images.first() }
        )
        return
    }
    val pagerState = rememberPagerState(pageCount = { images.size })
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxWidth()) { page ->
            Image(
                bitmap = images[page].asImageBitmap(),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { fullScreenImage = images[page] }
            )
        }
        Text(
            text = "${pagerState.currentPage + 1}/${images.size}",
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
    }
}

@Composable
private fun FullScreenImageDialog(bitmap: android.graphics.Bitmap, onDismiss: () -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .clickable(onClick = onDismiss)
        ) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.align(Alignment.TopEnd)
            ) {
                Icon(Icons.Default.Close, contentDescription = "关闭", tint = Color.White)
            }
        }
    }
}

@Composable
private fun FullScreenVideoDialog(uri: Uri, onDismiss: () -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        var landscape by remember { mutableStateOf(false) }
        var aspectRatio by remember { mutableStateOf(0f) }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            val viewHolder = remember { mutableStateOf<VideoView?>(null) }
            DisposableEffect(Unit) {
                onDispose {
                    viewHolder.value?.stopPlayback()
                }
            }
            AndroidView(
                factory = { context ->
                    VideoView(context).apply {
                        val controller = MediaController(context)
                        controller.setAnchorView(this)
                        setMediaController(controller)
                        setVideoURI(uri)
                        tag = uri
                        setOnPreparedListener { mediaPlayer ->
                            aspectRatio =
                                if (mediaPlayer.videoHeight > 0) {
                                    mediaPlayer.videoWidth.toFloat() / mediaPlayer.videoHeight.toFloat()
                                } else {
                                    0f
                                }
                            mediaPlayer.start()
                        }
                        viewHolder.value = this
                    }
                },
                update = { view ->
                    if (view.tag != uri) {
                        view.tag = uri
                        view.setVideoURI(uri)
                    }
                },
                modifier = Modifier
                    .fillMaxSize()
                    .then(
                        if (aspectRatio > 0f) {
                            Modifier
                                .align(Alignment.Center)
                                .aspectRatio(aspectRatio)
                        } else {
                            Modifier
                        }
                    )
                    .graphicsLayer {
                        rotationZ = if (landscape) 90f else 0f
                    }
            )
            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 4.dp, end = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = { landscape = !landscape },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color.White)
                ) {
                    Text(if (landscape) "竖屏" else "横屏")
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "关闭", tint = Color.White)
                }
            }
        }
    }
}

private fun decodeQuoteImages(payload: String?, vm: ResourceViewModel): List<android.graphics.Bitmap> {
    if (payload.isNullOrBlank()) return emptyList()
    val base64List = runCatching {
        val array = JSONArray(payload)
        buildList {
            for (i in 0 until array.length()) {
                val item = array.optString(i)
                if (item.isNotBlank()) add(item)
            }
        }
    }.getOrElse { listOf(payload) }
    return base64List.mapNotNull { vm.decodeBase64ToBitmap(it) }
}
