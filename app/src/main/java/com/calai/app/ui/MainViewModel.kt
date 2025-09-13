package com.calai.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.calai.app.data.MainRepository
import com.calai.app.core.net.NetworkResult
import com.calai.app.data.prefs.TokenManager
import com.calai.app.net.InfoDTO
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class UiState {
    data object Idle : UiState()
    data object Loading : UiState()
    data class Hello(val text: String) : UiState()
    data class Info(val data: InfoDTO) : UiState()
    data class Error(val msg: String) : UiState()
}

@HiltViewModel
class MainViewModel @Inject constructor(
    private val repo: MainRepository,
    private val tokenManager: TokenManager   // ← 新增
) : ViewModel() {


    private val _state = MutableStateFlow<UiState>(UiState.Idle)
    val state: StateFlow<UiState> = _state

    fun callHello() {
        _state.value = UiState.Loading
        viewModelScope.launch {
            when (val r = repo.hello()) {
                is NetworkResult.Success   -> _state.value = UiState.Hello(r.data)
                is NetworkResult.HttpError -> _state.value = UiState.Error("HTTP ${r.code}: ${r.body ?: ""}")
                is NetworkResult.NetworkError -> _state.value = UiState.Error(r.message)
                is NetworkResult.Unexpected -> _state.value = UiState.Error(r.message)
            }
        }
    }

    fun callInfo() {
        _state.value = UiState.Loading
        viewModelScope.launch {
            when (val r = repo.info()) {
                is NetworkResult.Success   -> _state.value = UiState.Info(r.data)
                is NetworkResult.HttpError -> _state.value = UiState.Error("HTTP ${r.code}: ${r.body ?: ""}")
                is NetworkResult.NetworkError -> _state.value = UiState.Error(r.message)
                is NetworkResult.Unexpected -> _state.value = UiState.Error(r.message)
            }
        }
    }

    fun setFakeToken() {
        tokenManager.setTokenAsync("FAKE-TOKEN-123")
    }

    fun clearToken() {
        tokenManager.clearTokenAsync()
    }
}
