package com.example.quotepicker.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.quotepicker.data.GroupEntity
import com.example.quotepicker.vm.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddQuoteDialog(
    groups: List<GroupEntity>,
    onDismiss: () -> Unit,
    onAddText: (groupId: Long, text: String, weight: Int) -> Unit,
    onAddImage: (groupId: Long, base64: String, text: String, weight: Int) -> Unit,
    vm: MainViewModel
) {
    var isImage by remember { mutableStateOf(false) }
    var groupId by remember { mutableStateOf(groups.firstOrNull()?.id ?: -1L) }
    var text by remember { mutableStateOf("") }
    var weightStr by remember { mutableStateOf("1") }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null && groupId > 0) {
            val b64 = vm.encodeImageToBase64(uri)
            val w = weightStr.toIntOrNull() ?: 1
            onAddImage(groupId, b64, text.trim(), w.coerceAtLeast(1))
            onDismiss()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("添加语录") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    FilterChip(selected = !isImage, onClick = { isImage = false }, label = { Text("文本") })
                    FilterChip(selected = isImage, onClick = { isImage = true }, label = { Text("图片") })
                }
                if (groups.isEmpty()) {
                    Text("请先添加分组")
                } else {
                    var expanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
                        TextField(
                            value = groups.firstOrNull { it.id == groupId }?.name ?: "选择分组",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("分组") },
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            groups.forEach {
                                DropdownMenuItem(
                                    text = { Text(it.name) },
                                    onClick = { groupId = it.id; expanded = false }
                                )
                            }
                        }
                    }
                }
                OutlinedTextField(
                    value = weightStr,
                    onValueChange = { weightStr = it },
                    label = { Text("权重(>=1)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                if (!isImage) {
                    OutlinedTextField(
                        value = text,
                        onValueChange = { text = it },
                        label = { Text("文本内容") },
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    OutlinedTextField(
                        value = text,
                        onValueChange = { text = it },
                        label = { Text("图片名称") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            if (!isImage) {
                val w = weightStr.toIntOrNull() ?: 1
                Button(onClick = {
                    if (groupId > 0 && text.isNotBlank()) {
                        onAddText(groupId, text.trim(), w.coerceAtLeast(1)); onDismiss()
                    }
                }, enabled = groups.isNotEmpty() && text.isNotBlank()) {
                    Text("确定")
                }
            } else {
                Button(onClick = { launcher.launch("image/*") }, enabled = groups.isNotEmpty() && text.isNotBlank()) {
                    Text("导入图片")
                }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}
