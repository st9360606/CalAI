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
 * 規則：
 * 1) start：timeseries 最早一筆；若沒有則使用 profileWeightKg（再退 current）。
 * 2) 沒有「新日誌」前（timeseries 空、或只有 1 筆且等於 current）→ 0%。
 * 3) 自動判斷方向；往反方向移動 → 0%；到達目標 → 100%。
 */
fun computeWeightProgress(
    timeseries: List<WeightItemDto>,
    currentKg: Double?,
    goalKg: Double?,
    profileWeightKg: Double?
): ProgressResult {
    if (currentKg == null || goalKg == null) {
        return ProgressResult(null, currentKg, goalKg, 0f)
    }

    val hasAnyLogs = timeseries.isNotEmpty()
    val startKg = if (hasAnyLogs) {
        timeseries.minByOrNull { it.logDate }?.weightKg
    } else {
        profileWeightKg ?: currentKg
    }

    if (startKg == null) return ProgressResult(null, currentKg, goalKg, 0f)

    // 沒新日誌 → 0%
    if (!hasAnyLogs || (timeseries.size == 1 && timeseries.first().weightKg == currentKg)) {
        return ProgressResult(startKg, currentKg, goalKg, 0f)
    }

    val total = abs(goalKg - startKg)
    if (total == 0.0) {
        return ProgressResult(startKg, currentKg, goalKg, if (currentKg == goalKg) 1f else 0f)
    }

    val moved = if (goalKg < startKg) {
        // 減重：只計算往下降的進度
        max(0.0, startKg - currentKg)
    } else {
        // 增重：只計算往上升的進度
        max(0.0, currentKg - startKg)
    }

    val fraction = (moved / total).toFloat().coerceIn(0f, 1f)
    return ProgressResult(startKg, currentKg, goalKg, fraction)
}
