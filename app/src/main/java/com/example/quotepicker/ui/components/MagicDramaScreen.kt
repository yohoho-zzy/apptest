package com.example.quotepicker.ui.components

import android.net.Uri
import android.media.MediaPlayer
import android.widget.VideoView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
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
    val voicePlaybackMode: MagicDramaVoicePlaybackMode = MagicDramaVoicePlaybackMode.ROLE_ONLY,
    val initialThemeId: String = DEFAULT_MAGIC_DRAMA_THEME_ID
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
    data class RemoveVariable(val variableName: String) : DramaCommand
    data class Jump(val blockId: String) : DramaCommand
    data class ConditionalJump(val condition: String, val targetBlock: String) : DramaCommand
    data class SetAtmosphere(val themeKey: String) : DramaCommand
    data object ClearResourceArea : DramaCommand
    data object ClearAllVariables : DramaCommand
    data object ClearDialogue : DramaCommand
    data class SetBackgroundMusic(val source: String) : DramaCommand
    data object StopBackgroundMusic : DramaCommand
    data object StopCountdown : DramaCommand
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
    var backgroundPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var backgroundVideoUri by remember { mutableStateOf<Uri?>(null) }

    val parsed = remember(script) { parseDramaScript(script) }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val voiceSettingsStore = remember(context) { VoiceSettingsStore(context) }
    val availableThemes = remember(context) { loadMagicDramaThemes(context) }
    val themeAliases = remember(availableThemes) { magicDramaAtmosphereAliases(availableThemes) }
    val piperSpeechEngine = remember(context) { PiperSpeechEngine(context) }
    val voiceSettings = remember { voiceSettingsStore.load() }
    var currentThemeId by remember(playbackOptions.initialThemeId, availableThemes) {
        mutableStateOf(
            availableThemes.firstOrNull { it.id == playbackOptions.initialThemeId }?.id
                ?: availableThemes.firstOrNull { it.name == "清新" }?.id
                ?: availableThemes.firstOrNull()?.id
                ?: DEFAULT_MAGIC_DRAMA_THEME_ID
        )
    }
    val currentTheme = remember(currentThemeId, availableThemes) {
        availableThemes.firstOrNull { it.id == currentThemeId }
            ?: availableThemes.firstOrNull { it.name == "清新" }
            ?: availableThemes.first()
    }

    fun stopBackgroundPlayback() {
        backgroundPlayer?.stop()
        backgroundPlayer?.release()
        backgroundPlayer = null
        backgroundVideoUri = null
    }

    DisposableEffect(piperSpeechEngine) {
        onDispose {
            countdownJob?.cancel()
            pendingBranch?.cancel()
            stopBackgroundPlayback()
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
        currentThemeId = availableThemes.firstOrNull { it.id == playbackOptions.initialThemeId }?.id
            ?: availableThemes.firstOrNull { it.name == "清新" }?.id
                    ?: availableThemes.first().id
        countdownJob?.cancel()
        countdownJob = null
        stopBackgroundPlayback()

        fun jumpTo(blockId: String, queue: ArrayDeque<DramaCommand>) {
            queue.clear()
            parsed.blocks[blockId]?.asReversed()?.forEach { queue.addFirst(it) }
        }

        val queue = ArrayDeque(parsed.blocks[parsed.startBlock].orEmpty())
        while (queue.isNotEmpty() || countdownJob?.isActive == true) {
            timeoutBlockToJump?.let { blockId ->
                timeoutBlockToJump = null
                jumpTo(blockId, queue)
            }
            if (queue.isEmpty()) {
                delay(50)
                continue
            }

            when (val cmd = queue.removeFirst()) {
                is DramaCommand.Narration -> {
                    val text = resolveVariablePlaceholders(renderRandomToken(cmd.text), variables)
                    if (playbackOptions.voicePlaybackMode == MagicDramaVoicePlaybackMode.ALL) {
                        val narrationRoleName = if (cmd.important) "注意" else "旁白"
                        speakByRole(
                            text = text,
                            roleName = narrationRoleName,
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
                    val text = resolveVariablePlaceholders(cmd.text, variables).replace("nn", "\n")
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

                is DramaCommand.ShowResource -> {
                    currentMedia = DramaMedia.Resource(resolveVariablePlaceholders(cmd.source, variables))
                }
                is DramaCommand.WaitSeconds -> delay(cmd.seconds.coerceAtLeast(0) * 1000L)
                is DramaCommand.SetVariable -> applyVariableExpression(cmd.expression, variables)
                is DramaCommand.RemoveVariable -> variables.remove(cmd.variableName)
                is DramaCommand.Jump -> jumpTo(cmd.blockId, queue)
                is DramaCommand.ConditionalJump -> {
                    if (evaluateCondition(cmd.condition, variables)) jumpTo(cmd.targetBlock, queue)
                }
                is DramaCommand.SetAtmosphere -> {
                    val normalized = cmd.themeKey.trim().lowercase()
                    val target = themeAliases[normalized]
                    if (!target.isNullOrBlank()) currentThemeId = target
                }
                is DramaCommand.ClearResourceArea -> currentMedia = null
                is DramaCommand.ClearAllVariables -> variables.clear()
                is DramaCommand.ClearDialogue -> messages.clear()
                is DramaCommand.SetBackgroundMusic -> {
                    stopBackgroundPlayback()
                    val backgroundSource = resolveVariablePlaceholders(cmd.source, variables)
                    val resolvedUri = resolveMediaPlaybackUri(backgroundSource, vm)
                    if (resolvedUri != null && vm.isVideoUri(resolvedUri)) {
                        backgroundVideoUri = resolvedUri
                    } else {
                        backgroundVideoUri = null
                        backgroundPlayer = createLoopMediaPlayer(backgroundSource, vm)
                    }
                }
                is DramaCommand.StopBackgroundMusic -> {
                    stopBackgroundPlayback()
                }
                is DramaCommand.StopCountdown -> {
                    countdownJob?.cancel()
                    countdownJob = null
                    countdownSeconds = null
                    timeoutBlockToJump = null
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
                    buttonOptions = cmd.options.map { option ->
                        option.copy(text = resolveVariablePlaceholders(option.text, variables))
                    }
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

    LaunchedEffect(messages.size, currentThemeId) {
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
                .background(currentTheme.dialogBackground)
        ) {
            backgroundVideoUri?.let { HiddenBackgroundVideoPlayer(uri = it) }
            Column(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .weight(3f)
                        .fillMaxWidth()
                        .background(currentTheme.resourceBackground)
                ) {
                    DramaMediaArea(media = currentMedia, vm = vm, settings = settings)
                    countdownSeconds?.let { left ->
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(12.dp)
                                .background(currentTheme.resourceCountdownBubble.copy(alpha = 0.9f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(text = "倒数 ${left}s", color = currentTheme.resourceCountdownText, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                if (variables.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(currentTheme.statusBackground)
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "状态", style = MaterialTheme.typography.labelSmall, color = currentTheme.statusTitleText)
                        variables.forEach { (k, v) ->
                            Text(
                                text = "$k:$v",
                                style = MaterialTheme.typography.labelSmall,
                                color = currentTheme.statusBubbleText,
                                modifier = Modifier
                                    .background(currentTheme.statusBubble, RoundedCornerShape(10.dp))
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                Column(
                    modifier = Modifier
                        .weight(2f)
                        .fillMaxWidth()
                        .background(currentTheme.dialogBackground)
                        .padding(10.dp)
                ) {
                    Text(text = title, color = currentTheme.statusTitleText, style = MaterialTheme.typography.labelMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        state = listState
                    ) {
                        items(messages) { message ->
                            when (message) {
                                is DramaMessage.Narration -> NarrationBubble(message, currentTheme)
                                is DramaMessage.Role -> RoleBubble(message, currentTheme)
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
                onClick = {
                    stopBackgroundPlayback()
                    onClose()
                },
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
    val raw = normalizeScriptSymbols(expression).trim()
    when {
        raw.contains("+=") -> {
            val (name, deltaRaw) = raw.split("+=", limit = 2)
            val key = name.trim()
            val delta = evaluateNumericExpression(deltaRaw.trim(), variables) ?: 0
            val current = variables[key]?.toIntOrNull() ?: 0
            variables[key] = (current + delta).toString()
        }
        raw.contains("-=") -> {
            val (name, deltaRaw) = raw.split("-=", limit = 2)
            val key = name.trim()
            val delta = evaluateNumericExpression(deltaRaw.trim(), variables) ?: 0
            val current = variables[key]?.toIntOrNull() ?: 0
            variables[key] = (current - delta).toString()
        }
        raw.contains("=") -> {
            val (name, value) = raw.split("=", limit = 2)
            if (name.isNotBlank()) {
                val resolved = evaluateNumericExpression(value.trim(), variables)?.toString() ?: value.trim()
                variables[name.trim()] = resolved
            }
        }
    }
}

private fun evaluateCondition(condition: String, variables: Map<String, String>): Boolean {
    val normalized = normalizeScriptSymbols(condition)
    val clauses = normalized.split("&").map { it.trim() }.filter { it.isNotBlank() }
    if (clauses.isEmpty()) return false
    return clauses.all { clause -> evaluateSingleCondition(clause, variables) }
}

private fun evaluateSingleCondition(condition: String, variables: Map<String, String>): Boolean {
    val normalized = normalizeScriptSymbols(condition)
    val ops = listOf(">=", "<=", "!=", ">", "<", "=")
    val op = ops.firstOrNull { normalized.contains(it) } ?: return false
    val parts = normalized.split(op, limit = 2)
    if (parts.size != 2) return false
    val leftRaw = parts[0].trim()
    val rightRaw = parts[1].trim()

    val leftInt = evaluateNumericExpression(leftRaw, variables)
    val rightInt = evaluateNumericExpression(rightRaw, variables)
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
        val leftValue = resolveTokenValue(leftRaw, variables)
        val rightValue = resolveTokenValue(rightRaw, variables)
        when (op) {
            "=" -> leftValue == rightValue
            "!=" -> leftValue != rightValue
            else -> false
        }
    }
}

private fun evaluateNumericExpression(expression: String, variables: Map<String, String>): Int? {
    val normalized = normalizeScriptSymbols(expression).replace(" ", "")
    if (normalized.isBlank()) return null
    val terms = Regex("[+-]?[^+-]+")
        .findAll(normalized)
        .map { it.value }
        .toList()
    if (terms.isEmpty()) return null

    var total = 0
    for (term in terms) {
        val sign = if (term.startsWith("-")) -1 else 1
        val token = term.removePrefix("+").removePrefix("-")
        val value = resolveNumericToken(token, variables) ?: return null
        total += sign * value
    }
    return total
}

private fun resolveNumericToken(token: String, variables: Map<String, String>): Int? {
    val dice = Regex("^(\\d+)d(\\d+)$", RegexOption.IGNORE_CASE).matchEntire(token)
    if (dice != null) {
        val count = dice.groupValues[1].toIntOrNull() ?: return null
        val side = dice.groupValues[2].toIntOrNull() ?: return null
        if (count <= 0 || side <= 0) return null
        return (1..count).sumOf { (1..side).random() }
    }
    return token.toIntOrNull() ?: variables[token]?.toIntOrNull()
}

private fun resolveTokenValue(token: String, variables: Map<String, String>): String {
    if (variables.containsKey(token)) return variables[token].orEmpty()
    return token
}


@Composable
private fun HiddenBackgroundVideoPlayer(uri: Uri) {
    val holder = remember { mutableStateOf<VideoView?>(null) }
    DisposableEffect(uri) {
        onDispose {
            holder.value?.stopPlayback()
            holder.value = null
        }
    }
    AndroidView(
        factory = { context ->
            VideoView(context).apply {
                alpha = 0f
                setVideoURI(uri)
                setOnPreparedListener { player ->
                    player.isLooping = true
                    start()
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
        modifier = Modifier.size(1.dp)
    )
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
private fun NarrationBubble(message: DramaMessage.Narration, theme: MagicDramaTheme) {
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Text(
            text = message.text,
            color = if (message.important) theme.warningText else theme.narrationText,
            modifier = Modifier
                .background(if (message.important) theme.warningBubble else theme.narrationBubble, RoundedCornerShape(10.dp))
                .padding(horizontal = 12.dp, vertical = 8.dp)
        )
    }
}

@Composable
private fun RoleBubble(message: DramaMessage.Role, theme: MagicDramaTheme) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .border(1.dp, theme.statusBubble, CircleShape)
                .background(theme.avatarBackground),
            contentAlignment = Alignment.Center
        ) {
            Text(text = message.role.take(4), color = theme.avatarText, style = MaterialTheme.typography.labelSmall)
        }
        Spacer(modifier = Modifier.width(8.dp))
        Column(
            modifier = Modifier
                .background(theme.characterBubble, RoundedCornerShape(10.dp))
                .padding(8.dp)
        ) {
            Text(text = message.role, color = theme.avatarText, style = MaterialTheme.typography.labelSmall)
            Text(text = message.text, color = theme.characterText)
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

private fun normalizeBlockRef(raw: String): String {
    return raw.trim().removePrefix("@").trim()
}

private fun sanitizeScriptLine(raw: String): String {
    val normalized = normalizeScriptSymbols(raw)
    val trimmed = normalized.trim()
    return trimmed.replace(Regex("\\s\\[[^\\[\\]]*]$"), "").trim()
}

private fun normalizeScriptSymbols(raw: String): String {
    val builder = StringBuilder(raw.length)
    raw.forEach { ch ->
        val converted = when {
            ch == '　' -> ' '
            ch.code in 0xFF01..0xFF5E -> (ch.code - 0xFEE0).toChar()
            else -> ch
        }
        builder.append(converted)
    }
    return builder.toString()
}

private fun resolveVariablePlaceholders(text: String, variables: Map<String, String>): String {
    return Regex("%([^%]+)%").replace(text) { match ->
        val key = match.groupValues[1].trim()
        variables[key] ?: match.value
    }
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
            line.startsWith("删:") -> {
                val name = line.substringAfter(':').trim()
                if (name.isNotBlank()) commands += DramaCommand.RemoveVariable(name)
            }
            line.startsWith("跳:") -> {
                val target = normalizeBlockRef(line.substringAfter(':'))
                if (target.isNotBlank()) commands += DramaCommand.Jump(target)
            }
            line.startsWith("判:") -> {
                val body = line.substringAfter(':').trim()
                val parts = body.split("--", limit = 2)
                if (parts.size == 2) {
                    val target = normalizeBlockRef(parts[1])
                    if (target.isNotBlank()) {
                        commands += DramaCommand.ConditionalJump(parts[0].trim(), target)
                    }
                }
            }
            line.startsWith("氛围:") -> {
                val key = line.substringAfter(':').trim()
                if (key.isNotBlank()) commands += DramaCommand.SetAtmosphere(key)
            }
            line.startsWith("计时:") -> {
                val body = line.substringAfter(':').trim()
                val parts = body.split("--", limit = 2)
                val sec = parts.firstOrNull()?.trim()?.toIntOrNull()
                val target = parts.getOrNull(1)?.let(::normalizeBlockRef)
                if (sec != null && !target.isNullOrBlank()) commands += DramaCommand.Countdown(sec, target)
            }
            line.equals("c1", ignoreCase = true) -> commands += DramaCommand.ClearResourceArea
            line.equals("c2", ignoreCase = true) -> commands += DramaCommand.ClearAllVariables
            line.equals("c3", ignoreCase = true) -> commands += DramaCommand.ClearDialogue
            line.startsWith("背景:") -> {
                val source = line.substringAfter(':').trim()
                if (source.isNotBlank()) commands += DramaCommand.SetBackgroundMusic(source)
            }
            line.equals("停:背景", ignoreCase = true) -> commands += DramaCommand.StopBackgroundMusic
            line.equals("停:计时", ignoreCase = true) -> commands += DramaCommand.StopCountdown
            line.startsWith("按钮:") -> {
                val options = mutableListOf<DramaButtonOption>()
                var j = i + 1
                while (j < lines.size && isButtonOptionLine(lines[j])) {
                    val p = lines[j].split("--", limit = 2)
                    val text = p.firstOrNull()?.trim().orEmpty()
                    val target = p.getOrNull(1)?.let(::normalizeBlockRef).orEmpty()
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

private fun isButtonOptionLine(line: String): Boolean {
    if (!line.contains("--")) return false
    if (line.startsWith("@")) return false
    return !isScriptCommandLine(line)
}

private fun isScriptCommandLine(line: String): Boolean {
    return line.matches(Regex("^w\\d+$", RegexOption.IGNORE_CASE)) ||
        line.equals("c1", ignoreCase = true) ||
        line.equals("c2", ignoreCase = true) ||
        line.equals("c3", ignoreCase = true) ||
        line.equals("停:背景", ignoreCase = true) ||
        line.equals("停:计时", ignoreCase = true) ||
        line.startsWith("资源:") ||
        line.startsWith("旁白:") ||
        line.startsWith("注意:") ||
        line.startsWith("设:") ||
        line.startsWith("删:") ||
        line.startsWith("跳:") ||
        line.startsWith("判:") ||
        line.startsWith("氛围:") ||
        line.startsWith("计时:") ||
        line.startsWith("按钮:") ||
        line.startsWith("背景:")
}

private fun resolveMediaPlaybackUri(source: String, vm: ResourceViewModel): Uri? {
    return vm.resolveMediaUriByCodeOrPath(source)
        ?: runCatching { Uri.parse(source) }.getOrNull()?.takeIf { it.scheme != null }
}

private fun createLoopMediaPlayer(source: String, vm: ResourceViewModel): MediaPlayer? {
    val uri = resolveMediaPlaybackUri(source, vm) ?: return null
    return runCatching {
        MediaPlayer.create(vm.getApplication(), uri)?.apply {
            isLooping = true
            start()
        }
    }.getOrNull()
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
