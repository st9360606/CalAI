package com.calai.app.ui.home.ui.weight.model

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.calai.app.data.profile.repo.UserProfileStore
import com.calai.app.data.weight.api.WeightItemDto
import com.calai.app.data.weight.repo.WeightRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class WeightViewModel @Inject constructor(
    private val repo: WeightRepository,
    private val store: UserProfileStore
) : ViewModel() {

    data class UiState(
        // ★ 畫面「預設 LBS（僅 UI 預設，不落盤）」：
        val unit: UserProfileStore.WeightUnit = UserProfileStore.WeightUnit.LBS,
        // ★ 預設就用 90 Days 的 key，與 FilterTabs 第一個 tab 對齊
        val range: String = "season",
        val goal: Double? = null,
        val current: Double? = null,
        val achievedPercent: Double = 0.0,
        val series: List<WeightItemDto> = emptyList(),
        val history7: List<WeightItemDto> = emptyList(),
        val profileWeightKg: Double? = null,
        val profileTargetWeightKg: Double? = null,
        val firstWeightAllTimeKg: Double? = null,
        val error: String? = null,
        val saving: Boolean = false,
        val toastMessage: String? = null
    )

    private val _ui = MutableStateFlow(UiState())
    val ui: StateFlow<UiState> = _ui.asStateFlow()

    private var initialized = false

    init {
        // ① 啟動：只讀一次目前偏好（若為 null，UI 繼續顯示預設 LBS），不做任何寫入
        viewModelScope.launch {
            val saved = store.getWeightUnitOnce() // 可能為 null
            if (saved != null && saved != _ui.value.unit) {
                _ui.update { it.copy(unit = saved) }
            }
            // ② 後續只監聽 DataStore 變更（誰寫都尊重）
            store.weightUnitFlow
                .distinctUntilChanged()
                .collect { u ->
                    if (u != null && u != _ui.value.unit) {
                        _ui.update { it.copy(unit = u) }
                        refresh()
                    }
                }
        }

        // ③ 監聽 profile（作為 current/goal 的本地來源）
        viewModelScope.launch {
            store.weightKgFlow.distinctUntilChanged()
                .collect { w -> _ui.update { it.copy(profileWeightKg = w?.toDouble()) } }
        }
        viewModelScope.launch {
            store.targetWeightKgFlow.distinctUntilChanged()
                .collect { t -> _ui.update { it.copy(profileTargetWeightKg = t?.toDouble()) } }
        }
    }

    fun initIfNeeded() {
        if (!initialized) {
            initialized = true
            viewModelScope.launch { refresh() }
        }
    }

    /** 使用者切單位 → 先樂觀更新，接著落盤（之後開啟會沿用） */
    fun setUnit(u: UserProfileStore.WeightUnit) = viewModelScope.launch {
        if (u != _ui.value.unit) _ui.update { it.copy(unit = u) } // 避免快閃
        store.setWeightUnit(u) // 只在用戶操作時寫入
    }

    fun setRange(r: String) { _ui.update { it.copy(range = r) }; refresh() }

    fun refresh() = viewModelScope.launch {
        runCatching {
            val s = repo.summary(_ui.value.range)
            val h = repo.recent7()
            val snapshot = _ui.value

            val effectiveGoal = snapshot.profileTargetWeightKg ?: s.goalKg

            _ui.update {
                it.copy(
                    goal = effectiveGoal,
                    // ✅ current = 後端 summary 回傳的最新 timeseries 體重（可能為 null）
                    current = s.currentKg,
                    achievedPercent = s.achievedPercent,
                    series = s.series,
                    history7 = h,
                    firstWeightAllTimeKg = s.firstWeightKgAllTimeKg,
                    error = null
                )
            }
        }.onFailure { e ->
            if (e is CancellationException) return@onFailure
            _ui.update { st -> st.copy(error = e.message ?: "Unknown error") }
        }
    }

    fun save(weightKg: Double, date: LocalDate?, photo: File?) = viewModelScope.launch {
        _ui.update { it.copy(saving = true, error = null) }
        runCatching { repo.log(weightKg, date?.toString(), photo) }
            .onSuccess { refresh(); _ui.update { it.copy(toastMessage = "Saved successfully") } }
            .onFailure { e ->
                if (e is CancellationException) return@onFailure
                _ui.update { it.copy(error = e.message ?: "Save failed") }
            }
            .also { _ui.update { it.copy(saving = false) } }
    }

    fun clearToast() { _ui.update { it.copy(toastMessage = null) } }
}
