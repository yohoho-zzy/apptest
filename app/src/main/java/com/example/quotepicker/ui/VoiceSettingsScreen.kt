package com.example.quotepicker.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AssistChip
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenu
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.quotepicker.util.RoleVoiceSetting
import com.example.quotepicker.util.VoiceProfile
import com.example.quotepicker.vm.VoiceSettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceSettingsScreen(
    onBack: () -> Unit,
    vm: VoiceSettingsViewModel = viewModel()
) {
    val ui by vm.uiState.collectAsState()
    var newProfileName by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("声音设置（Piper + ONNX，仅中文）") },
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
                Text("音色库")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = newProfileName,
                        onValueChange = { newProfileName = it },
                        modifier = Modifier.weight(1f),
                        label = { Text("新增音色名称") },
                        singleLine = true
                    )
                    AssistChip(
                        onClick = {
                            vm.addProfile(newProfileName)
                            newProfileName = ""
                        },
                        label = { Text("添加") }
                    )
                }
            }
            items(ui.settings.profiles, key = { it.id }) { profile ->
                VoiceProfileEditor(
                    profile = profile,
                    onUpdate = vm::updateProfile,
                    onDelete = { vm.removeProfile(profile.id) }
                )
            }

            item { Text("角色绑定（旁白也按角色处理）") }
            item {
                RoleVoiceSettingRow(
                    roleName = ui.narratorName,
                    allProfiles = ui.settings.profiles,
                    initial = ui.settings.roleSettings.firstOrNull { it.roleName == ui.narratorName },
                    onSave = { vm.updateRoleSetting(ui.narratorName, it.profileId, it.speechRate) }
                )
            }
            items(ui.characterNames, key = { it }) { roleName ->
                RoleVoiceSettingRow(
                    roleName = roleName,
                    allProfiles = ui.settings.profiles,
                    initial = ui.settings.roleSettings.firstOrNull { it.roleName == roleName },
                    onSave = { vm.updateRoleSetting(roleName, it.profileId, it.speechRate) }
                )
            }
            item {
                Text("ONNX 必填；JSON 配置可选（vits-zh-hf-fanchen-C.onnx 可不提供 json）。")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RoleVoiceSettingRow(
    roleName: String,
    allProfiles: List<VoiceProfile>,
    initial: RoleVoiceSetting?,
    onSave: (RoleVoiceSetting) -> Unit
) {
    var expanded by remember(roleName, allProfiles) { mutableStateOf(false) }
    var profileId by remember(roleName, initial?.profileId) { mutableStateOf(initial?.profileId) }
    var speed by remember(roleName, initial?.speechRate) { mutableStateOf(initial?.speechRate ?: 1.0f) }
    Column {
        Text(roleName)
        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
            OutlinedTextField(
                value = allProfiles.firstOrNull { it.id == profileId }?.name ?: "未设置",
                onValueChange = {},
                readOnly = true,
                label = { Text("音色") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor()
            )
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                DropdownMenuItem(text = { Text("未设置") }, onClick = {
                    profileId = null
                    expanded = false
                    onSave(RoleVoiceSetting(roleName = roleName, profileId = null, speechRate = speed))
                })
                allProfiles.forEach { profile ->
                    DropdownMenuItem(text = { Text(profile.name) }, onClick = {
                        profileId = profile.id
                        expanded = false
                        onSave(RoleVoiceSetting(roleName = roleName, profileId = profile.id, speechRate = speed))
                    })
                }
            }
        }
        Row {
            Text("语速 ${"%.2f".format(speed)}")
            Spacer(modifier = Modifier.width(8.dp))
            Slider(
                value = speed,
                onValueChange = {
                    speed = it
                    onSave(RoleVoiceSetting(roleName = roleName, profileId = profileId, speechRate = it))
                },
                valueRange = 0.6f..1.8f,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun VoiceProfileEditor(
    profile: VoiceProfile,
    onUpdate: (VoiceProfile) -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    var speakerText by remember(profile.id, profile.speakerId) { mutableStateOf(profile.speakerId?.toString() ?: "") }
    var noiseScaleText by remember(profile.id, profile.noiseScale) { mutableStateOf(profile.noiseScale?.toString() ?: "") }
    var noiseWText by remember(profile.id, profile.noiseW) { mutableStateOf(profile.noiseW?.toString() ?: "") }
    var sentenceSilenceText by remember(profile.id, profile.sentenceSilence) { mutableStateOf(profile.sentenceSilence?.toString() ?: "") }

    val modelLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        uri?.let {
            context.contentResolver.takePersistableUriPermission(it, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            onUpdate(profile.copy(modelUri = it.toString()))
        }
    }
    val configLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        uri?.let {
            context.contentResolver.takePersistableUriPermission(it, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            onUpdate(profile.copy(configUri = it.toString()))
        }
    }

    fun saveAdvanced() {
        onUpdate(
            profile.copy(
                speakerId = speakerText.toIntOrNull(),
                noiseScale = noiseScaleText.toFloatOrNull(),
                noiseW = noiseWText.toFloatOrNull(),
                sentenceSilence = sentenceSilenceText.toFloatOrNull()
            )
        )
    }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(profile.name)
            IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, contentDescription = "删除音色") }
        }
        AssistChip(onClick = { modelLauncher.launch(arrayOf("*/*")) }, label = { Text("导入ONNX模型（必填）") })
        AssistChip(onClick = { configLauncher.launch(arrayOf("application/json", "text/plain", "*/*")) }, label = { Text("导入配置JSON（可选）") })
        Text("模型: ${profile.modelUri.ifBlank { "未导入" }}")
        Text("配置: ${profile.configUri.ifBlank { "未导入（将使用模型默认参数）" }}")

        OutlinedTextField(
            value = speakerText,
            onValueChange = {
                speakerText = it
                saveAdvanced()
            },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Speaker ID（可选）") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )
        OutlinedTextField(
            value = noiseScaleText,
            onValueChange = {
                noiseScaleText = it
                saveAdvanced()
            },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("noise_scale（可选）") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
        )
        OutlinedTextField(
            value = noiseWText,
            onValueChange = {
                noiseWText = it
                saveAdvanced()
            },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("noise_w（可选）") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
        )
        OutlinedTextField(
            value = sentenceSilenceText,
            onValueChange = {
                sentenceSilenceText = it
                saveAdvanced()
            },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("sentence_silence（可选）") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
        )
    }
}
