package com.calai.app.ui.home.components

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
 * - outerSizeDp：外圈可點擊/灰色閃光區（比按鈕本體大）
 * - innerSizeDp：實際黑色圓按鈕尺寸
 * - 按下時：顯示一個半透明深灰圓，120ms 後淡掉
 *
 * 這是為了讓 Workout 卡片的 + 按鈕，行為/大小跟 Water 卡片的 RoundActionButton 一致。
 */
@Composable
fun WorkoutAddButton(
    onClick: () -> Unit,
    outerSizeDp: Dp = 36.dp,  // 觸控區 / 閃光圈，和水卡一致
    innerSizeDp: Dp = 26.dp   // 黑色實際按鈕大小，和水卡一致
) {
    val scope = rememberCoroutineScope()

    // flashAlphaTarget：我們想要的閃光強度 (0f ~ 0.4f)
    var flashAlphaTarget by remember { mutableFloatStateOf(0f) }

    // 用動畫平滑淡出閃光
    val animatedAlpha by animateFloatAsState(
        targetValue = flashAlphaTarget,
        label = "workoutAddFlash"
    )

    // 我們自己控制按下效果，不用 ripple
    val noRipple = remember { MutableInteractionSource() }

    Box(
        modifier = Modifier
            .size(outerSizeDp)
            .clickable(
                interactionSource = noRipple,
                indication = null
            ) {
                // 點擊時先閃深灰圓，再消掉
                scope.launch {
                    flashAlphaTarget = 0.4f   // 類似水卡：黑 * 0.4
                    delay(120)
                    flashAlphaTarget = 0f
                }

                // 呼叫外部行為（打開 Workout Tracker sheet）
                onClick()
            },
        contentAlignment = Alignment.Center
    ) {
        // 這一層是「半透明灰圓」閃一下的視覺
        if (animatedAlpha > 0f) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        color = Color.Black.copy(alpha = animatedAlpha * 0.4f),
                        shape = CircleShape
                    )
            )
        }

        // 內層真正的黑色圓按鈕 (和水卡 + 一樣視覺)
        Box(
            modifier = Modifier
                .size(innerSizeDp)
                .background(
                    color = Color(0xFF111114), // 你的全局深黑
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = "add workout",
                tint = Color.White
            )
        }
    }
}
