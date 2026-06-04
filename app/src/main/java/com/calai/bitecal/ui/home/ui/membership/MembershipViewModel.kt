package com.calai.bitecal.ui.home.ui.membership

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.calai.bitecal.data.entitlement.model.PremiumStatus
import com.calai.bitecal.data.membership.repo.MembershipRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MembershipUiState(
    val loading: Boolean = true,
    val premiumStatus: PremiumStatus = PremiumStatus.FREE,
    val currentPremiumUntil: String? = null,
    val trialEndsAt: String? = null,
    val trialDaysLeft: Int? = null,
    val paymentIssue: Boolean = false,
    val error: String? = null
) {
    val canUseScan: Boolean
        get() = premiumStatus == PremiumStatus.TRIAL ||
                premiumStatus == PremiumStatus.PREMIUM
}

@HiltViewModel
class MembershipViewModel @Inject constructor(
    private val membershipRepository: MembershipRepository
) : ViewModel() {

    private val _ui = MutableStateFlow(MembershipUiState())
    val ui = _ui.asStateFlow()

    private var refreshSequence: Long = 0L

    init {
        refresh()
    }

    fun refresh() {
        val requestSequence = ++refreshSequence

        viewModelScope.launch {
            _ui.value = _ui.value.copy(loading = true, error = null)

            val result = runCatching {
                membershipRepository.getSummary()
            }

            if (requestSequence != refreshSequence) {
                return@launch
            }

            result.onSuccess { dto ->
                _ui.value = MembershipUiState(
                    loading = false,
                    premiumStatus = PremiumStatus.from(dto.premiumStatus),
                    currentPremiumUntil = dto.currentPremiumUntil,
                    trialEndsAt = dto.trialEndsAt,
                    trialDaysLeft = dto.trialDaysLeft,
                    paymentIssue = dto.paymentIssue,
                    error = null
                )
            }.onFailure { e ->
                _ui.value = _ui.value.copy(
                    loading = false,
                    error = e.message ?: "Failed to load membership"
                )
            }
        }
    }
}
