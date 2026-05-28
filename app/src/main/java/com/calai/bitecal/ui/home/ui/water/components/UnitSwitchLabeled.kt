package com.calai.bitecal.ui.home.ui.water.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.calai.bitecal.ui.common.haptic.biteCalClickable

@Composable
fun UnitSwitchLabeled(
    checked: Boolean,                         // true = 右側 (ml)
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    width: Dp = 92.dp,
    height: Dp = 30.dp,
    padding: Dp = 3.dp,                       // 外框上下左右留白
    leftLabel: String = "oz",
    rightLabel: String = "ml",
    trackBase: Color = Color(0xFF888888).copy(alpha = 0.25f),
    trackOn: Color = Color(0xFF111114),
    textOn: Color = Color.White,              // 膠囊內（選中）
    textOff: Color = Color(0xFF111114),       // 外層（未選）
    textStyle: TextStyle = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
    pillExtraWidth: Dp = 6.dp,                // 膠囊比半寬多一點
    labelPadding: Dp = 6.dp                   // 外層文字左右內距，避免靠中線太擠
) {
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val outerCorner = RoundedCornerShape(height / 2)
    val interaction = remember { MutableInteractionSource() }

    // 幾何
    val innerWidth = width - padding * 2
    val half = innerWidth / 2
    val pillWidth = (half + pillExtraWidth).coerceIn(half, innerWidth)
    val pillHeight = height - padding * 2
    val pillCorner = RoundedCornerShape(pillHeight / 2)

    // 位置：左=padding；右=padding+(innerWidth - pillWidth)
    val baseLeftX = padding
    val baseRightX = padding + (innerWidth - pillWidth)
    val goalX = if (checked.xor(isRtl)) baseRightX else baseLeftX
    val pillX by animateDpAsState(targetValue = goalX, label = "pillX")

    val stateText = if (checked) rightLabel else leftLabel

    Box(
        modifier = modifier
            .size(width, height)
            .clip(outerCorner)
            .background(trackBase)
            .semantics(mergeDescendants = true) {
                role = Role.Switch
                contentDescription = "Unit switch"
                stateDescription = stateText
            }
    ) {
        // ① 外層底文字（兩側都畫，未選側會顯示；選側會被膠囊蓋住）
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = padding),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 左側外層文字
            Box(
                modifier = Modifier
                    .width(half)
                    .fillMaxHeight(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isRtl) rightLabel else leftLabel,
                    style = textStyle,
                    color = textOff,
                    maxLines = 1,
                    modifier = Modifier.padding(horizontal = labelPadding)
                )
            }
            // 右側外層文字
            Box(
                modifier = Modifier
                    .width(half)
                    .fillMaxHeight(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isRtl) leftLabel else rightLabel,
                    style = textStyle,
                    color = textOff,
                    maxLines = 1,
                    modifier = Modifier.padding(horizontal = labelPadding)
                )
            }
        }

        // ② 選取膠囊（黑）＋ 置中白字（只顯示當前選中標籤）
        Box(
            modifier = Modifier
                .offset(x = pillX)
                .align(Alignment.CenterStart)    // 垂直置中
                .width(pillWidth)
                .fillMaxHeight()
                .padding(vertical = padding)     // 與外框保留上下空氣
                .clip(pillCorner)
                .background(trackOn),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stateText,
                style = textStyle,
                color = textOn,
                maxLines = 1
            )
        }

        // ③ 點擊覆蓋層（透明，專職處理互動）
        Row(modifier = Modifier.fillMaxSize()) {
            // 左半（oz）
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .biteCalClickable(
                        interactionSource = interaction,
                        indication = null
                    ) {
                        val wantChecked = if (isRtl) true else false
                        if (checked != wantChecked) onCheckedChange(wantChecked)
                    }
            )
            // 右半（ml）
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .biteCalClickable(
                        interactionSource = interaction,
                        indication = null
                    ) {
                        val wantChecked = if (isRtl) false else true
                        if (checked != wantChecked) onCheckedChange(wantChecked)
                    }
            )
        }
    }
}
