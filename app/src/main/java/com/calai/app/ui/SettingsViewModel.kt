package com.calai.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.calai.app.data.prefs.TokenManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val tokenManager: TokenManager
) : ViewModel() {

    // 觀察目前 token（null 表示未設定）
    val tokenState: StateFlow<String?> =
        tokenManager.tokenState.stateIn(viewModelScope, SharingStarted.Companion.Eagerly, null)

    fun saveToken(text: String) {
        viewModelScope.launch { tokenManager.setTokenAsync(text) }
    }

    fun clearToken() {
        viewModelScope.launch { tokenManager.clearTokenAsync() }
    }
}