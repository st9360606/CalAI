package com.calai.app.ui.home.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun StepsConnectHintCard(
    text: String,
    modifier: Modifier = Modifier,
    corner: Dp = 16.dp,
    paddingH: Dp = 8.dp,
    paddingV: Dp = 11.dp,
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
        modifier = modifier
            .defaultMinSize(minHeight = minHeight)
            .then(clickableMod),
        shape = RoundedCornerShape(corner),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, Color(0xFFE5E7EB))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = paddingH, vertical = paddingV),
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
