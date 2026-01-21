package com.example.quotepicker.ui.components

import android.media.AudioManager
import android.media.ToneGenerator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class EventSequenceRunner(
    val overlayText: String?,
    val start: (EventSequence) -> Unit
)

@Composable
fun rememberEventSequenceRunner(): EventSequenceRunner {
    val coroutineScope = rememberCoroutineScope()
    var overlayText by remember { mutableStateOf<String?>(null) }
    var eventJob by remember { mutableStateOf<Job?>(null) }
    val toneGenerator = remember { ToneGenerator(AudioManager.STREAM_MUSIC, 80) }

    DisposableEffect(Unit) {
        onDispose {
            toneGenerator.release()
        }
    }

    val startSequence: (EventSequence) -> Unit = { sequence ->
        eventJob?.cancel()
        eventJob = coroutineScope.launch {
            runEventSequence(sequence, toneGenerator) { overlayText = it }
        }
    }

    return remember(overlayText) {
        EventSequenceRunner(
            overlayText = overlayText,
            start = startSequence
        )
    }
}

private suspend fun runEventSequence(
    sequence: EventSequence,
    toneGenerator: ToneGenerator,
    onOverlayUpdate: (String?) -> Unit
) {
    for (step in sequence.steps) {
        when (step) {
            is EventStep.Display -> {
                onOverlayUpdate(step.text)
                delay(step.seconds * 1000L)
            }
            is EventStep.Countdown -> {
                val interval = step.intervalMs ?: step.randomRange?.random() ?: 1000L
                for (count in step.total downTo 1) {
                    onOverlayUpdate(count.toString())
                    toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 80)
                    delay(interval)
                }
            }
        }
    }
    onOverlayUpdate(null)
}
