package com.calai.bitecal.ui.home.ui.settings.referral.model

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.calai.bitecal.data.referral.api.ReferralSummaryDto
import com.calai.bitecal.data.referral.repo.ReferralRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ReferralViewModel @Inject constructor(
    private val referralRepository: ReferralRepository
) : ViewModel() {

    data class UiState(
        val loading: Boolean = true,
        val summary: ReferralSummaryDto? = null,
        val claimInFlight: Boolean = false,
        val error: String? = null
    )

    private val _ui = MutableStateFlow(UiState())
    val ui: StateFlow<UiState> = _ui

    init {
        refresh()
    }

    fun refresh() = viewModelScope.launch {
        _ui.update { it.copy(loading = true, error = null) }
        runCatching { referralRepository.getSummary() }
            .onSuccess { summary -> _ui.update { it.copy(loading = false, summary = summary, error = null) } }
            .onFailure { t -> _ui.update { it.copy(loading = false, error = t.message ?: t.javaClass.simpleName) } }
    }

    fun claim(promoCode: String, onSuccess: () -> Unit) = viewModelScope.launch {
        _ui.update { it.copy(claimInFlight = true, error = null) }
        runCatching { referralRepository.claim(promoCode) }
            .onSuccess {
                _ui.update { it.copy(claimInFlight = false, error = null) }
                refresh()
                onSuccess()
            }
            .onFailure { t -> _ui.update { it.copy(claimInFlight = false, error = t.message ?: t.javaClass.simpleName) } }
    }
}
