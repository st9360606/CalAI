package com.calai.app.ui.landing

import android.app.Activity
import android.app.Activity.OVERRIDE_TRANSITION_CLOSE
import android.app.Activity.OVERRIDE_TRANSITION_OPEN
import android.app.ActivityOptions
import android.content.Intent
import android.os.Build

/** 以無動畫的方式重啟當前 Activity，避免閃黑與轉場閃爍 */
fun Activity.restartNoAnimation() {
    // 取回當前 intent；加上 NO_ANIMATION 旗標（雙保險）
    val i: Intent = intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)

    // 先結束自己，並關閉「關閉動畫」
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        // API 34+
        overrideActivityTransition(OVERRIDE_TRANSITION_CLOSE, 0, 0)
    } else {
        @Suppress("DEPRECATION")
        overridePendingTransition(0, 0)
    }
    finish()

    // 以「自訂動畫 = 0」重新啟動自己（開啟動畫也設為 0）
    val options = ActivityOptions.makeCustomAnimation(this, 0, 0)
    startActivity(i, options.toBundle())

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        overrideActivityTransition(OVERRIDE_TRANSITION_OPEN, 0, 0)
    } else {
        @Suppress("DEPRECATION")
        overridePendingTransition(0, 0)
    }
}
