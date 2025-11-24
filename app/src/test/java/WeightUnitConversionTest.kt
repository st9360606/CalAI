package com.calai.app.ui.home.ui.weight.logic

import com.calai.app.data.profile.repo.kgToLbs1
import com.calai.app.data.profile.repo.lbsToKg1
import org.junit.Assert.assertEquals
import org.junit.Test

class WeightUnitConversionTest {

    @Test
    fun `kgToLbs1 floors to 1 decimal`() {
        // 73.0 kg → 160.937... lbs → 160.9 lbs
        val lbs = kgToLbs1(73.0)
        assertEquals(160.9, lbs, 1e-6)
    }

    @Test
    fun `lbsToKg1 floors to 1 decimal`() {
        // 152.0 lbs → 68.946... kg → 68.9 kg
        val kg = lbsToKg1(152.0)
        assertEquals(68.9, kg, 1e-6)
    }
}
