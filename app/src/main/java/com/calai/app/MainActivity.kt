package com.calai.app

import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.withFrameNanos
import com.calai.app.ui.BiteCalApp

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Volatile private var splashUnlocked = false
    private val fallbackMs = 350L
    private var fuseJob: Job? = null

    private fun logPoint(tag: String) {
        Log.d("BootTrace", "${SystemClock.uptimeMillis()} : $tag")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        logPoint("onCreate:start")

        val splash = installSplashScreen()
        splash.setKeepOnScreenCondition { !splashUnlocked }

        super.onCreate(savedInstanceState)
        logPoint("after super.onCreate")

        // 350ms 保險絲：無論如何放行，避免極端卡住
        fuseJob = lifecycleScope.launch {
            delay(fallbackMs)
            unlockSplash("fallback-${fallbackMs}ms")
        }

        setContent {
            // 第一幀一出現就放行（最快路徑）
            FirstFrameUnlock {
                unlockSplash("first-frame")
                // 正確回報 TTFD：等到首屏可互動即回報
                window?.decorView?.post { reportFullyDrawn() }
            }
            logPoint("setContent-enter")
            // ★ 把 Activity 傳進 App 根組件
            BiteCalApp(hostActivity = this)
        }

        logPoint("onCreate:end")
    }

    override fun onResume() {
        super.onResume()
        // 後備：若因任何原因未解鎖（理論上不會），此處保險
        if (!splashUnlocked) unlockSplash("onResume-fallback")
    }

    override fun onDestroy() {
        fuseJob?.cancel()
        super.onDestroy()
    }

    private fun unlockSplash(reason: String) {
        if (!splashUnlocked) {
            splashUnlocked = true
            logPoint("unlock:$reason")
        }
    }
}

@Composable
private fun FirstFrameUnlock(onUnlock: () -> Unit) {
    LaunchedEffect(Unit) {
        withFrameNanos { /* wait next choreographer frame */ }
        onUnlock()
    }
}
