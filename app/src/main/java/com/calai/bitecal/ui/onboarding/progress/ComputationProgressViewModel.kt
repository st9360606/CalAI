package com.calai.bitecal.ui.onboarding.progress

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ComputationProgressViewModel @Inject constructor() : ViewModel() {

    private val _ui = MutableStateFlow(ProgressUiState())
    val ui: StateFlow<ProgressUiState> = _ui.asStateFlow()

    private var started = false

    fun start(
        durationMs: Long = 4200L,
        holdNearCompleteMs: Long = 420L
    ) {
        if (started || _ui.value.done || _ui.value.percent > 0) return
        started = true

        viewModelScope.launch {
            val requestedTotal = durationMs.coerceAtLeast(2600L)
            val holdMs = holdNearCompleteMs.coerceIn(180L, 1200L)
            val totalMs = maxOf(requestedTotal, holdMs + 1800L)
            val animationBudgetMs = totalMs - holdMs

            val segments = listOf(
                ProgressSegment(start = 0, end = 18, portion = 0.16f),
                ProgressSegment(start = 19, end = 46, portion = 0.25f),
                ProgressSegment(start = 47, end = 74, portion = 0.27f),
                ProgressSegment(start = 75, end = 90, portion = 0.20f),
                ProgressSegment(start = 91, end = 99, portion = 0.12f)
            )

            updateProgress(0, isDone = false)

            for (segment in segments) {
                if (!isActive) return@launch
                runSegment(segment = segment, animationBudgetMs = animationBudgetMs)
            }

            if (!isActive) return@launch

            delay(holdMs)
            updateProgress(100, isDone = true)
        }
    }

    fun reset() {
        started = false
        _ui.value = ProgressUiState()
    }

    private suspend fun runSegment(
        segment: ProgressSegment,
        animationBudgetMs: Long
    ) {
        val stepCount = (segment.end - segment.start + 1).coerceAtLeast(1)
        val segmentDurationMs = (animationBudgetMs * segment.portion)
            .toLong()
            .coerceAtLeast(stepCount * 10L)

        val baseTickMs = (segmentDurationMs / stepCount).coerceAtLeast(10L)

        for (p in segment.start..segment.end) {
            if (!currentCoroutineContext().isActive) return

            updateProgress(p, isDone = false)
            delay(baseTickMs + extraDelayFor(p))
        }
    }

    private fun updateProgress(
        rawPercent: Int,
        isDone: Boolean
    ) {
        _ui.update { prev ->
            val percent = rawPercent
                .coerceIn(0, 100)
                .coerceAtLeast(prev.percent)

            prev.copy(
                percent = percent,
                phase = phaseFor(percent),
                checks = prev.checks.copy(
                    calories = prev.checks.calories || percent >= 12,
                    carbs = prev.checks.carbs || percent >= 30,
                    protein = prev.checks.protein || percent >= 52,
                    fats = prev.checks.fats || percent >= 74,
                    healthScore = prev.checks.healthScore || percent >= 92
                ),
                done = isDone || percent >= 100
            )
        }
    }

    private fun extraDelayFor(percent: Int): Long {
        return when {
            percent < 20 -> 0L
            percent < 50 -> 6L
            percent < 75 -> 12L
            percent < 91 -> 20L
            else -> 30L
        }
    }

    private fun phaseFor(percent: Int): ProgressPhase {
        return when {
            percent < 20 -> ProgressPhase.P1
            percent < 48 -> ProgressPhase.P2
            percent < 78 -> ProgressPhase.P3
            else -> ProgressPhase.P4
        }
    }

    private data class ProgressSegment(
        val start: Int,
        val end: Int,
        val portion: Float
    )
}
