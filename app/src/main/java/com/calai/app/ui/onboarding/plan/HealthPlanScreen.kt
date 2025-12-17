package com.calai.app.ui.onboarding.plan

import android.util.Log
import androidx.compose.ui.text.style.TextOverflow
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.calai.app.R
import com.calai.app.core.health.BmiClass
import com.calai.app.core.health.HealthCalc
import com.calai.app.core.health.MacroPlan
import com.calai.app.data.profile.repo.UserProfileStore
import com.calai.app.data.profile.repo.kgToLbs1 // ★ 共用轉換工具
import java.util.Locale
import kotlin.math.min
import kotlin.math.roundToInt
import androidx.compose.ui.graphics.lerp
import com.calai.app.core.health.Gender
import kotlinx.coroutines.launch
import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

// === Colors（保持你的設定） ===
val PrimaryGreen = Color(0xFF59B34C)
val NeutralText = Color(0xFF6B7280)
val RingTrack = Color(0xFFF0F2F6)
val CarbColor = Color(0xFFFBBC05) // Amber 600
val ProteinColor = Color(0xFFEA4335) // Salmon/Red 400-500
val FatColor = Color(0xFF34A853) // Emerald 500
val WaterColor = Color(0xFF3B82F6) // 水量藍
val WeightColor = Color(0xFF6366F1) // 體重紫
val BmiGood = Color(0xFF2E7D32)
val BmiBorder = Color(0xFFDCEFE0)

// === 圓環粗細 ===
private const val DONUT_STROKE_PX = 80f     // 大圓
private const val MINI_RING_STROKE_PX = 20f // 小圓

@Composable
fun HealthPlanScreen(
    vm: HealthPlanViewModel,
    onStart: () -> Unit
) {
    val ui = vm.ui.collectAsStateWithLifecycle().value
    if (ui.loading || ui.plan == null || ui.inputs == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }
    val plan = ui.plan
    val inputs = ui.inputs
    val weightUnit = ui.weightUnit
    val goalWeightKg = ui.goalWeightKg
    val goalWeightUnit = ui.goalWeightUnit ?: weightUnit

    val scroll = rememberScrollState()
    val scope = rememberCoroutineScope()
// ✅ 防連點：避免快速連點導致重複導航/重複存檔
    var starting by remember { mutableStateOf(false) }

    // ✅ 關鍵：當這個 destination「重新回到前景」(ON_RESUME) 時，重置 loading 狀態
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                // 從 Gate / 其他頁 pop 回來：允許再次按「開始使用」
                starting = false
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Scaffold(
        containerColor = Color.White,
        bottomBar = {
            Box {
                Button(
                    onClick = {
                        if (starting) return@Button
                        starting = true
                        scope.launch {
                            runCatching { onStart() }
                            .onFailure { starting = false }
                        }
                    },
                    enabled = !starting,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(start = 20.dp, end = 20.dp, bottom = 59.dp)
                        .fillMaxWidth()
                        .height(70.dp),
                    shape = RoundedCornerShape(28.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Black,
                        contentColor = Color.White,
                        disabledContainerColor = Color.Black.copy(alpha = 0.65f),
                        disabledContentColor = Color.White
                    )
                ) {
                    if (starting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = Color.White
                        )
                        Spacer(Modifier.width(10.dp))
                    }
                    Text(
                        text = stringResource(R.string.plan_cta_start),
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
                .verticalScroll(scroll)
        ) {
            // ↑ 標題往下移一點（比原本更低）
            Spacer(Modifier.height(8.dp))

            Text(
                stringResource(R.string.plan_title_congrats),
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(R.string.plan_subtitle_ready),
                color = NeutralText,
                fontSize = 18.sp,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )

            // ↓ 副標題與大圓距離變「更近」
            Box(Modifier.offset(y = (-10).dp)) {
                DonutMacros(
                    kcal = plan.kcal,
                    carbs = plan.carbsGrams,
                    protein = plan.proteinGrams,
                    fat = plan.fatGrams
                )
            }

            // 第一排：Carbs / Protein / Fat
            Box(Modifier.offset(y = (-20).dp)) {
                MacrosRings(plan)
            }

            Spacer(Modifier.height(16.dp))
            // 第二排：Water / Current Weight / Goal Δ
            HydrationAndWeightRings(
                weightKg = inputs.weightKg,
                gender = inputs.gender,
                displayUnit = ui.displayUnit ?: weightUnit ?: UserProfileStore.WeightUnit.KG,
                displayWeight = ui.weightDisplay,
                displayGoal = ui.goalWeightDisplay
            )

            Spacer(Modifier.height(18.dp))
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center   // ★ 用二維 Alignment，水平靠左、垂直置中
            ) {
                Text(
                    text = stringResource(R.string.plan_edit_anytime),
                    color = Color(0xFF9AA3AF),
                    fontSize = 12.sp,
                    modifier = Modifier.fillMaxWidth(0.55f),
                    textAlign = TextAlign.Center             // ★ 文字也用 Start，比較自然
                )
            }

            Spacer(Modifier.height(24.dp))
            BmiCard(
                bmi = plan.bmi,
                klass = plan.bmiClass,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 26.dp) // ← 和小圓進度條一致的左右內距
            )
            // 留一點結尾空白（內容可滑，不會被底部按鈕遮住）
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun DonutMacros(
    kcal: Int,
    carbs: Int,
    protein: Int,
    fat: Int
) {
    val carbsK = carbs * 4f
    val proteinK = protein * 4f
    val fatK = fat * 9f
    val total = (carbsK + proteinK + fatK).coerceAtLeast(1f)

    val carbsPct = carbsK / total
    val proteinPct = proteinK / total
    val fatPct = fatK / total

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(260.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.size(200.dp)) {
            val stroke = DONUT_STROKE_PX
            val padding = stroke / 2
            val arcSize = Size(size.width - stroke, size.height - stroke)
            val topLeft = Offset(padding, padding)

            drawArc(
                color = RingTrack,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
                size = arcSize,
                topLeft = topLeft
            )

            var start = -90f
            fun seg(color: Color, pct: Float) {
                if (pct <= 0f) return
                val sweep = 360f * pct
                drawArc(
                    color = color,
                    startAngle = start,
                    sweepAngle = sweep,
                    useCenter = false,
                    style = Stroke(width = stroke, cap = StrokeCap.Round),
                    size = arcSize,
                    topLeft = topLeft
                )
                start += sweep
            }
            seg(CarbColor, carbsPct)
            seg(FatColor, fatPct)
            seg(ProteinColor, proteinPct)
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(kcal.toString(), fontSize = 44.sp, fontWeight = FontWeight.ExtraBold)
            Text(stringResource(R.string.plan_unit_kcal_day), color = NeutralText, fontSize = 18.sp)
        }
    }
}

/** 第一排：P/C/F 三個小圓形進度條 */
@Composable
private fun MacrosRings(plan: MacroPlan) {
    val carbsK = plan.carbsGrams * 4f
    val proteinK = plan.proteinGrams * 4f
    val fatK = plan.fatGrams * 9f
    val total = (carbsK + proteinK + fatK).coerceAtLeast(1f)

    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        MacroRingItem(
            title = stringResource(R.string.plan_macros_proteins),
            centerText = "${plan.proteinGrams}${stringResource(R.string.plan_unit_g)}",
            color = ProteinColor,
            progress = proteinK / total
        )
        MacroRingItem(
            title = stringResource(R.string.plan_macros_carbs),
            centerText = "${plan.carbsGrams}${stringResource(R.string.plan_unit_g)}",
            color = CarbColor,
            progress = carbsK / total
        )
        MacroRingItem(
            title = stringResource(R.string.plan_macros_fat),
            centerText = "${plan.fatGrams}${stringResource(R.string.plan_unit_g)}",
            color = FatColor,
            progress = fatK / total
        )
    }
}

/** 第二排：Water / Current Weight / Goal Δ */
@Composable
private fun HydrationAndWeightRings(
    weightKg: Float,
    gender: Gender, // ✅ 新增
    displayUnit: UserProfileStore.WeightUnit,
    displayWeight: Float?,
    displayGoal: Float?
) {
    // ✅ 跟後端一致：round(kg*35) + 性別上限
    val base = (35f * weightKg).roundToInt().coerceAtLeast(0)
    val cap = if (gender == Gender.Male) 3700 else 2700
    val waterMl = min(base, cap)

    // 2) current 顯示
    val (currText, currProgress) = when (displayUnit) {
        UserProfileStore.WeightUnit.LBS -> {
            val lbs = displayWeight ?: kgToLbsFloor1(weightKg)
            val text = String.format(Locale.getDefault(), "%.1f lbs", lbs)
            val progress = min(lbs / 330f, 1f)
            text to progress
        }
        UserProfileStore.WeightUnit.KG -> {
            val kg = displayWeight ?: ((weightKg * 10f).toInt() / 10f)
            val text = String.format(Locale.getDefault(), "%.1f kg", kg)
            val progress = min(kg / 150f, 1f)
            text to progress
        }
    }

    // 3) delta 顯示：直接用「顯示用的數字」算
    val (deltaText, deltaProgress) =
        if (displayGoal == null || displayWeight == null) {
            "—" to 0f
        } else {
            val diff = delta1(displayGoal, displayWeight)  // goal - current
            val unitStr = if (displayUnit == UserProfileStore.WeightUnit.LBS) "lbs" else "kg"
            val abs = kotlin.math.abs(diff)
            val full = if (displayUnit == UserProfileStore.WeightUnit.LBS) 44f else 20f
            val progress = min(abs / full, 1f)
            String.format(Locale.getDefault(), "%.1f %s", diff, unitStr) to progress
        }

    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        MacroRingItem(
            title = stringResource(R.string.plan_water_daily),
            centerText = "${waterMl} ml",
            color = WaterColor,
            progress = min(waterMl / 4000f, 1f)
        )
        MacroRingItem(
            title = stringResource(R.string.plan_weight_current),
            centerText = currText,
            color = WeightColor,
            progress = currProgress
        )
        MacroRingItem(
            title = stringResource(R.string.plan_weight_delta),
            centerText = deltaText,
            color = WeightColor.copy(alpha = 0.50f),
            progress = deltaProgress
        )
    }
}

// ★ 工具：用「x10 再整數」確保 0.1 精度的差值
private fun delta1(goal: Float, current: Float): Float {
    val t10 = (goal * 10f).roundToInt()
    val c10 = (current * 10f).roundToInt()
    val diff10 = t10 - c10
    return diff10 / 10f
}

/** 小圓形進度條（通用版本） */
@Composable
private fun MacroRingItem(
    title: String,
    centerText: String,
    color: Color,
    progress: Float
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier.size(92.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(Modifier.matchParentSize()) {
                val stroke = MINI_RING_STROKE_PX
                val padding = stroke / 2
                val arcSize = Size(size.width - stroke, size.height - stroke)
                val topLeft = Offset(padding, padding)
                // 軌道
                drawArc(
                    color = RingTrack,
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    style = Stroke(width = stroke, cap = StrokeCap.Round),
                    size = arcSize,
                    topLeft = topLeft
                )
                // 進度
                val sweep = 360f * progress.coerceIn(0f, 1f)
                drawArc(
                    color = color,
                    startAngle = -90f,
                    sweepAngle = sweep,
                    useCenter = false,
                    style = Stroke(width = stroke, cap = StrokeCap.Round),
                    size = arcSize,
                    topLeft = topLeft
                )
            }
            Text(
                centerText,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(title, color = NeutralText, fontSize = 12.sp, textAlign = TextAlign.Center)
    }
}

@Composable
private fun BmiCard(
    bmi: Double,
    klass: BmiClass,
    modifier: Modifier = Modifier
) {
    val labelRes = when (klass) {
        BmiClass.Underweight -> R.string.plan_bmi_label_underweight
        BmiClass.Normal -> R.string.plan_bmi_label_normal
        BmiClass.Overweight -> R.string.plan_bmi_label_overweight
        BmiClass.Obesity -> R.string.plan_bmi_label_obesity
    }
    val baseLabel = stringResource(labelRes)
    // 文字顏色：跟彩條同位置一致（你要的效果）
    val valueTone = BmiScale.colorAt(bmi)

    // 分類顏色：語意用（膠囊用這個比較合理）
    val klassTone = BmiPalette.colorOf(klass)


    // 依 BMI 顯示「Obesity class I/II/III」
    val obesityStage = HealthCalc.obesityClass(bmi)
    val displayLabel = if (klass == BmiClass.Obesity && obesityStage != null) {
        val roman = when (obesityStage) {
            1 -> "I"
            2 -> "II"
            3 -> "III"
            else -> obesityStage.toString()
        }
        // 文字而已，不改排版
        "$baseLabel class $roman"
    } else {
        baseLabel
    }

    // 保持你原本的視覺設定（不動排版）
    val bmiBgBlue = Color(0xFFF3F8FF)

    Column(
        modifier
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, BmiBorder, RoundedCornerShape(16.dp))
            .background(bmiBgBlue)
            .padding(18.dp)
    ) {
        Text(
            stringResource(R.string.plan_bmi_index),
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                val bmiText = remember(bmi) { String.format(Locale.getDefault(), "%.1f", bmi) }
                Text(bmiText, fontSize = 42.sp, fontWeight = FontWeight.ExtraBold, color = valueTone)
                // 用 displayLabel（可能含 class）
                Text(displayLabel, color = valueTone, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(klassTone.copy(alpha = 0.15f))
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                // 「You are classified as %1$s」→ 帶入同一組文字
                val lower = displayLabel.lowercase(Locale.getDefault())
                Text(
                    text = stringResource(R.string.plan_bmi_classified_as, lower),
                    color = klassTone,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Spacer(Modifier.height(10.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(BmiScale.brush())
        )
        Spacer(Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            listOf("15", "20", "25", "30", "35").forEach { m ->
                Text(
                    text = m,
                    color = NeutralText,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }

    Spacer(Modifier.height(12.dp))
    Text(
        text = stringResource(R.string.plan_disclaimer),
        color = Color(0xFF9AA3AF),
        fontSize = 12.sp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp)
    )

    Spacer(Modifier.height(16.dp))
    GoalsHowToSection(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 34.dp),
        trackMealsIconRes = R.drawable.ic_dish2,
        mealBalanceIconRes = R.drawable.ic_meal_balance,
        bookIconRes = R.drawable.ic_book,
        onSeeMore = { /* TODO */ }
    )
}

// === 「How to reach your goals」整段區塊 ===
@Composable
fun GoalsHowToSection(
    modifier: Modifier = Modifier,
    @DrawableRes trackMealsIconRes: Int,
    @DrawableRes mealBalanceIconRes: Int,
    @DrawableRes bookIconRes: Int,           // 仍保留給 ResearchSourcesBlock 使用
    onSeeMore: () -> Unit = {}
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 🎯 標題（更大、置中）
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(72.dp)                        // 箭靶容器
                    .clip(CircleShape)
                    .background(Color(0xFFFFE4E0)),
                contentAlignment = Alignment.Center
            ) {
                Text("🎯", fontSize = 40.sp)            // 箭靶更大
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text = stringResource(R.string.plan_goals_title),
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                lineHeight = 34.sp,
                textAlign = TextAlign.Center
            )
        }

        Spacer(Modifier.height(16.dp))

        // 1) 健康評分（❤️ emoji，略放大）
        FeatureCard(
            titleRes = R.string.plan_goal_card_health_scores,
            emoji = "❤️",
            emojiBg = Color(0xFFEAF7E9),
            modifier = Modifier.fillMaxWidth(),
            emojiFontSp = 28                    // ❤️ 放大
        )

        Spacer(Modifier.height(12.dp))

        // 2) 輕鬆記錄餐點（🍽️ emoji，維持預設或可微調）
        FeatureCard(
            titleRes = R.string.plan_goal_card_track_meals,
            iconRes = trackMealsIconRes,
            emojiBg = Color(0xFFE6F7F7),
            modifier = Modifier.fillMaxWidth(),
            iconSize = 70.dp
        )

        Spacer(Modifier.height(12.dp))

        // 3) 每日熱量（🔥 emoji，再放大）
        FeatureCard(
            titleRes = R.string.plan_goal_card_daily_calories,
            emoji = "🔥",
            emojiBg = Color(0xFFFFF3DF),
            modifier = Modifier.fillMaxWidth(),
            emojiFontSp = 31                    // 🔥 再更大
        )

        Spacer(Modifier.height(12.dp))

        // 4) 平衡營養素（改用 PNG 檔，並放大圖示）
        FeatureCard(
            titleRes = R.string.plan_goal_card_balance_macros,
            iconRes = mealBalanceIconRes,       // ← 使用你的 PNG 餐盤圖
            emojiBg = Color(0xFFEAF5FF),
            modifier = Modifier.fillMaxWidth(),
            iconSize = 56.dp                    // ← PNG 放大
        )

        Spacer(Modifier.height(16.dp))

        // 研究來源區塊（保留你的原用法）
        ResearchSourcesBlock(
            bookIconRes = bookIconRes,
            modifier = Modifier.fillMaxWidth(),
            onSeeMore = onSeeMore
        )
    }
}

@Composable
private fun FeatureCard(
    @StringRes titleRes: Int,
    @DrawableRes iconRes: Int? = null,    // 有 PNG 就用 PNG
    emoji: String? = null,                // 否則用 emoji
    emojiBg: Color,
    modifier: Modifier = Modifier,
    iconSize: Dp = 44.dp,                 // PNG 大小可調
    emojiFontSp: Int = 30                 // emoji 大小可調
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = Color.White,
        shadowElevation = 8.dp,
        modifier = modifier
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 左側圖塊（背景色塊 + 圖標/emoji）
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(emojiBg),
                contentAlignment = Alignment.Center
            ) {
                if (iconRes != null) {
                    // 使用 PNG 圖
                    Image(
                        painter = painterResource(iconRes),
                        contentDescription = null,
                        modifier = Modifier.size(iconSize)
                    )
                } else {
                    // 使用 emoji
                    Text(emoji.orEmpty(), fontSize = emojiFontSp.sp)
                }
            }
            Spacer(Modifier.width(16.dp))

            // 右側文字
            Text(
                text = stringResource(titleRes),
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                lineHeight = 20.sp
            )
        }
    }
}

// === 書本＋文字（置中），圖示放大到 28dp（可改 32.dp） ===
@Composable
private fun SourcesHeader(
    @DrawableRes bookIconRes: Int? = null,
    text: String,
    iconSize: Dp = 32.dp,          // ← 放大（可改 28/34/36）
    maxTextWidth: Dp = 360.dp,
    nudgeLeft: Dp = 16.dp          // ← 往左靠一點的幅度（可改 12–16.dp）
) {
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center  // 整體仍以中線對齊
    ) {
        Row(
            modifier = Modifier.offset(x = -nudgeLeft), // ← 微移到左側
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (bookIconRes != null) {
                Image(
                    painter = painterResource(bookIconRes),
                    contentDescription = null,
                    modifier = Modifier.size(iconSize),
                    alpha = 0.9f
                )
            }
            Spacer(Modifier.width(10.dp))
            Text(
                text = text,
                color = Color(0xFF9AA3AF),
                fontSize = 13.sp,
                lineHeight = 18.sp,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.widthIn(max = maxTextWidth)
            )
        }
    }
}

// === Block：toggle 置中，links 置中 ===
@Composable
fun ResearchSourcesBlock(
    @DrawableRes bookIconRes: Int,
    modifier: Modifier = Modifier,
    onSeeMore: () -> Unit = {}
) {
    val uriHandler = LocalUriHandler.current
    var expanded by rememberSaveable { mutableStateOf(false) }

    val links: List<Pair<String, String>> = listOf(
        "CDC – Adult BMI Categories" to
                "https://www.cdc.gov/bmi/adult-calculator/bmi-categories.html?utm_source=chatgpt.com",
        "MyProtein – How to Calculate BMR & TDEE" to
                "https://us.myprotein.com/thezone/nutrition/how-to-calculate-bmr-tdee/?utm_source=chatgpt.com",
        "US DRI – Water (National Academies)" to
                "https://nap.nationalacademies.org/read/10925/chapter/6?utm_source=chatgpt.com",
        "EU – Food-Based Dietary Guidelines (Table 16)" to
                "https://knowledge4policy.ec.europa.eu/health-promotion-knowledge-gateway/food-based-dietary-guidelines-europe-table-16_en?utm_source=chatgpt.com",
        "NIH/NCBI – DRI (Macronutrients/Water)" to
                "https://www.ncbi.nlm.nih.gov/books/NBK610333/?utm_source=chatgpt.com"
    )

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.Start
    ) {
        SourcesHeader(
            bookIconRes = bookIconRes,
            text = stringResource(R.string.plan_sources_based_on),
            iconSize = 32.dp,
            nudgeLeft = 16.dp
        )

        Spacer(Modifier.height(6.dp))

        val toggleLabel = if (expanded) stringResource(R.string.plan_sources_hide) else stringResource(R.string.plan_sources_more)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("sources_toggle")
                .semantics {
                    role = Role.Button
                    stateDescription = if (expanded) "expanded" else "collapsed"
                },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = toggleLabel,
                color = Color(0xFF9AA3AF),
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.clickable {
                    val next = !expanded
                    expanded = next
                    if (next) onSeeMore()
                }
            )
        }

        Spacer(Modifier.height(8.dp))

        // ✅ 展開後的連結「置中」
        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(animationSpec = tween(180)) + fadeIn(tween(180)),
            exit = shrinkVertically(animationSpec = tween(160)) + fadeOut(tween(120))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .testTag("sources_links"),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                links.forEach { (label, url) ->
                    Text(
                        text = label,
                        color = Color(0xFF9AA3AF),
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        textAlign = TextAlign.Center,
                        style = TextStyle(textDecoration = TextDecoration.Underline),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable { uriHandler.openUri(url) }
                    )
                }
            }
        }
    }
}

// === Utils ===
// 使用 DataStore 共用的 lbs 換算（1kg = 2.2lbs，無條件捨去到 0.1）
private fun kgToLbsFloor1(v: Float): Float =
    kgToLbs1(v.toDouble()).toFloat()

// Δ（目標 - 現在），結果一律無條件捨去到 0.1 lbs
private fun lbsDiffFloor1(currKg: Float, goalKg: Float): Float {
    val currLbs = kgToLbsFloor1(currKg)
    val goalLbs = kgToLbsFloor1(goalKg)
    val diff = goalLbs - currLbs
    return ((diff * 10f).toInt() / 10f)
}


object BmiPalette {
    // ✅ 這四個顏色要「同時」供：BMI數字/label/膠囊/進度條 使用
    val Under = Color(0xFF60A5FA)
    val Normal = Color(0xFF69BC8E)
    val Over = Color(0xFFF9AE30)
    val Obese = Color(0xFFE83E56)

    fun colorOf(klass: BmiClass): Color = when (klass) {
        BmiClass.Underweight -> Under
        BmiClass.Normal -> Normal
        BmiClass.Overweight -> Over
        BmiClass.Obesity -> Obese
    }

    val GradientColors: List<Color> = listOf(Under, Normal, Over, Obese)
}

private object BmiScale {
    // ✅ 新刻度範圍
    private const val MIN = 15f
    private const val MAX = 35f

    // ✅ 以刻度當 stop：15/20/25/30/35
    private val stops: List<Pair<Float, Color>> = listOf(
        15f to Color(0xFF60A5FA), // 15: blue
        20f to Color(0xFF69BC8E), // 20: green
        25f to Color(0xFFF9AE30), // 25: yellow/orange
        30f to Color(0xFFE87A3C), // 30: orange
        35f to Color(0xFFE83E56)  // 35: red
    )

    /** 給彩條用：位置對齊刻度的 Gradient */
    fun brush(): Brush {
        val colorStops = stops
            .map { (value, color) ->
                val p = ((value - MIN) / (MAX - MIN)).coerceIn(0f, 1f)
                p to color
            }
            .toTypedArray()

        return Brush.horizontalGradient(colorStops = colorStops)
    }

    /** 給 BMI 數字用：在同一套 stops 內插值取色 */
    fun colorAt(bmi: Double): Color {
        val v = bmi.toFloat().coerceIn(MIN, MAX)

        val idx = stops.indexOfLast { it.first <= v }.coerceAtLeast(0)
        if (idx >= stops.lastIndex) return stops.last().second

        val (aV, aC) = stops[idx]
        val (bV, bC) = stops[idx + 1]

        val t = ((v - aV) / (bV - aV)).coerceIn(0f, 1f)
        return lerp(aC, bC, t)
    }
}

