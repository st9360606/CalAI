package com.calai.app.ui.nav

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.HiltViewModelFactory
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.calai.app.R
import com.calai.app.data.auth.net.SessionBus
import com.calai.app.di.AppEntryPoint
import com.calai.app.i18n.LocalLocaleController
import com.calai.app.i18n.currentLocaleKey
import com.calai.app.ui.appentry.AppEntryRoute
import com.calai.app.ui.auth.RequireSignInScreen
import com.calai.app.ui.auth.SignInSheetHost
import com.calai.app.ui.auth.SignUpScreen
import com.calai.app.ui.auth.email.EmailCodeScreen
import com.calai.app.ui.auth.email.EmailEnterScreen
import com.calai.app.ui.auth.email.EmailSignInViewModel
import com.calai.app.ui.home.HomeScreen
import com.calai.app.ui.landing.LandingScreen
import com.calai.app.ui.nav.Routes.APP_ENTRY
import com.calai.app.ui.nav.Routes.HOME
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
import com.calai.app.ui.nav.Routes.REQUIRE_SIGN_IN
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
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
    const val REQUIRE_SIGN_IN = "require_sign_in"
    const val HOME = "home"
    const val APP_ENTRY = "app_entry"
}

private tailrec fun Context.findActivity(): Activity? =
    when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }

private fun androidx.navigation.NavController.safePopBackStack(): Boolean =
    if (previousBackStackEntry != null) popBackStack() else false

@Composable
fun BiteCalNavHost(
    hostActivity: ComponentActivity,
    modifier: Modifier = Modifier,
    onSetLocale: (String) -> Unit,
) {
    val nav = rememberNavController()

    val appCtx = LocalContext.current.applicationContext
    val ep = remember(appCtx) {
        EntryPointAccessors.fromApplication(appCtx, AppEntryPoint::class.java)
    }
    val authState = remember(ep) { ep.authState() }
    val isSignedIn by authState.isSignedInFlow.collectAsState(initial = null)

    // 逾期處理：跳 Email 登入頁，預設成功後回 HOME
    LaunchedEffect(Unit) {
        SessionBus.expired.collect {
            nav.navigate("$SIGNIN_EMAIL_ENTER?redirect=$HOME") { popUpTo(0) { inclusive = true } }
        }
    }

    // ★ 冷啟起點 → APP_ENTRY
    NavHost(navController = nav, startDestination = APP_ENTRY, modifier = modifier) {

        composable(APP_ENTRY) {
            AppEntryRoute(
                onGoLanding = { nav.navigate(LANDING) { popUpTo(0) { inclusive = true } } },
                onGoHome = { nav.navigate(HOME) { popUpTo(0) { inclusive = true } } }
            )
        }

        composable(LANDING) {
            LandingScreen(
                hostActivity = hostActivity,
                navController = nav,
                onStart = { nav.navigate(ONBOARD_GENDER) },
                // Landing 走 Email 登入 → 成功後回 GENDER
                onLogin = { nav.navigate("$SIGNIN_EMAIL_ENTER?redirect=$ONBOARD_GENDER") },
                onSetLocale = onSetLocale,
            )
        }

        composable(SIGN_UP) {
            SignUpScreen(onBack = { nav.safePopBackStack() }, onSignedUp = { })
        }

        // ===== Email：輸入 Email 畫面（帶 redirect）=====
        composable(
            route = "${SIGNIN_EMAIL_ENTER}?redirect={redirect}",
            arguments = listOf(
                navArgument("redirect") {
                    type = NavType.StringType
                    defaultValue = Routes.HOME
                }
            )
        ) { backStackEntry ->
            val activity = (LocalContext.current.findActivity() ?: hostActivity)
            val vm: EmailSignInViewModel = viewModel(
                viewModelStoreOwner = backStackEntry,
                factory = HiltViewModelFactory(activity, backStackEntry)
            )
            val redirect = backStackEntry.arguments?.getString("redirect") ?: Routes.HOME

            EmailEnterScreen(
                vm = vm,
                onBack = { nav.safePopBackStack() },
                onSent = { email ->
                    nav.navigate("${SIGNIN_EMAIL_CODE}?email=$email&redirect=$redirect")
                }
            )
        }

        // ===== Email：輸入驗證碼畫面（帶 redirect）=====
        composable(
            route = "${SIGNIN_EMAIL_CODE}?email={email}&redirect={redirect}",
            arguments = listOf(
                navArgument("email") { type = NavType.StringType; defaultValue = "" },
                navArgument("redirect") { type = NavType.StringType; defaultValue = Routes.HOME }
            )
        ) { backStackEntry ->
            val activity = (LocalContext.current.findActivity() ?: hostActivity)
            val vm: EmailSignInViewModel = viewModel(
                viewModelStoreOwner = backStackEntry,
                factory = HiltViewModelFactory(activity, backStackEntry)
            )
            val email = backStackEntry.arguments?.getString("email") ?: ""
            val redirect = backStackEntry.arguments?.getString("redirect") ?: Routes.HOME

            // ★ 需要用到的單例（DataStore & Repository）
            val store = remember(ep) { ep.userProfileStore() }
            val profileRepo = remember(ep) { ep.profileRepository() }
            val localeController = LocalLocaleController.current
            val localeTag = localeController.tag.ifBlank { "en" }
            val scope = rememberCoroutineScope()

            EmailCodeScreen(
                vm = vm,
                email = email,
                onBack = { nav.safePopBackStack() },
                onSuccess = {
                    // 先把目前語言寫回 DataStore，再 upsert 到後端，最後導頁
                    scope.launch {
                        withContext(Dispatchers.IO) {
                            runCatching { store.setLocaleTag(localeTag) }
                            runCatching { profileRepo.upsertFromLocal() }
                        }
                        Toast.makeText(
                            hostActivity,
                            hostActivity.getString(R.string.msg_login_success),
                            Toast.LENGTH_SHORT
                        ).show()
                        nav.navigate(redirect) {
                            popUpTo(0) { inclusive = true }
                            launchSingleTop = true
                            restoreState = false
                        }
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
                onNext = { _: GenderKey -> nav.navigate(ONBOARD_REFERRAL) { launchSingleTop = true } }
            )
        }

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

        composable(ONBOARD_HEIGHT) { backStackEntry ->
            val activity = (LocalContext.current.findActivity() ?: hostActivity)
            val vm: HeightSelectionViewModel = viewModel(
                viewModelStoreOwner = backStackEntry,
                factory = HiltViewModelFactory(activity, backStackEntry)
            )
            HeightSelectionScreen(
                vm = vm,
                onBack = { nav.safePopBackStack() },
                onNext = { nav.navigate(ONBOARD_WEIGHT) { launchSingleTop = true } }
            )
        }

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

        composable(ONBOARD_EXERCISE_FREQ) { backStackEntry ->
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

        composable(ONBOARD_GOAL) { backStackEntry ->
            val activity = (LocalContext.current.findActivity() ?: hostActivity)
            val vm: GoalSelectionViewModel = viewModel(
                viewModelStoreOwner = backStackEntry,
                factory = HiltViewModelFactory(activity, backStackEntry)
            )
            GoalSelectionScreen(
                vm = vm,
                onBack = { nav.safePopBackStack() },
                onNext = { nav.navigate(ONBOARD_TARGET_WEIGHT) { launchSingleTop = true } }
            )
        }

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

        composable(ONBOARD_NOTIF) {
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
            HealthPlanScreen(vm = vm, onStart = {
                val target = Routes.HOME
                if (isSignedIn == true) {
                    nav.navigate(target) { popUpTo(0) { inclusive = true } }
                } else {
                    nav.navigate("${REQUIRE_SIGN_IN}?redirect=$target")
                }
            })
        }

        // ===== 登入 Gate（未登入顯示 Create an account，登入/Skip → redirect）=====
        composable(
            route = "${REQUIRE_SIGN_IN}?redirect={redirect}",
            arguments = listOf(navArgument("redirect") {
                type = NavType.StringType
                defaultValue = Routes.HOME
            })
        ) { backStackEntry ->
            val redirect = backStackEntry.arguments?.getString("redirect") ?: Routes.HOME

            val snackbarHostState = remember { SnackbarHostState() }
            val scope = rememberCoroutineScope()
            val ctx = LocalContext.current
            val localeController = LocalLocaleController.current
            val localeKey = currentLocaleKey()
            val localeTag = localeController.tag.ifBlank { "en" }

            // 面板開關：第一次進來就顯示，取消後可再打開
            val showSheet = remember { mutableStateOf(true) }

            RequireSignInScreen(
                onBack = { nav.safePopBackStack() },
                onGoogleClick = { showSheet.value = true },
                onSkip = { nav.navigate(redirect) },
                snackbarHostState = snackbarHostState
            )

            // 永遠掛著，用 visible 控制顯示
            key(localeKey) {
                SignInSheetHost(
                    activity = hostActivity,
                    navController = nav,
                    localeTag = localeTag,
                    visible = showSheet.value,
                    onDismiss = { showSheet.value = false },

                    onGoogle = {
                        showSheet.value = false
                        scope.launch {
                            snackbarHostState.showSnackbar(ctx.getString(R.string.msg_login_success))
                        }
                    },

                    onEmail = {
                        showSheet.value = false
                        // 帶上 redirect，成功後回預期頁面
                        nav.navigate("$SIGNIN_EMAIL_ENTER?redirect=$redirect")
                    },

                    onShowError = { msg ->
                        showSheet.value = false
                        scope.launch { snackbarHostState.showSnackbar(msg.toString()) }
                    },

                    postLoginNavigate = { controller ->
                        controller.navigate(redirect) {
                            popUpTo(0) { inclusive = true }
                            launchSingleTop = true
                            restoreState = false
                        }
                    }
                )
            }
        }

        // HOME
        composable(HOME) {
            HomeScreen(onSignOut = {
                nav.navigate(LANDING) { popUpTo(0) { inclusive = true } }
            })
        }
    }
}
