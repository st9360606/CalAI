// app/src/main/java/com/calai/app/data/account/repo/AccountRepository.kt
package com.calai.bitecal.data.account.repo

import com.calai.bitecal.data.account.api.AccountApi
import com.calai.bitecal.data.account.api.AccountDeletionRequest
import com.calai.bitecal.data.auth.repo.TokenStore
import com.calai.bitecal.data.profile.repo.UserProfileStore
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
     * Account deletion must be confirmed by the backend before local auth is cleared.
     *
     * Do not treat 401/403 as success here. If the token is expired or invalid,
     * the deletion request may not have been created on the server. The UI should
     * ask the user to sign in again and retry the deletion request.
     */
    suspend fun deleteAccount(
        subscriptionWarningAcknowledged: Boolean = false,
        userRequestedGooglePlayCancel: Boolean = false
    ): Result<Unit> {
        return runCatching {
            val res = api.requestDeletion(
                AccountDeletionRequest(
                    subscriptionWarningAcknowledged = subscriptionWarningAcknowledged,
                    userRequestedGooglePlayCancel = userRequestedGooglePlayCancel
                )
            )
            if (!res.ok) throw IllegalStateException("DELETE_ACCOUNT_FAILED")

            clearLocalAuth()
            Unit
        }.recoverCatching { e ->
            if (e is HttpException && (e.code() == 401 || e.code() == 403)) {
                throw IllegalStateException("Please sign in again before deleting your account.", e)
            }

            if (e is HttpException && e.code() == 409) {
                throw IllegalStateException(
                    "Please manage your Google Play subscription, then try deleting your account again.",
                    e
                )
            }

            throw e
        }
    }

    suspend fun getDeletionPreview() = runCatching {
        api.deletionPreview()
    }

    private suspend fun clearLocalAuth() {
        tokenStore.clear()
        runCatching { profileStore.clearHasServerProfile() }
        runCatching { profileStore.clearOnboarding() }
    }
}
