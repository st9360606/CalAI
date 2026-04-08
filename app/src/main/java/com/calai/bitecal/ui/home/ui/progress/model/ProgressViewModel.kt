package com.calai.bitecal.ui.home.ui.progress.model

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.calai.bitecal.data.foodlog.model.FoodLogWeeklyProgressDto
import com.calai.bitecal.data.foodlog.model.ProgressDayDto
import com.calai.bitecal.data.foodlog.repo.FoodLogsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.Locale
import javax.inject.Inject
import kotlin.math.abs
import kotlin.math.roundToInt

data class ProgressBarDayUi(
    val date: String,
    val dayLabel: String,
    val proteinG: Float,
    val carbsG: Float,
    val fatsG: Float,
    val totalG: Float,
    val totalKcal: Int
)

data class ProgressUiState(
    val loading: Boolean = true,
    val selectedWeekOffset: Int = 0,
    val totalCaloriesText: String = "0 cals",
    val deltaText: String = "--",
    val deltaDirection: String = "NONE",
    val days: List<ProgressBarDayUi> = emptyList(),
    val periodLabel: String = "This Week",
    val error: String? = null
) {
    val isEmpty: Boolean get() = !loading && error == null && days.all { it.totalG <= 0f && it.totalKcal <= 0 }
}

@HiltViewModel
class ProgressViewModel @Inject constructor(
    private val repo: FoodLogsRepository
) : ViewModel() {

    private val _ui = MutableStateFlow(ProgressUiState())
    val ui: StateFlow<ProgressUiState> = _ui.asStateFlow()

    private var loaded = false

    fun loadIfNeeded() {
        if (loaded) return
        refresh(0)
    }

    fun selectWeek(weekOffset: Int) {
        val safe = weekOffset.coerceIn(0, 3)
        if (_ui.value.selectedWeekOffset == safe && loaded) return
        refresh(safe)
    }

    fun retry() {
        refresh(_ui.value.selectedWeekOffset)
    }

    private fun refresh(weekOffset: Int) {
        viewModelScope.launch {
            _ui.update { it.copy(loading = true, selectedWeekOffset = weekOffset, error = null) }
            runCatching { repo.getWeeklyProgress(weekOffset) }
                .onSuccess { dto ->
                    loaded = true
                    _ui.value = dto.toUiState(weekOffset)
                }
                .onFailure { t ->
                    _ui.update {
                        it.copy(
                            loading = false,
                            selectedWeekOffset = weekOffset,
                            error = t.message ?: "Load progress failed"
                        )
                    }
                }
        }
    }
}

private fun FoodLogWeeklyProgressDto.toUiState(weekOffset: Int): ProgressUiState {
    val dayUis = days.map { it.toUi() }
    val isCurrentWeek = weekOffset == 0

    val today = LocalDate.now()
    val yesterday = today.minusDays(1)

    val todayKcal = dayUis.firstOrNull { it.date == today.toString() }?.totalKcal
    val yesterdayKcal = dayUis.firstOrNull { it.date == yesterday.toString() }?.totalKcal

    val effectiveDeltaValue = if (isCurrentWeek && todayKcal != null && yesterdayKcal != null) {
        calculateDayDeltaPercent(
            todayCalories = todayKcal,
            yesterdayCalories = yesterdayKcal
        )
    } else {
        summary.deltaPercent
    }

    val effectiveTotalCaloriesText = if (isCurrentWeek && todayKcal != null) {
        String.format(Locale.US, "%.1f cals", todayKcal.toDouble())
    } else {
        "${summary.totalCalories.roundToInt()} cals"
    }

    return ProgressUiState(
        loading = false,
        selectedWeekOffset = weekOffset,
        totalCaloriesText = effectiveTotalCaloriesText,
        deltaText = effectiveDeltaValue.toDeltaText(),
        deltaDirection = effectiveDeltaValue.toDeltaDirection(summary.deltaDirection),
        days = dayUis,
        periodLabel = period.label.toPrettyLabel(),
        error = null
    )
}

private fun calculateDayDeltaPercent(
    todayCalories: Int,
    yesterdayCalories: Int
): Double? {
    return when {
        todayCalories == 0 && yesterdayCalories == 0 -> 0.0
        yesterdayCalories == 0 -> 100.0
        else -> ((todayCalories - yesterdayCalories).toDouble() / yesterdayCalories.toDouble()) * 100.0
    }
}

private fun Double?.toDeltaText(): String {
    if (this == null) return "--"

    val prefix = when {
        this > 0 -> "↑ "
        this < 0 -> "↓ "
        else -> ""
    }

    return prefix + abs(this).roundToInt() + "%"
}

private fun Double?.toDeltaDirection(
    fallback: String = "NONE"
): String {
    if (this == null) return fallback

    return when {
        this > 0 -> "UP"
        this < 0 -> "DOWN"
        else -> "NONE"
    }
}

private fun ProgressDayDto.toUi(): ProgressBarDayUi {
    val protein = proteinG.toFloat()
    val carbs = carbsG.toFloat()
    val fats = fatsG.toFloat()
    return ProgressBarDayUi(
        date = date,
        dayLabel = dayOfWeek.take(3),
        proteinG = protein,
        carbsG = carbs,
        fatsG = fats,
        totalG = protein + carbs + fats,
        totalKcal = totalKcal.roundToInt()
    )
}

private fun String.toPrettyLabel(): String = when (uppercase(Locale.ROOT)) {
    "THIS_WEEK" -> "This Week"
    "LAST_WEEK" -> "Last Week"
    "TWO_WEEKS_AGO" -> "2 wks. ago"
    "THREE_WEEKS_AGO" -> "3 wks. ago"
    else -> this.replace('_', ' ')
}
