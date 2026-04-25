package com.calai.bitecal.ui.subscription

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.calai.bitecal.data.billing.BiteCalBillingProducts
import com.calai.bitecal.data.entitlement.EntitlementSyncer
import com.calai.bitecal.data.entitlement.api.EntitlementSyncResponse
import com.calai.bitecal.data.entitlement.api.TrialGrantResponse
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.HttpException
import javax.inject.Inject

data class SubscriptionUiState(
    val selectedProductId: String = BiteCalBillingProducts.MONTHLY,
    val startingTrial: Boolean = false,
    val purchasing: Boolean = false,
    val error: String? = null
) {
    val busy: Boolean
        get() = startingTrial || purchasing
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

    fun startTrial(
        onSuccess: (TrialGrantResponse) -> Unit
    ) {
        if (_ui.value.busy) return

        viewModelScope.launch {
            _ui.update {
                it.copy(
                    startingTrial = true,
                    error = null
                )
            }

            val result = runCatching {
                entitlementSyncer.grantTrial()
            }

            result
                .onSuccess { response ->
                    _ui.update {
                        it.copy(
                            startingTrial = false,
                            error = null
                        )
                    }
                    onSuccess(response)
                }
                .onFailure { throwable ->
                    _ui.update {
                        it.copy(
                            startingTrial = false,
                            error = throwable.toTrialMessage()
                        )
                    }
                }
        }
    }

    fun purchase(
        activity: Activity,
        onSuccess: (EntitlementSyncResponse) -> Unit
    ) {
        val productId = _ui.value.selectedProductId
        if (_ui.value.busy) return

        viewModelScope.launch {
            _ui.update {
                it.copy(
                    purchasing = true,
                    error = null
                )
            }

            val result = entitlementSyncer.purchaseSubscriptionAndSync(
                activity = activity,
                productId = productId
            )

            if (result.success && result.response != null) {
                _ui.update {
                    it.copy(
                        purchasing = false,
                        error = null
                    )
                }
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

    private fun Throwable.toTrialMessage(): String {
        val raw = when (this) {
            is HttpException -> {
                val body = runCatching {
                    response()?.errorBody()?.string()
                }.getOrNull()

                body ?: message()
            }

            else -> message
        }.orEmpty()

        return when {
            raw.contains("TRIAL_ALREADY_USED", ignoreCase = true) ||
                    raw.contains("EMAIL_ALREADY_USED", ignoreCase = true) ||
                    raw.contains("DEVICE_ALREADY_USED", ignoreCase = true) ->
                "This free trial has already been used. Please choose a subscription plan to continue."

            raw.contains("DEVICE_ID_REQUIRED", ignoreCase = true) ->
                "Unable to start trial on this device. Please restart the app and try again."

            raw.contains("TRIAL_HASH_SECRET_NOT_CONFIGURED", ignoreCase = true) ->
                "Trial is temporarily unavailable. Please choose a subscription plan or try again later."

            else ->
                "Unable to start free trial. Please try again or choose a subscription plan."
        }
    }
}
