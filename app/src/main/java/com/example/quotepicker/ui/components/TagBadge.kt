package com.example.quotepicker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp
import com.example.quotepicker.data.TagEntity

@Composable
fun TagBadge(
    tag: TagEntity,
    modifier: Modifier = Modifier
) {
    val bg = Color(tag.colorArgb)
    val textColor = tagTextColor(bg)
    Box(
        modifier = modifier
            .background(bg, shape = MaterialTheme.shapes.small)
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(text = formatTagLabel(tag.name), color = textColor, style = MaterialTheme.typography.labelSmall)
    }
}

fun tagTextColor(color: Color): Color {
    return if (color.luminance() < 0.5f) Color.White else Color.Black
}
