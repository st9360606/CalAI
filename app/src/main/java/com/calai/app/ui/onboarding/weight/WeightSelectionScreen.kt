package com.calai.app.ui.onboarding.weight

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.calai.app.R
import com.calai.app.data.auth.store.UserProfileStore
import com.calai.app.ui.common.OnboardingProgress
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.composed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.layout.ime
import androidx.compose.ui.platform.LocalDensity
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun WeightSelectionScreen(
    vm: WeightSelectionViewModel,
    onBack: () -> Unit,
    onNext: () -> Unit
) {
    val weightKg by vm.weightKgState.collectAsState()
    val savedUnit by vm.weightUnitState.collectAsState()

    var useMetric by rememberSaveable(savedUnit) {
        mutableStateOf(savedUnit == UserProfileStore.WeightUnit.KG)
    }
    var valueKg by remember(weightKg) { mutableStateOf(weightKg.toDouble()) }

    var text by remember(weightKg, useMetric) {
        mutableStateOf(
            if (weightKg == 0f) "" else
                formatOneDecimal(if (useMetric) valueKg else kgToLbs(valueKg))
        )
    }

    val listState = rememberLazyListState()
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    val scope = rememberCoroutineScope()

    // 依 IME（鍵盤）是否可見，動態調整 CTA 間距
    val density = LocalDensity.current
    val imeVisible = WindowInsets.ime.getBottom(density) > 0
    val ctaBottomGap = if (imeVisible) 10.dp else 59.dp   // 鍵盤出現時更貼近（10.dp 可再調）

    Scaffold(
        containerColor = Color.White,
        topBar = {
            TopAppBar(
                title = {},
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White,
                    navigationIconContentColor = Color(0xFF111114)
                ),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Box(
                            modifier = Modifier
                                .size(39.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color(0xFFF1F3F7)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = Color(0xFF111114)
                            )
                        }
                    }
                }
            )
        },
        bottomBar = {
            // 只讓整個 bottom 區跟著 IME 往上；額外間距改用動態值
            Box(Modifier.imePadding()) {
                val navBarsMod = if (imeVisible) Modifier else Modifier.navigationBarsPadding()
                Button(
                    onClick = {
                        val clean = text.replace(',', '.').trim()
                        val typed = clean.toDoubleOrNull()
                        val kgToSave = when {
                            clean.isEmpty() -> 0.0
                            typed == null -> valueKg
                            useMetric -> typed
                            else -> lbsToKg(typed)
                        }
                        vm.saveWeightKg(roundKg2(kgToSave))
                        vm.saveWeightUnit(
                            if (useMetric) UserProfileStore.WeightUnit.KG
                            else UserProfileStore.WeightUnit.LBS
                        )
                        onNext()
                    },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .then(navBarsMod)                            // 鍵盤出現時不再吃 nav bar padding
                        .padding(start = 20.dp, end = 20.dp, bottom = ctaBottomGap)
                        .fillMaxWidth()
                        .height(64.dp),
                    shape = RoundedCornerShape(28.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Black,
                        contentColor = Color.White
                    )
                ) {
                    Text(
                        stringResource(R.string.continue_text),
                        fontSize = 19.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

    ) { inner ->
        // 讓內容可捲動，並在底部多預留 CTA 高度 + CTA 的 bottom padding + 12dp 緩衝
        val layoutDir = LocalLayoutDirection.current
        val navBars = WindowInsets.navigationBars.asPaddingValues()
        val extraBottom = 64.dp + ctaBottomGap + 12.dp
        val listContentPadding = PaddingValues(
            start = navBars.calculateStartPadding(layoutDir),
            top = navBars.calculateTopPadding(),
            end = navBars.calculateEndPadding(layoutDir),
            bottom = navBars.calculateBottomPadding() + extraBottom
        )

        // ⬇️ 外層 Box 套「點空白區關閉鍵盤」
        Box(
            Modifier
                .fillMaxSize()
                .padding(inner)
                .clearFocusOnTapOutside()
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = listContentPadding
            ) {
                item {
                    OnboardingProgress(
                        stepIndex = 5,
                        totalSteps = 11,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                    )
                }
                item {
                    Text(
                        text = stringResource(R.string.onboard_weight_title),
                        style = MaterialTheme.typography.headlineLarge.copy(fontSize = 34.sp),
                        fontWeight = FontWeight.ExtraBold,
                        lineHeight = 40.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp)
                            .padding(top = 20.dp)
                    )
                }
                item { Spacer(Modifier.height(6.dp)) }
                item {
                    Text(
                        text = stringResource(R.string.onboard_weight_subtitle),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 16.sp,
                            lineHeight = 22.sp
                        ),
                        color = Color(0xFFB6BDC6),
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 6.dp)
                    )
                }
                item {
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        WeightUnitSegmented(
                            useMetric = useMetric,
                            onChange = { isMetric ->
                                useMetric = isMetric
                                text = if (text.isEmpty()) "" else
                                    formatOneDecimal(if (useMetric) valueKg else kgToLbs(valueKg))
                            },
                            modifier = Modifier.padding(top = 12.dp)
                        )
                    }
                }
                item { Spacer(Modifier.height(39.dp)) }

                // —— 輸入卡片 —— //
                item {
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Surface(
                            color = Color(0xFFF1F3F7),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth(0.88f)
                                .heightIn(min = 68.dp)
                        ) {
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                BasicTextField(
                                    value = text,
                                    onValueChange = { new ->
                                        val sanitized = sanitizeToOneDecimal(new)
                                        text = sanitized
                                        sanitized.toDoubleOrNull()?.let { v ->
                                            valueKg = if (useMetric) v else lbsToKg(v)
                                        }
                                    },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    textStyle = TextStyle(
                                        fontSize = 42.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFF111114),
                                        textAlign = TextAlign.Center
                                    ),
                                    decorationBox = { innerField ->
                                        Box(
                                            modifier = Modifier.widthIn(min = 84.dp, max = 148.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (text.isEmpty()) {
                                                Text(
                                                    "0",
                                                    fontSize = 42.sp,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = Color(0xFFB6BDC6),
                                                    textAlign = TextAlign.Center,
                                                    maxLines = 1
                                                )
                                            }
                                            innerField()
                                        }
                                    },
                                    modifier = Modifier
                                        .wrapContentWidth()
                                        .bringIntoViewRequester(bringIntoViewRequester)
                                        .onFocusEvent { f ->
                                            if (f.isFocused) {
                                                scope.launch {
                                                    // 等鍵盤動畫一下再滾動
                                                    delay(200)
                                                    bringIntoViewRequester.bringIntoView()
                                                }
                                            }
                                        }
                                )

                                Text(
                                    text = if (useMetric) "kg" else "lbs",
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF111114),
                                    maxLines = 1,
                                    softWrap = false,
                                    modifier = Modifier.offset(x = (-10).dp)
                                )
                            }
                        }
                    }
                }

                item {
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text(
                            text = stringResource(R.string.onboard_weight_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF9AA3AE),
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth(0.62f)
                                .padding(top = 16.dp)
                        )
                    }
                }

                item { Spacer(Modifier.height(16.dp)) }
            }
        }
    }
}

/** 黑底白字、等寬的 lbs / kg 分段（保持你的樣式） */
@Composable
private fun WeightUnitSegmented(
    useMetric: Boolean,
    onChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(26.dp),
        color = Color(0xFFF1F3F7),
        modifier = modifier
            .fillMaxWidth(0.58f)
            .heightIn(min = 45.dp)
    ) {
        Row(Modifier.padding(6.dp)) {
            SegItem(
                text = "lbs",
                selected = !useMetric,
                onClick = { onChange(false) },
                selectedColor = Color.Black,
                modifier = Modifier
                    .weight(1f)
                    .height(45.dp)
            )
            Spacer(Modifier.width(6.dp))
            SegItem(
                text = "kg",
                selected = useMetric,
                onClick = { onChange(true) },
                selectedColor = Color.Black,
                modifier = Modifier
                    .weight(1f)
                    .height(45.dp)
            )
        }
    }
}

@Composable
private fun SegItem(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    selectedColor: Color,
    modifier: Modifier = Modifier
) {
    val corner = 22.dp
    val minH = 48.dp
    val fSize = 22.sp

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(corner),
        color = if (selected) selectedColor else Color.Transparent,
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .defaultMinSize(minHeight = minH)
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 10.dp),
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

private const val MAX_INT_DIGITS = 3

/** 僅允許數字與一個小數點；整數最多 3 位、小數最多 1 位；允許空字串 */
private fun sanitizeToOneDecimal(s: String): String {
    if (s.isEmpty()) return ""
    var out = buildString {
        var dotSeen = false
        for (c in s) {
            if (c.isDigit()) append(c)
            else if (c == '.' && !dotSeen) {
                append('.'); dotSeen = true
            }
        }
    }
    if (out.startsWith(".")) out = "0$out"
    val dotIdx = out.indexOf('.')
    return if (dotIdx >= 0) {
        val intPart = out.substring(0, dotIdx).take(MAX_INT_DIGITS)
        val fracPart = out.substring(dotIdx + 1).take(1)
        if (fracPart.isEmpty()) "$intPart." else "$intPart.$fracPart"
    } else {
        out.take(MAX_INT_DIGITS)
    }
}

/** 顯示時固定到 1 位小數（你也可去掉 .0） */
private fun formatOneDecimal(v: Double): String {
    val s = String.format(java.util.Locale.US, "%.1f", round1(v))
    return s
}

/** 存檔用：公斤保留 2 位小數，避免 lbs↔kg 來回換算誤差 */
private fun roundKg2(v: Double): Float {
    return (kotlin.math.round(v * 100.0) / 100.0).toFloat()
}

/** 點擊空白處時清焦（鍵盤收起）；子元件有消費點擊時不會觸發 */
private fun Modifier.clearFocusOnTapOutside(): Modifier = composed {
    val focusManager = LocalFocusManager.current
    pointerInput(Unit) {
        detectTapGestures(onTap = { focusManager.clearFocus() })
    }
}