package com.calai.app.ui.onboarding.plan

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.calai.app.core.health.BmiClass
import com.calai.app.core.health.MacroPlan
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import com.calai.app.R
import java.util.Locale

// 建議放 Theme 或此檔內：更容易之後統一替換
val PrimaryGreen = androidx.compose.ui.graphics.Color(0xFF59B34C) // CTA & 正向
val NeutralText = androidx.compose.ui.graphics.Color(0xFF6B7280)
val RingTrack = androidx.compose.ui.graphics.Color(0xFFF0F2F6)
val CarbColor = androidx.compose.ui.graphics.Color(0xFFF59E0B)    // Amber 600
val ProteinColor = androidx.compose.ui.graphics.Color(0xFFEF5350) // Salmon/Red 400-500
val FatColor = androidx.compose.ui.graphics.Color(0xFF22C55E)     // Emerald 500
val BmiGood = androidx.compose.ui.graphics.Color(0xFF2E7D32)
val BmiBorder = androidx.compose.ui.graphics.Color(0xFFDCEFE0)

@Composable
fun HealthPlanScreen(
    vm: HealthPlanViewModel,
    onStart: () -> Unit
) {
    val ui = vm.ui.collectAsStateWithLifecycle().value
    if (ui.loading || ui.plan == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }
    val plan = ui.plan

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(horizontal = 24.dp)
    ) {
        Spacer(Modifier.height(28.dp))
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

        Spacer(Modifier.height(28.dp))

        DonutMacros(
            kcal = plan.kcal,
            carbs = plan.carbsGrams,
            protein = plan.proteinGrams,
            fat = plan.fatGrams
        )

        Spacer(Modifier.height(8.dp))
        MacrosRow(plan)

        Spacer(Modifier.height(18.dp))
        Text(
            stringResource(R.string.plan_edit_anytime),
            color = NeutralText,
            fontSize = 16.sp,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(24.dp))
        BmiCard(plan.bmi, plan.bmiClass)

        Spacer(Modifier.weight(1f))

        Button(
            onClick = onStart,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(bottom = 18.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
            shape = RoundedCornerShape(28.dp)
        ) {
            Text(stringResource(R.string.plan_cta_start), fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
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
        Canvas(Modifier.size(220.dp)) {
            val stroke = 28f
            val padding = stroke / 2
            val arcSize = Size(size.width - stroke, size.height - stroke)
            val topLeft = Offset(padding, padding)

            // 背景軌
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
            seg(ProteinColor, proteinPct)
            seg(FatColor, fatPct)
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(kcal.toString(), fontSize = 44.sp, fontWeight = FontWeight.ExtraBold)
            Text(stringResource(R.string.plan_unit_kcal_day), color = NeutralText, fontSize = 18.sp)
        }
    }
}

@Composable
private fun MacrosRow(plan: MacroPlan) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        MacroItem(stringResource(R.string.plan_macros_carbs), plan.carbsGrams, stringResource(R.string.plan_unit_g), CarbColor)
        MacroItem(stringResource(R.string.plan_macros_proteins), plan.proteinGrams, stringResource(R.string.plan_unit_g), ProteinColor)
        MacroItem(stringResource(R.string.plan_macros_fat), plan.fatGrams, stringResource(R.string.plan_unit_g), FatColor)
    }
}

@Composable
private fun MacroItem(title: String, value: Int, unit: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier
                .size(14.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(Modifier.height(6.dp))
        Text(title, color = NeutralText, fontSize = 16.sp)
        Text("$value$unit", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
    }
}

@Composable
private fun BmiCard(bmi: Double, klass: BmiClass) {
    val labelRes = when (klass) {
        BmiClass.Underweight -> R.string.plan_bmi_label_underweight
        BmiClass.Normal      -> R.string.plan_bmi_label_normal
        BmiClass.Overweight  -> R.string.plan_bmi_label_overweight
        BmiClass.Obesity     -> R.string.plan_bmi_label_obesity
    }
    val label = stringResource(labelRes)
    val tone  = bmiClassColor(klass) // ← 依分類取色

    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, BmiBorder, RoundedCornerShape(16.dp))
            .background(Color(0xFFF7FBF8))
            .padding(18.dp)
    ) {
        Text(stringResource(R.string.plan_bmi_index), fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                // 固定一位小數，避免顯示 23.0/23 風格不一致
                val bmiText = remember(bmi) { String.format(Locale.getDefault(), "%.1f", bmi) }
                Text(bmiText, fontSize = 42.sp, fontWeight = FontWeight.ExtraBold, color = tone)
                Text(label, color = tone, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(tone.copy(alpha = 0.15f))
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                val lower = label.lowercase(Locale.getDefault())
                Text(
                    text = stringResource(R.string.plan_bmi_classified_as, lower),
                    color = tone,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Spacer(Modifier.height(10.dp))
        // BMI 漸層條 & 刻度（保留你的設計）
        Box(
            Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            Color(0xFF60A5FA), // 偏低
                            Color(0xFF22C55E), // 正常
                            Color(0xFFF59E0B), // 過重
                            Color(0xFFEF4444)  // 肥胖
                        )
                    )
                )
        )
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            listOf(
                stringResource(R.string.plan_bmi_tick_15),
                stringResource(R.string.plan_bmi_tick_18_5),
                stringResource(R.string.plan_bmi_tick_25),
                stringResource(R.string.plan_bmi_tick_30),
                stringResource(R.string.plan_bmi_tick_35)
            ).forEach { m ->
                Text(m, color = NeutralText, fontSize = 12.sp)
            }
        }
    }

    Spacer(Modifier.height(12.dp))
    Text(
        stringResource(R.string.plan_disclaimer),
        color = NeutralText, fontSize = 12.sp
    )
}

@Composable
private fun bmiClassColor(klass: BmiClass): Color = when (klass) {
    BmiClass.Underweight -> Color(0xFF60A5FA) // Blue
    BmiClass.Normal      -> BmiGood          // Green
    BmiClass.Overweight  -> Color(0xFFF59E0B) // Amber
    BmiClass.Obesity     -> Color(0xFFEF4444) // Red
}

