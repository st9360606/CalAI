package com.calai.app.ui.home.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun ScanFab(onClick: () -> Unit) {
    FloatingActionButton(
        onClick = onClick,
        shape = CircleShape, // ✅ 強制圓形
        containerColor = Color(0xFF111114),
        contentColor = Color.White,
        elevation = FloatingActionButtonDefaults.elevation(
            defaultElevation = 6.dp,
            pressedElevation = 8.dp
        ),
        modifier = Modifier
            .size(68.dp)
            .offset(x = 6.dp, y = 6.dp)
    ) {
        ScanCameraIcon(
            modifier = Modifier.size(45.dp),
            frameRatio = 0.74f,
            cornerLenRatio = 0.28f,
            cornerRoundness = 0.6f,
            frameStrokeWidth = 1.6.dp,
            frameAlpha = 0.55f,
            plusSizeRatio = 0.49f,
            plusStrokeWidth = 2.0.dp
        )
    }
}

@Composable
fun ScanCameraIcon(
    modifier: Modifier = Modifier,
    frameRatio: Float = 0.74f,
    cornerLenRatio: Float = 0.70f,
    cornerRoundness: Float = 0.72f,
    frameStrokeWidth: Dp = 2.2.dp,
    frameAlpha: Float = 0.55f,
    plusSizeRatio: Float = 0.62f,
    plusStrokeWidth: Dp = 1.0.dp,
    color: Color = Color.White
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val d = minOf(w, h)

        val frameSize = d * frameRatio
        val left = (w - frameSize) / 2f
        val top = (h - frameSize) / 2f
        val right = left + frameSize
        val bottom = top + frameSize

        val cornerLen = frameSize * cornerLenRatio
        val radius = (cornerLen * cornerRoundness).coerceIn(0f, cornerLen)

        val frameStroke = Stroke(width = frameStrokeWidth.toPx(), cap = StrokeCap.Round)
        val frameColor = color.copy(alpha = frameAlpha)

        fun drawCornerPath(path: Path) {
            drawPath(path = path, color = frameColor, style = frameStroke)
        }

        drawCornerPath(
            Path().apply {
                moveTo(left + cornerLen, top)
                lineTo(left + radius, top)
                quadraticBezierTo(left, top, left, top + radius)
                lineTo(left, top + cornerLen)
            }
        )
        drawCornerPath(
            Path().apply {
                moveTo(right - cornerLen, top)
                lineTo(right - radius, top)
                quadraticBezierTo(right, top, right, top + radius)
                lineTo(right, top + cornerLen)
            }
        )
        drawCornerPath(
            Path().apply {
                moveTo(left, bottom - cornerLen)
                lineTo(left, bottom - radius)
                quadraticBezierTo(left, bottom, left + radius, bottom)
                lineTo(left + cornerLen, bottom)
            }
        )
        drawCornerPath(
            Path().apply {
                moveTo(right - cornerLen, bottom)
                lineTo(right - radius, bottom)
                quadraticBezierTo(right, bottom, right, bottom - radius)
                lineTo(right, bottom - cornerLen)
            }
        )

        val cx = w / 2f
        val cy = h / 2f
        val plusLen = frameSize * plusSizeRatio
        val plusStrokePx = plusStrokeWidth.toPx()

        drawLine(
            color = color,
            start = Offset(cx - plusLen / 2f, cy),
            end = Offset(cx + plusLen / 2f, cy),
            strokeWidth = plusStrokePx,
            cap = StrokeCap.Round
        )
        drawLine(
            color = color,
            start = Offset(cx, cy - plusLen / 2f),
            end = Offset(cx, cy + plusLen / 2f),
            strokeWidth = plusStrokePx,
            cap = StrokeCap.Round
        )
    }
}
