package com.calai.app.ui.home.ui.water.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
import androidx.compose.ui.platform.LocalLayoutDirection

@Composable
fun UnitSwitchLabeled(
    checked: Boolean,                         // true = 右側 (ml)
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    width: Dp = 92.dp,
    height: Dp = 40.dp,
    padding: Dp = 3.dp,
    leftLabel: String = "oz",
    rightLabel: String = "ml",
    trackBase: Color = Color(0xFF888888).copy(alpha = 0.25f),
    trackOn: Color = Color(0xFF111114),
    textOn: Color = Color.White,
    textOff: Color = Color(0xFF111114),
    textStyle: TextStyle = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
) {
    val layoutDir = LocalLayoutDirection.current
    val isRtl = layoutDir == LayoutDirection.Rtl

    val corner = RoundedCornerShape(height / 2)
    val interaction = remember { MutableInteractionSource() }

    val half = (width - padding * 2) / 2
    val targetX = if (checked.xor(isRtl)) padding + half else padding
    val pillX by animateDpAsState(targetValue = targetX, label = "pillX")

    val stateText = if (checked) rightLabel else leftLabel

    Box(
        modifier = modifier
            .size(width, height)
            .clip(corner)
            .background(trackBase)
            .semantics(mergeDescendants = true) {
                role = Role.Switch
                contentDescription = "Unit switch"
                stateDescription = stateText
            }
    ) {
        // 選取膠囊（半寬）
        Box(
            modifier = Modifier
                .offset(x = pillX)
                .padding(vertical = padding)
                .width(half)
                .fillMaxHeight()
                .clip(corner)
                .background(trackOn)
        )

        // 兩個可點擊半區：不再使用 matchParentSize，改用 fillMaxSize 與 weight
        Row(
            modifier = Modifier.fillMaxSize()
        ) {
            // 左半（oz）
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clickable(
                        interactionSource = interaction,
                        indication = null
                    ) {
                        val wantChecked = if (isRtl) true else false
                        if (checked != wantChecked) onCheckedChange(wantChecked)
                    },
                contentAlignment = Alignment.Center
            ) {
                val activeLeft = !checked.xor(isRtl)
                Text(
                    text = if (isRtl) rightLabel else leftLabel,
                    style = textStyle,
                    color = if (activeLeft) textOn else textOff
                )
            }

            // 右半（ml）
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clickable(
                        interactionSource = interaction,
                        indication = null
                    ) {
                        val wantChecked = if (isRtl) false else true
                        if (checked != wantChecked) onCheckedChange(wantChecked)
                    },
                contentAlignment = Alignment.Center
            ) {
                val activeRight = checked.xor(isRtl)
                Text(
                    text = if (isRtl) leftLabel else rightLabel,
                    style = textStyle,
                    color = if (activeRight) textOn else textOff
                )
            }
        }
    }
}
