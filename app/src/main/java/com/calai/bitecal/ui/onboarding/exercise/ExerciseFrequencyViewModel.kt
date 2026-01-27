package com.calai.bitecal.ui.onboarding.exercise

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.calai.bitecal.data.profile.repo.UserProfileStore
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * UI 狀態：selected 為「代表值」
 * 0、2、4、6、7 分別代表：0、1–3、3–5、6–7、7+
 */
data class ExerciseFreqUiState(
    val selected: Int? = null
)

/**
 * 將 DataStore 已存的頻率 (0..7) 映射到 UI 卡片代表值。
 * 邏輯：
 * 0 -> 0
 * 1..3 -> 2
 * 4..5 -> 4
 * 6..7 -> 6
 * >=8 -> 7 (理論上不會出現，做保護)
 */
internal fun bucketFreq(saved: Int): Int = when {
    saved <= 0 -> 0
    saved in 1..3 -> 2
    saved in 4..5 -> 4
    saved in 6..7 -> 6
    else -> 7
}

@HiltViewModel
class ExerciseFrequencyViewModel @Inject constructor(
    private val store: UserProfileStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExerciseFreqUiState())
    val uiState: StateFlow<ExerciseFreqUiState> = _uiState

    init {
        // 啟動時回填已存值對應的卡片（沒有就不預選）
        viewModelScope.launch {
            val saved: Int? = store.exerciseFreqPerWeekFlow.first() // 0..7 或 null
            saved?.let { v ->
                _uiState.update { it.copy(selected = bucketFreq(v)) }
            }
        }
    }

    /** 使用者點選卡片：直接記「代表值」0/2/4/6/7 */
    fun select(representative: Int) {
        _uiState.update { it.copy(selected = representative) }
    }

    /** 寫入 DataStore（僅在非空時）；仍以 Int 0..7 儲存，保持相容 */
    fun saveSelected() {
        val sel = _uiState.value.selected ?: return
        viewModelScope.launch {
            // 代表值已是 0/2/4/6/7，額外 coerceIn 做保護
            store.setExerciseFreqPerWeek(sel.coerceIn(0, 7))
        }
    }
}
