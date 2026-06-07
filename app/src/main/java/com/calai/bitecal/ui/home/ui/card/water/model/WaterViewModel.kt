package com.calai.bitecal.ui.home.ui.card.water.model

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.calai.bitecal.data.water.repo.WaterRepository
import com.calai.bitecal.data.water.store.WaterPrefsStore
import com.calai.bitecal.data.water.store.WaterUnit
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class WaterUiState(
    val loading: Boolean = true,
    val cups: Int = 0,     // 幾杯
    val ml: Int = 0,       // 累積毫升
    val flOz: Int = 0,     // 累積 fl oz
    val unit: WaterUnit = WaterUnit.ML,
    val error: String? = null
)

@HiltViewModel
class WaterViewModel @Inject constructor(
    private val repo: WaterRepository,
    private val prefs: WaterPrefsStore
) : ViewModel() {

    private companion object {
        const val CUP_ML = 237
        const val CUP_FL_OZ = 8
        const val ADJUST_COALESCE_MS = 180L
        const val RESYNC_COALESCE_MS = 90L
    }

    private val _ui = MutableStateFlow(WaterUiState())
    private var pendingCupsDelta: Int = 0
    private var adjustJob: Job? = null
    val ui: StateFlow<WaterUiState> =
        combine(_ui, prefs.unitFlow) { base, unitPref ->
            base.copy(unit = unitPref)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = WaterUiState()
        )

    init {
        refresh()
    }

    /** 重新打後端 GET /water/today */
    fun refresh() = viewModelScope.launch {
        _ui.update { it.copy(loading = it.cups == 0 && it.ml == 0, error = null) }
        runCatching { withContext(Dispatchers.IO) { repo.loadToday() } }
            .onSuccess { dto -> applyServerSummary(dto.cups, dto.ml, dto.flOz) }
            .onFailure { e ->
                _ui.update { it.copy(loading = false, error = e.message) }
            }
    }

    /** +1 杯 或 -1 杯：先更新 UI，再把短時間連點合併成較少 API 請求。 */
    fun adjust(delta: Int) {
        val acceptedDelta = applyOptimisticDelta(delta)
        if (acceptedDelta == 0) return

        pendingCupsDelta += acceptedDelta
        if (adjustJob?.isActive == true) return

        adjustJob = viewModelScope.launch {
            delay(ADJUST_COALESCE_MS)
            while (pendingCupsDelta != 0) {
                val deltaToSync = pendingCupsDelta
                pendingCupsDelta = 0

                runCatching {
                    withContext(Dispatchers.IO) { repo.adjustCups(deltaToSync) }
                }.onSuccess { dto ->
                    applyServerSummary(dto.cups, dto.ml, dto.flOz)
                }.onFailure { e ->
                    _ui.update { it.copy(loading = false, error = e.message) }
                    refresh()
                    return@launch
                }

                if (pendingCupsDelta != 0) {
                    delay(RESYNC_COALESCE_MS)
                }
            }
        }
    }

    private fun applyOptimisticDelta(delta: Int): Int {
        if (delta == 0) return 0

        var acceptedDelta = 0
        _ui.update { current ->
            val currentCups = current.cups.coerceAtLeast(0)
            val nextCups = (currentCups + delta).coerceAtLeast(0)
            acceptedDelta = nextCups - currentCups

            if (acceptedDelta == 0) {
                current.copy(loading = false, error = null)
            } else {
                current.copy(
                    loading = false,
                    cups = nextCups,
                    ml = nextCups * CUP_ML,
                    flOz = nextCups * CUP_FL_OZ,
                    error = null
                )
            }
        }
        return acceptedDelta
    }

    private fun applyServerSummary(cups: Int, ml: Int, flOz: Int) {
        val pendingAfterResponse = pendingCupsDelta
        val displayCups = (cups + pendingAfterResponse).coerceAtLeast(0)
        val hasPendingLocalClicks = pendingAfterResponse != 0

        _ui.update {
            it.copy(
                loading = false,
                cups = displayCups,
                ml = if (hasPendingLocalClicks) displayCups * CUP_ML else ml.coerceAtLeast(0),
                flOz = if (hasPendingLocalClicks) displayCups * CUP_FL_OZ else flOz.coerceAtLeast(0),
                error = null
            )
        }
    }

    /** 使用者在齒輪按鈕切單位 ML <-> OZ */
    fun toggleUnit() = viewModelScope.launch {
        val next = when (ui.value.unit) {
            WaterUnit.ML -> WaterUnit.OZ
            WaterUnit.OZ -> WaterUnit.ML
        }
        prefs.setUnit(next)
        // _ui 不用手動改，因為 combine() 會自動回推
    }
}
