package com.calai.app.ui.home.ui.personal.details.common

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class PersonalDetailsToastViewModel @Inject constructor() : ViewModel() {

    data class UiState(
        val success: String? = null,
        val error: String? = null
    )

    private val _ui = MutableStateFlow(UiState())
    val ui: StateFlow<UiState> = _ui

    fun showSuccess(msg: String) {
        _ui.update { it.copy(success = msg, error = null) }
    }

    fun showError(msg: String) {
        _ui.update { it.copy(error = msg, success = null) }
    }

    fun clearSuccess() {
        _ui.update { it.copy(success = null) }
    }

    fun clearError() {
        _ui.update { it.copy(error = null) }
    }
}
