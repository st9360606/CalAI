package com.calai.bitecal.ui.landing

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.calai.bitecal.i18n.LanguageSessionFlag
import com.calai.bitecal.i18n.LocalLocaleController
import com.calai.bitecal.i18n.flagAndLabelFromTag
import com.calai.bitecal.ui.landing.device.DeviceFrameIPhone
import androidx.compose.ui.platform.LocalContext
import com.calai.bitecal.R
import com.calai.bitecal.ui.common.haptic.biteCalClickable
import com.calai.bitecal.ui.common.design.BiteCalLandingLanguageTopBar
import com.calai.bitecal.ui.common.design.BiteCalOnboardingBottomContainer
import com.calai.bitecal.ui.common.design.BiteCalPrimaryButton
import com.calai.bitecal.ui.common.design.BiteCalSize
import com.calai.bitecal.ui.common.design.BiteCalSpacing
import com.calai.bitecal.ui.common.design.BiteCalTextStyles

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LandingScreen(
    navController: NavController,
    onStart: () -> Unit,
    onLogin: () -> Unit,
    onSetLocale: (String) -> Unit,
) {
    val composeLocale = LocalLocaleController.current
    val context = LocalContext.current
    var showLang by rememberSaveable { mutableStateOf(false) }
    var switching by rememberSaveable { mutableStateOf(false) }

    // ===== 尺寸自適應 =====
    val titleSize = 32.sp
    val titleLineHeight = 42.sp
    val titleWidthFraction = 0.85f
    val spaceVideoToTitle = 14.dp

    // 以系統語系當 fallback，不要硬塞 "en"
    val systemTag = remember(context) {
        context.resources.configuration.locales[0].toLanguageTag()
    }
    val currentTag = composeLocale.tag.ifBlank { systemTag }
    val (flagEmoji, langLabel) = remember(currentTag) { flagAndLabelFromTag(currentTag) }

    val isRoot = navController.previousBackStackEntry == null
    BackHandler(enabled = isRoot) { /* stay */ }

    Scaffold(
        containerColor = Color.White,
        topBar = {
            BiteCalLandingLanguageTopBar(
                flag = flagEmoji,
                label = langLabel,
                onClick = { if (!switching) showLang = true },
                modifier = Modifier.offset(y = (-2).dp)
            )
        },
        bottomBar = {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center   // ★ 讓 bottomBar 內容水平置中
            ) {
                LandingBottomBar(
                    onStart = onStart,
                    onLogin = onLogin
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

            Spacer(Modifier.height(6.dp))

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
) {
    BiteCalOnboardingBottomContainer(
        hasSecondaryAction = true
    ) {
        BiteCalPrimaryButton(
            text = stringResource(R.string.cta_get_started),
            onClick = onStart,
            height = BiteCalSize.primaryButtonHeight
        )

        Spacer(Modifier.height(BiteCalSpacing.bottomButtonToSecondary))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(BiteCalSize.secondaryTextButtonHeight)
                .biteCalClickable { onLogin() },
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.cta_login_prefix),
                style = BiteCalTextStyles.secondaryButton().copy(
                    fontSize = 16.sp,
                    lineHeight = 16.sp,
                    fontWeight = FontWeight.Normal,
                    color = Color(0xFF111114)
                ),
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.width(5.dp))

            Text(
                text = stringResource(R.string.cta_login),
                style = BiteCalTextStyles.secondaryButton().copy(
                    fontSize = 16.sp,
                    lineHeight = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                ),
                textAlign = TextAlign.Center
            )
        }
    }
}
