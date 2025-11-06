package com.calai.app.ui.home.ui.workout.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

object WorkoutSheetSpec {
    // 想再低：改 0.84f / 0.82f
    const val HEIGHT_FRACTION: Float = 0.86f
}

@Composable
fun trackerSheetHeight(fraction: Float = WorkoutSheetSpec.HEIGHT_FRACTION): Dp {
    val h = LocalConfiguration.current.screenHeightDp
    return (h * fraction).dp
}