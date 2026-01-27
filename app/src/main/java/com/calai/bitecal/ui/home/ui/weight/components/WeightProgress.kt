package com.calai.bitecal.ui.home.ui.weight.components

import com.calai.bitecal.data.weight.api.WeightItemDto
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
 *    若沒有，才退回：timeSeries 最早一筆 → 再退 current。
 * 2) 沒有「新日誌」前（timeSeries 空、或只有 1 筆且等於 start）→ 0%。
 * 3) 自動判斷方向；往反方向移動 → 0%；到達目標 → 100%。
 */
fun computeWeightProgress(
    timeSeries: List<WeightItemDto>,
    currentKg: Double?,
    goalKg: Double?,
    profileWeightKg: Double?
): ProgressResult {
    // 沒有 current 或 goal → 沒辦法算
    if (currentKg == null || goalKg == null) {
        return ProgressResult(null, currentKg, goalKg, 0f)
    }

    // ★ 1) 起點一律以 user_profiles.weight_kg 為主
    val earliestFromSeries = timeSeries
        .minByOrNull { it.logDate }    // 最早日期那筆
        ?.weightKg

    val startKg = profileWeightKg           // user_profiles.weight_kg
        ?: earliestFromSeries              // 沒有 profile 才退 timeSeries 最早一筆
        ?: currentKg                       // 最後保底

    // ★ 2) 沒有「新日誌」：完全沒 history，
    //    或只有一筆且 weight 等於 start（代表目前只是起點）
    val hasAnyLogs = timeSeries.isNotEmpty()
    if (!hasAnyLogs || (timeSeries.size == 1 && timeSeries.first().weightKg == startKg)) {
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

/**
 * ✅ 只給「ACHIEVED xx% OF GOAL」用（LBS 模式）
 * 規則完全比照 computeWeightProgress 的邏輯，但改用 lbs：
 * 1) start：profileWeightLbs 優先；否則最早一筆 series.weightLbs；最後保底 currentLbs
 * 2) 沒有新日誌 → 0%
 * 3) 自動判斷方向；往反方向 → 0%；到達 → 100%
 *
 * 回傳 null 表示 LBS 目前資料不足（current/goal 缺），呼叫端可 fallback 回 kg fraction。
 */
internal fun computeWeightProgressFractionLbs(
    timeSeries: List<WeightItemDto>,
    currentLbs: Double?,
    goalLbs: Double?,
    profileWeightLbs: Double?
): Float? {
    if (currentLbs == null || goalLbs == null) return null

    val earliestFromSeriesLbs = timeSeries
        .minByOrNull { it.logDate }
        ?.weightLbs

    val startLbs = profileWeightLbs ?: earliestFromSeriesLbs ?: currentLbs

    // 沒有新日誌：timeSeries 空，或只有一筆且等於 start
    val hasAnyLogs = timeSeries.isNotEmpty()
    if (!hasAnyLogs || (timeSeries.size == 1 && timeSeries.first().weightLbs == startLbs)) {
        return 0f
    }

    val total = abs(goalLbs - startLbs)
    if (total == 0.0) {
        return if (currentLbs == goalLbs) 1f else 0f
    }

    val moved = if (goalLbs < startLbs) {
        // 減重：往下掉才算
        max(0.0, startLbs - currentLbs)
    } else {
        // 增重：往上升才算
        max(0.0, currentLbs - startLbs)
    }

    return (moved / total).toFloat().coerceIn(0f, 1f)
}
