package com.calai.app.ui.home

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.health.connect.client.PermissionController
import androidx.lifecycle.compose.LifecycleResumeEffect
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.calai.app.R
import com.calai.app.data.fasting.notifications.NotificationPermission
import com.calai.app.data.home.repo.HomeSummary
import com.calai.app.data.profile.repo.UserProfileStore
import com.calai.app.data.profile.repo.kgToLbs1
import com.calai.app.ui.home.components.CalendarStrip
import com.calai.app.ui.home.components.CaloriesCardModern
import com.calai.app.ui.home.components.LightHomeBackground
import com.calai.app.ui.home.components.MacroRowModern
import com.calai.app.ui.home.components.MealCard
import com.calai.app.ui.home.components.PagerDots
import com.calai.app.ui.home.components.PanelHeights
import com.calai.app.ui.home.components.StepsWorkoutRowModern
import com.calai.app.ui.home.components.WeightFastingRowModern
import com.calai.app.ui.home.model.HomeViewModel
import com.calai.app.ui.home.ui.components.MainBottomBar
import com.calai.app.ui.home.ui.components.ScanFab
import com.calai.app.ui.home.ui.components.SuccessTopToast
import com.calai.app.ui.home.ui.fasting.model.FastingPlanViewModel
import com.calai.app.ui.home.ui.water.components.WaterIntakeCard
import com.calai.app.ui.home.ui.water.model.WaterUiState
import com.calai.app.ui.home.ui.water.model.WaterViewModel
import com.calai.app.ui.home.ui.weight.components.formatDeltaGoalMinusCurrentFromDb
import com.calai.app.ui.home.ui.weight.model.WeightViewModel
import com.calai.app.ui.home.ui.workout.WorkoutTrackerHost
import com.calai.app.ui.home.ui.workout.model.WorkoutViewModel
import kotlinx.coroutines.delay
import java.math.RoundingMode
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.sqrt
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.StepsRecord
import com.calai.app.data.activity.healthconnect.HealthConnectPermissionProxyActivity
import com.calai.app.data.activity.model.DailyActivityStatus
import com.calai.app.ui.home.components.RecentlyUploadedEmptySection
import androidx.compose.ui.semantics.Role

enum class HomeTab { Home, Progress, Weight, Fasting, Workout, Personal }
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    vm: HomeViewModel,
    waterVm: WaterViewModel,
    workoutVm: WorkoutViewModel,
    weightVm: WeightViewModel,
    onOpenCamera: () -> Unit,
    onOpenTab: (HomeTab) -> Unit,
    onOpenFastingPlans: () -> Unit,
    onOpenActivityHistory: () -> Unit,
    fastingVm: FastingPlanViewModel,
    // ★ 新增這兩個參數
    onOpenWeight: () -> Unit,
    onQuickLogWeight: () -> Unit
) {
    val ui by vm.ui.collectAsState()
    val waterState by waterVm.ui.collectAsState()

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
    val weightUnit = weightUi.unit

    // current：kg/lbs 都準備好（LBS 顯示與差值用 lbs；progress 仍用 kg）
    val currentKg  = weightUi.current ?: weightUi.profileWeightKg
    val currentLbs = weightUi.currentLbs ?: weightUi.profileWeightLbs

    // goal：Home 建議一律用 DB 真值（SummaryCards 也這樣做）
    val goalKg  = weightUi.goal      // DB goal_weight_kg
    val goalLbs = weightUi.goalLbs   // DB goal_weight_lbs

    val weightPrimaryTextRaw = formatDeltaGoalMinusCurrentFromDb(
        goalKg = goalKg,
        goalLbs = goalLbs,
        currentKg = currentKg,
        currentLbs = currentLbs,
        unit = weightUnit,
        lbsAsInt = false // 保持原本邏輯，最後再統一 floor
    )
    val weightPrimaryText = roundFirstNumberToIntText(weightPrimaryTextRaw)

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

    val weightProgress: Float = computeHomeWeightProgress(
        unit = weightUnit,
        profileWeightKg = weightUi.profileWeightKg,
        profileWeightLbs = weightUi.profileWeightLbs,
        goalWeightKg = goalKg,                 // ✅ 你上面已經定義：DB goal_weight_kg
        goalWeightLbs = goalLbs,               // ✅ 你上面已經定義：DB goal_weight_lbs
        latestWeightKg = weightUi.current,     // 最新 timeseries kg
        latestWeightLbs = weightUi.currentLbs  // ✅ 最新 timeseries lbs（DB）
    )

    // ★ 新增：監聽 Workout VM 狀態（為了一次性導航）
    val workoutUi by workoutVm.ui.collectAsState()

    // ✅ 確保 Home 進入就有 today total（跟 History 一致）
    LaunchedEffect(Unit) {
        workoutVm.init()
        workoutVm.refreshToday()
    }
    val workoutTotalKcalToday: Int? = workoutUi.today?.totalKcalToday

    val stepsToday by vm.dailyStepsToday.collectAsState()
    val activeKcalToday by vm.dailyActiveKcalToday.collectAsState()
    val dailyStatus by vm.dailyStatus.collectAsState()

    val ctx = LocalContext.current
    val timeFmt = remember { DateTimeFormatter.ofPattern("HH:mm") }

    // ⚠️ 關鍵：先拿 owner；可能為 null（某些 Nav/容器或 Preview）
    val registryOwner = LocalActivityResultRegistryOwner.current

    // === 這裡是新增的狀態：控制 bottom sheet (Workout Tracker) 是否顯示 ===
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

    // ✅ 有成功訊息就關掉 Host（停留在 HOME）
    LaunchedEffect(workoutUi.toastMessage) {
        if (workoutUi.toastMessage != null) {
            showWorkoutSheet.value = false
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

    val onStepsCardClick: () -> Unit = {
        when (dailyStatus) {
            DailyActivityStatus.PERMISSION_NOT_GRANTED -> {
                HealthConnectPermissionProxyActivity.start(ctx, hcPermissions)
            }
            DailyActivityStatus.ERROR_RETRYABLE -> vm.refreshDailyActivity(force = true) // ✅ 手動重試
            DailyActivityStatus.NO_DATA -> vm.refreshDailyActivity(force = true)         // ✅ 手動重抓
            DailyActivityStatus.HC_NOT_INSTALLED,
            DailyActivityStatus.HC_UNAVAILABLE -> vm.onDailyCtaClick(ctx)
            DailyActivityStatus.AVAILABLE_GRANTED -> Unit
        }
    }

    LifecycleResumeEffect(Unit) {
        vm.refreshDailyActivity()
        onPauseOrDispose { }
    }

    // ========= 「背景」改在這裡放一層即可 =========
    Box(Modifier.fillMaxSize()) {
        LightHomeBackground() // ← 背景
//        DarkHomeBackground();

        Scaffold(
            containerColor = Color.Transparent,   // ★ 讓下方漸層透出
            floatingActionButton = { ScanFab(onClick = onOpenCamera) },
            bottomBar = {
                MainBottomBar(
                    current = HomeTab.Home,
                    onOpenTab = { tab -> onOpenTab(tab) }
                )
            }
        ) { inner ->
            val s = ui.summary ?: return@Scaffold

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
                    .padding(horizontal = 20.dp)
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
                        modifier = Modifier.padding(end = 5.dp)
                    )
                }
                val today = LocalDate.now()
                val pastDays = 20
                val futureDays = 1   // 若不想顯示未來任何一天，改成 0
                val days =
                    remember(today) { (-pastDays..futureDays).map { today.plusDays(it.toLong()) } }
                var selected by rememberSaveable(today) { mutableStateOf(today) }
                CalendarStrip(
                    days = days,
                    selected = selected,
                    onSelect = { selected = it },
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
                val baseHeight = 128.dp    // ← 每張卡的基準高度（兩張卡都用這個），改這裡就能拉高/降低總高度
                val verticalGap = 10.dp    // ← 上下卡的間距

                // 將 VM 狀態轉為卡片顯示字串
                val planName = fastingUi.selected.code
                val startText = fastingUi.start.format(timeFmt)
                val endText = fastingUi.end.format(timeFmt)

                TwoPagePager(
                    summary = s,
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
                    onDailyCtaClick = onStepsCardClick,
                    stepsGoalOverride = stepsGoal,
                    cardHeight = 104.dp,
                    ringSize = 74.dp,
                    centerDisk = 38.dp,
                    ringStroke = 6.dp,
                    onAddWorkoutClick = { showWorkoutSheet.value = true },
                    onWorkoutCardClick = { onOpenActivityHistory() }
                )
                // ===== Fourth block: 最近上傳
                Spacer(Modifier.height(24.dp))

                if (s.recentMeals.isEmpty()) {
                    // ✅ 空狀態：跟截圖一樣
                    RecentlyUploadedEmptySection(
                        cardHeight = 100.dp // 你想更大就改這裡
                    )
                } else {
                    // ✅ 有資料：沿用你原本的列表（先不動）
                    Text(
                        text = stringResource(R.string.recently_uploaded),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    for (m in s.recentMeals) {
                        MealCard(m)
                        Spacer(Modifier.height(12.dp))
                    }
                }
                Spacer(Modifier.height(70.dp))
            }
        }
        // ===== ✅ Toast 疊加層（先顯示 Fasting，再顯示 Workout） =====
        val canShowWorkoutToast = !showWorkoutSheet.value
        val workoutToast = workoutUi.toastMessage
        val fastingToast = fastingUi.toastMessage   // ★ 來自 FastingPlanViewModel
        Box(Modifier.fillMaxSize()) {
            when {
                // 1️⃣ 優先顯示 Fasting 儲存結果（不管 Workout 有沒有）
                fastingToast != null -> {
                    SuccessTopToast(
                        message = fastingToast,
                        modifier = Modifier.align(Alignment.TopCenter),
                        minWidth = 150.dp,
                        minHeight = 30.dp
                    )
                    LaunchedEffect(fastingToast) {
                        delay(2000)
                        fastingVm.clearToast()   // ★ 呼叫剛剛加的 clearToast()
                    }
                }

                // 2️⃣ 沒有 Fasting toast 時，才顯示 Workout 的
                canShowWorkoutToast && workoutToast != null -> {
                    SuccessTopToast(
                        message = workoutToast,
                        modifier = Modifier.align(Alignment.TopCenter),
                        minWidth = 240.dp,
                        minHeight = 30.dp
                    )
                    LaunchedEffect(workoutToast) {
                        delay(2000)
                        workoutVm.clearToast()
                    }
                }
            }
        }
        // ===== 共用 BottomSheet Host（常駐），以 visible 控制顯示 =====
        WorkoutTrackerHost(
            vm = workoutVm,
            visible = showWorkoutSheet.value,
            onCloseFull = { showWorkoutSheet.value = false },
            onCollapseOnly = { showWorkoutSheet.value = false }
        )
    }
}

@Composable
private fun Avatar(
    url: Uri?,
    avatarSize: Dp = 40.dp,
    touchSize: Dp = 48.dp,
    startPadding: Dp = 0.dp,
    onClick: (() -> Unit)? = null, // ✅ NEW
) {
    val interaction = remember { MutableInteractionSource() }

    Box(
        modifier = Modifier
            .padding(start = startPadding)
            .size(touchSize)
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = interaction,
                        indication = null,
                        role = Role.Button
                    ) { onClick() }
                } else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        val avatarModifier = Modifier
            .size(avatarSize)
            .clip(CircleShape)

        if (url == null) {
            Image(
                painter = painterResource(R.drawable.profile),
                contentDescription = "avatar",
                modifier = avatarModifier,
                contentScale = ContentScale.Crop
            )
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
                contentDescription = "avatar",
                modifier = avatarModifier,
                contentScale = ContentScale.Crop,
                error = painterResource(R.drawable.profile)
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TwoPagePager(
    summary: HomeSummary,
    modifier: Modifier = Modifier,
    topSwap: Dp = 0.dp,
    bottomSwap: Dp = 0.dp,
    baseHeight: Dp = PanelHeights.Metric,
    verticalGap: Dp = 10.dp,
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
    val pageCount = 2
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { pageCount })

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
            // ★ 外層留白 + 陰影強化分頁感
            Box(modifier = Modifier.fillMaxSize())
            {
                Column(Modifier.fillMaxSize()) {
                    when (page) {
                        0 -> {
                            CaloriesCardModern(
                                caloriesLeft = summary.tdee,
                                progress = 0f,
                                cardHeight = caloriesH,
                                ringSize = 76.dp,
                                centerDisk = 38.dp,
                                ringStroke = 6.dp
                            )
                            Spacer(Modifier.height(spacerV))
                            MacroRowModern(
                                s = summary,
                                cardHeight = macroH
                            )
                        }

                        1 -> {
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

                            // ★ 取代原本的 ExerciseDiaryCard
                            WaterIntakeCard(
                                cardHeight = workoutH,
                                state = waterState,
                                onPlus = onWaterPlus,
                                onMinus = onWaterMinus,
                                onToggleUnit = onToggleUnit // ← 用 switch 切 ml/oz
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        // 分頁圓點
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
    val bg = Color(0xFFE4E7EA)
    val fg = Color(0xFF858C98)

    Box(
        modifier = modifier.size(touchSize),     // ✅ 48dp 熱區
        contentAlignment = Alignment.Center
    ) {
        FilledTonalIconButton(
            onClick = onClick,
            modifier = Modifier.size(visualSize), // ✅ 40dp 視覺圓
            colors = IconButtonDefaults.filledTonalIconButtonColors(
                containerColor = bg,
                contentColor = fg
            )
        ) {
            Icon(
                imageVector = Icons.Filled.Settings,
                contentDescription = "settings",
                modifier = Modifier.size(iconSize)
            )
        }
    }
}

