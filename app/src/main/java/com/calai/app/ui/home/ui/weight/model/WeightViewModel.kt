package com.calai.app.ui.home.ui.weight.model

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.calai.app.data.common.RepoInvalidationBus
import com.calai.app.data.profile.repo.ProfileRepository
import com.calai.app.data.profile.repo.UserProfileStore
import com.calai.app.data.util.ThrottledRefresher
import com.calai.app.data.weight.api.WeightItemDto
import com.calai.app.data.weight.repo.WeightRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class WeightViewModel @Inject constructor(
    private val repo: WeightRepository,
    private val store: UserProfileStore,
    private val profileRepo: ProfileRepository,
    private val bus: RepoInvalidationBus
) : ViewModel() {

    data class UiState(
        /** 已提交（commit）的顯示單位：只會在「成功存檔」後才改變 */
        val unit: UserProfileStore.WeightUnit = UserProfileStore.WeightUnit.LBS,

        /** 草稿單位：使用者切換時先放這裡，成功存檔後才 commit 到 unit */
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
        val profileGoalWeightKg: Double? = null,
        val profileGoalWeightLbs: Double? = null,

        val achievedPercent: Double = 0.0,
        val series: List<WeightItemDto> = emptyList(),
        val history7: List<WeightItemDto> = emptyList(),
        val firstWeightAllTimeKg: Double? = null,

        val error: String? = null,
        val saving: Boolean = false
    )

    private val _ui = MutableStateFlow(UiState())
    val ui: StateFlow<UiState> = _ui.asStateFlow()

    // baseline 只做一次
    private var baselineEnsured = false

    // ✅ 節流：10 秒內重複進頁/重複呼叫 initIfNeeded() 不重打 API
    private val refreshGate = ThrottledRefresher(minIntervalMs = 5_000L)

    // =========================
    // ★ FIX：history7 排序穩定化（新→舊）
    // =========================
    private fun parseDateOrMin(s: String): LocalDate =
        runCatching { LocalDate.parse(s) }.getOrElse { LocalDate.MIN }

    private fun sortHistoryDescStable(list: List<WeightItemDto>): List<WeightItemDto> {
        return list
            .sortedWith(compareByDescending { parseDateOrMin(it.logDate) })
            .toList()
    }

    init {
        // Unit：DataStore 是唯一真相
        viewModelScope.launch {
            store.weightUnitFlow
                .map { it ?: UserProfileStore.WeightUnit.LBS }   // null -> 預設
                .distinctUntilChanged()
                .collect { u ->
                    Log.d("WeightVM", "weightUnitFlow emit = $u")
                    _ui.update { it.copy(unit = u) }
                }
        }

        viewModelScope.launch {
            store.weightKgFlow.distinctUntilChanged()
                .collect { w -> _ui.update { it.copy(profileWeightKg = w?.toDouble()) } }
        }
        viewModelScope.launch {
            store.weightLbsFlow.distinctUntilChanged()
                .collect { w -> _ui.update { it.copy(profileWeightLbs = w?.toDouble()) } }
        }
        viewModelScope.launch {
            store.goalWeightKgFlow.distinctUntilChanged()
                .collect { t -> _ui.update { it.copy(profileGoalWeightKg = t?.toDouble()) } }
        }
        viewModelScope.launch {
            store.goalWeightLbsFlow.distinctUntilChanged()
                .collect { t -> _ui.update { it.copy(profileGoalWeightLbs = t?.toDouble()) } }
        }

        // ✅ 任何 repo 寫入成功（profile/weight）都會 emit invalidate -> force refresh（不受節流影響）
        viewModelScope.launch {
            merge(bus.weight, bus.profile).collect {
                refreshGate.invalidate()
                refreshThrottled(force = true)
            }
        }
    }

    /**
     * 入口：畫面進來常呼叫這個
     * ✅ 節流版：10 秒內重進頁不重打
     */
    fun initIfNeeded() {
        refreshThrottled(force = false)
    }

    /**
     * ✅ 節流 + baseline + refresh
     * - force=false：受節流保護
     * - force=true：一定刷新（例如寫入成功後）
     */
    fun refreshThrottled(force: Boolean) {
        refreshGate.launch(viewModelScope, force = force) {
            // baseline 只做一次
            if (!baselineEnsured) {
                baselineEnsured = true
                runCatching { repo.ensureBaseline() }
                    .onFailure { e -> if (e is CancellationException) throw e }
            }
            // ✅ 注意：refresh() 本身是 launch 版（會再開 coroutine）
            // 這裡維持你原本寫法，最少改動。
            refresh()
        }
    }

    /** 使用者切單位：只寫入 DataStore；UI 會因 flow emit 自動更新 */
    fun setUnit(u: UserProfileStore.WeightUnit) = viewModelScope.launch {
        Log.d("WeightVM", "setUnit() called u=$u")
        runCatching { store.setWeightUnit(u) }
            .onFailure { e ->
                Log.e("WeightVM", "setWeightUnit failed", e)
                _ui.update { it.copy(error = "Change unit failed") }
            }
    }

    /** 只有在「存檔成功」時才呼叫：commit 單位到 DataStore + 更新 ui.unit */
    private suspend fun commitUnitAfterSuccess(u: UserProfileStore.WeightUnit) {
        _ui.update { it.copy(unit = u, pendingUnit = null) }
        runCatching { store.setWeightUnit(u) }
            .onFailure { e ->
                Log.w("WeightVM", "commitUnitAfterSuccess failed: ${e.message}", e)
            }
    }

    fun setRange(r: String) {
        _ui.update { it.copy(range = r) }
        // ✅ 使用者主動切 range：直接強制刷新（不節流）
        refreshThrottled(force = true)
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
     * 更穩版本：只有「存檔成功」後，才把本次使用的單位寫回 DataStore
     * ✅ 注意：成功提示 toast 不在 VM 處理（避免跨頁殘留）
     */
    fun save(
        weightKg: Double,
        weightLbs: Double,
        date: LocalDate?,
        photo: File?,
        unitUsedToPersist: UserProfileStore.WeightUnit? = null,
        onResult: (Result<Unit>) -> Unit
    ) = viewModelScope.launch {
        _ui.update { it.copy(saving = true, error = null) }
        runCatching {
            repo.log(
                weightKg = weightKg,
                weightLbs = weightLbs,
                logDate = date?.toString(),
                photoFile = photo
            )
        }.onSuccess {
            if (unitUsedToPersist != null) {
                Log.d("WeightVM", "save success -> persist unit = $unitUsedToPersist")
                runCatching { store.setWeightUnit(unitUsedToPersist) }
            }

            // ✅ 寫入成功：強制刷新（不節流）
            refreshThrottled(force = true)

            _ui.update { it.copy(saving = false, error = null) }
            onResult(Result.success(Unit))
        }.onFailure { e ->
            if (e is CancellationException) throw e
            _ui.update { it.copy(error = "Save failed", saving = false) }
            onResult(Result.failure(e))
        }
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
     * ✅ 注意：成功提示 toast 不在 VM 處理（避免跨頁殘留）
     */
    fun updateGoalWeight(
        value: Double,
        unit: UserProfileStore.WeightUnit,
        onResult: (Result<Unit>) -> Unit
    ) {
        viewModelScope.launch {
            val res = profileRepo.updateGoalWeight(value, unit)
            res.onSuccess {
                commitUnitAfterSuccess(unit)

                // ✅ profile 寫入成功：強制刷新（不節流）
                refreshThrottled(force = true)

                _ui.update { it.copy(error = null) }
                onResult(Result.success(Unit))
            }.onFailure { e ->
                _ui.update { it.copy(error = "Save failed") }
                onResult(Result.failure(e))
            }
        }
    }
}
