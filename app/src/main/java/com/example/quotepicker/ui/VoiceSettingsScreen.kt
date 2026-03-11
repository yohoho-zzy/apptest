package com.example.quotepicker.ui

import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.example.quotepicker.vm.VoiceSettingsViewModel
import java.io.File
import java.util.Locale
import kotlinx.coroutines.launch
import org.json.JSONObject

private const val BUILTIN_MODEL_URI = "asset://tts/vits-zh-hf-fanchen-C.onnx"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceSettingsScreen(
    onBack: () -> Unit,
    vm: VoiceSettingsViewModel = viewModel()
) {
    val ui by vm.uiState.collectAsState()
    val builtInProfile = ui.settings.profiles.firstOrNull { it.modelUri == BUILTIN_MODEL_URI }
    val allRoles = remember(ui.narratorName, ui.noticeName, ui.characterNames) {
        (listOf(ui.narratorName, ui.noticeName) + ui.characterNames).distinct()
    }

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
                Text("可为每个角色单独设置 speaker 和参数，并可直接预览/保存。")
            }

            items(allRoles, key = { it }) { roleName ->
                val current = ui.settings.roleSettings.firstOrNull { it.roleName == roleName }
                RoleVoiceSettingRow(
                    roleName = roleName,
                    baseProfile = builtInProfile,
                    initial = current,
                    onSave = {
                        vm.updateRoleSetting(
                            roleName,
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
                    onSaveAsText = { payload -> vm.saveRoleConfigAsText(roleName, payload) },
                    onSaveAsSound = { uri -> vm.savePreviewAsSound(roleName, uri) }
                )
            }
        }
    }
}

@Composable
private fun RoleVoiceSettingRow(
    roleName: String,
    baseProfile: VoiceProfile?,
    initial: RoleVoiceSetting?,
    onSave: (RoleVoiceSetting) -> Unit,
    onSaveAsText: (String) -> Unit,
    onSaveAsSound: (Uri) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val speech = remember(context) { PiperSpeechEngine(context) }

    var speed by remember(roleName, initial?.speechRate) { mutableStateOf((initial?.speechRate ?: 1.0f).coerceIn(0.6f, 1.8f)) }
    var speakerId by remember(roleName, initial?.speakerId) { mutableStateOf((initial?.speakerId ?: 0).coerceIn(0, 186)) }
    var noiseScale by remember(roleName, initial?.noiseScale) { mutableStateOf((initial?.noiseScale ?: 0.667f).coerceIn(0.1f, 2.0f)) }
    var noiseScaleW by remember(roleName, initial?.noiseScaleW) { mutableStateOf((initial?.noiseScaleW ?: 0.8f).coerceIn(0.1f, 2.0f)) }
    var lengthScale by remember(roleName, initial?.lengthScale) { mutableStateOf((initial?.lengthScale ?: 1.0f).coerceIn(0.5f, 2.0f)) }
    var maxNumSentences by remember(roleName, initial?.maxNumSentences) { mutableStateOf((initial?.maxNumSentences ?: 1).coerceIn(1, 10)) }
    var silenceScale by remember(roleName, initial?.silenceScale) { mutableStateOf((initial?.silenceScale ?: 0.2f).coerceIn(0f, 1f)) }
    var previewText by remember(roleName) { mutableStateOf("你好，我是$roleName，这是一段语音预览。") }
    var statusText by remember(roleName) { mutableStateOf("") }
    var showSaveDialog by remember(roleName) { mutableStateOf(false) }

    fun currentSetting() = RoleVoiceSetting(
        roleName = roleName,
        speechRate = speed,
        speakerId = speakerId,
        noiseScale = noiseScale,
        noiseScaleW = noiseScaleW,
        lengthScale = lengthScale,
        maxNumSentences = maxNumSentences,
        silenceScale = silenceScale
    )

    fun effectiveProfile(setting: RoleVoiceSetting): VoiceProfile? = baseProfile?.copy(
        speakerId = setting.speakerId,
        noiseScale = setting.noiseScale,
        noiseScaleW = setting.noiseScaleW,
        lengthScale = setting.lengthScale,
        maxNumSentences = setting.maxNumSentences,
        silenceScale = setting.silenceScale
    )

    fun voiceConfigPayload(setting: RoleVoiceSetting): String = JSONObject()
        .put("kind", "voice_config")
        .put("roleName", roleName)
        .put("speechRate", setting.speechRate)
        .put("speakerId", setting.speakerId)
        .put("noiseScale", setting.noiseScale)
        .put("noiseScaleW", setting.noiseScaleW)
        .put("lengthScale", setting.lengthScale)
        .put("maxNumSentences", setting.maxNumSentences)
        .put("silenceScale", setting.silenceScale)
        .put("previewText", previewText)
        .toString()

    fun saveCurrent() = onSave(currentSetting())

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("$roleName（每项都限制在安全区间）")

        Text("speakerId: 选择音色说话人。推荐 0（可尝试不同角色分配不同 speaker）。")
        Row {
            Text("$speakerId")
            Slider(value = speakerId.toFloat(), onValueChange = { speakerId = it.toInt(); saveCurrent() }, valueRange = 0f..186f, modifier = Modifier.weight(1f))
        }

        Text("speed: 语速。推荐 1.0。")
        Row {
            Text("${"%.2f".format(Locale.US, speed)}")
            Slider(value = speed, onValueChange = { speed = it; saveCurrent() }, valueRange = 0.6f..1.8f, modifier = Modifier.weight(1f))
        }

        Text("noiseScale: 发音随机度，越高越活泼。推荐 0.667。")
        Row {
            Text("${"%.3f".format(Locale.US, noiseScale)}")
            Slider(value = noiseScale, onValueChange = { noiseScale = it; saveCurrent() }, valueRange = 0.1f..2.0f, modifier = Modifier.weight(1f))
        }

        Text("noiseScaleW: 音素时长随机度。推荐 0.8。")
        Row {
            Text("${"%.3f".format(Locale.US, noiseScaleW)}")
            Slider(value = noiseScaleW, onValueChange = { noiseScaleW = it; saveCurrent() }, valueRange = 0.1f..2.0f, modifier = Modifier.weight(1f))
        }

        Text("lengthScale: 句子整体时长缩放，越大越慢。推荐 1.0。")
        Row {
            Text("${"%.2f".format(Locale.US, lengthScale)}")
            Slider(value = lengthScale, onValueChange = { lengthScale = it; saveCurrent() }, valueRange = 0.5f..2.0f, modifier = Modifier.weight(1f))
        }

        Text("maxNumSentences: 单次切句上限。推荐 1。")
        Row {
            Text("$maxNumSentences")
            Slider(value = maxNumSentences.toFloat(), onValueChange = { maxNumSentences = it.toInt().coerceIn(1, 10); saveCurrent() }, valueRange = 1f..10f, modifier = Modifier.weight(1f))
        }

        Text("silenceScale: 句间停顿缩放。推荐 0.2。")
        Row {
            Text("${"%.2f".format(Locale.US, silenceScale)}")
            Slider(value = silenceScale, onValueChange = { silenceScale = it; saveCurrent() }, valueRange = 0f..1f, modifier = Modifier.weight(1f))
        }

        OutlinedTextField(
            value = previewText,
            onValueChange = { previewText = it },
            label = { Text("预览文本（可修改）") },
            modifier = Modifier.fillMaxWidth()
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = {
                    val setting = currentSetting()
                    saveCurrent()
                    val effective = effectiveProfile(setting) ?: return@Button
                    scope.launch {
                        try {
                            statusText = "正在初始化/朗读..."
                            speech.speak(previewText, effective, setting.speechRate)
                            statusText = "朗读中"
                        } catch (e: Throwable) {
                            statusText = "失败：${e.message ?: e.javaClass.simpleName}"
                            android.util.Log.e("VoiceSettings", "preview failed", e)
                        }
                    }
                },
                enabled = baseProfile != null
            ) { Text("预览朗读") }

            Button(onClick = { scope.launch { speech.stop(); statusText = "已停止" } }) { Text("停止") }
            Button(onClick = { showSaveDialog = true }, enabled = baseProfile != null && previewText.isNotBlank()) { Text("保存") }
        }

        if (statusText.isNotBlank()) Text(statusText)
    }

    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = { Text("保存方式") },
            text = { Text("选择作为声音或作为文本保存") },
            confirmButton = {
                TextButton(onClick = {
                    val setting = currentSetting()
                    saveCurrent()
                    onSaveAsText(voiceConfigPayload(setting))
                    statusText = "已保存为文本配置"
                    showSaveDialog = false
                }) { Text("作为文本") }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = {
                        val setting = currentSetting()
                        saveCurrent()
                        val effective = effectiveProfile(setting) ?: return@TextButton
                        scope.launch {
                            val out = File(context.filesDir, "audio/voice_config_${roleName}_${System.currentTimeMillis()}.wav")
                            val ok = speech.synthesizeToWav(previewText, effective, setting.speechRate, out)
                            if (ok) {
                                onSaveAsSound(Uri.fromFile(out))
                                statusText = "已保存为声音"
                            } else {
                                statusText = "保存声音失败"
                            }
                        }
                        showSaveDialog = false
                    }) { Text("作为声音") }
                    TextButton(onClick = { showSaveDialog = false }) { Text("取消") }
                }
            }
        )
    }
}
