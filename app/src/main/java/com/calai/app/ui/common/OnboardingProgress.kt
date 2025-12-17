package com.calai.app.ui.common

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.material3.LinearProgressIndicator

/**
 * Onboarding 共用進度條（圓角、固定厚度與色碼）
 *
 * @param stepIndex  目前第幾步（1-based，會自動夾在 [1, totalSteps]）
 * @param totalSteps 總步數（最少為 1）
 * @param height     預設 4.dp（統一厚度）
 * @param trackColor 預設 #EDEEF0（淺灰）
 * @param barColor   預設 #111114（近黑）
 * @param animate    是否進度動畫
 */
@Composable
fun OnboardingProgress(
    stepIndex: Int,
    totalSteps: Int,
    modifier: Modifier = Modifier,
    height: Dp = 4.dp,
    trackColor: Color = Color(0xFFEDEEF0),
    barColor: Color = Color(0xFF111114),
    animate: Boolean = true
) {
    val clampedTotal = totalSteps.coerceAtLeast(1)
    val clampedIndex = stepIndex.coerceIn(1, clampedTotal)
    val goal = clampedIndex / clampedTotal.toFloat()

    // Material3 建議使用 lambda 版本；若你專案版本較舊，可改用 progress = goal 的重載
    val progressVal by if (animate) {
        animateFloatAsState(targetValue = goal, label = "onboarding-progress")
    } else {
        rememberUpdatedState(goal)
    }

    LinearProgressIndicator(
        progress = { progressVal },
        trackColor = trackColor,
        color = barColor,
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(999.dp)) // 圓角外觀一致
    )
}
