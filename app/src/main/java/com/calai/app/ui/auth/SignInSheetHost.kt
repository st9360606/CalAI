package com.calai.app.ui.auth

import android.app.Activity
import android.accounts.AccountManager
import android.content.Context
import android.content.ContextWrapper
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.navigation.NavController
import com.calai.app.R
import com.calai.app.data.auth.GoogleAuthService
import com.calai.app.data.auth.NoGoogleCredentialAvailableException
import com.calai.app.di.AppEntryPoint
import com.calai.app.ui.nav.navigateToOnboardAfterLogin
import com.google.android.gms.auth.api.identity.GetSignInIntentRequest
import com.google.android.gms.auth.api.identity.Identity
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.launch

private const val TAG = "SignInSheetHost"

private tailrec fun Context.findActivity(): Activity? =
    when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }

private fun hasGoogleAccount(context: Context): Boolean =
    try { AccountManager.get(context).getAccountsByType("com.google").isNotEmpty() }
    catch (_: Exception) { false }

/** 使用你提供的字串：Sign-in wasn’t completed (code=%1$d). Please try again. */
private fun fmtNotCompleted(ctx: Context, resultCode: Int): CharSequence =
    ctx.getString(R.string.err_google_not_completed_with_code, resultCode)

@Composable
fun SignInSheetHost(
    activity: ComponentActivity,   // 呼叫端保證提供
    navController: NavController,
    localeTag: String,
    visible: Boolean,
    onDismiss: () -> Unit,
    onGoogle: () -> Unit,
    onApple: () -> Unit = {},
    onEmail: () -> Unit = {},
    onShowError: (CharSequence) -> Unit = {},
    // ★ 登入成功後要怎麼導頁（預設維持原本導到性別頁）
    postLoginNavigate: (NavController) -> Unit = { it.navigateToOnboardAfterLogin() }
) {
    if (!visible) return

    val ctx = LocalContext.current
    val appCtx = ctx.applicationContext

    val msgIdTokenEmpty   = stringResource(R.string.err_google_id_token_empty)
    val msgParseFailed    = stringResource(R.string.err_google_result_parse)
    val msgCancelled      = stringResource(R.string.err_google_cancelled)
    val tipNoAccount      = stringResource(R.string.err_google_no_account_hint)
    val fmtLaunchFailed   = { extra: String -> ctx.getString(R.string.err_google_launch_failed, extra) }
    val fallbackSignInErr = stringResource(R.string.err_google_signin_failed)
    val msgLoginSuccess   = stringResource(R.string.msg_login_success)

    val repo = remember(appCtx) {
        val ep = EntryPointAccessors.fromApplication(appCtx, AppEntryPoint::class.java)
        ep.authRepository()
    }

    var loading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun toast(text: CharSequence) {
        Toast.makeText(ctx, text, Toast.LENGTH_SHORT).show()
    }

    CompositionLocalProvider(LocalActivityResultRegistryOwner provides activity) {

        val signInLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.StartIntentSenderForResult()
        ) { res: ActivityResult ->
            Log.d(TAG, "GetSignInIntent resultCode=${res.resultCode}")
            if (res.resultCode == Activity.RESULT_OK) {
                try {
                    @Suppress("DEPRECATION")
                    val credential = Identity.getSignInClient(ctx)
                        .getSignInCredentialFromIntent(res.data)
                    val idToken = credential.googleIdToken
                    Log.d(TAG, "GetSignInIntent OK, idTokenIsNull=${idToken.isNullOrEmpty()}")
                    if (idToken.isNullOrEmpty()) {
                        loading = false
                        onDismiss()
                        onShowError(msgIdTokenEmpty)
                        toast(msgIdTokenEmpty)
                    } else {
                        scope.launch {
                            try {
                                repo.loginWithGoogle(idToken)
                                Log.d(TAG, "Backend login success (intent path)")
                                loading = false
                                onDismiss()
                                // ★ 先提示，再導頁（避免導頁後 Toast 被吃掉）
                                toast(msgLoginSuccess)
                                postLoginNavigate(navController)
                                onGoogle()
                            } catch (e: Exception) {
                                Log.e(TAG, "Backend login failed (intent path): ${e.message}", e)
                                loading = false
                                onDismiss()
                                val msg = e.message?.toString() ?: fallbackSignInErr
                                onShowError(msg)
                                toast(msg)
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Parse sign-in result failed", e)
                    loading = false
                    onDismiss()
                    onShowError(msgParseFailed)
                    toast(msgParseFailed)
                }
            } else {
                loading = false
                onDismiss()
                val msg = fmtNotCompleted(ctx, res.resultCode)
                Log.w(TAG, "Sign-in not completed: code=${res.resultCode}")
                onShowError(msg)
                toast(msg)
            }
        }

        fun launchGoogleSignInIntent() {
            // 若你專案已有 default_web_client_id，可改成它；否則保留 google_web_client_id 以避免資源缺失
            val serverClientId = ctx.getString(R.string.google_web_client_id)
            val req = GetSignInIntentRequest.Builder()
                .setServerClientId(serverClientId)
                .build()

            Identity.getSignInClient(activity).getSignInIntent(req)
                .addOnSuccessListener { pendingIntent ->
                    Log.d(TAG, "Launching GetSignInIntent…")
                    signInLauncher.launch(IntentSenderRequest.Builder(pendingIntent).build())
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "GetSignInIntent failed: ${e.message}", e)
                    loading = false
                    val extra = if (hasGoogleAccount(ctx)) "" else "\n$tipNoAccount"
                    val msg = fmtLaunchFailed(extra)
                    onShowError(msg)
                    toast(msg)
                }
        }

        fun signInWithGoogle() {
            if (loading) return
            loading = true
            scope.launch {
                try {
                    Log.d(TAG, "CredentialManager: start getIdToken()")
                    val idToken = GoogleAuthService(ctx).getIdToken()
                    Log.d(TAG, "CredentialManager: token acquired, posting to backend")
                    repo.loginWithGoogle(idToken)
                    Log.d(TAG, "CredentialManager: backend OK")
                    loading = false
                    onDismiss()
                    toast(msgLoginSuccess)
                    // ★ 先提示，再導頁
                    postLoginNavigate(navController)
                    onGoogle()
                } catch (e: NoGoogleCredentialAvailableException) {
                    Log.w(TAG, "No credential available, fallback to GetSignInIntent")
                    launchGoogleSignInIntent()
                } catch (e: GetCredentialCancellationException) {
                    Log.w(TAG, "User cancelled credential flow")
                    loading = false
                    onDismiss()
                    onShowError(msgCancelled)
                    toast(msgCancelled)
                } catch (e: Exception) {
                    Log.e(TAG, "Credential flow error: ${e.message}", e)
                    loading = false
                    val tip = if (!hasGoogleAccount(ctx)) "\n$tipNoAccount" else ""
                    val msg = (e.message ?: fallbackSignInErr) + tip
                    onShowError(msg)
                    toast(msg)
                }
            }
        }

        // UI：把 Google 按鈕事件串到流程；Email 先收面板再導頁
        SignInSheet(
            localeTag = localeTag,
            onApple = onApple,
            onGoogle = { signInWithGoogle() },
            onEmail = { onDismiss(); onEmail() },
            onDismiss = onDismiss
        )
    }
}
