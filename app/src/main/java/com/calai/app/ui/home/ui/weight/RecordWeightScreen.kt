package com.calai.app.ui.home.ui.weight

import android.content.Context
import android.content.ContextWrapper
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultRegistryOwner
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material3.*
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.calai.app.R
import com.calai.app.data.profile.repo.UserProfileStore
import com.calai.app.data.profile.repo.kgToLbs1
import com.calai.app.data.profile.repo.lbsToKg1
import com.calai.app.ui.home.ui.weight.components.WeightTopBar
import com.calai.app.ui.home.ui.weight.model.WeightViewModel
import java.io.File
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.Month
import java.time.Year
import java.time.format.DateTimeFormatter
import java.util.Locale

/* =========================================================
 * 外層 wrapper：負責找 ActivityResultRegistryOwner 並塞進 Local
 * ========================================================= */

@Composable
fun RecordWeightScreen(
    vm: WeightViewModel,
    onBack: () -> Unit,
    onSaved: () -> Unit,
    rowHeight: Dp = 56.dp
) {
    val outerContext = LocalContext.current
    val isPreview = LocalInspectionMode.current

    // 1. 先看目前 CompositionLocal 裡有沒有 owner
    val localOwner = LocalActivityResultRegistryOwner.current
    // 2. 再從 Context 鏈一路往外找 ActivityResultRegistryOwner（例如 ComponentActivity）
    val ownerFromContext = remember(outerContext) {
        outerContext.findActivityResultRegistryOwner()
    }

    val effectiveOwner = localOwner ?: ownerFromContext
    val canUseActivityResult = !isPreview && effectiveOwner != null

    Log.d(
        "RecordWeightScreen",
        "wrapper localOwner=$localOwner, ctxOwner=$ownerFromContext, " +
                "canUseActivityResult=$canUseActivityResult, isPreview=$isPreview"
    )

    if (effectiveOwner != null && localOwner == null) {
        // 情境：Local 是 null，但 Context 其實有 owner → 幫你補一個
        CompositionLocalProvider(LocalActivityResultRegistryOwner provides effectiveOwner) {
            RecordWeightScreenContent(
                vm = vm,
                onBack = onBack,
                onSaved = onSaved,
                rowHeight = rowHeight,
                canUseActivityResult = canUseActivityResult
            )
        }
    } else {
        // 一般情況：Local 已經有 owner 或完全沒有 owner
        RecordWeightScreenContent(
            vm = vm,
            onBack = onBack,
            onSaved = onSaved,
            rowHeight = rowHeight,
            canUseActivityResult = canUseActivityResult
        )
    }
}

/* =========================================================
 * 內層真正的畫面：只看 canUseActivityResult，不再碰 RegistryOwner
 * ========================================================= */

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun RecordWeightScreenContent(
    vm: WeightViewModel,
    onBack: () -> Unit,
    onSaved: () -> Unit,
    rowHeight: Dp,
    canUseActivityResult: Boolean
) {
    val ui by vm.ui.collectAsState()

    // 1) KG / LBS 範圍
    val KG_MIN = 20.0
    val KG_MAX = 800.0

    val LBS_TENTHS_MIN = kgToLbsTenthsRecord(KG_MIN)
    val LBS_TENTHS_MAX = kgToLbsTenthsRecord(KG_MAX)
    val LBS_INT_MIN = LBS_TENTHS_MIN / 10
    val LBS_INT_MAX = LBS_TENTHS_MAX / 10

    // 2) 日期狀態（預設今天）＋顯示格式 (2025/11/21)
    val today = remember { LocalDate.now() }
    var selectedDate by rememberSaveable { mutableStateOf(today) }
    val dateFormatterDisplay = remember {
        DateTimeFormatter.ofPattern("yyyy/MM/dd")
    }
    var showDateSheet by remember { mutableStateOf(false) }

    // 3) 單位：沿用 WeightScreen 的 unit
    var useMetric by rememberSaveable(ui.unit) {
        mutableStateOf(
            when (ui.unit) {
                UserProfileStore.WeightUnit.KG -> true
                UserProfileStore.WeightUnit.LBS -> false
            }
        )
    }

    // 4) 初始體重：current > profileWeightKg > 65kg
    val initialKgRaw = ui.current ?: ui.profileWeightKg ?: 65.0
    val initialKg = initialKgRaw.coerceIn(KG_MIN, KG_MAX)
    val initialLbsTenths = kgToLbsTenthsRecord(initialKg)
        .coerceIn(LBS_TENTHS_MIN, LBS_TENTHS_MAX)

    var valueKg by remember(ui.current, ui.profileWeightKg) { mutableStateOf(initialKg) }
    var valueLbsTenths by remember(ui.current, ui.profileWeightKg) {
        mutableStateOf(initialLbsTenths)
    }

    // 5) 從 value 推 wheel 選中值
    val kgTenths = (valueKg * 10.0).toInt()
        .coerceIn((KG_MIN * 10).toInt(), (KG_MAX * 10).toInt())
    val kgIntSel = kgTenths / 10
    val kgDecSel = kgTenths % 10

    val lbsTenthsClamped = valueLbsTenths
        .coerceIn(LBS_TENTHS_MIN, LBS_TENTHS_MAX)
    val lbsIntSel = lbsTenthsClamped / 10
    val lbsDecSel = lbsTenthsClamped % 10

    // 6) 照片：用 Uri string 存起來（可 saveable）
    val context = LocalContext.current
    val isPreview = LocalInspectionMode.current
    var photoUriString by rememberSaveable { mutableStateOf<String?>(null) }

    LaunchedEffect(canUseActivityResult, isPreview) {
        Log.d(
            "RecordWeightScreen",
            "content canUseActivityResult=$canUseActivityResult, isPreview=$isPreview"
        )
    }

    // 拍照（回傳 Bitmap，再存成 cache 檔）
    val takePhotoLauncher =
        if (canUseActivityResult) {
            rememberLauncherForActivityResult(
                contract = ActivityResultContracts.TakePicturePreview()
            ) { bitmap: Bitmap? ->
                if (bitmap != null) {
                    val file = bitmapToCacheFile(context, bitmap)
                    photoUriString = Uri.fromFile(file).toString()
                    Log.d("RecordWeightScreen", "camera photo file = $file")
                } else {
                    Log.d("RecordWeightScreen", "camera returned null bitmap")
                }
            }
        } else {
            null
        }

    fun launchTakePhoto() {
        val launcher = takePhotoLauncher
        if (launcher == null) {
            val msg = if (isPreview) {
                "Camera only works in the running app, not in Preview."
            } else {
                "Camera is not available in this screen."
            }
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            return
        }
        launcher.launch(null) // TakePicturePreview 不需要參數
    }

    // 7) 日期 BottomSheet（放在 Scaffold 外層疊加）
    WeighingDateSheet(
        visible = showDateSheet,
        currentDate = selectedDate,
        maxDate = today,              // ★ 加這行
        onDismiss = { showDateSheet = false },
        onConfirm = { newDate ->
            selectedDate = newDate
            showDateSheet = false
        }
    )

    Scaffold(
        containerColor = Color(0xFFF5F5F5),
        topBar = {
            WeightTopBar(
                title = "Record Weight",
                onBack = onBack
            )
        },
        bottomBar = {
            Box {
                Button(
                    onClick = {
                        // ✅ 0) 把「使用者這次用的單位」寫回 DataStore，WeightScreen 才會跟著變
                        val unitUsed = if (useMetric) {
                            UserProfileStore.WeightUnit.KG
                        } else {
                            UserProfileStore.WeightUnit.LBS
                        }

                        // 1) 依目前 UI 單位計算要送到後端的 kg & lbs
                        val (kgToSave, lbsToSave) = if (useMetric) {
                            val kgClamped = valueKg.coerceIn(KG_MIN, KG_MAX)
                            val kgRounded = roundToOneDecimal(kgClamped)
                            val lbs = kgToLbs1(kgRounded)
                            kgRounded to lbs
                        } else {
                            val rawLbs = (valueLbsTenths.coerceIn(LBS_TENTHS_MIN, LBS_TENTHS_MAX)) / 10.0
                            val lbsRounded = roundToOneDecimal(rawLbs)
                            val kg = lbsToKg1(lbsRounded).coerceIn(KG_MIN, KG_MAX)
                            kg to lbsRounded
                        }

                        val photoFile = uriStringToCacheFile(context, photoUriString)

                        vm.save(
                            weightKg = kgToSave,
                            weightLbs = lbsToSave,
                            date = selectedDate,
                            photo = photoFile,
                            unitUsedToPersist = unitUsed  // ✅ 只有成功才會寫回
                        )
                        onSaved()
                    },
                    enabled = valueKg > 0.0 && !ui.saving,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(start = 20.dp, end = 20.dp, bottom = 40.dp)
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(28.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Black,
                        contentColor = Color.White
                    )
                ) {
                    Text(
                        text = "Save",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.Medium,
                            letterSpacing = 0.2.sp
                        )
                    )
                }
            }
        }
    ) { inner ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
        ) {
            // ★ 縮小 Title 與日曆 pill 的距離
            Spacer(Modifier.height(2.dp))

            // === 日期 pill：白底黑字，點擊打開 sheet ===
            DateHeader(
                dateText = selectedDate.format(dateFormatterDisplay),
                onClick = { showDateSheet = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
            )

            // === 照片（只支援拍照） ===
            PhotoPickerBlock(
                photoUriString = photoUriString,
                cameraAvailable = canUseActivityResult,
                onPickPhoto = {
                    launchTakePhoto()
                }
            )

            Spacer(Modifier.height(26.dp))

            // === 單位切換 (lbs / kg) ===
            WeightUnitSegmentedRecord(
                useMetric = useMetric,
                onChange = { useMetric = it },
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
            )

            // === KG / LBS 輪盤 ===
            Spacer(Modifier.height(10.dp))

            if (useMetric) {
                // KG 模式：整數位 + 小數位
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 2.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    NumberWheelRecord(
                        range = KG_MIN.toInt()..KG_MAX.toInt(),
                        value = kgIntSel,
                        onValueChange = { newInt ->
                            val newTenths = (newInt * 10 + kgDecSel)
                                .coerceIn(
                                    (KG_MIN * 10).toInt(),
                                    (KG_MAX * 10).toInt()
                                )
                            val newKg = newTenths / 10.0
                            valueKg = newKg
                            valueLbsTenths = kgToLbsTenthsRecord(newKg)
                        },
                        rowHeight = rowHeight,
                        centerTextSize = 26.sp,
                        TextSize = 22.sp,
                        sideAlpha = 0.35f,
                        modifier = Modifier
                            .width(120.dp)
                            .padding(start = 20.dp)
                    )
                    Text(
                        ".",
                        fontSize = 34.sp,
                        modifier = Modifier.padding(horizontal = 6.dp)
                    )
                    NumberWheelRecord(
                        range = 0..9,
                        value = kgDecSel,
                        onValueChange = { newDec ->
                            val newTenths = (kgIntSel * 10 + newDec)
                                .coerceIn(
                                    (KG_MIN * 10).toInt(),
                                    (KG_MAX * 10).toInt()
                                )
                            val newKg = newTenths / 10.0
                            valueKg = newKg
                            valueLbsTenths = kgToLbsTenthsRecord(newKg)
                        },
                        rowHeight = rowHeight,
                        centerTextSize = 26.sp,
                        TextSize = 22.sp,
                        sideAlpha = 0.35f,
                        modifier = Modifier
                            .width(80.dp)
                            .padding(start = 5.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("kg", fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
                }
            } else {
                // LBS 模式：整數位 + 小數位
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 2.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    NumberWheelRecord(
                        range = LBS_INT_MIN..LBS_INT_MAX,
                        value = lbsIntSel,
                        onValueChange = { newInt ->
                            val newTenths = (newInt * 10 + lbsDecSel)
                                .coerceIn(LBS_TENTHS_MIN, LBS_TENTHS_MAX)
                            valueLbsTenths = newTenths
                            val newLbs = newTenths / 10.0
                            val newKg = lbsToKg1(newLbs)
                            valueKg = newKg.coerceIn(KG_MIN, KG_MAX)
                        },
                        rowHeight = rowHeight,
                        centerTextSize = 26.sp,
                        TextSize = 22.sp,
                        sideAlpha = 0.35f,
                        modifier = Modifier
                            .width(120.dp)
                            .padding(start = 30.dp)
                    )
                    Text(
                        ".",
                        fontSize = 34.sp,
                        modifier = Modifier.padding(horizontal = 6.dp)
                    )
                    NumberWheelRecord(
                        range = 0..9,
                        value = lbsDecSel,
                        onValueChange = { newDec ->
                            val intPart = lbsIntSel
                            val newTenths = (intPart * 10 + newDec)
                                .coerceIn(LBS_TENTHS_MIN, LBS_TENTHS_MAX)
                            valueLbsTenths = newTenths
                            val newLbs = newTenths / 10.0
                            val newKg = lbsToKg1(newLbs)
                            valueKg = newKg.coerceIn(KG_MIN, KG_MAX)
                        },
                        rowHeight = rowHeight,
                        centerTextSize = 26.sp,
                        TextSize = 22.sp,
                        sideAlpha = 0.35f,
                        modifier = Modifier
                            .width(80.dp)
                            .padding(start = 5.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("lbs", fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(Modifier.height(18.dp))

            Box(
                Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "This weight will be recorded for ${
                        selectedDate.format(dateFormatterDisplay)
                    }.",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 12.sp
                    ),
                    color = Color(0xFF9AA3AE),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(0.8f)
                )
            }
        }
    }
}

/* ---------------------------- 日期 Header ---------------------------- */

@Composable
private fun DateHeader(
    dateText: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Surface(
            onClick = onClick,
            shape = RoundedCornerShape(999.dp),
            color = Color(0xFFF5F5F5),
            shadowElevation = 0.dp,
        ) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.CalendarMonth,
                    contentDescription = "Select date",
                    tint = Color.Black,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = dateText,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF111114)
                )
            }
        }
    }
}

/* ---------------------------- 單位 Segmented ---------------------------- */

@Composable
private fun WeightUnitSegmentedRecord(
    useMetric: Boolean,
    onChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = Color(0xFFE2E5EA),
        modifier = modifier
            .fillMaxWidth(0.55f)
            .heightIn(min = 40.dp)
    ) {
        Row(Modifier.padding(4.dp)) {
            SegItemRecord(
                text = "lbs",
                selected = !useMetric,
                onClick = { onChange(false) },
                selectedColor = Color.Black,
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp)
            )
            Spacer(Modifier.width(6.dp))
            SegItemRecord(
                text = "kg",
                selected = useMetric,
                onClick = { onChange(true) },
                selectedColor = Color.Black,
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp)
            )
        }
    }
}

@Composable
private fun SegItemRecord(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    selectedColor: Color,
    modifier: Modifier = Modifier
) {
    val corner = 22.dp
    val fSize = 18.sp
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(corner),
        color = if (selected) selectedColor else Color.Transparent,
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .defaultMinSize(minHeight = 40.dp)
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                fontSize = fSize,
                fontWeight = FontWeight.SemiBold,
                color = if (selected) Color.White else Color(0xFF333333),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/* ---------------------------- 數字輪盤（含 label） ---------------------------- */

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun NumberWheelRecord(
    range: IntRange,
    value: Int,
    onValueChange: (Int) -> Unit,
    rowHeight: Dp,
    centerTextSize: TextUnit,
    TextSize: TextUnit,
    sideAlpha: Float,
    modifier: Modifier = Modifier,
    label: (Int) -> String = { it.toString() }
) {
    val VISIBLE_COUNT = 5
    val MID = VISIBLE_COUNT / 2
    val items = remember(range) { range.toList() }
    val selectedIdx = (value - range.first).coerceIn(0, items.lastIndex)

    val state = rememberLazyListState()
    val fling = rememberSnapFlingBehavior(lazyListState = state)

    var initialized by remember(range) { mutableStateOf(false) }
    LaunchedEffect(range, value) {
        if (!initialized) {
            state.scrollToItem(selectedIdx)
            initialized = true
        }
    }

    val centerIndex by remember {
        derivedStateOf {
            val li = state.layoutInfo
            if (li.visibleItemsInfo.isEmpty()) return@derivedStateOf selectedIdx
            val viewportCenter = (li.viewportStartOffset + li.viewportEndOffset) / 2
            li.visibleItemsInfo.minByOrNull { info ->
                kotlin.math.abs((info.offset + info.size / 2) - viewportCenter)
            }?.index ?: selectedIdx
        }
    }

    LaunchedEffect(centerIndex, initialized) {
        if (initialized) onValueChange(items[centerIndex])
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(rowHeight * VISIBLE_COUNT)
    ) {
        LazyColumn(
            state = state,
            flingBehavior = fling,
            contentPadding = PaddingValues(vertical = rowHeight * MID),
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxSize()
        ) {
            itemsIndexed(items) { index, num ->
                val isCenter = index == centerIndex
                val alpha = if (isCenter) 1f else sideAlpha
                val size = if (isCenter) centerTextSize else TextSize
                val weight = if (isCenter) FontWeight.SemiBold else FontWeight.Normal

                Row(
                    modifier = Modifier
                        .height(rowHeight)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = label(num),
                        fontSize = size,
                        fontWeight = weight,
                        color = Color.Black.copy(alpha = alpha),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        val lineColor = Color(0x11000000)
        val half = rowHeight / 2
        val lineThickness = 1.dp
        Box(
            Modifier
                .align(Alignment.Center)
                .offset(y = -half)
                .fillMaxWidth()
                .height(lineThickness)
                .background(lineColor)
        )
        Box(
            Modifier
                .align(Alignment.Center)
                .offset(y = half - lineThickness)
                .fillMaxWidth()
                .height(lineThickness)
                .background(lineColor)
        )
    }
}

/* ---------------------------- 日期 BottomSheet ---------------------------- */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WeighingDateSheet(
    visible: Boolean,
    currentDate: LocalDate,
    maxDate: LocalDate,                // ★ 上限日期（用戶當地今天）
    onDismiss: () -> Unit,             // ★ 只有 Cancel 會呼叫
    onConfirm: (LocalDate) -> Unit     // ★ 更新日期，但不關閉 Sheet
) {
    if (!visible) return

    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        // ★ 阻擋任何往 Hidden 的狀態切換 → 無法靠滑動把 Sheet 關掉
        confirmValueChange = { target ->
            target != SheetValue.Hidden
        }
    )

    // ★ 今天：所有上限都以「maxDate（用戶當地今天）」為準
    val today = remember(maxDate) { maxDate }

    // 年份範圍：從 today - 5 年到 today（不能超過今天那一年）
    val yearRange = remember(today) {
        (today.year - 5)..today.year
    }

    var year by rememberSaveable(currentDate) { mutableStateOf(currentDate.year) }
    var month by rememberSaveable(currentDate) { mutableStateOf(currentDate.monthValue) }
    var day by rememberSaveable(currentDate) { mutableStateOf(currentDate.dayOfMonth) }

    val months = remember {
        Month.values().map { m ->
            m.getDisplayName(java.time.format.TextStyle.FULL, Locale.ENGLISH)
        }
    }

    // ★ 根據 year / today 決定「月份可選範圍」
    val monthRange: IntRange = remember(year, today) {
        if (year >= today.year) {
            // 今年或超過今年 → 最多只能到本月
            1..today.monthValue
        } else {
            // 過去年份 → 1..12 都可以
            1..12
        }
    }

    // ★ 根據 year / month / today 決定「日期可選範圍」
    val dayRange: IntRange = remember(year, month, today) {
        val maxDayOfMonth = Month.of(month).length(Year.of(year).isLeap)
        val maxDay = if (year == today.year && month == today.monthValue) {
            // 今年本月 → 最多只能到今天
            kotlin.math.min(maxDayOfMonth, today.dayOfMonth)
        } else {
            maxDayOfMonth
        }
        1..maxDay
    }

    // ★ clampDay 也一併考慮「不能超過今天」
    fun clampDay(y: Int, m: Int, d: Int): Int {
        val maxDayOfMonth = Month.of(m).length(Year.of(y).isLeap)
        val maxDay = if (y == today.year && m == today.monthValue) {
            kotlin.math.min(maxDayOfMonth, today.dayOfMonth)
        } else {
            maxDayOfMonth
        }
        return d.coerceIn(1, maxDay)
    }

    ModalBottomSheet(
        // ★ 不處理外部 dismiss：滑動 / 點外面 / 返回鍵都不會關
        onDismissRequest = {
            // 故意留空，只有 Cancel 會真的關
        },
        sheetState = sheetState,
        containerColor = Color(0xFFF5F5F5)
    ) {
        // ★ 提高最小高度，讓 Sheet 看起來更高、更穩
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 520.dp)  // ⬆️ 比原本 420 再高一階
                .padding(start = 20.dp, end = 20.dp, top = 5.dp, bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Select your weighing date",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF111114)
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "When did you step on the scale?",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF6B7280)
            )

            Spacer(Modifier.height(18.dp))

            // ★ 把 rowHeight 拉大，滑動區域變高，不容易滑出 Sheet
            val rowHeight = 52.dp

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Day：依 dayRange 限制
                NumberWheelRecord(
                    range = dayRange,
                    value = day,
                    onValueChange = { day = it },
                    rowHeight = rowHeight,
                    centerTextSize = 22.sp,
                    TextSize = 17.sp,
                    sideAlpha = 0.35f,
                    modifier = Modifier.width(70.dp)
                )
                Spacer(Modifier.width(12.dp))
                // Month：依 monthRange 限制
                NumberWheelRecord(
                    range = monthRange,
                    value = month,
                    onValueChange = { month = it },
                    rowHeight = rowHeight,
                    centerTextSize = 22.sp,
                    TextSize = 17.sp,
                    sideAlpha = 0.35f,
                    modifier = Modifier.width(130.dp),
                    label = { idx -> months[idx - 1] }
                )
                Spacer(Modifier.width(12.dp))
                // Year：最多到今天那一年
                NumberWheelRecord(
                    range = yearRange,
                    value = year,
                    onValueChange = { year = it },
                    rowHeight = rowHeight,
                    centerTextSize = 22.sp,
                    TextSize = 17.sp,
                    sideAlpha = 0.35f,
                    modifier = Modifier.width(90.dp)
                )
            }
            Spacer(Modifier.height(30.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // ★ Save：在上面，只回傳日期，不關閉 Sheet
                Button(
                    onClick = {
                        val safeYear = year.coerceIn(yearRange)
                        val safeMonth = if (safeYear == today.year) {
                            month.coerceIn(1, today.monthValue)
                        } else {
                            month.coerceIn(1, 12)
                        }
                        val safeDay = clampDay(safeYear, safeMonth, day)
                        val raw = LocalDate.of(safeYear, safeMonth, safeDay)
                        val finalDate = if (raw.isAfter(today)) today else raw

                        onConfirm(finalDate)  // ★ 外層只更新 selectedDate，不關閉 Sheet
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(55.dp),
                    shape = RoundedCornerShape(999.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Black,
                        contentColor = Color.White
                    )
                ) {
                    Text("Save", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }

                // ★ Cancel：在下面，唯一會關閉 Sheet 的入口
                OutlinedButton(
                    onClick = {
                        onDismiss()          // 外層把 visible = false
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(55.dp),
                    shape = RoundedCornerShape(999.dp),
                    border = BorderStroke(1.dp, Color(0xFFE5E5E5)),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Color(0xFFE1E4EA),
                        contentColor = Color(0xFF111114)
                    )
                ) {
                    Text(
                        text = "Cancel",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

/* ---------------------------- 照片區塊（只用相機） ---------------------------- */

@Composable
private fun PhotoPickerBlock(
    photoUriString: String?,
    cameraAvailable: Boolean,
    onPickPhoto: () -> Unit
) {
    val uri = photoUriString?.takeIf { it.isNotBlank() }?.let { Uri.parse(it) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(144.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFF111114).copy(alpha = 0.06f))
                .clickable(onClick = onPickPhoto),
            contentAlignment = Alignment.Center
        ) {
            if (uri != null) {
                AsyncImage(
                    model = uri,
                    contentDescription = "Weight photo",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(
                    painter = painterResource(id = R.drawable.weight_image_2),
                    contentDescription = "Add weight photo",
                    tint = Color(0xFF9AA3AE),
                    modifier = Modifier.size(44.dp)
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        Text(
            text = when {
                !cameraAvailable -> "Camera not available"
                uri == null -> "Take a photo"
                else -> "Retake photo"
            },
            color = if (cameraAvailable) Color.Black else Color(0xFF9AA3AE),
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

/* ---------------------------- Uri / Bitmap → 暫存 File ---------------------------- */

private fun uriStringToCacheFile(
    context: Context,
    uriString: String?
): File? {
    if (uriString.isNullOrBlank()) return null
    return try {
        val uri = Uri.parse(uriString)
        val inputStream = context.contentResolver.openInputStream(uri) ?: return null
        val tempFile = File.createTempFile("weight_photo_", ".jpg", context.cacheDir)
        tempFile.outputStream().use { out ->
            inputStream.use { it.copyTo(out) }
        }
        tempFile
    } catch (e: Exception) {
        null // 失敗就當沒帶照片，不要讓使用者整個流程壞掉
    }
}

private fun bitmapToCacheFile(
    context: Context,
    bitmap: Bitmap
): File {
    val tempFile = File.createTempFile("weight_camera_", ".jpg", context.cacheDir)
    tempFile.outputStream().use { out ->
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
    }
    return tempFile
}

/* ---------------------------- 換算工具 ---------------------------- */

/**
 * 給 RecordWeight 使用的 0.1 lbs 刻度（Int = 實際磅數 * 10）。
 * 例如 70 kg -> 154.3 lbs -> 傳回 1543。
 */
private fun kgToLbsTenthsRecord(kg: Double): Int =
    (kgToLbs1(kg) * 10.0).toInt()

/**
 * 一位小數四捨五入的共用函式。
 * 使用 BigDecimal.valueOf(...)，避免浮點誤差導致 78.2 → 78.1。
 */
private fun roundToOneDecimal(value: Double): Double =
    BigDecimal.valueOf(value)
        .setScale(1, RoundingMode.HALF_UP)
        .toDouble()

/* ---------------------------- Context 尋找 ActivityResultRegistryOwner ---------------------------- */

/**
 * 從 ContextWrapper 鏈一路往上找 ActivityResultRegistryOwner。
 * - 若 Activity 是 ComponentActivity / AppCompatActivity，通常會實作這個介面。
 */
private tailrec fun Context.findActivityResultRegistryOwner(): ActivityResultRegistryOwner? {
    return when (this) {
        is ActivityResultRegistryOwner -> this
        is ContextWrapper -> baseContext.findActivityResultRegistryOwner()
        else -> null
    }
}
