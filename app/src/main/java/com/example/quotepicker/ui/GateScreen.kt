package com.example.quotepicker.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.delay

enum class Corner { LEFT_TOP, RIGHT_TOP, LEFT_BOTTOM, RIGHT_BOTTOM, NONE }

/**
 * 根据触点位置判断点击在哪个区域
 * 屏幕按中心点划分为四个大区域
 */
private fun classifyCorner(p: Offset, size: IntSize): Corner {
    val midX = size.width / 2f
    val midY = size.height / 2f

    return when {
        p.x < midX && p.y < midY -> Corner.LEFT_TOP
        p.x >= midX && p.y < midY -> Corner.RIGHT_TOP
        p.x < midX && p.y >= midY -> Corner.LEFT_BOTTOM
        p.x >= midX && p.y >= midY -> Corner.RIGHT_BOTTOM
        else -> Corner.NONE
    }
}

@Composable
fun GateScreen(onPassed: () -> Unit) {
    var step by remember { mutableStateOf(0) }   // 当前步骤
    var count by remember { mutableStateOf(0) }  // 当前步骤已点次数
    var lastTick by remember { mutableStateOf(System.currentTimeMillis()) }

    // 解锁目标顺序：右上3次 → 左上1次 → 左下2次
    val targets = listOf(
        Corner.RIGHT_TOP to 3,
        Corner.LEFT_TOP to 1,
        Corner.LEFT_BOTTOM to 2
    )

    // 每次点击更新最后时间
    LaunchedEffect(step, count) {
        lastTick = System.currentTimeMillis()
    }

    // 超时10秒自动重置
    LaunchedEffect(lastTick) {
        while (true) {
            delay(1000)
            if (System.currentTimeMillis() - lastTick > 10_000) {
                step = 0
                count = 0
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
                    val corner = classifyCorner(down.position, this.size)
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
                        // 点击错误，重置
                        step = 0
                        count = 0
                    }
                }
            }
    )
}
