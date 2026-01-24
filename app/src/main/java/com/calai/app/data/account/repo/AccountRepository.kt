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
     * ✅ 最小閉環：
     * 1) 呼叫後端 deletion-request（後端會 revoke token + 去識別）
     * 2) 本機清 token + 清 hasServerProfile（避免下次誤判）
     */
    suspend fun deleteAccount(): Result<Unit> {
        return runCatching {
            val res = api.requestDeletion()
            if (!res.ok) throw IllegalStateException("DELETE_ACCOUNT_FAILED")

            // ✅ 本機登出（不要依賴 /auth/logout，因為後端已 revoke）
            tokenStore.clear()
            runCatching { profileStore.clearHasServerProfile() }
            runCatching { profileStore.clearOnboarding() }

            Unit
        }.recoverCatching { e ->
            // 你若想「401/403 也當失敗」，這裡直接丟回去即可
            // （如果你想 401/403 視為成功，我再給你另一版）
            if (e is HttpException && (e.code() == 401 || e.code() == 403)) {
                throw e
            }
            throw e
        }
    }
}
