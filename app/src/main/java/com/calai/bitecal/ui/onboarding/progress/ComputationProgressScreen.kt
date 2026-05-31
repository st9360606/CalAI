package com.calai.bitecal.ui.onboarding.progress

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.calai.bitecal.R
import com.calai.bitecal.ui.common.design.BiteCalComputationProgressTokens as ProgressTokens
import com.calai.bitecal.ui.common.design.BiteCalScreenSpacing
import kotlinx.coroutines.delay

private enum class ItemVisualState {
    DONE, ACTIVE, PENDING
}

@Composable
fun ComputationProgressScreen(
    onDone: () -> Unit,
    vm: ComputationProgressViewModel
) {
    val ui by vm.ui.collectAsState()

    LaunchedEffect(Unit) {
        vm.start(durationMs = 4200L, holdNearCompleteMs = 420L)
    }

    LaunchedEffect(ui.done) {
        if (ui.done) {
            delay(850)
            onDone()
        }
    }

    val progress by animateFloatAsState(
        targetValue = ui.percent.coerceIn(0, 100) / 100f,
        animationSpec = tween(durationMillis = 450, easing = FastOutSlowInEasing),
        label = "progress_ring"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    horizontal = BiteCalScreenSpacing.ContentWideHorizontal,
                    vertical = BiteCalScreenSpacing.ContentVertical),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(20.dp))

            HeroProgressRing(
                progress = progress,
                percent = ui.percent
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = stringResource(R.string.progress_hero_title),
                fontSize = 28.sp,
                lineHeight = 34.sp,
                fontWeight = FontWeight.Bold,
                color = ProgressTokens.TextPrimary,
                textAlign = TextAlign.Center,
                style = LocalTextStyle.current.copy(
                    platformStyle = PlatformTextStyle(includeFontPadding = false),
                    lineHeightStyle = LineHeightStyle(
                        alignment = LineHeightStyle.Alignment.Center,
                        trim = LineHeightStyle.Trim.Both
                    )
                )
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = stringResource(R.string.progress_hero_subtitle),
                fontSize = 15.sp,
                lineHeight = 22.sp,
                fontWeight = FontWeight.Medium,
                color = ProgressTokens.TextSecondary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            PhasePill(phase = ui.phase)

            Spacer(modifier = Modifier.height(22.dp))

            ProgressChecklistCard(ui = ui)

            Spacer(modifier = Modifier.weight(1f))

            Text(
                text = stringResource(R.string.progress_footer_hint),
                fontSize = 15.sp,
                lineHeight = 22.sp,
                color = ProgressTokens.TextSecondary,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun HeroProgressRing(
    progress: Float,
    percent: Int
) {
    Box(
        modifier = Modifier.size(214.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = 14.dp.toPx()

            drawCircle(
                color = ProgressTokens.ProgressTrack,
                style = Stroke(width = stroke)
            )

            drawArc(
                color = ProgressTokens.ProgressPrimary,
                startAngle = -90f,
                sweepAngle = 360f * progress,
                useCenter = false,
                style = Stroke(
                    width = stroke,
                    cap = StrokeCap.Round
                )
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "${percent.coerceIn(0, 100)}%",
                color = ProgressTokens.TextPrimary,
                fontSize = 42.sp,
                lineHeight = 46.sp,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = stringResource(R.string.progress_center_caption),
                color = ProgressTokens.TextSecondary,
                fontSize = 15.sp,
                lineHeight = 18.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun PhasePill(
    phase: ProgressPhase
) {
    val text = when (phase) {
        ProgressPhase.P1 -> stringResource(R.string.progress_phase_pill_1)
        ProgressPhase.P2 -> stringResource(R.string.progress_phase_pill_2)
        ProgressPhase.P3 -> stringResource(R.string.progress_phase_pill_3)
        ProgressPhase.P4 -> stringResource(R.string.progress_phase_pill_4)
    }

    Surface(
        shape = RoundedCornerShape(999.dp),
        color = ProgressTokens.ProgressPrimarySoft
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            color = ProgressTokens.ProgressPrimary,
            fontSize = 14.sp,
            lineHeight = 17.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun ProgressChecklistCard(
    ui: ProgressUiState
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = ProgressTokens.CardBg,
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = stringResource(R.string.progress_card_title_v2),
                fontSize = 16.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.Bold,
                color = ProgressTokens.TextPrimary
            )

            buildChecklistRows(ui).forEach { row ->
                ProgressChecklistRow(
                    title = row.title,
                    state = row.state
                )
            }
        }
    }
}

private data class ChecklistRowUi(
    val title: String,
    val state: ItemVisualState
)

@Composable
private fun buildChecklistRows(
    ui: ProgressUiState
): List<ChecklistRowUi> {
    val activeIndex = when {
        !ui.checks.calories -> 0
        !ui.checks.carbs -> 1
        !ui.checks.protein -> 2
        !ui.checks.fats -> 3
        !ui.checks.healthScore -> 4
        else -> -1
    }

    fun stateOf(index: Int, done: Boolean): ItemVisualState {
        return when {
            done -> ItemVisualState.DONE
            index == activeIndex -> ItemVisualState.ACTIVE
            else -> ItemVisualState.PENDING
        }
    }

    return listOf(
        ChecklistRowUi(
            title = stringResource(R.string.progress_row_calorie_target),
            state = stateOf(0, ui.checks.calories)
        ),
        ChecklistRowUi(
            title = stringResource(R.string.progress_row_carb_distribution),
            state = stateOf(1, ui.checks.carbs)
        ),
        ChecklistRowUi(
            title = stringResource(R.string.progress_row_protein_recommendation),
            state = stateOf(2, ui.checks.protein)
        ),
        ChecklistRowUi(
            title = stringResource(R.string.progress_row_fat_recommendation),
            state = stateOf(3, ui.checks.fats)
        ),
        ChecklistRowUi(
            title = stringResource(R.string.progress_row_health_score_estimate),
            state = stateOf(4, ui.checks.healthScore)
        )
    )
}

@Composable
private fun ProgressChecklistRow(
    title: String,
    state: ItemVisualState
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        when (state) {
            ItemVisualState.DONE -> {
                AnimatedVisibility(
                    visible = true,
                    enter = fadeIn() + scaleIn()
                ) {
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(ProgressTokens.ProgressPrimary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            ItemVisualState.ACTIVE -> {
                val transition = rememberInfiniteTransition(label = "pulse_transition")
                val alpha by transition.animateFloat(
                    initialValue = 0.45f,
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(durationMillis = 900, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "pulse_alpha"
                )

                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(ProgressTokens.ProgressPrimary.copy(alpha = alpha))
                )
            }

            ItemVisualState.PENDING -> {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(ProgressTokens.PendingDot)
                )
            }
        }

        Spacer(modifier = Modifier.size(12.dp))

        Text(
            text = title,
            color = when (state) {
                ItemVisualState.DONE -> ProgressTokens.TextPrimary
                ItemVisualState.ACTIVE -> ProgressTokens.TextPrimary
                ItemVisualState.PENDING -> ProgressTokens.TextSecondary
            },
            fontSize = 15.sp,
            lineHeight = 20.sp,
            fontWeight = if (state == ItemVisualState.ACTIVE) FontWeight.SemiBold else FontWeight.Medium
        )
    }
}
