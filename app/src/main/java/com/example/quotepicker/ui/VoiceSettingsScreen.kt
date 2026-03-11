package com.example.quotepicker.ui

import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.quotepicker.util.PiperSpeechEngine
import com.example.quotepicker.util.RoleVoiceSetting
import com.example.quotepicker.util.VoiceProfile
import com.example.quotepicker.vm.ResourceViewModel
import com.example.quotepicker.vm.VoiceSettingsViewModel
import java.util.Locale
import kotlinx.coroutines.launch

private const val BUILTIN_MODEL_URI = "asset://tts/vits-zh-hf-fanchen-C.onnx"

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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("声音设置（内置 vits-zh-hf-fanchen-C）") },
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
                Text("只维护一套通用配置；保存时可绑定到任意角色的“声音配置”文本资源。")
            }
            item {
                VoiceSettingEditor(
                    title = "通用配置",
                    baseProfile = builtInProfile,
                    initial = current,
                    initialPreviewText = ui.settings.rolePreviewTexts[VoiceSettingsViewModel.DEFAULT_ROLE_KEY],
                    allTags = resourceUi.tags,
                    allCharacters = resourceUi.characters,
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
                    onSaveAsText = { title, content, tagIds, characterIds ->
                        resourceVm.addTextResource(title, content, tagIds, characterIds)
                    },
                    onSaveAsSound = { title, uri, tagIds, characterIds ->
                        resourceVm.addSoundGroup(title, listOf(uri), tagIds, characterIds)
                    },
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
    allTags: List<com.example.quotepicker.data.TagEntity>,
    allCharacters: List<com.example.quotepicker.data.CharacterEntity>,
    onSave: (RoleVoiceSetting) -> Unit,
    onPersistPreviewText: (String) -> Unit,
    onSaveAsText: (String, String, List<Long>, List<Long>) -> Unit,
    onSaveAsSound: (String, Uri, List<Long>, List<Long>) -> Unit,
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
    var showSaveTextDialog by remember { mutableStateOf(false) }
    var showSaveSoundDialog by remember { mutableStateOf(false) }
    var tempAudioUri by remember { mutableStateOf<Uri?>(null) }

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

        Text("speakerId")
        Row {
            Text("$speakerId")
            Slider(value = speakerId.toFloat(), onValueChange = { speakerId = it.toInt(); saveCurrent() }, valueRange = 0f..186f, modifier = Modifier.weight(1f))
        }

        Text("speed")
        Row {
            Text("${"%.2f".format(Locale.US, speed)}")
            Slider(value = speed, onValueChange = { speed = it; saveCurrent() }, valueRange = 0.6f..1.8f, modifier = Modifier.weight(1f))
        }

        Text("noiseScale")
        Row {
            Text("${"%.3f".format(Locale.US, noiseScale)}")
            Slider(value = noiseScale, onValueChange = { noiseScale = it; saveCurrent() }, valueRange = 0.1f..2.0f, modifier = Modifier.weight(1f))
        }

        Text("noiseScaleW")
        Row {
            Text("${"%.3f".format(Locale.US, noiseScaleW)}")
            Slider(value = noiseScaleW, onValueChange = { noiseScaleW = it; saveCurrent() }, valueRange = 0.1f..2.0f, modifier = Modifier.weight(1f))
        }

        Text("lengthScale")
        Row {
            Text("${"%.2f".format(Locale.US, lengthScale)}")
            Slider(value = lengthScale, onValueChange = { lengthScale = it; saveCurrent() }, valueRange = 0.5f..2.0f, modifier = Modifier.weight(1f))
        }

        Text("maxNumSentences")
        Row {
            Text("$maxNumSentences")
            Slider(value = maxNumSentences.toFloat(), onValueChange = { maxNumSentences = it.toInt().coerceIn(1, 10); saveCurrent() }, valueRange = 1f..10f, modifier = Modifier.weight(1f))
        }

        Text("silenceScale")
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
            Button(onClick = { showSaveTextDialog = true }) { Text("保存") }
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
                        tempAudioUri = Uri.fromFile(file)
                        showSaveSoundDialog = true
                        statusText = "已生成临时声音文件"
                    } else {
                        statusText = "生成失败"
                    }
                }
            }) { Text("留声") }
        }

        if (statusText.isNotBlank()) Text(statusText)

        if (showSaveTextDialog) {
            SaveTextConfigDialog(
                initialContent = buildConfigText(),
                tags = allTags,
                characters = allCharacters,
                onDismiss = { showSaveTextDialog = false },
                onConfirm = { saveTitle, content, tagIds, characterIds ->
                    onSaveAsText(saveTitle, content, tagIds, characterIds)
                    showSaveTextDialog = false
                    statusText = "已创建文本资源"
                }
            )
        }

        if (showSaveSoundDialog) {
            SaveSoundDialog(
                tempUri = tempAudioUri,
                tags = allTags,
                characters = allCharacters,
                onDismiss = {
                    tempAudioUri?.path?.let { java.io.File(it).delete() }
                    tempAudioUri = null
                    showSaveSoundDialog = false
                },
                onConfirm = { saveTitle, uri, tagIds, characterIds ->
                    onSaveAsSound(saveTitle, uri, tagIds, characterIds)
                    uri.path?.let { java.io.File(it).delete() }
                    tempAudioUri = null
                    showSaveSoundDialog = false
                    statusText = "已保存声音资源"
                }
            )
        }
    }
}

@Composable
private fun SaveTextConfigDialog(
    initialContent: String,
    tags: List<com.example.quotepicker.data.TagEntity>,
    characters: List<com.example.quotepicker.data.CharacterEntity>,
    onDismiss: () -> Unit,
    onConfirm: (String, String, List<Long>, List<Long>) -> Unit
) {
    var title by remember { mutableStateOf("声音配置") }
    var content by remember(initialContent) { mutableStateOf(initialContent) }
    var tagInput by remember { mutableStateOf("") }
    var characterInput by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("保存声音配置文本") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("文本名") })
                OutlinedTextField(value = content, onValueChange = { content = it }, label = { Text("文本内容") })
                OutlinedTextField(value = tagInput, onValueChange = { tagInput = it }, label = { Text("标签(逗号分隔)") })
                OutlinedTextField(value = characterInput, onValueChange = { characterInput = it }, label = { Text("角色名(逗号分隔)") })
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val tagNames = tagInput.split(',').map { it.trim() }
                val roleNames = characterInput.split(',').map { it.trim() }
                val selectedTagIds = tags.filter { t -> t.name in tagNames }.map { it.id }
                val selectedCharacterIds = characters.filter { c -> c.name in roleNames }.map { it.id }
                onConfirm(title.trim().ifBlank { "声音配置" }, content, selectedTagIds, selectedCharacterIds)
            }) { Text("保存") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

@Composable
private fun SaveSoundDialog(
    tempUri: Uri?,
    tags: List<com.example.quotepicker.data.TagEntity>,
    characters: List<com.example.quotepicker.data.CharacterEntity>,
    onDismiss: () -> Unit,
    onConfirm: (String, Uri, List<Long>, List<Long>) -> Unit
) {
    var title by remember { mutableStateOf("留声") }
    var tagInput by remember { mutableStateOf("") }
    var characterInput by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("保存声音资源") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("声音资源名") })
                Text("文件：${tempUri?.lastPathSegment ?: "未生成"}")
                OutlinedTextField(value = tagInput, onValueChange = { tagInput = it }, label = { Text("标签(逗号分隔)") })
                OutlinedTextField(value = characterInput, onValueChange = { characterInput = it }, label = { Text("角色名(逗号分隔)") })
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val uri = tempUri ?: return@TextButton
                val tagNames = tagInput.split(',').map { it.trim() }
                val roleNames = characterInput.split(',').map { it.trim() }
                val selectedTagIds = tags.filter { t -> t.name in tagNames }.map { it.id }
                val selectedCharacterIds = characters.filter { c -> c.name in roleNames }.map { it.id }
                onConfirm(title.trim(), uri, selectedTagIds, selectedCharacterIds)
            }) { Text("保存") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}
