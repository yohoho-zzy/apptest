package com.example.quotepicker.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.quotepicker.data.ResourceType
import com.example.quotepicker.data.ResourceWithTagsCharacters
import com.example.quotepicker.data.TagCategoryEntity
import com.example.quotepicker.data.TagEntity
import org.json.JSONArray

@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun ResourceListRow(
    resource: ResourceWithTagsCharacters,
    categories: List<TagCategoryEntity>,
    roleText: String,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val tags = sortTagsForDisplay(resource.tags, categories)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "[${typeLabel(resource.resource.type)}]",
                color = Color(0xFF7B4DFF),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = resource.resource.title,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = roleText,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = resourceCountLabel(resource),
                color = Color(0xFF1565C0),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold
            )
        }
        if (tags.isEmpty()) {
            Text("无标签", style = MaterialTheme.typography.labelSmall)
        } else {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                tags.forEach { tag ->
                    CompactTagLabel(tag = tag)
                }
            }
        }
    }
}

@Composable
private fun CompactTagLabel(tag: TagEntity) {
    val bgColor = Color(tag.colorArgb)
    Text(
        text = formatTagLabel(tag.name),
        color = tagTextColor(bgColor),
        fontSize = 10.sp,
        lineHeight = 10.sp,
        modifier = Modifier
            .background(bgColor)
            .border(0.5.dp, bgColor)
            .padding(horizontal = 4.dp, vertical = 1.dp)
    )
}

private fun resourceCountLabel(resource: ResourceWithTagsCharacters): String {
    val data = resource.resource
    return when (data.type) {
        ResourceType.IMAGE -> "${jsonArraySize(data.contentUriOrPath)}张"
        ResourceType.VIDEO -> "${jsonArraySize(data.contentUriOrPath)}个"
        ResourceType.SOUND -> "${jsonArraySize(data.contentUriOrPath)}个"
        ResourceType.TEXT -> "${data.quoteText?.length ?: 0}字"
        ResourceType.SCENE -> "${jsonArraySize(data.sceneJson)}对话"
        ResourceType.FLOW -> "${jsonArraySize(data.sceneJson)}个引用"
    }
}

private fun jsonArraySize(raw: String?): Int {
    val text = raw?.trim().orEmpty()
    if (text.isEmpty()) return 0
    return runCatching { JSONArray(text).length() }.getOrDefault(0)
}

private fun typeLabel(type: ResourceType): String = when (type) {
    ResourceType.FLOW -> "流程"
    ResourceType.IMAGE -> "图片"
    ResourceType.VIDEO -> "视频"
    ResourceType.SOUND -> "声音"
    ResourceType.TEXT -> "文本"
    ResourceType.SCENE -> "情景"
}
