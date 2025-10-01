package com.calai.app.ui.onboarding.exercise

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.calai.app.data.auth.store.UserProfileStore
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ExerciseFreqUiState(
    val selected: Int? = null // ★ 預設不選（null）
)

@HiltViewModel
class ExerciseFrequencyViewModel @Inject constructor(
    private val store: UserProfileStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExerciseFreqUiState())
    val uiState: StateFlow<ExerciseFreqUiState> = _uiState

    init {
        // 若先前已存過，啟動時回填對應卡片；沒有則維持 null（不預選）
        viewModelScope.launch {
            val saved = store.exerciseFreqPerWeekFlow.first() // Int?（0..7）
            saved?.let { v ->
                _uiState.update { it.copy(selected = bucket(v)) }
            }
        }
    }

    fun select(v: Int) {
        _uiState.update { it.copy(selected = v) }
    }

    /** 將目前選擇寫入 DataStore（僅在非空時） */
    fun saveSelected() {
        val sel = _uiState.value.selected ?: return
        viewModelScope.launch {
            store.setExerciseFreqPerWeek(sel.coerceIn(0, 7))
        }
    }

    /** 把任意 0..7 映到三張卡：0–2→2、3–5→5、6+→7 */
    private fun bucket(v: Int): Int = when {
        v >= 6 -> 7
        v >= 3 -> 5
        else -> 2
    }
}
