package com.example.quotepicker.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.quotepicker.ui.components.ResourcePreviewScreen
import com.example.quotepicker.vm.RandomViewModel
import com.example.quotepicker.vm.ResourceViewModel

@Composable
fun RandomScreen(modifier: Modifier = Modifier, vm: RandomViewModel = viewModel(), resourceVm: ResourceViewModel = viewModel()) {
    val ui by vm.uiState.collectAsState()
    var showPreview by remember { mutableStateOf(false) }

    if (showPreview && ui.selectedResource != null) {
        ResourcePreviewScreen(
            resource = ui.selectedResource!!,
            vm = resourceVm,
            onBack = { showPreview = false }
        )
        return
    }

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
}
