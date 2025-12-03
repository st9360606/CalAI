package com.calai.app.ui.home.ui.personal.details.model

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
class EditDailyStepGoalViewModel @Inject constructor(
    private val repo: ProfileRepository,
    private val store: UserProfileStore
) : ViewModel() {

    data class UiState(
        val previousGoal: Int = 10000,
        val input: String = "10000",
        val isSaving: Boolean = false,
        val error: String? = null
    ) {
        fun parsedOrNull(): Int? = input.toIntOrNull()

        fun canSave(): Boolean {
            val v = parsedOrNull() ?: return false
            if (v < 0 || v > 200000) return false
            return v != previousGoal && !isSaving
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
            val local = store.dailyStepGoalFlow.first()
            _ui.update { it.copy(previousGoal = local, input = local.toString()) }

            // ② 再打後端 DB 覆蓋（準）
            refreshFromRemote()
        }
    }

    private suspend fun refreshFromRemote() {
        val result = repo.getDailyStepGoalFromServer()

        result.fold(
            onSuccess = { remoteOrNull ->
                // server 沒設定就不要蓋掉目前 UI（避免把 default 當成真值）
                val remote = remoteOrNull ?: return

                // ✅ 回寫 store，確保下次進來也對
                store.applyRemoteDailyStepGoal(remote)

                // ✅ 更新畫面
                _ui.update { it.copy(previousGoal = remote, input = remote.toString(), error = null) }
            },
            onFailure = {
                // 網路失敗：保留 local，不要把 UI 弄壞
            }
        )
    }

    fun onInputChange(raw: String) {
        val digitsOnly = raw.filter { it.isDigit() }.take(6) // 200000 最多 6 位
        _ui.update { it.copy(input = digitsOnly, error = null) }
    }

    fun revert() {
        _ui.update { it.copy(input = it.previousGoal.toString(), error = null) }
    }

    fun save() {
        val v = _ui.value.parsedOrNull() ?: run {
            _ui.update { it.copy(error = "Invalid number") }
            return
        }
        if (v < 0 || v > 200000) {
            _ui.update { it.copy(error = "Out of range") }
            return
        }
        if (v == _ui.value.previousGoal) return

        viewModelScope.launch {
            _ui.update { it.copy(isSaving = true, error = null) }

            val result = repo.updateDailyStepGoalOnly(v)
            result.fold(
                onSuccess = { resp: UserProfileDto ->
                    val saved = resp.dailyStepGoal ?: v

                    // ✅ UI 用 server 回傳的值（避免 server clamp/修正造成不一致）
                    _ui.update { it.copy(previousGoal = saved, input = saved.toString(), isSaving = false) }

                    _events.emit(UiEvent.Saved)
                },
                onFailure = { e ->
                    _ui.update { it.copy(isSaving = false, error = e.message ?: "Save failed") }
                }
            )
        }
    }
}
