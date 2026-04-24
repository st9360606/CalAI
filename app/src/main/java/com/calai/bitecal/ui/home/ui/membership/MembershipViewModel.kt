package com.calai.bitecal.ui.home.ui.membership

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.calai.bitecal.data.entitlement.api.EntitlementApi
import com.calai.bitecal.data.entitlement.model.PremiumStatus
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
    val error: String? = null
) {
    val canUseScan: Boolean
        get() = premiumStatus == PremiumStatus.TRIAL || premiumStatus == PremiumStatus.PREMIUM
}

@HiltViewModel
class MembershipViewModel @Inject constructor(
    private val api: EntitlementApi
) : ViewModel() {

    private val _ui = MutableStateFlow(MembershipUiState())
    val ui = _ui.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            runCatching {
                api.me()
            }.onSuccess { dto ->
                _ui.value = MembershipUiState(
                    loading = false,
                    premiumStatus = PremiumStatus.from(dto.premiumStatus),
                    currentPremiumUntil = dto.currentPremiumUntil,
                    trialEndsAt = dto.trialEndsAt,
                    trialDaysLeft = dto.trialDaysLeft
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
