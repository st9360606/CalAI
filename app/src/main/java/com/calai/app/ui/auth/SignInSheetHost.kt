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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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

private fun fmtNotCompleted(ctx: Context, resultCode: Int): CharSequence =
    ctx.getString(R.string.err_google_not_completed_with_code, resultCode)

@Composable
fun SignInSheetHost(
    activity: ComponentActivity,
    navController: NavController,
    localeTag: String,                     // ← 目前 App 語言
    visible: Boolean,
    onDismiss: () -> Unit,
    onGoogle: () -> Unit,
    onApple: () -> Unit = {},
    onEmail: () -> Unit = {},
    onShowError: (CharSequence) -> Unit = {},
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

    val ep = remember(appCtx) {
        EntryPointAccessors.fromApplication(appCtx, AppEntryPoint::class.java)
    }
    val repo = remember(ep) { ep.authRepository() }
    val profileRepo = remember(ep) { ep.profileRepository() }     // ← ✅
    val store = remember(ep) { ep.userProfileStore() }            // ← ✅

    var loading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun toast(text: CharSequence) {
        Toast.makeText(ctx, text, Toast.LENGTH_SHORT).show()
    }

    CompositionLocalProvider(LocalActivityResultRegistryOwner provides activity) {

        suspend fun persistLocaleAndUpsert() {
            // 1) 先把目前語言寫回 DataStore（避免 snapshot 讀到舊值）
            withContext(Dispatchers.IO) {
                runCatching { store.setLocaleTag(localeTag) }.onFailure {
                    Log.w(TAG, "setLocaleTag failed: ${it.message}", it)
                }
            }
            // 2) 再把整包 Profile 上傳（含 locale）
            runCatching { profileRepo.upsertFromLocal() }.onFailure {
                Log.w(TAG, "upsertFromLocal failed: ${it.message}", it)
            }
        }

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
                        onDismiss()
                        onShowError(msgIdTokenEmpty)
                        toast(msgIdTokenEmpty)
                    } else {
                        scope.launch {
                            try {
                                repo.loginWithGoogle(idToken)
                                // ★ 先寫 locale、再 upsert、最後導頁
                                persistLocaleAndUpsert()
                                loading = false
                                onDismiss()
                                toast(msgLoginSuccess)
                                postLoginNavigate(navController)
                                onGoogle()
                            } catch (e: Exception) {
                                loading = false
                                onDismiss()
                                val msg = e.message?.toString() ?: fallbackSignInErr
                                onShowError(msg); toast(msg)
                            }
                        }
                    }
                } catch (e: Exception) {
                    loading = false
                    onDismiss()
                    onShowError(msgParseFailed); toast(msgParseFailed)
                }
            } else {
                loading = false
                onDismiss()
                val msg = fmtNotCompleted(ctx, res.resultCode)
                onShowError(msg); toast(msg)
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
                    val extra = if (hasGoogleAccount(ctx)) "" else "\n$tipNoAccount"
                    val msg = fmtLaunchFailed(extra)
                    onShowError(msg); toast(msg)
                }
        }

        fun signInWithGoogle() {
            if (loading) return
            loading = true
            scope.launch {
                try {
                    val idToken = GoogleAuthService(ctx).getIdToken()
                    repo.loginWithGoogle(idToken)
                    // ★ 先寫 locale、再 upsert、最後導頁
                    persistLocaleAndUpsert()
                    loading = false
                    onDismiss()
                    toast(msgLoginSuccess)
                    postLoginNavigate(navController)
                    onGoogle()
                } catch (e: NoGoogleCredentialAvailableException) {
                    launchGoogleSignInIntent()
                } catch (e: GetCredentialCancellationException) {
                    loading = false
                    onDismiss()
                    onShowError(msgCancelled); toast(msgCancelled)
                } catch (e: Exception) {
                    loading = false
                    val tip = if (!hasGoogleAccount(ctx)) "\n$tipNoAccount" else ""
                    val msg = (e.message ?: fallbackSignInErr) + tip
                    onShowError(msg); toast(msg)
                }
            }
        }

        SignInSheet(
            localeTag = localeTag,
            onApple = onApple,
            onGoogle = { signInWithGoogle() },
            onEmail = { onDismiss(); onEmail() },
            onDismiss = onDismiss
        )
    }
}
