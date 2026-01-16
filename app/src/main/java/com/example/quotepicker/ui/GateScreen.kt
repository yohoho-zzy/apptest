package com.example.quotepicker.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
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
    val density = LocalDensity.current
    val circleSize = 156.dp
    val circleSizePx = with(density) { circleSize.toPx() }

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
        Image(
            painter = painterResource(id = com.example.quotepicker.R.drawable.magic_circle),
            contentDescription = null,
            modifier = Modifier
                .offset {
                    IntOffset(
                        (pos.x - circleSizePx / 2f).roundToInt(),
                        (pos.y - circleSizePx / 2f).roundToInt()
                    )
                }
                .size(circleSize)
                .background(Color.Transparent)
                .graphicsLayer(
                    alpha = opacity.value,
                    rotationZ = rotation,
                    shadowElevation = with(density) { 12.dp.toPx() },
                    scaleX = 1.05f,
                    scaleY = 1.05f
                )
        )
    }
}
