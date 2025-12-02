// app/src/main/java/com/calai/app/ui/nav/BiteCalNavHost.kt
package com.calai.app.ui.nav

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.result.ActivityResultRegistryOwner
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.HiltViewModelFactory
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.calai.app.data.auth.net.SessionBus
import com.calai.app.di.AppEntryPoint
import com.calai.app.i18n.LocalLocaleController
import com.calai.app.i18n.LanguageSessionFlag
import com.calai.app.i18n.currentLocaleKey
import com.calai.app.ui.appentry.AppEntryRoute
import com.calai.app.ui.auth.RequireSignInScreen
import com.calai.app.ui.auth.SignInSheetHost
import com.calai.app.ui.auth.email.EmailCodeScreen
import com.calai.app.ui.auth.email.EmailEnterScreen
import com.calai.app.ui.auth.email.EmailSignInViewModel
import com.calai.app.ui.home.HomeScreen
import com.calai.app.ui.home.HomeTab
import com.calai.app.ui.home.model.HomeViewModel
import com.calai.app.ui.home.ui.fasting.FastingPlansScreen
import com.calai.app.ui.home.ui.fasting.model.FastingPlanViewModel
import com.calai.app.ui.home.ui.personal.PersonalScreen
import com.calai.app.ui.home.ui.personal.details.PersonalDetailsScreen
import com.calai.app.ui.home.ui.personal.model.PersonalViewModel
import com.calai.app.ui.home.ui.water.model.WaterViewModel
import com.calai.app.ui.home.ui.weight.RecordWeightScreen
import com.calai.app.ui.home.ui.weight.model.WeightViewModel
import com.calai.app.ui.home.ui.workout.WorkoutHistoryScreen
import com.calai.app.ui.home.ui.workout.model.WorkoutViewModel
import com.calai.app.ui.landing.LandingScreen
import com.calai.app.ui.onboarding.notifications.NotificationPermissionScreen
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.calai.app.ui.home.ui.weight.WeightScreen
import com.calai.app.ui.onboarding.age.AgeSelectionScreen
import com.calai.app.ui.onboarding.age.AgeSelectionViewModel
import com.calai.app.ui.onboarding.exercise.ExerciseFrequencyScreen
import com.calai.app.ui.onboarding.exercise.ExerciseFrequencyViewModel
import com.calai.app.ui.onboarding.gender.GenderKey
import com.calai.app.ui.onboarding.gender.GenderSelectionScreen
import com.calai.app.ui.onboarding.gender.GenderSelectionViewModel
import com.calai.app.ui.onboarding.goal.GoalSelectionScreen
import com.calai.app.ui.onboarding.goal.GoalSelectionViewModel
import com.calai.app.ui.onboarding.healthconnect.HealthConnectIntroScreen
import com.calai.app.ui.onboarding.height.HeightSelectionScreen
import com.calai.app.ui.onboarding.height.HeightSelectionViewModel
import com.calai.app.ui.onboarding.plan.HealthPlanScreen
import com.calai.app.ui.onboarding.plan.HealthPlanViewModel
import com.calai.app.ui.onboarding.progress.ComputationProgressScreen
import com.calai.app.ui.onboarding.progress.ComputationProgressViewModel
import com.calai.app.ui.onboarding.referralsource.ReferralSourceScreen
import com.calai.app.ui.onboarding.referralsource.ReferralSourceViewModel
import com.calai.app.ui.onboarding.weight.WeightSelectionScreen
import com.calai.app.ui.onboarding.weight.WeightSelectionViewModel
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import kotlinx.coroutines.delay
import com.calai.app.ui.home.ui.components.SuccessTopToast
import com.calai.app.ui.home.ui.components.ErrorTopToast
import com.calai.app.ui.home.ui.components.SuccessTopToast
import com.calai.app.ui.home.ui.personal.details.EditHeightScreen
import com.calai.app.ui.home.ui.personal.details.model.EditHeightViewModel
import com.calai.app.ui.home.ui.weight.EditGoalWeightScreen
import com.calai.app.ui.onboarding.goalweight.WeightGoalScreen
import com.calai.app.ui.onboarding.goalweight.WeightGoalViewModel

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
    const val ONBOARD_GOAL_WEIGHT = "onboard_goal_weight"
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
    const val WORKOUT = "workout"
    const val DAILY = "daily"
    const val FASTING = "fasting"
    const val PERSONAL = "personal"
    const val CAMERA = "camera"
    const val REMINDERS = "reminders"
    const val WORKOUT_HISTORY = "workout_history"
    const val WEIGHT = "weight"
    const val RECORD_WEIGHT = "record_weight"
    const val EDIT_GOAL_WEIGHT = "edit_goal_weight"
    const val PERSONAL_DETAILS = "personal_details"
    const val EDIT_HEIGHT = "edit_height"
}
private fun NavController.GoHome() {
    // 1) back stack 裡有 HOME → 直接 pop 回 HOME
    val popped = popBackStack(Routes.HOME, inclusive = false)
    if (popped) return

    // 2) back stack 沒 HOME（極少數）→ 直接導回 HOME，並清乾淨
    navigate(Routes.HOME) {
        popUpTo(0) { inclusive = true }
        launchSingleTop = true
        restoreState = true
    }
}

private fun toHttpUriOrNull(raw: String?): Uri? {
    val s = raw?.trim().orEmpty()
    if (s.isBlank()) return null
    val uri = runCatching { Uri.parse(s) }.getOrNull() ?: return null
    return uri.takeIf { it.scheme == "http" || it.scheme == "https" }
}

private tailrec fun Context.findActivity(): Activity? =
    when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }

private fun NavController.safePopBackStack(): Boolean =
    previousBackStackEntry != null && popBackStack()

@Composable
fun BiteCalNavHost(
    hostActivity: ComponentActivity,
    modifier: Modifier = Modifier,
    onSetLocale: (String) -> Unit,
) {
    val nav = rememberNavController()

    val appCtx = LocalContext.current.applicationContext
    val ep = remember(appCtx) { EntryPointAccessors.fromApplication(appCtx, AppEntryPoint::class.java) }

    val authState = remember(ep) { ep.authState() }
    val isSignedIn by authState.isSignedInFlow.collectAsState(initial = null)

    val profileRepo = remember(ep) { ep.profileRepository() }

    val weightRepo  = remember(ep) { ep.weightRepository() }

    val store = remember(ep) { ep.userProfileStore() }

    // ✅ NEW: HealthPlanRepository（用來 flush pending）
    val healthPlanRepo = remember(ep) { ep.healthPlanRepository() }

    val localeController = LocalLocaleController.current

    // ✅ NEW: 只要登入狀態變成 true，就保險 flush 一次（不影響流程，best-effort）
    LaunchedEffect(isSignedIn) {
        if (isSignedIn == true) {
            withContext(Dispatchers.IO) {
                runCatching { healthPlanRepo.flushPendingBestEffort() }
            }
        }
    }

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
                onStart = { nav.navigate(Routes.ONBOARD_GENDER) { launchSingleTop = true } },
                onLogin = {
                    // 使用者主動點「登入」：自動開啟 Sheet
                    nav.navigate("${Routes.REQUIRE_SIGN_IN}?redirect=${Routes.HOME}&auto=true")
                },
                onSetLocale = { tag ->
                    localeControllerLocal.set(tag)
                    com.calai.app.i18n.LanguageManager.applyLanguage(tag)
                    onSetLocale(tag)
                    scope.launch {
                        withContext(Dispatchers.IO) { runCatching { store.setLocaleTag(tag) } }
                    }
                },
            )
        }

        // ===== Email：輸入 Email（帶 redirect + uploadLocal）=====
        composable(
            route = "${Routes.SIGNIN_EMAIL_ENTER}?redirect={redirect}&uploadLocal={uploadLocal}",
            arguments = listOf(
                navArgument("redirect") { type = NavType.StringType; defaultValue = Routes.HOME },
                navArgument("uploadLocal") { type = NavType.BoolType; defaultValue = false }
            )
        ) { backStackEntry ->
            val activity = (LocalContext.current.findActivity() ?: hostActivity)
            val vm: EmailSignInViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
                viewModelStoreOwner = backStackEntry,
                factory = HiltViewModelFactory(activity, backStackEntry)
            )
            val redirect = backStackEntry.arguments?.getString("redirect") ?: Routes.HOME
            val uploadLocal = backStackEntry.arguments?.getBoolean("uploadLocal") ?: false

            EmailEnterScreen(
                vm = vm,
                onBack = { nav.safePopBackStack() },
                onSent = { email ->
                    nav.navigate("${Routes.SIGNIN_EMAIL_CODE}?email=$email&redirect=$redirect&uploadLocal=$uploadLocal")
                }
            )
        }

        // ===== Email：輸入驗證碼畫面（帶 redirect + uploadLocal）=====
        composable(
            route = "${Routes.SIGNIN_EMAIL_CODE}?email={email}&redirect={redirect}&uploadLocal={uploadLocal}",
            arguments = listOf(
                navArgument("email") { type = NavType.StringType; defaultValue = "" },
                navArgument("redirect") { type = NavType.StringType; defaultValue = Routes.HOME },
                navArgument("uploadLocal") { type = NavType.BoolType; defaultValue = false }
            )
        ) { backStackEntry ->
            val activity = (LocalContext.current.findActivity() ?: hostActivity)
            val vm: EmailSignInViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
                viewModelStoreOwner = backStackEntry,
                factory = HiltViewModelFactory(activity, backStackEntry)
            )
            val email = backStackEntry.arguments?.getString("email") ?: ""
            val redirect = backStackEntry.arguments?.getString("redirect") ?: Routes.HOME
            val uploadLocal = backStackEntry.arguments?.getBoolean("uploadLocal") ?: false

            val currentTag = localeController.tag.ifBlank { "en" }
            val scope = rememberCoroutineScope()

            EmailCodeScreen(
                vm = vm,
                email = email,
                onBack = { nav.safePopBackStack() },
                onSuccess = {
                    scope.launch {
                        // ★ 先印一個 flow log，確定有進來
                        val dest = withContext(Dispatchers.IO) {
                            val exists = runCatching { profileRepo.existsOnServer() }.getOrDefault(false)
                            if (uploadLocal) {
                                // ★ 從 ROUTE_PLAN 帶上來：一定 upsert 本機資料（不管 exists）
                                runCatching { store.setLocaleTag(currentTag) }
                                runCatching { profileRepo.upsertFromLocal() }
                                runCatching { store.setHasServerProfile(true) }
                                runCatching { weightRepo.ensureBaseline() }   // ← 在這裡打 /baseline
                                // ✅ NEW: 登入成功後補送 pending health plan
                                runCatching { healthPlanRepo.flushPendingBestEffort() }
                                Routes.HOME
                            } else if (exists) {
                                // 既有用戶從 Landing 登入：只需補語系改變（若本次有變）
                                val changedThisSession = LanguageSessionFlag.consumeChanged()
                                if (changedThisSession) runCatching { profileRepo.updateLocaleOnly(currentTag) }
                                runCatching { store.setHasServerProfile(true) }
                                // ✅ NEW: 登入成功後補送 pending health plan
                                runCatching { healthPlanRepo.flushPendingBestEffort() }
                                Routes.HOME
                            } else {
                                // 首次登入且不是從 ROUTE_PLAN 來：照流程從 Gender 開始
                                runCatching { store.setHasServerProfile(false) }
                                Routes.ONBOARD_GENDER
                            }
                        }

                        nav.navigate(dest) {
                            popUpTo(Routes.REQUIRE_SIGN_IN) { inclusive = true }
                            launchSingleTop = true
                            restoreState = false
                        }
                    }
                }
            )
        }

        // ===== Onboarding：性別 → ... → Plan =====
        composable(Routes.ONBOARD_GENDER) { backStackEntry ->
            val activity = (LocalContext.current.findActivity() ?: hostActivity)
            val vm: GenderSelectionViewModel = viewModel(
                viewModelStoreOwner = backStackEntry,
                factory = HiltViewModelFactory(activity, backStackEntry)
            )
            GenderSelectionScreen(
                vm = vm,
                onBack = { nav.safePopBackStack() },
                onNext = { _: GenderKey ->
                    nav.navigate(Routes.ONBOARD_REFERRAL) { launchSingleTop = true }
                }
            )
        }

        composable(Routes.ONBOARD_REFERRAL) { backStackEntry ->
            val activity = (LocalContext.current.findActivity() ?: hostActivity)
            val vm: ReferralSourceViewModel = viewModel(
                viewModelStoreOwner = backStackEntry,
                factory = HiltViewModelFactory(activity, backStackEntry)
            )
            ReferralSourceScreen(
                vm = vm,
                onBack = { nav.safePopBackStack() },
                onNext = { nav.navigate(Routes.ONBOARD_AGE) { launchSingleTop = true } }
            )
        }

        composable(Routes.ONBOARD_AGE) { backStackEntry ->
            val activity = (LocalContext.current.findActivity() ?: hostActivity)
            val vm: AgeSelectionViewModel = viewModel(
                viewModelStoreOwner = backStackEntry,
                factory = HiltViewModelFactory(activity, backStackEntry)
            )
            AgeSelectionScreen(
                vm = vm,
                onBack = { nav.safePopBackStack() },
                onNext = { nav.navigate(Routes.ONBOARD_HEIGHT) { launchSingleTop = true } }
            )
        }

        composable(Routes.ONBOARD_HEIGHT) { backStackEntry ->
            val activity = (LocalContext.current.findActivity() ?: hostActivity)
            val vm: HeightSelectionViewModel = viewModel(
                viewModelStoreOwner = backStackEntry,
                factory = HiltViewModelFactory(activity, backStackEntry)
            )
            HeightSelectionScreen(
                vm = vm,
                onBack = { nav.safePopBackStack() },
                onNext = { nav.navigate(Routes.ONBOARD_WEIGHT) { launchSingleTop = true } }
            )
        }

        composable(Routes.ONBOARD_WEIGHT) { backStackEntry ->
            val activity = (LocalContext.current.findActivity() ?: hostActivity)
            val vm: WeightSelectionViewModel = viewModel(
                viewModelStoreOwner = backStackEntry,
                factory = HiltViewModelFactory(activity, backStackEntry)
            )
            WeightSelectionScreen(
                vm = vm,
                onBack = { nav.safePopBackStack() },
                onNext = { nav.navigate(Routes.ONBOARD_EXERCISE_FREQ) { launchSingleTop = true } }
            )
        }

        composable(Routes.ONBOARD_EXERCISE_FREQ) { backStackEntry ->
            val activity = (LocalContext.current.findActivity() ?: hostActivity)
            val vm: ExerciseFrequencyViewModel = viewModel(
                viewModelStoreOwner = backStackEntry,
                factory = HiltViewModelFactory(activity, backStackEntry)
            )
            ExerciseFrequencyScreen(
                vm = vm,
                onBack = { nav.safePopBackStack() },
                onNext = { nav.navigate(Routes.ONBOARD_GOAL) { launchSingleTop = true } }
            )
        }

        composable(Routes.ONBOARD_GOAL) { backStackEntry ->
            val activity = (LocalContext.current.findActivity() ?: hostActivity)
            val vm: GoalSelectionViewModel = viewModel(
                viewModelStoreOwner = backStackEntry,
                factory = HiltViewModelFactory(activity, backStackEntry)
            )
            GoalSelectionScreen(
                vm = vm,
                onBack = { nav.safePopBackStack() },
                onNext = { nav.navigate(Routes.ONBOARD_GOAL_WEIGHT) { launchSingleTop = true } }
            )
        }

        composable(Routes.ONBOARD_GOAL_WEIGHT) { backStackEntry ->
            val activity = (LocalContext.current.findActivity() ?: hostActivity)
            val vm: WeightGoalViewModel = viewModel(
                viewModelStoreOwner = backStackEntry,
                factory = HiltViewModelFactory(activity, backStackEntry)
            )
            WeightGoalScreen(
                vm = vm,
                onBack = { nav.safePopBackStack() },
                onNext = { nav.navigate(Routes.ONBOARD_NOTIF) { launchSingleTop = true } }
            )
        }

        // =====★ 調整這一段：在 route 外層提供 ActivityResultRegistryOwner ★=====
        composable(Routes.ONBOARD_NOTIF) { backStackEntry ->
            val ctx = LocalContext.current
            // 1) 優先取當前 Activity；2) 退回用你外層傳入的 hostActivity
            val owner: ActivityResultRegistryOwner? =
                (ctx.findActivity() as? ComponentActivity) ?: (hostActivity as? ComponentActivity)

            if (owner != null) {
                CompositionLocalProvider(LocalActivityResultRegistryOwner provides owner) {
                    NotificationPermissionScreen(
                        onBack = { nav.safePopBackStack() },
                        onNext = { nav.navigate(Routes.ONBOARD_HEALTH_CONNECT) { launchSingleTop = true } }
                    )
                }
            } else {
                // 極少數情境（例如 Preview 或特殊容器）取不到 owner：畫面照常顯示，
                // 你的 NotificationPermissionScreen 會走「不建 launcher → 直接 onNext()」路徑，不會崩。
                NotificationPermissionScreen(
                    onBack = { nav.safePopBackStack() },
                    onNext = { nav.navigate(Routes.ONBOARD_HEALTH_CONNECT) { launchSingleTop = true } }
                )
            }
        }

        composable(Routes.ONBOARD_HEALTH_CONNECT) { backStackEntry ->
            val ctx = LocalContext.current
            val activity = (ctx.findActivity() as? ComponentActivity)
                ?: (hostActivity as? ComponentActivity)  // 你專案裡已經有 hostActivity 的話，這行可留作備援

            if (activity != null) {
                // ★ 關鍵：在 route 外層提供 Owner，確保 rememberLauncherForActivityResult 有可用的 registry
                CompositionLocalProvider(LocalActivityResultRegistryOwner provides activity) {
                    HealthConnectIntroScreen(
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
            } else {
                // 拿不到 owner（極少數情況，例如 Preview）→ 畫面照常，按「繼續」會直接 onSkip，不會閃退
                HealthConnectIntroScreen(
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
        }

        // 運算進度頁
        composable(Routes.PLAN_PROGRESS) { backStackEntry ->
            val activity = (LocalContext.current.findActivity() ?: hostActivity)
            val vm: ComputationProgressViewModel = viewModel(
                viewModelStoreOwner = backStackEntry,
                factory = HiltViewModelFactory(activity, backStackEntry)
            )
            ComputationProgressScreen(
                vm = vm,
                onDone = {
                    nav.navigate(Routes.ROUTE_PLAN) {
                        popUpTo(Routes.PLAN_PROGRESS) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }

        // ROUTE_PLAN：未登入 → Gate(禁止 Skip)；已登入 → 先 upsert 再進 HOME
        composable(Routes.ROUTE_PLAN) { backStackEntry ->
            val activity = (LocalContext.current.findActivity() ?: hostActivity)
            val vm: HealthPlanViewModel = viewModel(
                viewModelStoreOwner = backStackEntry,
                factory = HiltViewModelFactory(activity, backStackEntry)
            )
            val routeScope = rememberCoroutineScope()

            HealthPlanScreen(
                vm = vm,
                onStart = {
                    val goal = Routes.HOME

                    routeScope.launch {
                        // ✅ 先存 health plan（best-effort：未登入/失敗會 pending）
                        withContext(Dispatchers.IO) {
                            runCatching { vm.savePlanBestEffort(source = "ONBOARDING") }
                        }

                        if (isSignedIn == true) {
                            // ✅ 已登入：補 upsert + baseline + flush，再進 HOME
                            withContext(Dispatchers.IO) {
                                runCatching { profileRepo.upsertFromLocal() }
                                runCatching { store.setHasServerProfile(true) }
                                runCatching { weightRepo.ensureBaseline() }
                                runCatching { healthPlanRepo.flushPendingBestEffort() }
                            }

                            nav.navigate(goal) {
                                popUpTo(0) { inclusive = true }
                                launchSingleTop = true
                                restoreState = false
                            }
                        } else {
                            // ✅ 未登入：先存 pending（上面已做），再進 Gate 自動彈 Sheet
                            nav.navigate("${Routes.REQUIRE_SIGN_IN}?redirect=$goal&auto=true&uploadLocal=true")
                        }
                    }
                }
            )
        }

        // Gate：支援 auto + uploadLocal，且可禁止 Skip
        composable(
            route = "${Routes.REQUIRE_SIGN_IN}?redirect={redirect}&auto={auto}&uploadLocal={uploadLocal}",
            arguments = listOf(
                navArgument("redirect") { type = NavType.StringType; defaultValue = Routes.HOME },
                navArgument("auto") { type = NavType.BoolType; defaultValue = false },
                navArgument("uploadLocal") { type = NavType.BoolType; defaultValue = false }
            )
        ) { backStackEntry ->
            val redirect = backStackEntry.arguments?.getString("redirect") ?: Routes.HOME
            val auto = backStackEntry.arguments?.getBoolean("auto") ?: false
            val uploadLocal = backStackEntry.arguments?.getBoolean("uploadLocal") ?: false

            val snackbarHostState = remember { SnackbarHostState() }
            val scope = rememberCoroutineScope()
            val ctx = LocalContext.current
            val localeKey = currentLocaleKey()
            val currentTag = localeController.tag.ifBlank { "en" }

            val showSheet = remember { mutableStateOf(auto) }

            RequireSignInScreen(
                onBack = { nav.safePopBackStack() },
                onGoogleClick = { showSheet.value = true },
                onSkip = {
                    if (uploadLocal) {
                        // 來自 ROUTE_PLAN：允許返回 ROUTE_PLAN（不登入先回規劃頁）
                        val popped = nav.popBackStack(Routes.ROUTE_PLAN, inclusive = false)
                        if (!popped) {
                            nav.navigate(Routes.ROUTE_PLAN) {
                                launchSingleTop = true
                                restoreState = false
                            }
                        }
                    } else {
                        // 其他情境（例如從 Landing 來）：維持原本邏輯
                        val popped = nav.safePopBackStack()
                        if (!popped) {
                            nav.navigate(redirect) {
                                popUpTo(0) { inclusive = true }
                                launchSingleTop = true
                                restoreState = false
                            }
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

                    uploadLocalOnLogin = uploadLocal,

                    onGoogle = {
                        showSheet.value = false
                        scope.launch {
                            withContext(Dispatchers.IO) {
                                runCatching { healthPlanRepo.flushPendingBestEffort() }
                            }
                        }
                    },
                    onEmail = {
                        showSheet.value = false
                        nav.navigate("${Routes.SIGNIN_EMAIL_ENTER}?redirect=$redirect&uploadLocal=$uploadLocal")
                    },
                    onShowError = { msg ->
                        showSheet.value = false
                        scope.launch { snackbarHostState.showSnackbar(msg.toString()) }
                    },
                )
            }
        }

        composable(Routes.HOME) { backStackEntry ->
            val activity = (LocalContext.current.findActivity() ?: hostActivity)
            val vm: HomeViewModel = viewModel(
                viewModelStoreOwner = backStackEntry,
                factory = HiltViewModelFactory(activity, backStackEntry)
            )
            val fastingVm: FastingPlanViewModel = viewModel(
                viewModelStoreOwner = backStackEntry,
                factory = HiltViewModelFactory(activity, backStackEntry)
            )
            val waterVm: WaterViewModel = viewModel(
                viewModelStoreOwner = backStackEntry,
                factory = HiltViewModelFactory(activity, backStackEntry)
            )
            val workoutVm: WorkoutViewModel = viewModel(
                viewModelStoreOwner = backStackEntry,
                factory = HiltViewModelFactory(activity, backStackEntry)
            )
            val weightVm: WeightViewModel = viewModel(
                viewModelStoreOwner = backStackEntry,
                factory = HiltViewModelFactory(activity, backStackEntry)
            )
            HomeScreen(
                vm = vm,
                waterVm = waterVm,
                workoutVm = workoutVm,
                fastingVm = fastingVm,
                weightVm = weightVm,
                onOpenAlarm = {
                    nav.navigate(Routes.REMINDERS) {
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                onOpenCamera = {
                    nav.navigate(Routes.CAMERA) {
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                onOpenTab = { tab ->
                    when (tab) {
                        HomeTab.Home -> Unit
                        HomeTab.Progress -> nav.navigate(Routes.PROGRESS) {
                            launchSingleTop = true
                            restoreState = true
                        }
                        HomeTab.Workout -> nav.navigate(Routes.WORKOUT_HISTORY) {
                            launchSingleTop = true
                            restoreState = true
                        }
                        HomeTab.Fasting -> nav.navigate(Routes.FASTING) {
                            launchSingleTop = true
                            restoreState = true
                        }
                        HomeTab.Personal -> nav.navigate(Routes.PERSONAL) {
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                onOpenFastingPlans = {
                    nav.navigate(Routes.FASTING) {
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                onOpenActivityHistory = {
                    nav.navigate(Routes.WORKOUT_HISTORY) {
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                onOpenWeight = {
                    nav.navigate(Routes.WEIGHT) {
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                onQuickLogWeight = {
                    nav.navigate(Routes.RECORD_WEIGHT) {
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }

        composable(Routes.PROGRESS) { SimplePlaceholder("Progress") }

        composable(Routes.FASTING) { backStackEntry ->
            val activity = (LocalContext.current.findActivity() ?: hostActivity)
            val homeBackStackEntry = remember(backStackEntry) { nav.getBackStackEntry(Routes.HOME) }

            val fastingVm: FastingPlanViewModel = viewModel(
                viewModelStoreOwner = homeBackStackEntry,
                factory = HiltViewModelFactory(activity, homeBackStackEntry)
            )
            val GoHome = remember(nav) { { nav.GoHome() } }
            // ✅ 系統返回鍵也回 HOME
            BackHandler { GoHome() }
            FastingPlansScreen(
                vm = fastingVm,
                onBack = GoHome // ✅ UI 返回也回 HOME
            )
        }

        composable(Routes.WORKOUT_HISTORY) { backStackEntry ->
            val activity = (LocalContext.current.findActivity() ?: hostActivity)
            val homeBackStackEntry = remember(backStackEntry) { nav.getBackStackEntry(Routes.HOME) }

            val workoutVm: WorkoutViewModel = viewModel(
                viewModelStoreOwner = homeBackStackEntry,
                factory = HiltViewModelFactory(activity, homeBackStackEntry)
            )
            LaunchedEffect(Unit) { workoutVm.init() }
            val GoHome = remember(nav) { { nav.GoHome() } }
            // ✅ 系統返回鍵也回 HOME
            BackHandler { GoHome() }
            WorkoutHistoryScreen(
                vm = workoutVm,
                onBack = GoHome
            )
        }

        // === ★ WEIGHT 主畫面（與 RECORD_WEIGHT 共用 HOME 作用域的 WeightViewModel） ===
        composable(Routes.WEIGHT) { backStackEntry ->
            val activity = (LocalContext.current.findActivity() ?: hostActivity)
            val homeBackStackEntry = remember(backStackEntry) { nav.getBackStackEntry(Routes.HOME) }
            val vm: WeightViewModel = viewModel(
                viewModelStoreOwner = homeBackStackEntry,
                factory = HiltViewModelFactory(activity, homeBackStackEntry)
            )
            // 視需求：初次進來做初始化
            LaunchedEffect(Unit) { vm.initIfNeeded() }

            WeightScreen(
                vm = vm,
                // Weight 畫面底部「Log Weight」→ Record Weight
                onLogClick = {
                    nav.navigate(Routes.RECORD_WEIGHT) {
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                onEditGoalWeight = {
                    nav.navigate(Routes.EDIT_GOAL_WEIGHT) {
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                onBack = { nav.popBackStack() }
            )
        }

        // === ★ WEIGHT 記錄頁（直接從 Home 開也能拿到同一顆 VM） ===
        composable(Routes.RECORD_WEIGHT) { backStackEntry ->
            val ctx = LocalContext.current

            // 1) 優先用 LocalContext 找到真正的 Activity；找不到就用你傳進來的 hostActivity
            val activity = (ctx.findActivity() ?: hostActivity)

            // 2) WeightViewModel 照舊：綁在 HOME 的 backStackEntry 上，共用同一顆 VM
            val homeBackStackEntry = remember(backStackEntry) { nav.getBackStackEntry(Routes.HOME) }
            val vm: WeightViewModel = viewModel(
                viewModelStoreOwner = homeBackStackEntry,
                factory = HiltViewModelFactory(activity, homeBackStackEntry)
            )

            // 3) 嘗試把 Activity 轉成 ActivityResultRegistryOwner
            val owner: ActivityResultRegistryOwner? =
                (activity as? ActivityResultRegistryOwner)
                    ?: (hostActivity as? ActivityResultRegistryOwner)

            if (owner != null) {
                // ★ 核心：在這個 route 外層明確提供 LocalActivityResultRegistryOwner
                CompositionLocalProvider(LocalActivityResultRegistryOwner provides owner) {
                    RecordWeightScreen(
                        vm = vm,
                        onSaved = { nav.popBackStack() },    // 完成後返回
                        onBack = { nav.popBackStack() }
                    )
                }
            } else {
                // 極少數情況（例如 Preview 或特殊容器）拿不到 owner，就讓 RecordWeightScreen 走自己內建的降級路徑
                RecordWeightScreen(
                    vm = vm,
                    onSaved = { nav.popBackStack() },
                    onBack = { nav.popBackStack() }
                )
            }
        }

        composable(Routes.PERSONAL) { backStackEntry ->
            val activity = (LocalContext.current.findActivity() ?: hostActivity)
            val homeBackStackEntry = remember(backStackEntry) { nav.getBackStackEntry(Routes.HOME) }

            val homeVm: HomeViewModel = viewModel(
                viewModelStoreOwner = homeBackStackEntry,
                factory = HiltViewModelFactory(activity, homeBackStackEntry)
            )

            val personalVm: PersonalViewModel = viewModel(
                viewModelStoreOwner = homeBackStackEntry,
                factory = HiltViewModelFactory(activity, homeBackStackEntry)
            )

            val homeUi by homeVm.ui.collectAsState()
            val pUi by personalVm.ui.collectAsState()

            // ✅ 1) UsersApi 的 pictureUrl 優先
            val avatarFromUsersApi = remember(pUi.pictureUrl) {
                toHttpUriOrNull(pUi.pictureUrl)
            }

            // ✅ 2) 沒有才 fallback 你原本 summary 的 avatar
            val avatar = avatarFromUsersApi ?: homeUi.summary?.avatarUrl

            // ✅ 3) UsersApi 的 name
            val nameText = pUi.name?.takeIf { it.isNotBlank() } ?: "—"

            // ✅ 4) age 先用 ProfileApi 回來的（你目前就是這樣）
            val ageText = pUi.profile?.age?.let { "${it} years old" } ?: "—"

            PersonalScreen(
                avatarUrl = avatar,
                profileName = nameText,
                ageText = ageText,
                currentTab = HomeTab.Personal,
                onOpenCamera = { nav.navigate(Routes.CAMERA) { launchSingleTop = true; restoreState = true } },
                onOpenTab = { tab ->
                    when (tab) {
                        HomeTab.Home -> nav.navigate(Routes.HOME) { launchSingleTop = true; restoreState = true }
                        HomeTab.Progress -> nav.navigate(Routes.PROGRESS) { launchSingleTop = true; restoreState = true }
                        HomeTab.Workout -> nav.navigate(Routes.WORKOUT_HISTORY) { launchSingleTop = true; restoreState = true }
                        HomeTab.Fasting -> nav.navigate(Routes.FASTING) { launchSingleTop = true; restoreState = true }
                        HomeTab.Personal -> Unit
                    }
                },
                onOpenGoalAndCurrentWeight = { nav.navigate(Routes.WEIGHT) { launchSingleTop = true; restoreState = true } },
                onOpenWeightHistory = { nav.navigate(Routes.WEIGHT) { launchSingleTop = true; restoreState = true } },
                onOpenPersonalDetails = { nav.navigate(Routes.PERSONAL_DETAILS) },
            )
        }

        composable(Routes.PERSONAL_DETAILS) { backStackEntry ->
            val activity = (LocalContext.current.findActivity() ?: hostActivity)
            val homeBackStackEntry = remember(backStackEntry) { nav.getBackStackEntry(Routes.HOME) }

            val personalVm: PersonalViewModel = viewModel(
                viewModelStoreOwner = homeBackStackEntry,
                factory = HiltViewModelFactory(activity, homeBackStackEntry)
            )

            val weightVm: WeightViewModel = viewModel(
                viewModelStoreOwner = homeBackStackEntry,
                factory = HiltViewModelFactory(activity, homeBackStackEntry)
            )

            LaunchedEffect(Unit) { weightVm.initIfNeeded() }

            val pUi by personalVm.ui.collectAsState()
            val wUi by weightVm.ui.collectAsState()

            Box(Modifier.fillMaxSize()) {

                PersonalDetailsScreen(
                    profile = pUi.profile,
                    unit = wUi.unit,
                    goalKgFromWeightVm = wUi.goal,
                    goalLbsFromWeightVm = wUi.goalLbs,
                    currentKgFromTimeseries = wUi.current,
                    currentLbsFromTimeseries = wUi.currentLbs,
                    onBack = { nav.popBackStack() },
                    onChangeGoal = { nav.navigate(Routes.EDIT_GOAL_WEIGHT) },
                    onEditCurrentWeight = { nav.navigate(Routes.RECORD_WEIGHT) },
                    onEditHeight = { nav.navigate(Routes.EDIT_HEIGHT) }
                )

                // ✅ 優先顯示 error（避免成功/失敗同時跳）
                val errorMsg = wUi.error?.takeIf { it.isNotBlank() }
                val successMsg = wUi.toastMessage?.takeIf { it.isNotBlank() }

                when {
                    errorMsg != null -> {
                        ErrorTopToast(
                            message = errorMsg,
                            modifier = Modifier.align(Alignment.TopCenter)
                        )
                        LaunchedEffect(errorMsg) {
                            delay(2_000)
                            weightVm.clearError()
                        }
                    }

                    successMsg != null -> {
                        SuccessTopToast(
                            message = successMsg,
                            modifier = Modifier.align(Alignment.TopCenter),
                            minWidth = 150.dp,
                            minHeight = 30.dp
                        )
                        LaunchedEffect(successMsg) {
                            delay(2_000)
                            weightVm.clearToast()
                        }
                    }
                }
            }
        }

        composable(Routes.EDIT_GOAL_WEIGHT) { backStackEntry ->
            val ctx = LocalContext.current
            val activity = (ctx.findActivity() ?: hostActivity)
            val homeBackStackEntry = remember(backStackEntry) { nav.getBackStackEntry(Routes.HOME) }
            val vm: WeightViewModel = viewModel(
                viewModelStoreOwner = homeBackStackEntry,
                factory = HiltViewModelFactory(activity, homeBackStackEntry)
            )
            EditGoalWeightScreen(
                vm = vm,
                onCancel = { nav.popBackStack() },
                onSaved = { nav.popBackStack() }
            )
        }

        composable(Routes.EDIT_HEIGHT) { backStackEntry ->
            val activity = (LocalContext.current.findActivity() ?: hostActivity)
            val homeBackStackEntry = remember(backStackEntry) { nav.getBackStackEntry(Routes.HOME) }

            val personalVm: PersonalViewModel = viewModel(
                viewModelStoreOwner = homeBackStackEntry,
                factory = HiltViewModelFactory(activity, homeBackStackEntry)
            )

            val vm: EditHeightViewModel = viewModel(
                viewModelStoreOwner = homeBackStackEntry,
                factory = HiltViewModelFactory(activity, homeBackStackEntry)
            )

            EditHeightScreen(
                vm = vm,
                onBack = { nav.popBackStack() },
                onSaved = {
                    // ✅ 讓 PersonalDetails (profile 來自 server) 也能更新顯示
                    personalVm.refreshProfileOnly()
                    nav.popBackStack()
                }
            )
        }
        composable(Routes.CAMERA) { SimplePlaceholder("Camera") }
        composable(Routes.REMINDERS) { SimplePlaceholder("Reminders") }

    }
}

@Composable
private fun SimplePlaceholder(title: String) {
    Text(modifier = Modifier.padding(24.dp), text = "TODO: $title page")
}
