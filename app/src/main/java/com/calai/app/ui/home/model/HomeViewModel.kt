package com.calai.app.ui.home.model


import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.calai.app.data.health.HealthConnectRepository
import com.calai.app.data.home.repo.HomeRepository
import com.calai.app.data.home.repo.HomeSummary

import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val loading: Boolean = true,
    val summary: HomeSummary? = null,
    val error: String? = null,
    val selectedDayOffset: Int = 0 // 0=今天，-1=昨天...
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repo: HomeRepository,
    private val hc: HealthConnectRepository
) : ViewModel() {

    private val _ui = MutableStateFlow(HomeUiState())
    val ui: StateFlow<HomeUiState> = _ui.asStateFlow()

    fun refresh() = viewModelScope.launch {
        _ui.value = _ui.value.copy(loading = true, error = null)
        runCatching { repo.loadSummary() }
            .onSuccess { _ui.value = HomeUiState(loading = false, summary = it) }
            .onFailure { _ui.value = HomeUiState(loading = false, error = it.message) }
    }

    fun onAddWater(ml: Int) = viewModelScope.launch {
        repo.addWater(ml)
        refresh()
    }

    fun onRequestHealthPermissions() = viewModelScope.launch {
        // 只提供給 UI 判斷是否要跳權限頁（你的 Onboarding 已處理）
        refresh()
    }

    init { refresh() }
}
