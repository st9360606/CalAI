package com.calai.bitecal.ui.home.ui.settings.model

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.calai.bitecal.data.entitlement.EntitlementSyncer
import com.calai.bitecal.data.entitlement.RestoreSubscriptionResult
import com.calai.bitecal.data.entitlement.model.PremiumStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class RestoreSubscriptionDialogState {
    Hidden,
    CandidateFound,
    Restoring,
    Restored,
    NoActivePurchase,
    Failed,
    BoundToAnotherAccount
}

data class RestoreSubscriptionUiState(
    val dialogState: RestoreSubscriptionDialogState = RestoreSubscriptionDialogState.Hidden,
    val dismissedInSession: Boolean = false,
    val checkingCandidate: Boolean = false,
    val message: String? = null
) {
    val isRestoring: Boolean
        get() = dialogState == RestoreSubscriptionDialogState.Restoring

    val visible: Boolean
        get() = dialogState != RestoreSubscriptionDialogState.Hidden
}

@HiltViewModel
class RestoreSubscriptionViewModel @Inject constructor(
    private val entitlementSyncer: EntitlementSyncer
) : ViewModel() {

    private companion object {
        /**
         * 只存在 App process 記憶體內。
         *
         * 目的：
         * - Home 關閉 Dialog 後，進 Settings 再回 Home，不要因 ViewModel 重建而再次自動跳出。
         * - App process 被殺掉 / 重新開 App 後會重置，符合「不是永久不再提示」。
         */
        @Volatile
        private var dismissedInAppSession: Boolean = false
    }

    private val _ui = MutableStateFlow(
        RestoreSubscriptionUiState(
            dismissedInSession = dismissedInAppSession
        )
    )
    val ui: StateFlow<RestoreSubscriptionUiState> = _ui

    fun checkCandidateAfterMembershipLoaded(
        premiumStatus: PremiumStatus,
        membershipLoading: Boolean
    ) {
        val current = _ui.value

        if (membershipLoading || premiumStatus != PremiumStatus.FREE) {
            return
        }

        if (
            dismissedInAppSession ||
            current.dismissedInSession ||
            current.visible ||
            current.checkingCandidate
        ) {
            return
        }

        viewModelScope.launch {
            _ui.update { state ->
                if (
                    dismissedInAppSession ||
                    state.dismissedInSession ||
                    state.visible ||
                    state.checkingCandidate
                ) {
                    state
                } else {
                    state.copy(
                        checkingCandidate = true,
                        message = null
                    )
                }
            }

            val shouldContinue =
                _ui.value.checkingCandidate &&
                        !dismissedInAppSession &&
                        !_ui.value.dismissedInSession &&
                        !_ui.value.visible

            if (!shouldContinue) {
                return@launch
            }

            val hasCandidate = entitlementSyncer.hasActiveSubscriptionOnDevice()

            _ui.update { state ->
                when {
                    dismissedInAppSession || state.dismissedInSession -> {
                        state.copy(
                            checkingCandidate = false,
                            dialogState = RestoreSubscriptionDialogState.Hidden,
                            message = null
                        )
                    }

                    state.visible -> {
                        state.copy(
                            checkingCandidate = false
                        )
                    }

                    hasCandidate -> {
                        state.copy(
                            checkingCandidate = false,
                            dialogState = RestoreSubscriptionDialogState.CandidateFound,
                            message = null
                        )
                    }

                    else -> {
                        state.copy(
                            checkingCandidate = false,
                            dialogState = RestoreSubscriptionDialogState.Hidden,
                            message = null
                        )
                    }
                }
            }
        }
    }

    fun openManualRestore() {
        _ui.update {
            it.copy(
                dialogState = RestoreSubscriptionDialogState.CandidateFound,
                checkingCandidate = false,
                message = null
            )
        }
    }

    fun restoreSubscription(onRestored: () -> Unit) {
        if (_ui.value.isRestoring) return

        viewModelScope.launch {
            _ui.update {
                it.copy(
                    dialogState = RestoreSubscriptionDialogState.Restoring,
                    checkingCandidate = false,
                    message = null
                )
            }

            when (val result = entitlementSyncer.restoreSubscription()) {
                is RestoreSubscriptionResult.Restored -> {
                    _ui.update {
                        it.copy(
                            dialogState = RestoreSubscriptionDialogState.Restored,
                            message = null
                        )
                    }
                    onRestored()
                }

                RestoreSubscriptionResult.NoActivePurchase -> {
                    _ui.update {
                        it.copy(
                            dialogState = RestoreSubscriptionDialogState.NoActivePurchase,
                            message = null
                        )
                    }
                }

                RestoreSubscriptionResult.BoundToAnotherAccount -> {
                    _ui.update {
                        it.copy(
                            dialogState = RestoreSubscriptionDialogState.BoundToAnotherAccount,
                            message = null
                        )
                    }
                }

                is RestoreSubscriptionResult.Failed -> {
                    _ui.update {
                        it.copy(
                            dialogState = RestoreSubscriptionDialogState.Failed,
                            message = result.message
                        )
                    }
                }
            }
        }
    }

    fun dismissForSession() {
        if (_ui.value.isRestoring) return

        dismissedInAppSession = true

        _ui.update {
            it.copy(
                dialogState = RestoreSubscriptionDialogState.Hidden,
                dismissedInSession = true,
                checkingCandidate = false,
                message = null
            )
        }
    }

    fun closeDialog() {
        if (_ui.value.isRestoring) return

        dismissedInAppSession = true

        _ui.update {
            it.copy(
                dialogState = RestoreSubscriptionDialogState.Hidden,
                dismissedInSession = true,
                checkingCandidate = false,
                message = null
            )
        }
    }
}
