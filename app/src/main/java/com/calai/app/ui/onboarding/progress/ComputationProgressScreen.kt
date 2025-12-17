package com.calai.app.ui.onboarding.progress

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.calai.app.R
import kotlinx.coroutines.delay
import kotlin.math.min

@Composable
fun ComputationProgressScreen(
    onDone: () -> Unit,
    vm: ComputationProgressViewModel   // 由 NavHost 建立後傳入
) {
    val ui by vm.ui.collectAsState()

    // 啟動動畫（整段約 4 秒跑完）
    LaunchedEffect(Unit) { vm.start(durationMs = 4000L) }

    // 完成後 600ms → onDone()
    LaunchedEffect(ui.done) {
        if (ui.done) {
            delay(600)
            onDone()
        }
    }

    val titleFont = remember { FontFamily(Font(R.font.montserrat_bold)) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 往下推一點
        Spacer(Modifier.height(160.dp))

        // ╭──────────────────── 圓形綠色進度條（含百分比） ────────────────────╮
        CircularProgressRing(
            percent = ui.percent,
            diameter = 220.dp,       // 小一點
            thickness = 14.dp,
            progressColor = Color(0xFF66D36E),
            trackColor = Color(0xFFE6E9EE),
            percentColor = Color(0xFF111114),
        )
        // ╰──────────────────────────────────────────────────────────────────╯

        Spacer(Modifier.height(28.dp))

        // 標題
        Text(
            text = stringResource(R.string.progress_title_heading),
            fontSize = 32.sp,
            lineHeight = 38.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF222326),
            textAlign = TextAlign.Center,
            style = LocalTextStyle.current.copy(
                platformStyle = PlatformTextStyle(includeFontPadding = false),
                lineHeightStyle = LineHeightStyle(
                    alignment = LineHeightStyle.Alignment.Center,
                    trim = LineHeightStyle.Trim.Both
                )
            )
        )

        Spacer(Modifier.height(15.dp))

        // 階段文案
        Text(
            text = when (ui.phase) {
                ProgressPhase.P1 -> stringResource(R.string.progress_phase_1)
                ProgressPhase.P2 -> stringResource(R.string.progress_phase_2)
                ProgressPhase.P3 -> stringResource(R.string.progress_phase_3)
                ProgressPhase.P4 -> stringResource(R.string.progress_phase_4)
            },
            style = MaterialTheme.typography.bodyMedium.copy(
                fontSize = 16.sp,
                color = Color(0xFF6B7280)
            ),
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(35.dp))

        // 卡片（整體縮小）
        ProgressCard(ui = ui)

        Spacer(Modifier.weight(1f))
    }
}

@Composable
private fun ProgressCard(ui: ProgressUiState) {
    val shape = RoundedCornerShape(24.dp) // 稍微小一點的圓角
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        shape = shape,
        color = Color(0xFFF6F7FB),
        shadowElevation = 10.dp
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 18.dp) // 內距縮小
        ) {
            Text(
                text = stringResource(R.string.progress_card_title),
                fontSize = 18.sp,                    // 20 → 18
                fontWeight = FontWeight.Bold,
                color = Color(0xFF222326)
            )
            Spacer(Modifier.height(10.dp))

            ProgressItem(stringResource(R.string.progress_item_calories), ui.checks.calories)
            ProgressItem(stringResource(R.string.progress_item_carbs), ui.checks.carbs)
            ProgressItem(stringResource(R.string.progress_item_protein), ui.checks.protein)
            ProgressItem(stringResource(R.string.progress_item_fats), ui.checks.fats)
            ProgressItem(stringResource(R.string.progress_item_health_score), ui.checks.healthScore)
        }
    }
}

@Composable
private fun ProgressItem(label: String, checked: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),   // 依你前面縮小設定
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "•  $label",
            fontSize = 16.sp,
            color = Color(0xFF1F2937),
            modifier = Modifier.weight(1f)
        )
        AnimatedVisibility(
            visible = checked,
            enter = fadeIn() + scaleIn()
        ) {
            val green = Color(0xFF66D36E)
            Box(
                modifier = Modifier
                    .size(22.dp)                // 綠圓較小
                    .clip(CircleShape)
                    .background(green),
                contentAlignment = Alignment.Center
            ) {
                // ✅ 在 DrawScope 中，用 3.6.dp.toPx()，不要用 LocalDensity.current
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(4.dp)
                ) {
                    val w = size.width
                    val h = size.height

                    // 三個關鍵點（白色粗勾）
                    val p1 = Offset(w * 0.18f, h * 0.55f)
                    val p2 = Offset(w * 0.42f, h * 0.75f)
                    val p3 = Offset(w * 0.80f, h * 0.28f)

                    val stroke = 2.7.dp.toPx()   // ← 關鍵：DrawScope 的 Dp.toPx()

                    drawLine(
                        color = Color.White,
                        start = p1, end = p2,
                        strokeWidth = stroke,
                        cap = StrokeCap.Round
                    )
                    drawLine(
                        color = Color.White,
                        start = p2, end = p3,
                        strokeWidth = stroke,
                        cap = StrokeCap.Round
                    )
                }
            }
        }
    }
}

