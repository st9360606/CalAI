package com.calai.bitecal.ui.home.ui.progress.model

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.calai.bitecal.data.foodlog.model.FoodLogWeeklyProgressDto
import com.calai.bitecal.data.foodlog.model.ProgressDayDto
import com.calai.bitecal.data.foodlog.repo.FoodLogsRepository
import com.calai.bitecal.data.profile.api.UserProfileDto
import com.calai.bitecal.data.profile.repo.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.Locale
import javax.inject.Inject
import kotlin.math.abs
import kotlin.math.pow
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

data class BmiCardUi(
    val bmiText: String = "--.--",
    val statusText: String = "--",
    val statusTone: BmiStatusTone = BmiStatusTone.Unknown,
    val markerProgress: Float = 0.5f
)

enum class BmiStatusTone {
    Underweight,
    Healthy,
    Overweight,
    Obese,
    Unknown
}

data class ProgressUiState(
    val loading: Boolean = true,
    val selectedWeekOffset: Int = 0,
    val totalCaloriesText: String = "0.0",
    val deltaText: String = "--",
    val deltaDirection: String = "NONE",
    val days: List<ProgressBarDayUi> = emptyList(),
    val periodLabel: String = "This Week",
    val bmiCard: BmiCardUi = BmiCardUi(),
    val error: String? = null
) {
    val isEmpty: Boolean
        get() = !loading && error == null && days.all { it.totalG <= 0f && it.totalKcal <= 0 }
}

@HiltViewModel
class ProgressViewModel @Inject constructor(
    private val repo: FoodLogsRepository,
    private val profileRepository: ProfileRepository
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
            _ui.update {
                it.copy(
                    loading = true,
                    selectedWeekOffset = weekOffset,
                    error = null
                )
            }

            coroutineScope {
                val progressDeferred = async {
                    runCatching { repo.getWeeklyProgress(weekOffset) }
                }

                val bmiDeferred = async {
                    runCatching {
                        profileRepository.getServerProfileOrNull()?.toBmiCardUi() ?: BmiCardUi()
                    }
                }

                val progressResult = progressDeferred.await()
                val bmiCard = bmiDeferred.await().getOrElse { _ui.value.bmiCard }

                progressResult
                    .onSuccess { dto ->
                        loaded = true
                        _ui.value = dto.toUiState(weekOffset).copy(bmiCard = bmiCard)
                    }
                    .onFailure { t ->
                        _ui.update {
                            it.copy(
                                loading = false,
                                selectedWeekOffset = weekOffset,
                                bmiCard = bmiCard,
                                error = t.message ?: "Load progress failed"
                            )
                        }
                    }
            }
        }
    }
}

private val ORDERED_WEEK_LABELS = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")

private fun FoodLogWeeklyProgressDto.toUiState(weekOffset: Int): ProgressUiState {
    val rawDayUis = days.map { it.toUi() }
    val normalizedDayUis = rawDayUis.normalizeWeekDays()
    val isCurrentWeek = weekOffset == 0

    val today = LocalDate.now()
    val yesterday = today.minusDays(1)

    val displayDay: ProgressBarDayUi
    val compareDay: ProgressBarDayUi?

    if (isCurrentWeek) {
        val todayUi = normalizedDayUis.firstOrNull { it.date == today.toString() }
        val yesterdayUi = normalizedDayUis.firstOrNull { it.date == yesterday.toString() }

        displayDay = todayUi ?: normalizedDayUis.lastOrNull { it.totalKcal > 0 } ?: emptyProgressDayUi("Sat")
        compareDay = yesterdayUi
    } else {
        displayDay = normalizedDayUis.firstOrNull { it.dayLabel == "Sat" } ?: emptyProgressDayUi("Sat")
        compareDay = normalizedDayUis.firstOrNull { it.dayLabel == "Fri" } ?: emptyProgressDayUi("Fri")
    }

    val effectiveDeltaValue = if (compareDay != null) {
        calculateDayDeltaPercent(
            todayCalories = displayDay.totalKcal,
            yesterdayCalories = compareDay.totalKcal
        )
    } else {
        null
    }

    val effectiveTotalCaloriesText = String.format(
        Locale.getDefault(),
        "%.1f",
        displayDay.totalKcal.toDouble()
    )
    return ProgressUiState(
        loading = false,
        selectedWeekOffset = weekOffset,
        totalCaloriesText = effectiveTotalCaloriesText,
        deltaText = effectiveDeltaValue.toDeltaText(),
        deltaDirection = effectiveDeltaValue.toDeltaDirection(),
        days = normalizedDayUis,
        periodLabel = period.label.toPrettyLabel(),
        error = null
    )
}

private fun List<ProgressBarDayUi>.normalizeWeekDays(): List<ProgressBarDayUi> {
    val dayMap = associateBy { it.dayLabel.take(3) }

    return ORDERED_WEEK_LABELS.map { label ->
        dayMap[label] ?: emptyProgressDayUi(label)
    }
}

private fun emptyProgressDayUi(label: String): ProgressBarDayUi {
    return ProgressBarDayUi(
        date = "",
        dayLabel = label,
        proteinG = 0f,
        carbsG = 0f,
        fatsG = 0f,
        totalG = 0f,
        totalKcal = 0
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

    val rounded = String.format(Locale.getDefault(), "%.1f", abs(this))
    return "$prefix$rounded%"
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

private fun UserProfileDto.toBmiCardUi(): BmiCardUi {
    val bmiValue = resolveBmiValue()
    val tone = resolveBmiTone(bmiValue)

    return BmiCardUi(
        bmiText = bmiValue?.let { String.format(Locale.getDefault(), "%.2f", it) } ?: "--.--",
        statusText = "",
        statusTone = tone,
        markerProgress = bmiValue
            ?.let { ((it - 15.0) / 20.0).toFloat().coerceIn(0f, 1f) }
            ?: 0.5f
    )
}

private fun UserProfileDto.resolveBmiValue(): Double? {
    bmi?.takeIf { it > 0.0 }?.let { return it }

    val resolvedWeightKg = when {
        weightKg != null && weightKg > 0.0 -> weightKg
        weightLbs != null && weightLbs > 0.0 -> weightLbs * 0.45359237
        else -> null
    }

    val resolvedHeightMeters = when {
        heightCm != null && heightCm > 0.0 -> heightCm / 100.0
        heightFeet != null && heightFeet >= 0 && heightInches != null && heightInches >= 0 -> {
            val totalInches = (heightFeet * 12) + heightInches
            (totalInches * 2.54) / 100.0
        }
        else -> null
    }

    return if (resolvedWeightKg != null && resolvedHeightMeters != null && resolvedHeightMeters > 0.0) {
        resolvedWeightKg / resolvedHeightMeters.pow(2)
    } else {
        null
    }
}

private fun UserProfileDto.resolveBmiTone(bmiValue: Double?): BmiStatusTone {
    return when (bmiClass?.trim()?.uppercase(Locale.ROOT)) {
        "UNDERWEIGHT" -> BmiStatusTone.Underweight
        "NORMAL", "HEALTHY" -> BmiStatusTone.Healthy
        "OVERWEIGHT" -> BmiStatusTone.Overweight
        "OBESE", "OBESITY" -> BmiStatusTone.Obese
        else -> when {
            bmiValue == null -> BmiStatusTone.Unknown
            bmiValue < 18.5 -> BmiStatusTone.Underweight
            bmiValue < 25.0 -> BmiStatusTone.Healthy
            bmiValue < 30.0 -> BmiStatusTone.Overweight
            else -> BmiStatusTone.Obese
        }
    }
}
