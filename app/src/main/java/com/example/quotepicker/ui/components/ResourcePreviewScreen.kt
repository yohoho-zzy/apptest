package com.example.quotepicker.ui.components

import android.net.Uri
import android.util.Log
import android.media.MediaPlayer
import android.widget.MediaController
import android.widget.Toast
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
import androidx.compose.material3.AlertDialog
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
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.quotepicker.data.ResourceType
import com.example.quotepicker.data.ResourceWithTagsCharacters
import com.example.quotepicker.ui.components.TagBadge
import com.example.quotepicker.ui.components.sortTagsForDisplay
import com.example.quotepicker.vm.ResourceViewModel
import org.json.JSONArray

private data class SceneMessage(val speaker: String, val content: String)

private data class ImagePreviewItem(
    val bitmap: android.graphics.Bitmap,
    val motionVideoUri: Uri? = null
)

private data class FlowPreviewItem(
    val type: ResourceType,
    val title: String? = null,
    val text: String = "",
    val sceneMessages: List<SceneMessage> = emptyList(),
    val images: List<ImagePreviewItem> = emptyList(),
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
    var quoteImages by remember { mutableStateOf<List<ImagePreviewItem>>(emptyList()) }
    var mediaUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var soundUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var mediaLoadFailed by remember { mutableStateOf(false) }
    var mediaReloadKey by remember { mutableStateOf(0) }
    var sceneMessages by remember { mutableStateOf<List<SceneMessage>>(emptyList()) }
    var flowItems by remember { mutableStateOf<List<FlowPreviewItem>>(emptyList()) }
    val scrollState = rememberScrollState()
    val uiState by vm.uiState.collectAsState()
    val allResources by vm.allResources.collectAsState()
    val context = LocalContext.current
    var filePreviewInfo by remember { mutableStateOf<ResourceFileInfo?>(null) }
    val sortedTags = remember(resource.tags, uiState.categories) {
        sortTagsForDisplay(resource.tags, uiState.categories)
    }
    val highlightedSpeaker = uiState.filters.selectedCharacterId?.let { id ->
        uiState.characters.firstOrNull { it.id == id }?.name
    }
    val eventRunner = rememberEventSequenceRunner()
    val handleFilePreview: (String) -> Unit = { info ->
        val decoded = decodeResourceFileInfo(info)
        if (decoded == null) {
            Toast.makeText(context, "文件信息无效", Toast.LENGTH_SHORT).show()
        } else {
            filePreviewInfo = decoded
        }
    }

    LaunchedEffect(resource.resource.id, mediaReloadKey, allResources) {
        val res = resource.resource
        quoteImages = emptyList()
        mediaLoadFailed = false
        flowItems = emptyList()
        if (res.type == ResourceType.IMAGE) {
            quoteImages = decodeImageSources(res.contentUriOrPath ?: res.quoteImageBase64, vm)
        } else if (res.type == ResourceType.TEXT) {
            quoteImages = decodeQuoteImages(res.quoteImageBase64, vm)
        } else if (res.type == ResourceType.FLOW) {
            flowItems = parseFlowItems(res.sceneJson.orEmpty(), vm, allResources)
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
        soundUris = when (res.type) {
            ResourceType.SOUND -> {
                val raw = res.contentUriOrPath
                if (raw.isNullOrBlank()) {
                    Log.e("ResourcePreview", "Missing media uri for type=${res.type} id=${res.id}")
                    mediaLoadFailed = true
                    return@LaunchedEffect
                }
                val paths = parseMediaPaths(raw)
                paths.mapNotNull { path -> path.takeIf { it.isNotBlank() }?.let(Uri::parse) }
            }
            else -> emptyList()
        }
        sceneMessages = if (res.type == ResourceType.SCENE) parseSceneMessages(res.sceneJson.orEmpty()) else emptyList()
    }

    val handleEventSequence: (EventSequence) -> Unit = { sequence ->
        eventRunner.start(sequence)
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
        Box(modifier = Modifier.fillMaxSize()) {
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
                        if (sortedTags.isNotEmpty()) {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                sortedTags.forEach { tag ->
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
                                    PreviewTextBlock(
                                        text = quoteText,
                                        onEventSequence = handleEventSequence,
                                        onFilePreview = handleFilePreview
                                    )
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
                            ResourceType.SOUND -> {
                                if (soundUris.isNotEmpty()) {
                                    AudioPreviewList(uris = soundUris)
                                } else if (mediaLoadFailed) {
                                    Text("音频加载失败")
                                } else {
                                    Text("音频加载中…")
                                }
                            }
                            ResourceType.SCENE -> {
                                if (sceneMessages.isNotEmpty()) {
                                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                        sceneMessages.forEach { message ->
                                            SceneBubble(
                                                message = message,
                                                highlightedSpeaker = highlightedSpeaker,
                                                onEventSequence = handleEventSequence,
                                                onFilePreview = handleFilePreview
                                            )
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
                                                            PreviewTextBlock(
                                                                text = item.text,
                                                                onEventSequence = handleEventSequence,
                                                                onFilePreview = handleFilePreview
                                                            )
                                                        }
                                                        ResourceType.SCENE -> {
                                                            item.sceneMessages.forEach { message ->
                                                                SceneBubble(
                                                                    message = message,
                                                                    highlightedSpeaker = highlightedSpeaker,
                                                                    onEventSequence = handleEventSequence,
                                                                    onFilePreview = handleFilePreview
                                                                )
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
            if (eventRunner.overlayText != null) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Text(
                        text = eventRunner.overlayText.orEmpty(),
                        modifier = Modifier.padding(top = inner.calculateTopPadding() + 24.dp),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontSize = MaterialTheme.typography.titleMedium.fontSize * 2
                        ),
                        color = Color.Red
                    )
                }
            }
        }
    }
    filePreviewInfo?.let { info ->
        FilePreviewDialog(
            info = info,
            vm = vm,
            onDismiss = { filePreviewInfo = null }
        )
    }
}

@Composable
private fun MediaPreview(uri: Uri, modifier: Modifier = Modifier) {
    val viewHolder = remember { mutableStateOf<VideoView?>(null) }
    DisposableEffect(Unit) {
        onDispose {
            viewHolder.value?.stopPlayback()
        }
    }
    val baseModifier = Modifier
        .fillMaxWidth()
        .height(420.dp)
        .background(MaterialTheme.colorScheme.surfaceVariant)
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
                setOnCompletionListener {
                    it.start()
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
        modifier = baseModifier.then(modifier)
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

private data class ImagePayloadEntry(
    val image: String,
    val motionVideo: String? = null
)

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

@Composable
private fun AudioPreviewList(uris: List<Uri>) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val playerState = remember { mutableStateOf<MediaPlayer?>(null) }
    DisposableEffect(Unit) {
        onDispose {
            playerState.value?.release()
            playerState.value = null
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        uris.forEachIndexed { index, uri ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "音频 ${index + 1}", style = MaterialTheme.typography.labelMedium)
                TextButton(onClick = {
                    playerState.value?.release()
                    playerState.value = MediaPlayer().apply {
                        setDataSource(context, uri)
                        setOnPreparedListener { it.start() }
                        prepareAsync()
                    }
                }) {
                    Text("播放")
                }
            }
        }
    }
}

private fun parseImagePayloadEntries(payload: String): List<ImagePayloadEntry> {
    val trimmed = payload.trim()
    return if (trimmed.startsWith("[")) {
        runCatching {
            val array = JSONArray(trimmed)
            buildList {
                for (i in 0 until array.length()) {
                    when (val entry = array.get(i)) {
                        is org.json.JSONObject -> {
                            val image = entry.optString("image")
                            val motion = entry.optString("motionVideo")
                            if (image.isNotBlank()) {
                                add(ImagePayloadEntry(image, motion.ifBlank { null }))
                            }
                        }
                        else -> {
                            val item = entry.toString()
                            if (item.isNotBlank()) add(ImagePayloadEntry(item))
                        }
                    }
                }
            }
        }.getOrDefault(emptyList())
    } else if (trimmed.startsWith("{")) {
        runCatching {
            val obj = org.json.JSONObject(trimmed)
            val image = obj.optString("image")
            val motion = obj.optString("motionVideo")
            if (image.isNotBlank()) listOf(ImagePayloadEntry(image, motion.ifBlank { null })) else emptyList()
        }.getOrDefault(emptyList())
    } else {
        listOf(ImagePayloadEntry(trimmed))
    }
}

private fun decodeImageSources(payload: String?, vm: ResourceViewModel): List<ImagePreviewItem> {
    if (payload.isNullOrBlank()) return emptyList()
    val items = parseImagePayloadEntries(payload)
    return items.mapNotNull { entry ->
        decodeImageSource(entry.image, vm)?.let { bitmap ->
            ImagePreviewItem(bitmap = bitmap, motionVideoUri = entry.motionVideo?.let(Uri::parse))
        }
    }
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
private fun SceneBubble(
    message: SceneMessage,
    highlightedSpeaker: String?,
    onEventSequence: (EventSequence) -> Unit,
    onFilePreview: (String) -> Unit
) {
    val isHighlighted = highlightedSpeaker != null && highlightedSpeaker == message.speaker
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isHighlighted) Arrangement.End else Arrangement.Start
    ) {
        Column(
            modifier = Modifier
                .background(
                    if (isHighlighted) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.primaryContainer,
                    MaterialTheme.shapes.medium
                )
                .padding(12.dp)
        ) {
            Text(
                text = message.speaker,
                style = MaterialTheme.typography.labelMedium,
                color = if (isHighlighted) {
                    MaterialTheme.colorScheme.onSecondaryContainer
                } else {
                    MaterialTheme.colorScheme.onPrimaryContainer
                }
            )
            Spacer(Modifier.height(4.dp))
            PreviewTextBlock(
                text = message.content,
                onEventSequence = onEventSequence,
                onFilePreview = onFilePreview,
                textStyle = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun FilePreviewDialog(
    info: ResourceFileInfo,
    vm: ResourceViewModel,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("文件预览") },
        text = {
            when (info.type) {
                ResourceType.IMAGE -> {
                    val preview by androidx.compose.runtime.produceState<android.graphics.Bitmap?>(initialValue = null, info.uri) {
                        value = vm.decodeUriToBitmap(info.uri)
                    }
                    if (preview != null) {
                        Image(
                            bitmap = preview!!.asImageBitmap(),
                            contentDescription = "图片预览",
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1.3f),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Text("图片加载中…")
                    }
                }
                ResourceType.VIDEO -> {
                    MediaPreview(
                        uri = info.uri,
                        modifier = Modifier.height(220.dp)
                    )
                }
                ResourceType.SOUND -> {
                    AudioPreviewSingle(uri = info.uri)
                }
                else -> Text("暂不支持的文件类型")
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("关闭") }
        }
    )
}

@Composable
private fun AudioPreviewSingle(uri: Uri) {
    val context = LocalContext.current
    val playerState = remember { mutableStateOf<MediaPlayer?>(null) }
    DisposableEffect(Unit) {
        onDispose {
            playerState.value?.release()
            playerState.value = null
        }
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = "音频文件", style = MaterialTheme.typography.labelMedium)
        TextButton(onClick = {
            playerState.value?.release()
            playerState.value = MediaPlayer().apply {
                setDataSource(context, uri)
                setOnPreparedListener { it.start() }
                prepareAsync()
            }
        }) {
            Text("播放")
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

private suspend fun parseFlowItems(
    raw: String,
    vm: ResourceViewModel,
    resources: List<ResourceWithTagsCharacters>
): List<FlowPreviewItem> {
    if (raw.isBlank()) return emptyList()
    val resourceMap = resources.associateBy { it.resource.id }
    return runCatching {
        val array = JSONArray(raw)
        buildList {
            for (i in 0 until array.length()) {
                val obj = array.optJSONObject(i) ?: continue
                val type = runCatching { ResourceType.valueOf(obj.optString("type")) }.getOrNull() ?: continue
                val title = obj.optString("title").ifBlank { null }
                val resourceId = obj.optLong("resourceId", -1L).takeIf { it > 0 }
                val resource = resourceId?.let { id -> resourceMap[id] }
                if (resource != null) {
                    add(flowPreviewItemFromResource(resource, vm))
                    continue
                }
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
                                when (val entry = images.get(j)) {
                                    is org.json.JSONObject -> {
                                        val image = entry.optString("image")
                                        val motion = entry.optString("motionVideo")
                                        if (image.isNotBlank()) {
                                            decodeImageSource(image, vm)?.let {
                                                add(ImagePreviewItem(it, motion.ifBlank { null }?.let(Uri::parse)))
                                            }
                                        }
                                    }
                                    else -> {
                                        val item = entry.toString()
                                        if (item.isNotBlank()) {
                                            decodeImageSource(item, vm)?.let { add(ImagePreviewItem(it)) }
                                        }
                                    }
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

private suspend fun flowPreviewItemFromResource(
    resource: ResourceWithTagsCharacters,
    vm: ResourceViewModel
): FlowPreviewItem {
    val data = resource.resource
    return when (data.type) {
        ResourceType.TEXT -> FlowPreviewItem(
            type = data.type,
            title = data.title,
            text = data.quoteText.orEmpty()
        )
        ResourceType.SCENE -> FlowPreviewItem(
            type = data.type,
            title = data.title,
            sceneMessages = parseSceneMessages(data.sceneJson.orEmpty())
        )
        ResourceType.IMAGE -> FlowPreviewItem(
            type = data.type,
            title = data.title,
            images = decodeImageSources(data.contentUriOrPath ?: data.quoteImageBase64, vm)
        )
        ResourceType.VIDEO -> {
            val raw = data.contentUriOrPath
            val paths = raw?.takeIf { it.isNotBlank() }?.let { parseMediaPaths(it) }.orEmpty()
            val uriList = paths.mapNotNull { path -> path.takeIf { it.isNotBlank() }?.let(Uri::parse) }
            FlowPreviewItem(
                type = data.type,
                title = data.title,
                mediaUris = uriList,
                mediaFailed = raw.isNullOrBlank() || (uriList.isEmpty() && raw.isNotBlank())
            )
        }
        else -> FlowPreviewItem(type = data.type, title = data.title)
    }
}

private fun typeLabel(type: ResourceType): String = when (type) {
    ResourceType.FLOW -> "流程"
    ResourceType.TEXT -> "文本"
    ResourceType.IMAGE -> "图片"
    ResourceType.VIDEO -> "视频"
    ResourceType.SOUND -> "声音"
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
private fun QuoteImagePager(images: List<ImagePreviewItem>) {
    if (images.isEmpty()) return
    var fullScreenImage by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    var fullScreenVideo by remember { mutableStateOf<Uri?>(null) }
    if (fullScreenImage != null) {
        FullScreenImageDialog(bitmap = fullScreenImage!!, onDismiss = { fullScreenImage = null })
    }
    if (fullScreenVideo != null) {
        FullScreenVideoDialog(uri = fullScreenVideo!!, onDismiss = { fullScreenVideo = null })
    }
    if (images.size == 1) {
        val item = images.first()
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Image(
                bitmap = item.bitmap.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { fullScreenImage = item.bitmap }
            )
            if (item.motionVideoUri != null) {
                TextButton(onClick = { fullScreenVideo = item.motionVideoUri }) {
                    Text("播放 Live")
                }
            }
        }
        return
    }
    val pagerState = rememberPagerState(pageCount = { images.size })
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxWidth()) { page ->
            val item = images[page]
            Image(
                bitmap = item.bitmap.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { fullScreenImage = item.bitmap }
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${pagerState.currentPage + 1}/${images.size}",
                style = MaterialTheme.typography.labelSmall
            )
            val motionUri = images[pagerState.currentPage].motionVideoUri
            if (motionUri != null) {
                TextButton(onClick = { fullScreenVideo = motionUri }) {
                    Text("播放 Live")
                }
            }
        }
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
                        setOnCompletionListener {
                            it.start()
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

private fun decodeQuoteImages(payload: String?, vm: ResourceViewModel): List<ImagePreviewItem> {
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
        .map { ImagePreviewItem(it) }
}
