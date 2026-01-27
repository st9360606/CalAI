package com.calai.bitecal.ui.home.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * WorkoutAddButton
 *
 * - outerSizeDp：外圈可點擊/灰色閃光區（建議 ≥ 48dp）
 * - innerSizeDp：黑色實心圓按鈕大小
 * - iconSizeDp：白色「＋」圖示大小
 * - 點擊：出現半透明深灰閃光 120ms
 */
@Composable
fun WorkoutAddButton(
    onClick: () -> Unit,
    outerSizeDp: Dp = 52.dp,  // ✅ 預設更大、更好按
    innerSizeDp: Dp = 36.dp,  // ✅ 黑圓加大
    iconSizeDp: Dp = 24.dp    // ✅ 白色「＋」加大
) {
    val scope = rememberCoroutineScope()
    var flashAlphaGoal by remember { mutableFloatStateOf(0f) }
    val animatedAlpha by animateFloatAsState(targetValue = flashAlphaGoal, label = "workoutAddFlash")
    val noRipple = remember { MutableInteractionSource() }

    Box(
        modifier = Modifier
            .size(outerSizeDp)
            .clickable(interactionSource = noRipple, indication = null) {
                scope.launch {
                    flashAlphaGoal = 0.4f
                    delay(120)
                    flashAlphaGoal = 0f
                }
                onClick()
            },
        contentAlignment = Alignment.Center
    ) {
        if (animatedAlpha > 0f) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(color = Color.Black.copy(alpha = animatedAlpha * 0.4f), shape = CircleShape)
            )
        }
        Box(
            modifier = Modifier
                .size(innerSizeDp)
                .background(color = Color(0xFF111114), shape = CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = "add workout",
                tint = Color.White,
                modifier = Modifier.size(iconSizeDp)
            )
        }
    }
}
