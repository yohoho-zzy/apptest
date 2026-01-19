package com.example.quotepicker.ui.components

import android.net.Uri
import android.util.Log
import android.widget.MediaController
import android.widget.VideoView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.AssistChip
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.quotepicker.data.ResourceType
import com.example.quotepicker.data.ResourceWithTagsCharacters
import com.example.quotepicker.ui.components.TagBadge
import com.example.quotepicker.vm.ResourceViewModel
import java.io.File
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONArray

private data class SceneMessage(val speaker: String, val content: String)

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
    val scrollState = rememberScrollState()

    LaunchedEffect(resource.resource.id, mediaReloadKey) {
        val res = resource.resource
        quoteImages = emptyList()
        mediaLoadFailed = false
        if (res.type == ResourceType.IMAGE) {
            val images = mutableListOf<android.graphics.Bitmap>()
            res.contentUriOrPath?.let { path ->
                val bytes = vm.loadDecryptedBytes(path)
                android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.let { images.add(it) }
            }
            images.addAll(decodeQuoteImages(res.quoteImageBase64, vm))
            quoteImages = images
        } else if (res.type == ResourceType.QUOTE) {
            quoteImages = decodeQuoteImages(res.quoteImageBase64, vm)
        }
        mediaUris = when (res.type) {
            ResourceType.VIDEO, ResourceType.AUDIO -> {
                val raw = res.contentUriOrPath
                if (raw.isNullOrBlank()) {
                    Log.e("ResourcePreview", "Missing media path for type=${res.type} id=${res.id}")
                    mediaLoadFailed = true
                    return@LaunchedEffect
                }
                val paths = parseMediaPaths(raw)
                if (paths.isEmpty()) {
                    Log.e("ResourcePreview", "Empty media path list for type=${res.type} id=${res.id}")
                    mediaLoadFailed = true
                    return@LaunchedEffect
                }
                val uriList = mutableListOf<Uri>()
                paths.forEachIndexed { index, path ->
                    val extension = resolvePreviewExtension(path, res.type)
                    Log.d(
                        "ResourcePreview",
                        "Preparing media preview type=${res.type} id=${res.id} index=$index path=$path"
                    )
                    val file = withTimeoutOrNull(60_000) {
                        vm.writeDecryptedToCache(path, extension)
                    }
                    if (file == null || !file.exists() || file.length() == 0L) {
                        Log.e(
                            "ResourcePreview",
                            "Failed to load media preview type=${res.type} id=${res.id} index=$index path=$path"
                        )
                        mediaLoadFailed = true
                    } else {
                        uriList.add(Uri.fromFile(file))
                        mediaUris = uriList.toList()
                    }
                }
                if (uriList.isEmpty()) {
                    mediaLoadFailed = true
                }
                uriList
            }
            else -> emptyList()
        }
        sceneMessages = parseSceneMessages(res.sceneJson.orEmpty())
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = resource.resource.title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
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
                            AssistChip(onClick = {}, label = { Text(character.name) })
                        }
                    }
                } else {
                    Text("未选择角色", style = MaterialTheme.typography.labelSmall)
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
                        ResourceType.QUOTE -> {
                            if (!resource.resource.quoteText.isNullOrBlank()) {
                                Text(resource.resource.quoteText.orEmpty())
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
                        ResourceType.AUDIO -> {
                            if (mediaUris.isNotEmpty()) {
                                MediaPreview(uri = mediaUris.first())
                                Text("音频播放中")
                            } else if (mediaLoadFailed) {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("音频加载失败")
                                    AssistChip(
                                        onClick = { mediaReloadKey += 1 },
                                        label = { Text("重试") }
                                    )
                                }
                            } else {
                                Text("音频加载中…")
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
            .height(240.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant)
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MediaPreviewPager(uris: List<Uri>) {
    val pagerState = rememberPagerState(pageCount = { uris.size })
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        HorizontalPager(state = pagerState) { page ->
            MediaPreview(uri = uris[page])
        }
        if (uris.size > 1) {
            Text(
                text = "视频 ${pagerState.currentPage + 1}/${uris.size}",
                style = MaterialTheme.typography.labelSmall
            )
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

private fun resolvePreviewExtension(path: String, type: ResourceType): String {
    val fallback = if (type == ResourceType.VIDEO) "mp4" else "mp3"
    val fileName = File(path).name.removeSuffix(".enc")
    val ext = fileName.substringAfterLast('.', "")
    return if (ext.isNotBlank()) ext else fallback
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
            Text(text = message.content)
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
    if (images.size == 1) {
        Image(
            bitmap = images.first().asImageBitmap(),
            contentDescription = null,
            modifier = Modifier.fillMaxWidth()
        )
        return
    }
    val pagerState = rememberPagerState(pageCount = { images.size })
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxWidth()) { page ->
            Image(
                bitmap = images[page].asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.fillMaxWidth()
            )
        }
        Text(
            text = "${pagerState.currentPage + 1}/${images.size}",
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
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
