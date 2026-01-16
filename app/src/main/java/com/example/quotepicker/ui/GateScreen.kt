package com.example.quotepicker.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.matchParentSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

enum class Corner { LEFT_TOP, RIGHT_TOP, LEFT_BOTTOM, RIGHT_BOTTOM, NONE }

private fun classifyCorner(p: Offset, size: IntSize): Corner {
    val quarterW = size.width * 0.25f
    val quarterH = size.height * 0.25f

    return when {
        p.x <= quarterW && p.y <= quarterH -> Corner.LEFT_TOP
        p.x >= size.width - quarterW && p.y <= quarterH -> Corner.RIGHT_TOP
        p.x <= quarterW && p.y >= size.height - quarterH -> Corner.LEFT_BOTTOM
        p.x >= size.width - quarterW && p.y >= size.height - quarterH -> Corner.RIGHT_BOTTOM
        else -> Corner.NONE
    }
}

@Composable
fun GateScreen(onPassed: () -> Unit) {
    var step by remember { mutableStateOf(0) }
    var count by remember { mutableStateOf(0) }
    var startTime by remember { mutableStateOf<Long?>(null) }
    var touchPosition by remember { mutableStateOf<Offset?>(null) }
    val opacity = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val infiniteTransition = rememberInfiniteTransition(label = "magicCircle")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(6000),
            repeatMode = RepeatMode.Restart
        ),
        label = "magicCircleRotation"
    )
    val counterRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4200),
            repeatMode = RepeatMode.Restart
        ),
        label = "magicCircleCounterRotation"
    )
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400),
            repeatMode = RepeatMode.Reverse
        ),
        label = "magicCirclePulse"
    )
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.15f,
        targetValue = 0.45f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600),
            repeatMode = RepeatMode.Reverse
        ),
        label = "magicCircleGlow"
    )
    val density = LocalDensity.current
    val circleSize = 156.dp
    val innerSize = 110.dp
    val outerSize = 230.dp
    val circleSizePx = with(density) { circleSize.toPx() }
    val outerSizePx = with(density) { outerSize.toPx() }

    // 解锁目标顺序：右上3次 → 左上1次 → 左下2次
    val targets = listOf(
        Corner.RIGHT_TOP to 3,
        Corner.LEFT_TOP to 1,
        Corner.LEFT_BOTTOM to 2
    )

    // 超时10秒自动重置
    LaunchedEffect(startTime) {
        while (true) {
            delay(500)
            val started = startTime ?: continue
            if (System.currentTimeMillis() - started > 10_000) {
                step = 0
                count = 0
                startTime = null
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown()
                    touchPosition = down.position
                    scope.launch {
                        opacity.snapTo(1f)
                    }
                    var pointerId = down.id
                    var upPosition = down.position
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == pointerId }
                        if (change == null) break
                        if (change.pressed) {
                            touchPosition = change.position
                            upPosition = change.position
                        } else {
                            upPosition = change.position
                            break
                        }
                    }
                    val corner = classifyCorner(upPosition, this.size)
                    if (corner != Corner.NONE) {
                        if (startTime == null) startTime = System.currentTimeMillis()
                        val (expectCorner, expectTimes) = targets[step]
                        if (corner == expectCorner) {
                            count += 1
                            if (count >= expectTimes) {
                                step += 1
                                count = 0
                                if (step >= targets.size) {
                                    onPassed()
                                }
                            }
                        } else {
                            step = 0
                            count = 0
                            startTime = null
                        }
                    }
                    scope.launch {
                        opacity.animateTo(0f, tween(200))
                    }
                    touchPosition = null
                }
            }
    )
    touchPosition?.let { pos ->
        Box(
            modifier = Modifier
                .offset {
                    IntOffset(
                        (pos.x - outerSizePx / 2f).roundToInt(),
                        (pos.y - outerSizePx / 2f).roundToInt()
                    )
                }
                .size(outerSize)
                .graphicsLayer(alpha = opacity.value)
        ) {
            Canvas(
                modifier = Modifier
                    .matchParentSize()
                    .graphicsLayer(scaleX = pulse, scaleY = pulse, alpha = glowAlpha)
            ) {
                val radius = size.minDimension / 2f
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFF7B4DFF), Color.Transparent)
                    ),
                    radius = radius
                )
                drawCircle(
                    color = Color(0xFFB388FF).copy(alpha = 0.5f),
                    radius = radius * 0.88f,
                    style = Stroke(width = radius * 0.08f)
                )
            }
            Image(
                painter = painterResource(id = com.example.quotepicker.R.drawable.magic_circle),
                contentDescription = null,
                modifier = Modifier
                    .size(circleSize)
                    .align(Alignment.Center)
                    .graphicsLayer(
                        rotationZ = rotation,
                        shadowElevation = with(density) { 14.dp.toPx() },
                        scaleX = 1.08f,
                        scaleY = 1.08f
                    )
            )
            Image(
                painter = painterResource(id = com.example.quotepicker.R.drawable.magic_circle),
                contentDescription = null,
                modifier = Modifier
                    .size(innerSize)
                    .align(Alignment.Center)
                    .graphicsLayer(
                        rotationZ = counterRotation,
                        alpha = 0.85f
                    )
            )
        }
    }
}
