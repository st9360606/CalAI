// app/src/main/java/com/calai/app/ui/landing/LandingScreen.kt
package com.calai.app.ui.landing

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import com.calai.app.i18n.LanguageStore
import com.calai.app.i18n.LocalLocaleController
import com.calai.app.ui.VideoPlayerRaw
import kotlinx.coroutines.launch
import java.util.Locale

@Composable
fun LandingScreen(
    onStart: () -> Unit,
    onLogin: () -> Unit,
    onSetLocale: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val store = remember(context) { LanguageStore(context) }
    val composeLocale = LocalLocaleController.current

    var showLang by remember { mutableStateOf(false) }
    var switching by remember { mutableStateOf(false) }

    // ===== 可調參數（依需求微調） =====
    val phoneTopPadding = 40.dp         // 影片區塊頂部留白（原 56.dp）
    val phoneWidthFraction = 0.78f      // 影片寬比例（原 0.74f）
    val phoneAspect = 10f / 19.8f        // 影片寬高比
    val phoneCorner = 28.dp             // 影片外框圓角

    val spaceVideoToTitle = 0.dp       // ★ 影片 → 標題 距離（拉遠）
    val titleWidthFraction = 0.96f      // ★ 標題寬度（加寬）
    val titleSize = 32.sp               // ★ 標題字級（加大）
    val titleLineHeight = 30.sp         // 行高與字級一致

    val ctaWidthFraction = 0.92f
    val ctaHeight = 56.dp
    val ctaCorner = 28.dp
    val spaceTitleToCTA = 14.dp         // 標題 → CTA 距離

    // 統一字型（與標題相同）
    val titleFont = remember { FontFamily(Font(R.font.montserrat_bold)) }

    // 語系（Compose 畫面語系）
    val currentTag = composeLocale.tag.ifBlank { Locale.getDefault().toLanguageTag() }
    val currentLang = LANGS.find { it.tag.equals(currentTag, true) }
        ?: LANGS.firstOrNull { it.tag.startsWith("en", true) } ?: LANGS.first()

    Box(Modifier
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

            // ===== 影片區塊（獨立 Box，不再疊標題） =====
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(phoneWidthFraction)
                        .aspectRatio(phoneAspect)
                        .clip(RoundedCornerShape(phoneCorner)),
                    contentAlignment = Alignment.Center
                ) {
                    VideoPlayerRaw(
                        resId = R.raw.intro,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }

            // 影片 → 標題：拉遠距離
            Spacer(Modifier.height(spaceVideoToTitle))

            // ===== 標題（更寬、更大） =====
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

            // 標題 → CTA：固定距離
            Spacer(Modifier.height(spaceTitleToCTA))

            // ===== CTA 與登入（字型與標題一致、放大） =====
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
                    // 與標題同字型、加大字級
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
                        fontFamily = titleFont,               // 同字型
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
                        fontFamily = titleFont,               // 同字型
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable(onClick = onLogin),
                        style = LocalTextStyle.current.copy(
                            platformStyle = PlatformTextStyle(includeFontPadding = false)
                        )
                    )
                }
            }
        }

        if (showLang) {
            LanguageDialog(
                title = stringResource(R.string.choose_language),
                currentTag = composeLocale.tag,
                onPick = { picked ->
                    if (switching) return@LanguageDialog
                    switching = true
                    showLang = false
                    scope.launch {
                        composeLocale.set(picked.tag)
                        onSetLocale(picked.tag)
                        store.save(picked.tag)
                        switching = false
                    }
                },
                onDismiss = { showLang = false }
            )
        }
    }
}

/* ---------- 旗幟膠囊與語言縮寫 ---------- */

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
