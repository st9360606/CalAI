package com.calai.app.ui.home.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.size

/**
 * 共用圓環儀表（灰底 + 進度 + 上方小刻度點）
 */
@Composable
fun GaugeRing(
    progress: Float,                 // 0f..1f
    modifier: Modifier = Modifier,
    sizeDp: Dp = 84.dp,
    strokeDp: Dp = 12.dp,
    trackColor: Color = Color(0xFFEFF0F3),
    progressColor: Color = Color(0xFF111827),
    drawTopTick: Boolean = true,
    tickColor: Color = progressColor
) {
    Canvas(modifier = modifier.size(sizeDp)) {
        val stroke = Stroke(width = strokeDp.toPx(), cap = StrokeCap.Round)
        val radius = size.minDimension / 2f
        val startAngle = -90f

        // 背景圈
        drawArc(
            color = trackColor,
            startAngle = startAngle,
            sweepAngle = 360f,
            useCenter = false,
            style = stroke
        )

        // 進度圈
        val p = progress.coerceIn(0f, 1f)
        if (p > 0f) {
            drawArc(
                color = progressColor,
                startAngle = startAngle,
                sweepAngle = 360f * p,
                useCenter = false,
                style = stroke
            )
        }

        // 12 點方向小刻度
        if (drawTopTick) {
            val r = radius - stroke.width / 2f
            val center = Offset(size.width / 2f, size.height / 2f)
            val dot = Offset(center.x, center.y - r)
            drawCircle(color = tickColor, radius = stroke.width / 2.2f, center = dot)
        }
    }
}
