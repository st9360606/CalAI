package com.calai.app.ui.home.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.size

@Composable
fun GaugeRing(
    progress: Float,
    sizeDp: Dp,
    strokeDp: Dp,
    trackColor: Color,
    progressColor: Color,
    drawTopTick: Boolean = false,
    tickColor: Color = progressColor,
    // ★ 新增：控制「起始小點」相對於線寬的比例（0~1）
    tickRadiusScale: Float = 0.45f
) {
    Canvas(modifier = Modifier.size(sizeDp)) {
        val strokePx = strokeDp.toPx()
        val radius = size.minDimension / 2f
        val inset = strokePx / 2f
        val clampedProgress = progress.coerceIn(0f, 1f)
        val sweep = 360f * clampedProgress

        val arcSize = Size(
            width = size.width - inset * 2,
            height = size.height - inset * 2
        )
        val arcTopLeft = Offset(inset, inset)

        val trackStroke = Stroke(width = strokePx, cap = StrokeCap.Round)

        // 背景圓環
        drawArc(
            color = trackColor,
            startAngle = -90f,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = arcTopLeft,
            size = arcSize,
            style = trackStroke
        )

        // 進度圓環
        if (sweep > 0f) {
            drawArc(
                color = progressColor,
                startAngle = -90f,
                sweepAngle = sweep,
                useCenter = false,
                topLeft = arcTopLeft,
                size = arcSize,
                style = trackStroke
            )
        }

        // ★ 起始小點（藍點）
        if (drawTopTick) {
            // 讓小點半徑 < 線寬：預設 0.45 * 線寬，看起來會比進度條細
            val tickRadius = strokePx * tickRadiusScale.coerceIn(0f, 1f)

            // 12 點鐘方向（稍微往內縮，避免超出外圈）
            val centerOffset = Offset(
                x = center.x,
                y = center.y - radius + inset
            )

            drawCircle(
                color = tickColor,
                radius = tickRadius,
                center = centerOffset
            )
        }
    }
}
