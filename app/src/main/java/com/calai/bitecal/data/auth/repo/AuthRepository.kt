package com.calai.bitecal.data.auth.repo

import com.calai.bitecal.data.auth.api.AuthApi
import com.calai.bitecal.data.auth.api.model.AuthResponse
import com.calai.bitecal.data.auth.api.model.GoogleSignInExchangeRequest
import com.calai.bitecal.data.auth.api.model.RefreshRequest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.firstOrNull
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    @Named("authApi") private val api: AuthApi,   // ← 指向 auth 專用 Retrofit
    private val tokenStore: TokenStore
) {
    suspend fun loginWithGoogle(idToken: String, clientId: String? = null): AuthResponse {
        val resp = api.googleLogin(
            GoogleSignInExchangeRequest(idToken = idToken, clientId = clientId)
        )
        // 你目前的 AuthResponse 若沒有 expiresIn / serverTime，可先用兩參數版本
        tokenStore.save(resp.accessToken, resp.refreshToken)
        return resp
    }

    suspend fun logout() {
        try {
            logoutRemote()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            // Keep legacy behavior: local sign-out must still complete if the remote call fails.
        }
        tokenStore.clear()
    }

    suspend fun logoutRemoteThenClear(): Result<Unit> =
        try {
            logoutRemote()
            tokenStore.clear()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }

    private suspend fun logoutRemote() {
        val access = tokenStore.accessTokenFlow.firstOrNull()
        val refresh = tokenStore.refreshTokenFlow.firstOrNull()
        if (access.isNullOrBlank() && refresh.isNullOrBlank()) return

        api.logout(
            authorization = access?.takeIf { it.isNotBlank() }?.let { "Bearer $it" },
            body = refresh?.takeIf { it.isNotBlank() }?.let { RefreshRequest(it) }
        )
    }

    /**
     * 是否已登入：
     * - access token 不為空
     * - 且（如有設定）未過期（加 5 秒緩衝避免臨界點）
     */
    suspend fun isSignedIn(): Boolean {
        val access = tokenStore.accessTokenFlow.firstOrNull()
        if (access.isNullOrBlank()) return false

        val expiresAtSec = tokenStore.accessExpiresAtFlow.firstOrNull()
        val nowSec = System.currentTimeMillis() / 1000
        // 若沒有記錄到期時間，就以「存在 access token」視為已登入
        return expiresAtSec == null || expiresAtSec > (nowSec + 5)
    }
}
