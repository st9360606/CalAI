package com.calai.bitecal.ui.appentry

import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.calai.bitecal.R
import com.calai.bitecal.di.AppEntryPoint
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

@Composable
fun AppEntryRoute(
    onGoLanding: () -> Unit,
    onGoHome: () -> Unit
) {
    val context = LocalContext.current
    val appCtx = context.applicationContext

    val ep = remember(appCtx) {
        EntryPointAccessors.fromApplication(appCtx, AppEntryPoint::class.java)
    }
    val profileRepo = remember(ep) { ep.profileRepository() }
    val store = remember(ep) { ep.userProfileStore() }
    val auth = remember(ep) { ep.authRepository() }

    // ✅ 只導頁一次（防止重組/前景回來造成二次 navigate）
    var navigated by rememberSaveable { mutableStateOf(false) }

    // ✅ 動畫：淡入 + 縮放
    val alphaAnim = remember { Animatable(0f) }
    val scaleAnim = remember { Animatable(0.8f) }

    // （可選）提示用：注意 showSnackbar 會 suspend，所以要 launch 不要擋住導頁
    val snack = remember { SnackbarHostState() }

    // ✅（可選）系統列顏色：更建議放 Activity.onCreate，但放這裡也行
    LaunchedEffect(Unit) {
        (context as? ComponentActivity)?.enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.White.toArgb(), Color.White.toArgb()),
            navigationBarStyle = SystemBarStyle.light(Color.White.toArgb(), Color.White.toArgb())
        )
    }

    LaunchedEffect(Unit) {
        // A) 動畫並行（不阻塞）
        launch {
            alphaAnim.animateTo(1f, tween(durationMillis = 400))
        }
        launch {
            scaleAnim.animateTo(1f, tween(durationMillis = 800, easing = FastOutSlowInEasing))
        }

        // B) Entry 判斷
        val MIN_SHOW_MS = 600L
        val start = SystemClock.uptimeMillis()

        // 1) 讀登入狀態 + 本機快取（快）
        val (signedIn, localProfileExists) = withContext(Dispatchers.IO) {
            val s = runCatching { auth.isSignedIn() }.getOrElse { false }
            val h = runCatching { store.hasServerProfile() }.getOrElse { false }
            s to h
        }

        // 2) 只有在「已登入但本機快取不存在」時，才短暫等待 server 結果來避免誤導至 Landing
        val shouldCheckServer = signedIn && !localProfileExists

        val serverProfileExists: Boolean? = if (shouldCheckServer) {
            withContext(Dispatchers.IO) {
                withTimeoutOrNull(1500) {
                    runCatching { profileRepo.existsOnServer() }.getOrNull()
                }
            }
        } else null

        // 3) 若 server 有結果就回寫快取（不導頁）
        if (serverProfileExists != null) {
            withContext(Dispatchers.IO) {
                runCatching { store.setHasServerProfile(serverProfileExists) }
            }
        } else if (shouldCheckServer) {
            // 可選：網路慢提示（不阻塞）
            launch {
                runCatching {
                    snack.showSnackbar(
                        message = "Network slow. Using cached status.",
                        duration = SnackbarDuration.Short
                    )
                }
            }
        }

        // 4) 最終判斷：server 優先，否則 fallback 本機
        val finalProfileExists = serverProfileExists ?: localProfileExists

        // 5) 最短顯示時間
        val elapsed = SystemClock.uptimeMillis() - start
        if (elapsed < MIN_SHOW_MS) delay(MIN_SHOW_MS - elapsed)

        // 6) 導頁（只做一次）
        if (!navigated) {
            navigated = true
            if (signedIn && finalProfileExists) onGoHome() else onGoLanding()
        }
    }

    // UI
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_focus_spoon_foreground),
            contentDescription = null,
            modifier = Modifier
                .size(160.dp)
                .scale(scaleAnim.value)
                .alpha(alphaAnim.value)
        )

        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.BottomCenter
        ) {
            SnackbarHost(hostState = snack)
        }
    }
}
