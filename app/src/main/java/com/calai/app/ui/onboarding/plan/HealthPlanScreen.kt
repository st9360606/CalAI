package com.calai.app.ui.onboarding.plan

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
import java.util.Locale
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.roundToInt

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
    val targetWeightKg = ui.targetWeightKg
    val targetWeightUnit = ui.targetWeightUnit ?: weightUnit

    val scroll = rememberScrollState()

    Scaffold(
        containerColor = Color.White,
        bottomBar = {
            Box {
                Button(
                    onClick = onStart,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(start = 20.dp, end = 20.dp, bottom = 59.dp)
                        .fillMaxWidth()
                        .height(70.dp),
                    shape = RoundedCornerShape(28.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Black,
                        contentColor = Color.White
                    )
                ) {
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
            // 第二排：Water / Current Weight / Target Δ
            HydrationAndWeightRings(
                weightKg = inputs.weightKg,
                weightUnit = weightUnit,
                targetWeightKg = targetWeightKg,
                targetWeightUnit = targetWeightUnit
            )

            Spacer(Modifier.height(18.dp))
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.plan_edit_anytime),
                    color = NeutralText,
                    fontSize = 12.sp,
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
            .padding(horizontal = 12.dp),
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

/** 第二排：Water / Current Weight / Target Δ */
@Composable
private fun HydrationAndWeightRings(
    weightKg: Float,
    weightUnit: UserProfileStore.WeightUnit?,
    targetWeightKg: Float?,
    targetWeightUnit: UserProfileStore.WeightUnit?
) {
    // 以當前體重頁單位為唯一真相（null 視為 KG）
    val displayUnit = when (weightUnit) {
        UserProfileStore.WeightUnit.LBS -> UserProfileStore.WeightUnit.LBS
        else -> UserProfileStore.WeightUnit.KG
    }

    // 1) 飲水量：35 ml * kg
    val waterMl = (35f * weightKg).roundToInt()

    // 2) 當前體重（用 displayUnit）
    val (currText, currProgress) = when (displayUnit) {
        UserProfileStore.WeightUnit.LBS -> {
            val lbs = kgToLbs(weightKg)
            val lbsInt = lbs.roundToInt()
            "$lbsInt lbs" to min(lbsInt / 330f, 1f) // 330 lb 上限
        }
        UserProfileStore.WeightUnit.KG -> {
            String.format(Locale.getDefault(), "%.1f kg", weightKg) to
                    min(weightKg / 150f, 1f) // 150 kg 上限
        }
    }

    // 3) 目標差 Δ（完全用 displayUnit，不看 targetWeightUnit）
    val (deltaText, deltaProgress) =
        if (targetWeightKg == null) {
            "—" to 0f
        } else {
            val deltaKg = targetWeightKg - weightKg // 先用 kg 算差
            when (displayUnit) {
                UserProfileStore.WeightUnit.LBS -> {
                    val deltaLbs = kgToLbs(deltaKg)
                    val deltaInt = deltaLbs.roundToInt()
                    (signedInt(deltaInt) + " lbs") to min(abs(deltaInt).toFloat() / 44f, 1f) // ±44 lb
                }
                UserProfileStore.WeightUnit.KG -> {
                    (signed(deltaKg) + " kg") to min(abs(deltaKg) / 20f, 1f) // ±20 kg
                }
            }
        }

    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        MacroRingItem(
            title = stringResource(R.string.plan_water_daily),
            centerText = "${waterMl} ml",
            color = WaterColor,
            progress = min(waterMl / 4000f, 1f) // 4L 作為上限
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
            modifier = Modifier.size(84.dp),
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
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(title, color = NeutralText, fontSize = 14.sp, textAlign = TextAlign.Center)
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
    val tone = bmiClassColor(klass)

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
                Text(bmiText, fontSize = 42.sp, fontWeight = FontWeight.ExtraBold, color = tone)
                // 用 displayLabel（可能含 class）
                Text(displayLabel, color = tone, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(tone.copy(alpha = 0.15f))
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                // 「You are classified as %1$s」→ 帶入同一組文字
                val lower = displayLabel.lowercase(Locale.getDefault())
                Text(
                    text = stringResource(R.string.plan_bmi_classified_as, lower),
                    color = tone,
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
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            Color(0xFF60A5FA), // under
                            Color(0xFF69BC8E), // normal
                            Color(0xFFF9AE30), // over
                            Color(0xFFE83E56)  // obese
                        )
                    )
                )
        )
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            listOf(
                stringResource(R.string.plan_bmi_tick_18),
                stringResource(R.string.plan_bmi_tick_25),
                stringResource(R.string.plan_bmi_tick_30),
                stringResource(R.string.plan_bmi_tick_35),
                stringResource(R.string.plan_bmi_tick_40)
            ).forEach { m ->
                Text(m, color = NeutralText, fontSize = 12.sp)
            }
        }
    }

    Spacer(Modifier.height(12.dp))
    Text(
        text = stringResource(R.string.plan_disclaimer),
        color = NeutralText,
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

@Composable
private fun bmiClassColor(klass: BmiClass): Color = when (klass) {
    BmiClass.Underweight -> Color(0xFF60A5FA)
    BmiClass.Normal -> BmiGood
    BmiClass.Overweight -> Color(0xFFF59E0B)
    BmiClass.Obesity -> Color(0xFFEF4444)
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
                color = NeutralText,
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

// === Block：toggle 置中（不變），links 左對齊 24dp（不變） ===
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
                color = NeutralText,
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
                    .padding(horizontal = 16.dp) // 保留些微左右留白
                    .testTag("sources_links"),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                links.forEach { (label, url) ->
                    Text(
                        text = label,
                        color = Color.Black,
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
private fun kgToLbs(kg: Float): Float = kg * 2.2046226f
private fun kgToLbs(kg: Double): Double = kg * 2.20462262185
private fun signed(v: Float): String =
    if (v >= 0f) "+${String.format(Locale.getDefault(), "%.1f", v)}"
    else String.format(Locale.getDefault(), "%.1f", v)

private fun signedInt(v: Int): String =
    if (v >= 0) "+$v" else "$v"
