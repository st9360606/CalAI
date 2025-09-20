package com.calai.app.ui.nav

import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.calai.app.ui.landing.LandingScreen
import com.calai.app.ui.auth.SignInScreen
import com.calai.app.ui.auth.SignUpScreen

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
    hostActivity: ComponentActivity,          // ★ 新增參數
    modifier: Modifier = Modifier,
    onSetLocale: (String) -> Unit,
) {
    val nav = rememberNavController()

    NavHost(
        navController = nav,
        startDestination = Routes.LANDING,
        modifier = modifier
    ) {
        composable(Routes.LANDING) {
            LandingScreen(
                hostActivity = hostActivity,     // ★ 傳給 Landing（再往下傳到 SignInSheetHost）
                onStart = { nav.navigate(Routes.SIGN_UP) },
                onLogin = { nav.navigate(Routes.SIGN_IN) },
                onSetLocale = onSetLocale,
            )
        }

        composable(Routes.SIGN_IN) {
            // 你已有的登入畫面；這裡放你的實作
            SignInScreen(
                onBack = { nav.popBackStack() },
                onSignedIn = {
                    // TODO: 導到真正的首頁（之後你接相機/首頁）
                    // nav.navigate("home") { popUpTo(Routes.LANDING) { inclusive = true } }
                }
            )
        }

        composable(Routes.SIGN_UP) {
            // 你已有的註冊畫面；這裡放你的實作
            SignUpScreen(
                onBack = { nav.popBackStack() },
                onSignedUp = {
                    // TODO: 導到真正的首頁
                }
            )
        }
    }
}
