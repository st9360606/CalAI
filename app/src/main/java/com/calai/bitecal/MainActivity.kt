package com.calai.bitecal

import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.mutableStateOf
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.withFrameNanos
import com.calai.bitecal.data.profile.repo.UserProfileStore
import com.calai.bitecal.widget.BiteCalWidgetNavigationRequest
import com.calai.bitecal.widget.BiteCalWidgetPendingIntents

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Volatile private var splashUnlocked = false
    private var fuseJob: Job? = null
    private var widgetNavigationRequestSeq = 0L
    private val widgetNavigationRequestState = mutableStateOf<BiteCalWidgetNavigationRequest?>(null)

    private fun logPoint(tag: String) {
        Log.d("BootTrace", "${SystemClock.uptimeMillis()} : $tag")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        logPoint("onCreate:start")

        val splash = installSplashScreen()
        splash.setKeepOnScreenCondition { !splashUnlocked }

        super.onCreate(savedInstanceState)
        logPoint("after super.onCreate")
        consumeWidgetDestination(intent)

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

        setContent {
            FirstFrameUnlock {
                unlockSplash("first-frame")
                window?.decorView?.post { reportFullyDrawn() }
            }
            logPoint("setContent-enter")
            BiteCalApp(
                hostActivity = this,
                widgetNavigationRequest = widgetNavigationRequestState.value
            )
        }
        logPoint("onCreate:end")
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        consumeWidgetDestination(intent)
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

    private fun consumeWidgetDestination(intent: Intent?) {
        val destination = intent
            ?.getStringExtra(BiteCalWidgetPendingIntents.EXTRA_WIDGET_DESTINATION)
            ?.takeIf(BiteCalWidgetPendingIntents::isSupportedDestination)
            ?: return

        widgetNavigationRequestSeq += 1
        widgetNavigationRequestState.value = BiteCalWidgetNavigationRequest(
            id = widgetNavigationRequestSeq,
            destination = destination
        )
        intent.removeExtra(BiteCalWidgetPendingIntents.EXTRA_WIDGET_DESTINATION)
    }
}

@Composable
private fun FirstFrameUnlock(onUnlock: () -> Unit) {
    LaunchedEffect(Unit) {
        withFrameNanos { /* wait next choreographer frame */ }
        onUnlock()
    }
}
