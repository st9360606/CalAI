// app/src/main/java/com/calai/app/ui/landing/LandingScreen.kt
package com.calai.app.ui.landing

import LandingVideo
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
import com.calai.app.R
import com.calai.app.i18n.LanguageManager
import com.calai.app.i18n.LanguageStore
import com.calai.app.i18n.LocalLocaleController
import com.calai.app.i18n.currentLocaleKey
import com.calai.app.ui.auth.SignInSheetHost
import kotlinx.coroutines.launch
import java.util.Locale

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
    onStart: () -> Unit,
    onLogin: () -> Unit,
    onSetLocale: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val store = remember(context) { LanguageStore(context) }
    val composeLocale = LocalLocaleController.current

    // ✅ 用 rememberSaveable 保存 UI 狀態
    var showLang by rememberSaveable { mutableStateOf(false) }
    var switching by rememberSaveable { mutableStateOf(false) }
    var showSignInSheet by rememberSaveable { mutableStateOf(false) }

    // ===== 可調參數 =====
    val phoneTopPadding = 40.dp
    val phoneWidthFraction = 0.78f
    val phoneAspect = 10f / 19.8f
    val phoneCorner = 28.dp

    val spaceVideoToTitle = 0.dp
    val titleWidthFraction = 0.96f
    val titleSize = 30.sp
    val titleLineHeight = 30.sp

    val ctaWidthFraction = 0.92f
    val ctaHeight = 56.dp
    val ctaCorner = 28.dp
    val spaceTitleToCTA = 14.dp

    // 統一字型
    val titleFont = remember { FontFamily(Font(R.font.montserrat_bold)) }

    // 語系（Compose 畫面語系）
    val currentTag = composeLocale.tag.ifBlank { Locale.getDefault().toLanguageTag() }
    val currentLang = LANGS.find { it.tag.equals(currentTag, true) }
        ?: LANGS.firstOrNull { it.tag.startsWith("en", true) } ?: LANGS.first()

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // 右上：旗幟膠囊
        FlagChip(
            flag = currentLang.flag,
            label = langShortLabelFromTag(currentTag),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
        ) { if (!switching) showLang = true }

        Column(Modifier.fillMaxSize()) {
            Spacer(Modifier.height(phoneTopPadding))

            // ===== 影片 =====
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                LandingVideo(
                    modifier = Modifier
                        .fillMaxWidth(phoneWidthFraction)
                        .aspectRatio(phoneAspect)
                        .clip(RoundedCornerShape(phoneCorner)),
                    resId = R.raw.intro
                )
            }

            Spacer(Modifier.height(spaceVideoToTitle))

            // ===== 標題 =====
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

            // ===== CTA 與登入 =====
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
                        .height(ctaHeight),
                    shape = RoundedCornerShape(ctaCorner),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Black,
                        contentColor = Color.White
                    )
                ) {
                    Text(
                        text = stringResource(R.string.cta_get_started),
                        fontSize = 18.sp,
                        fontFamily = titleFont,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(Modifier.height(12.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(R.string.cta_login_prefix),
                        fontSize = 17.sp,
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

        // ===== 語言對話框 =====
        if (showLang) {
            LanguageDialog(
                title = stringResource(R.string.choose_language),
                currentTag = composeLocale.tag.ifBlank {
                    java.util.Locale.getDefault().toLanguageTag()
                },
                onPick = { picked ->
                    if (switching) return@LanguageDialog
                    switching = true
                    showLang = false
                    scope.launch {
                        // 1) Compose 層立即套
                        composeLocale.set(picked.tag)
                        // 2) 全域（非 Compose）也切
                        LanguageManager.applyLanguage(picked.tag)
                        // 3) 外部回呼
                        onSetLocale(picked.tag)
                        // 4) 保存
                        store.save(picked.tag)
                        switching = false
                    }
                },
                onDismiss = { showLang = false },
                maxWidth = 320.dp
            )
        }

        /* === 返回鍵：面板開啟時先關面板 === */
        BackHandler(enabled = showSignInSheet) {
            showSignInSheet = false
        }

        // ===== 登入底部面板 =====
        if (showSignInSheet) {
            val localeKey = currentLocaleKey() // 由資源實際語系產生 key

            key(localeKey) {
                SignInSheetHost(
                    activity = hostActivity,
                    localeTag = composeLocale.tag.ifBlank { Locale.getDefault().toLanguageTag() },
                    visible = true,
                    onDismiss = { showSignInSheet = false },
                    // 成功登入（Google）時：先關面板，再提示/導頁
                    onGoogle = {
                        showSignInSheet = false
                        Toast.makeText(context, "登入成功", Toast.LENGTH_SHORT).show()
                        // TODO: 導頁
                    },
                    onApple = {
                        showSignInSheet = false
                        // TODO: 之後支援 Apple
                    },
                    // ★ 這裡是 Email 入口：先關面板，再導航到 Email 輸入頁
                    onEmail = {
                        showSignInSheet = false
                        onLogin()
                    },
                    onShowError = { msg ->
                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }
    }
}

/* ---------- 旗幟膠囊與語言縮寫（原樣保留） ---------- */
@Composable
private fun FlagChip(
    flag: String,
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(22.dp))
            .clickable(onClick = onClick),
        color = Color(0xFFF2F2F2),
        shadowElevation = 0.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = flag, fontSize = 20.sp)
            Spacer(Modifier.width(8.dp))
            Text(
                text = label,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF111114)
            )
        }
    }
}

private fun langShortLabelFromTag(tag: String): String = when {
    tag.startsWith("zh", true) -> "CH"
    tag.startsWith("en", true) -> "EN"
    tag.startsWith("es", true) -> "ES"
    tag.startsWith("ar", true) -> "AR"
    tag.startsWith("bn", true) -> "BN"
    tag.startsWith("pt", true) -> "PT"
    tag.startsWith("ru", true) -> "RU"
    tag.startsWith("ja", true) -> "JP"
    tag.startsWith("de", true) -> "DE"
    tag.startsWith("pa", true) -> "PA"
    tag.startsWith("jv", true) -> "JV"
    tag.startsWith("fr", true) -> "FR"
    tag.startsWith("vi", true) -> "VI"
    tag.startsWith("th", true) -> "TH"
    tag.startsWith("ms", true) -> "MS"
    tag.startsWith("ko", true) -> "KR"
    tag.startsWith("id", true) -> "ID"
    else -> tag.take(2).uppercase()
}
