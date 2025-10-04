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
import com.calai.app.data.auth.store.UserProfileStore

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

        if (savedInstanceState == null) {
            lifecycleScope.launch {
                try {
                    UserProfileStore(applicationContext).clearOnboarding()
                    logPoint("onboarding:cleared")
                } catch (t: Throwable) {
                    Log.w("BootTrace", "clearOnboarding failed", t)
                }
            }
        }

        fuseJob = lifecycleScope.launch {
            delay(fallbackMs)
            unlockSplash("fallback-${fallbackMs}ms")
        }

        setContent {
            FirstFrameUnlock {
                unlockSplash("first-frame")
                window?.decorView?.post { reportFullyDrawn() }
            }
            logPoint("setContent-enter")
            BiteCalApp(hostActivity = this)
        }

        logPoint("onCreate:end")
    }

    override fun onResume() {
        super.onResume()
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
