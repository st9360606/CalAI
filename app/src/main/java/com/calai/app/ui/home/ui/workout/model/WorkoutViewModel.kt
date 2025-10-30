package com.calai.app.ui.home.ui.workout.model

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.calai.app.data.workout.api.EstimateResponse
import com.calai.app.data.workout.api.PresetWorkoutDto
import com.calai.app.data.workout.api.TodayWorkoutResponse
import com.calai.app.data.workout.repo.WorkoutRepository
import com.calai.app.data.workout.store.WorkoutTodayStore
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
    val estimateResult: EstimateResponse? = null,        // (6.jpg)
    val showDurationPickerFor: PresetWorkoutDto? = null, // (2.jpg)
    val toastMessage: String? = null,                    // 成功儲存吐司
    val errorScanFailed: Boolean = false                 // Scan Failed (7.jpg)
)

@HiltViewModel
class WorkoutViewModel @Inject constructor(
    private val repo: WorkoutRepository,
    private val todayStore: WorkoutTodayStore
) : ViewModel() {

    private val _ui = MutableStateFlow(WorkoutUiState())
    val ui: StateFlow<WorkoutUiState> = _ui

    // 這組是你想要顯示在列表裡的固定項目 (跟截圖一樣)
    private val fallbackPresets = listOf(
        PresetWorkoutDto(
            activityId = 1L,
            name = "Walking",
            kcalPer30Min = 140,
            iconKey = "walk"
        ),
        PresetWorkoutDto(
            activityId = 2L,
            name = "Running",
            kcalPer30Min = 350,
            iconKey = "run"
        ),
        PresetWorkoutDto(
            activityId = 3L,
            name = "Cycling",
            kcalPer30Min = 325,
            iconKey = "bike"
        ),
        PresetWorkoutDto(
            activityId = 4L,
            name = "Swimming",
            kcalPer30Min = 400,
            iconKey = "swimming"
        ),
        PresetWorkoutDto(
            activityId = 5L,
            name = "Hiking",
            kcalPer30Min = 300,
            iconKey = "hiking"
        ),
        PresetWorkoutDto(
            activityId = 6L,
            name = "Aerobic exercise",
            kcalPer30Min = 350,
            iconKey = "aerobic_exercise"
        ),
        PresetWorkoutDto(
            activityId = 7L,
            name = "Strength Training",
            kcalPer30Min = 240,
            iconKey = "strength"
        ),
        PresetWorkoutDto(
            activityId = 8L,
            name = "Weight training",
            kcalPer30Min = 300,
            iconKey = "weight_training"
        ),
        PresetWorkoutDto(
            activityId = 7L,
            name = "Basketball",
            kcalPer30Min = 300,
            iconKey = "basketball"
        ),
        PresetWorkoutDto(
            activityId = 8L,
            name = "Soccer",
            kcalPer30Min = 320,
            iconKey = "soccer"
        ),
        PresetWorkoutDto(
            activityId = 9L,
            name = "Tennis",
            kcalPer30Min = 250,
            iconKey = "tennis"
        ),
        PresetWorkoutDto(
            activityId = 10L,
            name = "Yoga",
            kcalPer30Min = 190,
            iconKey = "yoga"
        ),
    )

    /**
     * 初始化 Workout Tracker：
     * - 抓 /presets 跟 /today
     * - 如果 /presets 是空的 / 失敗，就塞 fallbackPresets
     */
    fun init() {
        viewModelScope.launch {
            // 嘗試叫後端
            val fromServerPresets = try {
                repo.loadPresets()
            } catch (e: Exception) {
                emptyList()
            }

            val presetsToUse = if (fromServerPresets.isNullOrEmpty()) {
                fallbackPresets
            } else {
                fromServerPresets
            }

            val todayResp = try {
                repo.loadToday()
            } catch (e: Exception) {
                null
            }

            _ui.value = _ui.value.copy(
                presets = presetsToUse,
                today = todayResp
            )

            // 同步給 Home，那張 ACTIVITY 卡能即時更新 kcal
            if (todayResp != null) {
                todayStore.setFromServer(todayResp)
            }
        }
    }

    fun onTextChanged(v: String) {
        _ui.value = _ui.value.copy(textInput = v)
    }

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

    fun confirmSaveFromEstimate() {
        val r = _ui.value.estimateResult ?: return
        val activityId = r.activityId ?: return
        val minutes = r.minutes ?: return

        viewModelScope.launch {
            val logResp = repo.saveWorkout(
                activityId = activityId,
                minutes = minutes,
                kcal = r.kcal
            )

            val todayResp = logResp.today
            todayStore.setFromServer(todayResp)

            _ui.value = _ui.value.copy(
                today = todayResp,
                toastMessage = "Workout saved successfully!",
                estimateResult = null,
                textInput = ""
            )
        }
    }

    fun openDurationPicker(preset: PresetWorkoutDto) {
        _ui.value = _ui.value.copy(showDurationPickerFor = preset)
    }

    fun savePresetDuration(minutes: Int) {
        val preset = _ui.value.showDurationPickerFor ?: return

        viewModelScope.launch {
            val logResp = repo.saveWorkout(
                activityId = preset.activityId,
                minutes = minutes,
                kcal = null // 後端自行算 kcal
            )

            val todayResp = logResp.today
            todayStore.setFromServer(todayResp)

            _ui.value = _ui.value.copy(
                today = todayResp,
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
