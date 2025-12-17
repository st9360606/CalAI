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
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject

private const val OTP_LEN = 4
private const val RESEND_SEC = 30

data class EmailEnterUiState(
    val email: String = "",
    val isValid: Boolean = false,
    val loading: Boolean = false,
    val error: String? = null
)

/** 用 enum 表示錯誤種類，畫面再用 stringResource 對應文字 */
enum class EmailCodeError {
    INVALID_CODE,          // 驗證碼錯誤
    TOO_MANY_ATTEMPTS,     // 嘗試太頻繁 / 429
    NETWORK,               // 網路異常
    SERVER,                // 伺服器錯誤（5xx 等）
    UNKNOWN                // 其他
}

data class EmailCodeUiState(
    val email: String,
    val code: String = "",
    val canResendInSec: Int = RESEND_SEC,
    val loading: Boolean = false,
    val error: EmailCodeError? = null,
    val errorMsg: String? = null // UNKNOWN 時的備援訊息
)

@HiltViewModel
class EmailSignInViewModel @Inject constructor(
    private val repo: EmailAuthRepository
) : ViewModel() {

    private val _enter = MutableStateFlow(EmailEnterUiState())
    val enter: StateFlow<EmailEnterUiState> = _enter

    private val _code = MutableStateFlow<EmailCodeUiState?>(null)
    val code: StateFlow<EmailCodeUiState?> = _code

    private var timerJob: Job? = null

    /* ---------------- Email 輸入畫面 ---------------- */

    fun onEmailChange(text: String) {
        _enter.value = _enter.value.copy(
            email = text,
            isValid = text.isNotBlank() && android.util.Patterns.EMAIL_ADDRESS.matcher(text).matches()
        )
    }

    fun sendCode(onSent: (String) -> Unit) {
        val email = _enter.value.email.trim()
        viewModelScope.launch {
            try {
                _enter.value = _enter.value.copy(loading = true, error = null)
                if (repo.start(email)) {
                    _enter.value = _enter.value.copy(loading = false)
                    _code.value = EmailCodeUiState(email = email, canResendInSec = RESEND_SEC)
                    startResendTimer(forceRestart = true)
                    onSent(email)
                } else {
                    _enter.value = _enter.value.copy(loading = false, error = "Send failed")
                }
            } catch (t: Throwable) {
                _enter.value = _enter.value.copy(loading = false, error = t.message)
            }
        }
    }

    /* ---------------- 驗證碼畫面 ---------------- */

    fun onCodeChange(text: String) {
        val clean = text.filter { it.isDigit() }.take(OTP_LEN)
        _code.value = _code.value?.copy(code = clean, error = null, errorMsg = null) // 輸入時清錯誤
    }

    fun verify(onSuccess: () -> Unit) {
        val s = _code.value ?: return
        if (s.code.length != OTP_LEN) return
        viewModelScope.launch {
            try {
                _code.value = s.copy(loading = true, error = null, errorMsg = null)
                repo.verify(s.email, s.code)
                _code.value = s.copy(loading = false)
                onSuccess()
            } catch (t: Throwable) {
                val (err, clear, detail) = when (t) {
                    is HttpException -> when (t.code()) {
                        400 -> Triple(EmailCodeError.INVALID_CODE, true, null)
                        429 -> Triple(EmailCodeError.TOO_MANY_ATTEMPTS, false, null)
                        in 500..599 -> Triple(EmailCodeError.SERVER, false, null)
                        else -> Triple(EmailCodeError.UNKNOWN, false, "HTTP ${t.code()}")
                    }
                    is IOException -> Triple(EmailCodeError.NETWORK, false, null)
                    else -> Triple(EmailCodeError.UNKNOWN, false, t.message)
                }
                _code.value = s.copy(
                    loading = false,
                    error = err,
                    errorMsg = detail,
                    code = if (clear) "" else s.code // 驗證碼錯誤要清空
                )
            }
        }
    }

    fun resend() {
        val s = _code.value ?: return
        if (s.canResendInSec > 0) return
        viewModelScope.launch {
            try {
                _code.value = s.copy(loading = true, error = null, errorMsg = null, code = "")
                if (repo.start(s.email)) {
                    _code.value = s.copy(loading = false, canResendInSec = RESEND_SEC, code = "")
                    startResendTimer(forceRestart = true)
                } else {
                    _code.value = s.copy(loading = false, error = EmailCodeError.UNKNOWN, errorMsg = "Resend failed")
                }
            } catch (t: Throwable) {
                _code.value = s.copy(loading = false, error = EmailCodeError.UNKNOWN, errorMsg = t.message)
            }
        }
    }

    /** 由 Code 畫面呼叫，初始化並開始倒數 */
    fun prepareCode(email: String) {
        val trimmed = email.trim()
        if (_code.value == null && trimmed.isNotBlank()) {
            _code.value = EmailCodeUiState(email = trimmed, canResendInSec = RESEND_SEC)
            startResendTimer(forceRestart = true)
        }
    }

    private fun startResendTimer(forceRestart: Boolean = false) {
        if (forceRestart) timerJob?.cancel()
        if (timerJob != null) return
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                val cur = _code.value ?: break
                val next = (cur.canResendInSec - 1).coerceAtLeast(0)
                _code.value = cur.copy(canResendInSec = next)
                if (next == 0) break
            }
            timerJob = null
        }
    }

    override fun onCleared() {
        timerJob?.cancel()
        super.onCleared()
    }
}
