package com.calai.app.data.auth.state

import com.calai.app.data.auth.repo.TokenStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 封裝登入狀態，用 TokenStore 的 accessToken 是否存在判斷。
 */
@Singleton
class AuthState @Inject constructor(
    tokenStore: TokenStore
) {
    val isSignedInFlow: Flow<Boolean> =
        tokenStore.accessTokenFlow.map { !it.isNullOrBlank() }
}
