package com.example.quotepicker.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.BorderStroke

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SquareGridItem(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    backgroundColor: Color = MaterialTheme.colorScheme.surface,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    borderColor: Color? = null,
    subtitleOnTop: Boolean = false,
    subtitleColor: Color? = null,
    subtitleFontWeight: FontWeight? = null,
    titleTextStyle: TextStyle = MaterialTheme.typography.bodyMedium,
    itemAspectRatio: Float = 1f,
    subtitleContent: (@Composable () -> Unit)? = null,
    bottomContent: (@Composable () -> Unit)? = null,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val border = borderColor?.let { BorderStroke(2.dp, it) } ?: CardDefaults.outlinedCardBorder()
    val resolvedSubtitleColor = subtitleColor ?: contentColor.copy(alpha = 0.8f)
    val resolvedSubtitleWeight = subtitleFontWeight ?: FontWeight.Normal
    OutlinedCard(
        modifier = modifier
            .aspectRatio(itemAspectRatio)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
        ,
        colors = CardDefaults.outlinedCardColors(containerColor = backgroundColor),
        border = border
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(6.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (subtitleOnTop) {
                    when {
                        subtitleContent != null -> subtitleContent()
                        subtitle != null -> {
                            Text(
                                text = subtitle,
                                style = MaterialTheme.typography.labelSmall,
                                color = resolvedSubtitleColor,
                                fontWeight = resolvedSubtitleWeight,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
                Text(
                    text = title,
                    style = titleTextStyle,
                    fontWeight = FontWeight.Bold,
                    color = contentColor,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (!subtitleOnTop) {
                    when {
                        subtitleContent != null -> subtitleContent()
                        subtitle != null -> {
                            Text(
                                text = subtitle,
                                style = MaterialTheme.typography.labelSmall,
                                color = resolvedSubtitleColor,
                                fontWeight = resolvedSubtitleWeight,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
                bottomContent?.invoke()
            }
        }
    }
}
