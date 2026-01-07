package com.calai.app.ui.home.ui.settings.editname.model

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.calai.app.data.users.repo.UsersRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

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
        fun canSave(): Boolean = trimmed().isNotEmpty() && trimmed() != initialName.trim()
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

        // 再跟 server 對一次（避免 personal 畫面是舊的）
        val me = usersRepo.meOrNull()
        val serverName = me?.name?.trim().orEmpty()
        if (serverName != snap) {
            _ui.update { it.copy(initialName = serverName, input = serverName) }
        }
    }

    fun onInputChange(v: String) {
        _ui.update { it.copy(input = v, error = null) }
    }

    fun save() = viewModelScope.launch {
        val cur = _ui.value
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
