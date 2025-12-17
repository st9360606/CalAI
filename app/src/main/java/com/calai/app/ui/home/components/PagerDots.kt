package com.calai.app.ui.home.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * 簡單頁面指示器（小黑圓點）
 */
@Composable
fun PagerDots(
    count: Int,
    current: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.wrapContentWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(count) { i ->
            val active = (i == current)
            val size = if (active) 8.dp else 6.dp
            val alpha = animateFloatAsState(if (active) 1f else 0.5f, label = "dotAlpha").value
            val color = animateColorAsState(if (active) Color.Black else Color.Black.copy(alpha = 0.5f), label = "dotColor").value
            Surface(
                modifier = Modifier.size(size),
                color = color.copy(alpha = alpha),
                shape = CircleShape,
                content = {}
            )
        }
    }
}
