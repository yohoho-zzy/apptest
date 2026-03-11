package com.example.quotepicker.ui.components

import android.net.Uri
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
import androidx.compose.runtime.mutableStateMapOf
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
import com.example.quotepicker.data.ResourceMarkState
import com.example.quotepicker.data.ResourceType
import com.example.quotepicker.util.PiperSpeechEngine
import com.example.quotepicker.util.RoleVoiceSetting
import com.example.quotepicker.util.VoiceSettingsStore
import com.example.quotepicker.vm.ResourceViewModel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

data class MagicDramaSettings(
    val defaultDelayMs: Long = 1000L,
    val imageIntervalMs: Long = 3000L
)

private const val DEFAULT_VOICE_SETTING_ROLE = "__default_voice_setting__"

data class MagicDramaPlaybackOptions(
    val voicePlaybackMode: MagicDramaVoicePlaybackMode = MagicDramaVoicePlaybackMode.ROLE_ONLY
)

enum class MagicDramaVoicePlaybackMode {
    SILENT,
    ROLE_ONLY,
    ALL
}

private sealed interface DramaCommand {
    data class Narration(val text: String, val important: Boolean) : DramaCommand
    data class RoleLine(val roleKey: String, val text: String) : DramaCommand
    data class ShowResource(val source: String) : DramaCommand
    data class ShowButtons(val options: List<DramaButtonOption>) : DramaCommand
    data class Countdown(val seconds: Int, val timeoutBlock: String) : DramaCommand
    data class WaitSeconds(val seconds: Int) : DramaCommand
    data class SetVariable(val expression: String) : DramaCommand
    data class Jump(val blockId: String) : DramaCommand
    data class ConditionalJump(val condition: String, val targetBlock: String) : DramaCommand
}

private data class DramaButtonOption(val text: String, val branchId: String)

private sealed interface DramaMessage {
    data class Narration(val text: String, val important: Boolean) : DramaMessage
    data class Role(val role: String, val text: String) : DramaMessage
}

private sealed interface DramaMedia {
    data class Resource(val source: String) : DramaMedia
}

@Composable
fun MagicDramaScreen(
    title: String,
    script: String,
    boundCharacters: List<CharacterEntity>,
    settings: MagicDramaSettings,
    playbackOptions: MagicDramaPlaybackOptions,
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
    val voiceSettingsStore = remember(context) { VoiceSettingsStore(context) }
    val piperSpeechEngine = remember(context) { PiperSpeechEngine(context) }
    val voiceSettings = remember { voiceSettingsStore.load() }

    DisposableEffect(Unit) {
        onDispose {
            countdownJob?.cancel()
            pendingBranch?.cancel()
            coroutineScope.launch {
                piperSpeechEngine.stop()
                piperSpeechEngine.cleanupPreviewTempFiles()
                piperSpeechEngine.release()
            }
        }
    }
    val variables = remember { mutableStateMapOf<String, String>() }
    var timeoutBlockToJump by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(script, settings.defaultDelayMs, voiceSettings, playbackOptions) {
        messages.clear()
        currentMedia = null
        countdownSeconds = null
        buttonOptions = emptyList()
        pendingBranch = null
        variables.clear()
        timeoutBlockToJump = null
        countdownJob?.cancel()
        countdownJob = null

        fun jumpTo(blockId: String, queue: ArrayDeque<DramaCommand>) {
            queue.clear()
            parsed.blocks[blockId]?.asReversed()?.forEach { queue.addFirst(it) }
        }

        val queue = ArrayDeque(parsed.blocks[parsed.startBlock].orEmpty())
        while (queue.isNotEmpty()) {
            timeoutBlockToJump?.let { blockId ->
                timeoutBlockToJump = null
                jumpTo(blockId, queue)
            }

            when (val cmd = queue.removeFirst()) {
                is DramaCommand.Narration -> {
                    val text = renderRandomToken(cmd.text)
                    if (playbackOptions.voicePlaybackMode == MagicDramaVoicePlaybackMode.ALL) {
                        speakByRole(
                            text = text,
                            roleName = "注意",
                            voiceSettings = voiceSettings,
                            piperSpeechEngine = piperSpeechEngine,
                            vm = vm
                        )
                    }
                    messages += DramaMessage.Narration(text = text, important = cmd.important)
                    delay(settings.defaultDelayMs)
                }

                is DramaCommand.RoleLine -> {
                    val role = resolveRoleName(cmd.roleKey, boundCharacters)
                    val text = cmd.text.replace("nn", "\n")
                    if (playbackOptions.voicePlaybackMode != MagicDramaVoicePlaybackMode.SILENT) {
                        speakByRole(
                            text = text,
                            roleName = role,
                            voiceSettings = voiceSettings,
                            piperSpeechEngine = piperSpeechEngine,
                            vm = vm
                        )
                    }
                    messages += DramaMessage.Role(role = role, text = text)
                    delay(settings.defaultDelayMs)
                }

                is DramaCommand.ShowResource -> currentMedia = DramaMedia.Resource(cmd.source)
                is DramaCommand.WaitSeconds -> delay(cmd.seconds.coerceAtLeast(0) * 1000L)
                is DramaCommand.SetVariable -> applyVariableExpression(cmd.expression, variables)
                is DramaCommand.Jump -> jumpTo(cmd.blockId, queue)
                is DramaCommand.ConditionalJump -> {
                    if (evaluateCondition(cmd.condition, variables)) jumpTo(cmd.targetBlock, queue)
                }

                is DramaCommand.Countdown -> {
                    countdownJob?.cancel()
                    countdownJob = coroutineScope.launch {
                        launchCountdown(cmd.seconds) { left -> countdownSeconds = left }
                        timeoutBlockToJump = cmd.timeoutBlock
                        pendingBranch?.complete(cmd.timeoutBlock)
                    }
                }

                is DramaCommand.ShowButtons -> {
                    buttonOptions = cmd.options
                    val waiter = CompletableDeferred<String>()
                    pendingBranch = waiter
                    val selected = waiter.await()
                    countdownJob?.cancel()
                    countdownJob = null
                    countdownSeconds = null
                    buttonOptions = emptyList()
                    pendingBranch = null
                    jumpTo(selected, queue)
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
                DramaMediaArea(media = currentMedia, vm = vm, settings = settings)
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
                Row(modifier = Modifier.weight(1f)) {
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
                    if (variables.isNotEmpty()) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(
                            modifier = Modifier
                                .width(120.dp)
                                .background(Color(0xFFF2F6FF), RoundedCornerShape(8.dp))
                                .padding(8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(text = "状态", style = MaterialTheme.typography.labelSmall, color = Color(0xFF42506A))
                            variables.forEach { (k, v) ->
                                Text(text = "$k:$v", style = MaterialTheme.typography.labelSmall, color = Color(0xFF1F2433))
                            }
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


private suspend fun speakByRole(
    text: String,
    roleName: String,
    voiceSettings: com.example.quotepicker.util.VoiceSettings,
    piperSpeechEngine: PiperSpeechEngine,
    vm: ResourceViewModel
) {
    val roleSetting = resolveRoleSettingFromTextResource(roleName, vm)
        ?: voiceSettings.roleSettings.firstOrNull { it.roleName == roleName }
        ?: voiceSettings.roleSettings.firstOrNull { it.roleName == DEFAULT_VOICE_SETTING_ROLE }
    val roleProfile = voiceSettings.profiles.firstOrNull { it.id == roleSetting?.profileId }
        ?: voiceSettings.profiles.firstOrNull { it.modelUri == "asset://tts/vits-zh-hf-fanchen-C.onnx" }
    if (roleProfile != null) {
        val effective = roleProfile.copy(
            speakerId = roleSetting?.speakerId,
            noiseScale = roleSetting?.noiseScale,
            noiseScaleW = roleSetting?.noiseScaleW,
            lengthScale = roleSetting?.lengthScale,
            maxNumSentences = roleSetting?.maxNumSentences,
            silenceScale = roleSetting?.silenceScale
        )
        piperSpeechEngine.speak(text, effective, roleSetting?.speechRate ?: 1.0f)
    }
}

private fun resolveRoleSettingFromTextResource(roleName: String, vm: ResourceViewModel): RoleVoiceSetting? {
    val candidates = vm.allResources.value.filter { item ->
        item.resource.type == ResourceType.TEXT &&
            item.resource.title.contains("声音配置") &&
            item.characters.any { it.name == roleName }
    }
    val selected = candidates.filter { it.resource.markState == ResourceMarkState.FAVORITE }
        .ifEmpty { candidates }
        .firstOrNull()
        ?: return null
    val raw = selected.resource.quoteText ?: return null
    return runCatching {
        val json = JSONObject(raw)
        RoleVoiceSetting(
            roleName = roleName,
            speechRate = json.optDouble("speechRate", 1.0).toFloat(),
            speakerId = if (json.has("speakerId")) json.optInt("speakerId") else null,
            noiseScale = if (json.has("noiseScale")) json.optDouble("noiseScale").toFloat() else null,
            noiseScaleW = if (json.has("noiseScaleW")) json.optDouble("noiseScaleW").toFloat() else null,
            lengthScale = if (json.has("lengthScale")) json.optDouble("lengthScale").toFloat() else null,
            maxNumSentences = if (json.has("maxNumSentences")) json.optInt("maxNumSentences") else null,
            silenceScale = if (json.has("silenceScale")) json.optDouble("silenceScale").toFloat() else null
        )
    }.getOrNull()
}

private suspend fun launchCountdown(total: Int, onTick: (Int?) -> Unit) {
    onTick(total)
    for (left in total - 1 downTo 0) {
        delay(1000)
        onTick(left)
    }
    onTick(null)
}

private fun applyVariableExpression(expression: String, variables: MutableMap<String, String>) {
    val raw = expression.trim()
    when {
        raw.contains("+=") -> {
            val (name, deltaRaw) = raw.split("+=", limit = 2)
            val key = name.trim()
            val delta = deltaRaw.trim().toIntOrNull() ?: 0
            val current = variables[key]?.toIntOrNull() ?: 0
            variables[key] = (current + delta).toString()
        }
        raw.contains("-=") -> {
            val (name, deltaRaw) = raw.split("-=", limit = 2)
            val key = name.trim()
            val delta = deltaRaw.trim().toIntOrNull() ?: 0
            val current = variables[key]?.toIntOrNull() ?: 0
            variables[key] = (current - delta).toString()
        }
        raw.contains("=") -> {
            val (name, value) = raw.split("=", limit = 2)
            if (name.isNotBlank()) variables[name.trim()] = value.trim()
        }
    }
}

private fun evaluateCondition(condition: String, variables: Map<String, String>): Boolean {
    val ops = listOf(">=", "<=", "!=", ">", "<", "=")
    val op = ops.firstOrNull { condition.contains(it) } ?: return false
    val parts = condition.split(op, limit = 2)
    if (parts.size != 2) return false
    val key = parts[0].trim()
    val right = parts[1].trim()
    val leftValue = variables[key] ?: ""

    val leftInt = leftValue.toIntOrNull()
    val rightInt = right.toIntOrNull()
    return if (leftInt != null && rightInt != null) {
        when (op) {
            "=" -> leftInt == rightInt
            "!=" -> leftInt != rightInt
            ">" -> leftInt > rightInt
            "<" -> leftInt < rightInt
            ">=" -> leftInt >= rightInt
            "<=" -> leftInt <= rightInt
            else -> false
        }
    } else {
        when (op) {
            "=" -> leftValue == right
            "!=" -> leftValue != right
            else -> false
        }
    }
}

@Composable
private fun LoopVideo(uri: Uri, onCompleted: (() -> Unit)? = null) {
    val holder = remember { mutableStateOf<VideoView?>(null) }
    DisposableEffect(Unit) {
        onDispose { holder.value?.stopPlayback() }
    }
    AndroidView(
        factory = { context ->
            VideoView(context).apply {
                setVideoURI(uri)
                setOnPreparedListener { it.start() }
                setOnCompletionListener {
                    if (onCompleted != null) onCompleted() else it.start()
                }
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
    val startBlock: String,
    val blocks: Map<String, List<DramaCommand>>
)

private fun parseDramaScript(raw: String): ParsedDramaScript {
    val lines = raw.lines().map { sanitizeScriptLine(it) }.filter { it.isNotBlank() }
    val blockLines = linkedMapOf<String, MutableList<String>>()
    var currentBlock: String? = null
    lines.forEach { line ->
        if (line.startsWith("@")) {
            currentBlock = line.removePrefix("@").trim()
            if (currentBlock!!.isNotBlank()) blockLines.getOrPut(currentBlock!!) { mutableListOf() }
        } else {
            currentBlock?.let { blockLines.getOrPut(it) { mutableListOf() }.add(line) }
        }
    }

    val start = blockLines.keys.firstOrNull() ?: "开始"
    val blocks = blockLines.mapValues { (_, content) -> parseBlockCommands(content) }
    return ParsedDramaScript(startBlock = start, blocks = blocks)
}

private fun sanitizeScriptLine(raw: String): String {
    val trimmed = raw.trim()
    return trimmed.replace(Regex("\\s\\[[^\\[\\]]*]$"), "").trim()
}

private fun parseBlockCommands(lines: List<String>): List<DramaCommand> {
    val commands = mutableListOf<DramaCommand>()
    var i = 0
    while (i < lines.size) {
        val line = lines[i]
        when {
            line.matches(Regex("^w\\d+$", RegexOption.IGNORE_CASE)) -> {
                commands += DramaCommand.WaitSeconds(line.drop(1).toIntOrNull() ?: 0)
            }
            line.startsWith("资源:") -> commands += DramaCommand.ShowResource(line.substringAfter(':').trim())
            line.startsWith("旁白:") -> commands += DramaCommand.Narration(line.substringAfter(':').trim(), important = false)
            line.startsWith("注意:") -> commands += DramaCommand.Narration(line.substringAfter(':').trim(), important = true)
            line.startsWith("设:") -> commands += DramaCommand.SetVariable(line.substringAfter(':').trim())
            line.startsWith("跳:") -> commands += DramaCommand.Jump(line.substringAfter(':').trim())
            line.startsWith("判:") -> {
                val body = line.substringAfter(':').trim()
                val parts = body.split("--", limit = 2)
                if (parts.size == 2) commands += DramaCommand.ConditionalJump(parts[0].trim(), parts[1].trim())
            }
            line.startsWith("计时:") -> {
                val body = line.substringAfter(':').trim()
                val parts = body.split("--", limit = 2)
                val sec = parts.firstOrNull()?.trim()?.toIntOrNull()
                val target = parts.getOrNull(1)?.trim()
                if (sec != null && !target.isNullOrBlank()) commands += DramaCommand.Countdown(sec, target)
            }
            line.startsWith("按钮:") -> {
                val options = mutableListOf<DramaButtonOption>()
                var j = i + 1
                while (j < lines.size && lines[j].contains("--")) {
                    val p = lines[j].split("--", limit = 2)
                    val text = p.firstOrNull()?.trim().orEmpty()
                    val target = p.getOrNull(1)?.trim().orEmpty()
                    if (text.isNotBlank() && target.isNotBlank()) options += DramaButtonOption(text, target)
                    j++
                }
                if (options.isNotEmpty()) commands += DramaCommand.ShowButtons(options)
                i = j - 1
            }
            line.contains(":") -> {
                val key = line.substringBefore(':').trim()
                val value = line.substringAfter(':').trim()
                if (key.isNotBlank()) commands += DramaCommand.RoleLine(key, value)
            }
        }
        i++
    }
    return commands
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
    decodeResourceFileInfo(trimmed)?.let { ref ->
        if (ref.type == ResourceType.IMAGE) {
            val uri = vm.resolveMediaUri(ResourceType.IMAGE, ref.name, ref.resourceId)
            if (uri != null) return vm.decodeUriToBitmap(uri)
        }
    }
    val uri = Uri.parse(trimmed)
    return if (uri.scheme != null) vm.decodeUriToBitmap(uri) else vm.decodeBase64ToBitmap(trimmed)
}

@Composable
private fun DramaMediaArea(media: DramaMedia?, vm: ResourceViewModel, settings: MagicDramaSettings) {
    when (media) {
        is DramaMedia.Resource -> {
            val playlist = remember(media.source, vm.allResources.value) { vm.resolveMixedGroupSources(media.source) }
            if (playlist.size > 1) {
                ResourceCarousel(source = media.source, vm = vm, intervalMs = settings.imageIntervalMs)
            } else {
                val uri = remember(media.source, vm.allResources.value) { vm.resolveMediaUriByCodeOrPath(media.source) }
                val isVideo = uri != null && vm.isVideoUri(uri)
                if (uri == null) {
                    val bitmap = remember(media.source) { decodeImageSource(media.source, vm) }
                    if (bitmap != null) {
                        androidx.compose.foundation.Image(bitmap = bitmap.asImageBitmap(), contentDescription = "资源图片", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
                    }
                } else if (isVideo) {
                    LoopVideo(uri = uri)
                } else {
                    val bitmap = remember(uri) { vm.decodeUriToBitmap(uri) }
                    if (bitmap != null) androidx.compose.foundation.Image(bitmap = bitmap.asImageBitmap(), contentDescription = "资源图片", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
                }
            }
        }
        null -> Box(modifier = Modifier.fillMaxSize())
    }
}

@Composable
private fun ResourceCarousel(source: String, vm: ResourceViewModel, intervalMs: Long) {
    val items = remember(source, vm.allResources.value) { vm.resolveMixedGroupSources(source) }
    var index by remember(source) { mutableStateOf(0) }
    val current = items.getOrNull(index)
    LaunchedEffect(items) { index = 0 }
    LaunchedEffect(items, index, intervalMs) {
        if (items.size > 1 && current?.type == ResourceType.IMAGE) {
            delay(intervalMs.coerceIn(300L, 10000L))
            index = (index + 1) % items.size
        }
    }
    when (current?.type) {
        ResourceType.VIDEO -> LoopVideo(uri = Uri.parse(current.path), onCompleted = {
            if (items.size > 1) index = (index + 1) % items.size
        })
        ResourceType.IMAGE -> {
            val bitmap = remember(current.path) { vm.decodeUriToBitmap(Uri.parse(current.path)) }
            if (bitmap != null) androidx.compose.foundation.Image(bitmap = bitmap.asImageBitmap(), contentDescription = "资源图片组", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
        }
        else -> Unit
    }
}
