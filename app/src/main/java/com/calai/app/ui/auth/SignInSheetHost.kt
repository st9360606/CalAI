// app/src/main/java/com/calai/app/ui/auth/SignInSheetHost.kt
package com.calai.app.ui.auth

import android.app.Activity
import android.accounts.AccountManager
import android.content.Context
import android.content.ContextWrapper
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.CompositionLocalProvider
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.calai.app.R
import com.calai.app.data.auth.GoogleAuthService
import com.calai.app.data.auth.NoGoogleCredentialAvailableException
import com.calai.app.di.AppEntryPoint
import com.google.android.gms.auth.api.identity.GetSignInIntentRequest
import com.google.android.gms.auth.api.identity.Identity
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.launch

private tailrec fun Context.findActivity(): Activity? =
    when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }

private fun hasGoogleAccount(context: Context): Boolean =
    try { AccountManager.get(context).getAccountsByType("com.google").isNotEmpty() }
    catch (_: Exception) { false }

@Composable
fun SignInSheetHost(
    activity: ComponentActivity,   // 呼叫端保證提供
    localeTag: String,
    visible: Boolean,
    onDismiss: () -> Unit,
    onSignedIn: () -> Unit,
    onApple: () -> Unit = {},
    onEmail: () -> Unit = {},
    onShowError: (CharSequence) -> Unit = {}
) {
    if (!visible) return

    val ctx = LocalContext.current
    val appCtx = ctx.applicationContext

    val repo = remember(appCtx) {
        val ep = EntryPointAccessors.fromApplication(appCtx, AppEntryPoint::class.java)
        ep.authRepository()
    }

    var loading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // ★ 關鍵：把 activity 明確提供給組成樹作為 ActivityResultRegistryOwner
    CompositionLocalProvider(LocalActivityResultRegistryOwner provides activity) {

        // Compose 版本 launcher（在有 owner 的樹內註冊）
        val signInLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.StartIntentSenderForResult()
        ) { res: ActivityResult ->
            if (res.resultCode == Activity.RESULT_OK) {
                try {
                    @Suppress("DEPRECATION")
                    val credential = Identity.getSignInClient(ctx)
                        .getSignInCredentialFromIntent(res.data)
                    val idToken = credential.googleIdToken
                    if (idToken.isNullOrEmpty()) {
                        loading = false
                        onShowError("Google 回傳為空的 ID Token")
                    } else {
                        scope.launch {
                            try {
                                repo.loginWithGoogle(idToken)
                                loading = false
                                onDismiss()
                                onSignedIn()
                            } catch (e: Exception) {
                                loading = false
                                onShowError(e.message?.toString() ?: "後端驗證失敗")
                            }
                        }
                    }
                } catch (e: Exception) {
                    loading = false
                    onShowError(e.message?.toString() ?: "解析登入結果失敗")
                }
            } else {
                // 這裡多給一些訊息，方便你判斷
                loading = false
                val code = res.resultCode
                // 在許多「設定不匹配」情形下，Google 會回 RESULT_CANCELED（=0）
                val hint = buildString {
                    append("登入未完成（code=$code）")
                    append("\n請檢查：")
                    append("\n1) ids.xml 是否使用正確『Web client ID』（dev 用 dev）")
                    append("\n2) GCP 憑證是否有 Android client（package/SHA-1 與此包一致）")
                    append("\n3) Web client 與 Android client 是否在同一 GCP 專案")
                    append("\n4) 已將此 Gmail 加到 OAuth 測試使用者")
                }
                onShowError(hint)
            }
        }

        fun launchGoogleSignInIntent() {
            val serverClientId = ctx.getString(R.string.google_web_client_id)
            val req = GetSignInIntentRequest.Builder()
                .setServerClientId(serverClientId)
                .build()

            Identity.getSignInClient(activity).getSignInIntent(req)
                .addOnSuccessListener { pendingIntent ->
                    signInLauncher.launch(IntentSenderRequest.Builder(pendingIntent).build())
                }
                .addOnFailureListener { e ->
                    loading = false
                    val hint = if (hasGoogleAccount(ctx)) "" else "（裝置內可能仍未登入 Google 帳號）"
                    onShowError("啟動 Google 登入失敗：${e.message?.toString() ?: "Unknown error"}$hint")
                }
        }

        fun signInWithGoogle() {
            if (loading) return
            loading = true
            scope.launch {
                try {
                    // ① Credential Manager：若已有授權帳號會直接回 token
                    val idToken = GoogleAuthService(ctx).getIdToken()
                    repo.loginWithGoogle(idToken)
                    loading = false
                    onDismiss()
                    onSignedIn()
                } catch (e: NoGoogleCredentialAvailableException) {
                    // ② 無憑證 → 後備 Intent（會彈 Google 的登入/選帳號 UI）
                    launchGoogleSignInIntent()
                } catch (e: GetCredentialCancellationException) {
                    loading = false
                    onShowError("你已取消登入")
                } catch (e: Exception) {
                    loading = false
                    val tip = if (!hasGoogleAccount(ctx))
                        "\n建議到「設定 → 帳號」先新增 Google 帳號，再重試" else ""
                    onShowError(e.message?.toString() ?: "Google 登入失敗$tip")
                }
            }
        }

        // UI：把 onGoogle 接到流程
        SignInSheet(
            localeTag = localeTag,
            onApple = onApple,
            onGoogle = { signInWithGoogle() },
            onEmail = onEmail,
            onDismiss = onDismiss
        )
    }
}
