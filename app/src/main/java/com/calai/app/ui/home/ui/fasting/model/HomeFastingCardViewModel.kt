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
import javax.inject.Inject

data class FastingCardUi(
    val loading: Boolean = true,
    val planCode: String = "16:8",
    val startText: String = "09:00",
    val endText: String = "17:00",
    val enabled: Boolean = false
)

@HiltViewModel
class HomeFastingCardViewModel @Inject constructor(
    private val repo: FastingRepository,
    private val scheduler: FastingAlarmScheduler,
    private val app: Application
) : ViewModel() {

    private val _ui = MutableStateFlow(FastingCardUi())
    val ui = _ui.asStateFlow()

    fun load() = viewModelScope.launch {
        val dto = repo.loadOrCreateDefault() // 404 則建立預設
        applyDto(dto)                        // 後端結構見：FastingPlanDto。
        // planCode / startTime / endTime / enabled / timeZone 皆由後端回填。:contentReference[oaicite:2]{index=2}
    }

    fun onToggleRequested(
        requested: Boolean,
        onNeedPermission: () -> Unit,
        onDenied: () -> Unit
    ) {
        // 權限保護：未授權先拉權限窗，再決定是否變更
        if (requested && !NotificationPermission.isGranted(app)) {
            onNeedPermission()
            return
        }
        setEnabledAndPersist(requested, onDenied)
    }

    fun onPermissionResult(granted: Boolean) {
        if (granted) setEnabledAndPersist(true) else setEnabledAndPersist(false)
    }

    private fun setEnabledAndPersist(request: Boolean, onDenied: (() -> Unit)? = null) {
        viewModelScope.launch {
            val current = _ui.value
            val saved = repo.save(
                planCode = current.planCode,
                start = LocalTime.parse(current.startText),
                enabled = request
            )
            applyDto(saved)
            rescheduleIfNeeded()
            if (request && !NotificationPermission.isGranted(app)) onDenied?.invoke()
        }
    }

    private suspend fun rescheduleIfNeeded() {
        scheduler.cancel()
        val s = _ui.value
        if (!s.enabled) return
        val plan = FastingPlan.of(s.planCode)
        val startLocal = LocalTime.parse(s.startText)
        val tr = repo.nextTriggers(plan, startLocal) // 後端用 UTC 計算下一次開始/結束。:contentReference[oaicite:3]{index=3}
        scheduler.schedule(
            java.time.Instant.parse(tr.nextStartUtc),
            java.time.Instant.parse(tr.nextEndUtc)
        )
    }

    private fun applyDto(dto: FastingPlanDto) {
        _ui.value = FastingCardUi(
            loading = false,
            planCode = dto.planCode,
            startText = dto.startTime,
            endText = dto.endTime,
            enabled = dto.enabled
        )
    }
}
