package com.calai.bitecal.ui.auth

import android.accounts.AccountManager
import android.content.Context
import androidx.activity.ComponentActivity
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.navigation.NavController
import androidx.lifecycle.lifecycleScope
import com.calai.bitecal.data.auth.GoogleAuthService
import com.calai.bitecal.di.AppEntryPoint
import com.calai.bitecal.i18n.LanguageSessionFlag
import com.calai.bitecal.ui.nav.Routes
import com.calai.bitecal.ui.nav.resolveOnboardingDestination
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.calai.bitecal.R
import com.calai.bitecal.data.onboarding.repo.OnboardingRepository

private fun hasGoogleAccount(context: Context): Boolean =
    try { AccountManager.get(context).getAccountsByType("com.google").isNotEmpty() }
    catch (_: Exception) { false }

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
    uploadLocalOnLogin: Boolean = false,
    allowHomeAfterOnboardingPaywallRejected: Boolean = false,
) {
    if (!visible) return

    val ctx = LocalContext.current
    val appCtx = ctx.applicationContext

    val tipNoAccount      = stringResource(R.string.err_google_no_account_hint)
    val fallbackSignInErr = stringResource(R.string.err_google_signin_failed)

    val ep = remember(appCtx) { EntryPointAccessors.fromApplication(appCtx, AppEntryPoint::class.java) }
    val repo = remember(ep) { ep.authRepository() }
    val profileRepo = remember(ep) { ep.profileRepository() }
    val weightRepo = remember(ep) { ep.weightRepository() }
    val store = remember(ep) { ep.userProfileStore() }
    val entitlementSyncer = remember(ep) { ep.entitlementSyncer() }
    val onboardingRepo: OnboardingRepository = remember(ep) {
        ep.onboardingRepository()
    }
    var loading by remember { mutableStateOf(false) }
    val scope = remember(activity) { activity.lifecycleScope }

    // ★ 登入後導頁：若帶 uploadLocalOnLogin=true，無論 exists 與否都先 upsert 本機資料
    suspend fun afterLoginNavigateByServerProfile() = withContext(Dispatchers.IO) {
        if (!uploadLocalOnLogin) {
            // 刪帳後重新登入時，只刷新後端既有權益摘要。
            // Google Play active purchase token 必須等使用者按 Restore Subscription 後才送 /sync。
            runCatching { entitlementSyncer.refreshServerEntitlementSummaryOnly() }
        }

        val exists = runCatching { profileRepo.existsOnServer() }.getOrDefault(false)

        if (uploadLocalOnLogin) {
            runCatching { store.setLocaleTag(localeTag) }
            runCatching { profileRepo.upsertFromLocalForOnboarding() }
            runCatching { store.setHasServerProfile(true) }
            runCatching { weightRepo.ensureBaseline() }

            // SignIn 後依照後端 bootstrap 決定下一頁；App 不自行猜 referral eligibility。
            val destination = resolveOnboardingDestination(
                entitlementSyncer = entitlementSyncer,
                onboardingRepository = onboardingRepo,
                allowHomeAfterRejectedPaywall = allowHomeAfterOnboardingPaywallRejected,
            )

            withContext(Dispatchers.Main) {
                navController.navigate(destination) {
                    popUpTo(Routes.REQUIRE_SIGN_IN_ROUTE) {
                        inclusive = true
                    }
                    launchSingleTop = true
                    restoreState = false
                }
            }
            return@withContext
        }

        if (exists) {
            val changedThisSession = LanguageSessionFlag.consumeChanged()
            if (changedThisSession) {
                runCatching { profileRepo.updateLocaleOnly(localeTag) }
            }

            runCatching { store.setHasServerProfile(true) }

            withContext(Dispatchers.Main) {
                navController.navigate(Routes.HOME) {
                    popUpTo(Routes.REQUIRE_SIGN_IN_ROUTE) {
                        inclusive = true
                    }
                    launchSingleTop = true
                    restoreState = false
                }
            }
        } else {
            runCatching { store.setHasServerProfile(false) }

            withContext(Dispatchers.Main) {
                navController.navigate(Routes.ONBOARD_GENDER) {
                    popUpTo(Routes.REQUIRE_SIGN_IN_ROUTE) {
                        inclusive = true
                    }
                    launchSingleTop = true
                    restoreState = false
                }
            }
        }
    }

    fun signInWithGoogle() {
        if (loading) return
        loading = true
        scope.launch {
            try {
                // 1. 使用現代化的 Credential Manager 取得 Token
                val idToken = GoogleAuthService(ctx).getIdToken()

                // 2. 伺服器登入
                repo.loginWithGoogle(idToken)

                // 3. 根據 Profile / Entitlement 導頁。
                // afterLoginNavigateByServerProfile() 會先同步 restore Google Play 訂閱，再做導頁判斷。

                // 4. 根據 Profile / Entitlement 導頁
                afterLoginNavigateByServerProfile()

                loading = false
                onDismiss()
                onGoogle()
            } catch (e: GetCredentialCancellationException) {
                // 使用者按取消，不報錯
                loading = false
            } catch (e: Exception) {
                loading = false
                val tip = if (!hasGoogleAccount(ctx)) "\n$tipNoAccount" else ""
                onShowError((e.message ?: fallbackSignInErr) + tip)
                // 注意：如果發生錯誤，通常不強制 onDismiss() 讓使用者有機會重試
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
