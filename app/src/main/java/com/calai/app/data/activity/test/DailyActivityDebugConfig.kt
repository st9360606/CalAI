package com.calai.app.data.activity.test

import com.calai.app.BuildConfig

/**
 * 全域 debug 開關：
 * - 預設：Debug build 開、Release build 關
 * - 你也可以在 runtime 手動改成 true/false
 */
object DailyActivityDebugConfig {

    /**
     * ✅ 變數開關：true 才會印出 HC_* debug log
     */

    var enabled: Boolean = true
//    var enabled: Boolean = false
}
