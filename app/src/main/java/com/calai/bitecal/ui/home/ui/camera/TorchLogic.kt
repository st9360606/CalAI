package com.calai.bitecal.ui.home.ui.camera

/**
 * Torch 切換規則：
 * - 只要不符合條件（相機未啟用 / 沒相機權限 / 沒閃光燈），一律回到 false
 * - 符合條件才允許切換 (取反)
 */
internal fun nextTorchState(
    current: Boolean,
    enableCameraX: Boolean,
    hasCameraPerm: Boolean,
    hasFlashUnit: Boolean
): Boolean {
    if (!enableCameraX) return false
    if (!hasCameraPerm) return false
    if (!hasFlashUnit) return false
    return !current
}
