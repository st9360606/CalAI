package com.calai.bitecal.ui.home.ui.settings.details.model

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.calai.bitecal.data.profile.api.UserProfileDto
import com.calai.bitecal.data.profile.repo.ProfileRepository
import com.calai.bitecal.data.profile.repo.UserProfileStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EditWorkoutGoalViewModel @Inject constructor(
    private val repo: ProfileRepository,
    private val store: UserProfileStore
) : ViewModel() {

    private companion object {
        const val DEFAULT_DAILY_WORKOUT_GOAL_KCAL = 450
        const val MAX_DAILY_WORKOUT_GOAL_KCAL = 5000
    }

    data class UiState(
        val previousGoalKcal: Int = DEFAULT_DAILY_WORKOUT_GOAL_KCAL,
        val input: String = DEFAULT_DAILY_WORKOUT_GOAL_KCAL.toString(),
        val isSaving: Boolean = false,
        val error: String? = null
    ) {
        fun parsedOrNull(): Int? = input.toIntOrNull()

        fun canSave(): Boolean {
            val v = parsedOrNull() ?: return false
            if (v !in 0..MAX_DAILY_WORKOUT_GOAL_KCAL) return false
            return v != previousGoalKcal && !isSaving
        }
    }

    sealed interface UiEvent { data object Saved : UiEvent }

    private val _ui = MutableStateFlow(UiState())
    val ui: StateFlow<UiState> = _ui.asStateFlow()

    private val _events = MutableSharedFlow<UiEvent>()
    val events = _events.asSharedFlow()

    init {
        viewModelScope.launch {
            // ① 先用本地值秒開
            val local = store.dailyWorkoutGoalUiFlow.first()
            _ui.update { it.copy(previousGoalKcal = local, input = local.toString()) }

            // ② 再用 server 覆蓋（server null 不覆蓋，避免把 default 當真值）
            refreshFromRemote()
        }
    }

    private suspend fun refreshFromRemote() {
        val result = repo.getDailyWorkoutGoalFromServer()
        result.fold(
            onSuccess = { remoteOrNull ->
                val remote = remoteOrNull ?: return
                store.applyRemoteDailyWorkoutGoal(remote)
                _ui.update { it.copy(previousGoalKcal = remote, input = remote.toString(), error = null) }
            },
            onFailure = { /* keep local */ }
        )
    }

    fun onInputChange(raw: String) {
        val digitsOnly = raw.filter { it.isDigit() }.take(4) // 5000 最多 4 位
        _ui.update { it.copy(input = digitsOnly, error = null) }
    }

    fun revert() {
        _ui.update { it.copy(input = it.previousGoalKcal.toString(), error = null) }
    }

    fun save() {
        val v = _ui.value.parsedOrNull() ?: run {
            _ui.update { it.copy(error = "Invalid number") }
            return
        }
        if (v !in 0..MAX_DAILY_WORKOUT_GOAL_KCAL) {
            _ui.update { it.copy(error = "Out of range") }
            return
        }
        if (v == _ui.value.previousGoalKcal) return

        viewModelScope.launch {
            _ui.update { it.copy(isSaving = true, error = null) }

            val result = repo.updateDailyWorkoutGoalOnly(v)
            result.fold(
                onSuccess = { resp: UserProfileDto ->
                    val saved = resp.dailyWorkoutGoalKcal ?: v
                    _ui.update { it.copy(previousGoalKcal = saved, input = saved.toString(), isSaving = false) }
                    _events.emit(UiEvent.Saved)
                },
                onFailure = { e ->
                    _ui.update { it.copy(isSaving = false, error = e.message ?: "Save failed") }
                }
            )
        }
    }
}
