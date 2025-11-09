// CardStyles.kt
package com.calai.app.ui.home.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

object CardStyles {
    // 卡片背景：微微偏暖白，避免和背景漸層黏成一片
    val Bg = Color(0xFFFDFDFE)

    // 邊框：比之前更深一點，幫助卡片邊界更明顯
    val BorderColor = Color(0xFFC3C8D1)
    val Border = BorderStroke(0.7.dp, BorderColor)

    // 統一圓角（Home 主要卡片都是 20.dp 圓角）
    val Corner = RoundedCornerShape(20.dp)

    // 統一陰影高度：讓卡片浮起來（立體感）
    val Elevation: Dp = 0.5.dp
}
