package com.calai.app.ui.onboarding.progress

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ComputationProgressViewModel @Inject constructor() : ViewModel() {

    private val _ui = MutableStateFlow(ProgressUiState())
    val ui: StateFlow<ProgressUiState> = _ui

    /**
     * 以固定總時長跑完 0..100%。預設約 4 秒（4000ms）。
     * 可重複呼叫但會忽略第二次，避免重入。
     */
    fun start(durationMs: Long = 4000L) {
        if (_ui.value.done || _ui.value.percent > 0) return

        viewModelScope.launch {
            val steps = 100                       // 每次 +1%
            val tick = (durationMs / steps).coerceAtLeast(8L)

            for (p in 0..steps) {
                if (!isActive) break

                _ui.update { s ->
                    s.copy(
                        percent = p,
                        phase = when {
                            p < 25 -> ProgressPhase.P1
                            p < 50 -> ProgressPhase.P2
                            p < 75 -> ProgressPhase.P3
                            else  -> ProgressPhase.P4
                        },
                        checks = s.checks.copy(
                            calories    = s.checks.calories     || p >= 15,
                            carbs       = s.checks.carbs        || p >= 35,
                            protein     = s.checks.protein      || p >= 55,
                            fats        = s.checks.fats         || p >= 75,
                            healthScore = s.checks.healthScore  || p >= 95
                        ),
                        done = p >= steps
                    )
                }

                delay(tick)
            }
        }
    }
}
