package com.calai.app.data.prefs

import com.calai.app.data.PrefsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TokenManager @Inject constructor(
    private val prefs: PrefsRepository   // ← 變成類別屬性
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // 快取目前 token（null 表示尚未登入/未設定）
    val tokenState: StateFlow<String?> =
        prefs.tokenFlow.stateIn(scope, SharingStarted.Eagerly, null)

    /** 提供攔截器或其他地方同步讀取 */
    fun currentToken(): String? = tokenState.value

    /** 測試/登入流程設定與清除 */
    fun setTokenAsync(token: String) = scope.launch { prefs.setToken(token) }
    fun clearTokenAsync()           = scope.launch { prefs.clearToken() }
}
