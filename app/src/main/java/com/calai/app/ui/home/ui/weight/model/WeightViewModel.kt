package com.calai.app.ui.home.ui.weight.model

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.calai.app.data.profile.repo.ProfileRepository
import com.calai.app.data.profile.repo.UserProfileStore
import com.calai.app.data.weight.api.WeightItemDto
import com.calai.app.data.weight.repo.WeightRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class WeightViewModel @Inject constructor(
    private val repo: WeightRepository,
    private val store: UserProfileStore,
    private val profileRepo: ProfileRepository
) : ViewModel() {

    data class UiState(
        /** ✅ 已提交（commit）的顯示單位：只會在「成功存檔」後才改變 */
        val unit: UserProfileStore.WeightUnit = UserProfileStore.WeightUnit.LBS,

        /** ✅ 草稿單位：使用者切換時先放這裡，成功存檔後才 commit 到 unit */
        val pendingUnit: UserProfileStore.WeightUnit? = null,

        val range: String = "season",

        // --- 以 kg 為主的欄位 ---
        val goal: Double? = null,       // kg
        val current: Double? = null,    // kg
        val goalLbs: Double? = null,
        val currentLbs: Double? = null,

        // --- Profile（本機快照），用來當最後一層 fallback ---
        val profileWeightKg: Double? = null,
        val profileWeightLbs: Double? = null,
        val profileTargetWeightKg: Double? = null,
        val profileTargetWeightLbs: Double? = null,

        val achievedPercent: Double = 0.0,
        val series: List<WeightItemDto> = emptyList(),
        val history7: List<WeightItemDto> = emptyList(),
        val firstWeightAllTimeKg: Double? = null,
        val error: String? = null,
        val saving: Boolean = false,
        val toastMessage: String? = null
    ) {
        /** ✅ 給 UI 使用：如果正在編輯就用 pendingUnit，否則用 unit */
        fun displayUnit(): UserProfileStore.WeightUnit = pendingUnit ?: unit
    }

    private val _ui = MutableStateFlow(UiState())
    val ui: StateFlow<UiState> = _ui.asStateFlow()

    private var initialized = false

    // =========================
    // ★ FIX：history7 排序穩定化（新→舊）
    // =========================
    private fun parseDateOrMin(s: String): LocalDate =
        runCatching { LocalDate.parse(s) }.getOrElse { LocalDate.MIN }

    private fun sortHistoryDescStable(list: List<WeightItemDto>): List<WeightItemDto> {
        return list
            .sortedWith(compareByDescending<WeightItemDto> { parseDateOrMin(it.logDate) })
            .toList()
    }

    init {
        // ✅ Unit：DataStore 是唯一真相（永遠不會被「讀一次」覆蓋回去）
        viewModelScope.launch {
            store.weightUnitFlow
                .map { it ?: UserProfileStore.WeightUnit.LBS }   // null -> 預設
                .distinctUntilChanged()
                .collect { u ->
                    Log.d("WeightVM", "weightUnitFlow emit = $u")
                    _ui.update { it.copy(unit = u) }
                }
        }

        // 其他 flows 你原本那些照舊（kg/lbs/target...）
        viewModelScope.launch {
            store.weightKgFlow.distinctUntilChanged()
                .collect { w -> _ui.update { it.copy(profileWeightKg = w?.toDouble()) } }
        }
        viewModelScope.launch {
            store.weightLbsFlow.distinctUntilChanged()
                .collect { w -> _ui.update { it.copy(profileWeightLbs = w?.toDouble()) } }
        }
        viewModelScope.launch {
            store.targetWeightKgFlow.distinctUntilChanged()
                .collect { t -> _ui.update { it.copy(profileTargetWeightKg = t?.toDouble()) } }
        }
        viewModelScope.launch {
            store.targetWeightLbsFlow.distinctUntilChanged()
                .collect { t -> _ui.update { it.copy(profileTargetWeightLbs = t?.toDouble()) } }
        }
    }

    fun initIfNeeded() {
        if (initialized) return
        initialized = true
        viewModelScope.launch {
            runCatching { repo.ensureBaseline() }
                .onFailure { e -> if (e is CancellationException) throw e }

            refresh()
        }
    }

    /** ✅ 使用者切單位：只寫入 DataStore；UI 會因 flow emit 自動更新 */
    fun setUnit(u: UserProfileStore.WeightUnit) = viewModelScope.launch {
        Log.d("WeightVM", "setUnit() called u=$u")
        runCatching { store.setWeightUnit(u) }
            .onFailure { e ->
                Log.e("WeightVM", "setWeightUnit failed", e)
                _ui.update { it.copy(error = "Change unit failed") }
            }
    }

    /** 使用者按取消：丟棄草稿單位 */
    fun discardPendingUnit() {
        _ui.update { it.copy(pendingUnit = null) }
    }

    /** ✅ 只有在「存檔成功」時才呼叫：commit 單位到 DataStore + 更新 ui.unit */
    private suspend fun commitUnitAfterSuccess(u: UserProfileStore.WeightUnit) {
        // 先更新 UI（避免 UI 等 DataStore 回寫才變）
        _ui.update { it.copy(unit = u, pendingUnit = null) }
        // 再落盤
        runCatching { store.setWeightUnit(u) }
            .onFailure { e ->
                // 若落盤失敗：不要把流程打爆，但你會在 Log 看到
                Log.w("WeightVM", "commitUnitAfterSuccess failed: ${e.message}", e)
            }
    }

    fun setRange(r: String) {
        _ui.update { it.copy(range = r) }
        refresh()
    }

    fun refresh() = viewModelScope.launch {
        runCatching {
            val range = _ui.value.range

            val summary = repo.summary(range)
            val history = sortHistoryDescStable(repo.recent7()).take(7)

            val snapshot = _ui.value
            val today = LocalDate.now()

            val currentFromSeries = pickCurrentFromTimeseries(
                series = summary.series,
                today = today
            )

            val effectiveGoalKg = summary.goalKg
            val effectiveGoalLbs = summary.goalLbs

            val effectiveCurrentKg =
                currentFromSeries?.weightKg
                    ?: summary.currentKg
                    ?: snapshot.profileWeightKg

            val effectiveCurrentLbs =
                currentFromSeries?.weightLbs
                    ?: summary.currentLbs
                    ?: snapshot.profileWeightLbs

            Log.d(
                "WeightVM",
                "summary range=$range " +
                        "goalKg=${summary.goalKg}, goalLbs=${summary.goalLbs}, " +
                        "currentKg=${summary.currentKg}, currentLbs=${summary.currentLbs}, " +
                        "profileWeightKg=${summary.profileWeightKg}, profileWeightLbs=${summary.profileWeightLbs}, " +
                        "currentFromSeriesKg=${currentFromSeries?.weightKg}, currentFromSeriesLbs=${currentFromSeries?.weightLbs}"
            )

            _ui.update { state ->
                state.copy(
                    goal = effectiveGoalKg,
                    goalLbs = effectiveGoalLbs,
                    current = effectiveCurrentKg,
                    currentLbs = effectiveCurrentLbs,

                    achievedPercent = summary.achievedPercent,
                    series = summary.series,
                    history7 = history,

                    firstWeightAllTimeKg = summary.firstWeightKgAllTime,

                    profileWeightKg = summary.profileWeightKg ?: state.profileWeightKg,
                    profileWeightLbs = summary.profileWeightLbs ?: state.profileWeightLbs,
                    error = null
                )
            }
        }.onFailure { e ->
            if (e is CancellationException) return@onFailure
            _ui.update { st -> st.copy(error = e.message ?: "Unknown error") }
        }
    }

    /**
     * ✅ 更穩版本：只有「存檔成功」後，才把本次使用的單位寫回 DataStore
     */
    fun save(
        weightKg: Double,
        weightLbs: Double,
        date: LocalDate?,
        photo: File?,
        unitUsedToPersist: UserProfileStore.WeightUnit? = null
    ) = viewModelScope.launch {
        _ui.update { it.copy(saving = true, error = null) }

        runCatching {
            repo.log(
                weightKg = weightKg,
                weightLbs = weightLbs,
                logDate = date?.toString(),
                photoFile = photo
            )
        }.onSuccess { saved ->
            // ✅ 只有成功才切單位（你要的穩定版）
            if (unitUsedToPersist != null) {
                Log.d("WeightVM", "save success -> persist unit = $unitUsedToPersist")
                runCatching { store.setWeightUnit(unitUsedToPersist) }
            }

            // 你原本 merge series/history + refresh + toast 的流程照舊即可
            // ...
            refresh()
            _ui.update { it.copy(toastMessage = "Saved successfully !", saving = false, error = null) }

        }.onFailure { e ->
            if (e is CancellationException) return@onFailure
            _ui.update { it.copy(error = "Save failed", toastMessage = null, saving = false) }
        }
    }

    fun clearToast() {
        _ui.update { it.copy(toastMessage = null) }
    }

    fun clearError() {
        _ui.update { it.copy(error = null) }
    }

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

        val notFuture = parsed.filter { (date, _) -> !date.isAfter(today) }
        if (notFuture.isEmpty()) return null

        return notFuture.maxByOrNull { it.first }?.second
    }

    /**
     * 更新目標體重（成功才切單位）
     */
    fun updateTargetWeight(
        value: Double,
        unit: UserProfileStore.WeightUnit,
        onResult: (Result<Unit>) -> Unit
    ) {
        viewModelScope.launch {
            val res = profileRepo.updateTargetWeight(value, unit)
            res.onSuccess {
                // ✅ 成功才 commit 單位（避免失敗也切）
                commitUnitAfterSuccess(unit)

                runCatching { refresh() }
                _ui.update {
                    it.copy(
                        toastMessage = "Target weight updated !",
                        error = null
                    )
                }
                onResult(Result.success(Unit))
            }.onFailure { e ->
                _ui.update {
                    it.copy(
                        error = "Save failed",
                        toastMessage = null
                    )
                }
                onResult(Result.failure(e))
            }
        }
    }
}
