package com.calai.app.ui.onboarding.plan

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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.graphics.lerp
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.calai.app.R
import com.calai.app.core.health.BmiClass
import com.calai.app.core.health.Gender
import com.calai.app.core.health.HealthCalc
import com.calai.app.core.health.MacroPlan
import com.calai.app.data.profile.repo.UserProfileStore
import com.calai.app.data.profile.repo.kgToLbs1 // ★ 共用轉換工具
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.min
import kotlin.math.roundToInt

// === Colors（保持你的設定） ===
val NeutralText = Color(0xFF6B7280)
val RingTrack = Color(0xFFF0F2F6)
val CarbColor = Color(0xFFFBBC05)    // Amber 600
val ProteinColor = Color(0xFFEA4335) // Salmon/Red 400-500
val FatColor = Color(0xFF34A853)     // Emerald 500
val WaterColor = Color(0xFF3B82F6)   // 水量藍
val WeightColor = Color(0xFF6366F1)  // 體重紫
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

    val scroll = rememberScrollState()
    val scope = rememberCoroutineScope()

    // ✅ 防連點：避免快速連點導致重複導航/重複存檔
    var starting by remember { mutableStateOf(false) }

    // ✅ 回到前景時重置
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                starting = false
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
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
                        .padding(start = 20.dp, end = 20.dp, bottom = 20.dp)
                        .fillMaxWidth()
                        .height(64.dp)
                        .semantics {
                            stateDescription = if (starting) "loading" else "idle"
                        },
                    shape = RoundedCornerShape(999.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Black,
                        contentColor = Color.White,
                        disabledContainerColor = Color.Black.copy(alpha = 0.65f),
                        disabledContentColor = Color.White
                    )
                ) {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        if (starting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = Color.White
                            )
                        } else {
                            Text(
                                text = stringResource(R.string.plan_cta_start),
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Medium,
                                    letterSpacing = 0.2.sp
                                ),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    ) { inner ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
                .verticalScroll(scroll),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(3.dp))

            Text(
                text = stringResource(R.string.plan_title_congrats),
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(8.dp))

            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.plan_subtitle_ready),
                    color = NeutralText,
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center
                )
            }

            Box(Modifier.offset(y = (-10).dp)) {
                DonutMacros(
                    kcal = plan.kcal,
                    carbs = plan.carbsGrams,
                    protein = plan.proteinGrams,
                    fat = plan.fatGrams
                )
            }

            Box(Modifier.offset(y = (-14).dp)) {
                MacrosRings(plan)
            }

            Spacer(Modifier.height(16.dp))

            HydrationAndWeightRings(
                weightKg = inputs.weightKg,
                gender = inputs.gender,
                displayUnit = ui.displayUnit ?: weightUnit ?: UserProfileStore.WeightUnit.KG,
                displayWeight = ui.weightDisplay,
                displayGoal = ui.goalWeightDisplay
            )

            Spacer(Modifier.height(20.dp))
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.plan_edit_anytime),
                    color = NeutralText.copy(alpha = 0.85f),
                    fontSize = 13.sp,
                    modifier = Modifier.fillMaxWidth(0.55f),
                    textAlign = TextAlign.Center
                )
            }

            Spacer(Modifier.height(24.dp))
            BmiCard(
                bmi = plan.bmi,
                klass = plan.bmiClass,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 26.dp)
            )

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
    gender: Gender,
    displayUnit: UserProfileStore.WeightUnit,
    displayWeight: Float?,
    displayGoal: Float?
) {
    // ✅ Redundant curly braces 修正："$waterMl ml"
    val base = (35f * weightKg).roundToInt().coerceAtLeast(0)
    val cap = if (gender == Gender.Male) 3700 else 2700
    val waterMl = min(base, cap)

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

    val (deltaText, deltaProgress) =
        if (displayGoal == null || displayWeight == null) {
            "—" to 0f
        } else {
            val diff = delta1(displayGoal, displayWeight)
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
            centerText = "$waterMl ml",
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
    return (t10 - c10) / 10f
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

                drawArc(
                    color = RingTrack,
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    style = Stroke(width = stroke, cap = StrokeCap.Round),
                    size = arcSize,
                    topLeft = topLeft
                )

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
                text = centerText,
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
    val valueTone = BmiScale.colorAt(bmi)
    val klassTone = BmiPalette.colorOf(klass)

    val obesityStage = HealthCalc.obesityClass(bmi)
    val displayLabel = if (klass == BmiClass.Obesity && obesityStage != null) {
        val roman = when (obesityStage) {
            1 -> "I"
            2 -> "II"
            3 -> "III"
            else -> obesityStage.toString()
        }
        "$baseLabel class $roman"
    } else {
        baseLabel
    }

    val bmiBgBlue = Color(0xFFF3F8FF)

    Column(
        modifier
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, BmiBorder, RoundedCornerShape(16.dp))
            .background(bmiBgBlue)
            .padding(18.dp)
    ) {
        Text(
            text = stringResource(R.string.plan_bmi_index),
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                val bmiText = remember(bmi) { String.format(Locale.getDefault(), "%.1f", bmi) }
                Text(
                    bmiText,
                    fontSize = 42.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = valueTone
                )
                Text(
                    displayLabel,
                    color = valueTone,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(klassTone.copy(alpha = 0.15f))
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
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

    Spacer(Modifier.height(22.dp))
    Text(
        text = stringResource(R.string.plan_disclaimer),
        color = Color(0xFF9AA3AF),
        fontSize = 12.sp,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 48.dp) // 原本 32.dp → 改更大
    )

    Spacer(Modifier.height(20.dp))
    GoalsHowToSection(
        trackMealsIconRes = R.drawable.ic_dish2,
        mealBalanceIconRes = R.drawable.ic_meal_balance,
        bookIconRes = R.drawable.ic_book,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 34.dp),
        onSeeMore = { /* TODO */ }
    )
}

// ✅ 修正：modifier 應該放「可選參數」的第一個（且通常放最後參數列中第一個）
@Composable
fun GoalsHowToSection(
    @DrawableRes trackMealsIconRes: Int,
    @DrawableRes mealBalanceIconRes: Int,
    @DrawableRes bookIconRes: Int,
    modifier: Modifier = Modifier,
    onSeeMore: () -> Unit = {}
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFFFE4E0)),
                contentAlignment = Alignment.Center
            ) {
                Text("🎯", fontSize = 40.sp)
            }
            Spacer(Modifier.height(10.dp))
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.plan_goals_title),
                    fontSize = 32.sp,
                    fontWeight = FontWeight.ExtraBold,
                    lineHeight = 34.sp,
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        FeatureCard(
            titleRes = R.string.plan_goal_card_health_scores,
            emojiBg = Color(0xFFEAF7E9),
            modifier = Modifier.fillMaxWidth(),
            emoji = "❤️",
            emojiFontSp = 28
        )

        Spacer(Modifier.height(12.dp))

        FeatureCard(
            titleRes = R.string.plan_goal_card_track_meals,
            emojiBg = Color(0xFFE6F7F7),
            modifier = Modifier.fillMaxWidth(),
            iconRes = trackMealsIconRes,
            iconSize = 70.dp
        )

        Spacer(Modifier.height(12.dp))

        FeatureCard(
            titleRes = R.string.plan_goal_card_daily_calories,
            emojiBg = Color(0xFFFFF3DF),
            modifier = Modifier.fillMaxWidth(),
            emoji = "🔥",
            emojiFontSp = 31
        )

        Spacer(Modifier.height(12.dp))

        FeatureCard(
            titleRes = R.string.plan_goal_card_balance_macros,
            emojiBg = Color(0xFFEAF5FF),
            modifier = Modifier.fillMaxWidth(),
            iconRes = mealBalanceIconRes,
            iconSize = 56.dp
        )

        Spacer(Modifier.height(16.dp))

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
    emojiBg: Color,                          // ✅ 必填參數
    modifier: Modifier = Modifier,           // ✅ modifier 要是第一個「可選參數」
    @DrawableRes iconRes: Int? = null,
    emoji: String? = null,
    iconSize: Dp = 44.dp,
    emojiFontSp: Int = 30
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
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(emojiBg),
                contentAlignment = Alignment.Center
            ) {
                if (iconRes != null) {
                    Image(
                        painter = painterResource(iconRes),
                        contentDescription = null,
                        modifier = Modifier.size(iconSize)
                    )
                } else {
                    Text(emoji.orEmpty(), fontSize = emojiFontSp.sp)
                }
            }

            Spacer(Modifier.width(16.dp))

            Text(
                text = stringResource(titleRes),
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                lineHeight = 20.sp
            )
        }
    }
}

@Composable
private fun SourcesHeader(
    @DrawableRes bookIconRes: Int? = null,
    text: String,
    iconSize: Dp = 32.dp,
    maxTextWidth: Dp = 360.dp,
    nudgeLeft: Dp = 16.dp
) {
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier.offset(x = -nudgeLeft),
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

@Composable
fun ResearchSourcesBlock(
    @DrawableRes bookIconRes: Int,
    modifier: Modifier = Modifier,
    onSeeMore: () -> Unit = {}
) {
    val uriHandler = LocalUriHandler.current
    var expanded by rememberSaveable { mutableStateOf(false) }

    val links: List<Pair<String, String>> = listOf(
        "CDC – Adult BMI Categories" to "https://www.cdc.gov/bmi/adult-calculator/bmi-categories.html",
        "MyProtein – How to Calculate BMR & TDEE" to "https://us.myprotein.com/thezone/nutrition/how-to-calculate-bmr-tdee/",
        "US DRI – Water (National Academies)" to "https://nap.nationalacademies.org/read/10925/chapter/6",
        "EU – Food-Based Dietary Guidelines (Table 16)" to "https://knowledge4policy.ec.europa.eu/health-promotion-knowledge-gateway/food-based-dietary-guidelines-europe-table-16_en",
        "NIH/NCBI – DRI (Macronutrients/Water)" to "https://www.ncbi.nlm.nih.gov/books/NBK610333/"
    )

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        SourcesHeader(
            bookIconRes = bookIconRes,
            text = stringResource(R.string.plan_sources_based_on),
            iconSize = 32.dp,
            nudgeLeft = 16.dp
        )

        Spacer(Modifier.height(6.dp))

        val toggleLabel =
            if (expanded) stringResource(R.string.plan_sources_hide) else stringResource(R.string.plan_sources_more)

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
private fun kgToLbsFloor1(v: Float): Float =
    kgToLbs1(v.toDouble()).toFloat()

object BmiPalette {
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
}

private object BmiScale {
    private const val MIN = 15f
    private const val MAX = 35f

    private val stops: List<Pair<Float, Color>> = listOf(
        15f to Color(0xFF60A5FA),
        20f to Color(0xFF69BC8E),
        25f to Color(0xFFF9AE30),
        30f to Color(0xFFE87A3C),
        35f to Color(0xFFE83E56)
    )

    fun brush(): Brush {
        val colorStops = stops
            .map { (value, color) ->
                val p = ((value - MIN) / (MAX - MIN)).coerceIn(0f, 1f)
                p to color
            }
            .toTypedArray()

        return Brush.horizontalGradient(colorStops = colorStops)
    }

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
