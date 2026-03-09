package com.example.quotepicker.ui.components

import android.net.Uri
import android.util.Log
import android.media.MediaPlayer
import android.widget.MediaController
import android.widget.Toast
import android.widget.VideoView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.material3.Switch
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.quotepicker.data.ResourceMarkState
import com.example.quotepicker.data.ResourceType
import com.example.quotepicker.data.ResourceWithTagsCharacters
import com.example.quotepicker.ui.components.TagBadge
import com.example.quotepicker.ui.components.sortTagsForDisplay
import com.example.quotepicker.vm.ResourceViewModel
import org.json.JSONArray
import java.io.File
import java.util.Locale

private data class SceneMessage(val speaker: String, val content: String)

private data class ImagePreviewItem(
    val bitmap: android.graphics.Bitmap,
    val motionVideoUri: Uri? = null,
    val resourceCode: String? = null,
    val sizeMbText: String = "0.0M"
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
    val liveResource = remember(uiState.resources, resource.resource.id) {
        uiState.resources.firstOrNull { it.resource.id == resource.resource.id } ?: resource
    }
    val sortedTags = remember(liveResource.tags, uiState.categories) {
        sortTagsForDisplay(liveResource.tags, uiState.categories)
    }
    val highlightedSpeaker = uiState.filters.selectedCharacterId?.let { id ->
        uiState.characters.firstOrNull { it.id == id }?.name
    }
    val eventRunner = rememberEventSequenceRunner()

    LaunchedEffect(resource.resource.id, mediaReloadKey, allResources) {
        val res = liveResource.resource
        quoteImages = emptyList()
        mediaLoadFailed = false
        flowItems = emptyList()
        if (res.type == ResourceType.IMAGE) {
            quoteImages = decodeImageSources(res.contentUriOrPath ?: res.quoteImageBase64, vm, res.resourceCode)
        } else if (res.type == ResourceType.TEXT) {
            quoteImages = decodeQuoteImages(res.quoteImageBase64, vm, res.resourceCode)
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
                    text = liveResource.resource.title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MarkCircleButton(
                        color = Color(0xFF2E7D32),
                        selected = liveResource.resource.markState == ResourceMarkState.CHECKED,
                        enabled = liveResource.tags.isNotEmpty(),
                        onClick = {
                            if (liveResource.tags.isNotEmpty()) {
                                val next = if (liveResource.resource.markState == ResourceMarkState.CHECKED) ResourceMarkState.NONE else ResourceMarkState.CHECKED
                                vm.updateResource(liveResource.resource.copy(markState = next))
                            }
                        }
                    )
                    MarkCircleButton(
                        color = Color(0xFFC62828),
                        selected = liveResource.resource.markState == ResourceMarkState.FAVORITE,
                        enabled = liveResource.tags.isNotEmpty() && liveResource.resource.markState != ResourceMarkState.NONE,
                        onClick = {
                            if (liveResource.tags.isNotEmpty() && liveResource.resource.markState == ResourceMarkState.CHECKED) {
                                vm.updateResource(liveResource.resource.copy(markState = ResourceMarkState.FAVORITE))
                            } else if (liveResource.resource.markState == ResourceMarkState.FAVORITE) {
                                vm.updateResource(liveResource.resource.copy(markState = ResourceMarkState.CHECKED))
                            }
                        }
                    )
                }
                if (liveResource.resource.type != ResourceType.FLOW) {
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
                        if (liveResource.characters.isNotEmpty()) {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                liveResource.characters.forEach { character ->
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
                        when (liveResource.resource.type) {
                            ResourceType.TEXT -> {
                                val quoteText = liveResource.resource.quoteText.orEmpty()
                                val isMagicDrama = liveResource.resource.title.contains("魔剧")
                                var showMagicDrama by remember(liveResource.resource.id) { mutableStateOf(false) }
                                var defaultDelayInput by remember(liveResource.resource.id) { mutableStateOf("3000") }
                                var imageIntervalInput by remember(liveResource.resource.id) { mutableStateOf("3000") }
                                var enableSpeech by remember(liveResource.resource.id) { mutableStateOf(true) }
                                var speechRateInput by remember(liveResource.resource.id) { mutableStateOf("1.0") }
                                var speechPitchInput by remember(liveResource.resource.id) { mutableStateOf("1.0") }
                                if (showMagicDrama) {
                                    MagicDramaScreen(
                                        title = liveResource.resource.title,
                                        script = quoteText,
                                        boundCharacters = liveResource.characters,
                                        settings = MagicDramaSettings(
                                            defaultDelayMs = defaultDelayInput.toLongOrNull()?.coerceIn(300L, 10_000L) ?: 1000L,
                                            imageIntervalMs = imageIntervalInput.toLongOrNull()?.coerceIn(300L, 10_000L) ?: 3000L,
                                            enableSpeech = enableSpeech,
                                            speechRate = speechRateInput.toFloatOrNull()?.coerceIn(0.5f, 2.0f) ?: 1.0f,
                                            speechPitch = speechPitchInput.toFloatOrNull()?.coerceIn(0.5f, 2.0f) ?: 1.0f
                                        ),
                                        vm = vm,
                                        onClose = { showMagicDrama = false }
                                    )
                                } else if (isMagicDrama) {
                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalArrangement = Arrangement.spacedBy(10.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        TextButton(onClick = { showMagicDrama = true }) {
                                            Text("开始魔剧")
                                        }
                                        Text(
                                            text = "播放设置（开始前设置）",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = Color(0xFF5F6280)
                                        )
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("文本显示时朗读")
                                            Switch(
                                                checked = enableSpeech,
                                                onCheckedChange = { enableSpeech = it }
                                            )
                                        }
                                        OutlinedTextField(
                                            value = defaultDelayInput,
                                            onValueChange = { defaultDelayInput = it },
                                            singleLine = true,
                                            label = { Text("文本默认停留毫秒(300-10000)") },
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                        OutlinedTextField(
                                            value = imageIntervalInput,
                                            onValueChange = { imageIntervalInput = it },
                                            singleLine = true,
                                            label = { Text("图片轮播毫秒(300-10000)") },
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                        if (enableSpeech) {
                                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                OutlinedTextField(
                                                    value = speechRateInput,
                                                    onValueChange = { speechRateInput = it },
                                                    singleLine = true,
                                                    label = { Text("语速0.5-2") },
                                                    modifier = Modifier.weight(1f)
                                                )
                                                OutlinedTextField(
                                                    value = speechPitchInput,
                                                    onValueChange = { speechPitchInput = it },
                                                    singleLine = true,
                                                    label = { Text("音调0.5-2") },
                                                    modifier = Modifier.weight(1f)
                                                )
                                            }
                                        }
                                    }
                                } else {
                                    if (quoteText.isNotBlank()) {
                                        PreviewTextWithInlineFile(
                                            text = quoteText,
                                            vm = vm,
                                            onEventSequence = handleEventSequence
                                        )
                                    }
                                    QuoteImagePager(images = quoteImages)
                                }
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
                                    MediaPreviewPager(uris = mediaUris, resourceCode = liveResource.resource.resourceCode)
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
                                                vm = vm
                                            )
                                        }
                                    }
                                } else {
                                    Text(liveResource.resource.sceneJson.orEmpty())
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
                                                            PreviewTextWithInlineFile(
                                                                text = item.text,
                                                                vm = vm,
                                                                onEventSequence = handleEventSequence
                                                            )
                                                        }
                                                        ResourceType.SCENE -> {
                                                            item.sceneMessages.forEach { message ->
                                                                SceneBubble(
                                                                    message = message,
                                                                    highlightedSpeaker = highlightedSpeaker,
                                                                    onEventSequence = handleEventSequence,
                                                                    vm = vm
                                                                )
                                                            }
                                                        }
                                                        ResourceType.IMAGE -> QuoteImagePager(images = item.images)
                                                        ResourceType.VIDEO -> {
                                                            if (item.mediaUris.isNotEmpty()) {
                                                                MediaPreviewPager(uris = item.mediaUris, resourceCode = null)
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
private fun MediaPreviewPager(uris: List<Uri>, resourceCode: String?) {
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
                val currentIndex = pagerState.currentPage + 1
                val currentCode = indexedResourceFileId(resourceCode, pagerState.currentPage)
                Text(
                    text = if (currentCode.isNullOrBlank()) {
                        "视频 $currentIndex/${uris.size}"
                    } else {
                        "视频 $currentIndex/${uris.size} $currentCode"
                    },
                    style = MaterialTheme.typography.labelSmall
                )
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

private fun formatSizeMb(path: String): String {
    val size = runCatching {
        val uri = Uri.parse(path)
        val file = uri.path?.let { File(it) }
        file?.takeIf { it.exists() }?.length() ?: 0L
    }.getOrDefault(0L)

    return String.format(Locale.US, "%.1fM", size.toDouble() / (1024 * 1024))
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

private fun decodeImageSources(payload: String?, vm: ResourceViewModel, resourceCode: String?): List<ImagePreviewItem> {
    if (payload.isNullOrBlank()) return emptyList()
    val items = parseImagePayloadEntries(payload)
    return items.mapIndexedNotNull { index, entry ->
        decodeImageSource(entry.image, vm)?.let { bitmap ->
            ImagePreviewItem(
                bitmap = bitmap,
                motionVideoUri = entry.motionVideo?.let(Uri::parse),
                resourceCode = indexedResourceFileId(resourceCode, index),
                sizeMbText = formatSizeMb(entry.image)
            )
        }
    }
}

private fun indexedResourceFileId(resourceCode: String?, index: Int): String? {
    if (resourceCode.isNullOrBlank() || index < 0) return null
    return "$resourceCode.${index + 1}"
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
    vm: ResourceViewModel
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
            PreviewTextWithInlineFile(
                text = message.content,
                vm = vm,
                onEventSequence = onEventSequence,
                textStyle = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun PreviewTextWithInlineFile(
    text: String,
    vm: ResourceViewModel,
    onEventSequence: (EventSequence) -> Unit,
    modifier: Modifier = Modifier,
    textStyle: TextStyle = MaterialTheme.typography.bodyMedium
) {
    val context = LocalContext.current
    var previewInfo by remember(text) { mutableStateOf<ResourceFileInfo?>(null) }
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        previewInfo?.let { info ->
            InlineFilePreview(info = info, vm = vm)
        }
        PreviewTextBlock(
            text = text,
            onEventSequence = onEventSequence,
            onFilePreview = { info ->
                val decoded = decodeResourceFileInfo(info)
                if (decoded == null) {
                    Toast.makeText(context, "文件信息无效", Toast.LENGTH_SHORT).show()
                } else {
                    previewInfo = if (previewInfo == decoded) null else decoded
                }
            },
            textStyle = textStyle
        )
    }
}

@Composable
private fun InlineFilePreview(
    info: ResourceFileInfo,
    vm: ResourceViewModel,
    modifier: Modifier = Modifier
) {
    val resolvedUri = remember(info, vm.allResources.value) {
        vm.resolveMediaUri(info.type, info.name, info.resourceId)
    }
    Card(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 220.dp, max = 360.dp)
                .padding(12.dp)
        ) {
            if (resolvedUri == null) {
                Text("未找到文件：${info.name}")
                return@Box
            }
            when (info.type) {
                ResourceType.IMAGE -> {
                    val preview by androidx.compose.runtime.produceState<android.graphics.Bitmap?>(initialValue = null, resolvedUri) {
                        value = vm.decodeUriToBitmap(resolvedUri)
                    }
                    if (preview != null) {
                        Image(
                            bitmap = preview!!.asImageBitmap(),
                            contentDescription = "图片预览",
                            modifier = Modifier.fillMaxWidth(),
                            contentScale = ContentScale.Fit
                        )
                    } else {
                        Text("图片加载中…")
                    }
                }
                ResourceType.VIDEO -> {
                    MediaPreview(
                        uri = resolvedUri,
                        modifier = Modifier.height(280.dp)
                    )
                }
                ResourceType.SOUND -> {
                    AudioPreviewSingle(uri = resolvedUri)
                }
                else -> Text("暂不支持的文件类型")
            }
        }
    }
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
                                                add(ImagePreviewItem(it, motion.ifBlank { null }?.let(Uri::parse), sizeMbText = formatSizeMb(image)))
                                            }
                                        }
                                    }
                                    else -> {
                                        val item = entry.toString()
                                        if (item.isNotBlank()) {
                                            decodeImageSource(item, vm)?.let { add(ImagePreviewItem(it, sizeMbText = formatSizeMb(item))) }
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
            images = decodeImageSources(data.contentUriOrPath ?: data.quoteImageBase64, vm, data.resourceCode)
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


@Composable
private fun MarkCircleButton(
    color: Color,
    selected: Boolean,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(28.dp)
            .clip(CircleShape)
            .background(if (selected) color else color.copy(alpha = if (enabled) 0.18f else 0.08f))
            .border(1.dp, if (enabled) color else color.copy(alpha = 0.45f), CircleShape)
            .clickable(enabled = enabled, onClick = onClick)
    )
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
            Text(
                text = "1/1 ${item.resourceCode.orEmpty()} (${item.sizeMbText})",
                style = MaterialTheme.typography.labelSmall
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
            val current = images[pagerState.currentPage]
            Text(
                text = "${pagerState.currentPage + 1}/${images.size} ${current.resourceCode.orEmpty()} (${current.sizeMbText})",
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
    val maxScale = 8f
    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    val transformState = rememberTransformableState { zoomChange, panChange, _ ->
        scale = (scale * zoomChange).coerceIn(1f, maxScale)
        offset += panChange
    }
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .transformable(state = transformState)
        ) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        translationX = offset.x
                        translationY = offset.y
                    }
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
    val maxScale = 8f
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        var landscape by remember { mutableStateOf(false) }
        var aspectRatio by remember { mutableStateOf(0f) }
        var scale by remember { mutableStateOf(1f) }
        var offset by remember { mutableStateOf(Offset.Zero) }
        val transformState = rememberTransformableState { zoomChange, panChange, _ ->
            scale = (scale * zoomChange).coerceIn(1f, maxScale)
            offset += panChange
        }
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
                    .transformable(state = transformState)
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
                        scaleX = scale
                        scaleY = scale
                        translationX = offset.x
                        translationY = offset.y
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

private fun decodeQuoteImages(payload: String?, vm: ResourceViewModel, resourceCode: String?): List<ImagePreviewItem> {
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
    return base64List.mapIndexedNotNull { index, item ->
        vm.decodeBase64ToBitmap(item)?.let { ImagePreviewItem(it, resourceCode = indexedResourceFileId(resourceCode, index)) }
    }
}
