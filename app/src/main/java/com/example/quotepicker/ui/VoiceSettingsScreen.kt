package com.example.quotepicker.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.input.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.quotepicker.util.PiperSpeechEngine
import com.example.quotepicker.util.RoleVoiceSetting
import com.example.quotepicker.util.VoiceProfile
import com.example.quotepicker.vm.VoiceSettingsViewModel
import kotlinx.coroutines.launch

private const val BUILTIN_MODEL_URI = "asset://tts/vits-zh-hf-fanchen-C.onnx"

@Composable
fun VoiceSettingsScreen(
    onBack: () -> Unit,
    vm: VoiceSettingsViewModel = viewModel()
) {
    val ui by vm.uiState.collectAsState()
    val builtInProfile = ui.settings.profiles.firstOrNull { it.modelUri == BUILTIN_MODEL_URI }

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
                Text("可为每个角色单独设置 speaker 和参数，并可直接预览。")
            }
            item {
                RoleVoiceSettingRow(
                    roleName = ui.narratorName,
                    baseProfile = builtInProfile,
                    initial = ui.settings.roleSettings.firstOrNull { it.roleName == ui.narratorName },
                    onSave = { vm.updateRoleSetting(ui.narratorName, builtInProfile?.id, it.speechRate, it.speakerId, it.noiseScale, it.noiseW, it.sentenceSilence) }
                )
            }
            items(ui.characterNames, key = { it }) { roleName ->
                RoleVoiceSettingRow(
                    roleName = roleName,
                    baseProfile = builtInProfile,
                    initial = ui.settings.roleSettings.firstOrNull { it.roleName == roleName },
                    onSave = { vm.updateRoleSetting(roleName, builtInProfile?.id, it.speechRate, it.speakerId, it.noiseScale, it.noiseW, it.sentenceSilence) }
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
    onSave: (RoleVoiceSetting) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val speech = remember(context) { PiperSpeechEngine(context) }

    var speed by remember(roleName, initial?.speechRate) { mutableStateOf(initial?.speechRate ?: 1.0f) }
    var speakerText by remember(roleName, initial?.speakerId) { mutableStateOf(initial?.speakerId?.toString() ?: "") }
    var noiseScaleText by remember(roleName, initial?.noiseScale) { mutableStateOf(initial?.noiseScale?.toString() ?: "") }
    var noiseWText by remember(roleName, initial?.noiseW) { mutableStateOf(initial?.noiseW?.toString() ?: "") }
    var sentenceSilenceText by remember(roleName, initial?.sentenceSilence) { mutableStateOf(initial?.sentenceSilence?.toString() ?: "") }
    var previewText by remember(roleName) { mutableStateOf("你好，我是$roleName，这是一段语音预览。") }

    fun currentSetting() = RoleVoiceSetting(
        roleName = roleName,
        speechRate = speed,
        speakerId = speakerText.toIntOrNull(),
        noiseScale = noiseScaleText.toFloatOrNull(),
        noiseW = noiseWText.toFloatOrNull(),
        sentenceSilence = sentenceSilenceText.toFloatOrNull()
    )

    fun saveCurrent() {
        onSave(currentSetting())
    }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(roleName)
        Row {
            Text("语速 ${"%.2f".format(speed)}")
            Spacer(modifier = Modifier.width(8.dp))
            Slider(
                value = speed,
                onValueChange = {
                    speed = it
                    saveCurrent()
                },
                valueRange = 0.6f..1.8f,
                modifier = Modifier.weight(1f)
            )
        }

        OutlinedTextField(
            value = speakerText,
            onValueChange = {
                speakerText = it
                saveCurrent()
            },
            label = { Text("speaker（可选）") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = noiseScaleText,
            onValueChange = {
                noiseScaleText = it
                saveCurrent()
            },
            label = { Text("noise_scale（可选）") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = noiseWText,
            onValueChange = {
                noiseWText = it
                saveCurrent()
            },
            label = { Text("noise_w（可选）") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = sentenceSilenceText,
            onValueChange = {
                sentenceSilenceText = it
                saveCurrent()
            },
            label = { Text("sentence_silence（可选）") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = previewText,
            onValueChange = { previewText = it },
            label = { Text("预览文本（可修改）") },
            modifier = Modifier.fillMaxWidth()
        )
        Button(
            onClick = {
                val base = baseProfile ?: return@Button
                val setting = currentSetting()
                saveCurrent()
                val effective = base.copy(
                    speakerId = setting.speakerId,
                    noiseScale = setting.noiseScale,
                    noiseW = setting.noiseW,
                    sentenceSilence = setting.sentenceSilence
                )
                scope.launch {
                    speech.speak(previewText, effective, setting.speechRate)
                }
            },
            enabled = baseProfile != null
        ) {
            Text("预览朗读")
        }
    }
}
