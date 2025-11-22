package com.calai.app.ui.home.ui.weight.components

import com.calai.app.data.weight.api.WeightItemDto
import kotlin.math.abs
import kotlin.math.max

data class ProgressResult(
    val startKg: Double?,
    val currentKg: Double?,
    val goalKg: Double?,
    val fraction: Float
)

/**
 * 規則（新版）：
 * 1) start：一律以 user_profiles.weight_kg（profileWeightKg）為主；
 *    若沒有，才退回：timeseries 最早一筆 → 再退 current。
 * 2) 沒有「新日誌」前（timeseries 空、或只有 1 筆且等於 start）→ 0%。
 * 3) 自動判斷方向；往反方向移動 → 0%；到達目標 → 100%。
 */
fun computeWeightProgress(
    timeseries: List<WeightItemDto>,
    currentKg: Double?,
    goalKg: Double?,
    profileWeightKg: Double?
): ProgressResult {
    // 沒有 current 或 goal → 沒辦法算
    if (currentKg == null || goalKg == null) {
        return ProgressResult(null, currentKg, goalKg, 0f)
    }

    // ★ 1) 起點一律以 user_profiles.weight_kg 為主
    val earliestFromSeries = timeseries
        .minByOrNull { it.logDate }    // 最早日期那筆
        ?.weightKg

    val startKg = profileWeightKg           // user_profiles.weight_kg
        ?: earliestFromSeries              // 沒有 profile 才退 timeseries 最早一筆
        ?: currentKg                       // 最後保底

    // 這裡基本上不會是 null，但保個險
    if (startKg == null) {
        return ProgressResult(null, currentKg, goalKg, 0f)
    }

    // ★ 2) 沒有「新日誌」：完全沒 history，
    //    或只有一筆且 weight 等於 start（代表目前只是起點）
    val hasAnyLogs = timeseries.isNotEmpty()
    if (!hasAnyLogs || (timeseries.size == 1 && timeseries.first().weightKg == startKg)) {
        return ProgressResult(startKg, currentKg, goalKg, 0f)
    }

    // ★ 3) 總距離：起點到目標
    val total = abs(goalKg - startKg)
    if (total == 0.0) {
        // 起點就等於目標 → 若 current 也到目標，視為 100%，否則 0%
        val frac = if (currentKg == goalKg) 1f else 0f
        return ProgressResult(startKg, currentKg, goalKg, frac)
    }

    // ★ 4) 目前移動距離（只算正確方向）
    val moved = if (goalKg < startKg) {
        // 減重：往下掉才算進度
        max(0.0, startKg - currentKg)
    } else {
        // 增重：往上升才算進度
        max(0.0, currentKg - startKg)
    }

    val fraction = (moved / total).toFloat().coerceIn(0f, 1f)

    return ProgressResult(
        startKg = startKg,
        currentKg = currentKg,
        goalKg = goalKg,
        fraction = fraction
    )
}
