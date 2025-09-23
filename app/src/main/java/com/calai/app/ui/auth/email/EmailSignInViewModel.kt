package com.calai.app.ui.auth.email

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.calai.app.data.auth.repo.EmailAuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val OTP_LEN = 4
private const val RESEND_SEC = 30

data class EmailEnterUiState(
    val email: String = "",
    val isValid: Boolean = false,
    val loading: Boolean = false,
    val error: String? = null
)

data class EmailCodeUiState(
    val email: String,
    val code: String = "",
    val canResendInSec: Int = RESEND_SEC,
    val loading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class EmailSignInViewModel @Inject constructor(
    private val repo: EmailAuthRepository
) : ViewModel() {

    private val _enter = MutableStateFlow(EmailEnterUiState())
    val enter: StateFlow<EmailEnterUiState> = _enter

    private var timerJob: Job? = null
    private val _code = MutableStateFlow<EmailCodeUiState?>(null)
    val code: StateFlow<EmailCodeUiState?> = _code

    fun onEmailChange(text: String) {
        _enter.value = _enter.value.copy(
            email = text,
            isValid = text.isNotBlank() && android.util.Patterns.EMAIL_ADDRESS.matcher(text)
                .matches()
        )
    }

    fun sendCode(onSent: (String) -> Unit) {
        val email = _enter.value.email.trim()
        viewModelScope.launch {
            try {
                _enter.value = _enter.value.copy(loading = true, error = null)
                if (repo.start(email)) {
                    _enter.value = _enter.value.copy(loading = false)
                    _code.value = EmailCodeUiState(email = email)
                    startResendTimer()
                    onSent(email)
                } else {
                    _enter.value = _enter.value.copy(loading = false, error = "Send failed")
                }
            } catch (t: Throwable) {
                _enter.value = _enter.value.copy(loading = false, error = t.message)
            }
        }
    }

    fun onCodeChange(text: String) {
        val clean = text.filter { it.isDigit() }.take(OTP_LEN)
        _code.value = _code.value?.copy(code = clean)
    }

    fun verify(onSuccess: () -> Unit) {
        val s = _code.value ?: return
        if (s.code.length != OTP_LEN) return
        viewModelScope.launch {
            try {
                _code.value = s.copy(loading = true, error = null)
                repo.verify(s.email, s.code)
                _code.value = s.copy(loading = false)
                onSuccess()
            } catch (t: Throwable) {
                _code.value = s.copy(loading = false, error = t.message)
            }
        }
    }

    fun resend() {
        val s = _code.value ?: return
        if (s.canResendInSec > 0) return
        viewModelScope.launch {
            try {
                _code.value = s.copy(loading = true, error = null)
                if (repo.start(s.email)) {
                    _code.value = s.copy(loading = false, canResendInSec = RESEND_SEC)
                    startResendTimer()
                } else {
                    _code.value = s.copy(loading = false, error = "Resend failed")
                }
            } catch (t: Throwable) {
                _code.value = s.copy(loading = false, error = t.message)
            }
        }
    }

    private fun startResendTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                val s = _code.value ?: break
                val next = (s.canResendInSec - 1).coerceAtLeast(0)
                _code.value = s.copy(canResendInSec = next)
                if (next == 0) break
            }
        }
    }

    // 在 EmailSignInViewModel 內新增
    fun prepareCode(email: String) {
        if (_code.value == null && email.isNotBlank()) {
            _code.value = EmailCodeUiState(email = email.trim())
        }
    }

}
