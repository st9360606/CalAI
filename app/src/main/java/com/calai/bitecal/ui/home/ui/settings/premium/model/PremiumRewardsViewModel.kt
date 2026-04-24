package com.calai.bitecal.ui.home.ui.settings.premium.model

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.calai.bitecal.data.membership.api.MembershipSummaryDto
import com.calai.bitecal.data.membership.api.RewardHistoryItemDto
import com.calai.bitecal.data.membership.repo.MembershipRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import javax.inject.Inject

@HiltViewModel
class PremiumRewardsViewModel @Inject constructor(
    private val membershipRepository: MembershipRepository
) : ViewModel() {

    data class UiState(
        val loading: Boolean = true,
        val summary: MembershipSummaryDto? = null,
        val rewards: List<RewardHistoryItemDto> = emptyList(),
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
            supervisorScope {
                val summary = async { membershipRepository.getSummary() }
                val rewards = async { membershipRepository.getRewardHistory() }
                summary.await() to rewards.await()
            }
        }.onSuccess { (summary, rewards) ->
            _ui.update { it.copy(loading = false, summary = summary, rewards = rewards, error = null) }
        }.onFailure { t ->
            _ui.update { it.copy(loading = false, error = t.message ?: t.javaClass.simpleName) }
        }
    }
}
