package com.example.quotepicker.ui

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.quotepicker.data.ExecutionSettingsEntity
import com.example.quotepicker.data.ResourceWithTagsCharacters
import com.example.quotepicker.ui.components.ResourceListRow
import com.example.quotepicker.ui.components.ResourcePreviewScreen
import com.example.quotepicker.vm.ExecutionViewModel
import com.example.quotepicker.vm.ResourceViewModel
import java.time.LocalDate
import kotlin.math.roundToInt

private data class ExecutionResourceItem(
    val resource: ResourceWithTagsCharacters,
    val characterId: Long,
    val characterName: String,
    val tagName: String
)

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ExecutionScreen(
    modifier: Modifier = Modifier,
    vm: ExecutionViewModel = viewModel(),
    resourceVm: ResourceViewModel = viewModel()
) {
    val ui by vm.uiState.collectAsState()
    var showSettings by remember { mutableStateOf(false) }
    var showPreview by remember { mutableStateOf(false) }
    var previewTarget by remember { mutableStateOf<ResourceWithTagsCharacters?>(null) }
    var executionItems by remember { mutableStateOf<List<ExecutionResourceItem>>(emptyList()) }
    var completionTarget by remember { mutableStateOf<ExecutionResourceItem?>(null) }
    var showDailyInputDialog by rememberSaveable { mutableStateOf(false) }
    val context = LocalContext.current
    val today = LocalDate.now().toString()
    val shouldPromptDailyInput = ui.settings.lastExecutionDate != today
    val isExecutionAvailable = ui.settings.remainingValue > 0
    val executionSlotsAvailable = executionItems.size < 5

    LaunchedEffect(shouldPromptDailyInput) {
        showDailyInputDialog = shouldPromptDailyInput
    }

    if (showPreview && previewTarget != null) {
        ResourcePreviewScreen(
            resource = previewTarget!!,
            vm = resourceVm,
            onBack = {
                showPreview = false
                previewTarget = null
            }
        )
        return
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("执行") },
                actions = {
                    IconButton(onClick = { showSettings = true }) {
                        Icon(Icons.Default.Settings, contentDescription = "设置")
                    }
                }
            )
        }
    ) { inner ->
        Column(
            modifier = Modifier
                .padding(inner)
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = ui.settings.pastAverage.toString(),
                        style = MaterialTheme.typography.titleMedium,
                        color = Color(0xFF1976D2)
                    )
                    Text(
                        text = ui.settings.lastInputValue.toString(),
                        style = MaterialTheme.typography.titleMedium,
                        color = Color(0xFFD32F2F)
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = ui.settings.dailyAverage.toString(),
                            style = MaterialTheme.typography.titleMedium,
                            color = Color(0xFF2E7D32)
                        )
                        Text(
                            text = "(${ui.settings.remainingValue})",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color(0xFF7B4DFF)
                        )
                    }
                }
                if (!isExecutionAvailable) {
                    Text("祈求不可用", color = MaterialTheme.colorScheme.error)
                }
                Text("响应记录", style = MaterialTheme.typography.titleMedium)
                if (ui.records.isEmpty()) {
                    Text("暂无响应记录", style = MaterialTheme.typography.labelMedium)
                } else {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ui.records.forEach { record ->
                            val label = buildString {
                                append(record.characterName)
                                append("的")
                                append(record.tagName)
                                if (record.count > 1) {
                                    append("×")
                                    append(record.count)
                                }
                            }
                            Button(onClick = {
                                if (shouldPromptDailyInput) {
                                    Toast.makeText(context, "请先输入前日数值", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                if (!isExecutionAvailable) {
                                    Toast.makeText(context, "祈求不可用", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                if (!executionSlotsAvailable) {
                                    Toast.makeText(context, "执行资源最多保留5个，请先完成", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                vm.consumeRecord(record.characterId, record.tagId)
                                val candidates = ui.resources.filter { res ->
                                    res.characters.any { it.id == record.characterId } &&
                                        res.tags.any { it.id == record.tagId }
                                }
                                val picked = candidates.randomOrNull()
                                if (picked == null) {
                                    Toast.makeText(context, "未找到匹配资源", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                executionItems = executionItems + ExecutionResourceItem(
                                    resource = picked,
                                    characterId = record.characterId,
                                    characterName = record.characterName,
                                    tagName = record.tagName
                                )
                            },
                                enabled = isExecutionAvailable && !shouldPromptDailyInput && executionSlotsAvailable,
                                shape = MaterialTheme.shapes.small
                            ) {
                                Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.weight(1f, fill = false)) {
                Text("执行资源", style = MaterialTheme.typography.titleMedium)
                if (executionItems.isEmpty()) {
                    Text("暂无执行资源", style = MaterialTheme.typography.labelMedium)
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(executionItems) { item ->
                            ResourceListRow(
                                resource = item.resource,
                                categories = emptyList(),
                                roleText = "${item.characterName} ${item.tagName}",
                                onClick = {
                                    previewTarget = item.resource
                                    showPreview = true
                                },
                                onLongClick = { completionTarget = item }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showSettings) {
        SettingsDialog(
            settings = ui.settings,
            onConfirm = { vm.updateSettings(it); showSettings = false },
            onDismiss = { showSettings = false }
        )
    }

    if (showDailyInputDialog) {
        DailyInputDialog(
            onConfirm = { vm.applyDailyInput(it) }
        )
    }

    completionTarget?.let { target ->
        CompletionDialog(
            characterName = target.characterName,
            onConfirm = { completion, belonging, emotion ->
                val sum = completion + belonging + emotion
                val currentPoints = ui.characters.firstOrNull { it.id == target.characterId }?.points ?: 0
                val newPoints = ((sum + currentPoints) / 2.0).roundToInt()
                vm.updateCharacterPoints(target.characterId, newPoints)
                vm.incrementCharacterFamiliarity(target.characterId)
                executionItems = executionItems.filterNot { it == target }
                completionTarget = null
            },
            onDismiss = { completionTarget = null }
        )
    }
}

@Composable
private fun SettingsDialog(
    settings: ExecutionSettingsEntity,
    onConfirm: (ExecutionSettingsEntity) -> Unit,
    onDismiss: () -> Unit
) {
    var buttonLabel by remember { mutableStateOf(settings.buttonLabel) }
    var successToast by remember { mutableStateOf(settings.successToast) }
    var failureToast by remember { mutableStateOf(settings.failureToast) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("设置") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = buttonLabel,
                    onValueChange = { buttonLabel = it },
                    label = { Text("按钮名") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = successToast,
                    onValueChange = { successToast = it },
                    label = { Text("响应 toast") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = failureToast,
                    onValueChange = { failureToast = it },
                    label = { Text("非响应 toast") },
                    modifier = Modifier.fillMaxWidth()
                )
                Text("提示：用 [] 代表角色或标签占位", style = MaterialTheme.typography.labelSmall)
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onConfirm(
                    settings.copy(
                        buttonLabel = buttonLabel.trim().ifBlank { settings.buttonLabel },
                        successToast = successToast.trim().ifBlank { settings.successToast },
                        failureToast = failureToast.trim().ifBlank { settings.failureToast }
                    )
                )
            }) { Text("确定") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

@Composable
private fun CompletionDialog(
    characterName: String,
    onConfirm: (Int, Int, Int) -> Unit,
    onDismiss: () -> Unit
) {
    var completionText by remember { mutableStateOf("") }
    var belongingText by remember { mutableStateOf("") }
    var emotionText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("完成记录") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("$characterName 的完成情况")
                OutlinedTextField(
                    value = completionText,
                    onValueChange = { completionText = it },
                    label = { Text("完成度(0-10)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = belongingText,
                    onValueChange = { belongingText = it },
                    label = { Text("归属感(0-10)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = emotionText,
                    onValueChange = { emotionText = it },
                    label = { Text("情绪值(0-10)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val completion = completionText.toIntOrNull()?.coerceIn(0, 10) ?: 0
                val belonging = belongingText.toIntOrNull()?.coerceIn(0, 10) ?: 0
                val emotion = emotionText.toIntOrNull()?.coerceIn(0, 10) ?: 0
                onConfirm(completion, belonging, emotion)
            }) { Text("完成") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

@Composable
private fun DailyInputDialog(
    onConfirm: (Int) -> Unit
) {
    var inputText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = {},
        title = { Text("前日数值") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    label = { Text("输入范围：-100 到 100") },
                    modifier = Modifier.fillMaxWidth()
                )
                Text("每日首次打开执行页面需要输入前日数值。", style = MaterialTheme.typography.labelSmall)
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val input = inputText.toIntOrNull()?.coerceIn(-100, 100) ?: 0
                onConfirm(input)
            }) { Text("确定") }
        }
    )
}
