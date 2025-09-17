package com.calai.app

import android.os.Bundle
import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.withFrameNanos
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.calai.app.ui.BiteCalApp
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    // 正向語意：是否已解鎖 Splash（true = 可以離開 Splash）
    @Volatile private var splashUnlocked = false

    private fun logPoint(tag: String) {
        android.util.Log.d("BootTrace", "${SystemClock.uptimeMillis()} : $tag")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        logPoint("onCreate:start")

        val splash = installSplashScreen()
        // 只看這個旗標；true 代表已解鎖 -> 不再顯示 Splash
        splash.setKeepOnScreenCondition { !splashUnlocked }

        super.onCreate(savedInstanceState)
        logPoint("after super.onCreate")

        // ★ 最遲 350ms 放行（比你原本 800ms 更積極）
        lifecycleScope.launch {
            delay(450)
            splashUnlocked = true
            logPoint("fallback-350ms")
        }

        setContent {
            // ★ 第一幀繪製完成即放行（最快路徑）
            FirstFrameUnlock {
                splashUnlocked = true
                logPoint("first-frame")
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
        // 等到第一個 frame 被繪製，代表 UI 已可呈現
        withFrameNanos { /* no-op */ }
        onUnlock()
    }
}
