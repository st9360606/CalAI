package com.calai.app.ui.home.ui.fasting.model

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.calai.app.data.fasting.api.FastingPlanDto
import com.calai.app.data.fasting.model.FastingPlan
import com.calai.app.data.fasting.notifications.FastingAlarmScheduler
import com.calai.app.data.fasting.notifications.NotificationPermission
import com.calai.app.data.fasting.repo.FastingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalTime
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

    fun load() = viewModelScope.launch {
        val dto = repo.loadOrCreateDefault()
        applyDto(dto)
        // ⛳ 進入頁面就做「權限與雲端狀態」一致性校正
        reconcileEnabledWithPermission()
        // 若裝置有權限且 DB=enabled，重裝後鬧鐘會被清空 → 這裡自動補排程
        maybeRescheduleIfEnabled()
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

    fun persistAndReschedule(showToast: Boolean = false) = viewModelScope.launch {
        try {
            val s = _state.value
            // upsert（Server 回填 end/enabled）
            val saved = repo.save(s.selected.code, s.start, s.enabled)
            applyDto(saved)

            // 重新排程（防禦性包 try-catch）
            try {
                scheduler.cancel()
                if (_state.value.enabled) {
                    val tr = repo.nextTriggers(_state.value.selected, _state.value.start)
                    scheduler.schedule(
                        java.time.Instant.parse(tr.nextStartUtc),
                        java.time.Instant.parse(tr.nextEndUtc)
                    )
                }
            } catch (_: Throwable) {
                // 忽略裝置層異常（無權限/廠商限制），不讓 UI 崩潰
            }

            if (showToast) {
                _state.value = _state.value.copy(
                    toastMessage = "Saved successfully !",
                )
            }
        } catch (_: Throwable) {
            if (showToast) {
                _state.value = _state.value.copy(
                    toastMessage = "Save failed"
                )
            }
            // 網路/Server 失敗也不讓 UI 崩潰
        }
    }

    // ====== 私有輔助 ======

    private fun isNotifGranted(): Boolean = NotificationPermission.isGranted(app)

    /**
     * 當 DB=enabled 但裝置無通知權限 → 自動關閉並回寫 DB=0，且取消排程。
     * 當 DB=disabled 或裝置已有權限 → 不動作。
     */
    private fun reconcileEnabledWithPermission() = viewModelScope.launch {
        val s = _state.value
        if (s.enabled && !isNotifGranted()) {
            // 本地關閉
            _state.value = s.copy(enabled = false)
            // 回寫 DB=0
            try {
                val saved = repo.save(s.selected.code, s.start, false)
                applyDto(saved.copy(enabled = false))
            } catch (_: Throwable) { /* 忽略寫回錯誤 */ }
            // 取消鬧鐘
            try { scheduler.cancel() } catch (_: Throwable) { }
        }
    }

    // 若開著（DB=1）且裝置有權限，進入時補排程（重裝後鬧鐘會被清掉）
    private fun maybeRescheduleIfEnabled() = viewModelScope.launch {
        val s = _state.value
        if (s.enabled && isNotifGranted()) {
            try {
                scheduler.cancel()
                val tr = repo.nextTriggers(s.selected, s.start)
                scheduler.schedule(
                    java.time.Instant.parse(tr.nextStartUtc),
                    java.time.Instant.parse(tr.nextEndUtc)
                )
            } catch (_: Throwable) { }
        }
    }

    fun clearToast() {
        _state.value = _state.value.copy(toastMessage = null)
    }
}
