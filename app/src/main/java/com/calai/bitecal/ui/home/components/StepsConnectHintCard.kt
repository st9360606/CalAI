package com.calai.bitecal.ui.home.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun StepsConnectHintCard(
    text: String,
    modifier: Modifier = Modifier,
    corner: Dp = 16.dp,
    paddingH: Dp = 8.dp,
    paddingV: Dp = 10.dp,
    iconGap: Dp = 6.dp,
    textStyle: TextStyle = MaterialTheme.typography.bodySmall,
    maxLines: Int = 5,
    minHeight: Dp = 68.dp,
    icon: @Composable (() -> Unit)? = null,
    onClick: (() -> Unit)? = null
) {
    val interaction = remember { MutableInteractionSource() }
    val clickableMod = if (onClick != null) {
        Modifier.clickable(
            interactionSource = interaction,
            indication = null
        ) { onClick() }
    } else Modifier

    Card(
        modifier = modifier.then(clickableMod),
        shape = RoundedCornerShape(corner),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, Color(0xFFE5E7EB))
    ) {
        // ✅ 撐 minHeight + 垂直置中：上下留白一致
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = minHeight)
                .padding(horizontal = paddingH, vertical = paddingV),
            contentAlignment = Alignment.Center
        ) {
            // ✅ 整組靠左
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start
            ) {
                if (icon != null) {
                    icon()
                    Spacer(Modifier.width(iconGap))
                }

                Text(
                    text = text,
                    modifier = Modifier.weight(1f),
                    style = textStyle,
                    color = Color(0xFF111114),
                    maxLines = maxLines,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Start
                )
            }
        }
    }
}
