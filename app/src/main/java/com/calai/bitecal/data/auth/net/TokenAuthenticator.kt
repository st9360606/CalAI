package com.calai.bitecal.data.auth.net

import com.calai.bitecal.data.auth.api.AuthApi
import com.calai.bitecal.data.auth.api.model.RefreshRequest
import com.calai.bitecal.data.auth.repo.TokenStore
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import javax.inject.Inject
import javax.inject.Named

/**
 * 全域 Session 事件匯流排：當 refresh 失敗（或整體會話失效）時發出通知，讓 UI 導回登入。
 */
object SessionBus {
    private val _expired = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val expired = _expired.asSharedFlow()
    fun emitExpired() { _expired.tryEmit(Unit) }
}

class TokenAuthenticator @Inject constructor(
    private val tokenStore: TokenStore,
    @Named("authApi") private val authApi: AuthApi // 使用不掛 authenticator 的 Retrofit，避免依賴循環
) : Authenticator {

    private val lock = Any()

    override fun authenticate(route: Route?, response: Response): Request? {
        // 避免無限重試
        if (responseCount(response) >= 2) return null

        synchronized(lock) {
            // 可能別的請求已經刷新成功 → 直接帶新 access 再試一次
            tokenStore.getAccessBlocking()?.let { existing ->
                val authOnReq = response.request.header("Authorization")
                if (authOnReq != "Bearer $existing") {
                    return response.request.newBuilder()
                        .header("Authorization", "Bearer $existing")
                        .build()
                }
            }

            val refresh = tokenStore.getRefreshBlocking() ?: return fail()

            val call = authApi.refresh(RefreshRequest(refreshToken = refresh, deviceId = null))
            val resp = call.execute() // 同步
            if (!resp.isSuccessful) return fail()

            val body = resp.body() ?: return fail()

            tokenStore.saveBlocking(body.accessToken, body.refreshToken ?: refresh)

            return response.request.newBuilder()
                .header("Authorization", "Bearer ${body.accessToken}")
                .build()
        }
    }

    private fun fail(): Request? {
        // 立刻清空 access（避免後續請求重複 401），並通知 UI 回登入
        tokenStore.saveBlocking(access = "", refresh = null)
        SessionBus.emitExpired()
        return null
    }

    private fun responseCount(response: Response): Int {
        var r: Response? = response
        var count = 1
        while (r?.priorResponse != null) { count++; r = r.priorResponse }
        return count
    }
}
