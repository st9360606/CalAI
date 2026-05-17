package com.calai.bitecal.ui.home.ui.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.calai.bitecal.data.notifications.api.NotificationItemDto
import com.calai.bitecal.data.notifications.repo.NotificationInboxRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class NotificationInboxUiState(
    val loading: Boolean = false,
    val error: String? = null,
    val items: List<NotificationItemDto> = emptyList(),
    val markingReadIds: Set<Long> = emptySet()
)

@HiltViewModel
class NotificationInboxViewModel @Inject constructor(
    private val repository: NotificationInboxRepository
) : ViewModel() {

    private val _ui = MutableStateFlow(NotificationInboxUiState())
    val ui: StateFlow<NotificationInboxUiState> = _ui.asStateFlow()

    private var refreshJob: Job? = null

    init {
        refresh()
    }

    fun refresh() {
        if (refreshJob?.isActive == true) return

        refreshJob = viewModelScope.launch {
            _ui.update { state ->
                state.copy(
                    loading = true,
                    error = null
                )
            }

            runCatching {
                repository.list()
            }.onSuccess { items ->
                _ui.update { state ->
                    state.copy(
                        loading = false,
                        error = null,
                        items = items
                    )
                }
            }.onFailure { throwable ->
                _ui.update { state ->
                    state.copy(
                        loading = false,
                        error = throwable.message ?: "Unable to load notifications."
                    )
                }
            }
        }
    }

    fun markRead(notificationId: Long) {
        val currentState = _ui.value
        val target = currentState.items.firstOrNull { item -> item.id == notificationId } ?: return

        if (target.read) return
        if (notificationId in currentState.markingReadIds) return

        viewModelScope.launch {
            _ui.update { state ->
                state.copy(markingReadIds = state.markingReadIds + notificationId)
            }

            runCatching {
                repository.markRead(notificationId)
            }.onSuccess { response ->
                if (response.read) {
                    _ui.update { state ->
                        state.copy(
                            items = state.items.map { item ->
                                if (item.id == notificationId) {
                                    item.copy(read = true)
                                } else {
                                    item
                                }
                            }
                        )
                    }
                }
            }.onFailure {
                // 不做 optimistic update。
                // mark read 失敗時保留 unread 狀態，避免 UI 和後端狀態不一致。
            }

            _ui.update { state ->
                state.copy(markingReadIds = state.markingReadIds - notificationId)
            }
        }
    }
}
