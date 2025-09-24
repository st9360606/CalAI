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
import com.calai.app.ui.nav.navigateToOnboardAfterLogin
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

/** 使用你提供的字串：Sign-in wasn’t completed (code=%1$d). Please try again. */
private fun fmtNotCompleted(ctx: Context, resultCode: Int): CharSequence =
    ctx.getString(R.string.err_google_not_completed_with_code, resultCode)

@Composable
fun SignInSheetHost(
    activity: ComponentActivity,   // 呼叫端保證提供
    navController: NavController,  // ★ 新增：用來直接導頁
    localeTag: String,
    visible: Boolean,
    onDismiss: () -> Unit,
    onGoogle: () -> Unit,          // 若你原本用它導頁，現在可改成 {} 或做分析事件
    onApple: () -> Unit = {},
    onEmail: () -> Unit = {},
    onShowError: (CharSequence) -> Unit = {}
) {
    if (!visible) return

    val ctx = LocalContext.current
    val appCtx = ctx.applicationContext

    // ==== 所有訊息改成可本地化字串 ====
    val msgIdTokenEmpty   = stringResource(R.string.err_google_id_token_empty)
    val msgParseFailed    = stringResource(R.string.err_google_result_parse)
    val msgCancelled      = stringResource(R.string.err_google_cancelled)
    val tipNoAccount      = stringResource(R.string.err_google_no_account_hint)
    val fmtLaunchFailed   = { extra: String -> ctx.getString(R.string.err_google_launch_failed, extra) }
    val fallbackSignInErr = stringResource(R.string.err_google_signin_failed)

    val repo = remember(appCtx) {
        val ep = EntryPointAccessors.fromApplication(appCtx, AppEntryPoint::class.java)
        ep.authRepository()
    }

    var loading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // 把 activity 提供給組成樹作為 ActivityResultRegistryOwner
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
                        onDismiss() // 空 token 也關面板
                        onShowError(msgIdTokenEmpty)
                    } else {
                        scope.launch {
                            try {
                                repo.loginWithGoogle(idToken)
                                loading = false
                                onDismiss()                 // 成功 → 關面板
                                navController.navigateToOnboardAfterLogin() // ★ 直接導到性別頁
                                onGoogle()                  // 可留作分析事件
                            } catch (e: Exception) {
                                loading = false
                                onDismiss()                 // 失敗 → 關面板
                                onShowError(e.message?.toString() ?: fallbackSignInErr)
                            }
                        }
                    }
                } catch (e: Exception) {
                    loading = false
                    onDismiss()       // 解析例外 → 關面板
                    onShowError(msgParseFailed)
                }
            } else {
                // 非 OK（例如使用者返回 / 流程被中止）
                loading = false
                onDismiss()           // ← 關面板（重點）
                onShowError(fmtNotCompleted(ctx, res.resultCode))
            }
        }

        fun launchGoogleSignInIntent() {
            val serverClientId = ctx.getString(R.string.google_web_client_id)
            val req = GetSignInIntentRequest.Builder()
                .setServerClientId(serverClientId)
                // 這裡不要呼叫 setFilterByAuthorizedAccounts，該方法不屬於此類別
                .build()

            Identity.getSignInClient(activity).getSignInIntent(req)
                .addOnSuccessListener { pendingIntent ->
                    signInLauncher.launch(IntentSenderRequest.Builder(pendingIntent).build())
                }
                .addOnFailureListener { _ ->
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
                    // ① Credential Manager：若已有授權帳號會直接回 token
                    val idToken = GoogleAuthService(ctx).getIdToken()
                    repo.loginWithGoogle(idToken)
                    loading = false
                    onDismiss()                       // 成功 → 關面板
                    navController.navigateToOnboardAfterLogin() // ★ 直接導到性別頁
                    onGoogle()                        // 可留作分析事件
                } catch (e: NoGoogleCredentialAvailableException) {
                    // ② 無憑證 → 後備 Intent（會彈 Google 登入/選帳號 UI）
                    launchGoogleSignInIntent()
                } catch (e: GetCredentialCancellationException) {
                    loading = false
                    onDismiss()   // 使用者在帳號選擇畫面按返回 → 關面板
                    onShowError(msgCancelled)
                } catch (e: Exception) {
                    loading = false
                    // 失敗保留面板讓使用者可改用 Email
                    val tip = if (!hasGoogleAccount(ctx)) "\n$tipNoAccount" else ""
                    onShowError((e.message ?: fallbackSignInErr) + tip)
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
