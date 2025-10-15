package com.calai.app.ui.appentry

import android.os.SystemClock
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.calai.app.R
import com.calai.app.di.AppEntryPoint
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

@Composable
fun AppEntryRoute(
    onGoLanding: () -> Unit,
    onGoHome: () -> Unit
) {
    val appCtx = LocalContext.current.applicationContext
    val ep = remember(appCtx) { EntryPointAccessors.fromApplication(appCtx, AppEntryPoint::class.java) }
    val profileRepo = remember(ep) { ep.profileRepository() }
    val store = remember(ep) { ep.userProfileStore() }
    val auth = remember(ep) { ep.authRepository() }

    val snack = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        // 最短顯示時間（可依需求 800~1500ms）
        val MIN_SHOW_MS = 1200L
        val start = SystemClock.uptimeMillis()

        // 1) 讀本機快取與登入狀態（快）
        val (signedIn, hasLocalProfile) = withContext(Dispatchers.IO) {
            val s = runCatching { auth.isSignedIn() }.getOrElse { false }
            val h = runCatching { store.hasServerProfile() }.getOrElse { false }
            s to h
        }

        // 2) 背景校驗伺服器存在性（限制 800ms，不阻塞導頁）
        val existsDeferred = async(Dispatchers.IO) {
            withTimeoutOrNull(800) {
                runCatching { profileRepo.existsOnServer() }.getOrDefault(false)
            }
        }

        // 3) 確保畫面至少顯示 MIN_SHOW_MS
        val elapsed = SystemClock.uptimeMillis() - start
        if (elapsed < MIN_SHOW_MS) delay(MIN_SHOW_MS - elapsed)

        // 4) 一次性導頁（不再二次導頁，避免閃屏）
        if (signedIn && hasLocalProfile) onGoHome() else onGoLanding()

        // 5) 背景結果回寫快取（不導頁）
        existsDeferred.await()?.let { exists ->
            withContext(Dispatchers.IO) {
                runCatching { store.setHasServerProfile(exists) }
            }
            // 若想修正導頁邏輯，可在此依需要再導頁；預設為不導頁以避免畫面抖動。
        }
    }

    // ✅ 介面：白底＋置中 App 圖標（取代任何轉圈）
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_focus_spoon_foreground),
            contentDescription = null,
            modifier = Modifier.size(160.dp)
        )
    }

    SnackbarHost(hostState = snack)
}
