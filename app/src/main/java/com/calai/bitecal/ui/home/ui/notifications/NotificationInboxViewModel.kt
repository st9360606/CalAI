package com.calai.bitecal.ui.home.ui.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.calai.bitecal.data.notifications.api.NotificationItemDto
import com.calai.bitecal.data.notifications.repo.NotificationInboxRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NotificationInboxViewModel @Inject constructor(
    private val repository: NotificationInboxRepository
) : ViewModel() {

    data class UiState(
        val loading: Boolean = true,
        val items: List<NotificationItemDto> = emptyList(),
        val error: String? = null
    )

    private val _ui = MutableStateFlow(UiState())
    val ui: StateFlow<UiState> = _ui

    init {
        refresh()
    }

    fun refresh() = viewModelScope.launch {
        _ui.update { it.copy(loading = true, error = null) }

        runCatching {
            repository.list()
        }.onSuccess { items ->
            _ui.update {
                it.copy(
                    loading = false,
                    items = items,
                    error = null
                )
            }
        }.onFailure { t ->
            _ui.update {
                it.copy(
                    loading = false,
                    error = t.message ?: t.javaClass.simpleName
                )
            }
        }
    }
}
