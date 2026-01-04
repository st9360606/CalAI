package com.calai.app.ui.home.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 中央浮出的提示小卡（不模糊）- Small 版
 */
@Composable
fun StepsConnectHintCard(
    text: String,
    modifier: Modifier = Modifier,
    corner: Dp = 16.dp,
    paddingH: Dp = 8.dp,
    paddingV: Dp = 11.dp,   // ✅ 15 -> 11（上下內距縮小）
    iconGap: Dp = 8.dp,     // ✅ 10 -> 8（icon 與文字距離縮小一點）
    textStyle: TextStyle = MaterialTheme.typography.bodySmall,
    maxLines: Int = 4,
    minHeight: Dp = 68.dp,
    icon: @Composable (() -> Unit)? = null
) {
    Card(
        modifier = modifier.defaultMinSize(minHeight = minHeight),
        shape = RoundedCornerShape(corner),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, Color(0xFFE5E7EB))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = paddingH, vertical = paddingV), // ✅ 內距在這裡
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            if (icon != null) {
                icon()
                Spacer(Modifier.width(iconGap))
            } else {
                Spacer(Modifier.size(2.dp))
            }
            Text(
                text = text,
                style = textStyle,
                color = Color(0xFF111114),
                maxLines = maxLines,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
