package com.calai.app.ui.landing

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.calai.app.R
import com.calai.app.i18n.LanguageManager
import com.calai.app.i18n.LanguageStore
import com.calai.app.i18n.LocalLocaleController
import com.calai.app.i18n.currentLocaleKey
import com.calai.app.i18n.flagAndLabelFromTag
import com.calai.app.ui.auth.SignInSheetHost
import com.calai.app.ui.common.FlagChip
import kotlinx.coroutines.launch
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarHost
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
// --- 安全往上溯源找 Activity（避免 Context 不是 Activity 的情況） ---
private tailrec fun Context.findActivity(): Activity? =
    when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }

@Composable
fun LandingScreen(
    hostActivity: ComponentActivity,
    navController: NavController,
    onStart: () -> Unit,
    onLogin: () -> Unit,
    onSetLocale: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val store = remember(context) { LanguageStore(context) }
    val composeLocale = LocalLocaleController.current

    // ✅ 首次啟動或尚未選過語言時，預設成 EN
    LaunchedEffect(Unit) {
        if (composeLocale.tag.isBlank()) {
            val def = "en"
            composeLocale.set(def)               // Compose 層
            LanguageManager.applyLanguage(def)   // 非 Compose 層
            onSetLocale(def)                     // 外部回呼（若有需要）
            store.save(def)                      // 記住下次打開仍為 EN
        }
    }

    // ✅ 用 rememberSaveable 保存 UI 狀態
    var showLang by rememberSaveable { mutableStateOf(false) }
    var switching by rememberSaveable { mutableStateOf(false) }
    var showSignInSheet by rememberSaveable { mutableStateOf(false) }

    // ★ 新增：SnackbarHostState
    val snackbarHostState = remember { SnackbarHostState() }


    // ===== 可調參數（已縮小影片框，放大語言膠囊）=====
    val phoneTopPadding = 118.dp
    val phoneWidthFraction = 0.81f
    val phoneAspect = 11f / 16.5f
    val phoneCorner = 28.dp

    val spaceVideoToTitle = 21.dp
    val titleWidthFraction = 0.96f
    val titleSize = 31.sp
    val titleLineHeight = 31.sp

    val ctaWidthFraction = 0.92f
    val spaceTitleToCTA = 18.dp

    // 統一字型
    val titleFont = remember { FontFamily(Font(R.font.montserrat_bold)) }

    // 語系（Compose 畫面語系）→ 旗幟＋短標籤（預設 EN）
    val currentTag = composeLocale.tag.ifBlank { "en" }
    val (flagEmoji, langLabel) = remember(currentTag) { flagAndLabelFromTag(currentTag) }

    // 根頁 Back 保護：在 Landing（沒有上一頁）時，按返回「不做事」
    val isRoot = navController.previousBackStackEntry == null
    BackHandler(enabled = !showSignInSheet && isRoot) { /* stay */ }
    // 面板開啟時按返回關面板
    BackHandler(enabled = showSignInSheet) { showSignInSheet = false }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // 右上：旗幟膠囊
        FlagChip(
            flag = flagEmoji,
            label = langLabel,
            modifier = Modifier
                .align(Alignment.TopEnd)

                .windowInsetsPadding(
                    WindowInsets.displayCutout.union(WindowInsets.statusBars)
                )
                .padding(top = 0.dp, end = 20.dp)
                .offset(y = (2).dp),
        ) { if (!switching) showLang = true }

        Column(Modifier.fillMaxSize()) {
            Spacer(Modifier.height(phoneTopPadding))

            // 影片
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                LandingVideo(
                    modifier = Modifier
                        .fillMaxWidth(phoneWidthFraction)
                        .aspectRatio(phoneAspect)
                        .clip(RoundedCornerShape(phoneCorner)),
                    resId = R.raw.intro,
                    posterResId = null,
                    placeholderColor = Color.White
                )
            }

            Spacer(Modifier.height(spaceVideoToTitle))

            // 標題
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = stringResource(R.string.landing_title),
                    fontSize = titleSize,
                    lineHeight = titleLineHeight,
                    fontFamily = titleFont,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF111114),
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth(titleWidthFraction),
                    style = LocalTextStyle.current.copy(
                        platformStyle = PlatformTextStyle(includeFontPadding = false),
                        lineHeightStyle = LineHeightStyle(
                            alignment = LineHeightStyle.Alignment.Center,
                            trim = LineHeightStyle.Trim.Both
                        )
                    )
                )
            }

            Spacer(Modifier.height(spaceTitleToCTA))

            // CTA 與登入
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Button(
                    onClick = onStart,
                    modifier = Modifier
                        .fillMaxWidth(ctaWidthFraction)
                        .height(64.dp)
                        .clip(RoundedCornerShape(28.dp)),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Black,
                        contentColor = Color.White
                    )
                ) {
                    Text(
                        text = stringResource(R.string.cta_get_started),
                        fontSize = 19.sp,
                        fontFamily = titleFont,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(Modifier.height(18.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(R.string.cta_login_prefix),
                        fontSize = 16.sp,
                        fontFamily = titleFont,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF111114).copy(alpha = 0.72f),
                        style = LocalTextStyle.current.copy(
                            platformStyle = PlatformTextStyle(includeFontPadding = false)
                        )
                    )
                    Spacer(Modifier.width(9.dp))
                    Text(
                        text = stringResource(R.string.cta_login),
                        fontSize = 17.sp,
                        fontFamily = titleFont,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable { showSignInSheet = true },
                        style = LocalTextStyle.current.copy(
                            platformStyle = PlatformTextStyle(includeFontPadding = false)
                        )
                    )
                }
            }
        }

        // ===== 語言對話框（預設 EN）=====
        if (showLang) {
            LanguageDialog(
                title = stringResource(R.string.choose_language),
                currentTag = composeLocale.tag.ifBlank { "en" },
                onPick = { picked ->
                    if (switching) return@LanguageDialog
                    switching = true
                    showLang = false
                    scope.launch {
                        composeLocale.set(picked.tag)
                        LanguageManager.applyLanguage(picked.tag)
                        onSetLocale(picked.tag)
                        store.save(picked.tag)
                        switching = false
                    }
                },
                onDismiss = { showLang = false },
                maxWidth = 320.dp
            )
        }

        // 登入底部面板
        if (showSignInSheet) {
            val localeKey = currentLocaleKey()
            key(localeKey) {
                SignInSheetHost(
                    activity = hostActivity,
                    navController = navController,
                    localeTag = composeLocale.tag.ifBlank { "en" },
                    visible = true,
                    onDismiss = { showSignInSheet = false },
                    // ★ 成功：Snackbar（替換原本 Toast）
                    onGoogle = {
                        showSignInSheet = false
                        scope.launch {
                            snackbarHostState.showSnackbar(
                                context.getString(R.string.msg_login_success)
                            )
                        }
                    },
                    onApple = { showSignInSheet = false },
                    onEmail = {
                        showSignInSheet = false
                        onLogin()
                    },
                    // ★ 失敗：Snackbar（替換原本 Toast）
                    onShowError = { msg ->
                        scope.launch { snackbarHostState.showSnackbar(msg.toString()) }
                    }
                )
            }
        }
    }
}
