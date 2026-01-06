package com.calai.app.data.activity.test

import com.calai.app.data.activity.sync.DataOriginPrefs

/**
 * 把挑來源邏輯抽出來，方便測試與 debug。
 */
object OriginPicker {

    /**
     * 規則：
     * 1) 先挑 preferred 中 steps > 0 的（忽略 "android" 這個 any-source 占位）
     * 2) 若 preferred 包含 "android"：允許任何來源，挑 steps 最大（可能為 0）
     * 3) 否則：只挑 preferred 之中存在的（即使為 0），都沒有才 null
     */
    fun choosePreferredOrigin(
        byOrigin: Map<String, Long>,
        preferred: List<String>
    ): String? {
        if (byOrigin.isEmpty()) return null

        fun stepsOf(pkg: String) = byOrigin[pkg]

        // 1) 先依偏好找：但必須 >0
        for (pkg in preferred) {
            if (pkg == DataOriginPrefs.ON_DEVICE_ANDROID) continue
            val v = stepsOf(pkg)
            if (v != null && v > 0L) return pkg
        }

        // 2) 允許任何來源：選 steps 最大（可能 0）
        if (preferred.contains(DataOriginPrefs.ON_DEVICE_ANDROID)) {
            return byOrigin.maxByOrNull { it.value }?.key
        }

        // 3) 不允許 any-source：挑偏好存在的（即使 0）
        for (pkg in preferred) {
            if (pkg == DataOriginPrefs.ON_DEVICE_ANDROID) continue
            if (byOrigin.containsKey(pkg)) return pkg
        }

        return null
    }
}
