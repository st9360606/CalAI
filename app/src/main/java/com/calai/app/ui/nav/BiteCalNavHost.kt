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
import androidx.navigation.NavController
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
import com.calai.app.ui.nav.Routes.LANDING
import com.calai.app.ui.nav.Routes.ONBOARD_AGE
import com.calai.app.ui.nav.Routes.ONBOARD_EXERCISE_FREQ
import com.calai.app.ui.nav.Routes.ONBOARD_GENDER
import com.calai.app.ui.nav.Routes.ONBOARD_GOAL
import com.calai.app.ui.nav.Routes.ONBOARD_HEIGHT
import com.calai.app.ui.nav.Routes.ONBOARD_NOTIF
import com.calai.app.ui.nav.Routes.ONBOARD_REFERRAL
import com.calai.app.ui.nav.Routes.ONBOARD_TARGET_WEIGHT
import com.calai.app.ui.nav.Routes.ONBOARD_WEIGHT
import com.calai.app.ui.nav.Routes.ROUTE_PLAN
import com.calai.app.ui.nav.Routes.SIGNIN_EMAIL_CODE
import com.calai.app.ui.nav.Routes.SIGNIN_EMAIL_ENTER
import com.calai.app.ui.nav.Routes.SIGN_UP
import com.calai.app.ui.onboarding.age.AgeSelectionScreen
import com.calai.app.ui.onboarding.age.AgeSelectionViewModel
import com.calai.app.ui.onboarding.exercise.ExerciseFrequencyScreen
import com.calai.app.ui.onboarding.exercise.ExerciseFrequencyViewModel
import com.calai.app.ui.onboarding.gender.GenderKey
import com.calai.app.ui.onboarding.gender.GenderSelectionScreen
import com.calai.app.ui.onboarding.gender.GenderSelectionViewModel
import com.calai.app.ui.onboarding.goal.GoalSelectionScreen
import com.calai.app.ui.onboarding.goal.GoalSelectionViewModel
import com.calai.app.ui.onboarding.height.HeightSelectionScreen
import com.calai.app.ui.onboarding.height.HeightSelectionViewModel
import com.calai.app.ui.onboarding.notifications.NotificationPermissionScreen
import com.calai.app.ui.onboarding.plan.HealthPlanScreen
import com.calai.app.ui.onboarding.plan.HealthPlanViewModel
import com.calai.app.ui.onboarding.referralsource.ReferralSourceScreen
import com.calai.app.ui.onboarding.referralsource.ReferralSourceViewModel
import com.calai.app.ui.onboarding.targetweight.WeightTargetScreen
import com.calai.app.ui.onboarding.targetweight.WeightTargetViewModel
import com.calai.app.ui.onboarding.weight.WeightSelectionScreen
import com.calai.app.ui.onboarding.weight.WeightSelectionViewModel

object Routes {
    const val LANDING = "landing"
    const val SIGN_UP = "signup"
    const val SIGNIN_EMAIL_ENTER = "signin_email_enter"
    const val SIGNIN_EMAIL_CODE = "signin_email_code"
    // Onboarding
    const val ONBOARD_GENDER = "onboard_gender"
    const val ONBOARD_REFERRAL = "onboard_referral"
    const val ONBOARD_AGE = "onboard_age"
    const val ONBOARD_HEIGHT = "onboard_height"
    const val ONBOARD_WEIGHT = "onboard_weight"
    const val ONBOARD_TARGET_WEIGHT = "onboard_target_weight"
    const val ONBOARD_EXERCISE_FREQ = "onboard_exercise_freq"
    const val ONBOARD_GOAL = "onboard_goal"
    const val ONBOARD_NOTIF = "onboard_notif"

    const val ROUTE_PLAN = "plan"
}

// ── 安全往上找 Activity ───────────────────────────────────────────────
private tailrec fun Context.findActivity(): Activity? =
    when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }

// ── 只在有上一頁時才 pop（根頁時不做事） ─────────────────────────────
private fun NavController.safePopBackStack(): Boolean {
    return if (previousBackStackEntry != null) popBackStack() else false
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
            nav.navigate(SIGNIN_EMAIL_ENTER) {
                popUpTo(0) { inclusive = true }
            }
        }
    }

    NavHost(
        navController = nav,
        startDestination = LANDING,
        modifier = modifier
    ) {
        composable(LANDING) {
            LandingScreen(
                hostActivity = hostActivity,
                navController = nav,
                onStart = { nav.navigate(ONBOARD_GENDER) },         // 免登入→性別頁
                onLogin = { nav.navigate(SIGNIN_EMAIL_ENTER) },
                onSetLocale = onSetLocale,
            )
        }

        composable(SIGN_UP) {
            SignUpScreen(
                onBack = { nav.safePopBackStack() },
                onSignedUp = { /* TODO */ }
            )
        }

        // ===== Email：輸入 Email 畫面 =====
        composable(SIGNIN_EMAIL_ENTER) { backStackEntry ->
            val activity = (LocalContext.current.findActivity() ?: hostActivity)
            val vm: EmailSignInViewModel = viewModel(
                viewModelStoreOwner = backStackEntry,
                factory = HiltViewModelFactory(activity, backStackEntry)
            )
            EmailEnterScreen(
                vm = vm,
                onBack = { nav.safePopBackStack() },
                onSent = { email -> nav.navigate("${SIGNIN_EMAIL_CODE}?email=$email") }
            )
        }

        // ===== Email：輸入驗證碼畫面 =====
        composable(
            route = "${SIGNIN_EMAIL_CODE}?email={email}",
            arguments = listOf(navArgument("email") {
                type = NavType.StringType; defaultValue = ""
            })
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
                onBack = { nav.safePopBackStack() },
                onSuccess = {
                    Toast.makeText(hostActivity, "登入成功", Toast.LENGTH_SHORT).show()
                    nav.navigate(ONBOARD_GENDER) {
                        popUpTo(LANDING) { inclusive = false; saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }

        // ===== Onboarding：性別 =====
        composable(ONBOARD_GENDER) { backStackEntry ->
            val activity = (LocalContext.current.findActivity() ?: hostActivity)
            val vm: GenderSelectionViewModel = viewModel(
                viewModelStoreOwner = backStackEntry,
                factory = HiltViewModelFactory(activity, backStackEntry)
            )
            GenderSelectionScreen(
                vm = vm,
                onBack = { nav.safePopBackStack() },
                onNext = { _: GenderKey ->
                    nav.navigate(ONBOARD_REFERRAL) { launchSingleTop = true }
                }
            )
        }

        // ===== Onboarding：你從哪裡知道我們 → 年齡 =====
        composable(ONBOARD_REFERRAL) { backStackEntry ->
            val activity = (LocalContext.current.findActivity() ?: hostActivity)
            val vm: ReferralSourceViewModel = viewModel(
                viewModelStoreOwner = backStackEntry,
                factory = HiltViewModelFactory(activity, backStackEntry)
            )
            ReferralSourceScreen(
                vm = vm,
                onBack = { nav.safePopBackStack() },
                onNext = { nav.navigate(ONBOARD_AGE) { launchSingleTop = true } }
            )
        }

        // ===== Onboarding：年齡 =====
        composable(ONBOARD_AGE) { backStackEntry ->
            val activity = (LocalContext.current.findActivity() ?: hostActivity)
            val vm: AgeSelectionViewModel = viewModel(
                viewModelStoreOwner = backStackEntry,
                factory = HiltViewModelFactory(activity, backStackEntry)
            )
            AgeSelectionScreen(
                vm = vm,
                onBack = { nav.safePopBackStack() },
                onNext = { nav.navigate(ONBOARD_HEIGHT) { launchSingleTop = true } }
            )
        }

        // ===== Onboarding：身高 =====
        composable(ONBOARD_HEIGHT) { backStackEntry ->
            val activity = (LocalContext.current.findActivity() ?: hostActivity)
            val vm: HeightSelectionViewModel = viewModel(
                viewModelStoreOwner = backStackEntry,
                factory = HiltViewModelFactory(activity, backStackEntry)
            )
            HeightSelectionScreen(
                vm = vm,
                onBack = { nav.safePopBackStack() },
                onNext = { nav.navigate(ONBOARD_WEIGHT)  { launchSingleTop = true } }
            )
        }

        // ===== Onboarding：體重 =====
        composable(ONBOARD_WEIGHT) { backStackEntry ->
            val activity = (LocalContext.current.findActivity() ?: hostActivity)
            val vm: WeightSelectionViewModel = viewModel(
                viewModelStoreOwner = backStackEntry,
                factory = HiltViewModelFactory(activity, backStackEntry)
            )
            WeightSelectionScreen(
                vm = vm,
                onBack = { nav.safePopBackStack() },
                onNext = { nav.navigate(ONBOARD_EXERCISE_FREQ) { launchSingleTop = true } }
            )
        }

        // ===== Onboarding：鍛鍊頻率 =====
        composable(route = ONBOARD_EXERCISE_FREQ) { backStackEntry ->
            val activity = (LocalContext.current.findActivity() ?: hostActivity)
            val vm: ExerciseFrequencyViewModel = viewModel(
                viewModelStoreOwner = backStackEntry,
                factory = HiltViewModelFactory(activity, backStackEntry)
            )
            ExerciseFrequencyScreen(
                vm = vm,
                onBack = { nav.safePopBackStack() },
                onNext = { nav.navigate(ONBOARD_GOAL) { launchSingleTop = true } }
            )
        }

        // ===== Onboarding：目標 =====
        composable(route = ONBOARD_GOAL) { backStackEntry ->
            val activity = (LocalContext.current.findActivity() ?: hostActivity)
            val vm: GoalSelectionViewModel = viewModel(
                viewModelStoreOwner = backStackEntry,
                factory = HiltViewModelFactory(activity, backStackEntry)
            )
            GoalSelectionScreen(
                vm = vm,
                onBack = { nav.safePopBackStack() },
                onNext = { nav.navigate(ONBOARD_TARGET_WEIGHT) { launchSingleTop = true } }
                    // TODO: 這裡接下一步頁（例如：飲食偏好 / Diet）
                    // nav.navigate(ONBOARD_DIET) { launchSingleTop = true; restoreState = true }

            )
        }

        // ===== Onboarding：目標體重 =====
        composable(ONBOARD_TARGET_WEIGHT) { backStackEntry ->
            val activity = (LocalContext.current.findActivity() ?: hostActivity)
            val vm: WeightTargetViewModel = viewModel(
                viewModelStoreOwner = backStackEntry,
                factory = HiltViewModelFactory(activity, backStackEntry)
            )
            WeightTargetScreen(
                vm = vm,
                onBack = { nav.safePopBackStack() },
                onNext = { nav.navigate(ONBOARD_NOTIF) { launchSingleTop = true } }
            )
        }

        composable(route = ONBOARD_NOTIF) {
            NotificationPermissionScreen(
                onBack = { nav.safePopBackStack() },
                onNext = { nav.navigate(ROUTE_PLAN) { launchSingleTop = true } }
            )
        }

        composable(ROUTE_PLAN) { backStackEntry ->
            val activity = (LocalContext.current.findActivity() ?: hostActivity)
            val vm: HealthPlanViewModel = viewModel(
                viewModelStoreOwner = backStackEntry,
                factory = HiltViewModelFactory(activity, backStackEntry)
            )
            HealthPlanScreen(vm = vm, onStart = { /* TODO: go to Home */ })
        }
    }
}
