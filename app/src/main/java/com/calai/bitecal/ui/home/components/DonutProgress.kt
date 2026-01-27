package com.calai.bitecal.ui.home.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun DonutProgress(
    progress: Float,             // 0f..1f
    modifier: Modifier = Modifier,
    strokeWidth: Dp = 10.dp
) {
    val color = MaterialTheme.colorScheme.primary
    val bg = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
    Box(modifier = modifier) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val sw = strokeWidth.toPx()
            val size = Size(size.width - sw, size.height - sw)
            val topLeft = Offset(sw / 2f, sw / 2f)
            // 背景圈
            drawArc(
                color = bg,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = size,
                style = Stroke(width = sw, cap = StrokeCap.Round)
            )
            // 進度圈
            drawArc(
                color = color,
                startAngle = -90f,
                sweepAngle = 360f * progress.coerceIn(0f, 1f),
                useCenter = false,
                topLeft = topLeft,
                size = size,
                style = Stroke(width = sw, cap = StrokeCap.Round)
            )
        }
    }
}
