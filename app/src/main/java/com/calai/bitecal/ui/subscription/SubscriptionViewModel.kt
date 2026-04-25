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
)

@HiltViewModel
class SubscriptionViewModel @Inject constructor(
    private val entitlementSyncer: EntitlementSyncer
) : ViewModel() {

    private val _ui = MutableStateFlow(SubscriptionUiState())
    val ui = _ui.asStateFlow()

    fun selectProduct(productId: String) {
        if (_ui.value.purchasing) return

        _ui.update {
            it.copy(
                selectedProductId = productId,
                error = null
            )
        }
    }

    fun purchase(
        activity: Activity,
        onSuccess: (EntitlementSyncResponse) -> Unit
    ) {
        val productId = _ui.value.selectedProductId
        if (_ui.value.purchasing) return

        viewModelScope.launch {
            _ui.update { it.copy(purchasing = true, error = null) }

            val result = entitlementSyncer.purchaseSubscriptionAndSync(
                activity = activity,
                productId = productId
            )

            if (result.success && result.response != null) {
                _ui.update { it.copy(purchasing = false, error = null) }
                onSuccess(result.response)
            } else {
                _ui.update {
                    it.copy(
                        purchasing = false,
                        error = result.message ?: "Purchase failed"
                    )
                }
            }
        }
    }
}
