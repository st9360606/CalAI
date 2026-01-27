package com.calai.bitecal.ui.home.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

object CardStyles {
    // 卡片背景：微暖白，避免與背景漸層黏成一片
    val Bg = Color(0xFFFDFDFE)

    // 邊框：更深一點，讓卡片邊界更明顯
    val BorderColor = Color(0xFFE0E2E6)
    val Border = BorderStroke(1.dp, BorderColor)

    // 統一圓角（Home 主要卡片 20.dp）
    val Corner = RoundedCornerShape(20.dp)

    // 輕量立體感
    val Elevation: Dp = 0.7.dp
}
