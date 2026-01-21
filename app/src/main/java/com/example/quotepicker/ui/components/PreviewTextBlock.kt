package com.example.quotepicker.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import kotlin.random.Random
import androidx.compose.foundation.layout.ExperimentalLayoutApi

sealed interface PreviewSegment {
    data class TextSegment(val text: String) : PreviewSegment
    data class EventSegment(val sequence: EventSequence) : PreviewSegment
}

data class EventSequence(
    val label: String,
    val steps: List<EventStep>
)

sealed interface EventStep {
    data class Display(val text: String, val seconds: Int) : EventStep
    data class Countdown(
        val total: Int,
        val intervalMs: Long?,
        val randomRange: LongRange? = null
    ) : EventStep
}

private val eventTokenRegex = Regex("\\+\\+(.*?)\\+\\+")
private val eventStepRegex = Regex("\\[(.*?)]")

fun parsePreviewSegments(text: String, random: Random = Random.Default): List<PreviewSegment> {
    if (text.isBlank()) return listOf(PreviewSegment.TextSegment(text))
    val segments = mutableListOf<PreviewSegment>()
    var lastIndex = 0
    eventTokenRegex.findAll(text).forEach { match ->
        if (match.range.first > lastIndex) {
            val raw = text.substring(lastIndex, match.range.first)
            if (raw.isNotBlank()) {
                segments.add(PreviewSegment.TextSegment(formatDisplayText(raw, random)))
            }
        }
        val token = match.groupValues.getOrNull(1).orEmpty()
        val sequence = parseEventSequence(token)
        if (sequence != null) {
            segments.add(PreviewSegment.EventSegment(sequence))
        } else if (token.isNotBlank()) {
            segments.add(PreviewSegment.TextSegment(formatDisplayText(token, random)))
        }
        lastIndex = match.range.last + 1
    }
    if (lastIndex < text.length) {
        val tail = text.substring(lastIndex)
        if (tail.isNotBlank()) {
            segments.add(PreviewSegment.TextSegment(formatDisplayText(tail, random)))
        }
    }
    return if (segments.isEmpty()) listOf(PreviewSegment.TextSegment(formatDisplayText(text, random))) else segments
}

fun parseEventSequence(raw: String): EventSequence? {
    val steps = mutableListOf<EventStep>()
    eventStepRegex.findAll(raw).forEach { match ->
        val parts = match.groupValues.getOrNull(1)
            ?.split(",", limit = 2)
            ?.map { it.trim() }
            .orEmpty()
        if (parts.size < 2) return@forEach
        val first = parts[0]
        val second = parts[1]
        val count = first.toIntOrNull()
        val duration = second.toIntOrNull()
        if (count != null && (duration != null || second.equals("x", ignoreCase = true))) {
            val interval = duration?.toLong()
            val range = if (second.equals("x", ignoreCase = true)) 200L..2000L else null
            steps.add(EventStep.Countdown(total = count, intervalMs = interval, randomRange = range))
        } else if (duration != null && first.isNotBlank()) {
            steps.add(EventStep.Display(text = first, seconds = duration))
        }
    }
    if (steps.isEmpty()) return null
    val label = (steps.firstOrNull { it is EventStep.Display } as? EventStep.Display)?.text ?: "开始"
    return EventSequence(label = label, steps = steps)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PreviewTextBlock(
    text: String,
    onEventSequence: (EventSequence) -> Unit,
    modifier: Modifier = Modifier,
    textStyle: TextStyle = MaterialTheme.typography.bodyMedium
) {
    val segments = remember(text) { parsePreviewSegments(text) }
    SelectionContainer {
        FlowRow(
            modifier = modifier,
            horizontalArrangement = Arrangement.spacedBy(0.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            segments.forEach { segment ->
                when (segment) {
                    is PreviewSegment.TextSegment -> {
                        if (segment.text.isNotBlank()) {
                            Text(text = segment.text, style = textStyle)
                        }
                    }
                    is PreviewSegment.EventSegment -> {
                        val label = "【${segment.sequence.label}】"
                        Text(
                            text = label,
                            modifier = Modifier
                                .clickable { onEventSequence(segment.sequence) }
                                .padding(vertical = 2.dp),
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold,
                            textDecoration = TextDecoration.Underline,
                            style = textStyle
                        )
                    }
                }
            }
        }
    }
}
