package com.example.quotepicker.ui

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.quotepicker.data.ResourceType
import com.example.quotepicker.vm.RandomViewModel
import com.example.quotepicker.vm.ResourceViewModel

@Composable
fun RandomScreen(modifier: Modifier = Modifier, vm: RandomViewModel = viewModel(), resourceVm: ResourceViewModel = viewModel()) {
    val ui by vm.uiState.collectAsState()
    var showPreview by remember { mutableStateOf(false) }

    Column(modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Button(onClick = { vm.randomCharacter() }) { Text("随机角色") }
        Text(ui.selectedCharacter?.name ?: "未选择角色")
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = { vm.randomResource() }, enabled = ui.selectedCharacter != null) { Text("随机资源") }
            Button(onClick = { vm.nextResource() }, enabled = ui.selectedCharacter != null) { Text("下一个") }
            Button(onClick = { vm.reset() }) { Text("重置") }
        }
        Spacer(Modifier.height(8.dp))
        val res = ui.selectedResource
        if (res == null) {
            Text("暂无资源")
        } else {
            Text("当前资源：${res.resource.title}")
            Button(onClick = { showPreview = true }) { Text("预览") }
        }
    }

    if (showPreview && ui.selectedResource != null) {
        RandomPreviewDialog(resource = ui.selectedResource!!, vm = resourceVm, onDismiss = { showPreview = false })
    }
}

@Composable
private fun RandomPreviewDialog(
    resource: com.example.quotepicker.data.ResourceWithTagsCharacters,
    vm: ResourceViewModel,
    onDismiss: () -> Unit
) {
    var bitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    LaunchedEffect(resource.resource.id) {
        val res = resource.resource
        bitmap = when (res.type) {
            ResourceType.IMAGE -> {
                val path = res.contentUriOrPath ?: return@LaunchedEffect
                val bytes = vm.loadDecryptedBytes(path)
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            }
            ResourceType.QUOTE -> {
                val b64 = res.quoteImageBase64 ?: return@LaunchedEffect
                vm.decodeBase64ToBitmap(b64)
            }
            else -> null
        }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(resource.resource.title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                when (resource.resource.type) {
                    ResourceType.QUOTE -> {
                        Text(resource.resource.quoteText.orEmpty())
                        bitmap?.let { Image(bitmap = it.asImageBitmap(), contentDescription = null) }
                    }
                    ResourceType.IMAGE -> {
                        bitmap?.let { Image(bitmap = it.asImageBitmap(), contentDescription = null) }
                    }
                    ResourceType.AUDIO -> Text("音频预览请在资源页查看")
                    ResourceType.VIDEO -> Text("视频预览请在资源页查看")
                    ResourceType.SCENE -> Text(resource.resource.sceneJson.orEmpty())
                }
            }
        },
        confirmButton = { Button(onClick = onDismiss) { Text("关闭") } }
    )
}
