package com.calai.bitecal.ui.home.ui.weight.components

import kotlin.math.abs

/**
 * 把 points 的 x(normalized 0..1) 轉成螢幕座標 X(px)
 * - chartWidthPx：整個 Box 寬度（含左右 padding）
 * - start/endPaddingPx：你 Canvas 內縮的左右 padding
 */
internal fun buildPointCentersPx(
    pointsXNorm: List<Float>,
    chartWidthPx: Float,
    startPaddingPx: Float,
    endPaddingPx: Float
): List<Float> {
    if (chartWidthPx <= 0f) return emptyList()
    val innerWidth = (chartWidthPx - startPaddingPx - endPaddingPx).coerceAtLeast(1f)

    return pointsXNorm.map { xNorm ->
        startPaddingPx + xNorm.coerceIn(0f, 1f) * innerWidth
    }
}

/**
 * 給定遞增的 centersPx，回傳 rawX 最接近的 index（binary search）
 */
internal fun nearestIndexByX(centersPx: List<Float>, rawX: Float): Int? {
    if (centersPx.isEmpty()) return null

    val x = rawX.coerceIn(centersPx.first(), centersPx.last())

    // 手寫 binary search（避免依賴 stdlib 的 binarySearch overload）
    var lo = 0
    var hi = centersPx.lastIndex
    while (lo <= hi) {
        val mid = (lo + hi) ushr 1
        val v = centersPx[mid]
        when {
            v < x -> lo = mid + 1
            v > x -> hi = mid - 1
            else -> return mid
        }
    }

    val ins = lo
    val left = (ins - 1).coerceAtLeast(0)
    val right = ins.coerceAtMost(centersPx.lastIndex)

    return if (abs(centersPx[left] - x) <= abs(centersPx[right] - x)) left else right
}
