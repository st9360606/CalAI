package com.calai.bitecal.ui.home.ui.fasting.model

import android.app.Application
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
import java.time.LocalTime
import javax.inject.Inject
import java.time.Duration
import java.time.Instant
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
        val dto = repo.ensureDefaultIfMissing()
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
        val s = _ui.value
        // ✅ 關閉就取消
        if (!s.enabled) {
            scheduler.cancel()
            return
        }

        val plan = FastingPlan.of(s.planCode)
        val startLocal = LocalTime.parse(s.startText)
        val tr = repo.nextTriggers(plan, startLocal) // 後端回傳 nextStartUtc / nextEndUtc
        val nextStart = Instant.parse(tr.nextStartUtc)
        val nextEnd = Instant.parse(tr.nextEndUtc)

        // ✅ endSoon = end - 60min（DST 安全）
        val endSoon = nextEnd.minus(Duration.ofHours(1))

        // ✅ 直接覆蓋排程（不用先 cancel；同 PendingIntent 會覆蓋）
        scheduler.schedule(
            startUtc = nextStart,
            endSoonUtc = endSoon,
            planCode = s.planCode,
            startTime = s.startText,
            endTime = s.endText
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
