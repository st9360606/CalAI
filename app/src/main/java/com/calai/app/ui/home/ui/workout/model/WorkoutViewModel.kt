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

    // 狀態控制 (對應你的畫面流程)
    val estimating: Boolean = false,
    val estimateResult: EstimateResponse? = null,        // (6.jpg) 確認卡路里頁
    val showDurationPickerFor: PresetWorkoutDto? = null, // (2.jpg) 選分鐘底板
    val toastMessage: String? = null,                    // (3.jpg) "Workout saved successfully!"
    val errorScanFailed: Boolean = false                 // (7.jpg) Scan Failed
)

@HiltViewModel
class WorkoutViewModel @Inject constructor(
    private val repo: WorkoutRepository,
    private val todayStore: WorkoutTodayStore
) : ViewModel() {

    private val _ui = MutableStateFlow(WorkoutUiState())
    val ui: StateFlow<WorkoutUiState> = _ui

    /**
     * 初始化 Workout Tracker bottom sheet:
     * - 載入預設清單 (Walking / Running ...)
     * - 載入今天已記錄的活動與總消耗 (給 Activity History / Home 卡片)
     *
     * 注意：repo.loadToday() 內部現在會自動帶 X-Client-Timezone
     */
    fun init() {
        viewModelScope.launch {
            val presets = repo.loadPresets()
            val today = repo.loadToday()

            _ui.value = _ui.value.copy(
                presets = presets,
                today = today
            )

            // 同步到全域 store，讓 Home 畫面 ACTIVITY 卡片即時更新
            todayStore.setFromServer(today)
        }
    }

    fun onTextChanged(v: String) {
        _ui.value = _ui.value.copy(textInput = v)
    }

    /**
     * WS2：
     * 使用者輸入「15 min walking」→ 按 Add Workout
     * 我們先呼叫 /estimate
     * UI 流程：
     *   1. estimating = true → 顯示 (5.jpg) loading
     *   2. 如果 status="ok" → estimateResult = resp → 彈出 (6.jpg)
     *   3. 否則 → errorScanFailed = true → 彈出 (7.jpg)
     */
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

    /**
     * WS3：
     * (6.jpg) 畫面按 Save
     *
     * 行為：
     *   - 呼叫 /log，後端回傳 LogWorkoutResponse { savedSession, today }
     *   - 我們不用再手動 call /today()，因為 today 已經是「依照 X-Client-Timezone 切出的當地今天」
     *   - 更新 todayStore → Home ACTIVITY 卡片的 kcal 立刻刷新
     *   - 清空 textInput
     *   - 顯示 "Workout saved successfully!"
     */
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

            val today = logResp.today
            todayStore.setFromServer(today)

            _ui.value = _ui.value.copy(
                today = today,
                toastMessage = "Workout saved successfully!",
                estimateResult = null,
                textInput = "" // 清掉輸入框
            )
        }
    }

    /**
     * WS4：
     * 預設清單 (Walking / Running...) 右邊的「＋」被點到
     * → 顯示 (2.jpg) 的時間選擇底板 (DurationPickerSheet)
     */
    fun openDurationPicker(preset: PresetWorkoutDto) {
        _ui.value = _ui.value.copy(showDurationPickerFor = preset)
    }

    /**
     * WS4：
     * 使用者在 DurationPickerSheet (2.jpg) 選好分鐘數按 Save
     *
     * 行為：
     *   - 呼叫 /log(activityId=那個 preset, minutes=使用者選的分鐘)
     *   - 同樣直接拿回 logResp.today 做 UI 更新，不再額外呼叫 /today()
     */
    fun savePresetDuration(minutes: Int) {
        val preset = _ui.value.showDurationPickerFor ?: return

        viewModelScope.launch {
            val logResp = repo.saveWorkout(
                activityId = preset.activityId,
                minutes = minutes,
                kcal = null // 後端會自己算 kcal
            )

            val today = logResp.today
            todayStore.setFromServer(today)

            _ui.value = _ui.value.copy(
                today = today,
                toastMessage = "Workout saved successfully!",
                showDurationPickerFor = null
            )
        }
    }

    /** 關掉上方白色圓角 Toast (3.jpg 風格) */
    fun clearToast() {
        _ui.value = _ui.value.copy(toastMessage = null)
    }

    /**
     * 關閉各種 Dialog/Sheet：
     * - Loading(5.jpg)
     * - Estimate 結果(6.jpg)
     * - Scan Failed(7.jpg)
     * - Duration picker(2.jpg)
     */
    fun dismissDialogs() {
        _ui.value = _ui.value.copy(
            estimating = false,
            estimateResult = null,
            errorScanFailed = false,
            showDurationPickerFor = null
        )
    }
}
