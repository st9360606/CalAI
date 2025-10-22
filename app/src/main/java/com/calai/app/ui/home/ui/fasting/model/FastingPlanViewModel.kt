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

data class FastingUiState(
    val loading: Boolean = true,
    val selected: FastingPlan = FastingPlan.P16_8,
    val start: LocalTime = LocalTime.of(9, 0),
    val end: LocalTime = LocalTime.of(17, 0),
    val enabled: Boolean = false
)

@HiltViewModel
class FastingPlanViewModel @Inject constructor(
    private val repo: FastingRepository,
    private val app: Application,
    private val scheduler: FastingAlarmScheduler
) : ViewModel() {

    private val _state = MutableStateFlow(FastingUiState())
    val state = _state.asStateFlow()

    fun load() = viewModelScope.launch {
        val dto = repo.loadOrCreateDefault()
        applyDto(dto)
    }

    private fun applyDto(dto: FastingPlanDto) {
        val plan = FastingPlan.of(dto.planCode)
        val start = LocalTime.parse(dto.startTime)
        val end = LocalTime.parse(dto.endTime)
        _state.value = FastingUiState(false, plan, start, end, dto.enabled)
    }

    fun onPlanSelected(plan: FastingPlan) {
        val start = _state.value.start
        val end = start.plusHours(plan.eatingHours.toLong())
        _state.value = _state.value.copy(selected = plan, end = end)
    }

    fun onChangeStart(start: LocalTime) {
        val plan = _state.value.selected
        _state.value = _state.value.copy(start = start, end = start.plusHours(plan.eatingHours.toLong()))
    }

    fun onToggleEnabled(requested: Boolean, onNeedPermission: () -> Unit, onDenied: () -> Unit) {
        if (requested && !NotificationPermission.isGranted(app)) {
            onNeedPermission(); return
        }
        _state.value = _state.value.copy(enabled = requested)
        persistAndReschedule()
        if (requested && !NotificationPermission.isGranted(app)) onDenied()
    }

    fun persistAndReschedule() = viewModelScope.launch {
        val s = _state.value
        // ★ 要傳 planCode（字串）
        val saved = repo.save(s.selected.code, s.start, s.enabled)
        applyDto(saved) // server 回填 end

        // ★ 取消舊排程
        scheduler.cancel()

        if (_state.value.enabled) {
            val tr = repo.nextTriggers(_state.value.selected, _state.value.start)
            scheduler.schedule(
                java.time.Instant.parse(tr.nextStartUtc),
                java.time.Instant.parse(tr.nextEndUtc)
            )
        }
    }
}
