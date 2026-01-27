package com.calai.bitecal.ui.home.ui.fasting.model

import android.app.Application
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.calai.bitecal.data.fasting.api.FastingPlanDto
import com.calai.bitecal.data.fasting.model.FastingPlan
import com.calai.bitecal.data.fasting.notifications.FastingAlarmScheduler
import com.calai.bitecal.data.fasting.notifications.NotificationPermission
import com.calai.bitecal.data.fasting.repo.FastingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.Instant
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.time.temporal.ChronoUnit
import javax.inject.Inject

data class FastingUiState(
    val loading: Boolean = true,
    val selected: FastingPlan = FastingPlan.P16_8,
    val start: LocalTime = LocalTime.of(9, 0),
    val end: LocalTime = LocalTime.of(17, 0),
    val enabled: Boolean = false,
    val toastMessage: String? = null
)
private const val TAG = "FastingVM"
@HiltViewModel
class FastingPlanViewModel @Inject constructor(
    private val repo: FastingRepository,
    private val app: Application,
    private val scheduler: FastingAlarmScheduler
) : ViewModel() {

    private val _state = MutableStateFlow(FastingUiState())
    val state = _state.asStateFlow()

    // 當使用者嘗試開啟但尚未授權通知時，暫存「待啟用」意圖
    private var pendingEnable = false

    private val hm: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault())

    fun load() = viewModelScope.launch {
        try {
            val dto = repo.ensureDefaultIfMissing()
            applyDto(dto)

            reconcileEnabledWithPermission()
            maybeRescheduleIfEnabled()
        } catch (t: Throwable) {
            Log.e(TAG, "load() failed", t)
            _state.value = _state.value.copy(loading = false)
        }
    }

    private fun applyDto(dto: FastingPlanDto) {
        val old = _state.value
        val plan = FastingPlan.of(dto.planCode)
        val start = LocalTime.parse(dto.startTime)
        val end = LocalTime.parse(dto.endTime)

        _state.value = old.copy(
            loading = false,
            selected = plan,
            start = start,
            end = end,
            enabled = dto.enabled
        )
    }

    fun onPlanSelected(plan: FastingPlan) {
        val start = _state.value.start
        val end = start.plusHours(plan.eatingHours.toLong())
        _state.value = _state.value.copy(selected = plan, end = end)
    }

    fun onChangeStart(start: LocalTime) {
        val plan = _state.value.selected
        _state.value = _state.value.copy(
            start = start,
            end = start.plusHours(plan.eatingHours.toLong())
        )
    }

    // 允許使用者直接改 end time（會回推 start）
    fun onChangeEnd(end: LocalTime) {
        val plan = _state.value.selected
        val start = end.minus(plan.eatingHours.toLong(), ChronoUnit.HOURS)
        _state.value = _state.value.copy(
            start = start,
            end = end
        )
    }

    /**
     * 切換禁食提醒：
     * - requested=true 且未授權 → 設 pendingEnable=true，由 UI 觸發權限流程
     * - requested=true 且已授權 → 立即啟用並落庫+排程
     * - requested=false → 關閉並落庫+取消排程
     */
    fun onToggleEnabled(
        requested: Boolean,
        onNeedPermission: () -> Unit,
        onDenied: () -> Unit
    ) {
        if (requested) {
            if (!isNotifGranted()) {
                pendingEnable = true
                onNeedPermission()
                return
            }
            pendingEnable = false
            _state.value = _state.value.copy(enabled = true)
            persistAndReschedule()
        } else {
            pendingEnable = false
            _state.value = _state.value.copy(enabled = false)
            persistAndReschedule()
        }
    }

    // 從系統設定頁返回 App 時呼叫；處理待啟用與一致性校正
    fun onAppResumed() {
        // 若剛才允許了通知，且之前 pendingEnable=true → 自動開啟 & 落庫
        if (pendingEnable && isNotifGranted()) {
            pendingEnable = false
            _state.value = _state.value.copy(enabled = true)
            persistAndReschedule()
            return
        }
        // 若使用者在系統裡關閉了通知 → 自動關閉 & 落庫
        reconcileEnabledWithPermission()
    }

    /**
     * ✅ 儲存到後端後：
     * - enabled=false → cancel
     * - enabled=true  → 取 nextTriggers(UTC) 後排兩個通知：
     *   1) nextStartUtc
     *   2) nextEndUtc - 60min (endSoon)
     */
    fun persistAndReschedule(showToast: Boolean = false) = viewModelScope.launch {
        try {
            val s = _state.value
            Log.d(TAG, "persistAndReschedule() start enabled=${s.enabled} plan=${s.selected.code} start=${s.start.format(hm)}")

            val saved = repo.save(s.selected.code, s.start, s.enabled)
            applyDto(saved)

            try {
                scheduleOrCancelUsingSaved(saved)
            } catch (t: Throwable) {
                Log.e(TAG, "scheduleOrCancelUsingSaved() failed", t)
            }

            if (showToast) {
                _state.value = _state.value.copy(toastMessage = "Saved successfully !")
            }
        } catch (t: Throwable) {
            Log.e(TAG, "persistAndReschedule() failed", t)
            if (showToast) {
                _state.value = _state.value.copy(toastMessage = "Save failed")
            }
        }
    }

    // ====== 私有輔助 ======

    private fun isNotifGranted(): Boolean = NotificationPermission.isGranted(app)

    /**
     * 當 DB=enabled 但裝置無通知權限 → 自動關閉並回寫 DB=0，且取消排程。
     */
    private fun reconcileEnabledWithPermission() = viewModelScope.launch {
        val s = _state.value
        if (s.enabled && !isNotifGranted()) {
            Log.w(TAG, "reconcile: enabled=1 but permission not granted -> force disable")
            _state.value = s.copy(enabled = false)

            try {
                val saved = repo.save(s.selected.code, s.start, false)
                applyDto(saved.copy(enabled = false))
            } catch (t: Throwable) {
                Log.e(TAG, "reconcile write-back failed", t)
            }

            try { scheduler.cancel() } catch (t: Throwable) { Log.e(TAG, "cancel failed", t) }
        }
    }

    /**
     * 若開著（DB=1）且裝置有權限，進入時補排程（重裝後鬧鐘會被清掉）
     *
     * ✅ 注意：不要先 cancel 再拿 triggers，避免網路失敗把排程清掉。
     * 這裡先算好 triggers 再 schedule。
     */
    private fun maybeRescheduleIfEnabled() = viewModelScope.launch {
        val s = _state.value
        if (s.enabled && isNotifGranted()) {
            try {
                val tr = repo.nextTriggers(s.selected, s.start)
                val nextStart = Instant.parse(tr.nextStartUtc)
                val nextEnd = Instant.parse(tr.nextEndUtc)
                val endSoon = nextEnd.minus(Duration.ofHours(1))

                Log.d(TAG, "maybeReschedule nextStart=$nextStart nextEnd=$nextEnd endSoon=$endSoon")

                scheduler.schedule(
                    startUtc = nextStart,
                    endSoonUtc = endSoon,
                    planCode = s.selected.code,
                    startTime = s.start.format(hm),
                    endTime = s.end.format(hm)
                )
            } catch (t: Throwable) {
                Log.e(TAG, "maybeRescheduleIfEnabled failed", t)
            }
        } else {
            Log.d(TAG, "maybeReschedule skip enabled=${s.enabled} granted=${isNotifGranted()}")
        }
    }

    /**
     * 實際排程/取消邏輯（以後端回填的 dto 為準）
     */
    private suspend fun scheduleOrCancelUsingSaved(saved: FastingPlanDto) {
        if (!isNotifGranted()) {
            Log.w(TAG, "scheduleOrCancel: permission not granted -> cancel")
            scheduler.cancel()
            return
        }
        if (!saved.enabled) {
            Log.d(TAG, "scheduleOrCancel: enabled=false -> cancel")
            scheduler.cancel()
            return
        }

        val plan = FastingPlan.of(saved.planCode)
        val startLocal = LocalTime.parse(saved.startTime)

        val tr = repo.nextTriggers(plan, startLocal)
        val nextStart = Instant.parse(tr.nextStartUtc)
        val nextEnd = Instant.parse(tr.nextEndUtc)
        val endSoon = nextEnd.minus(Duration.ofHours(1))

        Log.d(TAG, "scheduleOrCancel: nextStart=$nextStart nextEnd=$nextEnd endSoon=$endSoon")

        scheduler.schedule(
            startUtc = nextStart,
            endSoonUtc = endSoon,
            planCode = saved.planCode,
            startTime = saved.startTime,
            endTime = saved.endTime
        )
    }

    fun clearToast() {
        _state.value = _state.value.copy(toastMessage = null)
    }
}
