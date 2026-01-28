package com.example.quotepicker.ui.components

import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.buildAnnotatedString
import kotlin.random.Random

sealed interface PreviewSegment {
    data class TextSegment(val text: String) : PreviewSegment
    data class EventSegment(val sequence: EventSequence) : PreviewSegment
    data class FileSegment(val label: String, val fileInfo: String) : PreviewSegment
}

data class EventSequence(
    val label: String,
    val steps: List<EventStep>,
    val toneIndex: Int? = null
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
private val fileTokenRegex = Regex("@([^@]+)@\\(([^)]*)\\)")

fun parsePreviewSegments(text: String, random: Random = Random.Default): List<PreviewSegment> {
    if (text.isBlank()) return listOf(PreviewSegment.TextSegment(text))
    val segments = mutableListOf<PreviewSegment>()
    var lastIndex = 0
    eventTokenRegex.findAll(text).forEach { match ->
        if (match.range.first > lastIndex) {
            val raw = text.substring(lastIndex, match.range.first)
            segments.addAll(parseFileSegments(raw, random))
        }
        val token = match.groupValues.getOrNull(1).orEmpty()
        val sequence = parseEventSequence(token)
        if (sequence != null) {
            segments.add(PreviewSegment.EventSegment(sequence))
        } else if (token.isNotBlank()) {
            segments.addAll(parseFileSegments(token, random))
        }
        lastIndex = match.range.last + 1
    }
    if (lastIndex < text.length) {
        val tail = text.substring(lastIndex)
        segments.addAll(parseFileSegments(tail, random))
    }
    return if (segments.isEmpty()) parseFileSegments(text, random) else segments
}

fun parseEventSequence(raw: String): EventSequence? {
    val trimmed = raw.trim()
    val toneMatch = Regex("^(\\d+)").find(trimmed)
    val toneIndex = toneMatch?.value?.toIntOrNull()?.minus(1)
    val content = if (toneMatch != null) trimmed.drop(toneMatch.value.length) else trimmed
    val steps = mutableListOf<EventStep>()
    eventStepRegex.findAll(content).forEach { match ->
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
    return EventSequence(label = label, steps = steps, toneIndex = toneIndex)
}

@Composable
fun PreviewTextBlock(
    text: String,
    onEventSequence: (EventSequence) -> Unit,
    onFilePreview: (String) -> Unit = {},
    modifier: Modifier = Modifier,
    textStyle: TextStyle = MaterialTheme.typography.bodyMedium
) {
    val eventColor = MaterialTheme.colorScheme.primary
    val preview = remember(text, eventColor) {
        buildPreviewAnnotatedText(text, eventColor, Color(0xFF2E7D32))
    }
    SelectionContainer {
        ClickableText(
            text = preview.annotatedString,
            modifier = modifier,
            style = textStyle,
            onClick = { offset ->
                preview.annotatedString
                    .getStringAnnotations("event", offset, offset)
                    .firstOrNull()
                    ?.item
                    ?.toIntOrNull()
                    ?.let { index -> preview.eventSequences.getOrNull(index) }
                    ?.let(onEventSequence)
                    ?: preview.annotatedString
                        .getStringAnnotations("file", offset, offset)
                        .firstOrNull()
                        ?.item
                        ?.toIntOrNull()
                        ?.let { index -> preview.fileInfos.getOrNull(index) }
                        ?.let(onFilePreview)
            }
        )
    }
}

private data class PreviewAnnotatedText(
    val annotatedString: AnnotatedString,
    val eventSequences: List<EventSequence>,
    val fileInfos: List<String>
)

private fun buildPreviewAnnotatedText(
    text: String,
    eventColor: Color,
    fileColor: Color
): PreviewAnnotatedText {
    val random = Random.Default
    val segments = parsePreviewSegments(text, random)
    val events = mutableListOf<EventSequence>()
    val files = mutableListOf<String>()
    val annotatedString = buildAnnotatedString {
        segments.forEach { segment ->
            when (segment) {
                is PreviewSegment.TextSegment -> append(segment.text)
                is PreviewSegment.EventSegment -> {
                    val display = "#${formatDisplayText(segment.sequence.label, random)}"
                    val start = length
                    append(display)
                    val end = length
                    addStyle(
                        SpanStyle(
                            color = eventColor,
                            fontWeight = FontWeight.SemiBold,
                            textDecoration = TextDecoration.Underline
                        ),
                        start,
                        end
                    )
                    addStringAnnotation("event", events.size.toString(), start, end)
                    events.add(segment.sequence)
                }
                is PreviewSegment.FileSegment -> {
                    val display = "@${formatDisplayText(segment.label, random)}"
                    val start = length
                    append(display)
                    val end = length
                    addStyle(
                        SpanStyle(
                            color = fileColor,
                            fontWeight = FontWeight.SemiBold,
                            textDecoration = TextDecoration.Underline
                        ),
                        start,
                        end
                    )
                    addStringAnnotation("file", files.size.toString(), start, end)
                    files.add(segment.fileInfo)
                }
            }
        }
    }
    return PreviewAnnotatedText(
        annotatedString = annotatedString,
        eventSequences = events,
        fileInfos = files
    )
}

private fun parseFileSegments(text: String, random: Random): List<PreviewSegment> {
    if (text.isBlank()) return emptyList()
    val segments = mutableListOf<PreviewSegment>()
    var lastIndex = 0
    fileTokenRegex.findAll(text).forEach { match ->
        if (match.range.first > lastIndex) {
            val raw = text.substring(lastIndex, match.range.first)
            if (raw.isNotBlank()) {
                segments.add(PreviewSegment.TextSegment(formatDisplayText(raw, random)))
            }
        }
        val label = match.groupValues.getOrNull(1).orEmpty().trim()
        val info = match.groupValues.getOrNull(2).orEmpty()
        if (label.isNotBlank() && info.isNotBlank()) {
            segments.add(PreviewSegment.FileSegment(label = label, fileInfo = info))
        } else if (match.value.isNotBlank()) {
            segments.add(PreviewSegment.TextSegment(formatDisplayText(match.value, random)))
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
