package com.calai.app.ui.appentry

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.background
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
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

@Composable
fun AppEntryRoute(
    onGoLanding: () -> Unit,
    onGoHome: () -> Unit
) {
    val appCtx = LocalContext.current.applicationContext
    val ep = remember(appCtx) {
        EntryPointAccessors.fromApplication(appCtx, AppEntryPoint::class.java)
    }
    val profileRepo = remember(ep) { ep.profileRepository() }
    val store = remember(ep) { ep.userProfileStore() }
    val auth = remember(ep) { ep.authRepository() }

    val snack = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        // 1) Fast path：本地快取 + 登入狀態（極快）
        val (signedIn, hasLocalProfile) = withContext(Dispatchers.IO) {
            val s = runCatching { auth.isSignedIn() }.getOrElse { false }
            val h = runCatching { store.hasServerProfile() }.getOrElse { false }
            s to h
        }
        if (signedIn && hasLocalProfile) onGoHome() else onGoLanding()

        // 2) Background verify（最長 800ms，不阻塞導頁）
        val exists = withContext(Dispatchers.IO) {
            withTimeoutOrNull(800) {
                runCatching { profileRepo.existsOnServer() }.getOrDefault(false)
            }
        }

        // 3) 若本地與伺服器不一致，無感修正並同步快取
        if (exists != null) {
            withContext(Dispatchers.IO) {
                runCatching { store.setHasServerProfile(exists) }
            }
            if (exists && signedIn) onGoHome() else if (!exists) onGoLanding()
        }
    }

    // ✅ 介面：白底＋置中 App 圖標（取代轉圈圈）
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
