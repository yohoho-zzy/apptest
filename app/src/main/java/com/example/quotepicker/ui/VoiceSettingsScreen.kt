package com.example.quotepicker.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.quotepicker.data.CharacterEntity
import com.example.quotepicker.data.TagEntity
import com.example.quotepicker.util.PiperSpeechEngine
import com.example.quotepicker.util.RoleVoiceSetting
import com.example.quotepicker.util.VoiceProfile
import com.example.quotepicker.vm.ResourceViewModel
import com.example.quotepicker.vm.VoiceSettingsViewModel
import java.util.Locale
import kotlinx.coroutines.launch

private const val BUILTIN_MODEL_URI = "asset://tts/vits-zh-hf-fanchen-C.onnx"

private sealed class VoiceSaveMode {
    data class Text(val initialContent: String) : VoiceSaveMode()
    data class Sound(val initialUri: Uri) : VoiceSaveMode()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceSettingsScreen(
    onBack: () -> Unit,
    vm: VoiceSettingsViewModel = viewModel(),
    resourceVm: ResourceViewModel = viewModel()
) {
    val ui by vm.uiState.collectAsState()
    val resourceUi by resourceVm.uiState.collectAsState()
    val builtInProfile = ui.settings.profiles.firstOrNull { it.modelUri == BUILTIN_MODEL_URI }
    val current = vm.globalRoleSetting(ui.settings)
    var saveMode by remember { mutableStateOf<VoiceSaveMode?>(null) }

    if (saveMode != null) {
        VoiceResourceSaveScreen(
            mode = saveMode!!,
            tags = resourceUi.tags,
            characters = resourceUi.characters,
            onBack = { saveMode = null },
            onSaveText = { title, content, tagIds, characterIds ->
                resourceVm.addTextResource(title, content, tagIds, characterIds)
                saveMode = null
            },
            onSaveSound = { title, uris, tagIds, characterIds ->
                resourceVm.addSoundGroup(title, uris, tagIds, characterIds)
                saveMode = null
            }
        )
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("声音设置") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { inner ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text("当前固定模型：vits-zh-hf-fanchen-C.onnx（无需导入）")
            }
            item {
                VoiceSettingEditor(
                    title = "通用配置",
                    baseProfile = builtInProfile,
                    initial = current,
                    initialPreviewText = ui.settings.rolePreviewTexts[VoiceSettingsViewModel.DEFAULT_ROLE_KEY],
                    onSave = {
                        vm.updateRoleSetting(
                            VoiceSettingsViewModel.DEFAULT_ROLE_KEY,
                            builtInProfile?.id,
                            it.speechRate,
                            it.speakerId,
                            it.noiseScale,
                            it.noiseScaleW,
                            it.lengthScale,
                            it.maxNumSentences,
                            it.silenceScale
                        )
                    },
                    onPersistPreviewText = { vm.updatePreviewText(VoiceSettingsViewModel.DEFAULT_ROLE_KEY, it) },
                    onOpenSaveTextPage = { content -> saveMode = VoiceSaveMode.Text(content) },
                    onOpenSaveSoundPage = { uri -> saveMode = VoiceSaveMode.Sound(uri) },
                    buildConfigText = { vm.buildDefaultConfigText() }
                )
            }
        }
    }
}

@Composable
private fun VoiceSettingEditor(
    title: String,
    baseProfile: VoiceProfile?,
    initial: RoleVoiceSetting?,
    initialPreviewText: String?,
    onSave: (RoleVoiceSetting) -> Unit,
    onPersistPreviewText: (String) -> Unit,
    onOpenSaveTextPage: (String) -> Unit,
    onOpenSaveSoundPage: (Uri) -> Unit,
    buildConfigText: () -> String
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val speech = remember(context) { PiperSpeechEngine(context) }

    var speed by remember(initial?.speechRate) { mutableStateOf((initial?.speechRate ?: 1.0f).coerceIn(0.6f, 1.8f)) }
    var speakerId by remember(initial?.speakerId) { mutableStateOf((initial?.speakerId ?: 0).coerceIn(0, 186)) }
    var noiseScale by remember(initial?.noiseScale) { mutableStateOf((initial?.noiseScale ?: 0.667f).coerceIn(0.1f, 2.0f)) }
    var noiseScaleW by remember(initial?.noiseScaleW) { mutableStateOf((initial?.noiseScaleW ?: 0.8f).coerceIn(0.1f, 2.0f)) }
    var lengthScale by remember(initial?.lengthScale) { mutableStateOf((initial?.lengthScale ?: 1.0f).coerceIn(0.5f, 2.0f)) }
    var maxNumSentences by remember(initial?.maxNumSentences) { mutableStateOf((initial?.maxNumSentences ?: 1).coerceIn(1, 10)) }
    var silenceScale by remember(initial?.silenceScale) { mutableStateOf((initial?.silenceScale ?: 0.2f).coerceIn(0f, 1f)) }
    var previewText by remember(initialPreviewText) { mutableStateOf(initialPreviewText ?: "这是一段语音预览。") }
    var statusText by remember { mutableStateOf("") }

    fun currentSetting() = RoleVoiceSetting(
        roleName = VoiceSettingsViewModel.DEFAULT_ROLE_KEY,
        speechRate = speed,
        speakerId = speakerId,
        noiseScale = noiseScale,
        noiseScaleW = noiseScaleW,
        lengthScale = lengthScale,
        maxNumSentences = maxNumSentences,
        silenceScale = silenceScale
    )

    fun saveCurrent() = onSave(currentSetting())

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("$title（每项都限制在安全区间）")

        Text("speakerId>说话者(0-186)")
        Row {
            Text("$speakerId")
            Slider(value = speakerId.toFloat(), onValueChange = { speakerId = it.toInt(); saveCurrent() }, valueRange = 0f..186f, modifier = Modifier.weight(1f))
        }

        Text("speed>语速(0.6-1.8)")
        Row {
            Text("${"%.2f".format(Locale.US, speed)}")
            Slider(value = speed, onValueChange = { speed = it; saveCurrent() }, valueRange = 0.6f..1.8f, modifier = Modifier.weight(1f))
        }

        Text("noiseScale>情绪:随机发音(0.1-2.0)")
        Row {
            Text("${"%.3f".format(Locale.US, noiseScale)}")
            Slider(value = noiseScale, onValueChange = { noiseScale = it; saveCurrent() }, valueRange = 0.1f..2.0f, modifier = Modifier.weight(1f))
        }

        Text("noiseScaleW>音素:随机间隔(0.1-2.0)")
        Row {
            Text("${"%.3f".format(Locale.US, noiseScaleW)}")
            Slider(value = noiseScaleW, onValueChange = { noiseScaleW = it; saveCurrent() }, valueRange = 0.1f..2.0f, modifier = Modifier.weight(1f))
        }

        Text("lengthScale>句子时长(0.5-2.0)")
        Row {
            Text("${"%.2f".format(Locale.US, lengthScale)}")
            Slider(value = lengthScale, onValueChange = { lengthScale = it; saveCurrent() }, valueRange = 0.5f..2.0f, modifier = Modifier.weight(1f))
        }

        Text("maxNumSentences>句子处理量(1-10)")
        Row {
            Text("$maxNumSentences")
            Slider(value = maxNumSentences.toFloat(), onValueChange = { maxNumSentences = it.toInt().coerceIn(1, 10); saveCurrent() }, valueRange = 1f..10f, modifier = Modifier.weight(1f))
        }

        Text("silenceScale>句子间隔(0-1)")
        Row {
            Text("${"%.2f".format(Locale.US, silenceScale)}")
            Slider(value = silenceScale, onValueChange = { silenceScale = it; saveCurrent() }, valueRange = 0f..1f, modifier = Modifier.weight(1f))
        }

        OutlinedTextField(
            value = previewText,
            onValueChange = {
                previewText = it
                onPersistPreviewText(it)
            },
            label = { Text("预览文本（可修改）") },
            modifier = Modifier.fillMaxWidth()
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = {
                    if (baseProfile == null) return@Button
                    val setting = currentSetting()
                    saveCurrent()
                    val effective = baseProfile.copy(
                        speakerId = setting.speakerId,
                        noiseScale = setting.noiseScale,
                        noiseScaleW = setting.noiseScaleW,
                        lengthScale = setting.lengthScale,
                        maxNumSentences = setting.maxNumSentences,
                        silenceScale = setting.silenceScale
                    )
                    scope.launch {
                        statusText = if (speech.speak(previewText, effective, setting.speechRate)) "朗读中" else "朗读失败"
                    }
                },
                enabled = baseProfile != null
            ) { Text("预览朗读") }

            Button(onClick = { scope.launch { speech.stop(); statusText = "已停止" } }) { Text("停止") }
            Button(onClick = { onOpenSaveTextPage(buildConfigText()) }) { Text("保存") }
            Button(onClick = {
                if (baseProfile == null) return@Button
                val setting = currentSetting()
                val effective = baseProfile.copy(
                    speakerId = setting.speakerId,
                    noiseScale = setting.noiseScale,
                    noiseScaleW = setting.noiseScaleW,
                    lengthScale = setting.lengthScale,
                    maxNumSentences = setting.maxNumSentences,
                    silenceScale = setting.silenceScale
                )
                scope.launch {
                    statusText = "正在生成声音文件..."
                    val file = speech.synthesizeToTempWav(previewText, effective, setting.speechRate)
                    if (file != null) {
                        onOpenSaveSoundPage(Uri.fromFile(file))
                        statusText = "已生成临时声音文件"
                    } else {
                        statusText = "生成失败"
                    }
                }
            }) { Text("留声") }
        }

        if (statusText.isNotBlank()) Text(statusText)

    }
}

@Composable
private fun VoiceResourceSaveScreen(
    mode: VoiceSaveMode,
    tags: List<TagEntity>,
    characters: List<CharacterEntity>,
    onBack: () -> Unit,
    onSaveText: (String, String, List<Long>, List<Long>) -> Unit,
    onSaveSound: (String, List<Uri>, List<Long>, List<Long>) -> Unit
) {
    val context = LocalContext.current
    var title by remember(mode) {
        mutableStateOf(
            when (mode) {
                is VoiceSaveMode.Text -> "声音配置"
                is VoiceSaveMode.Sound -> "留声"
            }
        )
    }
    var content by remember(mode) { mutableStateOf((mode as? VoiceSaveMode.Text)?.initialContent ?: "") }
    var selectedTagIds by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var selectedCharacterIds by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var showTagPicker by remember { mutableStateOf(false) }
    var showCharacterPicker by remember { mutableStateOf(false) }
    var soundUris by remember(mode) {
        mutableStateOf(
            when (mode) {
                is VoiceSaveMode.Sound -> listOf(mode.initialUri)
                is VoiceSaveMode.Text -> emptyList()
            }
        )
    }

    val soundPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        soundUris = uris
        uris.forEach { uri ->
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
        }
    }

    val canSave = title.isNotBlank() && selectedCharacterIds.isNotEmpty() && (
        mode is VoiceSaveMode.Text && content.isNotBlank() || mode is VoiceSaveMode.Sound
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (mode is VoiceSaveMode.Text) "创建文本" else "上传声音组") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            when (mode) {
                                is VoiceSaveMode.Text -> onSaveText(title.trim(), content.trim(), selectedTagIds.toList(), selectedCharacterIds.toList())
                                is VoiceSaveMode.Sound -> onSaveSound(title.trim(), soundUris, selectedTagIds.toList(), selectedCharacterIds.toList())
                            }
                        },
                        enabled = canSave
                    ) { Text("保存") }
                }
            )
        }
    ) { inner ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text(if (mode is VoiceSaveMode.Text) "文本名" else "名称") }, modifier = Modifier.fillMaxWidth())
            if (mode is VoiceSaveMode.Text) {
                OutlinedTextField(value = content, onValueChange = { content = it }, label = { Text("文本内容") }, modifier = Modifier.fillMaxWidth())
            } else {
                TextButton(onClick = { soundPicker.launch(arrayOf("audio/*")) }) {
                    Spacer(Modifier.width(8.dp))
                    Text("选择音频")
                }
                Text(if (soundUris.isEmpty()) "未选择音频（将创建空声音组）" else "已选择 ${soundUris.size} 个音频")
            }

            SelectSummaryRow("标签", tags.filter { selectedTagIds.contains(it.id) }.map { it.name }, onPick = { showTagPicker = true })
            SelectSummaryRow("角色", characters.filter { selectedCharacterIds.contains(it.id) }.map { it.name }, onPick = { showCharacterPicker = true })
        }
    }

    if (showTagPicker) {
        MultiSelectDialog(
            title = "选择标签",
            options = tags.map { it.id to it.name },
            selectedIds = selectedTagIds,
            onDismiss = { showTagPicker = false },
            onConfirm = {
                selectedTagIds = it
                showTagPicker = false
            }
        )
    }

    if (showCharacterPicker) {
        MultiSelectDialog(
            title = "选择角色",
            options = characters.map { it.id to it.name },
            selectedIds = selectedCharacterIds,
            onDismiss = { showCharacterPicker = false },
            onConfirm = {
                selectedCharacterIds = it
                showCharacterPicker = false
            }
        )
    }
}

@Composable
private fun SelectSummaryRow(label: String, selectedNames: List<String>, onPick: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text("$label：", modifier = Modifier.padding(bottom = 6.dp))
        Row(modifier = Modifier.fillMaxWidth().clickable { onPick() }, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            if (selectedNames.isEmpty()) {
                Text("未选择$label", maxLines = 1, overflow = TextOverflow.Ellipsis)
            } else {
                selectedNames.take(3).forEach { name ->
                    FilterChip(selected = true, onClick = onPick, label = { Text(name, maxLines = 1, overflow = TextOverflow.Ellipsis) })
                }
            }
            TextButton(onClick = onPick) { Text("选择") }
        }
    }
}

@Composable
private fun MultiSelectDialog(
    title: String,
    options: List<Pair<Long, String>>,
    selectedIds: Set<Long>,
    onDismiss: () -> Unit,
    onConfirm: (Set<Long>) -> Unit
) {
    var localSelected by remember(selectedIds) { mutableStateOf(selectedIds) }
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                options.forEach { option ->
                    item {
                    FilterChip(
                        selected = localSelected.contains(option.first),
                        onClick = {
                            localSelected = if (localSelected.contains(option.first)) {
                                localSelected - option.first
                            } else {
                                localSelected + option.first
                            }
                        },
                        label = { Text(option.second) }
                    )
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(localSelected) }) { Text("确定") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}
