package com.calai.bitecal.data.profile.repo

import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.math.roundToInt

class HeightQuantizeTest {

    private fun quantize01(v: Double): Double = (v * 10.0).roundToInt() / 10.0

    @Test
    fun quantize_should_keep_182_2_even_if_float_like_value() {
        val floatLike = 182.1999969482421875 // 模擬 Float 182.2f 常見的實際值
        val q = quantize01(floatLike)
        assertEquals(182.2, q, 0.0)
    }

    @Test
    fun ui_cmTenths_roundToInt_should_not_drop_one_step() {
        val cmVal = 182.199999999
        val tenths = (cmVal * 10.0).roundToInt()
        assertEquals(1822, tenths)
    }
}
