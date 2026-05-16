package com.calai.bitecal.ui.home.ui.settings.editname.model

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.calai.bitecal.data.users.repo.UsersRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val NAME_MAX_LENGTH = 40

@HiltViewModel
class EditNameViewModel @Inject constructor(
    private val usersRepo: UsersRepository
) : ViewModel() {

    data class UiState(
        val initialName: String = "",
        val input: String = "",
        val isSaving: Boolean = false,
        val error: String? = null
    ) {
        fun trimmed(): String = input.trim()
        fun canSave(): Boolean {
            val value = trimmed()
            return value.isNotEmpty() && value.length <= NAME_MAX_LENGTH && value != initialName.trim() && !isSaving
        }
    }

    sealed interface Event {
        data class Saved(val newName: String) : Event
        data class Error(val message: String) : Event
    }

    private val _ui = MutableStateFlow(UiState())
    val ui = _ui.asStateFlow()

    private val _events = MutableSharedFlow<Event>(extraBufferCapacity = 1)
    val events = _events.asSharedFlow()

    fun load(initialFromPersonal: String?) = viewModelScope.launch {
        // 先用 Personal 傳進來的值，畫面立刻有字
        val snap = initialFromPersonal?.trim().orEmpty()
        _ui.update { it.copy(initialName = snap, input = snap, error = null) }

        // 再跟 server 對一次（避免 personal 畫面是舊的）；失敗就保留上一頁快照，避免網路失敗讓畫面卡住。
        val serverName = runCatching { usersRepo.meOrNull()?.name?.trim().orEmpty() }
            .getOrDefault(snap)
        if (serverName != snap) {
            _ui.update { it.copy(initialName = serverName, input = serverName) }
        }
    }

    fun onInputChange(v: String) {
        val next = v.take(NAME_MAX_LENGTH)
        _ui.update {
            it.copy(
                input = next,
                error = if (v.length > NAME_MAX_LENGTH) {
                    "Name can be up to $NAME_MAX_LENGTH characters."
                } else {
                    null
                }
            )
        }
    }

    fun save() = viewModelScope.launch {
        val cur = _ui.value
        if (cur.isSaving) return@launch
        if (!cur.canSave()) return@launch

        _ui.update { it.copy(isSaving = true, error = null) }
        try {
            val me = usersRepo.updateName(cur.trimmed())
            val newName = me.name?.trim().orEmpty()
            _ui.update { it.copy(isSaving = false, initialName = newName, input = newName) }
            _events.tryEmit(Event.Saved(newName))
        } catch (t: Throwable) {
            val msg = t.message ?: t.javaClass.simpleName
            _ui.update { it.copy(isSaving = false, error = msg) }
            _events.tryEmit(Event.Error(msg))
        }
    }
}
