package com.calai.bitecal.ui.home.ui.progress

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.calai.bitecal.R
import com.calai.bitecal.ui.home.ui.progress.model.ProgressAverageOverviewUi
import java.text.NumberFormat

private val AverageTabsContainerBg = Color(0xFFF1F1F3)
private val AverageTabsActiveBg = Color(0xFFFFFFFF)
private val AverageTabsActiveBorder = Color(0xFFE2E2E6)
private val AverageTabsActiveText = Color(0xFF2D2F35)
private val AverageTabsIdleText = Color(0xFF3A3D43)

private val AverageCardBg = Color.White
private val AverageBorder = Color(0xFFD9D9DB)
private val AverageTitle = Color(0xFF1B1B21)
private val AverageMuted = Color(0xFF74747A)
private val AverageMetricBg = Color(0xFFF8FAFC)
private val AverageMetricBorder = Color(0xFFE2E8F0)
private val AverageBadgeBg = Color(0xFFEEF2FF)
private val AverageBadgeBorder = Color(0xFFC7D2FE)
private val AverageBadgeText = Color(0xFF4338CA)
private val AverageMetricLabel = Color(0xFF64748B)
private val AverageMetricValue = Color(0xFF0F172A)

@Composable
internal fun ProgressAverageOverviewSection(
    selectedDays: Int,
    onSelectedDaysChange: (Int) -> Unit,
    items: List<ProgressAverageOverviewUi>,
    loading: Boolean,
    error: String?,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    val availableDays = remember(items) { items.map { it.days }.ifEmpty { listOf(7, 15, 30, 60) } }
    val selectedItem = remember(items, selectedDays) {
        items.firstOrNull { it.days == selectedDays } ?: items.firstOrNull()
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        AverageRangeTabs(
            selectedDays = selectedDays,
            availableDays = availableDays,
            onSelectedDaysChange = onSelectedDaysChange
        )

        when {
            loading && selectedItem == null -> ProgressAverageOverviewLoadingCard()
            error != null && selectedItem == null -> ProgressAverageOverviewErrorCard(
                message = error,
                onRetry = onRetry
            )
            selectedItem != null -> ProgressAverageOverviewCard(item = selectedItem)
        }
    }
}

@Composable
private fun AverageRangeTabs(
    selectedDays: Int,
    availableDays: List<Int>,
    onSelectedDaysChange: (Int) -> Unit
) {
    val labels = listOf(
        7 to stringResource(R.string.progress_average_tab_7_days),
        15 to stringResource(R.string.progress_average_tab_15_days),
        30 to stringResource(R.string.progress_average_tab_30_days),
        60 to stringResource(R.string.progress_average_tab_60_days)
    )
    val containerShape = RoundedCornerShape(18.dp)
    val activeTabShape = RoundedCornerShape(14.dp)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(AverageTabsContainerBg, containerShape)
            .padding(horizontal = 6.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        labels.forEach { (days, label) ->
            val active = selectedDays == days
            val enabled = availableDays.contains(days)

            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp)
                    .then(
                        if (active) {
                            Modifier
                                .shadow(
                                    elevation = 1.5.dp,
                                    shape = activeTabShape,
                                    clip = false
                                )
                                .background(AverageTabsActiveBg, activeTabShape)
                                .border(1.dp, AverageTabsActiveBorder, activeTabShape)
                        } else {
                            Modifier.background(Color.Transparent, activeTabShape)
                        }
                    )
                    .clickable(enabled = enabled) { onSelectedDaysChange(days) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    color = if (active) AverageTabsActiveText else AverageTabsIdleText.copy(alpha = if (enabled) 1f else 0.42f),
                    fontSize = 14.sp,
                    fontWeight = if (active) FontWeight.Bold else FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun ProgressAverageOverviewCard(
    item: ProgressAverageOverviewUi
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(AverageCardBg, RoundedCornerShape(28.dp))
            .border(1.dp, AverageBorder, RoundedCornerShape(28.dp))
            .padding(horizontal = 18.dp, vertical = 18.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.progress_average_overview_title),
                    color = AverageTitle,
                    fontSize = 20.sp,
                    lineHeight = 24.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = stringResource(R.string.progress_average_overview_subtitle),
                    color = AverageMuted,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Box(
                modifier = Modifier
                    .background(AverageBadgeBg, RoundedCornerShape(999.dp))
                    .border(1.dp, AverageBadgeBorder, RoundedCornerShape(999.dp))
                    .padding(horizontal = 12.dp, vertical = 7.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.progress_average_days_badge, item.days),
                    color = AverageBadgeText,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        AnimatedContent(
            targetState = item,
            transitionSpec = {
                fadeIn(animationSpec = tween(140)) togetherWith fadeOut(animationSpec = tween(90))
            },
            label = "progress_average_overview_content"
        ) { target ->
            AverageMetricGrid(metrics = averageMetricsFor(target))
        }
    }
}

@Composable
private fun averageMetricsFor(item: ProgressAverageOverviewUi): List<AverageMetricUi> {
    val waterText = stringResource(R.string.water_chart_value_ml, formatAverageInt(item.waterMl))
    val stepsText = stringResource(R.string.progress_average_steps_value, formatAverageInt(item.steps))

    return listOf(
        AverageMetricUi(
            label = stringResource(R.string.progress_average_total_calories),
            value = stringResource(R.string.progress_chart_value_cals, item.caloriesKcal),
            emoji = "🔥",
            accent = Color(0xFF9CA3AF)
        ),
        AverageMetricUi(
            label = stringResource(R.string.progress_average_protein),
            value = stringResource(R.string.progress_tooltip_grams_value, item.proteinG),
            emoji = "🥩",
            accent = Color(0xFFD96A6A)
        ),
        AverageMetricUi(
            label = stringResource(R.string.progress_average_carbs),
            value = stringResource(R.string.progress_tooltip_grams_value, item.carbsG),
            emoji = "🌾",
            accent = Color(0xFFC88445)
        ),
        AverageMetricUi(
            label = stringResource(R.string.progress_average_fats),
            value = stringResource(R.string.progress_tooltip_grams_value, item.fatsG),
            emoji = "🥑",
            accent = Color(0xFF5F86C9)
        ),
        AverageMetricUi(
            label = stringResource(R.string.progress_average_fiber),
            value = stringResource(R.string.progress_tooltip_grams_value, item.fiberG),
            emoji = "🌿",
            accent = Color(0xFF8B7AE6)
        ),
        AverageMetricUi(
            label = stringResource(R.string.progress_average_sugar),
            value = stringResource(R.string.progress_tooltip_grams_value, item.sugarG),
            emoji = "🍯",
            accent = Color(0xFFD76A96)
        ),
        AverageMetricUi(
            label = stringResource(R.string.progress_average_sodium),
            value = stringResource(R.string.progress_tooltip_mg_value, item.sodiumMg),
            emoji = "🍚",
            accent = Color(0xFF5DA9D8)
        ),
        AverageMetricUi(
            label = stringResource(R.string.progress_average_workout),
            value = stringResource(R.string.workout_chart_value_kcal, item.workoutKcal),
            emoji = "🏋️",
            accent = Color(0xFF8E72D6)
        ),
        AverageMetricUi(
            label = stringResource(R.string.progress_average_water),
            value = waterText,
            emoji = "💧",
            accent = Color(0xFF3BA6C9)
        ),
        AverageMetricUi(
            label = stringResource(R.string.progress_average_health_score),
            value = stringResource(R.string.progress_average_health_score_value, item.healthScore),
            emoji = "💚",
            accent = Color(0xFF45B96A)
        ),
        AverageMetricUi(
            label = stringResource(R.string.progress_average_steps),
            value = stepsText,
            emoji = "👟",
            accent = Color(0xFF4EA3C7)
        )
    )
}

@Composable
private fun AverageMetricGrid(metrics: List<AverageMetricUi>) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        metrics.chunked(2).forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                rowItems.forEach { metric ->
                    AverageMetricTile(
                        metric = metric,
                        modifier = Modifier.weight(1f)
                    )
                }
                if (rowItems.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun AverageMetricTile(
    metric: AverageMetricUi,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .background(AverageMetricBg, RoundedCornerShape(18.dp))
            .border(1.dp, AverageMetricBorder, RoundedCornerShape(18.dp))
            .padding(horizontal = 12.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .background(metric.accent.copy(alpha = 0.14f), RoundedCornerShape(999.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(text = metric.emoji, fontSize = 15.sp)
        }

        Spacer(modifier = Modifier.width(9.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = metric.label,
                color = AverageMetricLabel,
                fontSize = 11.sp,
                lineHeight = 13.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = metric.value,
                color = AverageMetricValue,
                fontSize = 14.sp,
                lineHeight = 17.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ProgressAverageOverviewLoadingCard() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(178.dp)
            .background(AverageCardBg, RoundedCornerShape(28.dp))
            .border(1.dp, AverageBorder, RoundedCornerShape(28.dp))
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(R.string.progress_average_loading),
            color = AverageMuted,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun ProgressAverageOverviewErrorCard(
    message: String,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(AverageCardBg, RoundedCornerShape(28.dp))
            .border(1.dp, AverageBorder, RoundedCornerShape(28.dp))
            .padding(horizontal = 20.dp, vertical = 22.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = message,
            color = Color(0xFF111114),
            fontSize = 14.sp,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(12.dp))

        Box(
            modifier = Modifier
                .background(Color(0xFF111114), RoundedCornerShape(999.dp))
                .clickable { onRetry() }
                .padding(horizontal = 18.dp, vertical = 10.dp)
        ) {
            Text(
                text = stringResource(R.string.progress_retry),
                color = Color.White,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

private data class AverageMetricUi(
    val label: String,
    val value: String,
    val emoji: String,
    val accent: Color
)

private fun formatAverageInt(value: Int): String {
    return NumberFormat.getIntegerInstance().format(value.coerceAtLeast(0))
}
