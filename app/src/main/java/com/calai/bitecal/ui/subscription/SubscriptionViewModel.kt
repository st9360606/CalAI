package com.calai.bitecal.ui.subscription

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.calai.bitecal.data.billing.BiteCalBillingProducts
import com.calai.bitecal.data.entitlement.EntitlementSyncer
import com.calai.bitecal.data.entitlement.api.EntitlementSyncResponse
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SubscriptionUiState(
    val selectedProductId: String = BiteCalBillingProducts.MONTHLY,
    val purchasing: Boolean = false,
    val error: String? = null
) {
    val busy: Boolean
        get() = purchasing
}

@HiltViewModel
class SubscriptionViewModel @Inject constructor(
    private val entitlementSyncer: EntitlementSyncer
) : ViewModel() {

    private val _ui = MutableStateFlow(SubscriptionUiState())
    val ui = _ui.asStateFlow()

    fun selectProduct(productId: String) {
        if (_ui.value.busy) return

        _ui.update {
            it.copy(
                selectedProductId = productId,
                error = null
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
                    error = null
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
                        error = null
                    )
                }
                onSuccess(result.response)
                return@launch
            }

            _ui.update {
                it.copy(
                    purchasing = false,
                    error = result.message ?: "Purchase failed"
                )
            }

            if (isPurchaseCancelledMessage(result.message)) {
                onCancelled?.invoke()
            }
        }
    }

    private fun isPurchaseCancelledMessage(message: String?): Boolean {
        val raw = message.orEmpty()

        return raw.contains("cancel", ignoreCase = true) ||
                raw.contains("USER_CANCELED", ignoreCase = true)
    }
}
