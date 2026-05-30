package com.calai.bitecal.ui.common.design

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.calai.bitecal.ui.common.haptic.biteCalClickable
import com.calai.bitecal.ui.common.haptic.rememberClickWithHaptic
import androidx.compose.foundation.layout.imePadding
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.IconButton
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.ui.res.stringResource
import com.calai.bitecal.R
import com.calai.bitecal.ui.common.FlagChip
import com.calai.bitecal.ui.common.OnboardingProgress

/**
 * BiteCal UI design tokens.
 *
 * Keep cross-screen spacing, colors, typography and button/top-bar sizing here so future
 * light/dark mode changes do not require editing every screen one by one.
 */
@Immutable
data class BiteCalColorTokens(
    val background: Color,
    val surface: Color,
    val surfaceMuted: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val border: Color,
    val topBarBackBackground: Color,
    val primaryButtonContainer: Color,
    val primaryButtonContent: Color,
    val secondaryButtonContent: Color,
    val disabledButtonContainer: Color,
    val disabledButtonContent: Color,
    val error: Color,
)

object BiteCalColors {
    val Light = BiteCalColorTokens(
        background = Color.White,
        surface = Color.White,
        surfaceMuted = Color(0xFFF8FAFC),
        textPrimary = Color(0xFF111114),
        textSecondary = Color(0xFF52525B),
        textMuted = Color(0xFF8F98A3),
        border = Color(0xFFE5E7EB),
        topBarBackBackground = Color(0xFFF1F3F7),
        primaryButtonContainer = Color.Black,
        primaryButtonContent = Color.White,
        secondaryButtonContent = Color(0xFF8F98A3),
        disabledButtonContainer = Color(0xFFDADBE2),
        disabledButtonContent = Color.White,
        error = Color(0xFFCC3D3D),
    )

    val Dark = BiteCalColorTokens(
        background = Color(0xFF09090B),
        surface = Color(0xFF111114),
        surfaceMuted = Color(0xFF18181B),
        textPrimary = Color(0xFFF8FAFC),
        textSecondary = Color(0xFFD4D4D8),
        textMuted = Color(0xFFA1A1AA),
        border = Color(0xFF27272A),
        topBarBackBackground = Color(0xFF27272A),
        primaryButtonContainer = Color.White,
        primaryButtonContent = Color.Black,
        secondaryButtonContent = Color(0xFFA1A1AA),
        disabledButtonContainer = Color(0xFF3F3F46),
        disabledButtonContent = Color(0xFFA1A1AA),
        error = Color(0xFFFF7A7A),
    )

    @Composable
    fun current(): BiteCalColorTokens {
        return if (MaterialTheme.colorScheme.background.luminanceCompat() < 0.5f) Dark else Light
    }
}

private fun Color.luminanceCompat(): Float {
    return (0.299f * red + 0.587f * green + 0.114f * blue)
}

object BiteCalSpacing {
    val screenHorizontal: Dp = 20.dp
    val screenHorizontalWide: Dp = 24.dp
    val contentHorizontal: Dp = 20.dp
    val topBarHorizontal: Dp = 16.dp
    val topBarTop: Dp = 16.dp
    val topBarBottom: Dp = 22.dp
    val bottomBarHorizontal: Dp = 20.dp
    val bottomBarBottom: Dp = 40.dp
    val bottomBarBottomCompact: Dp = 8.dp
    val bottomButtonToSecondary: Dp = 8.dp
    val cardPadding: Dp = 16.dp
    val sectionGap: Dp = 16.dp

    // Legacy-compatible names used while migrating existing screens into the design system.
    val TopBarEndPadding: Dp = 8.dp
    val ContentWideHorizontal: Dp = screenHorizontalWide
    val ContentVertical: Dp = BiteCalScreenFrame.detailBottom
    val ContentTitleHorizontal: Dp = BiteCalScreenFrame.contentHorizontalMedium
    val BottomHorizontal: Dp = bottomBarHorizontal
    val BottomSingleAction: Dp = bottomBarBottom
    val BottomWithSecondaryAction: Dp = bottomBarBottomCompact
    val BottomGap: Dp = bottomButtonToSecondary
    val OnboardingHorizontal: Dp = screenHorizontal
    val OnboardingTitleHorizontal: Dp = 18.dp
}


object BiteCalScreenFrame {
    /** Standard horizontal distance from the phone edge for full-screen content. */
    val contentHorizontalCompact: Dp = 16.dp
    val contentHorizontalMedium: Dp = 18.dp
    val contentHorizontal: Dp = BiteCalSpacing.screenHorizontal
    val contentHorizontalComfort: Dp = 22.dp
    val contentHorizontalWide: Dp = BiteCalSpacing.screenHorizontalWide
    val contentHorizontalLarge: Dp = 28.dp
    val contentHorizontalExtraWide: Dp = 30.dp
    val contentHorizontalPaywall: Dp = 32.dp
    val contentHorizontalHero: Dp = 36.dp

    /** Default vertical breathing room for content directly under a Scaffold topBar. */
    val contentVertical: Dp = 16.dp
    val contentTopTiny: Dp = 5.dp
    val contentTopSmall: Dp = 8.dp
    val contentTop: Dp = 12.dp
    val contentBottomLarge: Dp = 24.dp

    /** Onboarding title and description horizontal padding. */
    val onboardingTitleHorizontal: Dp = contentHorizontalMedium
    val onboardingSubtitleHorizontal: Dp = 48.dp

    /** Main onboarding content horizontal distance. */
    val onboardingHorizontal: Dp = BiteCalSpacing.screenHorizontal
    val onboardingWideHorizontal: Dp = contentHorizontalExtraWide

    /** Home/feed content distances. */
    val homeHorizontal: Dp = BiteCalSpacing.screenHorizontal
    val homeTop: Dp = 12.dp
    val homeBottom: Dp = 0.dp

    /** Settings/detail screens. */
    val detailHorizontal: Dp = BiteCalSpacing.screenHorizontal
    val detailHorizontalWide: Dp = contentHorizontalComfort
    val detailVertical: Dp = 16.dp
    val detailTop: Dp = 14.dp
    val detailBottom: Dp = 20.dp
    val settingsHorizontal: Dp = contentHorizontalMedium
    val settingsTop: Dp = 6.dp
    val settingsBottom: Dp = 50.dp

    /** Bottom CTA content frame values. */
    val bottomActionSingle: Dp = BiteCalSpacing.bottomBarBottom

    /** Content that intentionally needs more side margin, such as referral code forms. */
    val formHorizontalWide: Dp = contentHorizontalExtraWide
    val authTitleVertical: Dp = 15.dp
}


object BiteCalScreenSpacing {
    val TopBarHorizontal: Dp = BiteCalSpacing.topBarHorizontal
    val TopBarEndPadding: Dp = 8.dp
    val ContentHorizontal: Dp = BiteCalScreenFrame.contentHorizontal
    val ContentWideHorizontal: Dp = BiteCalScreenFrame.contentHorizontalWide
    val ContentVertical: Dp = BiteCalScreenFrame.detailBottom
    val ContentTitleHorizontal: Dp = BiteCalScreenFrame.contentHorizontalMedium
    val BottomHorizontal: Dp = BiteCalScreenFrame.contentHorizontal
    val BottomSingleAction: Dp = BiteCalSpacing.bottomBarBottom
    val BottomWithSecondaryAction: Dp = BiteCalSpacing.bottomBarBottomCompact
    val BottomGap: Dp = BiteCalSpacing.bottomButtonToSecondary
    val PrimaryButtonHeight: Dp = BiteCalSize.primaryButtonHeight
    val SecondaryButtonHeight: Dp = BiteCalSize.secondaryTextButtonHeight
    val ButtonCorner: Dp = BiteCalSize.pillCorner
    val BackButtonSize: Dp = BiteCalSize.backButton
    val BackButtonCorner: Dp = 20.dp
    val BackButtonBackground: Color = BiteCalColors.Light.topBarBackBackground
    val PrimaryText: Color = BiteCalColors.Light.textPrimary
    val SecondaryActionText: Color = BiteCalColors.Light.secondaryButtonContent
}

object BiteCalSize {
    val topBarHeight: Dp = 64.dp
    val profileTopBarHeight: Dp = 36.dp
    val backButton: Dp = 36.dp
    val backButtonCompact: Dp = 36.dp
    val primaryButtonHeight: Dp = 68.dp
    val primaryButtonHeightCompact: Dp = 55.dp
    val secondaryTextButtonHeight: Dp = 44.dp
    val pillCorner: Dp = 999.dp
}

object BiteCalShape {
    val backButton = RoundedCornerShape(20.dp)
    val button = RoundedCornerShape(BiteCalSize.pillCorner)
    val card = RoundedCornerShape(28.dp)
}

object BiteCalTextStyles {
    @Composable
    fun topBarTitle(): TextStyle = MaterialTheme.typography.headlineSmall.copy(
        fontWeight = FontWeight.Bold,
        color = BiteCalColors.current().textPrimary,
    )

    @Composable
    fun screenTitle(): TextStyle = MaterialTheme.typography.headlineLarge.copy(
        fontSize = 34.sp,
        lineHeight = 41.sp,
        fontWeight = FontWeight.ExtraBold,
        color = BiteCalColors.current().textPrimary,
    )

    @Composable
    fun body(): TextStyle = MaterialTheme.typography.bodyLarge.copy(
        fontSize = 17.sp,
        lineHeight = 24.sp,
        fontWeight = FontWeight.Normal,
        color = BiteCalColors.current().textSecondary,
    )

    @Composable
    fun primaryButton(): TextStyle = MaterialTheme.typography.bodyLarge.copy(
        fontSize = 19.sp,
        lineHeight = 23.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.2.sp,
    )

    @Composable
    fun secondaryButton(): TextStyle = MaterialTheme.typography.bodyLarge.copy(
        fontSize = 17.sp,
        lineHeight = 20.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.1.sp,
    )
}

@Composable
fun BiteCalBackButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    contentDescription: String = "Back",
) {
    val colors = BiteCalColors.current()
    Box(
        modifier = modifier
            .size(BiteCalSize.backButton)
            .clip(BiteCalShape.backButton)
            .background(colors.topBarBackBackground)
            .biteCalClickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = contentDescription,
            tint = colors.textPrimary,
            modifier = Modifier.size(24.dp),
        )
    }
}

@Composable
fun BiteCalCompactBackButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    contentDescription: String = "Back",
) {
    val colors = BiteCalColors.current()
    Box(
        modifier = modifier
            .size(BiteCalSize.backButtonCompact)
            .clip(CircleShape)
            .background(colors.topBarBackBackground)
            .biteCalClickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = contentDescription,
            tint = colors.textPrimary,
            modifier = Modifier.size(24.dp),
        )
    }
}

@Composable
fun BiteCalTopBar(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    actions: @Composable BoxScope.() -> Unit = {},
) {
    val colors = BiteCalColors.current()
    Box(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(
                start = BiteCalSpacing.topBarHorizontal,
                top = BiteCalSpacing.topBarTop,
                end = BiteCalSpacing.topBarHorizontal,
                bottom = BiteCalSpacing.topBarBottom,
            )
            .height(BiteCalSize.profileTopBarHeight),
    ) {
        BiteCalCompactBackButton(
            onClick = onBack,
            modifier = Modifier.align(Alignment.CenterStart),
        )

        Text(
            text = title,
            modifier = Modifier.align(Alignment.Center),
            style = BiteCalTextStyles.topBarTitle(),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = colors.textPrimary,
        )

        Box(
            modifier = Modifier.align(Alignment.CenterEnd),
            content = actions,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BiteCalProgressTopBar(
    stepIndex: Int,
    totalSteps: Int,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    progress: @Composable RowScope.() -> Unit,
    actions: @Composable RowScope.() -> Unit = {},
) {
    TopAppBar(
        modifier = modifier.padding(start = BiteCalSpacing.topBarHorizontal, end = BiteCalSpacing.topBarHorizontal),
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = BiteCalColors.current().background,
            navigationIconContentColor = BiteCalColors.current().textPrimary,
        ),
        navigationIcon = { BiteCalBackButton(onClick = onBack) },
        title = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(end = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                content = progress,
            )
        },
        actions = actions,
    )
}

@Composable
fun BiteCalPrimaryButton(
    text: String,
    enabled: Boolean = true,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    loading: Boolean = false,
    height: Dp = BiteCalSize.primaryButtonHeight,
    containerColor: Color? = null,
    contentColor: Color? = null,
    textStyle: TextStyle? = null,
) {
    val colors = BiteCalColors.current()
    val resolvedContainerColor = containerColor ?: colors.primaryButtonContainer
    val resolvedContentColor = contentColor ?: colors.primaryButtonContent
    val hapticClick = rememberClickWithHaptic(enabled = enabled && !loading, onClick = onClick)
    Button(
        onClick = hapticClick,
        enabled = enabled && !loading,
        modifier = modifier
            .fillMaxWidth()
            .height(height),
        shape = BiteCalShape.button,
        colors = ButtonDefaults.buttonColors(
            containerColor = resolvedContainerColor,
            contentColor = resolvedContentColor,
            disabledContainerColor = if (enabled) resolvedContainerColor else colors.disabledButtonContainer,
            disabledContentColor = if (enabled) resolvedContentColor else colors.disabledButtonContent,
        ),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 0.dp),
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                strokeWidth = 2.dp,
                color = resolvedContentColor,
            )
        } else {
            Text(
                text = text,
                style = textStyle ?: BiteCalTextStyles.primaryButton(),
                color = resolvedContentColor,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
fun BiteCalBottomActionBar(
    primaryText: String,
    onPrimaryClick: () -> Unit,
    modifier: Modifier = Modifier,
    primaryEnabled: Boolean = true,
    primaryLoading: Boolean = false,
    secondaryText: String? = null,
    onSecondaryClick: (() -> Unit)? = null,
    secondaryEnabled: Boolean = true,
    compactBottomPadding: Boolean = false,
    useImePadding: Boolean = false,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .then(if (useImePadding) Modifier.imePadding() else Modifier)
            .navigationBarsPadding()
            .padding(
                start = BiteCalSpacing.bottomBarHorizontal,
                end = BiteCalSpacing.bottomBarHorizontal,
                bottom = if (compactBottomPadding) BiteCalSpacing.bottomBarBottomCompact else BiteCalSpacing.bottomBarBottom,
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        BiteCalPrimaryButton(
            text = primaryText,
            enabled = primaryEnabled,
            loading = primaryLoading,
            onClick = onPrimaryClick,
        )

        if (secondaryText != null && onSecondaryClick != null) {
            Spacer(Modifier.height(BiteCalSpacing.bottomButtonToSecondary))
            TextButton(
                onClick = rememberClickWithHaptic(enabled = secondaryEnabled && !primaryLoading, onClick = onSecondaryClick),
                enabled = secondaryEnabled && !primaryLoading,
                modifier = Modifier.height(BiteCalSize.secondaryTextButtonHeight),
                colors = ButtonDefaults.textButtonColors(
                    contentColor = BiteCalColors.current().secondaryButtonContent,
                    disabledContentColor = BiteCalColors.current().secondaryButtonContent.copy(alpha = 0.45f),
                ),
            ) {
                Text(
                    text = secondaryText,
                    style = BiteCalTextStyles.secondaryButton(),
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
fun BiteCalScreenSurface(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier.background(BiteCalColors.current().background),
        content = content,
    )
}

@Composable
fun BiteCalScreenColumn(
    modifier: Modifier = Modifier,
    horizontalPadding: Dp = BiteCalSpacing.screenHorizontal,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = horizontalPadding),
        content = content,
    )
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BiteCalOnboardingTopBar(
    stepIndex: Int,
    totalSteps: Int,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    showBack: Boolean = true,
    actions: @Composable RowScope.() -> Unit = {},
) {
    TopAppBar(
        modifier = modifier.padding(
            start = BiteCalSpacing.topBarHorizontal,
            end = BiteCalSpacing.topBarHorizontal,
        ),
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = BiteCalColors.current().background,
            navigationIconContentColor = BiteCalColors.current().textPrimary,
        ),
        navigationIcon = {
            if (showBack) {
                BiteCalBackButton(onClick = onBack)
            }
        },
        title = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(end = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OnboardingProgress(
                    stepIndex = stepIndex,
                    totalSteps = totalSteps,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        actions = actions,
    )
}

@Composable
fun BiteCalOnboardingPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
) {
    BiteCalPrimaryButton(
        text = text,
        enabled = enabled,
        loading = loading,
        onClick = onClick,
        modifier = modifier,
    )
}

@Composable
fun BiteCalOnboardingSecondaryTextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    TextButton(
        onClick = rememberClickWithHaptic(enabled = enabled, onClick = onClick),
        enabled = enabled,
        modifier = modifier.height(BiteCalSize.secondaryTextButtonHeight),
        colors = ButtonDefaults.textButtonColors(
            contentColor = BiteCalColors.current().secondaryButtonContent,
            disabledContentColor = BiteCalColors.current().secondaryButtonContent.copy(alpha = 0.45f),
        ),
    ) {
        Text(
            text = text,
            style = BiteCalTextStyles.secondaryButton(),
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
fun BiteCalOnboardingBottomContainer(
    modifier: Modifier = Modifier,
    hasSecondaryAction: Boolean = false,
    useImePadding: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .then(if (useImePadding) Modifier.imePadding() else Modifier)
            .navigationBarsPadding()
            .padding(
                start = BiteCalSpacing.bottomBarHorizontal,
                end = BiteCalSpacing.bottomBarHorizontal,
                bottom = if (hasSecondaryAction) BiteCalSpacing.bottomBarBottomCompact else BiteCalSpacing.bottomBarBottom,
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        content = content,
    )
}

@Composable
fun BiteCalOnboardingBottomBar(
    primaryText: String,
    onPrimaryClick: () -> Unit,
    modifier: Modifier = Modifier,
    primaryEnabled: Boolean = true,
    primaryLoading: Boolean = false,
    compactBottom: Boolean = false,
    useImePadding: Boolean = false,
    secondaryText: String? = null,
    onSecondaryClick: (() -> Unit)? = null,
    secondaryEnabled: Boolean = true,
) {
    BiteCalBottomActionBar(
        primaryText = primaryText,
        onPrimaryClick = onPrimaryClick,
        modifier = modifier,
        primaryEnabled = primaryEnabled,
        primaryLoading = primaryLoading,
        secondaryText = secondaryText,
        onSecondaryClick = onSecondaryClick,
        secondaryEnabled = secondaryEnabled,
        compactBottomPadding = compactBottom || (secondaryText != null && onSecondaryClick != null),
        useImePadding = useImePadding,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BiteCalPlainBackTopBar(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    TopAppBar(
        modifier = modifier.padding(
            start = BiteCalSpacing.topBarHorizontal,
            end = BiteCalSpacing.topBarHorizontal,
        ),
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = BiteCalColors.current().background,
            navigationIconContentColor = BiteCalColors.current().textPrimary,
            titleContentColor = BiteCalColors.current().textPrimary,
        ),
        navigationIcon = {
            BiteCalBackButton(
                onClick = onBack,
                contentDescription = stringResource(R.string.common_back)
            )
        },
        title = {}
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BiteCalLandingLanguageTopBar(
    flag: String,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TopAppBar(
        modifier = modifier,
        title = {},
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = BiteCalColors.current().background,
            navigationIconContentColor = BiteCalColors.current().textPrimary,
        ),
        actions = {
            Box(
                modifier = Modifier
                    .padding(end = BiteCalSpacing.topBarHorizontal),
                contentAlignment = Alignment.Center,
            ) {
                FlagChip(
                    flag = flag,
                    label = label,
                    onClick = onClick,
                )
            }
        },
    )
}
