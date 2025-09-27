package com.calai.app.ui.nav

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.widget.Toast
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
import com.calai.app.ui.onboarding.gender.GenderKey
import com.calai.app.ui.onboarding.gender.GenderSelectionScreen
import com.calai.app.ui.onboarding.gender.GenderSelectionViewModel
import com.calai.app.ui.onboarding.referralsource.ReferralSourceScreen
import com.calai.app.ui.onboarding.referralsource.ReferralSourceViewModel

object Routes {
    const val LANDING = "landing"
    const val SIGN_UP = "signup"
    const val SIGNIN_EMAIL_ENTER = "signin_email_enter"
    const val SIGNIN_EMAIL_CODE  = "signin_email_code"
    // Onboarding
    const val ONBOARD_GENDER = "onboard_gender"
    const val ONBOARD_REFERRAL = "onboard_referral"
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
                navController = nav,
                onStart = { nav.navigate(Routes.ONBOARD_GENDER) },         // 免登入→性別頁
                onLogin = { nav.navigate(Routes.SIGNIN_EMAIL_ENTER) },
                onSetLocale = onSetLocale,
            )
        }

        composable(Routes.SIGN_UP) {
            SignUpScreen(
                onBack = { nav.popBackStack() },
                onSignedUp = { /* TODO */ }
            )
        }

        // ===== Email：輸入 Email 畫面（用 Activity + backStackEntry 建 Hilt VM） =====
        composable(Routes.SIGNIN_EMAIL_ENTER) { backStackEntry ->
            val activity = (LocalContext.current.findActivity() ?: hostActivity)
            val vm: EmailSignInViewModel = viewModel(
                viewModelStoreOwner = backStackEntry,
                factory = HiltViewModelFactory(activity, backStackEntry)
            )
            EmailEnterScreen(
                vm = vm,
                onBack = { nav.popBackStack() },
                onSent = { email -> nav.navigate("${Routes.SIGNIN_EMAIL_CODE}?email=$email") }
            )
        }

        // ===== Email：輸入驗證碼畫面 =====
        composable(
            route = "${Routes.SIGNIN_EMAIL_CODE}?email={email}",
            arguments = listOf(navArgument("email") { type = NavType.StringType; defaultValue = "" })
        ) { backStackEntry ->
            val activity = (LocalContext.current.findActivity() ?: hostActivity)
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
                    nav.navigate(Routes.ONBOARD_GENDER) {
                        popUpTo(Routes.LANDING) { inclusive = false; saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }

        // ===== Onboarding：性別 =====
        composable(Routes.ONBOARD_GENDER) { backStackEntry ->
            // ⚠️ 關鍵：在 NavHost 用 HiltViewModelFactory 建 VM，再傳入畫面（避免 hiltViewModel + LocalContext 被語系包裝）
            val activity = (LocalContext.current.findActivity() ?: hostActivity)
            val vm: GenderSelectionViewModel = viewModel(
                viewModelStoreOwner = backStackEntry,
                factory = HiltViewModelFactory(activity, backStackEntry)
            )
            GenderSelectionScreen(
                vm = vm,
                onBack = { nav.popBackStack() },
                onNext = { _: GenderKey ->
                    // 畫面已保存到 DataStore，這裡只負責導頁
                    nav.navigate(Routes.ONBOARD_REFERRAL) { launchSingleTop = true }
                }
            )
        }

        // ===== Onboarding：你從哪裡知道我們 =====
        composable(Routes.ONBOARD_REFERRAL) { backStackEntry ->
            // 同理，顯式建 VM 並傳入
            val activity = (LocalContext.current.findActivity() ?: hostActivity)
            val vm: ReferralSourceViewModel = viewModel(
                viewModelStoreOwner = backStackEntry,
                factory = HiltViewModelFactory(activity, backStackEntry)
            )
            ReferralSourceScreen(
                vm = vm,
                onBack = { nav.popBackStack() },
                onNext = {
                    // TODO: 這裡改成你的下一頁 Route（例如年齡/目標）
                    nav.popBackStack()
                }
            )
        }
    }
}
