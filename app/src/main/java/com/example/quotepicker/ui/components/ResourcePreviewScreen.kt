package com.example.quotepicker.ui.components

import android.net.Uri
import android.widget.MediaController
import android.widget.VideoView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
    var mediaUri by remember { mutableStateOf<Uri?>(null) }
    var sceneMessages by remember { mutableStateOf<List<SceneMessage>>(emptyList()) }
    val scrollState = rememberScrollState()

    LaunchedEffect(resource.resource.id) {
        val res = resource.resource
        quoteImages = emptyList()
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
        mediaUri = when (res.type) {
            ResourceType.VIDEO, ResourceType.AUDIO -> {
                val path = res.contentUriOrPath ?: return@LaunchedEffect
                val file = withContext(Dispatchers.IO) {
                    vm.writeDecryptedToCache(path)
                } ?: return@LaunchedEffect
                Uri.fromFile(file)
            }
            else -> null
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
            if (resource.tags.isNotEmpty()) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    resource.tags.forEach { tag ->
                        TagBadge(tag = tag)
                    }
                }
            } else {
                Text("无标签", style = MaterialTheme.typography.labelMedium)
            }
            if (resource.characters.isNotEmpty()) {
                Text("角色", style = MaterialTheme.typography.labelMedium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    resource.characters.forEach { character ->
                        AssistChip(onClick = {}, label = { Text(character.name) })
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
                            if (mediaUri != null) {
                                MediaPreview(uri = mediaUri!!)
                            } else {
                                Text("视频加载中…")
                            }
                        }
                        ResourceType.AUDIO -> {
                            if (mediaUri != null) {
                                MediaPreview(uri = mediaUri!!)
                                Text("音频播放中")
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
    AndroidView(
        factory = { context ->
            VideoView(context).apply {
                val controller = MediaController(context)
                controller.setAnchorView(this)
                setMediaController(controller)
                setVideoURI(uri)
                setOnPreparedListener { it.start() }
            }
        },
        update = { view ->
            view.setVideoURI(uri)
        },
        modifier = Modifier
            .fillMaxWidth()
            .height(240.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant)
    )
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
