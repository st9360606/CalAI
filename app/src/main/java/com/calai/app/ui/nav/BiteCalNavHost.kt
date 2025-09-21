package com.calai.app.ui.nav

import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.calai.app.ui.landing.LandingScreen
import com.calai.app.ui.auth.SignInScreen
import com.calai.app.ui.auth.SignUpScreen
import com.calai.app.data.auth.net.SessionBus // ← 監聽會話過期事件

object Routes {
    const val LANDING = "landing"
    const val SIGN_IN = "signin"
    const val SIGN_UP = "signup"
}

/**
 * App 的導航樹入口。
 * - hostActivity：從 MainActivity 傳進來，往下交給需要啟動 Activity Result 的畫面（例如 Google Sign-In）。
 * - onSetLocale：從 BiteCalApp 傳入，用來做「無重啟的語言切換」。
 */
@Composable
fun BiteCalNavHost(
    hostActivity: ComponentActivity,
    modifier: Modifier = Modifier,
    onSetLocale: (String) -> Unit,
) {
    val nav = rememberNavController()

    // 收到「會話過期」事件 → 導回登入，並清掉返回棧
    LaunchedEffect(Unit) {
        SessionBus.expired.collect {
            nav.navigate(Routes.SIGN_IN) {
                popUpTo(0) { inclusive = true }
            }
        }
    }

    NavHost(
        navController = nav,
        startDestination = Routes.LANDING,
        modifier = modifier
    ) {
        composable(Routes.LANDING) {
            LandingScreen(
                hostActivity = hostActivity,
                onStart = { nav.navigate(Routes.SIGN_UP) },
                onLogin = { nav.navigate(Routes.SIGN_IN) },
                onSetLocale = onSetLocale,
            )
        }

        composable(Routes.SIGN_IN) {
            SignInScreen(
                onBack = { nav.popBackStack() },
                onSignedIn = {
                    // TODO: 成功後導到真正首頁
                    // nav.navigate("home") { popUpTo(Routes.LANDING) { inclusive = true } }
                }
            )
        }

        composable(Routes.SIGN_UP) {
            SignUpScreen(
                onBack = { nav.popBackStack() },
                onSignedUp = {
                    // TODO: 成功後導到真正首頁
                }
            )
        }
    }
}
