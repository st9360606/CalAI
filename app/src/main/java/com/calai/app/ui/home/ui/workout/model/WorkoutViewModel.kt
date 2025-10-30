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
import kotlinx.coroutines.flow.collectLatest
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
    val errorScanFailed: Boolean = false,                // Scan Failed (7.jpg)
    val saving: Boolean = false                          // 防止連點送出
)

@HiltViewModel
class WorkoutViewModel @Inject constructor(
    private val repo: WorkoutRepository,
    private val todayStore: WorkoutTodayStore
) : ViewModel() {

    private val _ui = MutableStateFlow(WorkoutUiState())
    val ui: StateFlow<WorkoutUiState> = _ui

    // 避免多次呼叫 init() 造成重複收集
    @Volatile private var initialized = false

    // 這組是你想要顯示在列表裡的固定項目 (跟截圖一樣)
    // ※ 僅作為後端失敗時的 UI 後備；若後端不可用，儲存也會失敗屬正常情況。
    private val fallbackPresets = listOf(
        PresetWorkoutDto(1L,  "Walking",            140, "walk"),
        PresetWorkoutDto(2L,  "Running",            350, "run"),
        PresetWorkoutDto(3L,  "Cycling",            325, "bike"),
        PresetWorkoutDto(4L,  "Swimming",           400, "swimming"),
        PresetWorkoutDto(5L,  "Hiking",             300, "hiking"),
        PresetWorkoutDto(6L,  "Aerobic exercise",   350, "aerobic_exercise"),
        PresetWorkoutDto(7L,  "Strength Training",  240, "strength"),
        PresetWorkoutDto(8L,  "Weight training",    300, "weight_training"),
        PresetWorkoutDto(9L,  "Basketball",         300, "basketball"),
        PresetWorkoutDto(10L, "Soccer",             320, "soccer"),
        PresetWorkoutDto(11L, "Tennis",             250, "tennis"),
        PresetWorkoutDto(12L, "Yoga",               190, "yoga")
    )

    /**
     * 初始化 Workout Tracker：
     * - 抓 /presets 與 /today（透過 todayStore）
     * - 如果 /presets 失敗或為空，就塞 fallbackPresets
     * - 只初始化一次，並長期收集 todayStore.today 讓所有畫面同步
     */
    fun init() {
        if (initialized) return
        initialized = true

        viewModelScope.launch {
            // 1) 載入 presets（失敗時用 fallback）
            val presetsToUse = try {
                val server = repo.loadPresets()
                if (server.isNullOrEmpty()) fallbackPresets else server
            } catch (_: Exception) {
                fallbackPresets
            }
            _ui.value = _ui.value.copy(presets = presetsToUse)

            // 2) 取得今天資料（透過 store；會自帶 X-Client-Timezone）
            runCatching { todayStore.refresh() }

            // 3) 長期收集 today 狀態，讓 ActivityHistoryScreen 立即更新
            viewModelScope.launch {
                todayStore.today.collectLatest { resp ->
                    _ui.value = _ui.value.copy(today = resp)
                }
            }
        }
    }

    fun onTextChanged(v: String) {
        _ui.value = _ui.value.copy(textInput = v)
    }

    /** 自由文字估算 → 顯示估算結果彈窗 */
    fun estimate() {
        val text = _ui.value.textInput.trim()
        if (text.isBlank()) return

        viewModelScope.launch {
            _ui.value = _ui.value.copy(
                estimating = true,
                errorScanFailed = false,
                estimateResult = null
            )
            val resp = runCatching { repo.estimateFreeText(text) }.getOrElse {
                _ui.value = _ui.value.copy(estimating = false, errorScanFailed = true)
                return@launch
            }

            if (resp.status == "ok") {
                _ui.value = _ui.value.copy(estimating = false, estimateResult = resp)
            } else {
                _ui.value = _ui.value.copy(estimating = false, errorScanFailed = true)
            }
        }
    }

    /** 在估算彈窗按下 Save → 寫 DB → 更新 today */
    fun confirmSaveFromEstimate() {
        val r = _ui.value.estimateResult ?: return
        val activityId = r.activityId ?: return
        val minutes = r.minutes ?: return
        if (_ui.value.saving) return

        viewModelScope.launch {
            _ui.value = _ui.value.copy(saving = true)
            val logResp = runCatching {
                repo.saveWorkout(activityId = activityId, minutes = minutes, kcal = r.kcal)
            }.getOrElse { e ->
                _ui.value = _ui.value.copy(saving = false, toastMessage = e.message ?: "Failed to save")
                return@launch
            }

            // 更新全域 today，歷史畫面立即刷新
            todayStore.setFromServer(logResp.today)
            _ui.value = _ui.value.copy(
                saving = false,
                toastMessage = "Workout saved successfully!",
                estimateResult = null,
                textInput = ""
            )
        }
    }

    /** 點擊預設活動的「+」→ 打開時長選擇面板 */
    fun openDurationPicker(preset: PresetWorkoutDto) {
        _ui.value = _ui.value.copy(showDurationPickerFor = preset)
    }

    /** 在時長面板按 Save → 寫 DB → 更新 today → 關閉面板 */
    fun savePresetDuration(minutes: Int) {
        val preset = _ui.value.showDurationPickerFor ?: return
        if (minutes <= 0 || _ui.value.saving) return

        viewModelScope.launch {
            _ui.value = _ui.value.copy(saving = true)
            val logResp = runCatching {
                // kcal = null → 後端依 MET*體重*分鐘計算
                repo.saveWorkout(activityId = preset.activityId, minutes = minutes, kcal = null)
            }.getOrElse { e ->
                _ui.value = _ui.value.copy(saving = false, toastMessage = e.message ?: "Failed to save")
                return@launch
            }

            todayStore.setFromServer(logResp.today)
            _ui.value = _ui.value.copy(
                saving = false,
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
