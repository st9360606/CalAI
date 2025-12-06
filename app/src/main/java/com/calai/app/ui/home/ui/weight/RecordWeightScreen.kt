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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SheetValue
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
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
import kotlinx.coroutines.launch
import java.io.File
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.Month
import java.time.Year
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs
import kotlin.math.min

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

    val localOwner = LocalActivityResultRegistryOwner.current
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
 * 內層真正的畫面：只看 canUseActivityResult
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
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // 1) KG / LBS 範圍
    val KG_MIN = 20.0
    val KG_MAX = 800.0
    val LBS_TENTHS_MIN = kgToLbsTenthsRecord(KG_MIN)
    val LBS_TENTHS_MAX = kgToLbsTenthsRecord(KG_MAX)
    val LBS_INT_MIN = LBS_TENTHS_MIN / 10
    val LBS_INT_MAX = LBS_TENTHS_MAX / 10

    // 2) 日期狀態（預設今天）
    val today = remember { LocalDate.now() }
    var selectedDate by rememberSaveable { mutableStateOf(today) }
    val dateFormatterDisplay = remember { DateTimeFormatter.ofPattern("yyyy/MM/dd") }
    var showDateSheet by remember { mutableStateOf(false) }

    // 3) ⚠️ 初始值只做一次（避免 ui refresh 時輪盤跳回）
    var initialized by rememberSaveable { mutableStateOf(false) }
    var useMetric by rememberSaveable { mutableStateOf(true) }
    var valueKg by rememberSaveable { mutableStateOf(65.0) }
    var valueLbsTenths by rememberSaveable { mutableStateOf(kgToLbsTenthsRecord(65.0)) }

    LaunchedEffect(ui.unit, ui.current, ui.profileWeightKg) {
        if (initialized) return@LaunchedEffect
        initialized = true

        useMetric = (ui.unit == UserProfileStore.WeightUnit.KG)

        val initialKgRaw = ui.current ?: ui.profileWeightKg ?: 65.0
        val initialKg = initialKgRaw.coerceIn(KG_MIN, KG_MAX)
        valueKg = initialKg
        valueLbsTenths = kgToLbsTenthsRecord(initialKg).coerceIn(LBS_TENTHS_MIN, LBS_TENTHS_MAX)
    }

    // 4) 從 value 推 wheel 選中值
    val kgTenths = (valueKg * 10.0).toInt()
        .coerceIn((KG_MIN * 10).toInt(), (KG_MAX * 10).toInt())
    val kgIntSel = kgTenths / 10
    val kgDecSel = kgTenths % 10

    val lbsTenthsClamped = valueLbsTenths.coerceIn(LBS_TENTHS_MIN, LBS_TENTHS_MAX)
    val lbsIntSel = lbsTenthsClamped / 10
    val lbsDecSel = lbsTenthsClamped % 10

    // 5) 照片：Uri string（可 saveable）
    val context = LocalContext.current
    val isPreview = LocalInspectionMode.current
    var photoUriString by rememberSaveable { mutableStateOf<String?>(null) }

    // 拍照（回傳 Bitmap，再存成 cache 檔）
    val takePhotoLauncher =
        if (canUseActivityResult) {
            rememberLauncherForActivityResult(
                contract = ActivityResultContracts.TakePicturePreview()
            ) { bitmap: Bitmap? ->
                if (bitmap != null) {
                    val file = bitmapToCacheFile(context, bitmap)
                    // 會是 file://...
                    photoUriString = Uri.fromFile(file).toString()
                    Log.d("RecordWeightScreen", "camera photo file=$file")
                } else {
                    Log.d("RecordWeightScreen", "camera returned null bitmap")
                }
            }
        } else null

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
        launcher.launch(null)
    }

    // 日期 BottomSheet
    WeighingDateSheet(
        visible = showDateSheet,
        currentDate = selectedDate,
        maxDate = today,
        onDismiss = { showDateSheet = false },
        onConfirm = { newDate ->
            selectedDate = newDate
            showDateSheet = false
        }
    )

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        containerColor = Color(0xFFF5F5F5),
        topBar = {
            WeightTopBar(
                title = "Record Weight",
                onBack = onBack
            )
        },
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(start = 20.dp, end = 20.dp, bottom = 40.dp)
            ) {
                Button(
                    onClick = {
                        val unitUsed = if (useMetric) {
                            UserProfileStore.WeightUnit.KG
                        } else {
                            UserProfileStore.WeightUnit.LBS
                        }

                        val (kgToSave, lbsToSave) = if (useMetric) {
                            val kgClamped = valueKg.coerceIn(KG_MIN, KG_MAX)
                            val kgRounded = roundToOneDecimal(kgClamped)
                            kgRounded to kgToLbs1(kgRounded)
                        } else {
                            val rawLbs = (valueLbsTenths.coerceIn(LBS_TENTHS_MIN, LBS_TENTHS_MAX)) / 10.0
                            val lbsRounded = roundToOneDecimal(rawLbs)
                            val kg = lbsToKg1(lbsRounded).coerceIn(KG_MIN, KG_MAX)
                            kg to lbsRounded
                        }

                        val photoFile = uriStringToCacheFile(context, photoUriString)

                        // ✅ 成功才 onSaved；失敗留在本頁 snackbar
                        vm.save(
                            weightKg = kgToSave,
                            weightLbs = lbsToSave,
                            date = selectedDate,
                            photo = photoFile,
                            unitUsedToPersist = unitUsed
                        ) { result ->
                            result.onSuccess {
                                onSaved()
                            }.onFailure { e ->
                                scope.launch {
                                    snackbarHostState.showSnackbar(
                                        message = e.message ?: "Save failed"
                                    )
                                }
                            }
                        }
                    },
                    enabled = valueKg > 0.0 && !ui.saving,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(28.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Black,
                        contentColor = Color.White
                    )
                ) {
                    if (ui.saving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = Color.White
                        )
                    } else {
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
        }
    ) { inner ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
        ) {
            Spacer(Modifier.height(2.dp))

            DateHeader(
                dateText = selectedDate.format(dateFormatterDisplay),
                onClick = { showDateSheet = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
            )

            PhotoPickerBlock(
                photoUriString = photoUriString,
                cameraAvailable = canUseActivityResult,
                onPickPhoto = { launchTakePhoto() }
            )

            Spacer(Modifier.height(26.dp))

            WeightUnitSegmentedRecord(
                useMetric = useMetric,
                onChange = { useMetric = it },
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            Spacer(Modifier.height(10.dp))

            if (useMetric) {
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
                                .coerceIn((KG_MIN * 10).toInt(), (KG_MAX * 10).toInt())
                            val newKg = newTenths / 10.0
                            valueKg = newKg
                            valueLbsTenths = kgToLbsTenthsRecord(newKg)
                        },
                        rowHeight = rowHeight,
                        centerTextSize = 26.sp,
                        textSize = 22.sp,
                        sideAlpha = 0.35f,
                        modifier = Modifier
                            .width(120.dp)
                            .padding(start = 20.dp)
                    )
                    Text(".", fontSize = 34.sp, modifier = Modifier.padding(horizontal = 6.dp))
                    NumberWheelRecord(
                        range = 0..9,
                        value = kgDecSel,
                        onValueChange = { newDec ->
                            val newTenths = (kgIntSel * 10 + newDec)
                                .coerceIn((KG_MIN * 10).toInt(), (KG_MAX * 10).toInt())
                            val newKg = newTenths / 10.0
                            valueKg = newKg
                            valueLbsTenths = kgToLbsTenthsRecord(newKg)
                        },
                        rowHeight = rowHeight,
                        centerTextSize = 26.sp,
                        textSize = 22.sp,
                        sideAlpha = 0.35f,
                        modifier = Modifier
                            .width(80.dp)
                            .padding(start = 5.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("kg", fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
                }
            } else {
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
                            val newKg = lbsToKg1(newTenths / 10.0).coerceIn(KG_MIN, KG_MAX)
                            valueKg = newKg
                        },
                        rowHeight = rowHeight,
                        centerTextSize = 26.sp,
                        textSize = 22.sp,
                        sideAlpha = 0.35f,
                        modifier = Modifier
                            .width(120.dp)
                            .padding(start = 30.dp)
                    )
                    Text(".", fontSize = 34.sp, modifier = Modifier.padding(horizontal = 6.dp))
                    NumberWheelRecord(
                        range = 0..9,
                        value = lbsDecSel,
                        onValueChange = { newDec ->
                            val newTenths = (lbsIntSel * 10 + newDec)
                                .coerceIn(LBS_TENTHS_MIN, LBS_TENTHS_MAX)
                            valueLbsTenths = newTenths
                            val newKg = lbsToKg1(newTenths / 10.0).coerceIn(KG_MIN, KG_MAX)
                            valueKg = newKg
                        },
                        rowHeight = rowHeight,
                        centerTextSize = 26.sp,
                        textSize = 22.sp,
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

            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(
                    text = "This weight will be recorded for ${selectedDate.format(dateFormatterDisplay)}.",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
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
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Surface(
            onClick = onClick,
            shape = RoundedCornerShape(999.dp),
            color = Color(0xFFF5F5F5),
            shadowElevation = 0.dp,
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
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
                modifier = Modifier.weight(1f).height(40.dp)
            )
            Spacer(Modifier.width(6.dp))
            SegItemRecord(
                text = "kg",
                selected = useMetric,
                onClick = { onChange(true) },
                selectedColor = Color.Black,
                modifier = Modifier.weight(1f).height(40.dp)
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
    val fontSize = 18.sp

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
                fontSize = fontSize,
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
    textSize: TextUnit,
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
                abs((info.offset + info.size / 2) - viewportCenter)
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
                val size = if (isCenter) centerTextSize else textSize
                val weight = if (isCenter) FontWeight.SemiBold else FontWeight.Normal

                Row(
                    modifier = Modifier.height(rowHeight).fillMaxWidth(),
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
    maxDate: LocalDate,
    onDismiss: () -> Unit,
    onConfirm: (LocalDate) -> Unit
) {
    if (!visible) return

    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { goal -> goal != SheetValue.Hidden }
    )

    val today = remember(maxDate) { maxDate }
    val yearRange = remember(today) { (today.year - 5)..today.year }

    var year by rememberSaveable(currentDate) { mutableStateOf(currentDate.year) }
    var month by rememberSaveable(currentDate) { mutableStateOf(currentDate.monthValue) }
    var day by rememberSaveable(currentDate) { mutableStateOf(currentDate.dayOfMonth) }

    val months = remember {
        Month.values().map { m ->
            m.getDisplayName(java.time.format.TextStyle.FULL, Locale.ENGLISH)
        }
    }

    val monthRange: IntRange = remember(year, today) {
        if (year >= today.year) 1..today.monthValue else 1..12
    }

    val dayRange: IntRange = remember(year, month, today) {
        val maxDayOfMonth = Month.of(month).length(Year.of(year).isLeap)
        val maxDay = if (year == today.year && month == today.monthValue) {
            min(maxDayOfMonth, today.dayOfMonth)
        } else {
            maxDayOfMonth
        }
        1..maxDay
    }

    fun clampDay(y: Int, m: Int, d: Int): Int {
        val maxDayOfMonth = Month.of(m).length(Year.of(y).isLeap)
        val maxDay = if (y == today.year && m == today.monthValue) {
            min(maxDayOfMonth, today.dayOfMonth)
        } else {
            maxDayOfMonth
        }
        return d.coerceIn(1, maxDay)
    }

    ModalBottomSheet(
        onDismissRequest = { /* 只有 Cancel 才關 */ },
        sheetState = sheetState,
        containerColor = Color(0xFFF5F5F5)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 520.dp)
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

            val wheelRowHeight = 52.dp

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                NumberWheelRecord(
                    range = dayRange,
                    value = day,
                    onValueChange = { day = it },
                    rowHeight = wheelRowHeight,
                    centerTextSize = 22.sp,
                    textSize = 17.sp,
                    sideAlpha = 0.35f,
                    modifier = Modifier.width(70.dp)
                )
                Spacer(Modifier.width(12.dp))
                NumberWheelRecord(
                    range = monthRange,
                    value = month,
                    onValueChange = { month = it },
                    rowHeight = wheelRowHeight,
                    centerTextSize = 22.sp,
                    textSize = 17.sp,
                    sideAlpha = 0.35f,
                    modifier = Modifier.width(130.dp),
                    label = { idx -> months[idx - 1] }
                )
                Spacer(Modifier.width(12.dp))
                NumberWheelRecord(
                    range = yearRange,
                    value = year,
                    onValueChange = { year = it },
                    rowHeight = wheelRowHeight,
                    centerTextSize = 22.sp,
                    textSize = 17.sp,
                    sideAlpha = 0.35f,
                    modifier = Modifier.width(90.dp)
                )
            }

            Spacer(Modifier.height(30.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
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

                        onConfirm(finalDate)
                    },
                    modifier = Modifier.fillMaxWidth().height(55.dp),
                    shape = RoundedCornerShape(999.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Black,
                        contentColor = Color.White
                    )
                ) {
                    Text("Save", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }

                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth().height(55.dp),
                    shape = RoundedCornerShape(999.dp),
                    border = BorderStroke(1.dp, Color(0xFFE5E5E5)),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Color(0xFFE1E4EA),
                        contentColor = Color(0xFF111114)
                    )
                ) {
                    Text("Cancel", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
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

private fun uriStringToCacheFile(context: Context, uriString: String?): File? {
    if (uriString.isNullOrBlank()) return null
    return runCatching {
        val uri = Uri.parse(uriString)

        // ✅ file:// 直接轉 File（你拍照是 Uri.fromFile(file) 產生的）
        if (uri.scheme == "file") {
            val path = uri.path ?: return null
            return File(path).takeIf { it.exists() }
        }

        // content:// 才用 contentResolver 複製成 temp file
        if (uri.scheme == "content") {
            val input = context.contentResolver.openInputStream(uri) ?: return null
            val tempFile = File.createTempFile("weight_photo_", ".jpg", context.cacheDir)
            tempFile.outputStream().use { out ->
                input.use { it.copyTo(out) }
            }
            return tempFile
        }

        null
    }.getOrNull()
}

private fun bitmapToCacheFile(context: Context, bitmap: Bitmap): File {
    val tempFile = File.createTempFile("weight_camera_", ".jpg", context.cacheDir)
    tempFile.outputStream().use { out ->
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
    }
    return tempFile
}

/* ---------------------------- 換算工具 ---------------------------- */

private fun kgToLbsTenthsRecord(kg: Double): Int =
    (kgToLbs1(kg) * 10.0).toInt()

private fun roundToOneDecimal(value: Double): Double =
    BigDecimal.valueOf(value).setScale(1, RoundingMode.HALF_UP).toDouble()

/* ---------------------------- Context 尋找 ActivityResultRegistryOwner ---------------------------- */

private tailrec fun Context.findActivityResultRegistryOwner(): ActivityResultRegistryOwner? {
    return when (this) {
        is ActivityResultRegistryOwner -> this
        is ContextWrapper -> baseContext.findActivityResultRegistryOwner()
        else -> null
    }
}
