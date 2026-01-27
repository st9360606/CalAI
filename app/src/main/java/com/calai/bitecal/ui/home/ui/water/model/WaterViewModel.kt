package com.calai.bitecal.ui.home.ui.water.model

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
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
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

    private val _ui = MutableStateFlow(WaterUiState())
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
        _ui.update { it.copy(loading = true, error = null) }
        runCatching { repo.loadToday() }
            .onSuccess { dto ->
                _ui.update {
                    it.copy(
                        loading = false,
                        cups = dto.cups,
                        ml = dto.ml,
                        flOz = dto.flOz,
                        error = null
                    )
                }
            }
            .onFailure { e ->
                _ui.update { it.copy(loading = false, error = e.message) }
            }
    }

    /** +1 杯 或 -1 杯 */
    fun adjust(delta: Int) = viewModelScope.launch {
        _ui.update { it.copy(loading = true, error = null) }
        runCatching { repo.adjustCups(delta) }
            .onSuccess { dto ->
                _ui.update {
                    it.copy(
                        loading = false,
                        cups = dto.cups,
                        ml = dto.ml,
                        flOz = dto.flOz,
                        error = null
                    )
                }
            }
            .onFailure { e ->
                _ui.update { it.copy(loading = false, error = e.message) }
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
