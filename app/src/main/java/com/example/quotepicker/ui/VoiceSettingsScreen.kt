package com.example.quotepicker.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.quotepicker.data.CharacterEntity
import com.example.quotepicker.data.TagCategoryEntity
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
            categories = resourceUi.categories,
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

    DisposableEffect(speech) {
        onDispose {
            scope.launch {
                speech.stop()
                speech.cleanupPreviewTempFiles()
                speech.release()
            }
        }
    }

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

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(20.dp))
                Column {
                    Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text("每项都限制在安全区间", style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        SliderSettingRow(
            label = "说话者",
            hint = "speakerId · 0-186",
            valueText = "$speakerId"
        ) {
            Slider(value = speakerId.toFloat(), onValueChange = { speakerId = it.toInt(); saveCurrent() }, valueRange = 0f..186f, modifier = Modifier.fillMaxWidth())
        }

        SliderSettingRow(
            label = "语速",
            hint = "speed · 0.6-1.8",
            valueText = "${"%.2f".format(Locale.US, speed)}"
        ) {
            Slider(value = speed, onValueChange = { speed = it; saveCurrent() }, valueRange = 0.6f..1.8f, modifier = Modifier.fillMaxWidth())
        }

        SliderSettingRow(
            label = "情绪随机度",
            hint = "noiseScale · 0.1-2.0",
            valueText = "${"%.3f".format(Locale.US, noiseScale)}"
        ) {
            Slider(value = noiseScale, onValueChange = { noiseScale = it; saveCurrent() }, valueRange = 0.1f..2.0f, modifier = Modifier.fillMaxWidth())
        }

        SliderSettingRow(
            label = "音素随机间隔",
            hint = "noiseScaleW · 0.1-2.0",
            valueText = "${"%.3f".format(Locale.US, noiseScaleW)}"
        ) {
            Slider(value = noiseScaleW, onValueChange = { noiseScaleW = it; saveCurrent() }, valueRange = 0.1f..2.0f, modifier = Modifier.fillMaxWidth())
        }

        SliderSettingRow(
            label = "句子时长",
            hint = "lengthScale · 0.5-2.0",
            valueText = "${"%.2f".format(Locale.US, lengthScale)}"
        ) {
            Slider(value = lengthScale, onValueChange = { lengthScale = it; saveCurrent() }, valueRange = 0.5f..2.0f, modifier = Modifier.fillMaxWidth())
        }

        SliderSettingRow(
            label = "句子处理量",
            hint = "maxNumSentences · 1-10",
            valueText = "$maxNumSentences"
        ) {
            Slider(value = maxNumSentences.toFloat(), onValueChange = { maxNumSentences = it.toInt().coerceIn(1, 10); saveCurrent() }, valueRange = 1f..10f, modifier = Modifier.fillMaxWidth())
        }

        SliderSettingRow(
            label = "句子间隔",
            hint = "silenceScale · 0-1",
            valueText = "${"%.2f".format(Locale.US, silenceScale)}"
        ) {
            Slider(value = silenceScale, onValueChange = { silenceScale = it; saveCurrent() }, valueRange = 0f..1f, modifier = Modifier.fillMaxWidth())
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

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
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
                enabled = baseProfile != null,
                modifier = Modifier.weight(1f)
            ) { Text("预览朗读") }

            Button(onClick = { scope.launch { speech.stop(); statusText = "已停止" } }, modifier = Modifier.weight(1f)) { Text("停止") }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            Button(onClick = { onOpenSaveTextPage(buildConfigText()) }, modifier = Modifier.weight(1f)) { Text("保存文本") }
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
            }, modifier = Modifier.weight(1f)) { Text("导出声音") }
        }

        if (statusText.isNotBlank()) {
            Text(
                statusText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }

    }
}

@Composable
private fun SliderSettingRow(
    label: String,
    hint: String,
    valueText: String,
    slider: @Composable () -> Unit
) {
    Card {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(label, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium)
                    Text(hint, style = MaterialTheme.typography.bodySmall)
                }
                Text(valueText, style = MaterialTheme.typography.titleSmall)
            }
            Divider()
            slider()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VoiceResourceSaveScreen(
    mode: VoiceSaveMode,
    categories: List<TagCategoryEntity>,
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

            ResourceTagPickerRow(
                label = "标签",
                allTags = tags,
                selected = selectedTagIds,
                onPick = { showTagPicker = true }
            )
            ResourceCharacterPickerRow(
                label = "角色",
                characters = characters,
                selected = selectedCharacterIds,
                onPick = { showCharacterPicker = true }
            )
        }
    }

    if (showTagPicker) {
        TagPickerDialog(
            categories = categories,
            tags = tags,
            selectedIds = selectedTagIds,
            onConfirm = { selectedTagIds = it },
            onDismiss = { showTagPicker = false }
        )
    }

    if (showCharacterPicker) {
        CharacterPickerDialog(
            characters = characters,
            selectedIds = selectedCharacterIds,
            onConfirm = { selectedCharacterIds = it },
            onDismiss = { showCharacterPicker = false }
        )
    }
}
