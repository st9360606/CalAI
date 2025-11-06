package com.calai.app.ui.home.ui.workout.model

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.calai.app.data.workout.api.EstimateResponse
import com.calai.app.data.workout.api.PresetWorkoutDto
import com.calai.app.data.workout.api.TodayWorkoutResponse
import com.calai.app.data.workout.repo.WorkoutRepository
import com.calai.app.data.workout.store.WorkoutTodayStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.roundToInt

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
    val saving: Boolean = false,                         // 防止連點送出

    // 一次性導航旗標（true 時讓畫面導到歷史頁，之後會被 consume 清回 false）
    val navigateHistoryOnce: Boolean = false
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

    // ★ 本地 MET 對照（需與後端 workout_dictionary 對齊）
    //   計算公式：kcal = round(met * userKg * (minutes/60))
    private data class FallbackMeta(
        val activityId: Long,
        val name: String,
        val met: Double,
        val iconKey: String
    )

    private val fallbackMeta = listOf(
        FallbackMeta(1L,  "Walking",            3.5, "walk"),
        FallbackMeta(2L,  "Running",            9.0, "run"),
        FallbackMeta(3L,  "Cycling",            8.0, "bike"),
        FallbackMeta(4L,  "Swimming",           8.0, "swimming"),
        FallbackMeta(5L,  "Hiking",             6.0, "hiking"),
        FallbackMeta(6L,  "Aerobic exercise",   8.0, "aerobic_exercise"),
        FallbackMeta(7L,  "Strength Training",  4.0, "strength"),
        FallbackMeta(8L,  "Weight training",    6.0, "weight_training"),
        FallbackMeta(9L,  "Basketball",         8.0, "basketball"),
        FallbackMeta(10L, "Soccer",             8.0, "soccer"),
        FallbackMeta(11L, "Tennis",             7.3, "tennis"),
        FallbackMeta(12L, "Yoga",               3.0, "yoga")
    )

    private fun kcalFor30Min(kg: Double, met: Double): Int {
        val kcal = met * kg * (30.0 / 60.0) // 30 分鐘
        return kcal.roundToInt()
    }

    private fun buildFallbackPresets(userKg: Double): List<PresetWorkoutDto> {
        val kg = if (userKg.isFinite() && userKg > 0.0) userKg else 70.0
        return fallbackMeta.map { m ->
            PresetWorkoutDto(
                activityId = m.activityId,
                name = m.name,
                kcalPer30Min = kcalFor30Min(kg, m.met),
                iconKey = m.iconKey
            )
        }
    }

    /** 初始化：抓 presets / today，並長期收集 todayStore 供所有畫面同步更新 */
    fun init() {
        if (initialized) return
        initialized = true

        viewModelScope.launch {
            // 1) 優先使用伺服器 /presets（已由後端依使用者體重計算）
            val serverPresets = runCatching { repo.loadPresets() }.getOrNull()

            if (!serverPresets.isNullOrEmpty()) {
                _ui.value = _ui.value.copy(presets = serverPresets)
            } else {
                // 2) fallback：取用戶體重計算本地 kcal/30
                val userKg = runCatching { repo.loadMyWeightKg() }.getOrElse { 70.0 }
                val fb = buildFallbackPresets(userKg)
                _ui.value = _ui.value.copy(presets = fb)
            }

            // 3) 取得今天資料（透過 store；會帶 X-Client-Timezone）
            runCatching { todayStore.refresh() }

            // 4) 長期收集 today 狀態，讓 ActivityHistoryScreen 立即更新
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

    // 新增：最少 5 秒轉圈的估算流程
    fun estimateWithSpinner() {
        val text = _ui.value.textInput.trim()
        if (text.isBlank()) return

        viewModelScope.launch {
            // 進入運轉中
            _ui.value = _ui.value.copy(
                estimating = true,
                errorScanFailed = false,
                estimateResult = null
            )

            // 並行：一邊打 API，一邊確保至少 5 秒
            val req = async { runCatching { repo.estimateFreeText(text) }.getOrNull() }
            val minSpinner = async { delay(5_000) }

            val resp = req.await()
            minSpinner.await() // 保證至少 5 秒

            if (resp != null && resp.status == "ok") {
                _ui.value = _ui.value.copy(estimating = false, estimateResult = resp)
            } else {
                _ui.value = _ui.value.copy(estimating = false, errorScanFailed = true)
            }
        }
    }

    /** 估算彈窗按 Save → 寫 DB → 更新 today → 觸發一次性導航 */
    fun confirmSaveFromEstimate() {
        val r = _ui.value.estimateResult ?: return
        val activityId = r.activityId ?: return
        val minutes = r.minutes ?: return
        if (_ui.value.saving) return

        viewModelScope.launch {
            _ui.value = _ui.value.copy(saving = true)
            val logResp = runCatching {
                // kcal = r.kcal 也可以，但為了前後一致，允許 null 由後端重算
                repo.saveWorkout(activityId = activityId, minutes = minutes, kcal = null)
            }.getOrElse { e ->
                _ui.value = _ui.value.copy(saving = false, toastMessage = e.message ?: "Failed to save")
                return@launch
            }

            todayStore.setFromServer(logResp.today)
            _ui.value = _ui.value.copy(
                saving = false,
                toastMessage = "Workout saved successfully!",
                estimateResult = null,
                textInput = "",
            )
        }
    }

    /** 點擊預設活動的「+」→ 打開時長面板 */
    fun openDurationPicker(preset: PresetWorkoutDto) {
        _ui.value = _ui.value.copy(showDurationPickerFor = preset)
    }

    /** 時長面板按 Save → 寫 DB → 更新 today → 關閉面板 → 觸發一次性導航 */
    fun savePresetDuration(minutes: Int) {
        val preset = _ui.value.showDurationPickerFor ?: return
        if (minutes <= 0 || _ui.value.saving) return

        viewModelScope.launch {
            _ui.value = _ui.value.copy(saving = true)
            val logResp = runCatching {
                // kcal = null → 後端依 MET*體重*分鐘計算，與 UI fallback 相同邏輯
                repo.saveWorkout(activityId = preset.activityId, minutes = minutes, kcal = null)
            }.getOrElse { e ->
                _ui.value = _ui.value.copy(saving = false, toastMessage = e.message ?: "Failed to save")
                return@launch
            }

            todayStore.setFromServer(logResp.today)
            _ui.value = _ui.value.copy(
                saving = false,
                toastMessage = "Workout saved successfully!",
                showDurationPickerFor = null,
            )
        }
    }

    /** 清除一次性導航事件（避免回到 Home 又再次觸發） */
    fun consumeNavigateHistory() {
        _ui.value = _ui.value.copy(navigateHistoryOnce = false)
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

    fun refreshToday() {
        viewModelScope.launch {
            runCatching { todayStore.refresh() }
                .onFailure { e ->
                    _ui.value = _ui.value.copy(
                        toastMessage = e.message ?: "Refresh failed"
                    )
                }
        }
    }
}
