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
import androidx.lifecycle.lifecycleScope
import com.calai.app.R
import com.calai.app.data.auth.GoogleAuthService
import com.calai.app.data.auth.NoGoogleCredentialAvailableException
import com.calai.app.di.AppEntryPoint
import com.calai.app.i18n.LanguageSessionFlag
import com.calai.app.ui.nav.Routes
import com.google.android.gms.auth.api.identity.GetSignInIntentRequest
import com.google.android.gms.auth.api.identity.Identity
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
    // ★ 從 ROUTE_PLAN 來要把本機資料（剛填的表單）寫到伺服器
    uploadLocalOnLogin: Boolean = false,
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

    val ep = remember(appCtx) { EntryPointAccessors.fromApplication(appCtx, AppEntryPoint::class.java) }
    val repo = remember(ep) { ep.authRepository() }
    val profileRepo = remember(ep) { ep.profileRepository() }
    val store = remember(ep) { ep.userProfileStore() }

    var loading by remember { mutableStateOf(false) }
    val scope = remember(activity) { activity.lifecycleScope }

    suspend fun goHome() = withContext(Dispatchers.Main) {
        navController.navigate(Routes.HOME) {
            popUpTo(0) { inclusive = true }
            launchSingleTop = true
            restoreState = false
        }
    }
    suspend fun goGender() = withContext(Dispatchers.Main) {
        navController.navigate(Routes.ONBOARD_GENDER) {
            popUpTo(0) { inclusive = true }
            launchSingleTop = true
            restoreState = false
        }
    }

    // ★ 登入後導頁：若帶 uploadLocalOnLogin=true，無論 exists 與否都先 upsert 本機資料
    suspend fun afterLoginNavigateByServerProfile() = withContext(Dispatchers.IO) {
        val exists = runCatching { profileRepo.existsOnServer() }.getOrDefault(false)

        if (uploadLocalOnLogin) {
            // 這條代表從 ROUTE_PLAN 來：把剛在本機填完的 Onboarding 直接上傳合併
            runCatching { store.setLocaleTag(localeTag) }       // 本機保持與當下語言一致
            runCatching { profileRepo.upsertFromLocal() }       // ★ 關鍵：一定要 upsert
            runCatching { store.setHasServerProfile(true) }
            withContext(Dispatchers.Main) {
                navController.navigate(com.calai.app.ui.nav.Routes.HOME) {
                    popUpTo(com.calai.app.ui.nav.Routes.REQUIRE_SIGN_IN) { inclusive = true }
                    launchSingleTop = true
                    restoreState = false
                }
            }
            return@withContext
        }

        // 不是從 ROUTE_PLAN 來（例如 Landing 主動 Sign in）
        if (exists) {
            val changedThisSession = LanguageSessionFlag.consumeChanged()
            if (changedThisSession) runCatching { profileRepo.updateLocaleOnly(localeTag) }
            runCatching { store.setHasServerProfile(true) }
            withContext(Dispatchers.Main) {
                navController.navigate(com.calai.app.ui.nav.Routes.HOME) {
                    popUpTo(com.calai.app.ui.nav.Routes.REQUIRE_SIGN_IN) { inclusive = true }
                    launchSingleTop = true
                    restoreState = false
                }
            }
        } else {
            // 首次登入但不是從 ROUTE_PLAN 來：正常走 Onboarding
            runCatching { store.setHasServerProfile(false) }
            withContext(Dispatchers.Main) {
                navController.navigate(com.calai.app.ui.nav.Routes.ONBOARD_GENDER) {
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
                    val credential = Identity.getSignInClient(ctx).getSignInCredentialFromIntent(res.data)
                    val idToken = credential.googleIdToken
                    if (idToken.isNullOrEmpty()) {
                        loading = false
                        onShowError(msgIdTokenEmpty); onDismiss()
                    } else {
                        scope.launch {
                            try {
                                repo.loginWithGoogle(idToken)
                                afterLoginNavigateByServerProfile()
                                loading = false
                                onDismiss()
                                onGoogle()
                            } catch (e: Exception) {
                                loading = false
                                onShowError(e.message?.toString() ?: fallbackSignInErr)
                                onDismiss()
                            }
                        }
                    }
                } catch (_: Exception) {
                    loading = false
                    onShowError(msgParseFailed); onDismiss()
                }
            } else {
                loading = false
                onShowError(fmtNotCompleted(ctx, res.resultCode)); onDismiss()
            }
        }

        fun launchGoogleSignInIntent() {
            val serverClientId = ctx.getString(R.string.google_web_client_id)
            val req = GetSignInIntentRequest.Builder().setServerClientId(serverClientId).build()
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
                    afterLoginNavigateByServerProfile()
                    loading = false
                    onDismiss(); onGoogle()
                } catch (e: NoGoogleCredentialAvailableException) {
                    launchGoogleSignInIntent()
                } catch (e: GetCredentialCancellationException) {
                    loading = false
                    onShowError(msgCancelled); onDismiss()
                } catch (e: Exception) {
                    loading = false
                    val tip = if (!hasGoogleAccount(ctx)) "\n$tipNoAccount" else ""
                    onShowError((e.message ?: fallbackSignInErr) + tip); onDismiss()
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
