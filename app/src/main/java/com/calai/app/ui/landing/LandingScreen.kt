package com.calai.app.ui.landing

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
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
import com.calai.app.i18n.LocalLocaleController
import com.calai.app.i18n.flagAndLabelFromTag
import com.calai.app.ui.common.FlagChip

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
    val composeLocale = LocalLocaleController.current

    // 不在這裡預設 EN；語言開機流程由 NavHost 統一負責
    var showLang by rememberSaveable { mutableStateOf(false) }
    var switching by rememberSaveable { mutableStateOf(false) }

    // 版面參數
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

    val titleFont = remember { FontFamily(Font(R.font.montserrat_bold)) }

    val currentTag = composeLocale.tag.ifBlank { "en" }
    val (flagEmoji, langLabel) = remember(currentTag) { flagAndLabelFromTag(currentTag) }

    val isRoot = navController.previousBackStackEntry == null
    BackHandler(enabled = isRoot) { /* stay */ }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // 右上：語言膠囊
        FlagChip(
            flag = flagEmoji,
            label = langLabel,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .windowInsetsPadding(
                    WindowInsets.displayCutout.union(WindowInsets.statusBars)
                )
                .padding(top = 0.dp, end = 20.dp)
                .offset(y = 2.dp),
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
                        modifier = Modifier.clickable { onLogin() }, // 導頁交給 Nav
                        style = LocalTextStyle.current.copy(
                            platformStyle = PlatformTextStyle(includeFontPadding = false)
                        )
                    )
                }
            }
        }

        // 語言選單
        if (showLang) {
            LanguageDialog(
                title = stringResource(R.string.choose_language),
                currentTag = composeLocale.tag.ifBlank { "en" },
                onPick = { picked ->
                    if (switching) return@LanguageDialog
                    switching = true
                    showLang = false
                    // 交給外部 onSetLocale（NavHost 會同時：更新 Compose、applyLanguage、寫 DataStore）
                    onSetLocale(picked.tag)
                    switching = false
                },
                onDismiss = { showLang = false },
                maxWidth = 320.dp
            )
        }
    }
}
