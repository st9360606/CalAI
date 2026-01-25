// app/src/main/java/com/calai/app/data/account/repo/AccountRepository.kt
package com.calai.app.data.account.repo

import com.calai.app.data.account.api.AccountApi
import com.calai.app.data.auth.repo.TokenStore
import com.calai.app.data.profile.repo.UserProfileStore
import retrofit2.HttpException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AccountRepository @Inject constructor(
    private val api: AccountApi,
    private val tokenStore: TokenStore,
    private val profileStore: UserProfileStore,
) {

    /**
     * ✅ 最小閉環（強化版）：
     * - 正常 200 ok=true：清本機 -> success
     * - 401/403：視為「後端已 revoke / token 不再可用」-> 仍清本機 -> success
     * - 其他錯：回 failure（讓 UI 顯示錯誤）
     */
    suspend fun deleteAccount(): Result<Unit> {
        return runCatching {
            val res = api.requestDeletion()
            if (!res.ok) throw IllegalStateException("DELETE_ACCOUNT_FAILED")

            clearLocalAuth()
            Unit
        }.recoverCatching { e ->
            // ✅ 重要：401/403 視為成功（revoke race）
            if (e is HttpException && (e.code() == 401 || e.code() == 403)) {
                clearLocalAuth()
                return@recoverCatching Unit
            }
            throw e
        }
    }

    private suspend fun clearLocalAuth() {
        tokenStore.clear()
        runCatching { profileStore.clearHasServerProfile() }
        runCatching { profileStore.clearOnboarding() }
    }
}
