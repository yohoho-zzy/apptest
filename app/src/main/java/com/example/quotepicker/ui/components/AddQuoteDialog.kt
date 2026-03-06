package com.example.quotepicker.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.quotepicker.data.GroupEntity
import com.example.quotepicker.vm.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddQuoteDialog(
    groups: List<GroupEntity>,
    onDismiss: () -> Unit,
    onAddText: (Long, String, Int) -> Unit,
    onAddImage: (Long, String, Int) -> Unit,
    vm: MainViewModel
) {
    var selectedGroupId by remember(groups) { mutableStateOf(groups.firstOrNull()?.id ?: 0L) }
    var content by remember { mutableStateOf("") }
    var weightText by remember { mutableStateOf("1") }
    var asImage by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("添加语录") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = selectedGroupId.toString(),
                    onValueChange = { selectedGroupId = it.toLongOrNull() ?: 0L },
                    label = { Text("分组ID") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text(if (asImage) "图片(base64或uri)" else "文本") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = weightText,
                    onValueChange = { weightText = it },
                    label = { Text("权重") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { asImage = false }) { Text("文本") }
                    TextButton(onClick = { asImage = true }) { Text("图片") }
                }
                if (groups.isEmpty()) {
                    Text("请先添加分组")
                }
            }
        },
        confirmButton = {
            val w = weightText.toIntOrNull() ?: 1
            Button(
                enabled = groups.isNotEmpty() && selectedGroupId > 0 && content.isNotBlank(),
                onClick = {
                    if (asImage) {
                        onAddImage(selectedGroupId, content.trim(), w)
                    } else {
                        onAddText(selectedGroupId, content.trim(), w)
                    }
                }
            ) {
                Text("确定")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}
