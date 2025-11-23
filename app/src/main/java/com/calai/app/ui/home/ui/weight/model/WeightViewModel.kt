package com.calai.app.ui.home.ui.weight.model

import android.util.Log
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
        val unit: UserProfileStore.WeightUnit = UserProfileStore.WeightUnit.LBS,
        val range: String = "season",

        // --- 以 kg 為主的欄位 ---
        val goal: Double? = null,       // kg
        val current: Double? = null,    // kg

        // --- DB / API 回來的 lbs ---
        val goalLbs: Double? = null,
        val currentLbs: Double? = null,

        // --- Profile（本機快照），用來當最後一層 fallback ---
        val profileWeightKg: Double? = null,
        val profileWeightLbs: Double? = null,           // ★ 新增
        val profileTargetWeightKg: Double? = null,
        val profileTargetWeightLbs: Double? = null,     // ★ 新增

        val achievedPercent: Double = 0.0,
        val series: List<WeightItemDto> = emptyList(),
        val history7: List<WeightItemDto> = emptyList(),
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

        // 監聽 kg
        viewModelScope.launch {
            store.weightKgFlow.distinctUntilChanged()
                .collect { w -> _ui.update { it.copy(profileWeightKg = w?.toDouble()) } }
        }
        // ★ 監聽 lbs
        viewModelScope.launch {
            store.weightLbsFlow.distinctUntilChanged()
                .collect { w -> _ui.update { it.copy(profileWeightLbs = w?.toDouble()) } }
        }
        // 監聽 target kg
        viewModelScope.launch {
            store.targetWeightKgFlow.distinctUntilChanged()
                .collect { t -> _ui.update { it.copy(profileTargetWeightKg = t?.toDouble()) } }
        }
        // ★ 監聽 target lbs
        viewModelScope.launch {
            store.targetWeightLbsFlow.distinctUntilChanged()
                .collect { t -> _ui.update { it.copy(profileTargetWeightLbs = t?.toDouble()) } }
        }

    }

    fun initIfNeeded() {
        if (initialized) return
        initialized = true
        viewModelScope.launch {
            // 先試著補 baseline（失敗不擋流程）
            runCatching {
                repo.ensureBaseline()
            }.onFailure { e ->
                if (e is CancellationException) throw e
            }
            // 再照你原本邏輯 refresh()
            refresh()
        }
    }

    /** 使用者切單位 → 先樂觀更新，接著落盤（之後開啟會沿用） */
    fun setUnit(u: UserProfileStore.WeightUnit) = viewModelScope.launch {
        if (u != _ui.value.unit) _ui.update { it.copy(unit = u) } // 避免快閃
        store.setWeightUnit(u) // 只在用戶操作時寫入
    }

    fun setRange(r: String) {
        _ui.update { it.copy(range = r) }; refresh()
    }

    fun refresh() = viewModelScope.launch {
        runCatching {
            val range = _ui.value.range

            // 後端 summary + 最近 7 筆歷史
            val summary = repo.summary(range)
            val history = repo.recent7()
            val snapshot = _ui.value
            val today = LocalDate.now()

            // 1) 從 weight_timeseries（summary.series）挑出「今天或最近的一筆」
            val currentFromSeries = pickCurrentFromTimeseries(
                series = summary.series,
                today = today
            )

            Log.d(
                "WeightVM",
                "refresh range=$range, series.size=${summary.series.size}, " +
                        "currentFromSeries=${currentFromSeries?.logDate}:" +
                        "${currentFromSeries?.weightKg}/${currentFromSeries?.weightLbs}"
            )

            // 2) 目標體重：Summary > Profile snapshot
            val effectiveGoalKg =
                summary.goalKg ?: snapshot.profileTargetWeightKg
            val effectiveGoalLbs =
                summary.goalLbs ?: snapshot.profileTargetWeightLbs

            // 3) CURRENT WEIGHT：
            //    1) weight_timeseries（today or latest past）
            //    2) summary.current*
            //    3) user_profiles（snapshot 的 profileWeight*）
            val effectiveCurrentKg =
                currentFromSeries?.weightKg
                    ?: summary.currentKg
                    ?: snapshot.profileWeightKg

            val effectiveCurrentLbs =
                currentFromSeries?.weightLbs
                    ?: summary.currentLbs
                    ?: snapshot.profileWeightLbs

            _ui.update { state ->
                state.copy(
                    goal       = effectiveGoalKg,
                    goalLbs    = effectiveGoalLbs,
                    current    = effectiveCurrentKg,
                    currentLbs = effectiveCurrentLbs,

                    achievedPercent = summary.achievedPercent,
                    series           = summary.series,          // weight_timeseries
                    history7         = history,

                    // ⬇️ 這個欄位名稱要跟你的 SummaryDto 一致
                    // 如果你的 data class 是 firstWeightKgAllTime，就改那個名字
                    firstWeightAllTimeKg = summary.firstWeightKgAllTimeKg,
                    // ★ 新增：優先拿後端 Summary，沒有才保留原本 DataStore 的值
                    profileWeightKg  = summary.profileWeightKg ?: state.profileWeightKg,
                    profileWeightLbs = summary.profileWeightLbs ?: state.profileWeightLbs,
                    error = null
                )
            }
        }.onFailure { e ->
            if (e is CancellationException) return@onFailure
            _ui.update { st ->
                st.copy(error = e.message ?: "Unknown error")
            }
        }
    }

    fun save(
        weightKg: Double,
        weightLbs: Double,
        date: LocalDate?,
        photo: File?
    ) = viewModelScope.launch {
        _ui.update { it.copy(saving = true, error = null) }

        runCatching {
            repo.log(
                weightKg = weightKg,
                weightLbs = weightLbs,
                logDate = date?.toString(),
                photoFile = photo
            )
        }
            .onSuccess { saved: WeightItemDto ->
                _ui.update { old ->
                    // 1) 先合併 timeseries
                    val mergedSeries = (old.series + saved)
                        .distinctBy { it.logDate }
                        .sortedBy { it.logDate }

                    // 2) 合併最近 7 筆歷史（畫列表用）
                    val mergedHistory7 = (old.history7 + saved)
                        .distinctBy { it.logDate }
                        .sortedByDescending { it.logDate }
                        .take(7)

                    // 3) 使用同一套「CURRENT WEIGHT 選取規則」
                    val today = LocalDate.now()
                    val latest = pickCurrentFromTimeseries(
                        series = mergedSeries,
                        today = today
                    )

                    val newCurrentKg =
                        latest?.weightKg
                            ?: old.current      // 可能是之前算好的 current
                            ?: old.profileWeightKg

                    val newCurrentLbs =
                        latest?.weightLbs
                            ?: old.currentLbs
                            ?: old.profileWeightLbs

                    old.copy(
                        current = newCurrentKg,
                        currentLbs = newCurrentLbs,
                        series = mergedSeries,
                        history7 = mergedHistory7,
                        error = null
                    )
                }

                // 再跟後端 summary 對齊（achievedPercent / firstWeightAllTimeKg 等）
                refresh()

                _ui.update { it.copy(toastMessage = "Saved successfully") }
            }
            .onFailure { e ->
                if (e is CancellationException) return@onFailure
                _ui.update { st ->
                    st.copy(error = e.message ?: "Save failed")
                }
            }
            .also {
                _ui.update { it.copy(saving = false) }
            }
    }

    fun clearToast() {
        _ui.update { it.copy(toastMessage = null) }
    }

    /**
     * 從 weight_timeseries（前端 = SummaryDto.series）中挑出「CURRENT WEIGHT」：
     *
     * 規則：
     * 1. 只看 log_date <= today 的紀錄（未來日期一律忽略）。
     * 2. 在這些紀錄中選擇 log_date 最大的一筆（等同「有當天就當天，否則用過去最新」）。
     * 3. 若沒有任何 log_date <= today 的資料，回傳 null（由呼叫端決定 fallback）。
     */
    private fun pickCurrentFromTimeseries(
        series: List<WeightItemDto>,
        today: LocalDate = LocalDate.now()
    ): WeightItemDto? {
        if (series.isEmpty()) return null

        val parsed = series.mapNotNull { item ->
            runCatching { LocalDate.parse(item.logDate) }
                .getOrNull()
                ?.let { date -> date to item }
        }
        if (parsed.isEmpty()) return null

        // 只保留「今天或以前」的紀錄，未來日期完全忽略
        val notFuture = parsed.filter { (date, _) -> !date.isAfter(today) }
        if (notFuture.isEmpty()) return null

        return notFuture.maxByOrNull { it.first }?.second
    }

}
