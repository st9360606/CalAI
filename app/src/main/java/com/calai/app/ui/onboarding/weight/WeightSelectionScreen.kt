package com.calai.app.ui.onboarding.weight

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.calai.app.R
import com.calai.app.ui.common.OnboardingProgress
import com.calai.app.ui.onboarding.height.feetInchesToCm

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun WeightSelectionScreen(
    vm: WeightSelectionViewModel,
    onBack: () -> Unit,
    onNext: () -> Unit
) {
    val weightKg by vm.weightKgState.collectAsState()

    var useMetric by remember { mutableStateOf(true) }   // true=kg, false=lbs
    var valueKg by remember(weightKg) { mutableStateOf(weightKg.toDouble()) }

    // A) 初始化：若儲存值為 0，或之後被清空 → 維持 ""（讓你自己已有的 placeholder 能顯示）
    var text by remember(weightKg, useMetric) {
        mutableStateOf(
            if (weightKg == 0f) "" else
                formatOneDecimal(if (useMetric) valueKg else kgToLbs(valueKg))
        )
    }

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
            Box(
            ) {
                Button(
                    onClick = {
                        // C) 送出：空字串視為 0
                        val kgToSave = if (text.isEmpty()) 0.0 else valueKg
                        vm.saveWeightKg(round1(kgToSave).toFloat())
                        onNext()
                    },
                    enabled = true,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        // 1) 讓 CTA 永遠避開系統導覽列（手勢列）
                        .navigationBarsPadding() // 先避開手勢列
                        // 2) 額外再往上推一點（你要的「再往上一點」）
                        .padding(start = 20.dp, end = 20.dp, bottom = 59.dp)
                        .fillMaxWidth()
                        .height(64.dp),
                    shape = RoundedCornerShape(28.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Black,
                        contentColor = Color.White
                    )
                ) {
                    Text(
                        text = stringResource(R.string.continue_text),
                        fontSize = 19.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    ) { inner ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
                .imePadding(),
        ) {
            OnboardingProgress(
                stepIndex = 5,
                totalSteps = 11,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
            )
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

            Spacer(Modifier.height(6.dp))

            Text(
                text = stringResource(R.string.onboard_weight_subtitle),
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 16.sp, lineHeight = 22.sp),
                color = Color(0xFFB6BDC6),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 6.dp)
            )

            WeightUnitSegmented(
                useMetric = useMetric,
                onChange = { isMetric ->
                    // A) 切換單位：若目前為空字串，就保持空；否則依新單位格式化
                    useMetric = isMetric
                    text = if (text.isEmpty()) "" else
                        formatOneDecimal(if (useMetric) valueKg else kgToLbs(valueKg))
                },
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(top = 12.dp)
            )

            Spacer(Modifier.height(39.dp))

            // 大卡片輸入：最多 1 位小數
            Surface(
                color = Color(0xFFF1F3F7),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .fillMaxWidth(0.88f)     // ← 原本是 fillMaxWidth()，縮窄整個輸入框
                    .heightIn(min = 68.dp)   // ← 原 82.dp，高度更小
            ) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),    // ← 內距縮小（原 20.dp）
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BasicTextField(
                        value = text,
                        onValueChange = { new ->
                            val sanitized = sanitizeToOneDecimal(new)   // ← 用第2段的新函式
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
                        // 🔽 空字串時顯示「0」的浮水印
                        decorationBox = { inner ->
                            Box(
                                modifier = Modifier
                                    .widthIn(min = 84.dp, max = 148.dp), // ← 比之前更小、也設上限
                                contentAlignment = Alignment.Center
                            ) {
                                if (text.isEmpty()) {
                                    Text(
                                        "0",
                                        fontSize = 42.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFFB6BDC6), // 淡灰
                                        textAlign = TextAlign.Center,
                                        maxLines = 1
                                    )
                                }
                                inner()
                            }
                        },
                        modifier = Modifier.wrapContentWidth()
                    )

                    Text(
                        text = if (useMetric) "kg" else "lbs",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF111114),
                        maxLines = 1,
                        softWrap = false,                  // 避免出現「k↵g」
                        modifier = Modifier.offset(x = (-10).dp)
                    )
                }
            }
            // 小提示（變窄＋置中＋自動換行）
            Text(
                text = stringResource(R.string.onboard_weight_hint),
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF9AA3AE),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally) // 先把自身置中
                    .fillMaxWidth(0.62f)                  // 只佔 72% 寬（可調 0.6f~0.8f）
                    .padding(top = 16.dp)
            )
        }
    }
}

/** 黑底白字、等寬的 lbs / kg 分段 */
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

    // 留下數字與第一個小數點
    var out = buildString {
        var dotSeen = false
        for (c in s) {
            if (c.isDigit()) append(c)
            else if (c == '.' && !dotSeen) { append('.'); dotSeen = true }
        }
    }

    // 以免以 "." 開頭
    if (out.startsWith(".")) out = "0$out"

    val dotIdx = out.indexOf('.')
    return if (dotIdx >= 0) {
        val intPart = out.substring(0, dotIdx).take(MAX_INT_DIGITS)
        val fracPart = out.substring(dotIdx + 1).take(1) // 只留 1 位小數
        if (fracPart.isEmpty()) "$intPart." else "$intPart.$fracPart"
    } else {
        // 沒小數點 → 限制整數長度
        out.take(MAX_INT_DIGITS)
    }
}

/** 顯示時固定到 1 位小數（去尾 0 也 OK） */
private fun formatOneDecimal(v: Double): String {
    val rounded = round1(v)
    val s = String.format(java.util.Locale.US, "%.1f", rounded)
    // 可選：去掉尾端 .0 想保留就註解下一行
    // return if (s.endsWith(".0")) s.dropLast(2) else s
    return s
}
