package com.calai.bitecal.ui.nav

import androidx.navigation.NavBackStackEntry
import com.calai.bitecal.ui.home.ui.camera.CameraMode

/**
 * 從 savedStateHandle 取出 camera_mode 並「消費一次」(取完即清除)。
 *
 * - 回傳 CameraMode?（找不到或不合法就 null）
 * - 取完會 remove，避免一直覆寫使用者手動切換
 */
fun NavBackStackEntry.consumeCameraModeRequest(
    key: String = "camera_mode"
): CameraMode? {
    val raw: String = savedStateHandle.get<String>(key) ?: return null
    val mode: CameraMode? = runCatching { CameraMode.valueOf(raw) }.getOrNull()

    // ✅ 用完就清掉：避免回到 Camera 後一直被覆寫
    savedStateHandle.remove<String>(key)

    return mode
}
