package com.calai.bitecal.ui.nav

import com.calai.bitecal.R
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
import com.calai.bitecal.data.auth.net.SessionBus
import com.calai.bitecal.di.AppEntryPoint
import com.calai.bitecal.i18n.LocalLocaleController
import com.calai.bitecal.i18n.LanguageSessionFlag
import com.calai.bitecal.i18n.currentLocaleKey
import com.calai.bitecal.ui.appentry.AppEntryRoute
import com.calai.bitecal.ui.auth.RequireSignInScreen
import com.calai.bitecal.ui.auth.SignInSheetHost
import com.calai.bitecal.ui.auth.email.EmailCodeScreen
import com.calai.bitecal.ui.auth.email.EmailEnterScreen
import com.calai.bitecal.ui.auth.email.EmailSignInViewModel
import com.calai.bitecal.ui.home.HomeScreen
import com.calai.bitecal.ui.home.HomeTab
import com.calai.bitecal.ui.home.model.HomeViewModel
import com.calai.bitecal.ui.home.ui.fasting.FastingPlansScreen
import com.calai.bitecal.ui.home.ui.fasting.model.FastingPlanViewModel
import com.calai.bitecal.ui.home.ui.settings.details.PersonalDetailsScreen
import com.calai.bitecal.ui.home.ui.water.model.WaterViewModel
import com.calai.bitecal.ui.home.ui.weight.RecordWeightScreen
import com.calai.bitecal.ui.home.ui.weight.model.WeightViewModel
import com.calai.bitecal.ui.home.ui.workout.WorkoutHistoryScreen
import com.calai.bitecal.ui.home.ui.workout.model.WorkoutViewModel
import com.calai.bitecal.ui.landing.LandingScreen
import com.calai.bitecal.ui.onboarding.notifications.NotificationPermissionScreen
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.calai.bitecal.ui.home.ui.weight.WeightScreen
import com.calai.bitecal.ui.onboarding.age.AgeSelectionScreen
import com.calai.bitecal.ui.onboarding.age.AgeSelectionViewModel
import com.calai.bitecal.ui.onboarding.exercise.ExerciseFrequencyScreen
import com.calai.bitecal.ui.onboarding.exercise.ExerciseFrequencyViewModel
import com.calai.bitecal.ui.onboarding.gender.GenderKey
import com.calai.bitecal.ui.onboarding.gender.GenderSelectionScreen
import com.calai.bitecal.ui.onboarding.gender.GenderSelectionViewModel
import com.calai.bitecal.ui.onboarding.goal.GoalSelectionScreen
import com.calai.bitecal.ui.onboarding.goal.GoalSelectionViewModel
import com.calai.bitecal.ui.onboarding.healthconnect.HealthConnectIntroScreen
import com.calai.bitecal.ui.onboarding.height.HeightSelectionScreen
import com.calai.bitecal.ui.onboarding.height.HeightSelectionViewModel
import com.calai.bitecal.ui.onboarding.plan.HealthPlanScreen
import com.calai.bitecal.ui.onboarding.plan.HealthPlanViewModel
import com.calai.bitecal.ui.onboarding.progress.ComputationProgressScreen
import com.calai.bitecal.ui.onboarding.progress.ComputationProgressViewModel
import com.calai.bitecal.ui.onboarding.referralsource.ReferralSourceScreen
import com.calai.bitecal.ui.onboarding.referralsource.ReferralSourceViewModel
import com.calai.bitecal.ui.onboarding.weight.WeightSelectionScreen
import com.calai.bitecal.ui.onboarding.weight.WeightSelectionViewModel
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import com.calai.bitecal.i18n.LanguageManager
import kotlinx.coroutines.delay
import com.calai.bitecal.ui.home.components.toast.SuccessTopToast
import com.calai.bitecal.ui.home.components.toast.ErrorTopToast
import com.calai.bitecal.ui.home.ui.settings.details.EditAgeScreen
import com.calai.bitecal.ui.home.ui.settings.details.EditDailyStepGoalScreen
import com.calai.bitecal.ui.home.ui.settings.details.EditGenderScreen
import com.calai.bitecal.ui.home.ui.settings.details.EditHeightScreen
import com.calai.bitecal.ui.home.ui.settings.details.model.EditAgeViewModel
import com.calai.bitecal.ui.home.ui.settings.details.model.EditDailyStepGoalViewModel
import com.calai.bitecal.ui.home.ui.settings.details.model.EditGenderViewModel
import com.calai.bitecal.ui.home.ui.settings.details.model.EditHeightViewModel
import com.calai.bitecal.ui.home.ui.weight.EditGoalWeightScreen
import com.calai.bitecal.ui.onboarding.goalweight.WeightGoalScreen
import com.calai.bitecal.ui.onboarding.goalweight.WeightGoalViewModel
import androidx.core.net.toUri
import com.calai.bitecal.ui.home.ui.settings.details.EditNutritionGoalsRoute
import com.calai.bitecal.ui.home.ui.settings.details.model.NutritionGoalsViewModel
import androidx.navigation.compose.navigation
import com.calai.bitecal.ui.home.ui.camera.CameraScreen
import com.calai.bitecal.ui.home.ui.foodlog.FoodLogDetailScreen
import com.calai.bitecal.ui.home.ui.foodlog.model.FoodLogFlowViewModel
import com.calai.bitecal.ui.home.ui.settings.SettingsScreen
import com.calai.bitecal.ui.home.ui.settings.details.AutoGenerateGoalsCalcScreen
import com.calai.bitecal.ui.home.ui.settings.details.EditStartingWeightScreen
import com.calai.bitecal.ui.home.ui.settings.details.EditWaterGoalScreen
import com.calai.bitecal.ui.home.ui.settings.details.EditWorkoutGoalScreen
import com.calai.bitecal.ui.home.ui.settings.details.model.AutoGenerateGoalsCalcViewModel
import com.calai.bitecal.ui.home.ui.settings.details.model.EditStartingWeightViewModel
import com.calai.bitecal.ui.home.ui.settings.details.model.EditWaterGoalViewModel
import com.calai.bitecal.ui.home.ui.settings.details.model.EditWorkoutGoalViewModel
import com.calai.bitecal.ui.home.ui.settings.editname.EditNameScreen
import com.calai.bitecal.ui.home.ui.settings.editname.model.EditNameViewModel
import com.calai.bitecal.ui.home.ui.settings.model.SettingsViewModel
import kotlinx.coroutines.flow.collectLatest
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource

object Routes {
    const val LANDING = "landing"
    const val SIGN_IN_EMAIL_ENTER = "signin_email_enter"
    const val SIGN_IN_EMAIL_CODE = "signin_email_code"
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
    const val PROGRESS = "progress"
    const val FASTING = "fasting"
    const val SETTINGS = "settings"
    const val CAMERA = "camera"
    const val REMINDERS = "reminders"
    const val WORKOUT_HISTORY = "workout_history"
    const val WEIGHT = "weight"
    const val RECORD_WEIGHT = "record_weight"
    const val EDIT_GOAL_WEIGHT = "edit_goal_weight"
    const val EDIT_STARTING_WEIGHT = "edit_start_weight"
    const val PERSONAL_DETAILS = "personal_details"
    const val EDIT_HEIGHT = "edit_height"
    const val EDIT_AGE = "edit_age"
    const val EDIT_GENDER = "edit_gender"
    const val EDIT_DAILY_STEP_GOAL = "edit_daily_step_goal"
    const val EDIT_WATER_GOAL = "edit_water_goal"
    const val EDIT_NUTRITION_GOALS = "edit_nutrition_goals"
    const val EDIT_NAME = "edit_name"
    const val EDIT_NAME_INITIAL = "edit_name_initial"
    const val AUTO_GENERATE_GOALS = "auto_generate_goals"
    const val AUTO_GENERATE_EXERCISE_FREQUENCY = "auto_generate_exercise_frequency"
    const val AUTO_GENERATE_HEIGHT = "auto_generate_height"
    const val AUTO_GENERATE_WEIGHT = "auto_generate_weight"
    const val AUTO_GENERATE_GOALS_CALC = "auto_generate_goals_calc"
    const val AUTO_GENERATE_FLOW = "auto_generate_flow"
    const val EDIT_WORKOUT_GOAL = "edit_workout_goal"

    /**
     * Camera Snapshots food log detail
     */
    const val FOOD_LOG_DETAIL = "foodLog/{id}"
    fun foodLogDetail(id: String) = "foodLog/$id"
}

object NavResults {
    const val SUCCESS_TOAST = "success_toast"
    const val ERROR_TOAST = "error_toast"
    const val AUTO_GEN_RELOAD = "auto_gen_reload"
}
private fun NavController.goHome() {
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
    val uri = runCatching { s.toUri() }.getOrNull() ?: return null
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

    val localeController = LocalLocaleController.current

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
                navController = nav,
                onStart = { nav.navigate(Routes.ONBOARD_GENDER) { launchSingleTop = true } },
                onLogin = {
                    // 使用者主動點「登入」：自動開啟 Sheet
                    nav.navigate("${Routes.REQUIRE_SIGN_IN}?redirect=${Routes.HOME}&auto=true")
                },
                onSetLocale = { tag ->
                    localeControllerLocal.set(tag)
                    LanguageManager.applyLanguage(tag)
                    onSetLocale(tag)
                    scope.launch {
                        withContext(Dispatchers.IO) { runCatching { store.setLocaleTag(tag) } }
                    }
                },
            )
        }

        // ===== Email：輸入 Email（帶 redirect + uploadLocal）=====
        composable(
            route = "${Routes.SIGN_IN_EMAIL_ENTER}?redirect={redirect}&uploadLocal={uploadLocal}",
            arguments = listOf(
                navArgument("redirect") { type = NavType.StringType; defaultValue = Routes.HOME },
                navArgument("uploadLocal") { type = NavType.BoolType; defaultValue = false }
            )
        ) { backStackEntry ->
            val activity = (LocalContext.current.findActivity() ?: hostActivity)
            val vm: EmailSignInViewModel = viewModel(
                viewModelStoreOwner = backStackEntry,
                factory = HiltViewModelFactory(activity, backStackEntry)
            )
            val redirect = backStackEntry.arguments?.getString("redirect") ?: Routes.HOME
            val uploadLocal = backStackEntry.arguments?.getBoolean("uploadLocal") ?: false

            EmailEnterScreen(
                vm = vm,
                onBack = { nav.safePopBackStack() },
                onSent = { email ->
                    nav.navigate("${Routes.SIGN_IN_EMAIL_CODE}?email=$email&redirect=$redirect&uploadLocal=$uploadLocal")
                }
            )
        }

        // ===== Email：輸入驗證碼畫面（帶 redirect + uploadLocal）=====
        composable(
            route = "${Routes.SIGN_IN_EMAIL_CODE}?email={email}&redirect={redirect}&uploadLocal={uploadLocal}",
            arguments = listOf(
                navArgument("email") { type = NavType.StringType; defaultValue = "" },
                navArgument("redirect") { type = NavType.StringType; defaultValue = Routes.HOME },
                navArgument("uploadLocal") { type = NavType.BoolType; defaultValue = false }
            )
        ) { backStackEntry ->
            val activity = (LocalContext.current.findActivity() ?: hostActivity)
            val vm: EmailSignInViewModel = viewModel(
                viewModelStoreOwner = backStackEntry,
                factory = HiltViewModelFactory(activity, backStackEntry)
            )
            val email = backStackEntry.arguments?.getString("email") ?: ""
            val uploadLocal = backStackEntry.arguments?.getBoolean("uploadLocal") ?: false

            val currentTag = localeController.tag.ifBlank { "en" }
            val scope = rememberCoroutineScope()
            val entitlementSyncer = remember(ep) { ep.entitlementSyncer() }
            EmailCodeScreen(
                vm = vm,
                email = email,
                onBack = { nav.safePopBackStack() },
                onSuccess = {
                    scope.launch {
                        // ✅ 不阻塞導頁：背景自動 sync 訂閱（無 restore 按鈕）
                        launch(Dispatchers.IO) { entitlementSyncer.syncAfterLoginSilently() }
                        // ★ 先印一個 flow log，確定有進來
                        val dest = withContext(Dispatchers.IO) {
                            val exists = runCatching { profileRepo.existsOnServer() }.getOrDefault(false)
                            if (uploadLocal) {
                                // ★ 從 ROUTE_PLAN 帶上來：一定 upsert 本機資料（不管 exists）
                                runCatching { store.setLocaleTag(currentTag) }
                                runCatching { profileRepo.upsertFromLocalForOnboarding() }
                                runCatching { store.setHasServerProfile(true) }
                                runCatching { weightRepo.ensureBaseline() }   // ← 在這裡打 /baseline
                                Routes.HOME
                            } else if (exists) {
                                // 既有用戶從 Landing 登入：只需補語系改變（若本次有變）
                                val changedThisSession = LanguageSessionFlag.consumeChanged()
                                if (changedThisSession) runCatching { profileRepo.updateLocaleOnly(currentTag) }
                                runCatching { store.setHasServerProfile(true) }
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
        composable(Routes.ONBOARD_NOTIF) {
            val ctx = LocalContext.current
            val owner: ActivityResultRegistryOwner = (ctx.findActivity() as? ComponentActivity) ?: hostActivity

            CompositionLocalProvider(LocalActivityResultRegistryOwner provides owner) {
                NotificationPermissionScreen(
                    onBack = { nav.safePopBackStack() },
                    onNext = { nav.navigate(Routes.ONBOARD_HEALTH_CONNECT) { launchSingleTop = true } }
                )
            }
        }

        composable(Routes.ONBOARD_HEALTH_CONNECT) {
            val ctx = LocalContext.current
            val activity: ComponentActivity = (ctx.findActivity() as? ComponentActivity) ?: hostActivity

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
                        if (isSignedIn == true) {
                            // ✅ 已登入：補 upsert + baseline + flush，再進 HOME
                            withContext(Dispatchers.IO) {
                                runCatching { profileRepo.upsertFromLocalForOnboarding() }
                                runCatching { store.setHasServerProfile(true) }
                                runCatching { weightRepo.ensureBaseline() }
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
            val localeKey = currentLocaleKey()
            val currentTag = localeController.tag.ifBlank { "en" }

            val showSheet = remember { mutableStateOf(auto) }

            RequireSignInScreen(
                onBack = { nav.safePopBackStack() },
                onGoogleClick = {
                    showSheet.value = true
                },
                onEmailClick = {
                    showSheet.value = true
                },
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
                snackBarHostState = snackbarHostState
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
                    },
                    onEmail = {
                        showSheet.value = false
                        nav.navigate("${Routes.SIGN_IN_EMAIL_ENTER}?redirect=$redirect&uploadLocal=$uploadLocal")
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

            // ✅ 讓 HOME 也能顯示「上一頁回傳」的 toast（例如 QuickLogWeight 回來）
            val successFlow = remember(backStackEntry) {
                backStackEntry.savedStateHandle.getStateFlow<String?>(NavResults.SUCCESS_TOAST, null)
            }
            val errorFlow = remember(backStackEntry) {
                backStackEntry.savedStateHandle.getStateFlow<String?>(NavResults.ERROR_TOAST, null)
            }
            val navSuccess by successFlow.collectAsState(initial = null)
            val navError by errorFlow.collectAsState(initial = null)

            LaunchedEffect(isSignedIn) {
                if (isSignedIn == true) vm.refreshAfterLogin()
            }

            Box(Modifier.fillMaxSize()) {
                HomeScreen(
                    vm = vm,
                    waterVm = waterVm,
                    workoutVm = workoutVm,
                    fastingVm = fastingVm,
                    weightVm = weightVm,
                    onOpenCamera = { nav.navigate(Routes.CAMERA) { launchSingleTop = true; restoreState = true } },
                    onOpenTab = { tab ->
                        when (tab) {
                            HomeTab.Home -> Unit
                            HomeTab.Progress -> nav.navigate(Routes.PROGRESS) { launchSingleTop = true; restoreState = true }
                            HomeTab.Weight -> nav.navigate(Routes.WEIGHT) { launchSingleTop = true; restoreState = true }
                            HomeTab.Fasting -> nav.navigate(Routes.FASTING) { launchSingleTop = true; restoreState = true }
                            HomeTab.Workout -> nav.navigate(Routes.WORKOUT_HISTORY) { launchSingleTop = true; restoreState = true }
                            HomeTab.Personal -> nav.navigate(Routes.SETTINGS) { launchSingleTop = true; restoreState = true }
                        }
                    },
                    onOpenFastingPlans = { nav.navigate(Routes.FASTING) { launchSingleTop = true; restoreState = true } },
                    onOpenActivityHistory = { nav.navigate(Routes.WORKOUT_HISTORY) { launchSingleTop = true; restoreState = true } },
                    onOpenWeight = { nav.navigate(Routes.WEIGHT) { launchSingleTop = true; restoreState = true } },
                    onQuickLogWeight = { nav.navigate(Routes.RECORD_WEIGHT) { launchSingleTop = true; restoreState = true } }
                )

                when {
                    !navError.isNullOrBlank() -> {
                        ErrorTopToast(message = navError!!, modifier = Modifier.align(Alignment.TopCenter))
                        LaunchedEffect(navError) {
                            delay(2_000)
                            backStackEntry.savedStateHandle[NavResults.ERROR_TOAST] = null
                        }
                    }
                    !navSuccess.isNullOrBlank() -> {
                        SuccessTopToast(
                            message = navSuccess!!,
                            modifier = Modifier.align(Alignment.TopCenter),
                            minWidth = 150.dp,
                            minHeight = 30.dp
                        )
                        LaunchedEffect(navSuccess) {
                            delay(2_000)
                            backStackEntry.savedStateHandle[NavResults.SUCCESS_TOAST] = null
                        }
                    }
                }
            }
        }

        composable(Routes.PROGRESS) { SimplePlaceholder("Progress") }

        composable(Routes.FASTING) { backStackEntry ->
            val activity = (LocalContext.current.findActivity() ?: hostActivity)
            val homeBackStackEntry = remember(backStackEntry) { nav.getBackStackEntry(Routes.HOME) }

            val fastingVm: FastingPlanViewModel = viewModel(
                viewModelStoreOwner = homeBackStackEntry,
                factory = HiltViewModelFactory(activity, homeBackStackEntry)
            )

            val goHome: () -> Unit = remember(nav) { { nav.goHome() } }

            // 底部導航列在 Fasting 畫面時的行為（先準備好，之後可共用到底部 Bar）
            val onOpenTab: (HomeTab) -> Unit = remember(nav) {
                { tab ->
                    when (tab) {
                        HomeTab.Home -> nav.goHome()
                        HomeTab.Progress -> nav.navigate(Routes.PROGRESS) { launchSingleTop = true; restoreState = true }
                        HomeTab.Weight -> nav.navigate(Routes.WEIGHT) { launchSingleTop = true; restoreState = true }
                        HomeTab.Fasting -> Unit
                        HomeTab.Workout -> nav.navigate(Routes.WORKOUT_HISTORY) { launchSingleTop = true; restoreState = true }
                        HomeTab.Personal -> nav.navigate(Routes.SETTINGS) { launchSingleTop = true; restoreState = true }
                    }
                }
            }
            FastingPlansScreen(
                vm = fastingVm,
                onBack = goHome,
                currentTab = HomeTab.Fasting,      // 保留 API，之後要把 BottomBar 搬進來會用到
                onOpenTab = onOpenTab
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
            val goBackHome = remember(nav) { { nav.goHome() } }
            // ✅ 系統返回鍵也回 HOME
            BackHandler { goBackHome() }
            val onOpenTab: (HomeTab) -> Unit = remember(nav) {
                { tab ->
                    when (tab) {
                        HomeTab.Home -> nav.goHome()
                        HomeTab.Progress -> nav.navigate(Routes.PROGRESS) { launchSingleTop = true; restoreState = true }
                        HomeTab.Weight -> nav.navigate(Routes.WEIGHT) { launchSingleTop = true; restoreState = true }
                        HomeTab.Fasting -> nav.navigate(Routes.FASTING) { launchSingleTop = true; restoreState = true }
                        HomeTab.Workout -> Unit
                        HomeTab.Personal -> nav.navigate(Routes.SETTINGS) { launchSingleTop = true; restoreState = true }
                    }
                }
            }
            WorkoutHistoryScreen(
                vm = workoutVm,
                onBack = goBackHome,
                currentTab = HomeTab.Workout,
                onOpenTab = onOpenTab
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

            // ✅ 接收上一頁（RecordWeight / EditGoalWeight）回傳的成功訊息：只顯示一次
            val successToastFlow = remember(backStackEntry) {
                backStackEntry.savedStateHandle.getStateFlow<String?>(NavResults.SUCCESS_TOAST, null)
            }
            val successToast by successToastFlow.collectAsState(initial = null)

            Box(modifier = Modifier.fillMaxSize()) {

                WeightScreen(
                    vm = vm,
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
                if (!successToast.isNullOrBlank()) {
                    SuccessTopToast(
                        message = successToast!!,
                        modifier = Modifier.align(Alignment.TopCenter),
                        minWidth = 150.dp,
                        minHeight = 30.dp
                    )

                    LaunchedEffect(successToast) {
                        delay(2_000)
                        backStackEntry.savedStateHandle[NavResults.SUCCESS_TOAST] = null // ✅ 消費完清掉
                    }
                }
            }
        }

        composable(Routes.RECORD_WEIGHT) { backStackEntry ->
            val ctx = LocalContext.current
            val activity = (ctx.findActivity() ?: hostActivity)
            val homeBackStackEntry = remember(backStackEntry) { nav.getBackStackEntry(Routes.HOME) }
            val vm: WeightViewModel = viewModel(
                viewModelStoreOwner = homeBackStackEntry,
                factory = HiltViewModelFactory(activity, homeBackStackEntry)
            )

            val settingsVm: SettingsViewModel = viewModel(
                viewModelStoreOwner = homeBackStackEntry,
                factory = HiltViewModelFactory(activity, homeBackStackEntry)
            )

            val owner: ActivityResultRegistryOwner? =
                (activity as? ActivityResultRegistryOwner)
                    ?: (hostActivity as? ActivityResultRegistryOwner)

            if (owner != null) {
                // ★ 核心：在這個 route 外層明確提供 LocalActivityResultRegistryOwner
                CompositionLocalProvider(LocalActivityResultRegistryOwner provides owner) {
                    RecordWeightScreen(
                        vm = vm,
                        onBack = { nav.popBackStack() },
                        onSaved = {
                            // ✅ 只把結果交給「上一頁」顯示
                            nav.previousBackStackEntry
                                ?.savedStateHandle
                                ?.set(NavResults.SUCCESS_TOAST, "Saved successfully!")
                            settingsVm.refreshProfileOnly()
                            nav.popBackStack()
                        }
                    )
                }
            } else {
                // 極少數情況（例如 Preview 或特殊容器）拿不到 owner，就讓 RecordWeightScreen 走自己內建的降級路徑
                RecordWeightScreen(
                    vm = vm,
                    onBack = { nav.popBackStack() },
                    onSaved = {
                        nav.previousBackStackEntry
                            ?.savedStateHandle
                            ?.set(NavResults.SUCCESS_TOAST, "Saved successfully!")
                        settingsVm.refreshProfileOnly()
                        nav.popBackStack()
                    }
                )
            }
        }

        composable(Routes.SETTINGS) { backStackEntry ->
            val activity = (LocalContext.current.findActivity() ?: hostActivity)
            val homeBackStackEntry = remember(backStackEntry) { nav.getBackStackEntry(Routes.HOME) }
            val scope = rememberCoroutineScope()
            val accountRepo = remember(ep) { ep.accountRepository() }

            val homeVm: HomeViewModel = viewModel(
                viewModelStoreOwner = homeBackStackEntry,
                factory = HiltViewModelFactory(activity, homeBackStackEntry)
            )

            val settingsVm: SettingsViewModel  = viewModel(
                viewModelStoreOwner = homeBackStackEntry,
                factory = HiltViewModelFactory(activity, homeBackStackEntry)
            )

            val homeUi by homeVm.ui.collectAsState()
            val pUi by settingsVm.ui.collectAsState()

            // ✅ 讓 PERSONAL 也能顯示「上一頁回傳」的 toast（例如 EditNutritionGoals 回來）
            val successFlow = remember(backStackEntry) {
                backStackEntry.savedStateHandle.getStateFlow<String?>(NavResults.SUCCESS_TOAST, null)
            }
            val errorFlow = remember(backStackEntry) {
                backStackEntry.savedStateHandle.getStateFlow<String?>(NavResults.ERROR_TOAST, null)
            }
            val navSuccess by successFlow.collectAsState(initial = null)
            val navError by errorFlow.collectAsState(initial = null)

            // ✅ 1) UsersApi 的 pictureUrl 優先
            val avatarFromUsersApi = remember(pUi.pictureUrl) {
                toHttpUriOrNull(pUi.pictureUrl)
            }

            // ✅ 2) 沒有才 fallback 你原本 summary 的 avatar
            val avatar = avatarFromUsersApi ?: homeUi.summary?.avatarUrl

            // ✅ 3) UsersApi 的 name
            val nameText = pUi.name?.takeIf { it.isNotBlank() } ?: "—"

            // ✅ 4) age 先用 ProfileApi 回來的
            val ageText = pUi.profile?.age?.let { "$it years old" } ?: "—"

            // ✅ URLs (Privacy Policy 內會包含 Data Deletion Policy)
            val uriHandler = LocalUriHandler.current
            val termsUrl = stringResource(R.string.url_terms)
            val privacyUrl = stringResource(R.string.url_privacy)
            val featureUrl = stringResource(R.string.url_feature_request)
            val supportMailUrl = stringResource(R.string.url_support_email)

            Box(Modifier.fillMaxSize()) {
                SettingsScreen(
                    avatarUrl = avatar,
                    profileName = nameText,
                    ageText = ageText,
                    currentTab = HomeTab.Personal,
                    onOpenCamera = { nav.navigate(Routes.CAMERA) { launchSingleTop = true; restoreState = true } },
                    onOpenTab = { tab ->
                        when (tab) {
                            HomeTab.Home -> nav.navigate(Routes.HOME) { launchSingleTop = true; restoreState = true }
                            HomeTab.Progress -> nav.navigate(Routes.PROGRESS) { launchSingleTop = true; restoreState = true }
                            HomeTab.Weight -> nav.navigate(Routes.WEIGHT) { launchSingleTop = true; restoreState = true }
                            HomeTab.Fasting -> nav.navigate(Routes.FASTING) { launchSingleTop = true; restoreState = true }
                            HomeTab.Workout -> nav.navigate(Routes.WORKOUT_HISTORY) { launchSingleTop = true; restoreState = true }
                            HomeTab.Personal -> Unit
                        }
                    },
                    onOpenEditName = {
                        backStackEntry.savedStateHandle[Routes.EDIT_NAME_INITIAL] = (pUi.name ?: "").trim()
                        nav.navigate(Routes.EDIT_NAME) {
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onOpenAdjustMacros = {
                        nav.navigate(Routes.EDIT_NUTRITION_GOALS) {
                            launchSingleTop = true
                            restoreState = false
                        }
                    },
                    onOpenGoalAndCurrentWeight = {
                        nav.navigate(Routes.PERSONAL_DETAILS) {
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onOpenWeightHistory = {
                        nav.navigate(Routes.WEIGHT) {
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onOpenPersonalDetails = { nav.navigate(Routes.PERSONAL_DETAILS) },

                    // ✅ Terms / Privacy / Support / Feature
                    onOpenTerms = { uriHandler.openUri(termsUrl) },
                    onOpenPrivacy = { uriHandler.openUri(privacyUrl) },
                    onOpenSupportEmail = { uriHandler.openUri(supportMailUrl) },
                    onOpenFeatureRequest = { uriHandler.openUri(featureUrl) },

                    onDeleteAccount = {
                        scope.launch {
                            val r = accountRepo.deleteAccount()
                            if (r.isSuccess) {
                                nav.navigate(Routes.LANDING) {
                                    popUpTo(0) { inclusive = true }
                                    launchSingleTop = true
                                    restoreState = false
                                }
                            } else {
                                backStackEntry.savedStateHandle[NavResults.ERROR_TOAST] =
                                    (r.exceptionOrNull()?.message ?: "Delete account failed")
                            }
                        }
                    }
                )

                when {
                    !navError.isNullOrBlank() -> {
                        ErrorTopToast(message = navError!!, modifier = Modifier.align(Alignment.TopCenter))
                        LaunchedEffect(navError) {
                            delay(2_000)
                            backStackEntry.savedStateHandle[NavResults.ERROR_TOAST] = null
                        }
                    }
                    !navSuccess.isNullOrBlank() -> {
                        SuccessTopToast(
                            message = navSuccess!!,
                            modifier = Modifier.align(Alignment.TopCenter),
                            minWidth = 150.dp,
                            minHeight = 30.dp
                        )
                        LaunchedEffect(navSuccess) {
                            delay(2_000)
                            backStackEntry.savedStateHandle[NavResults.SUCCESS_TOAST] = null
                        }
                    }
                }
            }
        }

        composable(Routes.PERSONAL_DETAILS) { backStackEntry ->
            val activity = (LocalContext.current.findActivity() ?: hostActivity)
            val homeBackStackEntry = remember(backStackEntry) { nav.getBackStackEntry(Routes.HOME) }

            val settingsVm: SettingsViewModel  = viewModel(
                viewModelStoreOwner = homeBackStackEntry,
                factory = HiltViewModelFactory(activity, homeBackStackEntry)
            )

            val weightVm: WeightViewModel = viewModel(
                viewModelStoreOwner = homeBackStackEntry,
                factory = HiltViewModelFactory(activity, homeBackStackEntry)
            )
            LaunchedEffect(Unit) {
                settingsVm.refreshProfileOnly()
                weightVm.initIfNeeded()
            }

            val pUi by settingsVm.ui.collectAsState()
            val wUi by weightVm.ui.collectAsState()

            val successFlow = remember(backStackEntry) {
                backStackEntry.savedStateHandle.getStateFlow<String?>(NavResults.SUCCESS_TOAST, null)
            }
            val errorFlow = remember(backStackEntry) {
                backStackEntry.savedStateHandle.getStateFlow<String?>(NavResults.ERROR_TOAST, null)
            }
            val navSuccess by successFlow.collectAsState(initial = null)
            val navError by errorFlow.collectAsState(initial = null)

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
                    onEditHeight = { nav.navigate(Routes.EDIT_HEIGHT) },
                    onEditAge = { nav.navigate(Routes.EDIT_AGE) },
                    onEditGender = { nav.navigate(Routes.EDIT_GENDER) },
                    onEditDailyStepGoal = { nav.navigate(Routes.EDIT_DAILY_STEP_GOAL) },
                    onEditStartingWeight = { nav.navigate(Routes.EDIT_STARTING_WEIGHT) },
                    onEditDailyWaterGoal = { nav.navigate(Routes.EDIT_WATER_GOAL) },
                    onEditDailyWorkoutGoal = { nav.navigate(Routes.EDIT_WORKOUT_GOAL) },
                )
                when {
                    !navError.isNullOrBlank() -> {
                        ErrorTopToast(
                            message = navError!!,
                            modifier = Modifier.align(Alignment.TopCenter)
                        )
                        LaunchedEffect(navError) {
                            delay(2_000)
                            backStackEntry.savedStateHandle[NavResults.ERROR_TOAST] = null
                        }
                    }

                    !navSuccess.isNullOrBlank() -> {
                        SuccessTopToast(
                            message = navSuccess!!,
                            modifier = Modifier.align(Alignment.TopCenter),
                            minWidth = 150.dp,
                            minHeight = 30.dp
                        )
                        LaunchedEffect(navSuccess) {
                            delay(2_000)
                            backStackEntry.savedStateHandle[NavResults.SUCCESS_TOAST] = null
                        }
                    }
                }
            }
        }

        composable(Routes.EDIT_NAME) { backStackEntry ->
            val activity = (LocalContext.current.findActivity() ?: hostActivity)
            val homeBackStackEntry = remember(backStackEntry) { nav.getBackStackEntry(Routes.HOME) }

            val vm: EditNameViewModel = viewModel(
                viewModelStoreOwner = homeBackStackEntry,
                factory = HiltViewModelFactory(activity, homeBackStackEntry)
            )

            val settingsVm: SettingsViewModel  = viewModel(
                viewModelStoreOwner = homeBackStackEntry,
                factory = HiltViewModelFactory(activity, homeBackStackEntry)
            )

            val ui by vm.ui.collectAsState()

            // ✅ 從上一頁（Personal）拿初始 name（拿不到就 null）
            val initialNameFromPersonal = remember {
                nav.previousBackStackEntry
                    ?.savedStateHandle
                    ?.get<String>(Routes.EDIT_NAME_INITIAL)
            }

            LaunchedEffect(Unit) {
                vm.load(initialNameFromPersonal)
            }

            LaunchedEffect(Unit) {
                vm.events.collectLatest { e ->
                    when (e) {
                        is EditNameViewModel.Event.Saved -> {
                            // ✅ 回 Personal 顯示 toast（你 PERSONAL 已經有讀 NavResults.SUCCESS_TOAST）
                            nav.previousBackStackEntry
                                ?.savedStateHandle
                                ?.set(NavResults.SUCCESS_TOAST, "Saved successfully!")

                            // ✅ 刷新 Users(me) 的 name（如果你已加 refreshMeOnly 就用它；沒加就先 refresh()）
                            runCatching { settingsVm.refreshMeOnly() }.getOrElse { settingsVm.refresh() }

                            nav.popBackStack()
                        }
                        is EditNameViewModel.Event.Error -> {
                            // 你也可改成丟 ERROR_TOAST 給上一頁
                            // nav.previousBackStackEntry?.savedStateHandle?.set(NavResults.ERROR_TOAST, e.message)
                        }
                    }
                }
            }
            EditNameScreen(
                input = ui.input,
                canSave = ui.canSave(),
                isSaving = ui.isSaving,
                errorText = ui.error,
                onBack = { nav.popBackStack() },
                onInputChange = vm::onInputChange,
                onSaved = vm::save
            )
        }

        composable(Routes.EDIT_GOAL_WEIGHT) { backStackEntry ->
            val ctx = LocalContext.current
            val activity = (ctx.findActivity() ?: hostActivity)
            val homeBackStackEntry = remember(backStackEntry) { nav.getBackStackEntry(Routes.HOME) }
            val vm: WeightViewModel = viewModel(
                viewModelStoreOwner = homeBackStackEntry,
                factory = HiltViewModelFactory(activity, homeBackStackEntry)
            )
            val settingsVm: SettingsViewModel  = viewModel(
                viewModelStoreOwner = homeBackStackEntry,
                factory = HiltViewModelFactory(activity, homeBackStackEntry)
            )
            EditGoalWeightScreen(
                vm = vm,
                onCancel = { nav.popBackStack() },
                onSaved = {
                    nav.previousBackStackEntry
                        ?.savedStateHandle
                        ?.set(NavResults.SUCCESS_TOAST, "Saved successfully!")
                    settingsVm.refreshProfileOnly()
                    nav.popBackStack()
                }
            )
        }

        composable(Routes.EDIT_HEIGHT) { backStackEntry ->
            val activity = (LocalContext.current.findActivity() ?: hostActivity)
            val homeBackStackEntry = remember(backStackEntry) { nav.getBackStackEntry(Routes.HOME) }
            val vm: EditHeightViewModel = viewModel(
                viewModelStoreOwner = homeBackStackEntry,
                factory = HiltViewModelFactory(activity, homeBackStackEntry)
            )
            val settingsVm: SettingsViewModel  = viewModel(
                viewModelStoreOwner = homeBackStackEntry,
                factory = HiltViewModelFactory(activity, homeBackStackEntry)
            )
            EditHeightScreen(
                vm = vm,
                onBack = { nav.popBackStack() },
                onSaved = {
                    nav.previousBackStackEntry
                        ?.savedStateHandle
                        ?.set(NavResults.SUCCESS_TOAST, "Saved successfully!")
                    settingsVm.refreshProfileOnly()
                    nav.popBackStack()
                }
            )
        }

        composable(Routes.EDIT_STARTING_WEIGHT) { backStackEntry ->
            val activity = (LocalContext.current.findActivity() ?: hostActivity)
            val homeBackStackEntry = remember(backStackEntry) { nav.getBackStackEntry(Routes.HOME) }

            val vm: EditStartingWeightViewModel = viewModel(
                viewModelStoreOwner = homeBackStackEntry,
                factory = HiltViewModelFactory(activity, homeBackStackEntry)
            )

            val settingsVm: SettingsViewModel = viewModel(
                viewModelStoreOwner = homeBackStackEntry,
                factory = HiltViewModelFactory(activity, homeBackStackEntry)
            )

            EditStartingWeightScreen(
                vm = vm,
                onCancel = { nav.popBackStack() },
                onSaved = {
                    nav.previousBackStackEntry
                        ?.savedStateHandle
                        ?.set(NavResults.SUCCESS_TOAST, "Saved successfully!")
                    settingsVm.refreshProfileOnly()
                    nav.popBackStack()
                }
            )
        }

        composable(Routes.EDIT_AGE) { backStackEntry ->
            val activity = (LocalContext.current.findActivity() ?: hostActivity)
            val homeBackStackEntry = remember(backStackEntry) { nav.getBackStackEntry(Routes.HOME) }
            val vm: EditAgeViewModel = viewModel(
                viewModelStoreOwner = homeBackStackEntry,
                factory = HiltViewModelFactory(activity, homeBackStackEntry)
            )
            val settingsVm: SettingsViewModel = viewModel(
                viewModelStoreOwner = homeBackStackEntry,
                factory = HiltViewModelFactory(activity, homeBackStackEntry)
            )
            EditAgeScreen(
                vm = vm,
                onBack = { nav.popBackStack() },
                onSaved = {
                    nav.previousBackStackEntry
                        ?.savedStateHandle
                        ?.set(NavResults.SUCCESS_TOAST, "Saved successfully!")
                    settingsVm.refreshProfileOnly()
                    nav.popBackStack()
                }
            )
        }

        composable(Routes.EDIT_GENDER) { backStackEntry ->
            val activity = (LocalContext.current.findActivity() ?: hostActivity)
            val homeBackStackEntry = remember(backStackEntry) { nav.getBackStackEntry(Routes.HOME) }
            val vm: EditGenderViewModel = viewModel(
                viewModelStoreOwner = homeBackStackEntry,
                factory = HiltViewModelFactory(activity, homeBackStackEntry)
            )
            val settingsVm: SettingsViewModel = viewModel(
                viewModelStoreOwner = homeBackStackEntry,
                factory = HiltViewModelFactory(activity, homeBackStackEntry)
            )
            EditGenderScreen(
                vm = vm,
                onBack = { nav.popBackStack() },
                onSaved = {
                    nav.previousBackStackEntry
                        ?.savedStateHandle
                        ?.set(NavResults.SUCCESS_TOAST, "Saved successfully!")
                    settingsVm.refreshProfileOnly()
                    nav.popBackStack()
                }
            )
        }

        composable(Routes.EDIT_DAILY_STEP_GOAL) { backStackEntry ->
            val activity = (LocalContext.current.findActivity() ?: hostActivity)
            val homeBackStackEntry = remember(backStackEntry) { nav.getBackStackEntry(Routes.HOME) }
            val vm: EditDailyStepGoalViewModel = viewModel(
                viewModelStoreOwner = homeBackStackEntry,
                factory = HiltViewModelFactory(activity, homeBackStackEntry)
            )
            val settingsVm: SettingsViewModel = viewModel(
                viewModelStoreOwner = homeBackStackEntry,
                factory = HiltViewModelFactory(activity, homeBackStackEntry)
            )
            EditDailyStepGoalScreen(
                vm = vm,
                onBack = { nav.popBackStack() },
                onSaved = {
                    nav.previousBackStackEntry
                        ?.savedStateHandle
                        ?.set(NavResults.SUCCESS_TOAST, "Saved successfully!")
                    settingsVm.refreshProfileOnly()
                    nav.popBackStack()
                }
            )
        }

        composable(Routes.EDIT_WATER_GOAL) { backStackEntry ->
            val activity = (LocalContext.current.findActivity() ?: hostActivity)
            val homeBackStackEntry = remember(backStackEntry) { nav.getBackStackEntry(Routes.HOME) }

            val vm: EditWaterGoalViewModel = viewModel(
                viewModelStoreOwner = homeBackStackEntry,
                factory = HiltViewModelFactory(activity, homeBackStackEntry)
            )

            val settingsVm: SettingsViewModel = viewModel(
                viewModelStoreOwner = homeBackStackEntry,
                factory = HiltViewModelFactory(activity, homeBackStackEntry)
            )

            EditWaterGoalScreen(
                vm = vm,
                onBack = { nav.popBackStack() },
                onSaved = {
                    nav.previousBackStackEntry
                        ?.savedStateHandle
                        ?.set(NavResults.SUCCESS_TOAST, "Saved successfully!")
                    settingsVm.refreshProfileOnly()
                    nav.popBackStack()
                }
            )
        }

        composable(Routes.EDIT_WORKOUT_GOAL) { backStackEntry ->
            val activity = (LocalContext.current.findActivity() ?: hostActivity)
            val homeBackStackEntry = remember(backStackEntry) { nav.getBackStackEntry(Routes.HOME) }

            val vm: EditWorkoutGoalViewModel = viewModel(
                viewModelStoreOwner = homeBackStackEntry,
                factory = HiltViewModelFactory(activity, homeBackStackEntry)
            )

            EditWorkoutGoalScreen(
                vm = vm,
                onBack = { nav.popBackStack() },
                onSaved = {
                    nav.previousBackStackEntry
                        ?.savedStateHandle
                        ?.set(NavResults.SUCCESS_TOAST, "Saved successfully!")
                    nav.popBackStack()
                }
            )
        }

        composable(Routes.EDIT_NUTRITION_GOALS) { backStackEntry ->
            val activity = (LocalContext.current.findActivity() ?: hostActivity)
            val homeBackStackEntry = remember(backStackEntry) { nav.getBackStackEntry(Routes.HOME) }

            val vm: NutritionGoalsViewModel = viewModel(
                viewModelStoreOwner = homeBackStackEntry,
                factory = HiltViewModelFactory(activity, homeBackStackEntry)
            )

            val settingsVm: SettingsViewModel = viewModel(
                viewModelStoreOwner = homeBackStackEntry,
                factory = HiltViewModelFactory(activity, homeBackStackEntry)
            )

            // ✅ NEW：同 HOME scope 拿 WeightViewModel（這樣 PersonalDetails 也會吃到同一份狀態）
            val weightVm: WeightViewModel = viewModel(
                viewModelStoreOwner = homeBackStackEntry,
                factory = HiltViewModelFactory(activity, homeBackStackEntry)
            )

            // ✅ 一次性消費：進入此 composable 時檢查一次，有就執行並立刻清掉
            LaunchedEffect(backStackEntry) {
                val handle = backStackEntry.savedStateHandle
                val shouldReload = handle.get<Boolean>(NavResults.AUTO_GEN_RELOAD) == true
                if (shouldReload) {
                    // 立刻清掉，避免重組/回來時又被觸發
                    handle.remove<Boolean>(NavResults.AUTO_GEN_RELOAD)
                    vm.reload()
                    settingsVm.refreshProfileOnly()
                    weightVm.initIfNeeded()
                }
            }

            // ✅ toast（跟你 HOME/PERSONAL 同套）
            val successFlow = remember(backStackEntry) {
                backStackEntry.savedStateHandle.getStateFlow<String?>(NavResults.SUCCESS_TOAST, null)
            }
            val errorFlow = remember(backStackEntry) {
                backStackEntry.savedStateHandle.getStateFlow<String?>(NavResults.ERROR_TOAST, null)
            }
            val navSuccess by successFlow.collectAsState(initial = null)
            val navError by errorFlow.collectAsState(initial = null)

            Box(Modifier.fillMaxSize()) {

                EditNutritionGoalsRoute(
                    onBack = { nav.popBackStack() },
                    onAutoGenerate = { nav.navigate(Routes.AUTO_GENERATE_FLOW) }, // ✅ 改這裡
                    onSaved = {
                        nav.previousBackStackEntry
                            ?.savedStateHandle
                            ?.set(NavResults.SUCCESS_TOAST, "Saved successfully!")
                        settingsVm.refreshProfileOnly()
                        nav.popBackStack()
                    },
                    vm = vm
                )

                when {
                    !navError.isNullOrBlank() -> {
                        ErrorTopToast(message = navError!!, modifier = Modifier.align(Alignment.TopCenter))
                        LaunchedEffect(navError) {
                            delay(2_000)
                            backStackEntry.savedStateHandle[NavResults.ERROR_TOAST] = null
                        }
                    }
                    !navSuccess.isNullOrBlank() -> {
                        SuccessTopToast(
                            message = navSuccess!!,
                            modifier = Modifier.align(Alignment.TopCenter),
                            minWidth = 150.dp,
                            minHeight = 30.dp
                        )
                        LaunchedEffect(navSuccess) {
                            delay(2_000)
                            backStackEntry.savedStateHandle[NavResults.SUCCESS_TOAST] = null
                        }
                    }
                }
            }
        }


        navigation(
            route = Routes.AUTO_GENERATE_FLOW,
            startDestination = Routes.AUTO_GENERATE_EXERCISE_FREQUENCY
        ) {

            composable(Routes.AUTO_GENERATE_EXERCISE_FREQUENCY) { backStackEntry ->
                val activity = (LocalContext.current.findActivity() ?: hostActivity)
                val vm: ExerciseFrequencyViewModel = viewModel(
                    viewModelStoreOwner = backStackEntry,
                    factory = HiltViewModelFactory(activity, backStackEntry)
                )

                ExerciseFrequencyScreen(
                    vm = vm,
                    onBack = { nav.popBackStack() }, // 回 EditNutritionGoals
                    onNext = { nav.navigate(Routes.AUTO_GENERATE_HEIGHT) },
                    stepIndex = 1,
                    totalSteps = 4,
                )
            }

            composable(Routes.AUTO_GENERATE_HEIGHT) { backStackEntry ->
                val activity = (LocalContext.current.findActivity() ?: hostActivity)
                val vm: HeightSelectionViewModel = viewModel(
                    viewModelStoreOwner = backStackEntry,
                    factory = HiltViewModelFactory(activity, backStackEntry)
                )

                HeightSelectionScreen(
                    vm = vm,
                    onBack = { nav.popBackStack() },
                    onNext = { nav.navigate(Routes.AUTO_GENERATE_WEIGHT) },
                    stepIndex = 2,
                    totalSteps = 4,
                )
            }

            composable(Routes.AUTO_GENERATE_WEIGHT) { backStackEntry ->
                val activity = (LocalContext.current.findActivity() ?: hostActivity)
                val vm: WeightSelectionViewModel = viewModel(
                    viewModelStoreOwner = backStackEntry,
                    factory = HiltViewModelFactory(activity, backStackEntry)
                )

                WeightSelectionScreen(
                    vm = vm,
                    onBack = { nav.popBackStack() },
                    onNext = { nav.navigate(Routes.AUTO_GENERATE_GOALS) },
                    stepIndex = 3,
                    totalSteps = 4,
                )

            }

            composable(Routes.AUTO_GENERATE_GOALS) { backStackEntry ->
                val activity = (LocalContext.current.findActivity() ?: hostActivity)
                val vm: GoalSelectionViewModel = viewModel(
                    viewModelStoreOwner = backStackEntry,
                    factory = HiltViewModelFactory(activity, backStackEntry)
                )

                GoalSelectionScreen(
                    vm = vm,
                    onBack = { nav.popBackStack() },
                    onNext = { nav.navigate(Routes.AUTO_GENERATE_GOALS_CALC) },
                    primaryButtonText = "Generate", // 建議換成 stringResource
                    stepIndex = 4,
                    totalSteps = 4,
                )
            }

            composable(Routes.AUTO_GENERATE_GOALS_CALC) { backStackEntry ->
                val activity = (LocalContext.current.findActivity() ?: hostActivity)
                val vm: AutoGenerateGoalsCalcViewModel = viewModel(
                    viewModelStoreOwner = backStackEntry,
                    factory = HiltViewModelFactory(activity, backStackEntry)
                )

                AutoGenerateGoalsCalcScreen(
                    vm = vm,
                    onDone = { successMsg ->
                        val target = runCatching { nav.getBackStackEntry(Routes.EDIT_NUTRITION_GOALS) }.getOrNull()
                        target?.savedStateHandle?.set(NavResults.AUTO_GEN_RELOAD, true)
                        target?.savedStateHandle?.set(NavResults.SUCCESS_TOAST, successMsg)

                        val popped = nav.popBackStack(Routes.EDIT_NUTRITION_GOALS, inclusive = false)
                        if (!popped) {
                            // fallback：不在 back stack（極少數）就直接 navigate 回去
                            nav.navigate(Routes.EDIT_NUTRITION_GOALS) {
                                launchSingleTop = true
                                restoreState = false
                            }
                        }
                    },
                    onFailToast = { errMsg ->
                        val target = runCatching { nav.getBackStackEntry(Routes.EDIT_NUTRITION_GOALS) }.getOrNull()
                        target?.savedStateHandle?.set(NavResults.ERROR_TOAST, errMsg)

                        val popped = nav.popBackStack(Routes.EDIT_NUTRITION_GOALS, inclusive = false)
                        if (!popped) {
                            nav.navigate(Routes.EDIT_NUTRITION_GOALS) {
                                launchSingleTop = true
                                restoreState = false
                            }
                        }
                    }
                )
            }
        }

        composable(Routes.CAMERA) { backStackEntry ->
            val ctx = LocalContext.current
            val activity: ComponentActivity = (ctx.findActivity() as? ComponentActivity) ?: hostActivity
            val owner: ActivityResultRegistryOwner = activity

            val flowVm: FoodLogFlowViewModel = viewModel(
                viewModelStoreOwner = backStackEntry,
                factory = HiltViewModelFactory(activity, backStackEntry)
            )

            CompositionLocalProvider(LocalActivityResultRegistryOwner provides owner) {
                CameraScreen(
                    onClose = { nav.popBackStack() },
                    onImagePicked = { uri ->
                        flowVm.submitAlbum(ctx, uri) { foodLogId ->
                            nav.navigate(Routes.foodLogDetail(foodLogId)) {
                                launchSingleTop = true
                            }
                        }
                    }
                )
            }
        }

        composable(Routes.FOOD_LOG_DETAIL) { backStackEntry ->
            val id = backStackEntry.arguments?.getString("id") ?: return@composable
            val ctx = LocalContext.current
            val activity: ComponentActivity = (ctx.findActivity() as? ComponentActivity) ?: hostActivity

            val flowVm: FoodLogFlowViewModel = viewModel(
                viewModelStoreOwner = backStackEntry,
                factory = HiltViewModelFactory(activity, backStackEntry)
            )

            FoodLogDetailScreen(
                foodLogId = id,
                vm = flowVm,
                onBack = { nav.popBackStack() },
                onOpenEditor = { foodLogId ->
                    // TODO: 導到你的可編輯頁（EditFoodLog）
                    // nav.navigate(Routes.EDIT_FOOD_LOG(foodLogId))
                }
            )
        }


        composable(Routes.REMINDERS) { SimplePlaceholder("Reminders") }

    }
}

@Composable
private fun SimplePlaceholder(title: String) {
    Text(modifier = Modifier.padding(24.dp), text = "TODO: $title page")
}
