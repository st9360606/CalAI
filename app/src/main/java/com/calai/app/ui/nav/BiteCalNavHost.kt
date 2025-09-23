package com.calai.app.ui.nav

import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.HiltViewModelFactory
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.calai.app.data.auth.net.SessionBus
import com.calai.app.ui.auth.SignUpScreen
import com.calai.app.ui.auth.email.EmailCodeScreen
import com.calai.app.ui.auth.email.EmailEnterScreen
import com.calai.app.ui.auth.email.EmailSignInViewModel
import com.calai.app.ui.landing.LandingScreen
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.widget.Toast

object Routes {
    const val LANDING = "landing"
    const val SIGN_UP = "signup"
    const val SIGNIN_EMAIL_ENTER = "signin_email_enter"
    const val SIGNIN_EMAIL_CODE  = "signin_email_code"
}

// 安全往上找 Activity
private tailrec fun Context.findActivity(): Activity? =
    when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }

@Composable
fun BiteCalNavHost(
    hostActivity: ComponentActivity,
    modifier: Modifier = Modifier,
    onSetLocale: (String) -> Unit,
) {
    val nav = rememberNavController()

    // 會話過期 → 直接帶到 Email 輸入頁（或改回 LANDING 視需求）
    LaunchedEffect(Unit) {
        SessionBus.expired.collect {
            nav.navigate(Routes.SIGNIN_EMAIL_ENTER) {
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
                onLogin = { nav.navigate(Routes.SIGNIN_EMAIL_ENTER) }, // 底部面板「以 Email 繼續」
                onSetLocale = onSetLocale,
            )
        }

        composable(Routes.SIGN_UP) {
            SignUpScreen(
                onBack = { nav.popBackStack() },
                onSignedUp = { /* TODO */ }
            )
        }

        // ===== Email：輸入 Email 畫面（改成用 Activity + backStackEntry 建 Hilt VM） =====
        composable(Routes.SIGNIN_EMAIL_ENTER) { backStackEntry ->
            val activity = (LocalContext.current.findActivity()
                ?: hostActivity) // 保底用 hostActivity
            val vm: EmailSignInViewModel = viewModel(
                viewModelStoreOwner = backStackEntry,
                factory = HiltViewModelFactory(activity, backStackEntry)
            )
            EmailEnterScreen(
                vm = vm,
                onBack = { nav.popBackStack() },
                onSent = { email ->
                    nav.navigate("${Routes.SIGNIN_EMAIL_CODE}?email=$email")
                }
            )
        }

        // ===== Email：輸入驗證碼畫面（同樣用手動 factory） =====
        composable(
            route = "${Routes.SIGNIN_EMAIL_CODE}?email={email}",
            arguments = listOf(
                navArgument("email") { type = NavType.StringType; defaultValue = "" }
            )
        ) { backStackEntry ->
            val activity = (LocalContext.current.findActivity()
                ?: hostActivity)
            val vm: EmailSignInViewModel = viewModel(
                viewModelStoreOwner = backStackEntry,
                factory = HiltViewModelFactory(activity, backStackEntry)
            )

            val email = backStackEntry.arguments?.getString("email") ?: ""
            EmailCodeScreen(
                vm = vm,
                email = email,
                onBack = { nav.popBackStack() },
                onSuccess = {
                    Toast.makeText(hostActivity, "登入成功", Toast.LENGTH_SHORT).show()
                    nav.navigate(Routes.LANDING) {
                        popUpTo(Routes.LANDING) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }
    }
}
