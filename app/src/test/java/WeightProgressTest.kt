package com.calai.app.ui.home.ui.weight.logic

import com.calai.app.data.weight.api.WeightItemDto
import com.calai.app.ui.home.ui.weight.components.computeWeightProgress
import org.junit.Assert.assertEquals
import org.junit.Test

class WeightProgressTest {

    private fun dto(date: String, kg: Double) =
        WeightItemDto(logDate = date, weightKg = kg)

    @Test
    fun case1_loseWeight_80_to_60_new70_is50() {
        // 先只有一筆 → 0%
        var r = computeWeightProgress(
            timeSeries = listOf(dto("2025-01-01", 80.0)),
            currentKg = 80.0,
            goalKg = 60.0,
            profileWeightKg = 80.0
        )
        assertEquals(0f, r.fraction)

        // 新增一筆 70 → 50%
        r = computeWeightProgress(
            timeSeries = listOf(dto("2025-01-01", 80.0), dto("2025-01-02", 70.0)),
            currentKg = 70.0,
            goalKg = 60.0,
            profileWeightKg = 80.0
        )
        assertEquals(0.5f, r.fraction, 0.0001f)
    }

    @Test
    fun case2_gainGoal_80_to_100_new70_is0() {
        val r = computeWeightProgress(
            timeSeries = listOf(dto("2025-01-01", 80.0), dto("2025-01-02", 70.0)),
            currentKg = 70.0, goalKg = 100.0, profileWeightKg = 80.0
        )
        assertEquals(0f, r.fraction)
    }

    @Test
    fun case3_gainGoal_80_to_100_new90_is50() {
        val r = computeWeightProgress(
            timeSeries = listOf(dto("2025-01-01", 80.0), dto("2025-01-02", 90.0)),
            currentKg = 90.0, goalKg = 100.0, profileWeightKg = 80.0
        )
        assertEquals(0.5f, r.fraction, 0.0001f)
    }

    @Test
    fun case4_loseGoal_80_to_60_new90_is0() {
        val r = computeWeightProgress(
            timeSeries = listOf(dto("2025-01-01", 80.0), dto("2025-01-02", 90.0)),
            currentKg = 90.0, goalKg = 60.0, profileWeightKg = 80.0
        )
        assertEquals(0f, r.fraction)
    }
}
