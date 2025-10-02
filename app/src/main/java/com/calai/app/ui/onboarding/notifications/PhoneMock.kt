package com.calai.app.ui.onboarding.notifications

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BatteryFull
import androidx.compose.material.icons.outlined.SignalCellular4Bar
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 可商用的「手機樣式外框」。
 */
@Composable
fun PhoneMock(
    modifier: Modifier = Modifier,
    style: PhoneMockStyle = PhoneMockStyle.PixelLike,
    showStatusBar: Boolean = true,
    timeText: String = "10:09",
    // ↓ 調薄邊框：把 contentPadding 調小（如 8.dp），把 bezelStrokeWidth=0.dp
    contentPadding: Dp = 8.dp,
    bezelStrokeWidth: Dp = 0.dp,
    screenCorner: Dp = if (style == PhoneMockStyle.iOSLike) 32.dp else 26.dp,
    bezelCorner: Dp = if (style == PhoneMockStyle.iOSLike) 40.dp else 32.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    val bezelColor = if (style == PhoneMockStyle.iOSLike) Color(0xFF111114) else Color(0xFF0F1113)
    val bezelBorder = if (style == PhoneMockStyle.iOSLike) Color(0xFF2B2E33) else Color(0xFF23262B)

    Surface(
        modifier = modifier,
        color = bezelColor,
        shape = RoundedCornerShape(bezelCorner),
        tonalElevation = 0.dp,
        shadowElevation = 14.dp
    ) {
        var inner = Modifier
            .then(
                if (bezelStrokeWidth > 0.dp)
                    Modifier.border(bezelStrokeWidth, bezelBorder.copy(alpha = 0.28f), RoundedCornerShape(bezelCorner))
                else Modifier
            )
            .padding(contentPadding) // 外框與螢幕距離（越小越薄）
            .clip(RoundedCornerShape(screenCorner))
            .background(Brush.verticalGradient(listOf(Color(0xFFF6F7F9), Color(0xFFEDEFF2))))

        Box(inner) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (showStatusBar) {
                    MockStatusBar(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        timeText = timeText
                    )
                }
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    content = content
                )
            }
        }
    }
}

@Composable
fun MockStatusBar(
    modifier: Modifier = Modifier,
    timeText: String = "10:09",
    textColor: Color = Color(0xFF111114).copy(alpha = 0.85f)
) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Text(text = timeText, fontSize = 14.sp, color = textColor)
        Spacer(Modifier.weight(1f))
        Icon(Icons.Outlined.SignalCellular4Bar, contentDescription = "Signal", tint = textColor)
        Spacer(Modifier.width(8.dp))
        Icon(Icons.Outlined.Wifi, contentDescription = "Wi-Fi", tint = textColor)
        Spacer(Modifier.width(8.dp))
        Icon(Icons.Outlined.BatteryFull, contentDescription = "Battery", tint = textColor)
    }
}

enum class PhoneMockStyle { PixelLike, iOSLike }
