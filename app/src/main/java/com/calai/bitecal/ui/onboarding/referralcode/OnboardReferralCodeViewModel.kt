package com.calai.bitecal.ui.onboarding.referralcode

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.calai.bitecal.data.referral.repo.ReferralRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException
import java.util.Locale
import javax.inject.Inject

data class OnboardReferralCodeUiState(
    val code: String = "",
    val submitting: Boolean = false,
    val applied: Boolean = false,
    val errorCode: String? = null,
    val showSkipAndContinue: Boolean = false,
) {
    val submitEnabled: Boolean
        get() = code.isNotBlank() && !submitting && !applied && isValidBasicCode(code)

    companion object {
        fun isValidBasicCode(code: String): Boolean {
            return code.length in 1..24 && code.all { it.isLetterOrDigit() }
        }
    }
}

@HiltViewModel
class OnboardReferralCodeViewModel @Inject constructor(
    private val referralRepository: ReferralRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardReferralCodeUiState())
    val uiState: StateFlow<OnboardReferralCodeUiState> = _uiState

    fun onCodeChanged(raw: String) {
        if (_uiState.value.applied) return

        val normalized = raw
            .trim()
            .uppercase(Locale.US)
            .filter { it.isLetterOrDigit() }
            .take(MAX_CODE_LENGTH)

        _uiState.update {
            it.copy(
                code = normalized,
                errorCode = null,
                showSkipAndContinue = false,
            )
        }
    }

    fun submit(onApplied: (() -> Unit)? = null) {
        val code = _uiState.value.code.trim()
        if (!_uiState.value.submitEnabled) return
        submitInternal(code = code, navigateOnSuccess = false, onReady = onApplied)
    }

    fun continueNext(onReady: () -> Unit) {
        val state = _uiState.value
        when {
            state.submitting -> Unit
            state.applied -> onReady()
            state.code.isBlank() -> onReady()
            else -> submitInternal(
                code = state.code.trim(),
                navigateOnSuccess = true,
                onReady = onReady,
            )
        }
    }

    fun skipAndContinue(onReady: () -> Unit) {
        if (_uiState.value.submitting) return
        onReady()
    }

    private fun submitInternal(
        code: String,
        navigateOnSuccess: Boolean,
        onReady: (() -> Unit)?,
    ) {
        if (!OnboardReferralCodeUiState.isValidBasicCode(code)) {
            _uiState.update {
                it.copy(
                    errorCode = ERROR_INVALID_FORMAT,
                    showSkipAndContinue = true,
                )
            }
            return
        }

        _uiState.update {
            it.copy(
                submitting = true,
                errorCode = null,
                showSkipAndContinue = false,
            )
        }

        viewModelScope.launch {
            runCatching {
                referralRepository.claim(code)
            }.onSuccess { response ->
                _uiState.update {
                    it.copy(
                        submitting = false,
                        applied = response.applied || response.alreadyApplied,
                        errorCode = null,
                        showSkipAndContinue = false,
                    )
                }
                if (navigateOnSuccess) onReady?.invoke()
            }.onFailure { throwable ->
                _uiState.update {
                    it.copy(
                        submitting = false,
                        errorCode = mapThrowableToErrorCode(throwable),
                        showSkipAndContinue = navigateOnSuccess,
                    )
                }
            }
        }
    }

    private fun mapThrowableToErrorCode(t: Throwable): String {
        if (t is IOException) return ERROR_NETWORK

        if (t is HttpException) {
            val raw = runCatching { t.response()?.errorBody()?.string().orEmpty() }
                .getOrDefault("")
                .uppercase(Locale.US)

            return when {
                "INVALID_PROMO_CODE" in raw -> "INVALID_PROMO_CODE"
                "SELF_REFERRAL" in raw -> "SELF_REFERRAL"
                "INVITEE_ALREADY_CLAIMED" in raw -> "INVITEE_ALREADY_CLAIMED"
                "INVITEE_ALREADY_SUBSCRIBED" in raw ||
                        "PREMIUM_ACTIVE" in raw ||
                        "TRIAL_ACTIVE" in raw ||
                        "PAYMENT_ISSUE" in raw ||
                        "PAYMENT_RECOVERY_REQUIRED" in raw ||
                        "HAS_PAID_HISTORY" in raw -> "INVITEE_ALREADY_SUBSCRIBED"
                "REFERRAL_DISABLED" in raw -> "REFERRAL_DISABLED"
                "RISK_REJECTED" in raw || "ABUSE_RISK" in raw -> "RISK_REJECTED"
                t.code() == 400 || t.code() == 404 -> "INVALID_PROMO_CODE"
                else -> ERROR_UNKNOWN
            }
        }

        return ERROR_UNKNOWN
    }

    companion object {
        private const val MAX_CODE_LENGTH = 24
        const val ERROR_INVALID_FORMAT = "INVALID_FORMAT"
        const val ERROR_NETWORK = "NETWORK"
        const val ERROR_UNKNOWN = "UNKNOWN"
    }
}
