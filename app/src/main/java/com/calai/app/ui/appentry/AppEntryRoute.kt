package com.calai.app.ui.appentry

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.calai.app.di.AppEntryPoint
import dagger.hilt.android.EntryPointAccessors

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
    val snack = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        // 決策邏輯：已登入且後端已有個人檔 => HOME；其餘 => LANDING
        val goHome = try {
            profileRepo.existsOnServer()
        } catch (_: Exception) {
            // 任何網路/序列化錯誤都先導 Landing，避免卡白屏或閃退
            false
        }
        if (goHome) onGoHome() else onGoLanding()
    }

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(
            color = Color.Black,
            strokeWidth = 4.dp,
            modifier = Modifier.size(28.dp)
        )
    }
    SnackbarHost(hostState = snack)
}
