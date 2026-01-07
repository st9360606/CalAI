package com.calai.app.ui.home.ui.settings.details.model

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.calai.app.data.profile.api.UserProfileDto
import com.calai.app.data.profile.repo.ProfileRepository
import com.calai.app.data.profile.repo.UserProfileStore
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
class EditWaterGoalViewModel @Inject constructor(
    private val repo: ProfileRepository,
    private val store: UserProfileStore
) : ViewModel() {

    private companion object {
        const val DEFAULT_WATER_GOAL_ML = 2000
        const val MAX_WATER_GOAL_ML = 20000
    }

    data class UiState(
        val previousGoalMl: Int = DEFAULT_WATER_GOAL_ML,
        val input: String = DEFAULT_WATER_GOAL_ML.toString(),
        val isSaving: Boolean = false,
        val error: String? = null
    ) {
        fun parsedOrNull(): Int? = input.toIntOrNull()

        fun canSave(): Boolean {
            val v = parsedOrNull() ?: return false
            if (v !in 0..MAX_WATER_GOAL_ML) return false
            return v != previousGoalMl && !isSaving
        }
    }

    sealed interface UiEvent {
        data object Saved : UiEvent
    }

    private val _ui = MutableStateFlow(UiState())
    val ui: StateFlow<UiState> = _ui.asStateFlow()

    private val _events = MutableSharedFlow<UiEvent>()
    val events = _events.asSharedFlow()

    init {
        viewModelScope.launch {
            // ① 先用本地值讓 UI 秒開（快）
            val local = store.waterGoalUiFlow.first()
            _ui.update { it.copy(previousGoalMl = local, input = local.toString()) }

            // ② 再打後端 DB 覆蓋（準）
            refreshFromRemote()
        }
    }

    private suspend fun refreshFromRemote() {
        val result = repo.getWaterGoalFromServer()
        result.fold(
            onSuccess = { remoteOrNull ->
                // server 沒設定就不要蓋掉目前 UI（避免把 default 當真值）
                val remote = remoteOrNull ?: return

                // ✅ 回寫 store，確保下次進來也對
                store.applyRemoteWaterGoal(remote)

                // ✅ 更新畫面
                _ui.update { it.copy(previousGoalMl = remote, input = remote.toString(), error = null) }
            },
            onFailure = {
                // 網路失敗：保留 local，不要把 UI 弄壞
            }
        )
    }

    fun onInputChange(raw: String) {
        val digitsOnly = raw.filter { it.isDigit() }.take(5) // 20000 最多 5 位
        _ui.update { it.copy(input = digitsOnly, error = null) }
    }

    fun revert() {
        _ui.update { it.copy(input = it.previousGoalMl.toString(), error = null) }
    }

    fun save() {
        val v = _ui.value.parsedOrNull() ?: run {
            _ui.update { it.copy(error = "Invalid number") }
            return
        }
        if (v !in 0..MAX_WATER_GOAL_ML) {
            _ui.update { it.copy(error = "Out of range") }
            return
        }
        if (v == _ui.value.previousGoalMl) return

        viewModelScope.launch {
            _ui.update { it.copy(isSaving = true, error = null) }

            val result = repo.updateDailyWaterGoalOnly(v)
            result.fold(
                onSuccess = { resp: UserProfileDto ->
                    val saved = resp.waterMl ?: v
                    _ui.update { it.copy(previousGoalMl = saved, input = saved.toString(), isSaving = false) }
                    _events.emit(UiEvent.Saved)
                },
                onFailure = { e ->
                    _ui.update { it.copy(isSaving = false, error = e.message ?: "Save failed") }
                }
            )
        }
    }
}
