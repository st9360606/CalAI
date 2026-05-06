package com.calai.bitecal.ui.subscription

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.calai.bitecal.data.billing.BiteCalBillingProducts
import com.calai.bitecal.data.entitlement.EntitlementSyncer
import com.calai.bitecal.data.entitlement.api.EntitlementSyncResponse
import com.calai.bitecal.data.membership.repo.MembershipRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

data class SubscriptionUiState(
    val selectedProductId: String = BiteCalBillingProducts.MONTHLY,
    val purchasing: Boolean = false,
    val error: String? = null,
    val canRestorePurchase: Boolean = false,
    val trialEligible: Boolean = false,
    val trialEligibilityLoaded: Boolean = false
) {
    val busy: Boolean
        get() = purchasing
}

@HiltViewModel
class SubscriptionViewModel @Inject constructor(
    private val entitlementSyncer: EntitlementSyncer,
    private val membershipRepository: MembershipRepository
) : ViewModel() {

    private val _ui = MutableStateFlow(SubscriptionUiState())
    val ui = _ui.asStateFlow()

    fun loadTrialEligibility() {
        if (_ui.value.trialEligibilityLoaded) return

        viewModelScope.launch {
            val summary = runCatching {
                membershipRepository.getSummary()
            }.getOrNull()

            _ui.update {
                it.copy(
                    trialEligible = summary?.trialEligible == true,
                    trialEligibilityLoaded = true
                )
            }
        }
    }

    fun selectProduct(productId: String) {
        if (_ui.value.busy) return

        _ui.update {
            it.copy(
                selectedProductId = productId,
                error = null,
                canRestorePurchase = false
            )
        }
    }

    fun purchase(
        activity: Activity,
        offerTag: String? = null,
        onSuccess: (EntitlementSyncResponse) -> Unit,
        onCancelled: (() -> Unit)? = null
    ) {
        purchaseProduct(
            activity = activity,
            productId = _ui.value.selectedProductId,
            offerTag = offerTag,
            onSuccess = onSuccess,
            onCancelled = onCancelled
        )
    }

    fun purchaseProduct(
        activity: Activity,
        productId: String,
        offerTag: String? = null,
        onSuccess: (EntitlementSyncResponse) -> Unit,
        onCancelled: (() -> Unit)? = null
    ) {
        if (_ui.value.busy) return

        viewModelScope.launch {
            _ui.update {
                it.copy(
                    selectedProductId = productId,
                    purchasing = true,
                    error = null,
                    canRestorePurchase = false
                )
            }

            val result = entitlementSyncer.purchaseSubscriptionAndSync(
                activity = activity,
                productId = productId,
                offerTag = offerTag
            )

            if (result.success && result.response != null) {
                _ui.update {
                    it.copy(
                        purchasing = false,
                        error = null,
                        canRestorePurchase = false,
                        trialEligible = result.response.trialEligible,
                        trialEligibilityLoaded = true
                    )
                }
                onSuccess(result.response)
                return@launch
            }

            if (isPurchaseCancelledMessage(result.message)) {
                _ui.update {
                    it.copy(
                        purchasing = false,
                        error = null,
                        canRestorePurchase = false
                    )
                }
                onCancelled?.invoke()
                return@launch
            }

            _ui.update {
                it.copy(
                    purchasing = false,
                    error = result.message ?: "Purchase failed",
                    canRestorePurchase = isPostPurchaseSyncFailure(result.message),
                    trialEligible = result.response?.trialEligible ?: it.trialEligible,
                    trialEligibilityLoaded = if (result.response != null) true else it.trialEligibilityLoaded
                )
            }
        }
    }

    fun restorePurchase(onSuccess: (EntitlementSyncResponse) -> Unit) {
        if (_ui.value.busy) return

        viewModelScope.launch {
            _ui.update {
                it.copy(
                    purchasing = true,
                    error = null,
                    canRestorePurchase = false
                )
            }

            val response = runCatching {
                entitlementSyncer.refreshEntitlementSummary()
            }.getOrElse { ex ->
                _ui.update {
                    it.copy(
                        purchasing = false,
                        error = ex.message ?: "Could not restore purchase. Please try again.",
                        canRestorePurchase = true
                    )
                }
                return@launch
            }

            _ui.update {
                it.copy(
                    purchasing = false,
                    trialEligible = response.trialEligible,
                    trialEligibilityLoaded = true
                )
            }

            if (hasOpenedEntitlement(response)) {
                _ui.update {
                    it.copy(
                        error = null,
                        canRestorePurchase = false
                    )
                }
                onSuccess(response)
            } else {
                _ui.update {
                    it.copy(
                        error = "No active purchase found.",
                        canRestorePurchase = true
                    )
                }
            }
        }
    }

    private fun hasOpenedEntitlement(response: EntitlementSyncResponse): Boolean {
        val premiumStatus = response.premiumStatus.uppercase(Locale.US)

        return response.status.equals("ACTIVE", ignoreCase = true) &&
                (premiumStatus == "PREMIUM" || premiumStatus == "TRIAL") &&
                !response.entitlementType.isNullOrBlank() &&
                response.currentPremiumUntil != null
    }

    private fun isPostPurchaseSyncFailure(message: String?): Boolean {
        return message.orEmpty().contains(
            other = "entitlement sync failed",
            ignoreCase = true
        )
    }

    private fun isPurchaseCancelledMessage(message: String?): Boolean {
        val raw = message.orEmpty()

        return raw.contains("cancel", ignoreCase = true) ||
                raw.contains("USER_CANCELED", ignoreCase = true)
    }
}
