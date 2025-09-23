// app/src/main/java/com/calai/app/data/auth/GoogleAuthService.kt
package com.calai.app.data.auth

import android.content.Context
import androidx.credentials.Credential
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.calai.app.R
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** 裝置沒有任何可用的 Google 憑證（沒登入帳號或未授權） */
class NoGoogleCredentialAvailableException : Exception()

/** 取得憑證流程的通用錯誤（避免 IllegalStateException 的歧義） */
class CredentialFlowException(message: String) : Exception(message)

class GoogleAuthService(private val context: Context) {

    /** 以 Credential Manager 取 Google ID Token；若無憑證會拋 NoGoogleCredentialAvailableException */
    suspend fun getIdToken(): String = withContext(Dispatchers.Main) {
        try {
            val serverClientId = context.getString(R.string.google_web_client_id)

            val googleIdOption = GetGoogleIdOption.Builder()
                .setServerClientId(serverClientId)
                .setFilterByAuthorizedAccounts(false)
                .setAutoSelectEnabled(false)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val cm = CredentialManager.create(context)
            val response = cm.getCredential(context, request) //打到google login

            val cred: Credential = response.credential
            if (cred is CustomCredential &&
                cred.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                val tokenCred = GoogleIdTokenCredential.createFrom(cred.data)
                tokenCred.idToken ?: throw CredentialFlowException("Google 回傳空的 ID Token")
            } else {
                throw CredentialFlowException("Unexpected credential type: ${cred::class.java.simpleName}")
            }
        } catch (e: NoCredentialException) {
            throw NoGoogleCredentialAvailableException()
        } catch (e: GetCredentialCancellationException) {
            throw e // 交由上層顯示「已取消」
        } catch (e: GetCredentialException) {
            throw CredentialFlowException((e.errorMessage ?: "取得憑證失敗") as String)
        }
    }
}
