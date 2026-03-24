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
 * 全域 Session 事件匯流排：當 refresh 明確失敗（或整體會話失效）時發出通知，讓 UI 導回登入。
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
            // 可能其他請求已經 refresh 成功了，直接帶新 token 重送
            tokenStore.getAccessBlocking()?.let { existing ->
                val authOnReq = response.request.header("Authorization")
                if (authOnReq != "Bearer $existing" && existing.isNotBlank()) {
                    return response.request.newBuilder()
                        .header("Authorization", "Bearer $existing")
                        .build()
                }
            }

            val refresh = tokenStore.getRefreshBlocking()
                ?.takeIf { it.isNotBlank() }
                ?: return failHard()

            val resp = try {
                authApi.refresh(
                    RefreshRequest(
                        refreshToken = refresh,
                        deviceId = null
                    )
                ).execute()
            } catch (t: Throwable) {
                // 暫時性網路/服務故障，不要直接登出使用者
                return failSoft()
            }

            if (resp.isSuccessful) {
                val body = resp.body() ?: return failSoft()

                val newAccess = body.accessToken.takeIf { it.isNotBlank() } ?: return failSoft()
                val newRefresh = body.refreshToken?.takeIf { it.isNotBlank() } ?: refresh

                tokenStore.saveBlocking(newAccess, newRefresh)

                return response.request.newBuilder()
                    .header("Authorization", "Bearer $newAccess")
                    .build()
            }

            return when (resp.code()) {
                400, 401, 403 -> failHard()
                else -> failSoft()
            }
        }
    }

    private fun failHard(): Request? {
        // refresh token 明確失效 / 被拒絕，才清 session 並通知 UI 回登入
        tokenStore.saveBlocking(access = "", refresh = null)
        SessionBus.emitExpired()
        return null
    }

    private fun failSoft(): Request? {
        // 暫時性故障：保留 session，讓當前請求失敗即可
        return null
    }

    private fun responseCount(response: Response): Int {
        var r: Response? = response
        var count = 1
        while (r?.priorResponse != null) {
            count++
            r = r.priorResponse
        }
        return count
    }
}
