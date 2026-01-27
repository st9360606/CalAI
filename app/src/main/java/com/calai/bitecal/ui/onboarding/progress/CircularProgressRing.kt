package com.calai.bitecal.ui.onboarding.progress

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 綠色圓形進度條（白底、中央顯示百分比）
 */
@Composable
fun CircularProgressRing(
    percent: Int,
    modifier: Modifier = Modifier,
    diameter: Dp = 220.dp,          // ← rename
    thickness: Dp = 18.dp,
    progressColor: Color = Color(0xFF66D36E),
    trackColor: Color = Color(0xFFE6E9EE),
    percentColor: Color = Color(0xFF111114),
) {
    val p = percent.coerceIn(0, 100)
    val sweep = (p / 100f) * 360f

    Box(modifier = modifier.size(diameter), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(diameter)) {
            val strokeWidth = thickness.toPx()
            val stroke = Stroke(width = strokeWidth, cap = StrokeCap.Round)

            val inset = strokeWidth / 2f
            val dPx = size.minDimension                      // ← this is DrawScope.size
            val arcSize = Size(dPx - strokeWidth, dPx - strokeWidth)

            // track
            drawArc(
                color = trackColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = stroke,
                size = arcSize,
                topLeft = Offset(inset, inset)
            )
            // progress
            if (sweep > 0f) {
                drawArc(
                    color = progressColor,
                    startAngle = -90f,
                    sweepAngle = sweep,
                    useCenter = false,
                    style = stroke,
                    size = arcSize,
                    topLeft = Offset(inset, inset)
                )
            }
        }

        Text(
            text = "$p%",
            color = percentColor,
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.titleLarge.copy(
                platformStyle = PlatformTextStyle(includeFontPadding = false),
                lineHeightStyle = LineHeightStyle(
                    alignment = LineHeightStyle.Alignment.Center,
                    trim = LineHeightStyle.Trim.Both
                )
            )
        )
    }
}

