package com.calai.app.ui.landing

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.calai.app.R
import com.calai.app.i18n.LanguageSessionFlag
import com.calai.app.i18n.LocalLocaleController
import com.calai.app.i18n.flagAndLabelFromTag
import com.calai.app.ui.common.FlagChip
import com.calai.app.ui.landing.device.DeviceFrameIPhone

private tailrec fun Context.findActivity(): Activity? =
    when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LandingScreen(
    navController: NavController,
    onStart: () -> Unit,
    onLogin: () -> Unit,
    onSetLocale: (String) -> Unit,
) {
    val composeLocale = LocalLocaleController.current
    var showLang by rememberSaveable { mutableStateOf(false) }
    var switching by rememberSaveable { mutableStateOf(false) }

    // ===== 尺寸自適應 =====
    val titleSize = 32.sp
    val titleLineHeight = 42.sp
    val titleWidthFraction = 0.85f
    val spaceVideoToTitle = 16.dp

    val currentTag = composeLocale.tag.ifBlank { "en" }
    val (flagEmoji, langLabel) = remember(currentTag) { flagAndLabelFromTag(currentTag) }

    // ★ 根據語系決定 bottomOffset
    val bottomOffset: Dp =
        if (currentTag.equals("zh-TW", ignoreCase = true) ||
            currentTag.equals("zh-CN", ignoreCase = true) ||
            currentTag.equals("zh-HK", ignoreCase = true)
        ) {
            33.dp
        } else {
            45.dp //越大越近
        }

    val isRoot = navController.previousBackStackEntry == null
    BackHandler(enabled = isRoot) { /* stay */ }

    Scaffold(
        containerColor = Color.White,
        topBar = {
            TopAppBar(
                title = {},
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White,
                    navigationIconContentColor = Color(0xFF111114)
                ),
                actions = {
                    FlagChip(
                        flag = flagEmoji,
                        label = langLabel,
                        modifier = Modifier
                            .align(Alignment.CenterVertically)
                            .padding(end = 16.dp)
                            .offset(y = (-8).dp),
                        onClick = { if (!switching) showLang = true }
                    )
                }
            )
        },
        bottomBar = {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center   // ★ 讓 bottomBar 內容水平置中
            ) {
                LandingBottomBar(
                    onStart = onStart,
                    onLogin = onLogin,
                    bottomOffset = bottomOffset   // ★ 用你動態算好的值
                )
            }
        }
    ) { inner ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                // 你自訂的 iPhone 外框
                DeviceFrameIPhone(
                    modifier = Modifier
                        .fillMaxWidth(0.67f)      // 讓整體窄一點，看起來更像真的手機
                        .aspectRatio(10.5f / 19.5f),// ★ 改成更接近真實手機的比例（寬 : 高 = 9 : 19.5）
                    islandWidthFraction = 0.32f,
                    islandHeight = 18.dp,
                    cornerRadius = 40.dp,
                    islandTopOffset = 0.dp,
                    islandStrokeWidth = 1.dp,
                    islandStrokeAlpha = 0.20f,
                    islandStrokeColor = Color.White,
                    frontCameraDotAlignRight = true,
                    frontCameraDotRightInset = 5.dp,
                    contentTopExtraPadding = 10.dp,
                    contentBottomExtraPadding = 3.dp,
                    powerButtonLengthFraction = 0.10f,
                    volumeButtonsCenterBias = 0.20f,
                    powerButtonCenterBias = 0.20f,
                    showFrontCameraDot = true
                ) {
                    LandingSlideshow(
                        modifier = Modifier.fillMaxSize(),
                        slides = listOf(
                            SlideItem(R.drawable.meal_1, contentDescription = "img_1"),
                            SlideItem(R.drawable.meal_2, contentDescription = "img_2"),
                            SlideItem(R.drawable.meal_3, contentDescription = "img_3")
                        ),
                        autoPlay = true,
                        autoPlayIntervalMs = 2800L
                    )
                }
            }

            Spacer(Modifier.height(spaceVideoToTitle))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = stringResource(R.string.landing_title),
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = titleSize,
                    lineHeight = titleLineHeight,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(titleWidthFraction),
                )
            }
        }
        if (showLang) {
            LanguageDialog(
                title = stringResource(R.string.choose_language),
                currentTag = currentTag,
                onPick = { picked ->
                    if (switching) return@LanguageDialog
                    switching = true
                    showLang = false
                    // ★ 若本次有改語言，打上 session 旗標
                    if (!picked.tag.equals(currentTag, ignoreCase = true)) {
                        LanguageSessionFlag.markChanged()
                    }
                    onSetLocale(picked.tag)
                    switching = false
                },
                onDismiss = { showLang = false },
                widthFraction = 0.92f,     // 92% 的螢幕寬
                maxHeightFraction = 0.60f  // 60% 的螢幕高
            )
        }
    }
}

@Composable
private fun LandingBottomBar(
    onStart: () -> Unit,
    onLogin: () -> Unit,
    bottomOffset: Dp,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(start = 16.dp, end = 16.dp, bottom = bottomOffset),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Button(
                onClick = onStart,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
                shape = RoundedCornerShape(999.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Black,
                    contentColor = Color.White
                )
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.cta_get_started),
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium,
                            letterSpacing = 0.2.sp
                        ),
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.cta_login_prefix),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Normal,
                    color = Color(0xFF111114),
                    style = LocalTextStyle.current.copy(
                        platformStyle = PlatformTextStyle(includeFontPadding = false)
                    )
                )

                Spacer(Modifier.width(3.dp))

                Text(
                    text = stringResource(R.string.cta_login),
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable { onLogin() },
                    style = LocalTextStyle.current.copy(
                        platformStyle = PlatformTextStyle(includeFontPadding = false)
                    )
                )
            }
        }
    }
}
