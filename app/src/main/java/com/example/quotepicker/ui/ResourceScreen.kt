package com.example.quotepicker.ui

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.quotepicker.data.ResourceEntity
import com.example.quotepicker.data.ResourceType
import com.example.quotepicker.data.ResourceWithTagsCharacters
import com.example.quotepicker.ui.components.AvatarListItem
import com.example.quotepicker.vm.ResourceViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResourceScreen(modifier: Modifier = Modifier, vm: ResourceViewModel = viewModel()) {
    val ui by vm.uiState.collectAsState()
    var showAddMenu by remember { mutableStateOf(false) }
    var showTextDialog by remember { mutableStateOf(false) }
    var showImageQuoteDialog by remember { mutableStateOf(false) }
    var showSceneDialog by remember { mutableStateOf(false) }
    var pickedMediaType by remember { mutableStateOf<ResourceType?>(null) }
    var previewTarget by remember { mutableStateOf<ResourceWithTagsCharacters?>(null) }
    var editTarget by remember { mutableStateOf<ResourceWithTagsCharacters?>(null) }
    var bottomSheetTarget by remember { mutableStateOf<ResourceWithTagsCharacters?>(null) }
    var deleteTarget by remember { mutableStateOf<ResourceWithTagsCharacters?>(null) }
    var filterTagDialog by remember { mutableStateOf(false) }
    var filterCharacterDialog by remember { mutableStateOf(false) }

    var selectedResourceInput by remember { mutableStateOf<ResourceInputState?>(null) }
    val mediaLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        val type = pickedMediaType ?: return@rememberLauncherForActivityResult
        val resolved = uri ?: return@rememberLauncherForActivityResult
        selectedResourceInput = ResourceInputState(title = "", type = type, uri = resolved)
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("资源") },
                actions = {
                    IconButton(onClick = { filterTagDialog = true }) {
                        Icon(Icons.Default.Edit, contentDescription = "筛选标签")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddMenu = true }) {
                Icon(Icons.Default.Add, contentDescription = null)
            }
        }
    ) { inner ->
        Column(Modifier.padding(inner).fillMaxSize()) {
            FilterBar(
                selectedType = ui.filters.selectedType,
                selectedTagIds = ui.filters.selectedTagIds,
                characters = ui.characters,
                selectedCharacterId = ui.filters.selectedCharacterId,
                onTypeChange = vm::updateTypeFilter,
                onCharacterDialog = { filterCharacterDialog = true },
                onTagDialog = { filterTagDialog = true }
            )
            if (ui.resources.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("暂无资源，点击右下角添加")
                }
            } else {
                LazyColumn(
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(ui.resources, key = { it.resource.id }) { res ->
                        AvatarListItem(
                            title = res.resource.title,
                            onClick = { previewTarget = res },
                            onLongClick = { bottomSheetTarget = res }
                        )
                    }
                }
            }
        }
    }

    if (showAddMenu) {
        ModalBottomSheet(onDismissRequest = { showAddMenu = false }) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                TextButton(onClick = { showTextDialog = true; showAddMenu = false }) { Text("创建文本语录") }
                TextButton(onClick = { showImageQuoteDialog = true; showAddMenu = false }) { Text("创建图片语录") }
                TextButton(onClick = { showSceneDialog = true; showAddMenu = false }) { Text("创建聊天情景") }
                TextButton(onClick = {
                    pickedMediaType = ResourceType.IMAGE
                    showAddMenu = false
                    mediaLauncher.launch("image/*")
                }) { Text("上传图片") }
                TextButton(onClick = {
                    pickedMediaType = ResourceType.VIDEO
                    showAddMenu = false
                    mediaLauncher.launch("video/*")
                }) { Text("上传视频") }
                TextButton(onClick = {
                    pickedMediaType = ResourceType.AUDIO
                    showAddMenu = false
                    mediaLauncher.launch("audio/*")
                }) { Text("上传声音") }
            }
        }
    }

    if (showTextDialog) {
        QuoteDialog(
            title = "创建文本语录",
            onConfirm = { title, text, tagIds, characterIds ->
                vm.addTextQuote(title, text, tagIds, characterIds)
                showTextDialog = false
            },
            tags = ui.tags,
            characters = ui.characters,
            onDismiss = { showTextDialog = false }
        )
    }

    if (showImageQuoteDialog) {
        ImageQuoteDialog(
            tags = ui.tags,
            characters = ui.characters,
            onConfirm = { title, uri, tagIds, characterIds ->
                vm.addImageQuote(title, uri, tagIds, characterIds)
                showImageQuoteDialog = false
            },
            onDismiss = { showImageQuoteDialog = false }
        )
    }

    if (showSceneDialog) {
        SceneDialog(
            tags = ui.tags,
            characters = ui.characters,
            onConfirm = { title, desc, json, tagIds, characterIds ->
                vm.addScene(title, desc, json, tagIds, characterIds)
                showSceneDialog = false
            },
            onDismiss = { showSceneDialog = false }
        )
    }

    selectedResourceInput?.let { input ->
        MediaDialog(
            input = input,
            tags = ui.tags,
            characters = ui.characters,
            onConfirm = { title, tagIds, characterIds ->
                vm.addEncryptedMedia(input.type, title, input.uri, tagIds, characterIds)
                selectedResourceInput = null
            },
            onDismiss = { selectedResourceInput = null }
        )
    }

    if (filterTagDialog) {
        FilterTagDialog(
            tags = ui.tags,
            selectedIds = ui.filters.selectedTagIds,
            onConfirm = { vm.updateTagFilter(it) },
            onDismiss = { filterTagDialog = false }
        )
    }
    if (filterCharacterDialog) {
        FilterCharacterDialog(
            characters = ui.characters,
            selectedId = ui.filters.selectedCharacterId,
            onConfirm = { vm.updateCharacterFilter(it) },
            onDismiss = { filterCharacterDialog = false }
        )
    }

    previewTarget?.let { target ->
        ResourcePreviewDialog(resource = target, vm = vm, onDismiss = { previewTarget = null })
    }

    editTarget?.let { target ->
        ResourceEditDialog(
            resource = target,
            tags = ui.tags,
            characters = ui.characters,
            onConfirm = { title, tagIds, characterIds ->
                vm.updateResource(target.resource.copy(title = title))
                vm.updateResourceTags(target.resource.id, tagIds)
                vm.updateResourceCharacters(target.resource.id, characterIds)
                editTarget = null
            },
            onDelete = {
                deleteTarget = target
                editTarget = null
            },
            onDismiss = { editTarget = null }
        )
    }

    bottomSheetTarget?.let { target ->
        ModalBottomSheet(onDismissRequest = { bottomSheetTarget = null }) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                TextButton(onClick = {
                    editTarget = target
                    bottomSheetTarget = null
                }) {
                    Icon(Icons.Default.Edit, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("编辑")
                }
                TextButton(onClick = {
                    deleteTarget = target
                    bottomSheetTarget = null
                }) {
                    Icon(Icons.Default.Delete, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("删除")
                }
                TextButton(onClick = { bottomSheetTarget = null }) { Text("关闭") }
            }
            Spacer(Modifier.height(12.dp))
        }
    }

    deleteTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("删除资源") },
            text = { Text("确定删除“${target.resource.title}”吗？") },
            confirmButton = {
                TextButton(onClick = {
                    vm.deleteResource(target.resource)
                    deleteTarget = null
                }) { Text("删除") }
            },
            dismissButton = { TextButton(onClick = { deleteTarget = null }) { Text("取消") } }
        )
    }
}

@Composable
private fun FilterBar(
    selectedType: ResourceType?,
    selectedTagIds: Set<Long>,
    characters: List<com.example.quotepicker.data.CharacterEntity>,
    selectedCharacterId: Long?,
    onTypeChange: (ResourceType?) -> Unit,
    onCharacterDialog: () -> Unit,
    onTagDialog: () -> Unit
) {
    Column(Modifier.fillMaxWidth().padding(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ResourceType.values().forEach { type ->
            FilterChip(
                selected = selectedType == type,
                onClick = { onTypeChange(if (selectedType == type) null else type) },
                label = { Text(typeLabel(type)) }
            )
        }
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            AssistChip(onClick = onTagDialog, label = { Text("标签筛选(${selectedTagIds.size})") })
            val selectedCharacter = characters.firstOrNull { it.id == selectedCharacterId }
            AssistChip(
                onClick = onCharacterDialog,
                label = { Text(selectedCharacter?.name ?: "全部角色") }
            )
        }
    }
}

private fun typeLabel(type: ResourceType): String = when (type) {
    ResourceType.IMAGE -> "图片"
    ResourceType.VIDEO -> "视频"
    ResourceType.AUDIO -> "声音"
    ResourceType.QUOTE -> "语录"
    ResourceType.SCENE -> "情景"
}

data class ResourceInputState(
    val title: String,
    val type: ResourceType,
    val uri: Uri
)

@Composable
private fun QuoteDialog(
    title: String,
    tags: List<com.example.quotepicker.data.TagEntity>,
    characters: List<com.example.quotepicker.data.CharacterEntity>,
    onConfirm: (String, String, List<Long>, List<Long>) -> Unit,
    onDismiss: () -> Unit
) {
    var quoteTitle by remember { mutableStateOf("") }
    var quoteText by remember { mutableStateOf("") }
    var selectedTags by remember { mutableStateOf(setOf<Long>()) }
    var selectedCharacters by remember { mutableStateOf(setOf<Long>()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = quoteTitle,
                    onValueChange = { quoteTitle = it },
                    label = { Text("标题") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = quoteText,
                    onValueChange = { quoteText = it },
                    label = { Text("文本内容") },
                    modifier = Modifier.fillMaxWidth()
                )
                MultiSelectRow("标签", tags.map { it.name }, selectedTags, tags.map { it.id }) {
                    selectedTags = it
                }
                MultiSelectRow("角色", characters.map { it.name }, selectedCharacters, characters.map { it.id }) {
                    selectedCharacters = it
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (quoteTitle.isNotBlank() && selectedCharacters.isNotEmpty()) {
                    onConfirm(quoteTitle.trim(), quoteText.trim(), selectedTags.toList(), selectedCharacters.toList())
                }
            }) { Text("确定") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

@Composable
private fun ImageQuoteDialog(
    tags: List<com.example.quotepicker.data.TagEntity>,
    characters: List<com.example.quotepicker.data.CharacterEntity>,
    onConfirm: (String, Uri, List<Long>, List<Long>) -> Unit,
    onDismiss: () -> Unit
) {
    var title by remember { mutableStateOf("") }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var selectedTags by remember { mutableStateOf(setOf<Long>()) }
    var selectedCharacters by remember { mutableStateOf(setOf<Long>()) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        imageUri = uri
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("创建图片语录") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("标题") })
                TextButton(onClick = { launcher.launch("image/*") }) { Text("选择图片") }
                Text(imageUri?.toString() ?: "未选择图片")
                MultiSelectRow("标签", tags.map { it.name }, selectedTags, tags.map { it.id }) {
                    selectedTags = it
                }
                MultiSelectRow("角色", characters.map { it.name }, selectedCharacters, characters.map { it.id }) {
                    selectedCharacters = it
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val uri = imageUri ?: return@TextButton
                if (title.isNotBlank() && selectedCharacters.isNotEmpty()) {
                    onConfirm(title.trim(), uri, selectedTags.toList(), selectedCharacters.toList())
                }
            }) { Text("确定") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

@Composable
private fun SceneDialog(
    tags: List<com.example.quotepicker.data.TagEntity>,
    characters: List<com.example.quotepicker.data.CharacterEntity>,
    onConfirm: (String, String?, String, List<Long>, List<Long>) -> Unit,
    onDismiss: () -> Unit
) {
    var title by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var sceneJson by remember { mutableStateOf("") }
    var selectedTags by remember { mutableStateOf(setOf<Long>()) }
    var selectedCharacters by remember { mutableStateOf(setOf<Long>()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("创建聊天情景") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("标题") })
                OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("描述(可选)") })
                OutlinedTextField(
                    value = sceneJson,
                    onValueChange = { sceneJson = it },
                    label = { Text("sceneJson") },
                    modifier = Modifier.fillMaxWidth()
                )
                MultiSelectRow("标签", tags.map { it.name }, selectedTags, tags.map { it.id }) {
                    selectedTags = it
                }
                MultiSelectRow("角色", characters.map { it.name }, selectedCharacters, characters.map { it.id }) {
                    selectedCharacters = it
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (title.isNotBlank() && selectedCharacters.isNotEmpty()) {
                    onConfirm(title.trim(), desc.ifBlank { null }, sceneJson, selectedTags.toList(), selectedCharacters.toList())
                }
            }) { Text("确定") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

@Composable
private fun MediaDialog(
    input: ResourceInputState,
    tags: List<com.example.quotepicker.data.TagEntity>,
    characters: List<com.example.quotepicker.data.CharacterEntity>,
    onConfirm: (String, List<Long>, List<Long>) -> Unit,
    onDismiss: () -> Unit
) {
    var title by remember { mutableStateOf("") }
    var selectedTags by remember { mutableStateOf(setOf<Long>()) }
    var selectedCharacters by remember { mutableStateOf(setOf<Long>()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("上传${typeLabel(input.type)}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("标题") })
                Text(input.uri.toString())
                MultiSelectRow("标签", tags.map { it.name }, selectedTags, tags.map { it.id }) {
                    selectedTags = it
                }
                MultiSelectRow("角色", characters.map { it.name }, selectedCharacters, characters.map { it.id }) {
                    selectedCharacters = it
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (title.isNotBlank() && selectedCharacters.isNotEmpty()) {
                    onConfirm(title.trim(), selectedTags.toList(), selectedCharacters.toList())
                }
            }) { Text("确定") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

@Composable
private fun FilterTagDialog(
    tags: List<com.example.quotepicker.data.TagEntity>,
    selectedIds: Set<Long>,
    onConfirm: (Set<Long>) -> Unit,
    onDismiss: () -> Unit
) {
    var selected by remember { mutableStateOf(selectedIds.toMutableSet()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择标签筛选") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                tags.forEach { tag ->
                    val isChecked = selected.contains(tag.id)
                    TextButton(onClick = {
                        if (isChecked) selected.remove(tag.id) else selected.add(tag.id)
                    }) { Text(if (isChecked) "✓ ${tag.name}" else tag.name) }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onConfirm(selected)
                onDismiss()
            }) { Text("确定") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

@Composable
private fun FilterCharacterDialog(
    characters: List<com.example.quotepicker.data.CharacterEntity>,
    selectedId: Long?,
    onConfirm: (Long?) -> Unit,
    onDismiss: () -> Unit
) {
    var current by remember { mutableStateOf(selectedId) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择角色筛选") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = { current = null }) { Text("全部角色") }
                characters.forEach { character ->
                    TextButton(onClick = { current = character.id }) {
                        Text(if (current == character.id) "✓ ${character.name}" else character.name)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onConfirm(current)
                onDismiss()
            }) { Text("确定") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

@Composable
private fun ResourcePreviewDialog(
    resource: ResourceWithTagsCharacters,
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
                        bitmap?.let {
                            Image(bitmap = it.asImageBitmap(), contentDescription = null)
                        }
                    }
                    ResourceType.IMAGE -> {
                        bitmap?.let {
                            Image(bitmap = it.asImageBitmap(), contentDescription = null)
                        } ?: Text("图片加载中…")
                    }
                    ResourceType.AUDIO -> Text("音频预览请使用外部播放器（已加密存储）")
                    ResourceType.VIDEO -> Text("视频预览请使用外部播放器（已加密存储）")
                    ResourceType.SCENE -> {
                        Text(resource.resource.sceneJson.orEmpty())
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("关闭") } }
    )
}

@Composable
private fun ResourceEditDialog(
    resource: ResourceWithTagsCharacters,
    tags: List<com.example.quotepicker.data.TagEntity>,
    characters: List<com.example.quotepicker.data.CharacterEntity>,
    onConfirm: (String, List<Long>, List<Long>) -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    var title by remember { mutableStateOf(resource.resource.title) }
    var selectedTags by remember { mutableStateOf(resource.tags.map { it.id }.toSet()) }
    var selectedCharacters by remember { mutableStateOf(resource.characters.map { it.id }.toSet()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("编辑资源") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("标题") })
                MultiSelectRow("标签", tags.map { it.name }, selectedTags, tags.map { it.id }) {
                    selectedTags = it
                }
                MultiSelectRow("角色", characters.map { it.name }, selectedCharacters, characters.map { it.id }) {
                    selectedCharacters = it
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (title.isNotBlank() && selectedCharacters.isNotEmpty()) {
                    onConfirm(title.trim(), selectedTags.toList(), selectedCharacters.toList())
                }
            }) { Text("保存") }
        },
        dismissButton = {
            TextButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("删除")
            }
        }
    )
}

@Composable
private fun MultiSelectRow(
    label: String,
    names: List<String>,
    selected: Set<Long>,
    ids: List<Long>,
    onChange: (Set<Long>) -> Unit
) {
    Column {
        Text(label)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ids.forEachIndexed { idx, id ->
                val name = names[idx]
                val isSelected = selected.contains(id)
                FilterChip(
                    selected = isSelected,
                    onClick = {
                        val newSet = selected.toMutableSet()
                        if (isSelected) newSet.remove(id) else newSet.add(id)
                        onChange(newSet)
                    },
                    label = { Text(name) }
                )
            }
        }
    }
}
