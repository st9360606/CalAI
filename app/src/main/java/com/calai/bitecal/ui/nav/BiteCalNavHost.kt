package com.calai.bitecal.ui.nav

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.result.ActivityResultRegistryOwner
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.navigation.HiltViewModelFactory
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.calai.bitecal.R
import com.calai.bitecal.data.auth.net.SessionBus
import com.calai.bitecal.data.foodlog.model.ClientAction
import com.calai.bitecal.data.foodlog.model.FoodLogEnvelopeDto
import com.calai.bitecal.data.foodlog.model.FoodLogStatus
import com.calai.bitecal.data.onboarding.repo.OnboardingRepository
import com.calai.bitecal.di.AppEntryPoint
import com.calai.bitecal.i18n.LanguageManager
import com.calai.bitecal.i18n.LanguageSessionFlag
import com.calai.bitecal.i18n.LanguageStore
import com.calai.bitecal.i18n.LocalLocaleController
import com.calai.bitecal.i18n.currentLocaleKey
import com.calai.bitecal.ui.appentry.AppEntryRoute
import com.calai.bitecal.ui.auth.RequireSignInScreen
import com.calai.bitecal.ui.auth.SignInSheetHost
import com.calai.bitecal.ui.auth.email.EmailCodeScreen
import com.calai.bitecal.ui.auth.email.EmailEnterScreen
import com.calai.bitecal.ui.auth.email.EmailSignInViewModel
import com.calai.bitecal.ui.home.HomeScreen
import com.calai.bitecal.ui.home.HomeTab
import com.calai.bitecal.ui.home.components.toast.ErrorTopToast
import com.calai.bitecal.ui.home.components.toast.SuccessTopToast
import com.calai.bitecal.ui.home.model.HomeViewModel
import com.calai.bitecal.ui.home.ui.camera.CameraMode
import com.calai.bitecal.ui.home.ui.camera.CameraScreen
import com.calai.bitecal.ui.home.ui.camera.common.ApiErrorCard
import com.calai.bitecal.ui.home.ui.camera.common.ApiErrorUiMapper
import com.calai.bitecal.ui.home.ui.fasting.FastingPlansScreen
import com.calai.bitecal.ui.home.ui.fasting.model.FastingPlanViewModel
import com.calai.bitecal.ui.home.ui.foodlog.FoodLogTimeResolver
import com.calai.bitecal.ui.home.ui.foodlog.RecentUploadDetailScreen
import com.calai.bitecal.ui.home.ui.foodlog.model.FoodLogFlowViewModel
import com.calai.bitecal.ui.home.ui.membership.MembershipUiMapper
import com.calai.bitecal.ui.home.ui.membership.MembershipViewModel
import com.calai.bitecal.ui.home.ui.notifications.NotificationInboxScreen
import com.calai.bitecal.ui.home.ui.notifications.NotificationInboxViewModel
import com.calai.bitecal.ui.home.ui.savedfood.SavedFoodsScreen
import com.calai.bitecal.ui.home.ui.savedfood.model.SavedFoodsViewModel
import com.calai.bitecal.ui.home.ui.settings.RingColorsExplainedScreen
import com.calai.bitecal.ui.home.ui.settings.SettingsScreen
import com.calai.bitecal.ui.home.ui.settings.dialog.RestoreSubscriptionDialog
import com.calai.bitecal.ui.home.ui.settings.details.AutoGenerateGoalsCalcScreen
import com.calai.bitecal.ui.home.ui.settings.details.EditAgeScreen
import com.calai.bitecal.ui.home.ui.settings.details.EditDailyStepGoalScreen
import com.calai.bitecal.ui.home.ui.settings.details.EditGenderScreen
import com.calai.bitecal.ui.home.ui.settings.details.EditHeightScreen
import com.calai.bitecal.ui.home.ui.settings.details.EditNutritionGoalsRoute
import com.calai.bitecal.ui.home.ui.settings.details.EditStartingWeightScreen
import com.calai.bitecal.ui.home.ui.settings.details.EditWaterGoalScreen
import com.calai.bitecal.ui.home.ui.settings.details.EditWorkoutGoalScreen
import com.calai.bitecal.ui.home.ui.settings.details.PersonalDetailsScreen
import com.calai.bitecal.ui.home.ui.settings.details.model.AutoGenerateGoalsCalcViewModel
import com.calai.bitecal.ui.home.ui.settings.details.model.EditAgeViewModel
import com.calai.bitecal.ui.home.ui.settings.details.model.EditDailyStepGoalViewModel
import com.calai.bitecal.ui.home.ui.settings.details.model.EditGenderViewModel
import com.calai.bitecal.ui.home.ui.settings.details.model.EditHeightViewModel
import com.calai.bitecal.ui.home.ui.settings.details.model.EditStartingWeightViewModel
import com.calai.bitecal.ui.home.ui.settings.details.model.EditWaterGoalViewModel
import com.calai.bitecal.ui.home.ui.settings.details.model.EditWorkoutGoalViewModel
import com.calai.bitecal.ui.home.ui.settings.details.model.NutritionGoalsViewModel
import com.calai.bitecal.ui.home.ui.settings.editname.EditNameScreen
import com.calai.bitecal.ui.home.ui.settings.editname.model.EditNameViewModel
import com.calai.bitecal.ui.home.ui.settings.model.RestoreSubscriptionDialogState
import com.calai.bitecal.ui.home.ui.settings.model.SettingsViewModel
import com.calai.bitecal.ui.home.ui.settings.model.RestoreSubscriptionViewModel
import com.calai.bitecal.ui.home.ui.settings.premium.PremiumRewardsScreen
import com.calai.bitecal.ui.home.ui.settings.premium.model.PremiumRewardsViewModel
import com.calai.bitecal.ui.home.ui.settings.referral.ReferralScreen
import com.calai.bitecal.ui.home.ui.settings.referral.model.ReferralViewModel
import com.calai.bitecal.ui.home.ui.settings.widgetguide.WidgetGuideScreen
import com.calai.bitecal.ui.home.ui.water.model.WaterViewModel
import com.calai.bitecal.ui.home.ui.weight.EditGoalWeightScreen
import com.calai.bitecal.ui.home.ui.weight.RecordWeightScreen
import com.calai.bitecal.ui.home.ui.weight.WeightScreen
import com.calai.bitecal.ui.home.ui.weight.model.WeightViewModel
import com.calai.bitecal.ui.home.ui.workout.WorkoutHistoryScreen
import com.calai.bitecal.ui.home.ui.workout.model.WorkoutViewModel
import com.calai.bitecal.ui.home.workoutgate.WorkoutSheetOpenRequest
import com.calai.bitecal.ui.landing.LandingScreen
import com.calai.bitecal.ui.onboarding.age.AgeSelectionScreen
import com.calai.bitecal.ui.onboarding.age.AgeSelectionViewModel
import com.calai.bitecal.ui.onboarding.exercise.ExerciseFrequencyScreen
import com.calai.bitecal.ui.onboarding.exercise.ExerciseFrequencyViewModel
import com.calai.bitecal.ui.onboarding.gender.GenderKey
import com.calai.bitecal.ui.onboarding.gender.GenderSelectionScreen
import com.calai.bitecal.ui.onboarding.gender.GenderSelectionViewModel
import com.calai.bitecal.ui.onboarding.goal.GoalSelectionScreen
import com.calai.bitecal.ui.onboarding.goal.GoalSelectionViewModel
import com.calai.bitecal.ui.onboarding.goalweight.WeightGoalScreen
import com.calai.bitecal.ui.onboarding.goalweight.WeightGoalViewModel
import com.calai.bitecal.ui.onboarding.healthconnect.HealthConnectIntroScreen
import com.calai.bitecal.ui.onboarding.comparison.WeightLossComparisonScreen
import com.calai.bitecal.ui.onboarding.height.HeightSelectionScreen
import com.calai.bitecal.ui.onboarding.height.HeightSelectionViewModel
import com.calai.bitecal.ui.onboarding.notifications.NotificationPermissionScreen
import com.calai.bitecal.ui.onboarding.plan.HealthPlanScreen
import com.calai.bitecal.ui.onboarding.plan.HealthPlanViewModel
import com.calai.bitecal.ui.onboarding.progress.ComputationProgressScreen
import com.calai.bitecal.ui.onboarding.progress.ComputationProgressViewModel
import com.calai.bitecal.ui.onboarding.referralsource.ReferralSourceScreen
import com.calai.bitecal.ui.onboarding.referralcode.OnboardReferralCodeRoute
import com.calai.bitecal.ui.onboarding.referralcode.OnboardReferralCodeViewModel
import com.calai.bitecal.ui.onboarding.referralsource.ReferralSourceViewModel
import com.calai.bitecal.ui.onboarding.weight.WeightSelectionScreen
import com.calai.bitecal.ui.onboarding.weight.WeightSelectionViewModel
import com.calai.bitecal.ui.subscription.OnboardSubscriptionScreen
import com.calai.bitecal.ui.subscription.SubscriptionViewModel
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object Routes {
    const val LANDING = "landing"
    const val SIGN_IN_EMAIL_ENTER = "signin_email_enter"
    const val SIGN_IN_EMAIL_CODE = "signin_email_code"
    const val ONBOARD_GENDER = "onboard_gender"
    const val ONBOARD_REFERRAL = "onboard_referral"
    const val ONBOARD_REFERRAL_CODE = "onboard_referral_code"
    const val ONBOARD_AGE = "onboard_age"
    const val ONBOARD_HEIGHT = "onboard_height"
    const val ONBOARD_WEIGHT = "onboard_weight"
    const val ONBOARD_GOAL_WEIGHT = "onboard_goal_weight"
    const val ONBOARD_WEIGHT_LOSS_COMPARISON = "onboard_weight_loss_comparison"
    const val ONBOARD_EXERCISE_FREQ = "onboard_exercise_freq"
    const val ONBOARD_GOAL = "onboard_goal"
    const val ONBOARD_NOTIF = "onboard_notif"
    const val ONBOARD_HEALTH_CONNECT = "onboard_health_connect"
    const val PLAN_PROGRESS = "plan_progress"
    const val ROUTE_PLAN = "plan"

    /**
     * Onboarding 完成後專用訂閱頁。
     *
     * 跟一般 Routes.SUBSCRIPTION 分開，是為了：
     * 1. Onboarding 訂閱頁不能 back 繞過
     * 2. Settings / Home 開啟的一般訂閱頁仍可返回
     */
    const val ONBOARD_SUBSCRIPTION = "onboard_subscription"

    /**
     * Home ScanFab 專用付費牆。
     *
     * 不要直接共用 ONBOARD_SUBSCRIPTION route，因為 Home 進來時：
     * - close/back 應該回 HOME
     * - 不應該回 SignIn
     * - 不應該標記 onboarding paywall rejected once
     */
    const val HOME_SCAN_SUBSCRIPTION = "home_scan_subscription"

    /**
     * Home WorkoutAddButton 專用付費牆。
     *
     * UI 可以共用既有 subscription screen，但 route 必須保留 Workout 語意，
     * 讓未來 analytics 與維護能分辨 Scan gate / Workout gate 來源。
     */
    const val HOME_WORKOUT_SUBSCRIPTION = "home_workout_subscription"

    /**
     * Settings ScanFab 專用付費牆。
     *
     * 跟 HOME_SCAN_SUBSCRIPTION 使用同一套 OnboardSubscriptionScreen UI，
     * 但 close/back 行為不同：
     * - close/back 應該回 SETTINGS
     * - 付款成功後回 HOME 並刷新會員狀態
     */
    const val SETTINGS_SCAN_SUBSCRIPTION = "settings_scan_subscription"
    const val MEMBERSHIP_REFRESH_TICK = "membership_refresh_tick"
    const val OPEN_WORKOUT_SHEET_TICK = "open_workout_sheet_tick"
    const val REQUIRE_SIGN_IN = "require_sign_in"

    const val REQUIRE_SIGN_IN_ROUTE =
        "$REQUIRE_SIGN_IN?redirect={redirect}&auto={auto}&uploadLocal={uploadLocal}"

    fun requireSignInRoute(
        redirect: String = HOME,
        auto: Boolean = false,
        uploadLocal: Boolean = false
    ): String {
        return "$REQUIRE_SIGN_IN?redirect=$redirect&auto=$auto&uploadLocal=$uploadLocal"
    }

    const val HOME = "home"
    const val APP_ENTRY = "app_entry"
    const val PROGRESS = "progress"
    const val FASTING = "fasting"
    const val SETTINGS = "settings"
    const val CAMERA = "camera"
    const val SAVED_FOODS = "saved_foods"
    const val REMINDERS = "reminders"
    const val REFERRALS = "referrals"
    const val PREMIUM_REWARDS = "premium_rewards"
    const val NOTIFICATION_INBOX = "notification_inbox"
    const val WIDGET_GUIDE = "widget_guide"
    const val RING_COLORS_EXPLAINED = "ring_colors_explained"
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
    fun foodLogDetail(id: String) = "foodLog/$id"

    const val RECENT_UPLOAD_DETAIL = "recentUploadDetail/{id}"
    fun recentUploadDetail(id: String) = "recentUploadDetail/$id"

    const val RECENT_UPLOAD_PREVIEW_URI = "recent_upload_preview_uri"
    const val RECENT_UPLOAD_TIME_TEXT = "recent_upload_time_text"
    const val RECENT_UPLOAD_SOURCE = "recent_upload_source"
}

object NavResults {
    const val SUCCESS_TOAST = "success_toast"
    const val ERROR_TOAST = "error_toast"
    const val AUTO_GEN_RELOAD = "auto_gen_reload"
}

@Composable
private fun ClearNavResultToastsOnDispose(backStackEntry: NavBackStackEntry) {
    DisposableEffect(backStackEntry) {
        onDispose {
            clearNavResultToasts(backStackEntry)
        }
    }
}

private fun clearNavResultToasts(backStackEntry: NavBackStackEntry) {
    backStackEntry.savedStateHandle[NavResults.SUCCESS_TOAST] = null
    backStackEntry.savedStateHandle[NavResults.ERROR_TOAST] = null
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

private fun resolveFoodLogTimeText(
    env: FoodLogEnvelopeDto,
    fallbackTimeText: String = ""
): String {
    return FoodLogTimeResolver.resolveDisplayTimeText(
        zoneId = ZoneId.systemDefault(),
        updatedAtUtc = env.updatedAtUtc,
        serverReceivedAtUtc = env.serverReceivedAtUtc,
        capturedAtUtc = env.capturedAtUtc,
        capturedLocalDate = env.capturedLocalDate
    ).ifBlank { fallbackTimeText }
}

private tailrec fun Context.findActivity(): Activity? =
    when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }

private fun NavController.safePopBackStack(): Boolean =
    previousBackStackEntry != null && popBackStack()

private fun NavController.backFromRequireSignIn(
    uploadLocal: Boolean
) {
    if (uploadLocal) {
        val poppedToPlan = popBackStack(
            route = Routes.ROUTE_PLAN,
            inclusive = false
        )

        if (poppedToPlan) return

        navigate(Routes.ROUTE_PLAN) {
            popUpTo(0) {
                inclusive = true
            }
            launchSingleTop = true
            restoreState = false
        }
        return
    }

    val popped = safePopBackStack()
    if (!popped) {
        navigate(Routes.LANDING) {
            popUpTo(0) {
                inclusive = true
            }
            launchSingleTop = true
            restoreState = false
        }
    }
}

private fun openGooglePlaySubscriptionManagement(
    context: Context,
    productId: String? = null
) {
    val packageName = context.packageName

    val uri = if (productId.isNullOrBlank()) {
        Uri.parse("https://play.google.com/store/account/subscriptions")
    } else {
        Uri.parse(
            "https://play.google.com/store/account/subscriptions" +
                    "?sku=$productId&package=$packageName"
        )
    }

    val playStoreIntent = Intent(Intent.ACTION_VIEW, uri).apply {
        setPackage("com.android.vending")
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    val browserFallbackIntent = Intent(Intent.ACTION_VIEW, uri).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    runCatching {
        context.startActivity(playStoreIntent)
    }.recoverCatching {
        context.startActivity(browserFallbackIntent)
    }.onFailure {
        Toast.makeText(
            context,
            "Unable to open Google Play subscriptions.",
            Toast.LENGTH_SHORT
        ).show()
    }
}

private fun NavController.resolveCameraFallbackPaywallRoute(): String {
    val previousRoute = previousBackStackEntry?.destination?.route

    return when (previousRoute) {
        Routes.SETTINGS,
        Routes.SETTINGS_SCAN_SUBSCRIPTION -> Routes.SETTINGS_SCAN_SUBSCRIPTION

        else -> Routes.HOME_SCAN_SUBSCRIPTION
    }
}

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
    val languageStore = remember(appCtx) { LanguageStore(appCtx) }

    val entitlementSyncer = remember(ep) { ep.entitlementSyncer() }

    val onboardingRepo: OnboardingRepository = remember(ep) {
        ep.onboardingRepository()
    }

    // 只記錄本次 App / 本輪 onboarding 是否已經關閉過 onboarding paywall。
    // 不寫入 DataStore：重新開 App 或重新開始 onboarding 時會重算。
    var onboardingPaywallRejectedOnce by remember { mutableStateOf(false) }

    val localeController = LocalLocaleController.current

    LaunchedEffect(nav) {
        SessionBus.expired.collect {
            val currentRoute = nav.currentBackStackEntry?.destination?.route

            // onboarding / app entry / auth flow 中都不要再被全域 gate 打斷
            if (isOnboardingRoute(currentRoute) || isAuthOrEntryRoute(currentRoute)) {
                return@collect
            }

            nav.navigate(
                Routes.requireSignInRoute(
                    redirect = Routes.HOME,
                    auto = false,
                    uploadLocal = false
                )
            ) {
                popUpTo(0) { inclusive = true }
                launchSingleTop = true
                restoreState = false
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
                onStart = {
                    onboardingPaywallRejectedOnce = false
                    nav.navigate(Routes.ONBOARD_GENDER) { launchSingleTop = true }
                },
                onLogin = {
                    // 使用者主動點「登入」：自動開啟 Sheet
                    nav.navigate(
                        Routes.requireSignInRoute(
                            redirect = Routes.HOME,
                            auto = true,
                            uploadLocal = false
                        )
                    )
                },
                onSetLocale = { tag ->
                    val normalizedTag = LanguageManager.normalizeTag(tag)

                    localeControllerLocal.set(normalizedTag)
                    LanguageManager.applyLanguage(normalizedTag)
                    onSetLocale(normalizedTag)

                    scope.launch {
                        withContext(Dispatchers.IO) {
                            runCatching { languageStore.save(normalizedTag) }
                            runCatching { store.setLocaleTag(normalizedTag) }
                        }
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
                    val encodedEmail = Uri.encode(email)
                    val encodedRedirect = Uri.encode(redirect)

                    nav.navigate(
                        "${Routes.SIGN_IN_EMAIL_CODE}?email=$encodedEmail&redirect=$encodedRedirect&uploadLocal=$uploadLocal"
                    )
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
            EmailCodeScreen(
                vm = vm,
                email = email,
                onBack = { nav.safePopBackStack() },
                onSuccess = {
                    scope.launch {
                        // 刪帳後重新登入時，先同步等待 Google Play restore + /membership/me，
                        // 再判斷是否已有 server profile，避免尚未過期訂閱晚一步恢復。
                        val allowHomeAfterRejectedPaywall = onboardingPaywallRejectedOnce
                        val dest = withContext(Dispatchers.IO) {
                            if (!uploadLocal) {
                                runCatching { entitlementSyncer.refreshServerEntitlementSummaryOnly() }
                            }

                            val exists = runCatching { profileRepo.existsOnServer() }.getOrDefault(false)
                            if (uploadLocal) {
                                runCatching { store.setLocaleTag(currentTag) }
                                runCatching { profileRepo.upsertFromLocalForOnboarding() }
                                runCatching { store.setHasServerProfile(true) }
                                runCatching { weightRepo.ensureBaseline() }

                                // SignIn 後依照後端 bootstrap 決定下一頁；App 不自行猜 referral eligibility。
                                resolveOnboardingDestination(
                                    entitlementSyncer = entitlementSyncer,
                                    onboardingRepository = onboardingRepo,
                                    allowHomeAfterRejectedPaywall = allowHomeAfterRejectedPaywall,
                                )
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
                            popUpTo(Routes.REQUIRE_SIGN_IN_ROUTE) {
                                inclusive = true
                            }
                            launchSingleTop = true
                            restoreState = false
                        }
                    }
                }
            )
        }

        // ===== Onboarding：性別 → ... → Plan =====
        composable(Routes.ONBOARD_GENDER) { backStackEntry ->
            LaunchedEffect(Unit) { onboardingPaywallRejectedOnce = false }
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
                onNext = { nav.navigate(Routes.ONBOARD_WEIGHT_LOSS_COMPARISON) { launchSingleTop = true } }
            )
        }

        composable(Routes.ONBOARD_WEIGHT_LOSS_COMPARISON) {
            WeightLossComparisonScreen(
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

        // ROUTE_PLAN：未登入 → SignIn Gate；已登入 → 先 upsert 再進 Onboarding 訂閱頁
        composable(Routes.ROUTE_PLAN) { backStackEntry ->
            val activity = (LocalContext.current.findActivity() ?: hostActivity)
            val vm: HealthPlanViewModel = viewModel(
                viewModelStoreOwner = backStackEntry,
                factory = HiltViewModelFactory(activity, backStackEntry)
            )
            val routeScope = rememberCoroutineScope()
            HealthPlanScreen(
                vm = vm,
                startEnabled = isSignedIn != null,
                onStart = {
                    routeScope.launch {
                        when (isSignedIn) {
                            true -> {
                                val allowHomeAfterRejectedPaywall = onboardingPaywallRejectedOnce
                                val destination = withContext(Dispatchers.IO) {
                                    runCatching { profileRepo.upsertFromLocalForOnboarding() }
                                    runCatching { store.setHasServerProfile(true) }
                                    runCatching { weightRepo.ensureBaseline() }

                                    // 已登入使用者重走 onboarding 時，也要走後端 bootstrap，避免 App 自行推論 referral eligibility。
                                    resolveOnboardingDestination(
                                        entitlementSyncer = entitlementSyncer,
                                        onboardingRepository = onboardingRepo,
                                        allowHomeAfterRejectedPaywall = allowHomeAfterRejectedPaywall,
                                    )
                                }

                                if (destination == Routes.ONBOARD_REFERRAL_CODE) {
                                    nav.navigate(destination) {
                                        popUpTo(Routes.ROUTE_PLAN) { inclusive = false }
                                        launchSingleTop = true
                                        restoreState = false
                                    }
                                } else {
                                    nav.navigate(destination) {
                                        popUpTo(0) { inclusive = true }
                                        launchSingleTop = true
                                        restoreState = false
                                    }
                                }
                            }

                            false -> {
                                nav.navigate(
                                    Routes.requireSignInRoute(
                                        redirect = Routes.HOME,
                                        auto = false,
                                        uploadLocal = true
                                    )
                                ) {
                                    launchSingleTop = true
                                    restoreState = false
                                }
                            }

                            null -> {
                                // auth state 還沒 ready，這時按鈕本來就應該是 disabled
                                return@launch
                            }
                        }
                    }
                }
            )
        }

        // Gate：支援 auto + uploadLocal；SignIn 不再提供 Skip
        composable(
            route = Routes.REQUIRE_SIGN_IN_ROUTE,
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
                onBack = {
                    nav.backFromRequireSignIn(uploadLocal = uploadLocal)
                },
                onGoogleClick = {
                    showSheet.value = true
                },
                onEmailClick = {
                    showSheet.value = true
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
                    allowHomeAfterOnboardingPaywallRejected = onboardingPaywallRejectedOnce,

                    onGoogle = {
                        showSheet.value = false
                    },
                    onEmail = {
                        showSheet.value = false

                        val encodedRedirect = Uri.encode(redirect)

                        nav.navigate(
                            "${Routes.SIGN_IN_EMAIL_ENTER}?redirect=$encodedRedirect&uploadLocal=$uploadLocal"
                        )
                    },
                    onShowError = { msg ->
                        showSheet.value = false
                        scope.launch { snackbarHostState.showSnackbar(msg.toString()) }
                    },
                )
            }
        }

        composable(Routes.ONBOARD_REFERRAL_CODE) { backStackEntry ->
            val activity = (LocalContext.current.findActivity() ?: hostActivity)

            val vm: OnboardReferralCodeViewModel = viewModel(
                viewModelStoreOwner = backStackEntry,
                factory = HiltViewModelFactory(activity, backStackEntry)
            )

            OnboardReferralCodeRoute(
                vm = vm,
                onBack = { nav.safePopBackStack() },
                onNext = {
                    nav.navigate(Routes.ONBOARD_SUBSCRIPTION) {
                        popUpTo(Routes.ONBOARD_REFERRAL_CODE) {
                            inclusive = true
                        }
                        launchSingleTop = true
                        restoreState = false
                    }
                }
            )
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

            val membershipVm: MembershipViewModel = viewModel(
                viewModelStoreOwner = backStackEntry,
                factory = HiltViewModelFactory(activity, backStackEntry)
            )
            val membershipUi by membershipVm.ui.collectAsState()

            val restoreSubscriptionVm: RestoreSubscriptionViewModel = viewModel(
                viewModelStoreOwner = backStackEntry,
                factory = HiltViewModelFactory(activity, backStackEntry)
            )
            val restoreSubscriptionUi by restoreSubscriptionVm.ui.collectAsState()

            val restoreLocaleKey = currentLocaleKey()

            val restoreDialogTitle = when (restoreSubscriptionUi.dialogState) {
                RestoreSubscriptionDialogState.Restored ->
                    stringResource(R.string.restore_subscription_dialog_success_title)

                RestoreSubscriptionDialogState.RestoredWithPaymentIssue ->
                    stringResource(R.string.restore_subscription_dialog_payment_issue_title)

                RestoreSubscriptionDialogState.NoActivePurchase ->
                    stringResource(R.string.restore_subscription_dialog_no_active_title)

                RestoreSubscriptionDialogState.Failed ->
                    stringResource(R.string.restore_subscription_dialog_failed_title)

                RestoreSubscriptionDialogState.BoundToAnotherAccount ->
                    stringResource(R.string.restore_subscription_dialog_bound_title)

                RestoreSubscriptionDialogState.Hidden,
                RestoreSubscriptionDialogState.CandidateFound,
                RestoreSubscriptionDialogState.Restoring ->
                    stringResource(R.string.restore_subscription_dialog_title)
            }

            val restoreDialogBody = when (restoreSubscriptionUi.dialogState) {
                RestoreSubscriptionDialogState.Restored ->
                    stringResource(R.string.restore_subscription_dialog_success_body)

                RestoreSubscriptionDialogState.RestoredWithPaymentIssue ->
                    stringResource(R.string.restore_subscription_dialog_payment_issue_body)

                RestoreSubscriptionDialogState.NoActivePurchase ->
                    stringResource(R.string.restore_subscription_dialog_no_active_body)

                RestoreSubscriptionDialogState.Failed ->
                    stringResource(R.string.restore_subscription_dialog_failed_body)

                RestoreSubscriptionDialogState.BoundToAnotherAccount ->
                    stringResource(R.string.restore_subscription_dialog_bound_body)

                RestoreSubscriptionDialogState.Hidden,
                RestoreSubscriptionDialogState.CandidateFound,
                RestoreSubscriptionDialogState.Restoring ->
                    stringResource(R.string.restore_subscription_dialog_body)
            }

            val restoreDialogCloseText = stringResource(R.string.common_close)
            val restoreDialogRestoreText = stringResource(R.string.settings_restore_subscription)
            val restoreDialogRestoringText = stringResource(R.string.restore_subscription_dialog_restoring)
            val restoreDialogMaybeLaterText = stringResource(R.string.common_maybe_later)

            val membershipRefreshTickFlow = remember(backStackEntry) {
                backStackEntry.savedStateHandle.getStateFlow<Long>(
                    Routes.MEMBERSHIP_REFRESH_TICK,
                    0L
                )
            }
            val membershipRefreshTick by membershipRefreshTickFlow.collectAsState()
            val openWorkoutSheetTickFlow = remember(backStackEntry) {
                backStackEntry.savedStateHandle.getStateFlow<Long>(
                    Routes.OPEN_WORKOUT_SHEET_TICK,
                    0L
                )
            }
            val openWorkoutSheetTick by openWorkoutSheetTickFlow.collectAsState()

            LaunchedEffect(membershipRefreshTick) {
                membershipVm.refresh()
            }

            // ✅ 讓 HOME 也能顯示「上一頁回傳」的 toast（例如 QuickLogWeight 回來）
            val successFlow = remember(backStackEntry) {
                backStackEntry.savedStateHandle.getStateFlow<String?>(NavResults.SUCCESS_TOAST, null)
            }
            val errorFlow = remember(backStackEntry) {
                backStackEntry.savedStateHandle.getStateFlow<String?>(NavResults.ERROR_TOAST, null)
            }
            val navSuccess by successFlow.collectAsState(initial = null)
            val navError by errorFlow.collectAsState(initial = null)

            ClearNavResultToastsOnDispose(backStackEntry)

            LaunchedEffect(isSignedIn) {
                if (isSignedIn == true) {
                    vm.refreshAfterLogin()
                    membershipVm.refresh()
                }
            }

            LaunchedEffect(
                isSignedIn,
                membershipUi.loading,
                membershipUi.premiumStatus
            ) {
                if (isSignedIn == true) {
                    restoreSubscriptionVm.checkCandidateAfterMembershipLoaded(
                        premiumStatus = membershipUi.premiumStatus,
                        membershipLoading = membershipUi.loading
                    )
                }
            }

            val homeScope = rememberCoroutineScope()

            Box(Modifier.fillMaxSize()) {
                HomeScreen(
                    vm = vm,
                    waterVm = waterVm,
                    workoutVm = workoutVm,
                    fastingVm = fastingVm,
                    weightVm = weightVm,
                    onOpenCamera = {
                        homeScope.launch {
                            val hasActiveAccess = withContext(Dispatchers.IO) {
                                entitlementSyncer.hasActivePremiumAccess()
                            }

                            membershipVm.refresh()

                            if (hasActiveAccess) {
                                nav.navigate(Routes.CAMERA) {
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            } else {
                                nav.navigate(Routes.HOME_SCAN_SUBSCRIPTION) {
                                    launchSingleTop = true
                                    restoreState = false
                                }
                            }
                        }
                    },
                    onOpenSavedFoods = {
                        nav.navigate(Routes.SAVED_FOODS) {
                            launchSingleTop = true
                        }
                    },
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
                    onQuickLogWeight = { nav.navigate(Routes.RECORD_WEIGHT) { launchSingleTop = true; restoreState = true } },
                    onOpenRecentUploadDetail = { foodLogId, previewUri, timeText ->
                        nav.currentBackStackEntry
                            ?.savedStateHandle
                            ?.set(Routes.RECENT_UPLOAD_PREVIEW_URI, previewUri)

                        nav.currentBackStackEntry
                            ?.savedStateHandle
                            ?.set(Routes.RECENT_UPLOAD_TIME_TEXT, timeText)

                        nav.navigate(Routes.recentUploadDetail(foodLogId)) {
                            launchSingleTop = true
                        }
                    },
                    canUseScan = membershipUi.canUseScan,
                    onOpenSubscription = {
                        nav.navigate(Routes.HOME_SCAN_SUBSCRIPTION) {
                            launchSingleTop = true
                            restoreState = false
                        }
                    },
                    onCheckCanUseScan = {
                        val hasActiveAccess = withContext(Dispatchers.IO) {
                            entitlementSyncer.hasActivePremiumAccess()
                        }
                        membershipVm.refresh()
                        hasActiveAccess
                    },
                    onOpenWorkoutSubscription = {
                        nav.navigate(Routes.HOME_WORKOUT_SUBSCRIPTION) {
                            launchSingleTop = true
                            restoreState = false
                        }
                    },
                    onCheckCanUseWorkout = {
                        val hasActiveAccess = withContext(Dispatchers.IO) {
                            entitlementSyncer.hasActivePremiumAccess()
                        }
                        membershipVm.refresh()
                        hasActiveAccess
                    },
                    openWorkoutSheetRequestTick = openWorkoutSheetTick,
                    onConsumeOpenWorkoutSheetRequest = {
                        backStackEntry.savedStateHandle[Routes.OPEN_WORKOUT_SHEET_TICK] =
                            WorkoutSheetOpenRequest.ConsumedTick
                    },
                    onWorkoutSavedGoHome = {
                        if (nav.currentDestination?.route != Routes.HOME) {
                            nav.goHome()
                        }
                    },
                )

                key(restoreLocaleKey) {
                    RestoreSubscriptionDialog(
                        uiState = restoreSubscriptionUi,
                        title = restoreDialogTitle,
                        body = restoreDialogBody,
                        closeText = restoreDialogCloseText,
                        restoreText = restoreDialogRestoreText,
                        restoringText = restoreDialogRestoringText,
                        maybeLaterText = restoreDialogMaybeLaterText,
                        onDismiss = restoreSubscriptionVm::closeDialog,
                        onMaybeLater = restoreSubscriptionVm::dismissForSession,
                        onRestore = {
                            restoreSubscriptionVm.restoreSubscription(
                                onRestored = {},
                                onMembershipMayHaveChanged = { membershipVm.refresh() }
                            )
                        }
                    )
                }

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
                            modifier = Modifier.align(Alignment.TopCenter)
                        )
                        LaunchedEffect(navSuccess) {
                            delay(2_000)
                            backStackEntry.savedStateHandle[NavResults.SUCCESS_TOAST] = null
                        }
                    }
                }
            }
        }

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

            ClearNavResultToastsOnDispose(backStackEntry)

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
                        modifier = Modifier.align(Alignment.TopCenter)
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
                                ?.set(NavResults.SUCCESS_TOAST, "Saved successfully !")
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
                            ?.set(NavResults.SUCCESS_TOAST, "Saved successfully !")
                        settingsVm.refreshProfileOnly()
                        nav.popBackStack()
                    }
                )
            }
        }

        composable(Routes.SETTINGS) { backStackEntry ->
            val activity = (LocalContext.current.findActivity() ?: hostActivity)
            val owner: ActivityResultRegistryOwner =
                (activity as? ActivityResultRegistryOwner) ?: hostActivity

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

            LaunchedEffect(settingsVm) {
                settingsVm.events.collect { event ->
                    when (event) {
                        SettingsViewModel.Event.LogoutSuccess -> {
                            nav.navigate(Routes.LANDING) {
                                popUpTo(0) { inclusive = true }
                                launchSingleTop = true
                                restoreState = false
                            }
                        }
                    }
                }
            }

            // ✅ 讓 PERSONAL 也能顯示「上一頁回傳」的 toast（例如 EditNutritionGoals 回來）
            val successFlow = remember(backStackEntry) {
                backStackEntry.savedStateHandle.getStateFlow<String?>(NavResults.SUCCESS_TOAST, null)
            }
            val errorFlow = remember(backStackEntry) {
                backStackEntry.savedStateHandle.getStateFlow<String?>(NavResults.ERROR_TOAST, null)
            }
            val navSuccess by successFlow.collectAsState(initial = null)
            val navError by errorFlow.collectAsState(initial = null)

            ClearNavResultToastsOnDispose(backStackEntry)

            // ✅ 1) UsersApi 的 pictureUrl 優先
            val avatarFromUsersApi = remember(pUi.pictureUrl) {
                toHttpUriOrNull(pUi.pictureUrl)
            }

            // ✅ 2) 沒有才 fallback 你原本 summary 的 avatar
            val avatar = avatarFromUsersApi ?: homeUi.summary?.avatarUrl

            // ✅ 3) UsersApi 的 name
            val nameText = pUi.name?.trim()?.takeIf { it.isNotBlank() } ?: "Guest"

            // ✅ 4) age 先用 ProfileApi 回來的
            val ageText = pUi.profile?.age?.let { "$it years old" } ?: "—"

            // ✅ URLs (Privacy Policy 內會包含 Data Deletion Policy)
            val uriHandler = LocalUriHandler.current
            val termsUrl = stringResource(R.string.url_terms)
            val privacyUrl = stringResource(R.string.url_privacy)
            val featureUrl = stringResource(R.string.url_feature_request)
            val supportMailUrl = stringResource(R.string.url_support_email)

            val membershipVm: MembershipViewModel = viewModel(
                viewModelStoreOwner = homeBackStackEntry,
                factory = HiltViewModelFactory(activity, homeBackStackEntry)
            )

            val membershipUi by membershipVm.ui.collectAsState()

            val restoreSubscriptionVm: RestoreSubscriptionViewModel = viewModel(
                viewModelStoreOwner = homeBackStackEntry,
                factory = HiltViewModelFactory(activity, homeBackStackEntry)
            )
            val restoreSubscriptionUi by restoreSubscriptionVm.ui.collectAsState()

            LaunchedEffect(Unit) {
                membershipVm.refresh()
            }

            val membershipDisplay = MembershipUiMapper.map(
                status = membershipUi.premiumStatus,
                currentPremiumUntil = membershipUi.currentPremiumUntil,
                trialDaysLeft = membershipUi.trialDaysLeft,
                paymentIssue = membershipUi.paymentIssue
            )

            Box(Modifier.fillMaxSize()) {
                CompositionLocalProvider(LocalActivityResultRegistryOwner provides owner) {
                    SettingsScreen(
                        avatarUrl = avatar,
                        profileName = nameText,
                        ageText = ageText,
                        homeSummary = homeUi.summary,
                        todayNutrition = homeUi.todayNutrition,
                        currentTab = HomeTab.Personal,
                        currentLanguageTag = localeController.tag,
                        onLanguageSelected = { tag ->
                            val normalizedTag = LanguageManager.normalizeTag(tag)

                            localeController.set(normalizedTag)
                            LanguageManager.applyLanguage(normalizedTag)
                            LanguageSessionFlag.markChanged()
                            onSetLocale(normalizedTag)

                            scope.launch {
                                withContext(Dispatchers.IO) {
                                    runCatching { languageStore.save(normalizedTag) }
                                    runCatching { store.setLocaleTag(normalizedTag) }
                                    runCatching { profileRepo.updateLocaleOnly(normalizedTag) }
                                }
                            }
                        },
                        onOpenCamera = {
                            scope.launch {
                                val hasActiveAccess = withContext(Dispatchers.IO) {
                                    entitlementSyncer.hasActivePremiumAccess()
                                }

                                membershipVm.refresh()

                                if (hasActiveAccess) {
                                    nav.navigate(Routes.CAMERA) {
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                } else {
                                    nav.navigate(Routes.SETTINGS_SCAN_SUBSCRIPTION) {
                                        launchSingleTop = true
                                        restoreState = false
                                    }
                                }
                            }
                        },
                        onCheckCanUseScan = {
                            val hasActiveAccess = withContext(Dispatchers.IO) {
                                entitlementSyncer.hasActivePremiumAccess()
                            }
                            membershipVm.refresh()
                            hasActiveAccess
                        },
                        onOpenSavedFoods = {
                            nav.navigate(Routes.SAVED_FOODS) {
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
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
                        onBack = {
                            nav.navigate(Routes.HOME) {
                                launchSingleTop = true
                                restoreState = true
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
                        onOpenRingColorsExplained = {
                            nav.navigate(Routes.RING_COLORS_EXPLAINED) {
                                launchSingleTop = true
                            }
                        },
                        onOpenPersonalDetails = { nav.navigate(Routes.PERSONAL_DETAILS) },
                        premiumStatusSubtitle = membershipDisplay.subtitle,
                        premiumStatusKind = membershipDisplay.kind,
                        canUseScan = membershipUi.canUseScan,
                        onOpenSubscription = {
                            nav.navigate(Routes.SETTINGS_SCAN_SUBSCRIPTION) {
                                launchSingleTop = true
                                restoreState = false
                            }
                        },
                        onManageSubscription = {
                            openGooglePlaySubscriptionManagement(activity)
                        },
                        onFixPaymentIssue = {
                            openGooglePlaySubscriptionManagement(activity)
                        },
                        onOpenReferral = {
                            nav.navigate(Routes.REFERRALS) {
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        onOpenNotificationInbox = {
                            nav.navigate(Routes.NOTIFICATION_INBOX) {
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        onOpenWidgetGuide = {
                            nav.navigate(Routes.WIDGET_GUIDE) {
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        onOpenTerms = { uriHandler.openUri(termsUrl) },
                        onOpenPrivacy = { uriHandler.openUri(privacyUrl) },
                        onOpenSupportEmail = { uriHandler.openUri(supportMailUrl) },
                        restoreSubscriptionUiState = restoreSubscriptionUi,
                        onOpenRestoreSubscription = restoreSubscriptionVm::openManualRestore,
                        onRestoreSubscription = {
                            restoreSubscriptionVm.restoreSubscription(
                                onRestored = {},
                                onMembershipMayHaveChanged = {
                                    membershipVm.refresh()
                                    runCatching {
                                        nav.getBackStackEntry(Routes.HOME)
                                            .savedStateHandle[Routes.MEMBERSHIP_REFRESH_TICK] = System.currentTimeMillis()
                                    }
                                }
                            )
                        },
                        onDismissRestoreSubscription = restoreSubscriptionVm::closeDialog,
                        onMaybeLaterRestoreSubscription = restoreSubscriptionVm::dismissForSession,
                        onDeleteAccount = { subscriptionWarningAcknowledged ->
                            scope.launch {
                                val r = accountRepo.deleteAccount(
                                    subscriptionWarningAcknowledged = subscriptionWarningAcknowledged
                                )
                                if (r.isSuccess) {
                                    nav.navigate(Routes.LANDING) {
                                        popUpTo(0) { inclusive = true }
                                        launchSingleTop = true
                                        restoreState = false
                                    }
                                } else {
                                    backStackEntry.savedStateHandle[NavResults.ERROR_TOAST] =
                                        (r.exceptionOrNull()?.message ?: "Delete account failed. Please sign in again and retry.")
                                }
                            }
                        },
                        logoutLoading = pUi.logoutLoading,
                        logoutErrorVisible = pUi.logoutError,
                        onLogout = settingsVm::logout
                    )
                }

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
                            modifier = Modifier.align(Alignment.TopCenter)
                        )
                        LaunchedEffect(navSuccess) {
                            delay(2_000)
                            backStackEntry.savedStateHandle[NavResults.SUCCESS_TOAST] = null
                        }
                    }
                }
            }
        }


        composable(Routes.WIDGET_GUIDE) {
            WidgetGuideScreen(
                onBack = { nav.popBackStack() }
            )
        }

        composable(Routes.RING_COLORS_EXPLAINED) {
            RingColorsExplainedScreen(
                onBack = { nav.popBackStack() }
            )
        }

        composable(Routes.PERSONAL_DETAILS) { backStackEntry ->
            val activity = (LocalContext.current.findActivity() ?: hostActivity)
            val homeBackStackEntry = remember(backStackEntry) {
                nav.getBackStackEntry(Routes.HOME)
            }

            val settingsVm: SettingsViewModel = viewModel(
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

            ClearNavResultToastsOnDispose(backStackEntry)

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
                    onEditDailyWorkoutGoal = { nav.navigate(Routes.EDIT_WORKOUT_GOAL) }
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
                            modifier = Modifier.align(Alignment.TopCenter)
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
                                ?.set(NavResults.SUCCESS_TOAST, "Saved successfully !")

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
                        ?.set(NavResults.SUCCESS_TOAST, "Saved successfully !")
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
                        ?.set(NavResults.SUCCESS_TOAST, "Saved successfully !")
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
                        ?.set(NavResults.SUCCESS_TOAST, "Saved successfully !")
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
                        ?.set(NavResults.SUCCESS_TOAST, "Saved successfully !")
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
                        ?.set(NavResults.SUCCESS_TOAST, "Saved successfully !")
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
                        ?.set(NavResults.SUCCESS_TOAST, "Saved successfully !")
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
                        ?.set(NavResults.SUCCESS_TOAST, "Saved successfully !")
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
                        ?.set(NavResults.SUCCESS_TOAST, "Saved successfully !")
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

            ClearNavResultToastsOnDispose(backStackEntry)

            Box(Modifier.fillMaxSize()) {

                EditNutritionGoalsRoute(
                    onBack = { nav.popBackStack() },
                    onAutoGenerate = { nav.navigate(Routes.AUTO_GENERATE_FLOW) }, // ✅ 改這裡
                    onSaved = {
                        nav.previousBackStackEntry
                            ?.savedStateHandle
                            ?.set(NavResults.SUCCESS_TOAST, "Saved successfully !")
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
                            modifier = Modifier.align(Alignment.TopCenter)
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
                    onNext = { nav.navigate(Routes.AUTO_GENERATE_HEIGHT) }
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
                    onNext = { nav.navigate(Routes.AUTO_GENERATE_WEIGHT) }
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
                    onNext = { nav.navigate(Routes.AUTO_GENERATE_GOALS) }
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
                    onNext = { nav.navigate(Routes.AUTO_GENERATE_GOALS_CALC) }
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

            val cameraFallbackPaywallRoute = remember(backStackEntry) {
                nav.resolveCameraFallbackPaywallRoute()
            }

            var cameraGateResolved by rememberSaveable { mutableStateOf(false) }
            var cameraAllowed by rememberSaveable { mutableStateOf(false) }

            LaunchedEffect(Unit) {
                val hasActiveAccess = withContext(Dispatchers.IO) {
                    entitlementSyncer.hasActivePremiumAccess()
                }

                if (hasActiveAccess) {
                    cameraAllowed = true
                    cameraGateResolved = true
                } else {
                    cameraAllowed = false
                    cameraGateResolved = true

                    nav.navigate(cameraFallbackPaywallRoute) {
                        popUpTo(Routes.CAMERA) {
                            inclusive = true
                        }
                        launchSingleTop = true
                        restoreState = false
                    }
                }
            }

            if (!cameraGateResolved || !cameraAllowed) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
                return@composable
            }

            val cameraOwner = backStackEntry
            val flowVm: FoodLogFlowViewModel = viewModel(
                viewModelStoreOwner = cameraOwner,
                factory = HiltViewModelFactory(activity, cameraOwner)
            )

            // ✅ 取得 HomeViewModel（讓 Camera 成功後可以把 recent upload 狀態交給 Home）
            val homeEntry = remember(nav) {
                runCatching { nav.getBackStackEntry(Routes.HOME) }.getOrNull()
            }
            val homeVm: HomeViewModel? = homeEntry?.let { entry ->
                viewModel(
                    viewModelStoreOwner = entry,
                    factory = HiltViewModelFactory(activity, entry)
                )
            }

            // ✅ 外部要求切模式（Detail 回來）
            var initialMode by rememberSaveable { mutableStateOf(CameraMode.FOOD) }
            val modeReqFlow = remember(backStackEntry) {
                backStackEntry.savedStateHandle.getStateFlow<String?>("camera_mode", null)
            }
            val modeReqRaw by modeReqFlow.collectAsState(initial = null)

            LaunchedEffect(modeReqRaw) {
                val raw = modeReqRaw ?: return@LaunchedEffect
                val req = runCatching { CameraMode.valueOf(raw) }.getOrNull()
                backStackEntry.savedStateHandle.remove<String>("camera_mode") // consume
                if (req != null) {
                    flowVm.reset()
                    initialMode = req
                }
            }

            CompositionLocalProvider(LocalActivityResultRegistryOwner provides owner) {
                val st by flowVm.state.collectAsState()

                // ✅ toast 2 秒後自動清掉（不動 envelope）
                LaunchedEffect(st.cooldown, st.refused, st.error) {
                    val hasToast = st.cooldown != null || st.refused != null || !st.error.isNullOrBlank()
                    if (hasToast) {
                        delay(2_000)
                        flowVm.clearTransient()
                    }
                }

                // ✅ 防止 Camera toast 還沒自動消失就離開 Camera，回來後又顯示舊 toast。
                DisposableEffect(flowVm) {
                    onDispose {
                        flowVm.clearTransient()
                    }
                }

                // ✅ 只在 FAILED/DELETED 且 error != null 時顯示卡片
                val apiErrUi = remember(st.envelope?.error, st.apiError) {
                    ApiErrorUiMapper.map(st.apiError ?: st.envelope?.error)
                }
                val showApiCard = apiErrUi != null && (
                        st.apiError != null ||
                        st.envelope?.status == FoodLogStatus.FAILED ||
                        st.envelope?.status == FoodLogStatus.DELETED
                )

                fun handleClientAction(action: ClientAction) {
                    when (action) {
                        ClientAction.TRY_LABEL -> {
                            flowVm.reset()
                            initialMode = CameraMode.LABEL
                        }

                        ClientAction.SCAN_AGAIN,
                        ClientAction.TRY_BARCODE -> {
                            flowVm.reset()
                            initialMode = CameraMode.BARCODE
                        }

                        ClientAction.TRY_PHOTO,
                        ClientAction.RETAKE_PHOTO -> {
                            flowVm.reset()
                            initialMode = CameraMode.FOOD
                        }

                        ClientAction.RETRY_LATER -> {
                            flowVm.reset()
                        }

                        ClientAction.CHECK_NETWORK -> {
                            openNetworkSettings(ctx)
                        }

                        ClientAction.CONTACT_SUPPORT -> {
                            openSupportEmail(ctx)
                        }

                        ClientAction.ENTER_MANUALLY -> {
                            Toast.makeText(ctx, "手動輸入尚未實作，先改用標籤辨識", Toast.LENGTH_SHORT).show()
                            flowVm.reset()
                            initialMode = CameraMode.LABEL
                        }
                    }
                }

                // ✅ 回 Home：優先 pop 回既有 Home；沒有再 navigate
                fun goHome() {
                    val popped = nav.popBackStack(Routes.HOME, false)
                    if (!popped) {
                        nav.navigate(Routes.HOME) {
                            launchSingleTop = true
                        }
                    }
                }

                // ✅ Home 第四區塊右上角時間，例如 11:10
                fun nowHm(): String =
                    LocalTime.now()
                        .format(DateTimeFormatter.ofPattern("HH:mm"))

                fun handleCreatedFoodLog(
                    env: FoodLogEnvelopeDto,
                    previewUri: String?
                ) {
                    val latestTimeText = resolveFoodLogTimeText(
                        env = env,
                        fallbackTimeText = nowHm()
                    )

                    homeVm?.onFoodLogCreated(
                        env = env,
                        previewUri = previewUri,
                        timeText = latestTimeText
                    )

                    when (env.status) {
                        FoodLogStatus.DRAFT,
                        FoodLogStatus.SAVED,
                        FoodLogStatus.PENDING -> {
                            flowVm.reset()
                            goHome()
                        }

                        FoodLogStatus.FAILED,
                        FoodLogStatus.DELETED -> {
                            nav.navigate(Routes.foodLogDetail(env.foodLogId)) {
                                launchSingleTop = true
                            }
                        }
                    }
                }

                // ✅ FOOD / LABEL 要先複製 preview，因為原始 file 之後會被 finally 刪掉
                fun createPreviewCopy(src: File): String? = runCatching {
                    val preview = File(ctx.cacheDir, "recent_preview_${System.currentTimeMillis()}.jpg")
                    src.copyTo(preview, overwrite = true)
                    Uri.fromFile(preview).toString()
                }.getOrNull()

                // ✅ ALBUM
                fun copyUriToPreviewFile(ctx: Context, srcUri: Uri): String? = runCatching {
                    val preview = File(ctx.cacheDir, "recent_preview_${System.currentTimeMillis()}.jpg")
                    ctx.contentResolver.openInputStream(srcUri)?.use { input ->
                        preview.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    } ?: return null
                    Uri.fromFile(preview).toString()
                }.getOrNull()

                Box(Modifier.fillMaxSize()) {
                    CameraScreen(
                        onClose = {
                            flowVm.reset()
                            nav.popBackStack()
                        },
                        busy = st.loading,
                        initialMode = initialMode,

                        // ✅ ALBUM 一律走 album，不再看當前 mode
                        onAlbumPicked = { uri ->
                            val previewUri = copyUriToPreviewFile(ctx, uri)
                            flowVm.submitAlbum(ctx, uri) { env ->
                                handleCreatedFoodLog(
                                    env = env,
                                    previewUri = previewUri
                                )
                            }
                        },

                        onShutterCaptured = { mode, file ->
                            when (mode) {
                                CameraMode.FOOD -> {
                                    val previewUri = createPreviewCopy(file)
                                    flowVm.submitPhotoFile(file) { env ->
                                        handleCreatedFoodLog(
                                            env = env,
                                            previewUri = previewUri
                                        )
                                    }
                                }

                                CameraMode.LABEL -> {
                                    val previewUri = createPreviewCopy(file)
                                    flowVm.submitLabelFile(file) { env ->
                                        handleCreatedFoodLog(
                                            env = env,
                                            previewUri = previewUri
                                        )
                                    }
                                }

                                CameraMode.BARCODE -> Unit
                            }
                        },

                        onBarcodeScanned = { code ->
                            if (st.loading) return@CameraScreen

                            flowVm.submitBarcode(code) { env ->
                                when (env.status) {
                                    FoodLogStatus.DRAFT,
                                    FoodLogStatus.SAVED -> {
                                        val latestTimeText = resolveFoodLogTimeText(
                                            env = env,
                                            fallbackTimeText = nowHm()
                                        )

                                        homeVm?.onFoodLogCreated(
                                            env = env,
                                            previewUri = null,
                                            timeText = latestTimeText
                                        )
                                        flowVm.reset()
                                        goHome()
                                    }

                                    FoodLogStatus.PENDING -> {
                                        val latestTimeText = resolveFoodLogTimeText(
                                            env = env,
                                            fallbackTimeText = nowHm()
                                        )

                                        homeVm?.onFoodLogCreated(
                                            env = env,
                                            previewUri = null,
                                            timeText = latestTimeText
                                        )
                                        flowVm.reset()
                                        goHome()
                                    }

                                    FoodLogStatus.FAILED,
                                    FoodLogStatus.DELETED -> {
                                        // 留在 Camera：ApiErrorCard 會顯示（用 st.envelope?.error / st.apiError）
                                    }
                                }
                            }
                        }
                    )

                    if (showApiCard && apiErrUi != null) {
                        ApiErrorCard(
                            ui = apiErrUi,
                            onAction = ::handleClientAction,
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(horizontal = 16.dp)
                                .padding(bottom = 200.dp) // 避開底部 tiles/shutter
                        )
                    }

                    when {
                        st.cooldown != null -> ErrorTopToast(
                            message = "冷卻中：${st.cooldown?.cooldownSeconds ?: "-"}s",
                            modifier = Modifier.align(Alignment.TopCenter)
                        )

                        st.refused != null -> ErrorTopToast(
                            message = st.refused?.hint ?: "無法識別，請只拍攝食物",
                            modifier = Modifier.align(Alignment.TopCenter)
                        )

                        !st.error.isNullOrBlank() -> ErrorTopToast(
                            message = st.error!!,
                            modifier = Modifier.align(Alignment.TopCenter)
                        )
                    }
                }
            }
        }

        composable(Routes.SAVED_FOODS) { backStackEntry ->
            val activity = (LocalContext.current.findActivity() ?: hostActivity)
            val homeBackStackEntry = remember(backStackEntry) {
                nav.getBackStackEntry(Routes.HOME)
            }

            val savedFoodsVm: SavedFoodsViewModel = viewModel(
                viewModelStoreOwner = homeBackStackEntry,
                factory = HiltViewModelFactory(activity, homeBackStackEntry)
            )

            SavedFoodsScreen(
                vm = savedFoodsVm,
                onBack = { nav.safePopBackStack() },
                onOpenDetail = { foodLogId, previewUri, timeText ->
                    nav.currentBackStackEntry
                        ?.savedStateHandle
                        ?.set(Routes.RECENT_UPLOAD_PREVIEW_URI, previewUri)

                    nav.currentBackStackEntry
                        ?.savedStateHandle
                        ?.set(Routes.RECENT_UPLOAD_TIME_TEXT, timeText)

                    nav.currentBackStackEntry
                        ?.savedStateHandle
                        ?.set(Routes.RECENT_UPLOAD_SOURCE, Routes.SAVED_FOODS)

                    nav.navigate(Routes.recentUploadDetail(foodLogId)) {
                        launchSingleTop = true
                    }
                }
            )
        }

        composable(Routes.RECENT_UPLOAD_DETAIL) { backStackEntry ->
            val activity = (LocalContext.current.findActivity() ?: hostActivity)
            val recentUploadOwner = backStackEntry

            val flowVm: FoodLogFlowViewModel = viewModel(
                viewModelStoreOwner = recentUploadOwner,
                factory = HiltViewModelFactory(activity, recentUploadOwner)
            )

            val homeEntry = remember(nav) {
                runCatching { nav.getBackStackEntry(Routes.HOME) }.getOrNull()
            }

            val homeVm: HomeViewModel? = homeEntry?.let { entry ->
                viewModel(
                    viewModelStoreOwner = entry,
                    factory = HiltViewModelFactory(activity, entry)
                )
            }

            val id = backStackEntry.arguments?.getString("id").orEmpty()

            val previewUri = remember(backStackEntry) {
                nav.previousBackStackEntry
                    ?.savedStateHandle
                    ?.get<String>(Routes.RECENT_UPLOAD_PREVIEW_URI)
            }

            val timeText = remember(backStackEntry) {
                nav.previousBackStackEntry
                    ?.savedStateHandle
                    ?.get<String>(Routes.RECENT_UPLOAD_TIME_TEXT)
                    .orEmpty()
            }

            val savedFoodsVm: SavedFoodsViewModel? = homeEntry?.let { entry ->
                viewModel(
                    viewModelStoreOwner = entry,
                    factory = HiltViewModelFactory(activity, entry)
                )
            }

            val source = remember(backStackEntry) {
                nav.previousBackStackEntry
                    ?.savedStateHandle
                    ?.get<String>(Routes.RECENT_UPLOAD_SOURCE)
            }

            RecentUploadDetailScreen(
                foodLogId = id,
                previewUri = previewUri,
                timeText = timeText,
                vm = flowVm,
                onBack = {
                    if (source == Routes.SAVED_FOODS) {
                        nav.safePopBackStack()
                    } else {
                        nav.goHome()
                    }
                },
                onSave = { updatedEnv ->
                    val latestTimeText = resolveFoodLogTimeText(
                        env = updatedEnv,
                        fallbackTimeText = timeText
                    )

                    nav.previousBackStackEntry
                        ?.savedStateHandle
                        ?.set(Routes.RECENT_UPLOAD_TIME_TEXT, latestTimeText)

                    homeVm?.onRecentUploadUpdated(
                        env = updatedEnv,
                        previewUri = previewUri
                    )

                    savedFoodsVm?.onFoodLogUpdatedFromDetail(
                        env = updatedEnv,
                        previewUri = previewUri
                    )

                    if (source == Routes.SAVED_FOODS) {
                        nav.safePopBackStack()
                    } else {
                        nav.goHome()
                    }
                },
                onDeleted = { deletedFoodLogId ->
                    homeVm?.onRecentUploadDeleted(deletedFoodLogId)

                    if (source == Routes.SAVED_FOODS) {
                        savedFoodsVm?.refresh()
                        nav.safePopBackStack()
                    } else {
                        nav.goHome()
                    }
                }
            )
        }

        composable(Routes.PROGRESS) { backStackEntry ->
            val activity = (LocalContext.current.findActivity() ?: hostActivity)
            val vm: com.calai.bitecal.ui.home.ui.progress.model.ProgressViewModel = viewModel(
                viewModelStoreOwner = backStackEntry,
                factory = HiltViewModelFactory(activity, backStackEntry)
            )
            val goBackHome = remember(nav) { { nav.goHome() } }
            val onOpenTab: (HomeTab) -> Unit = remember(nav) {
                { tab ->
                    when (tab) {
                        HomeTab.Home -> nav.goHome()
                        HomeTab.Progress -> Unit
                        HomeTab.Weight -> nav.navigate(Routes.WEIGHT) { launchSingleTop = true; restoreState = true }
                        HomeTab.Fasting -> nav.navigate(Routes.FASTING) { launchSingleTop = true; restoreState = true }
                        HomeTab.Workout -> nav.navigate(Routes.WORKOUT_HISTORY) { launchSingleTop = true; restoreState = true }
                        HomeTab.Personal -> nav.navigate(Routes.SETTINGS) { launchSingleTop = true; restoreState = true }
                    }
                }
            }
            com.calai.bitecal.ui.home.ui.progress.ProgressScreen(
                vm = vm,
                onBack = goBackHome,
                onOpenTab = onOpenTab
            )
        }
        composable(Routes.REFERRALS) { backStackEntry ->
            val activity = (LocalContext.current.findActivity() ?: hostActivity)

            val vm: ReferralViewModel = viewModel(
                viewModelStoreOwner = backStackEntry,
                factory = HiltViewModelFactory(activity, backStackEntry)
            )

            val ui by vm.ui.collectAsState()
            val summary = ui.summary

            ReferralScreen(
                promoCode = summary?.promoCode ?: "—",
                successCount = summary?.successCount ?: 0L,
                pendingCount = summary?.pendingVerificationCount ?: 0L,
                rejectedCount = summary?.rejectedCount ?: 0L,
                recentClaims = summary?.recentClaims.orEmpty(),
                claimInFlight = ui.claimInFlight,
                error = ui.error,
                onBack = { nav.popBackStack() },
                onSubmitClaim = { code ->
                    vm.claim(code) {
                        nav.currentBackStackEntry
                            ?.savedStateHandle
                            ?.set(NavResults.SUCCESS_TOAST, "Referral code submitted")
                    }
                }
            )
        }

        composable(Routes.ONBOARD_SUBSCRIPTION) { backStackEntry ->
            val activity = (LocalContext.current.findActivity() ?: hostActivity)

            val vm: SubscriptionViewModel = viewModel(
                viewModelStoreOwner = backStackEntry,
                factory = HiltViewModelFactory(activity, backStackEntry)
            )

            val restoreSubscriptionVm: RestoreSubscriptionViewModel = viewModel(
                viewModelStoreOwner = backStackEntry,
                factory = HiltViewModelFactory(activity, backStackEntry)
            )
            val restoreSubscriptionUi by restoreSubscriptionVm.ui.collectAsState()

            val restoreLocaleKey = currentLocaleKey()

            val restoreDialogTitle = when (restoreSubscriptionUi.dialogState) {
                RestoreSubscriptionDialogState.Restored ->
                    stringResource(R.string.restore_subscription_dialog_success_title)

                RestoreSubscriptionDialogState.RestoredWithPaymentIssue ->
                    stringResource(R.string.restore_subscription_dialog_payment_issue_title)

                RestoreSubscriptionDialogState.NoActivePurchase ->
                    stringResource(R.string.restore_subscription_dialog_no_active_title)

                RestoreSubscriptionDialogState.Failed ->
                    stringResource(R.string.restore_subscription_dialog_failed_title)

                RestoreSubscriptionDialogState.BoundToAnotherAccount ->
                    stringResource(R.string.restore_subscription_dialog_bound_title)

                RestoreSubscriptionDialogState.Hidden,
                RestoreSubscriptionDialogState.CandidateFound,
                RestoreSubscriptionDialogState.Restoring ->
                    stringResource(R.string.restore_subscription_dialog_title)
            }

            val restoreDialogBody = when (restoreSubscriptionUi.dialogState) {
                RestoreSubscriptionDialogState.Restored ->
                    stringResource(R.string.restore_subscription_dialog_success_body)

                RestoreSubscriptionDialogState.RestoredWithPaymentIssue ->
                    stringResource(R.string.restore_subscription_dialog_payment_issue_body)

                RestoreSubscriptionDialogState.NoActivePurchase ->
                    stringResource(R.string.restore_subscription_dialog_no_active_body)

                RestoreSubscriptionDialogState.Failed ->
                    stringResource(R.string.restore_subscription_dialog_failed_body)

                RestoreSubscriptionDialogState.BoundToAnotherAccount ->
                    stringResource(R.string.restore_subscription_dialog_bound_body)

                RestoreSubscriptionDialogState.Hidden,
                RestoreSubscriptionDialogState.CandidateFound,
                RestoreSubscriptionDialogState.Restoring ->
                    stringResource(R.string.restore_subscription_dialog_body)
            }

            val restoreDialogCloseText = stringResource(R.string.common_close)
            val restoreDialogRestoreText = stringResource(R.string.settings_restore_subscription)
            val restoreDialogRestoringText = stringResource(R.string.restore_subscription_dialog_restoring)
            val restoreDialogMaybeLaterText = stringResource(R.string.common_maybe_later)

            fun goHomeAfterOnboardingSubscription() {
                runCatching {
                    nav.getBackStackEntry(Routes.HOME)
                        .savedStateHandle[Routes.MEMBERSHIP_REFRESH_TICK] = System.currentTimeMillis()
                }

                nav.navigate(Routes.HOME) {
                    popUpTo(0) { inclusive = true }
                    launchSingleTop = true
                    restoreState = false
                }
            }

            fun closeOnboardingSubscriptionAsFree() {
                onboardingPaywallRejectedOnce = true

                /**
                 * 使用者在 onboarding paywall 明確關閉：
                 * - 不開 trial
                 * - 不 premium
                 * - 直接以 FREE 身分進 HOME
                 * - 清掉整個 onboarding / paywall back stack，避免返回又回到訂閱頁
                 */
                nav.navigate(Routes.HOME) {
                    popUpTo(0) { inclusive = true }
                    launchSingleTop = true
                    restoreState = false
                }
            }

            var entitlementGateResolved by rememberSaveable { mutableStateOf(false) }
            var allowPaywall by rememberSaveable { mutableStateOf(false) }
            var showRestoreCandidate by rememberSaveable { mutableStateOf(false) }

            fun continueToOnboardingPaywallWithoutRestore() {
                showRestoreCandidate = false
                allowPaywall = true
                entitlementGateResolved = true
            }

            LaunchedEffect(Unit) {
                val hasServerAccess = withContext(Dispatchers.IO) {
                    entitlementSyncer.hasServerPremiumAccess()
                }

                if (hasServerAccess) {
                    goHomeAfterOnboardingSubscription()
                    return@LaunchedEffect
                }

                val hasRestoreCandidate = withContext(Dispatchers.IO) {
                    entitlementSyncer.hasActiveSubscriptionOnDevice()
                }

                if (hasRestoreCandidate) {
                    showRestoreCandidate = true
                    allowPaywall = false
                    entitlementGateResolved = true
                    restoreSubscriptionVm.openManualRestore()
                } else {
                    continueToOnboardingPaywallWithoutRestore()
                }
            }

            if (!entitlementGateResolved) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
                return@composable
            }

            if (showRestoreCandidate && !allowPaywall) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }

                key(restoreLocaleKey) {
                    RestoreSubscriptionDialog(
                        uiState = restoreSubscriptionUi,
                        title = restoreDialogTitle,
                        body = restoreDialogBody,
                        closeText = restoreDialogCloseText,
                        restoreText = restoreDialogRestoreText,
                        restoringText = restoreDialogRestoringText,
                        maybeLaterText = restoreDialogMaybeLaterText,
                        onDismiss = {
                            restoreSubscriptionVm.closeDialog()
                            continueToOnboardingPaywallWithoutRestore()
                        },
                        onMaybeLater = {
                            restoreSubscriptionVm.dismissForSession()
                            continueToOnboardingPaywallWithoutRestore()
                        },
                        onRestore = {
                            restoreSubscriptionVm.restoreSubscription(
                                onRestored = { goHomeAfterOnboardingSubscription() }
                            )
                        }
                    )
                }
                return@composable
            }

            if (!allowPaywall) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
                return@composable
            }

            BackHandler(enabled = true) {
                closeOnboardingSubscriptionAsFree()
            }

            OnboardSubscriptionScreen(
                vm = vm,
                activity = activity,
                onCloseToSignIn = {
                    closeOnboardingSubscriptionAsFree()
                },
                onPurchased = {
                    goHomeAfterOnboardingSubscription()
                }
            )
        }

        composable(Routes.HOME_SCAN_SUBSCRIPTION) { backStackEntry ->
            val activity = (LocalContext.current.findActivity() ?: hostActivity)

            val vm: SubscriptionViewModel = viewModel(
                viewModelStoreOwner = backStackEntry,
                factory = HiltViewModelFactory(activity, backStackEntry)
            )

            fun goHomeAfterHomeScanSubscription() {
                runCatching {
                    nav.getBackStackEntry(Routes.HOME)
                        .savedStateHandle[Routes.MEMBERSHIP_REFRESH_TICK] = System.currentTimeMillis()
                }

                val popped = nav.popBackStack(
                    route = Routes.HOME,
                    inclusive = false
                )

                if (!popped) {
                    nav.navigate(Routes.HOME) {
                        popUpTo(Routes.HOME_SCAN_SUBSCRIPTION) {
                            inclusive = true
                        }
                        launchSingleTop = true
                        restoreState = false
                    }
                }
            }

            var entitlementGateResolved by rememberSaveable { mutableStateOf(false) }
            var allowPaywall by rememberSaveable { mutableStateOf(false) }

            LaunchedEffect(Unit) {
                val hasActiveAccess = withContext(Dispatchers.IO) {
                    entitlementSyncer.hasActivePremiumAccess()
                }

                if (hasActiveAccess) {
                    goHomeAfterHomeScanSubscription()
                } else {
                    allowPaywall = true
                    entitlementGateResolved = true
                }
            }

            if (!entitlementGateResolved || !allowPaywall) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
                return@composable
            }

            BackHandler(enabled = true) {
                goHomeAfterHomeScanSubscription()
            }

            OnboardSubscriptionScreen(
                vm = vm,
                activity = activity,
                onCloseToSignIn = {
                    // Home ScanFab 進來的付費牆，關閉後回 HOME；不變更使用者權益。
                    goHomeAfterHomeScanSubscription()
                },
                onPurchased = {
                    // Google Play 付款 / trial 成功後，刷新 HOME membership 後回 HOME。
                    goHomeAfterHomeScanSubscription()
                }
            )
        }

        composable(Routes.HOME_WORKOUT_SUBSCRIPTION) { backStackEntry ->
            val activity = (LocalContext.current.findActivity() ?: hostActivity)

            val vm: SubscriptionViewModel = viewModel(
                viewModelStoreOwner = backStackEntry,
                factory = HiltViewModelFactory(activity, backStackEntry)
            )

            fun goHomeAfterWorkoutPaywallClose() {
                runCatching {
                    nav.getBackStackEntry(Routes.HOME)
                        .savedStateHandle[Routes.MEMBERSHIP_REFRESH_TICK] = System.currentTimeMillis()
                }

                val popped = nav.popBackStack(
                    route = Routes.HOME,
                    inclusive = false
                )

                if (!popped) {
                    nav.navigate(Routes.HOME) {
                        popUpTo(Routes.HOME_WORKOUT_SUBSCRIPTION) {
                            inclusive = true
                        }
                        launchSingleTop = true
                        restoreState = false
                    }
                }
            }

            fun goHomeAfterWorkoutPurchase() {
                runCatching {
                    val homeHandle = nav.getBackStackEntry(Routes.HOME).savedStateHandle
                    homeHandle[Routes.MEMBERSHIP_REFRESH_TICK] = System.currentTimeMillis()
                    homeHandle[Routes.OPEN_WORKOUT_SHEET_TICK] = System.currentTimeMillis()
                }

                val popped = nav.popBackStack(
                    route = Routes.HOME,
                    inclusive = false
                )

                if (!popped) {
                    nav.navigate(Routes.HOME) {
                        popUpTo(Routes.HOME_WORKOUT_SUBSCRIPTION) {
                            inclusive = true
                        }
                        launchSingleTop = true
                        restoreState = false
                    }
                }
            }

            var entitlementGateResolved by rememberSaveable { mutableStateOf(false) }
            var allowPaywall by rememberSaveable { mutableStateOf(false) }

            LaunchedEffect(Unit) {
                val hasActiveAccess = withContext(Dispatchers.IO) {
                    entitlementSyncer.hasActivePremiumAccess()
                }

                if (hasActiveAccess) {
                    goHomeAfterWorkoutPaywallClose()
                } else {
                    allowPaywall = true
                    entitlementGateResolved = true
                }
            }

            if (!entitlementGateResolved || !allowPaywall) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
                return@composable
            }

            BackHandler(enabled = true) {
                goHomeAfterWorkoutPaywallClose()
            }

            OnboardSubscriptionScreen(
                vm = vm,
                activity = activity,
                onCloseToSignIn = {
                    goHomeAfterWorkoutPaywallClose()
                },
                onPurchased = {
                    goHomeAfterWorkoutPurchase()
                }
            )
        }

        composable(Routes.SETTINGS_SCAN_SUBSCRIPTION) { backStackEntry ->
            val activity = (LocalContext.current.findActivity() ?: hostActivity)

            val vm: SubscriptionViewModel = viewModel(
                viewModelStoreOwner = backStackEntry,
                factory = HiltViewModelFactory(activity, backStackEntry)
            )

            fun goSettingsAfterSettingsScanSubscription() {
                val popped = nav.popBackStack(
                    route = Routes.SETTINGS,
                    inclusive = false
                )

                if (!popped) {
                    nav.navigate(Routes.SETTINGS) {
                        popUpTo(Routes.SETTINGS_SCAN_SUBSCRIPTION) {
                            inclusive = true
                        }
                        launchSingleTop = true
                        restoreState = false
                    }
                }
            }

            fun goCameraAfterSettingsScanGate() {
                nav.navigate(Routes.CAMERA) {
                    popUpTo(Routes.SETTINGS_SCAN_SUBSCRIPTION) {
                        inclusive = true
                    }
                    launchSingleTop = true
                    restoreState = true
                }
            }

            fun goHomeAfterSettingsScanPurchase() {
                runCatching {
                    nav.getBackStackEntry(Routes.HOME)
                        .savedStateHandle[Routes.MEMBERSHIP_REFRESH_TICK] = System.currentTimeMillis()
                }

                val popped = nav.popBackStack(
                    route = Routes.HOME,
                    inclusive = false
                )

                if (!popped) {
                    nav.navigate(Routes.HOME) {
                        popUpTo(Routes.SETTINGS_SCAN_SUBSCRIPTION) {
                            inclusive = true
                        }
                        launchSingleTop = true
                        restoreState = false
                    }
                }
            }

            var entitlementGateResolved by rememberSaveable { mutableStateOf(false) }
            var allowPaywall by rememberSaveable { mutableStateOf(false) }

            LaunchedEffect(Unit) {
                val hasActiveAccess = withContext(Dispatchers.IO) {
                    entitlementSyncer.hasActivePremiumAccess()
                }

                if (hasActiveAccess) {
                    // 理論上 active user 不該進到這個 route。
                    // 但若 membershipUi stale 導致誤進，直接讓他使用 Scan。
                    goCameraAfterSettingsScanGate()
                } else {
                    allowPaywall = true
                    entitlementGateResolved = true
                }
            }

            if (!entitlementGateResolved || !allowPaywall) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
                return@composable
            }

            BackHandler(enabled = true) {
                goSettingsAfterSettingsScanSubscription()
            }

            OnboardSubscriptionScreen(
                vm = vm,
                activity = activity,
                onCloseToSignIn = {
                    // Settings ScanFab 進來的付費牆：
                    // close 後回 Settings，不改使用者權益。
                    goSettingsAfterSettingsScanSubscription()
                },
                onPurchased = {
                    // 付款成功 / trial 成功：
                    // 更新會員狀態後回 HOME。
                    goHomeAfterSettingsScanPurchase()
                }
            )
        }

        composable(Routes.PREMIUM_REWARDS) { backStackEntry ->
            val activity = (LocalContext.current.findActivity() ?: hostActivity)

            val vm: PremiumRewardsViewModel = viewModel(
                viewModelStoreOwner = backStackEntry,
                factory = HiltViewModelFactory(activity, backStackEntry)
            )

            val ui by vm.ui.collectAsState()

            PremiumRewardsScreen(
                loading = ui.loading,
                error = ui.error,
                summary = ui.summary,
                rewards = ui.rewards,
                onRetry = { vm.refresh() },
                onBack = { nav.popBackStack() }
            )
        }


        composable(Routes.NOTIFICATION_INBOX) { backStackEntry ->
            val activity = (LocalContext.current.findActivity() ?: hostActivity)

            val vm: NotificationInboxViewModel = viewModel(
                viewModelStoreOwner = backStackEntry,
                factory = HiltViewModelFactory(activity, backStackEntry)
            )

            val ui by vm.ui.collectAsState()

            NotificationInboxScreen(
                loading = ui.loading,
                error = ui.error,
                items = ui.items,
                markingReadIds = ui.markingReadIds,
                onRetry = { vm.refresh() },
                onMarkRead = { notificationId ->
                    vm.markRead(notificationId)
                },
                onBack = { nav.popBackStack() }
            )
        }

        composable(Routes.REMINDERS) { SimplePlaceholder("Reminders") }

    }
}

@Composable
private fun SimplePlaceholder(title: String) {
    Text(modifier = Modifier.padding(24.dp), text = "TODO: $title page")
}

private fun openNetworkSettings(ctx: Context) {
    val intent = Intent(Settings.ACTION_WIFI_SETTINGS).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    runCatching { ctx.startActivity(intent) }
}

private fun openSupportEmail(ctx: Context) {
    val intent = Intent(Intent.ACTION_SENDTO).apply {
        data = Uri.parse("mailto:support@bitcalai.example")
        putExtra(Intent.EXTRA_SUBJECT, "BiteCal Support")
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    runCatching { ctx.startActivity(intent) }
}

private fun isOnboardingRoute(route: String?): Boolean {
    if (route.isNullOrBlank()) return false
    return route == Routes.LANDING ||
            route == Routes.ONBOARD_GENDER ||
            route == Routes.ONBOARD_REFERRAL ||
            route == Routes.ONBOARD_REFERRAL_CODE ||
            route == Routes.ONBOARD_AGE ||
            route == Routes.ONBOARD_HEIGHT ||
            route == Routes.ONBOARD_WEIGHT ||
            route == Routes.ONBOARD_GOAL_WEIGHT ||
            route == Routes.ONBOARD_EXERCISE_FREQ ||
            route == Routes.ONBOARD_GOAL ||
            route == Routes.ONBOARD_NOTIF ||
            route == Routes.ONBOARD_HEALTH_CONNECT ||
            route == Routes.PLAN_PROGRESS ||
            route == Routes.ROUTE_PLAN ||
            route == Routes.ONBOARD_SUBSCRIPTION
}

private fun isAuthOrEntryRoute(route: String?): Boolean {
    if (route.isNullOrBlank()) return false

    return route == Routes.APP_ENTRY ||
            route.startsWith(Routes.REQUIRE_SIGN_IN) ||
            route.startsWith(Routes.SIGN_IN_EMAIL_ENTER) ||
            route.startsWith(Routes.SIGN_IN_EMAIL_CODE)
}
