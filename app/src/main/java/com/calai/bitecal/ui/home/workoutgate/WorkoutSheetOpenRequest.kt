package com.calai.bitecal.ui.home.workoutgate

object WorkoutSheetOpenRequest {
    const val ConsumedTick: Long = 0L

    fun shouldOpen(tick: Long): Boolean = tick > ConsumedTick
}
