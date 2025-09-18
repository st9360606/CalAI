// app/src/main/java/com/calai/app/MainActivity.kt
package com.calai.app

import android.os.Bundle
import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.calai.app.ui.BiteCalApp
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.withFrameNanos

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Volatile private var splashUnlocked = false
    private val fallbackMs = 350L  // ← 統一數值

    private fun logPoint(tag: String) {
        Log.d("BootTrace", "${SystemClock.uptimeMillis()} : $tag")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        logPoint("onCreate:start")

        val splash = installSplashScreen()
        splash.setKeepOnScreenCondition { !splashUnlocked }

        super.onCreate(savedInstanceState)
        logPoint("after super.onCreate")

        // ★ 最遲 350ms 放行（保險絲）
        lifecycleScope.launch {
            delay(fallbackMs)
            splashUnlocked = true
            logPoint("fallback-${fallbackMs}ms")
        }

        setContent {
            // ★ 第一幀繪製完成即放行（最快路徑）
            FirstFrameUnlock {
                splashUnlocked = true
                logPoint("first-frame")
                // 告訴系統：我們的首屏已可互動（統計用途）
                // 注意：這不會「讓它更快」，是正確回報。
                window?.decorView?.post { reportFullyDrawn() }
            }
            logPoint("setContent-enter")
            BiteCalApp()
        }

        logPoint("onCreate:end")
    }
}

@Composable
private fun FirstFrameUnlock(onUnlock: () -> Unit) {
    LaunchedEffect(Unit) {
        withFrameNanos { /* 等待第一幀 */ }
        onUnlock()
    }
}
