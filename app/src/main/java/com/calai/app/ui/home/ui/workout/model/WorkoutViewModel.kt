package com.calai.app.ui.home.ui.workout.model

import com.calai.app.data.workout.store.WorkoutTodayStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.calai.app.data.workout.api.EstimateResponse
import com.calai.app.data.workout.api.PresetWorkoutDto
import com.calai.app.data.workout.api.TodayWorkoutResponse
import com.calai.app.data.workout.repo.WorkoutRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class WorkoutUiState(
    val textInput: String = "",
    val presets: List<PresetWorkoutDto> = emptyList(),
    val today: TodayWorkoutResponse? = null,

    // 狀態控制
    val estimating: Boolean = false,
    val estimateResult: EstimateResponse? = null, // 用於顯示(6.jpg)
    val showDurationPickerFor: PresetWorkoutDto? = null, // (2.jpg)
    val toastMessage: String? = null,
    val errorScanFailed: Boolean = false
)

@HiltViewModel
class WorkoutViewModel @Inject constructor(
    private val repo: WorkoutRepository,
    private val todayStore: WorkoutTodayStore
) : ViewModel() {

    private val _ui = MutableStateFlow(WorkoutUiState())
    val ui: StateFlow<WorkoutUiState> = _ui

    fun init() {
        viewModelScope.launch {
            val presets = repo.loadPresets()
            val today = repo.loadToday()
            _ui.value = _ui.value.copy(presets = presets, today = today)
            todayStore.setFromServer(today) // push 給首頁
        }
    }

    fun onTextChanged(v: String) {
        _ui.value = _ui.value.copy(textInput = v)
    }

    /** WS2：自由輸入 → /estimate → 顯示 Loading(5.jpg) → 結果 or ScanFailed(7.jpg) */
    fun estimate() {
        val text = _ui.value.textInput.trim()
        if (text.isBlank()) return

        viewModelScope.launch {
            _ui.value = _ui.value.copy(
                estimating = true,
                errorScanFailed = false,
                estimateResult = null
            )

            val resp = repo.estimateFreeText(text)
            if (resp.status == "ok") {
                _ui.value = _ui.value.copy(
                    estimating = false,
                    estimateResult = resp
                )
            } else {
                _ui.value = _ui.value.copy(
                    estimating = false,
                    errorScanFailed = true
                )
            }
        }
    }

    /** WS3：使用者在結果頁(6.jpg)按 Save → /log → Toast + 回主畫面 + 刷新首頁卡片 */
    fun confirmSaveFromEstimate() {
        val r = _ui.value.estimateResult ?: return
        val activityId = r.activityId ?: return
        val minutes = r.minutes ?: return

        viewModelScope.launch {
            val logResp = repo.saveWorkout(activityId, minutes, r.kcal)

            // 更新今日資料（Activity History + 首頁卡片）
            val today = repo.loadToday()
            todayStore.setFromServer(today)

            _ui.value = _ui.value.copy(
                today = today,
                toastMessage = "Workout saved successfully!",
                estimateResult = null,
                textInput = "" // 清掉輸入框
            )
        }
    }

    /** WS4：點預設清單的 + → (2.jpg) 選分鐘 → Save */
    fun openDurationPicker(preset: PresetWorkoutDto) {
        _ui.value = _ui.value.copy(showDurationPickerFor = preset)
    }

    fun savePresetDuration(minutes: Int) {
        val preset = _ui.value.showDurationPickerFor ?: return
        viewModelScope.launch {
            val logResp = repo.saveWorkout(
                activityId = preset.activityId,
                minutes = minutes,
                kcal = null
            )

            val today = repo.loadToday()
            todayStore.setFromServer(today)

            _ui.value = _ui.value.copy(
                today = today,
                toastMessage = "Workout saved successfully!",
                showDurationPickerFor = null
            )
        }
    }

    fun clearToast() {
        _ui.value = _ui.value.copy(toastMessage = null)
    }

    fun dismissDialogs() {
        _ui.value = _ui.value.copy(
            estimating = false,
            estimateResult = null,
            errorScanFailed = false,
            showDurationPickerFor = null
        )
    }
}

