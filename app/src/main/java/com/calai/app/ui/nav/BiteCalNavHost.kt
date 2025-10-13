package com.calai.app.ui.nav

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
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
import com.calai.app.i18n.LanguageSessionFlag
import com.calai.app.i18n.currentLocaleKey
import com.calai.app.ui.appentry.AppEntryRoute
import com.calai.app.ui.auth.RequireSignInScreen
import com.calai.app.ui.auth.SignInSheetHost
import com.calai.app.ui.auth.SignUpScreen
import com.calai.app.ui.auth.email.EmailCodeScreen
import com.calai.app.ui.auth.email.EmailEnterScreen
import com.calai.app.ui.auth.email.EmailSignInViewModel
import com.calai.app.ui.home.HomeRoute
import com.calai.app.ui.home.HomeTab
import com.calai.app.ui.home.HomeViewModel
import com.calai.app.ui.landing.LandingScreen
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
    const val ONBOARD_HEALTH_CONNECT = "onboard_health_connect"
    const val PLAN_PROGRESS = "plan_progress"
    const val ROUTE_PLAN = "plan"
    const val REQUIRE_SIGN_IN = "require_sign_in"
    const val HOME = "home"
    const val APP_ENTRY = "app_entry"
    // 其他暫時頁
    const val PROGRESS = "progress"
    const val NOTE = "note"
    const val FASTING = "fasting"
    const val PERSONAL = "personal"
    const val CAMERA = "camera"
    const val REMINDERS = "reminders"
}

private tailrec fun Context.findActivity(): Activity? =
    when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }

private fun androidx.navigation.NavController.safePopBackStack(): Boolean =
    previousBackStackEntry != null && popBackStack()

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

    val profileRepo = remember(ep) { ep.profileRepository() }
    val store = remember(ep) { ep.userProfileStore() }

    val localeController = LocalLocaleController.current

    // ✅ 不再在這裡初始化語言（交由 BiteCalApp 進行），以免造成啟動畫面語言跳動
    // （原本 DataStore→裝置語言→set 的 LaunchedEffect 已移除）

    // Token 逾期：帶到 Gate，成功後回 HOME
    LaunchedEffect(Unit) {
        SessionBus.expired.collect {
            nav.navigate("${Routes.REQUIRE_SIGN_IN}?redirect=${Routes.HOME}") {
                popUpTo(0) { inclusive = true }
            }
        }
    }

    NavHost(navController = nav, startDestination = Routes.APP_ENTRY, modifier = modifier) {

        composable(Routes.APP_ENTRY) {
            AppEntryRoute(
                onGoLanding = { nav.navigate(Routes.LANDING) { popUpTo(0) { inclusive = true } } },
                onGoHome = { nav.navigate(Routes.HOME) { popUpTo(0) { inclusive = true } } }
            )
        }

        composable(Routes.LANDING) {
            val scope = rememberCoroutineScope()
            val localeControllerLocal = LocalLocaleController.current

            LandingScreen(
                hostActivity = hostActivity,
                navController = nav,
                onStart = {
                    nav.navigate(Routes.ONBOARD_GENDER) { launchSingleTop = true }
                },
                onLogin = {
                    scope.launch {
                        val has = store.hasServerProfile()
                        val target = if (has) Routes.HOME else Routes.ONBOARD_GENDER
                        nav.navigate("${Routes.REQUIRE_SIGN_IN}?redirect=$target")
                    }
                },
                onSetLocale = { tag ->
                    // 只在使用者主動切換時才更新（這裡會順便寫 DataStore）
                    localeControllerLocal.set(tag)
                    // 如果你有 AppCompatDelegate.apply 的封裝，就在這裡呼叫
                    com.calai.app.i18n.LanguageManager.applyLanguage(tag)
                    onSetLocale(tag)
                    scope.launch {
                        withContext(Dispatchers.IO) {
                            runCatching { store.setLocaleTag(tag) }
                        }
                    }
                },
            )
        }

        composable(Routes.SIGN_UP) {
            SignUpScreen(onBack = { nav.safePopBackStack() }, onSignedUp = { })
        }

        // ===== Email：輸入 Email 畫面（帶 redirect）=====
        composable(
            route = "${Routes.SIGNIN_EMAIL_ENTER}?redirect={redirect}",
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
                    nav.navigate("${Routes.SIGNIN_EMAIL_CODE}?email=$email&redirect=$redirect")
                }
            )
        }

        // ===== Email：輸入驗證碼畫面（帶 redirect）=====
        composable(
            route = "${Routes.SIGNIN_EMAIL_CODE}?email={email}&redirect={redirect}",
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

            val currentTag = localeController.tag.ifBlank { "en" }
            val scope = rememberCoroutineScope()

            EmailCodeScreen(
                vm = vm,
                email = email,
                onBack = { nav.safePopBackStack() },
                onSuccess = {
                    scope.launch {
                        withContext(Dispatchers.IO) {
                            // 新/舊分流：存在＝回訪；不存在＝新用戶
                            val exists = runCatching { profileRepo.existsOnServer() }.getOrDefault(false)
                            if (!exists) {
                                // 新用戶：把當前語言寫入，再上傳完整 Onboarding
                                runCatching { store.setLocaleTag(currentTag) }
                                runCatching { profileRepo.upsertFromLocal() }
                                runCatching { store.setHasServerProfile(true) }
                            } else {
                                // 回訪用戶：僅在本次 session 有改語言才覆蓋後端語系
                                val changedThisSession = LanguageSessionFlag.consumeChanged()
                                if (changedThisSession) {
                                    runCatching { profileRepo.updateLocaleOnly(currentTag) }
                                }
                                runCatching { store.setHasServerProfile(true) }
                            }
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

        // ===== Onboarding：性別 → 目標體重 → 通知 → Plan =====
        composable(Routes.ONBOARD_GENDER) { backStackEntry ->
            val activity = (LocalContext.current.findActivity() ?: hostActivity)
            val vm: com.calai.app.ui.onboarding.gender.GenderSelectionViewModel = viewModel(
                viewModelStoreOwner = backStackEntry,
                factory = HiltViewModelFactory(activity, backStackEntry)
            )
            com.calai.app.ui.onboarding.gender.GenderSelectionScreen(
                vm = vm,
                onBack = { nav.safePopBackStack() },
                onNext = { _: com.calai.app.ui.onboarding.gender.GenderKey ->
                    nav.navigate(Routes.ONBOARD_REFERRAL) { launchSingleTop = true }
                }
            )
        }

        composable(Routes.ONBOARD_REFERRAL) { backStackEntry ->
            val activity = (LocalContext.current.findActivity() ?: hostActivity)
            val vm: com.calai.app.ui.onboarding.referralsource.ReferralSourceViewModel = viewModel(
                viewModelStoreOwner = backStackEntry,
                factory = HiltViewModelFactory(activity, backStackEntry)
            )
            com.calai.app.ui.onboarding.referralsource.ReferralSourceScreen(
                vm = vm,
                onBack = { nav.safePopBackStack() },
                onNext = { nav.navigate(Routes.ONBOARD_AGE) { launchSingleTop = true } }
            )
        }

        composable(Routes.ONBOARD_AGE) { backStackEntry ->
            val activity = (LocalContext.current.findActivity() ?: hostActivity)
            val vm: com.calai.app.ui.onboarding.age.AgeSelectionViewModel = viewModel(
                viewModelStoreOwner = backStackEntry,
                factory = HiltViewModelFactory(activity, backStackEntry)
            )
            com.calai.app.ui.onboarding.age.AgeSelectionScreen(
                vm = vm,
                onBack = { nav.safePopBackStack() },
                onNext = { nav.navigate(Routes.ONBOARD_HEIGHT) { launchSingleTop = true } }
            )
        }

        composable(Routes.ONBOARD_HEIGHT) { backStackEntry ->
            val activity = (LocalContext.current.findActivity() ?: hostActivity)
            val vm: com.calai.app.ui.onboarding.height.HeightSelectionViewModel = viewModel(
                viewModelStoreOwner = backStackEntry,
                factory = HiltViewModelFactory(activity, backStackEntry)
            )
            com.calai.app.ui.onboarding.height.HeightSelectionScreen(
                vm = vm,
                onBack = { nav.safePopBackStack() },
                onNext = { nav.navigate(Routes.ONBOARD_WEIGHT) { launchSingleTop = true } }
            )
        }

        composable(Routes.ONBOARD_WEIGHT) { backStackEntry ->
            val activity = (LocalContext.current.findActivity() ?: hostActivity)
            val vm: com.calai.app.ui.onboarding.weight.WeightSelectionViewModel = viewModel(
                viewModelStoreOwner = backStackEntry,
                factory = HiltViewModelFactory(activity, backStackEntry)
            )
            com.calai.app.ui.onboarding.weight.WeightSelectionScreen(
                vm = vm,
                onBack = { nav.safePopBackStack() },
                onNext = { nav.navigate(Routes.ONBOARD_EXERCISE_FREQ) { launchSingleTop = true } }
            )
        }

        composable(Routes.ONBOARD_EXERCISE_FREQ) { backStackEntry ->
            val activity = (LocalContext.current.findActivity() ?: hostActivity)
            val vm: com.calai.app.ui.onboarding.exercise.ExerciseFrequencyViewModel = viewModel(
                viewModelStoreOwner = backStackEntry,
                factory = HiltViewModelFactory(activity, backStackEntry)
            )
            com.calai.app.ui.onboarding.exercise.ExerciseFrequencyScreen(
                vm = vm,
                onBack = { nav.safePopBackStack() },
                onNext = { nav.navigate(Routes.ONBOARD_GOAL) { launchSingleTop = true } }
            )
        }

        composable(Routes.ONBOARD_GOAL) { backStackEntry ->
            val activity = (LocalContext.current.findActivity() ?: hostActivity)
            val vm: com.calai.app.ui.onboarding.goal.GoalSelectionViewModel = viewModel(
                viewModelStoreOwner = backStackEntry,
                factory = HiltViewModelFactory(activity, backStackEntry)
            )
            com.calai.app.ui.onboarding.goal.GoalSelectionScreen(
                vm = vm,
                onBack = { nav.safePopBackStack() },
                onNext = { nav.navigate(Routes.ONBOARD_TARGET_WEIGHT) { launchSingleTop = true } }
            )
        }

        composable(Routes.ONBOARD_TARGET_WEIGHT) { backStackEntry ->
            val activity = (LocalContext.current.findActivity() ?: hostActivity)
            val vm: com.calai.app.ui.onboarding.targetweight.WeightTargetViewModel = viewModel(
                viewModelStoreOwner = backStackEntry,
                factory = HiltViewModelFactory(activity, backStackEntry)
            )
            com.calai.app.ui.onboarding.targetweight.WeightTargetScreen(
                vm = vm,
                onBack = { nav.safePopBackStack() },
                onNext = { nav.navigate(Routes.ONBOARD_NOTIF) { launchSingleTop = true } }
            )
        }

        composable(Routes.ONBOARD_NOTIF) {
            com.calai.app.ui.onboarding.notifications.NotificationPermissionScreen(
                onBack = { nav.safePopBackStack() },
                onNext = { nav.navigate(Routes.ONBOARD_HEALTH_CONNECT) { launchSingleTop = true } }
            )
        }

        // Health Connect 連結頁 → 完成/略過都先進「運算進度頁」
        composable(Routes.ONBOARD_HEALTH_CONNECT) {
            com.calai.app.ui.onboarding.healthconnect.HealthConnectIntroScreen(
                onBack = { nav.safePopBackStack() },
                onSkip = {
                    nav.navigate(Routes.PLAN_PROGRESS) {
                        popUpTo(Routes.ONBOARD_HEALTH_CONNECT) { inclusive = true }
                        launchSingleTop = true
                    }
                },
                onConnected = {
                    nav.navigate(Routes.PLAN_PROGRESS) {
                        popUpTo(Routes.ONBOARD_HEALTH_CONNECT) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }

        // 運算進度頁
        composable(Routes.PLAN_PROGRESS) { backStackEntry ->
            val activity = (LocalContext.current.findActivity() ?: hostActivity)
            val vm: com.calai.app.ui.onboarding.progress.ComputationProgressViewModel = viewModel(
                viewModelStoreOwner = backStackEntry,
                factory = HiltViewModelFactory(activity, backStackEntry)
            )
            com.calai.app.ui.onboarding.progress.ComputationProgressScreen(
                vm = vm,
                onDone = {
                    nav.navigate(Routes.ROUTE_PLAN) {
                        popUpTo(Routes.PLAN_PROGRESS) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }

        composable(Routes.ROUTE_PLAN) { backStackEntry ->
            val activity = (LocalContext.current.findActivity() ?: hostActivity)
            val vm: com.calai.app.ui.onboarding.plan.HealthPlanViewModel = viewModel(
                viewModelStoreOwner = backStackEntry,
                factory = HiltViewModelFactory(activity, backStackEntry)
            )
            com.calai.app.ui.onboarding.plan.HealthPlanScreen(vm = vm, onStart = {
                val target = Routes.HOME
                if (isSignedIn == true) {
                    nav.navigate(target) { popUpTo(0) { inclusive = true } }
                } else {
                    nav.navigate("${Routes.REQUIRE_SIGN_IN}?redirect=$target")
                }
            })
        }

        // 登入 Gate
        composable(
            route = "${Routes.REQUIRE_SIGN_IN}?redirect={redirect}",
            arguments = listOf(navArgument("redirect") {
                type = NavType.StringType
                defaultValue = Routes.HOME
            })
        ) { backStackEntry ->
            val redirect = backStackEntry.arguments?.getString("redirect") ?: Routes.HOME

            val snackbarHostState = remember { SnackbarHostState() }
            val scope = rememberCoroutineScope()
            val ctx = LocalContext.current
            val localeKey = currentLocaleKey()
            val currentTag = localeController.tag.ifBlank { "en" }

            val showSheet = remember { mutableStateOf(true) }

            RequireSignInScreen(
                onBack = { nav.safePopBackStack() },
                onGoogleClick = { showSheet.value = true },
                onSkip = {
                    val popped = nav.safePopBackStack()
                    if (!popped) {
                        nav.navigate(redirect) {
                            popUpTo(0) { inclusive = true }
                            launchSingleTop = true
                            restoreState = false
                        }
                    }
                },
                snackbarHostState = snackbarHostState
            )

            key(localeKey) {
                SignInSheetHost(
                    activity = hostActivity,
                    navController = nav,
                    localeTag = currentTag,
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
                        nav.navigate("${Routes.SIGNIN_EMAIL_ENTER}?redirect=$redirect")
                    },

                    onShowError = { msg ->
                        showSheet.value = false
                        scope.launch { snackbarHostState.showSnackbar(msg.toString()) }
                    },

                    // 登入後僅導航；資料寫入由 SignInSheetHost 處理
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

        // HOME + 暫時頁
        composable(Routes.HOME) { backStackEntry ->
            val activity = (LocalContext.current.findActivity() ?: hostActivity)
            val vm: HomeViewModel = viewModel(
                viewModelStoreOwner = backStackEntry,
                factory = HiltViewModelFactory(activity, backStackEntry)
            )
            HomeRoute(
                vm = vm,
                onOpenAlarm = { nav.navigate(Routes.REMINDERS) },
                onOpenCamera = { nav.navigate(Routes.CAMERA) },
                onOpenTab = { tab ->
                    when (tab) {
                        HomeTab.Home -> { /* stay */ }
                        HomeTab.Progress -> nav.navigate(Routes.PROGRESS)
                        HomeTab.Note -> nav.navigate(Routes.NOTE)
                        HomeTab.Fasting -> nav.navigate(Routes.FASTING)
                        HomeTab.Personal -> nav.navigate(Routes.PERSONAL)
                    }
                }
            )
        }

        composable(Routes.PROGRESS) { SimplePlaceholder("Progress") }
        composable(Routes.NOTE) { SimplePlaceholder("Note") }
        composable(Routes.FASTING) { SimplePlaceholder("Fasting") }
        composable(Routes.PERSONAL) { SimplePlaceholder("Personal") }
        composable(Routes.CAMERA) { SimplePlaceholder("Camera") }
        composable(Routes.REMINDERS) { SimplePlaceholder("Reminders") }
    }
}

@Composable
private fun SimplePlaceholder(title: String) {
    Text(
        modifier = Modifier.padding(24.dp),
        text = "TODO: $title page"
    )
}
