package com.calai.app.data.activity.util

import kotlin.math.roundToInt

/**
 * 估算「步行消耗熱量」：
 * kcal ≈ weightKg × steps × 0.0005
 *
 * 注意：這是估算值（非醫療/運動科學精準值）。
 */
object ActivityKcalEstimator {
    private const val COEFF = 0.0005

    fun estimateActiveKcal(weightKg: Double, steps: Long): Int {
        if (weightKg <= 0.0 || steps <= 0L) return 0
        return (weightKg * steps.toDouble() * COEFF).roundToInt()
    }
}
