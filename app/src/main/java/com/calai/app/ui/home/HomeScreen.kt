package com.calai.app.ui.home

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
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
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SheetState
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
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
import androidx.lifecycle.compose.LifecycleResumeEffect
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.calai.app.R
import com.calai.app.data.fasting.notifications.NotificationPermission
import com.calai.app.data.home.repo.HomeSummary
import com.calai.app.data.profile.repo.UserProfileStore
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
import com.calai.app.ui.home.ui.weight.components.formatDeltaGoalMinusCurrent
import com.calai.app.ui.home.ui.weight.model.WeightViewModel
import com.calai.app.ui.home.ui.workout.WorkoutTrackerHost
import com.calai.app.ui.home.ui.workout.model.WorkoutViewModel
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.abs
import kotlin.math.sqrt


enum class HomeTab { Home, Progress, Workout, Fasting, Personal }
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    vm: HomeViewModel,
    waterVm: WaterViewModel,
    workoutVm: WorkoutViewModel,
    weightVm: WeightViewModel,
    onOpenAlarm: () -> Unit,
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

    // === Weight UI（為了拿跟 SummaryCards 相同的 TO TARGET） ===
    val weightUi by weightVm.ui.collectAsState()

    // 確保 Weight summary 有被拉一次
    LaunchedEffect(Unit) {
        weightVm.initIfNeeded()
    }

    // === Weight UI（為了拿跟 SummaryCards 相同的 TO TARGET） ===
    val weightUnit = weightUi.unit
    val effectiveCurrentKg = weightUi.current ?: weightUi.profileWeightKg
    val effectiveGoalKg = weightUi.profileTargetWeightKg ?: weightUi.goal

    // ★ 修改這裡：固定用小數點一位（KG 本來就一位；LBS 原本是整數）
    val weightPrimaryText = formatDeltaGoalMinusCurrent(
        goalKg = effectiveGoalKg,
        currentKg = effectiveCurrentKg,
        unit = weightUnit,
        lbsAsInt = (weightUnit == UserProfileStore.WeightUnit.LBS)          // ← 原本是 (weightUnit == UserProfileStore.WeightUnit.LBS)
    )

    // ✅ 新：Home WeightCardNew 進度 = (latest - start)/(target - start)
    val weightProgress: Float = computeHomeWeightProgress(
        profileWeightKg = weightUi.profileWeightKg,                 // start = user_profiles.weight_kg
        targetWeightKg = weightUi.profileTargetWeightKg ?: weightUi.goal, // target
        latestWeightKg = weightUi.current                          // latest = 最新 timeseries
    )

    // ★ 新增：監聽 Workout VM 狀態（為了一次性導航）
    val workoutUi by workoutVm.ui.collectAsState()

    val ctx = LocalContext.current
    val timeFmt = remember { DateTimeFormatter.ofPattern("HH:mm") }

    // ⚠️ 關鍵：先拿 owner；可能為 null（某些 Nav/容器或 Preview）
    val registryOwner = LocalActivityResultRegistryOwner.current

    // === 這裡是新增的狀態：控制 bottom sheet (Workout Tracker) 是否顯示 ===
    var showWorkoutSheet by rememberSaveable { mutableStateOf(false) }

    // ★ 共用 BottomSheetState（僅建立一次）
    val workoutSheetState: SheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )

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
            showWorkoutSheet = false
        }
    }

    // Switch 行為：一律讓 VM 做權限判斷；onNeedPermission 內採用「能 launcher 就 launcher；否則導到設定頁」
    val onToggleFasting: (Boolean) -> Unit = remember {
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
                    // 可選：顯示提示；不加也不會影響穩定性
                }
            )
        }
    }
    // ========= 「背景」改在這裡放一層即可 =========
    Box(Modifier.fillMaxSize()) {
        LightHomeBackground() // ← 背景
//        DarkHomeBackground();
    }
    Scaffold(
        containerColor = Color.Transparent,   // ★ 讓下方漸層透出
        floatingActionButton = { ScanFab(onClick = onOpenCamera) },
        bottomBar = {
            MainBottomBar(
                current = HomeTab.Home,
                onOpenTab = { tab ->
                    when (tab) {
                        HomeTab.Workout -> {
                            onOpenTab(HomeTab.Workout)
                        }

                        else -> onOpenTab(tab)
                    }
                }
            )
        }
    ) { inner ->
        val s = ui.summary ?: return@Scaffold

        val scrollState = rememberScrollState()

// ✅ Pager 水平滑動時，暫停外層垂直捲動，避免搶手勢
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

                // 手指放開，恢復垂直捲動
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
                Avatar(s.avatarUrl, size = 48.dp, startPadding = 6.dp)  // 放大＋往右一些
                IconButton(
                    onClick = onOpenAlarm,
                    modifier = Modifier.padding(end = 4.dp)   // 👈 往左移一點（離右邊邊界遠一點）
                ) {
                    Icon(
                        painter = painterResource(R.drawable.home_notification),
                        contentDescription = "alarm",
                        modifier = Modifier.size(32.dp)       // 👈 比原本 28.dp 再大一點
                    )
                }
            }

            val today = remember { LocalDate.now() }
            val pastDays = 20
            val futureDays = 1   // 若不想顯示未來任何一天，改成 0
            val days =
                remember(today) { (-pastDays..futureDays).map { today.plusDays(it.toLong()) } }
            var selected by rememberSaveable { mutableStateOf(LocalDate.now()) }
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
                16.dp        // 例：Calories -8dp；Macro +8dp  第一頁 Calories 變矮、Macro 變高（或相反），但整頁高度不變、不跳動。
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
                // ★ 新增：直接傳 SummaryCards 的 TO TARGET 字串
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

            // ★ 想再小就把 cardHeight 調更小，環也可一起調
            StepsWorkoutRowModern(
                summary = s,
                cardHeight = 112.dp,   // ← 你想要的高度
                ringSize = 74.dp,      // ← 對應縮小的圓環
                centerDisk = 36.dp,    // ← 對應縮小的中心灰圓
                ringStroke = 6.dp,      // ← 視覺厚度；想更輕可 7.dp
                onAddWorkoutClick = { showWorkoutSheet = true },
                onWorkoutCardClick = { onOpenActivityHistory() }   // ★ 新增：點整張卡 → 歷史頁
            )
            // ===== Fourth block: 最近上傳
            if (s.recentMeals.isNotEmpty()) {
                Spacer(Modifier.height(20.dp))
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
            Spacer(Modifier.height(80.dp))
        }
    }
    // ===== ✅ Toast 疊加層（先顯示 Fasting，再顯示 Workout） =====
    val sheetFullyHidden by remember {
        derivedStateOf { workoutSheetState.currentValue == SheetValue.Hidden }
    }

    val canShowWorkoutToast = !showWorkoutSheet || sheetFullyHidden
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
        sheetState = workoutSheetState,
        visible = showWorkoutSheet,
        onCloseFull = {            // 完整關閉：收合 + 清 VM
            showWorkoutSheet = false
            workoutVm.dismissDialogs()
        },
        onCollapseOnly = {         // 只收合：不清 VM（供 onClickPresetPlus 使用）
            showWorkoutSheet = false
            // 切記不要呼叫 workoutVm.dismissDialogs()
        }
    )
}

@Composable
private fun Avatar(
    url: Uri?,
    size: Dp = 40.dp,
    startPadding: Dp = 0.dp
) {
    val modifier = Modifier
        .padding(start = startPadding) // ← 新增：整體往右一點
        .size(size)                    // ← 新增：支援放大
        .clip(CircleShape)

    if (url == null) {
        Image(
            painter = painterResource(R.drawable.profile),
            contentDescription = "avatar_default",
            modifier = modifier,
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
            modifier = modifier,
            contentScale = ContentScale.Crop,
            error = painterResource(R.drawable.profile)
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TwoPagePager(
    summary: HomeSummary,
    topSwap: Dp = 0.dp,
    bottomSwap: Dp = 0.dp,
    baseHeight: Dp = PanelHeights.Metric,
    verticalGap: Dp = 10.dp,
    onOpenFastingPlans: () -> Unit = {},
    // Fasting 卡片資料
    planOverride: String? = null,
    fastingStartText: String? = null,
    fastingEndText: String? = null,
    fastingEnabled: Boolean = false,
    onToggleFasting: (Boolean) -> Unit = {},
    // ★ 新增：直接吃 Weight 畫面算好的 TO TARGET 文案
    weightPrimary: String,
    weightProgress: Float,
    // ★ 新增：Weight 導航事件
    onOpenWeight: () -> Unit,
    onQuickLogWeight: () -> Unit,
    // ★ 新增：喝水卡需要的資料與 callback
    waterState: WaterUiState,
    onWaterPlus: () -> Unit,
    onWaterMinus: () -> Unit,
    onToggleUnit: () -> Unit,
    modifier: Modifier = Modifier
) {
    val pageCount = 2
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { pageCount })

    // 固定頁面總高度
    val spacerV = verticalGap
    val pageHeight = baseHeight * 2 + spacerV
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
    profileWeightKg: Double?,     // user_profiles.weight_kg（起始）
    targetWeightKg: Double?,      // user_profiles.target_weight_kg（目標）
    latestWeightKg: Double?       // weight_timeseries 最新一筆 weight_kg
): Float {
    // 情境3：沒有 timeseries → 最新體重 = null → 進度 0%
    if (latestWeightKg == null) return 0f
    if (profileWeightKg == null || targetWeightKg == null) return 0f

    val start = profileWeightKg
    val target = targetWeightKg
    val latest = latestWeightKg

    val denominator = target - start
    if (denominator == 0.0) return 0f      // 起始 = 目標 → 避免除以 0

    // ✅ 已完成比例： (latest - start) / (target - start)
    val raw = ((latest - start) / denominator).toFloat()

    // clamp 到 0~1，避免超過目標或資料錯誤
    return raw.coerceIn(0f, 1f)
}
