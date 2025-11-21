package com.calai.app.ui.landing

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.calai.app.R
import com.calai.app.i18n.LanguageSessionFlag
import com.calai.app.i18n.LocalLocaleController
import com.calai.app.i18n.flagAndLabelFromTag
import com.calai.app.ui.common.FlagChip

private tailrec fun Context.findActivity(): Activity? =
    when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LandingScreen(
    hostActivity: ComponentActivity,
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
    val titleLineHeight = 34.sp
    val titleWidthFraction = 0.85f
    val spaceVideoToTitle = 15.dp

    val titleFont = remember {
        FontFamily(
            Font(R.font.montserrat_bold, weight = FontWeight.ExtraBold),
            Font(R.font.notosanstc_bold, weight = FontWeight.Bold),
            Font(R.font.notosanssc_bold, weight = FontWeight.Bold)
        )
    }

    val currentTag = composeLocale.tag.ifBlank { "en" }
    val (flagEmoji, langLabel) = remember(currentTag) { flagAndLabelFromTag(currentTag) }

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
                            .windowInsetsPadding(
                                WindowInsets.displayCutout.union(WindowInsets.statusBars)
                            )
                            .padding(top = 0.dp, end = 16.dp)
                            .offset(y = (-11).dp),
                        onClick = { if (!switching) showLang = true }
                    )
                }
            )
        },
        bottomBar = {
            LandingBottomBar(
                onStart = onStart,
                onLogin = onLogin,
                titleFont = titleFont,
                bottomOffset = 37.dp
            )
        }
    ) { inner ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(inner)
                .imePadding()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                // 你自訂的 iPhone 外框
                DeviceFrameIPhone(
                    modifier = Modifier
                        .fillMaxWidth(0.75f)
                        .aspectRatio(11f / 18.2f),
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
                    powerButtonCenterBias = 0.12f,
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
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 5.dp),
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
                maxWidth = 320.dp
            )
        }
    }
}

@Composable
private fun LandingBottomBar(
    onStart: () -> Unit,
    onLogin: () -> Unit,
    titleFont: FontFamily,
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
                .padding(start = 20.dp, end = 20.dp, bottom = bottomOffset),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Button(
                onClick = onStart,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
                shape = RoundedCornerShape(28.dp),
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

            Spacer(Modifier.height(16.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.cta_login_prefix),
                    fontSize = 15.sp,
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
                    modifier = Modifier.clickable { onLogin() },
                    style = LocalTextStyle.current.copy(
                        platformStyle = PlatformTextStyle(includeFontPadding = false)
                    )
                )
            }
        }
    }
}
