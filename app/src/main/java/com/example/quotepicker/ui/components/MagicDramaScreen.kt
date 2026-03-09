package com.example.quotepicker.ui.components

import android.net.Uri
import android.speech.tts.TextToSpeech
import android.widget.VideoView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.quotepicker.data.CharacterEntity
import com.example.quotepicker.vm.ResourceViewModel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONArray
import java.util.Locale

data class MagicDramaSettings(
    val defaultDelayMs: Long = 1000L,
    val enableSpeech: Boolean = true,
    val speechRate: Float = 1.0f,
    val speechPitch: Float = 1.0f
)

private sealed interface DramaCommand {
    data class Narration(val text: String, val delayMs: Long, val important: Boolean) : DramaCommand
    data class RoleLine(val roleKey: String, val text: String, val delayMs: Long) : DramaCommand
    data class ShowVideo(val source: String) : DramaCommand
    data class ShowImage(val source: String) : DramaCommand
    data class ShowButtons(val options: List<DramaButtonOption>) : DramaCommand
    data class Countdown(val seconds: Int) : DramaCommand
}

private data class DramaButtonOption(val text: String, val branchId: String)

private sealed interface DramaMessage {
    data class Narration(val text: String, val important: Boolean) : DramaMessage
    data class Role(val role: String, val text: String) : DramaMessage
}

private sealed interface DramaMedia {
    data class Image(val source: String) : DramaMedia
    data class Video(val source: String) : DramaMedia
}

@Composable
fun MagicDramaScreen(
    title: String,
    script: String,
    boundCharacters: List<CharacterEntity>,
    settings: MagicDramaSettings,
    vm: ResourceViewModel,
    onClose: () -> Unit
) {
    val listState = rememberLazyListState()
    val messages = remember { mutableStateListOf<DramaMessage>() }
    var currentMedia by remember { mutableStateOf<DramaMedia?>(null) }
    var countdownSeconds by remember { mutableStateOf<Int?>(null) }
    var buttonOptions by remember { mutableStateOf<List<DramaButtonOption>>(emptyList()) }
    var pendingBranch by remember { mutableStateOf<CompletableDeferred<String>?>(null) }
    var countdownJob by remember { mutableStateOf<Job?>(null) }

    val parsed = remember(script) { parseDramaScript(script) }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    var ttsReady by remember { mutableStateOf(false) }
    val tts = remember {
        TextToSpeech(context) { status ->
            ttsReady = status == TextToSpeech.SUCCESS
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            countdownJob?.cancel()
            pendingBranch?.cancel()
            tts.stop()
            tts.shutdown()
        }
    }

    LaunchedEffect(script, settings.defaultDelayMs, settings.enableSpeech, settings.speechRate, settings.speechPitch) {
        messages.clear()
        currentMedia = null
        countdownSeconds = null
        buttonOptions = emptyList()
        pendingBranch = null
        countdownJob?.cancel()
        countdownJob = null
        if (settings.enableSpeech) {
            tts.language = Locale.CHINA
            tts.setSpeechRate(settings.speechRate)
            tts.setPitch(settings.speechPitch)
        } else {
            tts.stop()
        }

        val queue = ArrayDeque(parsed.main)
        while (queue.isNotEmpty()) {
            when (val cmd = queue.removeFirst()) {
                is DramaCommand.Narration -> {
                    val text = renderRandomToken(cmd.text)
                    messages += DramaMessage.Narration(text = text, important = cmd.important)
                    if (settings.enableSpeech && ttsReady) {
                        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "narration_${messages.size}")
                    }
                    delay(if (cmd.delayMs > 0) cmd.delayMs else settings.defaultDelayMs)
                }
                is DramaCommand.RoleLine -> {
                    val role = resolveRoleName(cmd.roleKey, boundCharacters)
                    val text = cmd.text.replace("nn", "\n")
                    messages += DramaMessage.Role(role = role, text = text)
                    if (settings.enableSpeech && ttsReady) {
                        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "role_${messages.size}")
                    }
                    delay(if (cmd.delayMs > 0) cmd.delayMs else settings.defaultDelayMs)
                }
                is DramaCommand.ShowImage -> currentMedia = DramaMedia.Image(cmd.source)
                is DramaCommand.ShowVideo -> currentMedia = DramaMedia.Video(cmd.source)
                is DramaCommand.Countdown -> {
                    countdownJob?.cancel()
                    countdownJob = coroutineScope.launch {
                        launchCountdown(cmd.seconds) { left -> countdownSeconds = left }
                    }
                }
                is DramaCommand.ShowButtons -> {
                    buttonOptions = cmd.options
                    val waiter = CompletableDeferred<String>()
                    pendingBranch = waiter
                    val selected = waiter.await()
                    buttonOptions = emptyList()
                    pendingBranch = null
                    parsed.branches[selected]?.asReversed()?.forEach { queue.addFirst(it) }
                }
            }
        }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.lastIndex)
        }
    }

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF5F7FB))
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .weight(3f)
                    .fillMaxWidth()
                    .background(Color(0xFFE8EEFF))
            ) {
                DramaMediaArea(media = currentMedia, vm = vm)
                countdownSeconds?.let { left ->
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(12.dp)
                            .background(Color(0xCC2E3A59), RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(text = "倒数 ${left}s", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Column(
                modifier = Modifier
                    .weight(2f)
                    .fillMaxWidth()
                    .background(Color(0xFFFDF7EA))
                    .padding(10.dp)
            ) {
                Text(text = title, color = Color(0xFF2D3561), style = MaterialTheme.typography.labelMedium)
                Spacer(modifier = Modifier.height(8.dp))
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    state = listState
                ) {
                    items(messages) { message ->
                        when (message) {
                            is DramaMessage.Narration -> NarrationBubble(message)
                            is DramaMessage.Role -> RoleBubble(message)
                        }
                    }
                }
                if (buttonOptions.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        buttonOptions.forEach { option ->
                            Button(
                                onClick = { pendingBranch?.complete(option.branchId) },
                                modifier = Modifier.weight(1f)
                            ) { Text(option.text) }
                        }
                    }
                }
            }
        }
            IconButton(
                onClick = onClose,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(10.dp)
                    .background(Color(0xAA2E3A59), CircleShape)
            ) {
                Icon(Icons.Default.Close, contentDescription = "关闭", tint = Color.White)
            }
        }
    }
}

private suspend fun launchCountdown(total: Int, onTick: (Int?) -> Unit) {
    onTick(total)
    for (left in total - 1 downTo 0) {
        delay(1000)
        onTick(left)
    }
    onTick(null)
}

@Composable
private fun DramaMediaArea(media: DramaMedia?, vm: ResourceViewModel) {
    when (media) {
        is DramaMedia.Image -> {
            val bitmap = remember(media.source) { decodeImageSource(media.source, vm) }
            if (bitmap != null) {
                androidx.compose.foundation.Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "资源图片",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            }
        }
        is DramaMedia.Video -> LoopVideo(uri = Uri.parse(media.source))
        null -> Box(modifier = Modifier.fillMaxSize())
    }
}

@Composable
private fun LoopVideo(uri: Uri) {
    val holder = remember { mutableStateOf<VideoView?>(null) }
    DisposableEffect(Unit) {
        onDispose { holder.value?.stopPlayback() }
    }
    AndroidView(
        factory = { context ->
            VideoView(context).apply {
                setVideoURI(uri)
                setOnPreparedListener { it.start() }
                setOnCompletionListener { it.start() }
                holder.value = this
            }
        },
        update = { view ->
            if (view.tag != uri) {
                view.tag = uri
                view.setVideoURI(uri)
                view.start()
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
private fun NarrationBubble(message: DramaMessage.Narration) {
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Text(
            text = message.text,
            color = if (message.important) Color(0xFFB33985) else Color(0xFF2B2F3A),
            modifier = Modifier
                .background(Color(0xFFEAF1FF), RoundedCornerShape(10.dp))
                .padding(horizontal = 12.dp, vertical = 8.dp)
        )
    }
}

@Composable
private fun RoleBubble(message: DramaMessage.Role) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .border(1.dp, Color(0xFFAAAAAA), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(text = message.role.take(4), color = Color(0xFF2B2F3A), style = MaterialTheme.typography.labelSmall)
        }
        Spacer(modifier = Modifier.width(8.dp))
        Column(
            modifier = Modifier
                .background(Color(0xFFFFFFFF), RoundedCornerShape(10.dp))
                .padding(8.dp)
        ) {
            Text(text = message.role, color = Color(0xFF5C647A), style = MaterialTheme.typography.labelSmall)
            Text(text = message.text, color = Color(0xFF1F2433))
        }
    }
}

private data class ParsedDramaScript(
    val main: List<DramaCommand>,
    val branches: Map<String, List<DramaCommand>>
)

private fun parseDramaScript(raw: String): ParsedDramaScript {
    val main = mutableListOf<DramaCommand>()
    val branches = mutableMapOf<String, MutableList<DramaCommand>>()
    raw.lines().forEach { line ->
        val trimmed = line.trim()
        if (!trimmed.startsWith("+")) return@forEach
        val body = trimmed.removePrefix("+")
        val branchMatch = Regex("^%(\\w+)(.+)$").find(body)
        if (branchMatch != null) {
            val branchId = branchMatch.groupValues[1]
            val cmd = parseDramaCommand(branchMatch.groupValues[2].trimStart(':'))
            if (cmd != null) branches.getOrPut(branchId) { mutableListOf() }.add(cmd)
        } else {
            parseDramaCommand(body)?.let(main::add)
        }
    }
    return ParsedDramaScript(main = main, branches = branches)
}

private fun parseDramaCommand(raw: String): DramaCommand? {
    val idx = raw.indexOf(':')
    if (idx <= 0) return null
    val key = raw.substring(0, idx).trim()
    val value = raw.substring(idx + 1).trim()
    return when {
        key == "旁白" -> {
            val (text, delay) = parseDelayText(value)
            DramaCommand.Narration(text, delay, important = false)
        }
        key == "重要" -> {
            val (text, delay) = parseDelayText(value)
            DramaCommand.Narration(text, delay, important = true)
        }
        key == "视频" -> DramaCommand.ShowVideo(value)
        key == "图片" -> DramaCommand.ShowImage(value)
        key == "按钮" -> {
            val options = value.split("-")
                .mapNotNull { item ->
                    val m = Regex("(.+)%([\\w]+)$").find(item.trim()) ?: return@mapNotNull null
                    DramaButtonOption(m.groupValues[1].trim(), m.groupValues[2].trim())
                }
            if (options.isNotEmpty()) DramaCommand.ShowButtons(options) else null
        }
        key == "倒数" -> value.toIntOrNull()?.let { DramaCommand.Countdown(it) }
        else -> {
            val (text, delay) = parseDelayText(value)
            DramaCommand.RoleLine(roleKey = key, text = text, delayMs = delay)
        }
    }
}

private fun parseDelayText(raw: String): Pair<String, Long> {
    val idx = raw.lastIndexOf('-')
    if (idx > 0) {
        val text = raw.substring(0, idx).trim()
        val ms = raw.substring(idx + 1).trim().toLongOrNull()
        if (ms != null) return text to ms
    }
    return raw to 0L
}

private fun resolveRoleName(input: String, boundCharacters: List<CharacterEntity>): String {
    val exact = boundCharacters.firstOrNull { it.name.equals(input, ignoreCase = true) }
    if (exact != null) return exact.name
    val contains = boundCharacters.firstOrNull { it.name.contains(input, ignoreCase = true) || input.contains(it.name, ignoreCase = true) }
    return contains?.name ?: input
}

private fun renderRandomToken(text: String): String {
    return Regex("\\[\\[(.+?)]]").replace(text) { match ->
        val options = match.groupValues[1].split(',').map { it.trim() }.filter { it.isNotBlank() }
        options.randomOrNull() ?: match.value
    }
}

private fun decodeImageSource(source: String, vm: ResourceViewModel): android.graphics.Bitmap? {
    val trimmed = source.trim()
    if (trimmed.startsWith("[")) {
        return runCatching {
            val arr = JSONArray(trimmed)
            (0 until arr.length()).firstNotNullOfOrNull { idx -> decodeImageSource(arr.get(idx).toString(), vm) }
        }.getOrNull()
    }
    val uri = Uri.parse(trimmed)
    return if (uri.scheme != null) vm.decodeUriToBitmap(uri) else vm.decodeBase64ToBitmap(trimmed)
}
