package com.calai.app.ui.home.ui.workout.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

object WorkoutSheetSpec {
    const val HEIGHT_FRACTION: Float = 0.92f
}

@Composable
fun trackerSheetHeight(fraction: Float = WorkoutSheetSpec.HEIGHT_FRACTION): Dp {
    val h = LocalConfiguration.current.screenHeightDp
    return (h * fraction).dp
}
