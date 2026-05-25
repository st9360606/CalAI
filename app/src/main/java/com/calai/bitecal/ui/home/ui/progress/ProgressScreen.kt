package com.calai.bitecal.ui.home.ui.progress

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.calai.bitecal.R
import com.calai.bitecal.ui.common.bmi.CommonBmiCard
import com.calai.bitecal.ui.common.bmi.CommonBmiCardModel
import com.calai.bitecal.ui.common.bmi.CommonBmiTone
import com.calai.bitecal.ui.home.HomeTab
import com.calai.bitecal.ui.home.components.MainBottomBar
import com.calai.bitecal.ui.home.ui.components.ProfileEditTopBar
import com.calai.bitecal.ui.home.ui.progress.model.BmiCardUi
import com.calai.bitecal.ui.home.ui.progress.model.BmiStatusTone
import com.calai.bitecal.ui.home.ui.progress.model.ProgressViewModel

private val ProgressBg = Color(0xFFF5F5F5)

private val WeekTabsContainerBg = Color(0xFFF1F1F3)
private val WeekTabsActiveBg = Color(0xFFFFFFFF)
private val WeekTabsActiveBorder = Color(0xFFE2E2E6)
private val WeekTabsActiveText = Color(0xFF2D2F35)
private val WeekTabsIdleText = Color(0xFF3A3D43)

@Composable
fun ProgressScreen(
    vm: ProgressViewModel,
    onBack: () -> Unit,
    onOpenTab: (HomeTab) -> Unit
) {
    val ui by vm.ui.collectAsState()

    LaunchedEffect(Unit) { vm.loadIfNeeded() }
    BackHandler { onBack() }

    Scaffold(
        containerColor = ProgressBg,
        topBar = {
            ProfileEditTopBar(
                title = stringResource(R.string.progress_screen_title),
                onBack = onBack
            )
        },
        bottomBar = {
            MainBottomBar(current = HomeTab.Progress, onOpenTab = onOpenTab)
        }
    ) { inner ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(ProgressBg)
                .padding(inner),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                CommonBmiCard(
                    model = rememberProgressBmiCardModel(ui.bmiCard),
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            item {
                WeekTabs(
                    selected = ui.selectedWeekOffset,
                    onSelect = vm::selectWeek,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            when {
                ui.loading -> {
                    item {
                        LoadingCard(
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }

                ui.error != null -> {
                    item {
                        ErrorCard(
                            message = ui.error?.takeIf { it.isNotBlank() }
                                ?: stringResource(R.string.progress_error_load_failed),
                            onRetry = vm::retry,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }

                ui.isEmpty -> {
                    item {
                        EmptyCard(
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }

                else -> {
                    item {
                        NutritionChartCard(
                            totalCaloriesText = ui.totalCaloriesText,
                            deltaText = ui.deltaText,
                            days = ui.days,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }

                    item {
                        MicronutrientChartCard(
                            days = ui.days,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }
            }

            item {
                when {
                    ui.workoutLoading && ui.workoutChart.days.isEmpty() -> {
                        WorkoutLoadingCard(
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }

                    ui.workoutError != null && ui.workoutChart.days.isEmpty() -> {
                        WorkoutErrorCard(
                            message = ui.workoutError?.takeIf { it.isNotBlank() }
                                ?: stringResource(R.string.workout_chart_error_load_failed),
                            onRetry = vm::retryWorkout,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }

                    else -> {
                        WorkoutChartCard(
                            chart = ui.workoutChart,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }
            }

            item {
                when {
                    ui.waterLoading && ui.waterChart.days.isEmpty() -> {
                        WaterLoadingCard(
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }

                    ui.waterError != null && ui.waterChart.days.isEmpty() -> {
                        WaterErrorCard(
                            message = ui.waterError?.takeIf { it.isNotBlank() }
                                ?: stringResource(R.string.water_chart_error_load_failed),
                            onRetry = vm::retryWater,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }

                    else -> {
                        WaterChartCard(
                            chart = ui.waterChart,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(5.dp)) }
        }
    }
}

@Composable
private fun WeekTabs(
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val labels = listOf(
        stringResource(R.string.progress_tab_this_week),
        stringResource(R.string.progress_tab_last_week),
        stringResource(R.string.progress_tab_two_weeks_ago),
        stringResource(R.string.progress_tab_three_weeks_ago)
    )
    val containerShape = RoundedCornerShape(18.dp)
    val activeTabShape = RoundedCornerShape(14.dp)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(WeekTabsContainerBg, containerShape)
            .padding(horizontal = 6.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        labels.forEachIndexed { index, label ->
            val active = index == selected

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
                                .background(WeekTabsActiveBg, activeTabShape)
                                .border(1.dp, WeekTabsActiveBorder, activeTabShape)
                        } else {
                            Modifier.background(Color.Transparent, activeTabShape)
                        }
                    )
                    .clickable { onSelect(index) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    color = if (active) WeekTabsActiveText else WeekTabsIdleText,
                    fontSize = 14.sp,
                    fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun rememberProgressBmiCardModel(
    ui: BmiCardUi
): CommonBmiCardModel {
    val title = stringResource(R.string.bmi_card_title)
    val subtitle = stringResource(R.string.bmi_card_subtitle)
    val underweight = stringResource(R.string.bmi_status_underweight)
    val healthy = stringResource(R.string.bmi_status_healthy)
    val overweight = stringResource(R.string.bmi_status_overweight)
    val obese = stringResource(R.string.bmi_status_obese)
    val unknown = stringResource(R.string.bmi_status_unknown)

    return remember(
        ui.bmiText,
        ui.statusTone,
        ui.markerProgress,
        title,
        subtitle,
        underweight,
        healthy,
        overweight,
        obese,
        unknown
    ) {
        val localizedStatus = when (ui.statusTone) {
            BmiStatusTone.Underweight -> underweight
            BmiStatusTone.Healthy -> healthy
            BmiStatusTone.Overweight -> overweight
            BmiStatusTone.Obese -> obese
            BmiStatusTone.Unknown -> unknown
        }

        CommonBmiCardModel(
            bmiText = ui.bmiText,
            statusText = localizedStatus,
            statusTone = ui.statusTone.toCommonBmiTone(),
            markerProgress = ui.markerProgress,
            titleText = title,
            subtitleText = subtitle
        )
    }
}

private fun BmiStatusTone.toCommonBmiTone(): CommonBmiTone {
    return when (this) {
        BmiStatusTone.Underweight -> CommonBmiTone.Underweight
        BmiStatusTone.Healthy -> CommonBmiTone.Healthy
        BmiStatusTone.Overweight -> CommonBmiTone.Overweight
        BmiStatusTone.Obese -> CommonBmiTone.Obese
        BmiStatusTone.Unknown -> CommonBmiTone.Unknown
    }
}
