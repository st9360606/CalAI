package com.calai.bitecal.ui.home

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.StepsRecord
import androidx.lifecycle.compose.LifecycleResumeEffect
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.calai.bitecal.R
import com.calai.bitecal.data.activity.healthconnect.HealthConnectPermissionIntents
import com.calai.bitecal.data.activity.healthconnect.HealthConnectPermissionPrefs
import com.calai.bitecal.data.activity.healthconnect.HealthConnectPermissionProxyActivity
import com.calai.bitecal.data.activity.model.DailyActivityStatus
import com.calai.bitecal.data.fasting.notifications.NotificationPermission
import com.calai.bitecal.data.foodlog.repo.HomeTodayNutritionSummary
import com.calai.bitecal.data.home.repo.HomeSummary
import com.calai.bitecal.data.profile.repo.UserProfileStore
import com.calai.bitecal.data.profile.repo.kgToLbs1
import com.calai.bitecal.i18n.currentLocaleKey
import com.calai.bitecal.ui.common.design.BiteCalScreenFrame
import com.calai.bitecal.ui.common.haptic.biteCalClickable
import com.calai.bitecal.ui.common.haptic.rememberClickWithHaptic
import com.calai.bitecal.ui.home.components.CalendarStrip
import com.calai.bitecal.ui.home.components.CaloriesCardModern
import com.calai.bitecal.ui.home.components.HealthScoreCardModern
import com.calai.bitecal.ui.home.components.LightHomeBackground
import com.calai.bitecal.ui.home.components.MacroRowModern
import com.calai.bitecal.ui.home.components.MainBottomBar
import com.calai.bitecal.ui.home.components.MicronutrientRowModern
import com.calai.bitecal.ui.home.components.PagerDots
import com.calai.bitecal.ui.home.components.PanelHeights
import com.calai.bitecal.ui.home.components.RecentlyUploadedEmptySection
import com.calai.bitecal.ui.home.components.StepsWorkoutRowModern
import com.calai.bitecal.ui.home.components.WeightFastingRowModern
import com.calai.bitecal.ui.home.components.menu.HomeQuickActionMenu
import com.calai.bitecal.ui.home.components.scan.ScanFab
import com.calai.bitecal.ui.home.components.toast.ErrorTopToast
import com.calai.bitecal.ui.home.components.toast.SuccessTopToast
import com.calai.bitecal.ui.home.model.HomeViewModel
import com.calai.bitecal.ui.home.ui.camera.components.CameraPermissionPrefs
import com.calai.bitecal.ui.home.ui.camera.components.CameraPermissionProxyActivity
import com.calai.bitecal.ui.home.ui.camera.components.openCameraPermissionSettings
import com.calai.bitecal.ui.home.ui.fasting.model.FastingPlanViewModel
import com.calai.bitecal.ui.home.ui.foodlog.RecentUploadCard
import com.calai.bitecal.ui.home.ui.foodlog.dialog.DeleteFoodLogDialog
import com.calai.bitecal.ui.home.ui.water.components.WaterIntakeCard
import com.calai.bitecal.ui.home.ui.water.model.WaterUiState
import com.calai.bitecal.ui.home.ui.water.model.WaterViewModel
import com.calai.bitecal.ui.home.ui.weight.components.computeWeightProgress
import com.calai.bitecal.ui.home.ui.weight.components.computeWeightProgressFractionLbs
import com.calai.bitecal.ui.home.ui.weight.model.WeightViewModel
import com.calai.bitecal.ui.home.ui.workout.WorkoutTrackerHost
import com.calai.bitecal.ui.home.ui.workout.model.WorkoutViewModel
import com.calai.bitecal.ui.home.workoutgate.WorkoutPremiumGate
import com.calai.bitecal.ui.home.workoutgate.WorkoutPremiumGateDecision
import com.calai.bitecal.ui.home.workoutgate.WorkoutSheetOpenRequest
import com.calai.bitecal.widget.BiteCalHomeWidgetUpdater
import com.calai.bitecal.widget.BiteCalWidgetSnapshotStore
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.math.RoundingMode
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.sqrt

enum class HomeTab { Home, Progress, Weight, Fasting, Workout, Personal }
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    vm: HomeViewModel,
    waterVm: WaterViewModel,
    workoutVm: WorkoutViewModel,
    weightVm: WeightViewModel,
    onOpenCamera: () -> Unit,
    onOpenSavedFoods: () -> Unit,
    onOpenTab: (HomeTab) -> Unit,
    onOpenFastingPlans: () -> Unit,
    onOpenActivityHistory: () -> Unit,
    fastingVm: FastingPlanViewModel,
    onOpenWeight: () -> Unit,
    onQuickLogWeight: () -> Unit,
    onOpenRecentUploadDetail: (foodLogId: String, previewUri: String?, timeText: String) -> Unit,
    canUseScan: Boolean = false,
    onOpenSubscription: () -> Unit = {},
    onCheckCanUseScan: suspend () -> Boolean = { canUseScan },
    onOpenWorkoutSubscription: () -> Unit = onOpenSubscription,
    onCheckCanUseWorkout: suspend () -> Boolean = { false },
    openWorkoutSheetRequestTick: Long = 0L,
    onConsumeOpenWorkoutSheetRequest: () -> Unit = {},
    onWorkoutSavedGoHome: () -> Unit = {},
) {
    val ui by vm.ui.collectAsState()
    val waterState by waterVm.ui.collectAsState()
    val recentUploads by vm.recentUploads.collectAsState()

    // ====== Fasting VM 狀態 / 權限設定 ======
    val fastingUi by fastingVm.state.collectAsState()
    // 首次進入 Home 就載入 DB（含 enabled/plan/time）
    LaunchedEffect(Unit) { fastingVm.load() }

    // === Weight UI（為了拿跟 SummaryCards 相同的 TO GOAL） ===
    val weightUi by weightVm.ui.collectAsState()

    // 確保 Weight summary 有被拉一次
    LaunchedEffect(Unit) {
        weightVm.initIfNeeded()
    }
    val stepsGoal by vm.dailyStepGoal.collectAsState()
    val workoutGoalKcal by vm.dailyWorkoutGoalKcal.collectAsState()
    val weightUnit = weightUi.unit

    // current：kg/lbs 都準備好（LBS 顯示與差值用 lbs；progress 仍用 kg）
    val currentKg  = weightUi.current ?: weightUi.profileWeightKg
    val currentLbs = weightUi.currentLbs ?: weightUi.profileWeightLbs

    // goal：Home 建議一律用 DB 真值（SummaryCards 也這樣做）
    val goalKg  = weightUi.goal      // DB goal_weight_kg
    val goalLbs = weightUi.goalLbs   // DB goal_weight_lbs



    // （可選）把 debug log 改成同時印 kg/lbs，才不會誤判
    LaunchedEffect(weightUnit, currentKg, currentLbs, goalKg, goalLbs) {
        Log.d(
            "weightDebug",
            String.format(
                Locale.US,
                "unit=%s currentKg=%.3f currentLbs=%s goalKg=%s goalLbs=%s",
                weightUnit,
                (currentKg ?: Double.NaN),
                (currentLbs?.let { String.format(Locale.US, "%.3f", it) } ?: "null"),
                (goalKg?.let { String.format(Locale.US, "%.3f", it) } ?: "null"),
                (goalLbs?.let { String.format(Locale.US, "%.3f", it) } ?: "null"),
            )
        )
    }

    val weightProgressKg: Float = computeWeightProgress(
        timeSeries = weightUi.series,
        currentKg = currentKg,
        goalKg = goalKg,
        profileWeightKg = weightUi.profileWeightKg
    ).fraction

    val weightProgress: Float = if (weightUnit == UserProfileStore.WeightUnit.KG) {
        weightProgressKg
    } else {
        computeWeightProgressFractionLbs(
            timeSeries = weightUi.series,
            currentLbs = currentLbs,
            goalLbs = goalLbs,
            profileWeightLbs = weightUi.profileWeightLbs
        ) ?: weightProgressKg
    }

    val weightPrimaryText = "${formatAchievedPercent1(weightProgress)} %"

    // ★ 新增：監聽 Workout VM 狀態（為了一次性導航）
    val workoutUi by workoutVm.ui.collectAsState()

    // ✅ 確保 Home 進入就有 today total（跟 History 一致）
    LaunchedEffect(Unit) {
        workoutVm.init()
        workoutVm.refreshToday()
    }
    val workoutSessionKcalToday: Int? = workoutUi.today?.totalKcalToday

    val stepsToday by vm.dailyStepsToday.collectAsState()
    val activeKcalToday by vm.dailyActiveKcalToday.collectAsState()
    val workoutTotalKcalToday = remember(workoutSessionKcalToday, activeKcalToday) {
        if (workoutSessionKcalToday == null && activeKcalToday == null) {
            null
        } else {
            (workoutSessionKcalToday ?: 0) + (activeKcalToday ?: 0)
        }
    }
    val dailyStatus by vm.dailyStatus.collectAsState()
    val dailyReady by vm.dailyReady.collectAsState()

    val ctx = LocalContext.current
    val timeFmt = remember { DateTimeFormatter.ofPattern("HH:mm") }

    // ⚠️ 關鍵：先拿 owner；可能為 null（某些 Nav/容器或 Preview）
    val registryOwner = LocalActivityResultRegistryOwner.current

    // === 這裡是新增的狀態：控制 bottom sheet (Workout Tracker) 是否顯示 ===
    val localeKey = currentLocaleKey()
    val showWorkoutSheet = rememberSaveable { mutableStateOf(false) }

    // 只有在 owner 存在時才建立 launcher，否則用 null 表示不用它
    val requestNotifications = if (registryOwner != null) {
        rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->
            if (granted) {
                // 允許後才真正開啟（VM 內會 persist & schedule）
                fastingVm.onToggleEnabled(
                    requested = true,
                    onNeedPermission = {},   // 已授權，不會再被叫到
                    onDenied = {}
                )
            }
        }
    } else null

    // ★ 監聽 App 回到前景（例如從系統設定頁返回），自動完成 pending 啟用與 DB 更新
    LifecycleResumeEffect(Unit) {
        // Activity/Fragment 進入 RESUMED 時會觸發這裡
        fastingVm.onAppResumed()
        onPauseOrDispose { /* no-op */ }
    }

    // ✅ 有成功/失敗訊息就先關掉 Host，避免 Toast 被 Sheet 擋住。
    LaunchedEffect(workoutUi.toastMessageResId) {
        if (workoutUi.toastMessageResId != null) {
            showWorkoutSheet.value = false
        }
    }

    // ✅ ResultContent / DurationPicker 保存成功後回到 Home，並保留 VM toast 狀態讓 Home 顯示成功吐司。
    LaunchedEffect(workoutUi.navigateHomeOnce) {
        if (workoutUi.navigateHomeOnce) {
            showWorkoutSheet.value = false
            onWorkoutSavedGoHome()
            workoutVm.consumeNavigateHome()
        }
    }

    // ✅ 防止快速離開 Home 時，LaunchedEffect 的 delay 被取消後 toast 狀態殘留。
    // 例如：成功 toast 還沒自動消失就進入 Settings / Weight / Camera，回來 Home 不應再次顯示舊 toast。
    DisposableEffect(Unit) {
        onDispose {
            workoutVm.clearToast()
            fastingVm.clearToast()
        }
    }

    // ✅ 建議：帶 key，避免 ctx / launcher 更新後仍用舊的
    val onToggleFasting: (Boolean) -> Unit = remember(ctx, requestNotifications, fastingVm) {
        { requested ->
            fastingVm.onToggleEnabled(
                requested = requested,
                onNeedPermission = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        if (NotificationPermission.isGranted(ctx)) {
                            fastingVm.onToggleEnabled(true, onNeedPermission = {}, onDenied = {})
                        } else {
                            requestNotifications?.launch(Manifest.permission.POST_NOTIFICATIONS)
                                ?: openAppNotificationSettings(ctx)
                        }
                    } else {
                        fastingVm.onToggleEnabled(true, onNeedPermission = {}, onDenied = {})
                    }
                },
                onDenied = {
                    // TODO: 你要的話可以丟 toast：「需要通知權限才能啟用提醒」
                }
            )
        }
    }

    val hcPermissions = remember {
        setOf(
            HealthPermission.getReadPermission(StepsRecord::class)
        )
    }

    // ✅ Health Connect 權限請求 launcher（官方：createRequestPermissionResultContract）:contentReference[oaicite:2]{index=2}
    val requestHealthConnectPerms =
        if (registryOwner != null) {
            rememberLauncherForActivityResult(
                contract = PermissionController.createRequestPermissionResultContract()
            ) { granted: Set<String> ->
                Log.e("HC_UI", "HC permission result granted=${granted.size} $granted")
                vm.refreshDailyActivity(force = true) // ✅ 授權後立刻更新，不吃 debounce
            }
        } else null

    var hcPromptedOnce by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(dailyStatus, registryOwner) {
        val canLaunch = requestHealthConnectPerms != null
        if (!hcPromptedOnce &&
            canLaunch &&
            dailyStatus == DailyActivityStatus.PERMISSION_NOT_GRANTED
        ) {
            hcPromptedOnce = true
            requestHealthConnectPerms.launch(hcPermissions)
        }
    }

    LaunchedEffect(dailyStatus) {
        if (dailyStatus == DailyActivityStatus.AVAILABLE_GRANTED) {
            HealthConnectPermissionPrefs.resetDeniedCount(ctx)
        }
    }

    val onStepsCardClick: () -> Unit = {
        when (dailyStatus) {
            DailyActivityStatus.PERMISSION_NOT_GRANTED -> {
                val deniedCount = HealthConnectPermissionPrefs.getDeniedCount(ctx)
                Log.d("HC_UI", "onStepsCardClick status=$dailyStatus deniedCount=$deniedCount sdkInt=${Build.VERSION.SDK_INT}")
                if (deniedCount >= 2) {
                    // ✅ 第三次（已拒絕兩次）→ 直接導設定頁
                    HealthConnectPermissionIntents.openHealthPermissionsSettings(ctx)
                } else {
                    // ✅ 第 1、2 次 → 彈 Health Connect 權限請求 UI（由 ProxyActivity 統一處理結果+計數）
                    HealthConnectPermissionProxyActivity.start(ctx, hcPermissions)
                }
            }

            DailyActivityStatus.ERROR_RETRYABLE -> vm.refreshDailyActivity(force = true)
            DailyActivityStatus.NO_DATA -> vm.onDailyCtaClick(ctx)
            DailyActivityStatus.HC_NOT_INSTALLED,
            DailyActivityStatus.HC_UNAVAILABLE -> vm.onDailyCtaClick(ctx)
            DailyActivityStatus.AVAILABLE_GRANTED -> Unit
        }
    }


    LifecycleResumeEffect(Unit) {
        vm.refreshDailyActivity()
        onPauseOrDispose { }
    }

    // ===== Camera permission gate for FAB =====
    val hasCameraPerm = remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.CAMERA) ==
                    PackageManager.PERMISSION_GRANTED
        )
    }

    val pendingOpenCamera by vm.pendingOpenCamera.collectAsState()
    val latestOnOpenCamera = rememberUpdatedState(onOpenCamera)

    var showQuickAddMenu by rememberSaveable { mutableStateOf(false) }
    var recentUploadDeleteTargetId by rememberSaveable { mutableStateOf<String?>(null) }
    var recentUploadDeleteRequested by rememberSaveable { mutableStateOf(false) }

    val scanFabScope = rememberCoroutineScope()
    val latestOnOpenSubscription = rememberUpdatedState(onOpenSubscription)
    val latestOnCheckCanUseScan = rememberUpdatedState(onCheckCanUseScan)
    val latestOnOpenWorkoutSubscription = rememberUpdatedState(onOpenWorkoutSubscription)
    val latestOnCheckCanUseWorkout = rememberUpdatedState(onCheckCanUseWorkout)
    val latestOnConsumeOpenWorkoutSheetRequest = rememberUpdatedState(onConsumeOpenWorkoutSheetRequest)

    var scanFabGateInFlight by rememberSaveable { mutableStateOf(false) }
    var workoutGateInFlight by rememberSaveable { mutableStateOf(false) }
    var showWorkoutGateError by rememberSaveable { mutableStateOf(false) }
    val workoutGateErrorMessage = stringResource(R.string.workout_membership_verify_failed)
    val workoutPremiumGate = remember {
        WorkoutPremiumGate {
            latestOnCheckCanUseWorkout.value.invoke()
        }
    }

    val onFabClick: () -> Unit = remember {
        {
            if (!scanFabGateInFlight) {
                scanFabScope.launch {
                    scanFabGateInFlight = true

                    val canOpenScanMenu = runCatching {
                        latestOnCheckCanUseScan.value.invoke()
                    }.getOrDefault(false)

                    scanFabGateInFlight = false

                    if (canOpenScanMenu) {
                        showQuickAddMenu = true
                    } else {
                        latestOnOpenSubscription.value.invoke()
                    }
                }
            }
        }
    }

    val onWorkoutAddClick: () -> Unit = remember(workoutPremiumGate) {
        {
            if (!workoutGateInFlight) {
                scanFabScope.launch {
                    workoutGateInFlight = true

                    when (workoutPremiumGate.check()) {
                        WorkoutPremiumGateDecision.OpenWorkout -> {
                            showWorkoutSheet.value = true
                        }
                        WorkoutPremiumGateDecision.OpenSubscription -> {
                            latestOnOpenWorkoutSubscription.value.invoke()
                        }
                        WorkoutPremiumGateDecision.VerificationFailed -> {
                            showWorkoutGateError = true
                        }
                        null -> Unit
                    }

                    workoutGateInFlight = false
                }
            }
        }
    }

    LaunchedEffect(openWorkoutSheetRequestTick) {
        if (WorkoutSheetOpenRequest.shouldOpen(openWorkoutSheetRequestTick)) {
            showWorkoutSheet.value = true
            latestOnConsumeOpenWorkoutSheetRequest.value.invoke()
        }
    }

    LaunchedEffect(workoutUi.subscriptionRequiredOnce) {
        if (workoutUi.subscriptionRequiredOnce) {
            showWorkoutSheet.value = false
            workoutVm.consumeSubscriptionRequired()
            latestOnOpenWorkoutSubscription.value.invoke()
        }
    }
    // 有 owner 才能用 launcher；沒有就 null（你已有 ProxyActivity 兜底）
    val requestCameraPermLauncher =
        if (registryOwner != null) {
            rememberLauncherForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { granted ->
                hasCameraPerm.value = granted

                if (granted) {
                    CameraPermissionPrefs.resetCameraDeniedCount(ctx)
                } else {
                    CameraPermissionPrefs.incrementCameraDeniedCount(ctx)
                    // ✅ 若使用者勾「不再詢問」：第二次起系統可能不會彈窗
                    // 這時直接導設定頁是唯一解
                    // （這段若你堅持第三次才導，可以移除，但 UX 會卡死）
                    // 這裡需要 Activity 才能 shouldShow...；HomeScreen 這裡 ctx 不一定是 Activity
                    // 所以我們不在這裡判斷 dontAskAgain，交給 ProxyActivity 或下一次點擊導頁即可
                }
            }
        } else null

    // App 回前景：使用者可能去設定頁手動開權限
    LifecycleResumeEffect(Unit) {
        val grantedNow = ContextCompat.checkSelfPermission(ctx, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        hasCameraPerm.value = grantedNow
        if (grantedNow) {
            CameraPermissionPrefs.resetCameraDeniedCount(ctx)
        }
        onPauseOrDispose { }
    }

    // ✅ 統一出口：只要「授權成功 + pending=true」就開相機一次
    LaunchedEffect(hasCameraPerm.value, pendingOpenCamera) {
        if (hasCameraPerm.value && pendingOpenCamera) {
            vm.clearPendingOpenCamera()
            latestOnOpenCamera.value.invoke()
        }
    }

    // 掃描食物：第 1、2 次都 request；第 3 次才導設定
    val onScanFoodClick: () -> Unit = remember(
        ctx,
        requestCameraPermLauncher,
        onOpenCamera,
        vm
    ) {
        {
            val deniedCount = CameraPermissionPrefs.getCameraDeniedCount(ctx)
            val grantedNow =
                ContextCompat.checkSelfPermission(ctx, Manifest.permission.CAMERA) ==
                        PackageManager.PERMISSION_GRANTED

            if (grantedNow) {
                CameraPermissionPrefs.resetCameraDeniedCount(ctx)
                onOpenCamera()
            } else {
                vm.markPendingOpenCamera()
                if (deniedCount >= 2) {
                    openCameraPermissionSettings(ctx)
                } else {
                    if (requestCameraPermLauncher != null) {
                        requestCameraPermLauncher.launch(Manifest.permission.CAMERA)
                    } else {
                        CameraPermissionProxyActivity.start(ctx)
                    }
                }
            }
        }
    }

    // ========= 「背景」改在這裡放一層即可 =========
    Box(Modifier.fillMaxSize()) {
        LightHomeBackground() // ← 背景
//        DarkHomeBackground();

        Scaffold(
            containerColor = Color.Transparent,   // ★ 讓下方漸層透出
            floatingActionButton = {
                ScanFab(onClick = onFabClick)
            },
            bottomBar = {
                MainBottomBar(
                    current = HomeTab.Home,
                    onOpenTab = { tab -> onOpenTab(tab) }
                )
            }
        ) { inner ->
            val s = ui.summary ?: return@Scaffold

            LaunchedEffect(s, ui.todayNutrition) {
                val widgetSnapshotChanged = BiteCalWidgetSnapshotStore.saveFrom(
                    context = ctx,
                    summary = s,
                    todayNutrition = ui.todayNutrition
                )
                if (widgetSnapshotChanged) {
                    BiteCalHomeWidgetUpdater.updateAll(ctx)
                }
            }

            val scrollState = rememberScrollState()

            var verticalScrollEnabled by remember { mutableStateOf(true) }

            val pagerGestureLockModifier = Modifier.pointerInput(Unit) {
                val slop = viewConfiguration.touchSlop

                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)

                    verticalScrollEnabled = true
                    var decided: Boolean? = null   // null=未決定; true=水平; false=垂直
                    var accX = 0f
                    var accY = 0f

                    while (true) {
                        val event = awaitPointerEvent(pass = PointerEventPass.Initial)
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (!change.pressed) break

                        val dx = change.position.x - change.previousPosition.x
                        val dy = change.position.y - change.previousPosition.y

                        if (decided == null) {
                            accX += dx
                            accY += dy

                            val dist = sqrt((accX * accX + accY * accY).toDouble()).toFloat()
                            if (dist > slop) {
                                val isHorizontal = abs(accX) > abs(accY)
                                decided = isHorizontal

                                // ✅ 水平：關掉外層 vertical scroll（Pager 會變超好滑）
                                verticalScrollEnabled = !isHorizontal
                            }
                        }
                    }
                    verticalScrollEnabled = true
                }
            }

            Column(
                modifier = Modifier
                    .padding(inner)
                    .fillMaxSize()
                    .verticalScroll(scrollState, enabled = verticalScrollEnabled)
                    .padding(horizontal = BiteCalScreenFrame.contentHorizontal)
            ) {
                // ===== Top bar: avatar + bell
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp, bottom = 0.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Avatar(
                        url = s.avatarUrl,
                        avatarSize = 42.dp,
                        touchSize = 48.dp,
                        startPadding = 5.dp,
                        onClick = null
                    )
                    TopBarSettingsButton(
                        onClick = { onOpenTab(HomeTab.Personal) },
                        modifier = Modifier.padding(end = 3.dp)
                    )
                }
                val today = LocalDate.now()
                val pastDays = 30
                val futureDays = 1
                val days =
                    remember(today) { (-pastDays..futureDays).map { today.plusDays(it.toLong()) } }
                val calendarCaloriesByDate = remember(ui.calendarNutritionByDate) {
                    ui.calendarNutritionByDate.mapValues { (_, nutrition) -> nutrition.eatenKcal }
                }
                CalendarStrip(
                    days = days,
                    selected = ui.selectedDate,
                    onSelect = vm::onCalendarDateSelected,
                    caloriesByDate = calendarCaloriesByDate,
                    dailyGoalKcal = s.tdee,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    selectedBgCorner = 16.dp   // ← 圓角更圓（原 8.dp）
                )

                // ★ 這兩個值就是你要調的數字（正數=上面減、下面加；負數相反）
                val topSwap =
                    16.dp    // 例：Calories -8dp；Macro +8dp  第一頁 Calories 變矮、Macro 變高（或相反），但整頁高度不變、不跳動。
                val bottomSwap =
                    8.dp    // 例：Workout -12dp；Weight/Fasting +12dp 第二頁 Workout 變矮、Weight/Fasting 變高（或相反），整頁高度不變。

                // ★ 兩邊總高度控制（共同升降）
                val baseHeight = 126.dp    // ← 每張卡的基準高度（兩張卡都用這個），改這裡就能拉高/降低總高度
                val verticalGap = 14.dp    // ← 上下卡的間距

                // 將 VM 狀態轉為卡片顯示字串
                val planName = fastingUi.selected.code
                val startText = fastingUi.start.format(timeFmt)
                val endText = fastingUi.end.format(timeFmt)

                TwoPagePager(
                    summary = s,
                    todayNutrition = ui.todayNutrition,
                    selectedDate = ui.selectedDate,
                    topSwap = topSwap,
                    bottomSwap = bottomSwap,
                    baseHeight = baseHeight,
                    verticalGap = verticalGap,
                    onOpenFastingPlans = onOpenFastingPlans,
                    // ★ 傳入 VM 狀態給 Home 卡片
                    planOverride = planName,
                    fastingStartText = startText,
                    fastingEndText = endText,
                    fastingEnabled = fastingUi.enabled,
                    onToggleFasting = onToggleFasting,
                    weightPrimary = weightPrimaryText,
                    weightProgress = weightProgress,
                    onOpenWeight = onOpenWeight,
                    onQuickLogWeight = onQuickLogWeight,
                    // ★ 傳進去給第二頁下半部喝水卡
                    waterState = waterState,
                    onWaterPlus = { waterVm.adjust(+1) },
                    onWaterMinus = { waterVm.adjust(-1) },
                    onToggleUnit = { waterVm.toggleUnit() },
                    modifier = pagerGestureLockModifier
                )

                Spacer(Modifier.height(5.dp))

                StepsWorkoutRowModern(
                    summary = s,
                    workoutTotalKcalOverride = workoutTotalKcalToday,
                    stepsOverride = stepsToday,
                    activeKcalOverride = activeKcalToday,
                    weightKgLatest = weightUi.current,
                    dailyStatus = dailyStatus,
                    dailyReady = dailyReady,
                    onDailyCtaClick = onStepsCardClick,
                    stepsGoalOverride = stepsGoal,
                    workoutGoalKcalOverride = workoutGoalKcal,
                    cardHeight = 104.dp,
                    ringSize = 74.dp,
                    centerDisk = 34.dp,
                    ringStroke = 5.dp,
                    onAddWorkoutClick = onWorkoutAddClick,
                    workoutAddEnabled = !workoutGateInFlight,
                    onWorkoutCardClick = { onOpenActivityHistory() }
                )
                // ===== Fourth block: 最近上傳
                val recentSectionTopGap = 18.dp
                val recentSectionTitleBottomGap = 16.dp
                val recentSectionTitleStart = 2.dp

                Spacer(Modifier.height(recentSectionTopGap))

                when {
                    recentUploads.isNotEmpty() -> {
                        Text(
                            text = stringResource(R.string.recently_uploaded),
                            style = TextStyle(
                                fontSize = 22.sp,
                                lineHeight = 30.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            modifier = Modifier.padding(
                                start = recentSectionTitleStart,
                                bottom = recentSectionTitleBottomGap
                            )
                        )

                        Column(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            recentUploads.forEachIndexed { index, item ->
                                RecentUploadCard(
                                    item = item,
                                    modifier = Modifier.fillMaxWidth(),
                                    onClick = {
                                        onOpenRecentUploadDetail(
                                            item.foodLogId,
                                            item.previewUri,
                                            item.timeText
                                        )
                                    },
                                    onDeleteClick = {
                                        recentUploadDeleteTargetId = item.foodLogId
                                    }
                                )
                                if (index != recentUploads.lastIndex) {
                                    Spacer(Modifier.height(20.dp))
                                }
                            }
                        }
                    }

                    else -> {
                        RecentlyUploadedEmptySection(
                            cardHeight = 120.dp,
                            titleStartPadding = recentSectionTitleStart,
                            titleBottomPadding = recentSectionTitleBottomGap,
                            titleFontSize = 22.sp,
                            lineHeight = 30.sp,
                            titleFontWeight = FontWeight.Bold
                        )
                    }
                }
                Spacer(Modifier.height(70.dp))
            }
        }
        DeleteFoodLogDialog(
            visible = recentUploadDeleteTargetId != null,
            onDismiss = {
                if (!recentUploadDeleteRequested) {
                    recentUploadDeleteTargetId = null
                }
            },
            onCancel = {
                if (!recentUploadDeleteRequested) {
                    recentUploadDeleteTargetId = null
                }
            },
            onDelete = {
                val targetId = recentUploadDeleteTargetId ?: return@DeleteFoodLogDialog
                if (recentUploadDeleteRequested) return@DeleteFoodLogDialog

                recentUploadDeleteRequested = true
                vm.deleteRecentUpload(
                    foodLogId = targetId,
                    onSuccess = {
                        recentUploadDeleteRequested = false
                        recentUploadDeleteTargetId = null
                    },
                    onFailure = {
                        recentUploadDeleteRequested = false
                    }
                )
            },
            deleting = recentUploadDeleteRequested
        )

        HomeQuickActionMenu(
            visible = showQuickAddMenu,
            onDismiss = { showQuickAddMenu = false },
            onSavedFoodsClick = {
                showQuickAddMenu = false
                onOpenSavedFoods()
            },
            onScanFoodClick = {
                showQuickAddMenu = false
                onScanFoodClick()
            }
        )

        // ===== ✅ Toast 疊加層（先顯示 Fasting，再顯示 Workout） =====
        val canShowWorkoutToast = !showWorkoutSheet.value
        val workoutToastResId = workoutUi.toastMessageResId
        val fastingToast = fastingUi.toastMessage   // ★ 來自 FastingPlanViewModel
        key(localeKey, workoutToastResId, fastingToast, showWorkoutGateError) {
            Box(Modifier.fillMaxSize()) {
                when {
                    // 1️⃣ 優先顯示 Fasting 儲存結果（不管 Workout 有沒有）
                    showWorkoutGateError -> {
                        ErrorTopToast(
                            message = workoutGateErrorMessage,
                            modifier = Modifier.align(Alignment.TopCenter)
                        )
                        LaunchedEffect(showWorkoutGateError) {
                            delay(2000)
                            showWorkoutGateError = false
                        }
                    }

                    fastingToast != null -> {
                        SuccessTopToast(
                            message = fastingToast,
                            modifier = Modifier.align(Alignment.TopCenter)
                        )
                        LaunchedEffect(fastingToast) {
                            delay(2000)
                            fastingVm.clearToast()   // ★ 呼叫剛剛加的 clearToast()
                        }
                    }

                    // 2️⃣ 沒有 Fasting toast 時，才顯示 Workout 的
                    canShowWorkoutToast && workoutToastResId != null -> {
                        SuccessTopToast(
                            message = stringResource(workoutToastResId),
                            modifier = Modifier.align(Alignment.TopCenter)
                        )
                        LaunchedEffect(workoutToastResId) {
                            delay(2000)
                            workoutVm.clearToast()
                        }
                    }
                }
            }
        }
        // ===== 共用 BottomSheet Host（常駐），以 visible 控制顯示 =====
        key(localeKey) {
            WorkoutTrackerHost(
                vm = workoutVm,
                visible = showWorkoutSheet.value,
                localeTag = localeKey,
                onCloseFull = { showWorkoutSheet.value = false },
                onCollapseOnly = { showWorkoutSheet.value = false }
            )
        }
    }
}

@Composable
private fun Avatar(
    url: Uri?,
    avatarSize: Dp = 40.dp,
    touchSize: Dp = 48.dp,
    startPadding: Dp = 0.dp,
    onClick: (() -> Unit)? = null,
    fallbackCircleSize: Dp = 43.dp,   // ✅ 對齊 Setting 的 visualSize
    fallbackIconSize: Dp = 43.dp      // ✅ spoon icon 大小
) {
    val interaction = remember { MutableInteractionSource() }

    Box(
        modifier = Modifier
            .padding(start = startPadding)
            .size(touchSize)
            .then(
                if (onClick != null) {
                    Modifier.biteCalClickable(
                        interactionSource = interaction,
                        indication = null,
                        role = Role.Button
                    ) { onClick() }
                } else {
                    Modifier
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        if (url == null) {
            Box(
                modifier = Modifier
                    .size(fallbackCircleSize)
                    .clip(CircleShape)
                    .background(Color.White)
                    .border(1.25.dp, Color(0xFFCDD5DF), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_focus_spoon_foreground),
                    contentDescription = "頭像",
                    modifier = Modifier.size(fallbackIconSize),
                    contentScale = ContentScale.Fit
                )
            }
        } else {
            val ctx = LocalContext.current
            val request = remember(url) {
                ImageRequest.Builder(ctx)
                    .data(url)
                    .crossfade(false)
                    .allowHardware(true)
                    .build()
            }

            AsyncImage(
                model = request,
                contentDescription = "頭像",
                modifier = Modifier
                    .size(avatarSize)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop,
                error = painterResource(R.drawable.profile)
            )
        }
    }
}

private fun uiSafeTodayNutritionValue(value: Int?): Int =
    (value ?: 0).coerceAtLeast(0)

private fun nutritionProgress(current: Int?, goal: Int?): Float {
    val c = (current ?: 0).coerceAtLeast(0)
    val g = (goal ?: 0).coerceAtLeast(0)
    if (g <= 0) return 0f
    return (c.toFloat() / g.toFloat()).coerceIn(0f, 1f)
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TwoPagePager(
    summary: HomeSummary,
    todayNutrition: HomeTodayNutritionSummary,
    selectedDate: LocalDate,
    modifier: Modifier = Modifier,
    topSwap: Dp = 0.dp,
    bottomSwap: Dp = 0.dp,
    baseHeight: Dp = PanelHeights.Metric,
    verticalGap: Dp = 14.dp,
    onOpenFastingPlans: () -> Unit = {},
    planOverride: String? = null,
    fastingStartText: String? = null,
    fastingEndText: String? = null,
    fastingEnabled: Boolean = false,
    onToggleFasting: (Boolean) -> Unit = {},
    weightPrimary: String,
    weightProgress: Float,
    onOpenWeight: () -> Unit,
    onQuickLogWeight: () -> Unit,
    waterState: WaterUiState,
    onWaterPlus: () -> Unit,
    onWaterMinus: () -> Unit,
    onToggleUnit: () -> Unit,
) {
    val pageCount = 3
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { pageCount })
    var showTodayNutritionProgress by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(selectedDate) {
        showTodayNutritionProgress = false
    }

    val toggleNutritionMode = { showTodayNutritionProgress = !showTodayNutritionProgress }

    val caloriesProgress = nutritionProgress(
        current = todayNutrition.eatenKcal,
        goal = summary.tdee
    )

    // 固定頁面總高度
    val spacerV = verticalGap
    val pageHeight = baseHeight + baseHeight + spacerV
    val minCard = 96.dp
    val maxSwap = (baseHeight - minCard).coerceAtLeast(0.dp)

    // 上下區塊對沖
    val topSwapClamped = topSwap.coerceIn(-maxSwap, maxSwap)
    val caloriesH = baseHeight - topSwapClamped
    val macroH = baseHeight + topSwapClamped

    val bottomSwapClamped = bottomSwap.coerceIn(-maxSwap, maxSwap)
    val workoutH = baseHeight - bottomSwapClamped
    val wfH = baseHeight + bottomSwapClamped

    Column {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .height(pageHeight)
                .then(modifier),
            pageSpacing = 38.dp,
            beyondViewportPageCount = 1
        ) { page ->
            Box(modifier = Modifier.fillMaxSize()) {
                Column(Modifier.fillMaxSize()) {
                    when (page) {
                        0 -> {
                            CaloriesCardModern(
                                goalKcal = summary.tdee,
                                eatenKcal = uiSafeTodayNutritionValue(todayNutrition.eatenKcal),
                                showTodayProgress = showTodayNutritionProgress,
                                onClick = toggleNutritionMode,
                                progress = caloriesProgress,
                                cardHeight = caloriesH,
                                ringSize = 82.dp,
                                centerDisk = 36.dp,
                                ringStroke = 6.dp,
                                valueFontSize = 38.sp,
                                labelFontSize = 12.sp,
                                fireIconSize = 22.dp
                            )
                            Spacer(Modifier.height(spacerV))

                            MacroRowModern(
                                s = summary,
                                todayNutrition = todayNutrition,
                                showTodayProgress = showTodayNutritionProgress,
                                onClick = toggleNutritionMode,
                                cardHeight = macroH,
                                valueFontSize = 15.sp,
                                labelFontSize = 12.sp,
                                ringSize = 58.dp,
                                centerDisk = 28.dp,
                                ringStroke = 5.dp,
                                spacingTop = 15.dp,
                                proteinIconSize = 22.dp,
                                carbsIconSize = 24.dp,
                                fatsIconSize = 19.dp
                            )
                        }

                        1 -> {
                            MicronutrientRowModern(
                                s = summary,
                                todayNutrition = todayNutrition,
                                showTodayProgress = showTodayNutritionProgress,
                                onClick = toggleNutritionMode,
                                cardHeight = macroH,
                                valueFontSize = 15.sp,
                                labelFontSize = 12.sp,
                                ringSize = 58.dp,
                                centerDisk = 28.dp,
                                ringStroke = 5.dp,
                                spacingTop = 15.dp
                            )

                            Spacer(Modifier.height(spacerV))

                            HealthScoreCardModern(
                                score = todayNutrition.avgHealthScore,
                                cardHeight = caloriesH
                            )
                        }

                        2 -> {
                            WeightFastingRowModern(
                                summary = summary,
                                cardHeight = wfH,
                                onOpenFastingPlans = onOpenFastingPlans,
                                planOverride = planOverride,
                                fastingStartText = fastingStartText,
                                fastingEndText = fastingEndText,
                                fastingEnabled = fastingEnabled,
                                onToggle = onToggleFasting,
                                weightPrimary = weightPrimary,
                                weightProgress = weightProgress,
                                onOpenWeight = onOpenWeight,
                                onQuickLogWeight = onQuickLogWeight
                            )

                            Spacer(Modifier.height(spacerV))

                            WaterIntakeCard(
                                cardHeight = workoutH,
                                state = waterState,
                                onPlus = onWaterPlus,
                                onMinus = onWaterMinus,
                                onToggleUnit = onToggleUnit
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            PagerDots(count = pageCount, current = pagerState.currentPage)
        }
    }
}

private fun openAppNotificationSettings(ctx: Context) {
    val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
        // 新舊 API 都照顧到
        putExtra(Settings.EXTRA_APP_PACKAGE, ctx.packageName)
        putExtra("android.provider.extra.APP_PACKAGE", ctx.packageName)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    ctx.startActivity(intent)
}

fun computeHomeWeightProgress(
    unit: UserProfileStore.WeightUnit,
    profileWeightKg: Double?,     // user_profiles.weight_kg（起始）
    profileWeightLbs: Double?,    // user_profiles.weight_lbs（起始）
    goalWeightKg: Double?,        // user_profiles.goal_weight_kg（目標）
    goalWeightLbs: Double?,       // user_profiles.goal_weight_lbs（目標）
    latestWeightKg: Double?,      // weight_timeseries 最新一筆 weight_kg
    latestWeightLbs: Double?      // weight_timeseries 最新一筆 weight_lbs
): Float {

    fun compute(start: Double?, goal: Double?, latest: Double?): Float {
        if (latest == null) return 0f
        if (start == null || goal == null) return 0f

        val denominator = goal - start
        if (denominator == 0.0) return 0f

        // 原始比例
        val raw = ((latest - start) / denominator).toFloat()

        // 先 clamp 0~1
        val clamped = raw.coerceIn(0f, 1f)

        // ✅ 轉成百分比後「無條件捨去」到整數%，再轉回 0~1
        val percentInt = floor((clamped * 100f).toDouble()).toInt() // 0..100
        return percentInt / 100f
    }

    return when (unit) {
        UserProfileStore.WeightUnit.KG -> {
            compute(
                start = profileWeightKg,
                goal = goalWeightKg,
                latest = latestWeightKg
            )
        }

        UserProfileStore.WeightUnit.LBS -> {
            val start = profileWeightLbs ?: profileWeightKg?.let { kgToLbs1(it) }
            val goal = goalWeightLbs ?: goalWeightKg?.let { kgToLbs1(it) }
            val latest = latestWeightLbs ?: latestWeightKg?.let { kgToLbs1(it) }
            compute(start = start, goal = goal, latest = latest)
        }
    }
}

private fun roundFirstNumberToIntText(input: String): String {
    // 支援：+ - Unicode minus(−) en-dash(–) em-dash(—)，以及 12.3 / 12,3
    val regex = Regex("""[+\-−–—]?\d+(?:[.,]\d+)?""")
    val m = regex.find(input) ?: return input

    val raw = m.value

    // 先把各種「負號」統一成 '-'
    val normalized = raw
        .replace('−', '-')
        .replace('–', '-')
        .replace('—', '-')
        .replace(',', '.') // 支援歐洲小數逗號

    val bd = normalized.toBigDecimalOrNull() ?: return input

    // ✅ 四捨五入到整數（HALF_UP）
    val roundedInt = bd.setScale(0, RoundingMode.HALF_UP).toInt()

    // 保留正號（如果原字串有 '+' 而且結果 > 0）
    val keepPlus = raw.startsWith('+') && roundedInt > 0
    val replaced = (if (keepPlus) "+$roundedInt" else roundedInt.toString())

    return input.replaceRange(m.range, replaced)
}
@Composable
private fun TopBarSettingsButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    touchSize: Dp = 50.dp,
    visualSize: Dp = 43.dp,
    iconSize: Dp = 30.dp
) {
    val bg = Color(0xFFE5E7EB)
    val fg = Color(0xFF979DA7)

    Box(
        modifier = modifier.size(touchSize),     // ✅ 48dp 熱區
        contentAlignment = Alignment.Center
    ) {
        FilledTonalIconButton(
            onClick = rememberClickWithHaptic(onClick = onClick),
            modifier = Modifier.size(visualSize), // ✅ 40dp 視覺圓
            colors = IconButtonDefaults.filledTonalIconButtonColors(
                containerColor = bg,
                contentColor = fg
            )
        ) {
            Icon(
                imageVector = Icons.Filled.Settings,
                contentDescription = "Settings Icon",
                modifier = Modifier.size(iconSize)
            )
        }
    }
}

internal fun formatAchievedPercent1(progress: Float): String {
    return String.format(Locale.US, "%.1f", progress * 100f)
}
