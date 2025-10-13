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
import androidx.compose.ui.res.stringResource
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.navigation.NavController
import com.calai.app.R
import com.calai.app.data.auth.GoogleAuthService
import com.calai.app.data.auth.NoGoogleCredentialAvailableException
import com.calai.app.di.AppEntryPoint
import com.calai.app.i18n.LanguageSessionFlag
import com.google.android.gms.auth.api.identity.GetSignInIntentRequest
import com.google.android.gms.auth.api.identity.Identity
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.lifecycle.lifecycleScope // ★ 新增

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
    localeTag: String,
    visible: Boolean,
    onDismiss: () -> Unit,
    onGoogle: () -> Unit,
    onApple: () -> Unit = {},
    onEmail: () -> Unit = {},
    onShowError: (CharSequence) -> Unit = {},
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

    val ep = remember(appCtx) {
        EntryPointAccessors.fromApplication(appCtx, AppEntryPoint::class.java)
    }
    val repo = remember(ep) { ep.authRepository() }
    val profileRepo = remember(ep) { ep.profileRepository() }
    val store = remember(ep) { ep.userProfileStore() }

    var loading by remember { mutableStateOf(false) }
    // ★ 用 Activity 的 lifecycleScope，避免 Sheet 關閉時取消中的網路請求
    val scope = remember(activity) { activity.lifecycleScope }

    // 登入後依伺服器是否已有 Profile 決定導頁；必要時僅更新 server 的語系
    suspend fun afterLoginNavigateByServerProfile() = withContext(Dispatchers.IO) {
        val exists = runCatching { profileRepo.existsOnServer() }.getOrDefault(false)
        if (exists) {
            val changedThisSession = LanguageSessionFlag.consumeChanged()
            if (changedThisSession) {
                runCatching { profileRepo.updateLocaleOnly(localeTag) }
            }
            runCatching { store.setHasServerProfile(true) }
            withContext(Dispatchers.Main) {
                navController.navigate(com.calai.app.ui.nav.Routes.HOME) {
                    // ★ 只彈掉 Gate，自然保留 Landing / 前一頁
                    popUpTo(com.calai.app.ui.nav.Routes.REQUIRE_SIGN_IN) { inclusive = true }
                    launchSingleTop = true
                    restoreState = false
                }
            }
        } else {
            runCatching { store.setHasServerProfile(false) }
            withContext(Dispatchers.Main) {
                navController.navigate(com.calai.app.ui.nav.Routes.ONBOARD_GENDER) {
                    // ★ 只彈掉 Gate，自然保留 Landing
                    popUpTo(com.calai.app.ui.nav.Routes.REQUIRE_SIGN_IN) { inclusive = true }
                    launchSingleTop = true
                    restoreState = false
                }
            }
        }
    }


    CompositionLocalProvider(LocalActivityResultRegistryOwner provides activity) {

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
                        onShowError(msgIdTokenEmpty)
                        onDismiss()
                    } else {
                        scope.launch {
                            try {
                                repo.loginWithGoogle(idToken)
                                // ★ 先做分流與導頁，再關閉 Sheet，避免取消 coroutine
                                afterLoginNavigateByServerProfile()
                                loading = false
                                onDismiss()
                                onGoogle()
                            } catch (e: Exception) {
                                loading = false
                                val msg = e.message?.toString() ?: fallbackSignInErr
                                onShowError(msg)
                                onDismiss()
                            }
                        }
                    }
                } catch (_: Exception) {
                    loading = false
                    onShowError(msgParseFailed)
                    onDismiss()
                }
            } else {
                loading = false
                onShowError(fmtNotCompleted(ctx, res.resultCode))
                onDismiss()
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
                .addOnFailureListener {
                    loading = false
                    val extra = if (hasGoogleAccount(ctx)) "" else "\n$tipNoAccount"
                    onShowError(fmtLaunchFailed(extra))
                }
        }

        fun signInWithGoogle() {
            if (loading) return
            loading = true
            scope.launch {
                try {
                    val idToken = GoogleAuthService(ctx).getIdToken()
                    repo.loginWithGoogle(idToken)
                    // ★ 先分流導頁，再關 Sheet
                    afterLoginNavigateByServerProfile()
                    loading = false
                    onDismiss()
                    onGoogle()
                } catch (e: NoGoogleCredentialAvailableException) {
                    // 走 One Tap / 選帳號流程
                    launchGoogleSignInIntent()
                } catch (e: GetCredentialCancellationException) {
                    loading = false
                    onShowError(msgCancelled)
                    onDismiss()
                } catch (e: Exception) {
                    loading = false
                    val tip = if (!hasGoogleAccount(ctx)) "\n$tipNoAccount" else ""
                    val msg = (e.message ?: fallbackSignInErr) + tip
                    onShowError(msg)
                    onDismiss()
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
