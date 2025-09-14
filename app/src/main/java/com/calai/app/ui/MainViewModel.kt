package com.calai.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.calai.app.core.AppResult
import com.calai.app.data.MainRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface UiState {
    data object Idle : UiState
    data object Loading : UiState
    data class Hello(val text: String): UiState
    data class Info(val message: String, val serverTime: String): UiState
    data class Error(val msg: String, val retry: () -> Unit): UiState
}

@HiltViewModel
class MainViewModel @Inject constructor(
    private val repo: MainRepository
): ViewModel() {
    private val _state = MutableStateFlow<UiState>(UiState.Idle)
    val state: StateFlow<UiState> = _state

    fun callHello() = runWithLoading { doHello() }
    fun callInfo()  = runWithLoading { doInfo()  }

    private fun runWithLoading(block: suspend () -> Unit) {
        _state.value = UiState.Loading
        viewModelScope.launch { block() }
    }

    private suspend fun doHello() {
        when (val r = repo.hello()) {
            is AppResult.Success -> _state.value = UiState.Hello(r.data)
            is AppResult.Error   -> _state.value = UiState.Error(r.message, retry = ::callHello)
        }
    }
    private suspend fun doInfo() {
        when (val r = repo.info()) {
            is AppResult.Success -> _state.value = UiState.Info(r.data.message, r.data.serverTime)
            is AppResult.Error   -> _state.value = UiState.Error(r.message, retry = ::callInfo)
        }
    }
}
