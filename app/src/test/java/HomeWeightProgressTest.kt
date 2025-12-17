package com.calai.app.ui.home.ui.weight.logic

import com.calai.app.ui.home.computeHomeWeightProgress
import org.junit.Assert.assertEquals
import org.junit.Test

class HomeWeightProgressTest {

    @Test
    fun scenario1_loss_80_to_60_latest75() {
        val p = computeHomeWeightProgress(
            profileWeightKg = 80.0,
            goalWeightKg = 60.0,
            latestWeightKg = 75.0
        )
        assertEquals(0.25f, p, 0.0001f)
    }

    @Test
    fun scenario2_gain_80_to_100_latest85() {
        val p = computeHomeWeightProgress(
            profileWeightKg = 80.0,
            goalWeightKg = 100.0,
            latestWeightKg = 85.0
        )
        assertEquals(0.25f, p, 0.0001f)
    }

    @Test
    fun scenario3_no_timeseries() {
        val p = computeHomeWeightProgress(
            profileWeightKg = 80.0,
            goalWeightKg = 60.0,
            latestWeightKg = null    // 沒有 timeseries
        )
        assertEquals(0f, p, 0.0001f)
    }
}
