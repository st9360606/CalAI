package com.calai.bitecal.ui.home.ui.settings

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.EggAlt
import androidx.compose.material.icons.outlined.Opacity
import androidx.compose.material.icons.outlined.BakeryDining
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PrivacyTip
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.Widgets
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import coil.request.ImageRequest
import com.calai.bitecal.R
import com.calai.bitecal.ui.home.HomeTab
import com.calai.bitecal.ui.home.components.HomeDetailTopBar
import com.calai.bitecal.ui.home.components.LightHomeBackground
import com.calai.bitecal.ui.home.components.MainBottomBar
import com.calai.bitecal.ui.home.components.menu.HomeQuickActionMenu
import com.calai.bitecal.ui.home.components.scan.ScanFab
import com.calai.bitecal.ui.home.ui.camera.components.CameraPermissionPrefs
import com.calai.bitecal.ui.home.ui.camera.components.CameraPermissionProxyActivity
import com.calai.bitecal.ui.home.ui.camera.components.openCameraPermissionSettings
import com.calai.bitecal.ui.home.ui.membership.MembershipDisplayKind
import com.calai.bitecal.ui.home.ui.settings.dialog.DeleteAccountDialog
import com.calai.bitecal.ui.home.ui.settings.dialog.PaymentIssueDialog
import com.calai.bitecal.ui.landing.LanguageDialog
import java.util.Locale
import kotlinx.coroutines.launch

/**
 * ✅ Personal => Settings（你圖上的那個）
 * - 內容可捲動
 * - BottomBar + FAB 固定（Scaffold）
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    avatarUrl: Uri?,
    profileName: String,
    ageText: String,
    currentTab: HomeTab = HomeTab.Personal,
    onOpenTab: (HomeTab) -> Unit,
    onBack: () -> Unit = {},
    onOpenCamera: () -> Unit,
    onOpenPersonalDetails: () -> Unit = {},
    onOpenEditName: () -> Unit = {},
    onOpenAdjustMacros: () -> Unit = {},
    onOpenGoalAndCurrentWeight: () -> Unit = {},
    onOpenWeightHistory: () -> Unit = {},
    premiumStatusSubtitle: String = "Upgrade",
    premiumStatusKind: MembershipDisplayKind = MembershipDisplayKind.FREE,
    canUseScan: Boolean = false,
    onOpenSubscription: () -> Unit = {},
    onFixPaymentIssue: () -> Unit = onOpenSubscription,
    onCheckCanUseScan: suspend () -> Boolean = { canUseScan },
    onOpenSavedFoods: () -> Unit = {},
    onOpenReferral: () -> Unit = {},
    onOpenNotificationInbox: () -> Unit = {},
    currentLanguageTag: String = "",
    onLanguageSelected: (String) -> Unit = {},
    onOpenTerms: () -> Unit = {},
    onOpenPrivacy: () -> Unit = {},
    onOpenSupportEmail: () -> Unit = {},
    onDeleteAccount: () -> Unit = {},
    onLogout: () -> Unit = {}
) {
    val ctx = LocalContext.current
    val registryOwner = LocalActivityResultRegistryOwner.current
    val scope = rememberCoroutineScope()

    var showQuickAddMenu by rememberSaveable { mutableStateOf(false) }
    var scanFabGateInFlight by rememberSaveable { mutableStateOf(false) }
    var showLanguageDialog by rememberSaveable { mutableStateOf(false) }
    var languageSwitching by rememberSaveable { mutableStateOf(false) }

    val latestOnOpenCamera = rememberUpdatedState(onOpenCamera)
    val latestOnOpenSubscription = rememberUpdatedState(onOpenSubscription)
    val latestOnCheckCanUseScan = rememberUpdatedState(onCheckCanUseScan)
    val latestOnLanguageSelected = rememberUpdatedState(onLanguageSelected)
    val effectiveLanguageTag = currentLanguageTag.ifBlank {
        ctx.resources.configuration.locales[0].toLanguageTag()
            .ifBlank { Locale.getDefault().toLanguageTag() }
    }

    val requestCameraPermLauncher =
        if (registryOwner != null) {
            rememberLauncherForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { granted ->
                if (granted) {
                    CameraPermissionPrefs.resetCameraDeniedCount(ctx)
                    latestOnOpenCamera.value.invoke()
                } else {
                    CameraPermissionPrefs.incrementCameraDeniedCount(ctx)
                }
            }
        } else {
            null
        }

    fun openScanFoodWithPermissionGate() {
        val grantedNow =
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.CAMERA) ==
                    PackageManager.PERMISSION_GRANTED

        if (grantedNow) {
            CameraPermissionPrefs.resetCameraDeniedCount(ctx)
            latestOnOpenCamera.value.invoke()
            return
        }

        val deniedCount = CameraPermissionPrefs.getCameraDeniedCount(ctx)

        if (deniedCount >= 2) {
            openCameraPermissionSettings(ctx)
        } else {
            requestCameraPermLauncher?.launch(Manifest.permission.CAMERA)
                ?: CameraPermissionProxyActivity.start(ctx)
        }
    }

    fun handleScanFabClick() {
        if (scanFabGateInFlight) return

        scope.launch {
            scanFabGateInFlight = true

            val hasActiveAccess = runCatching {
                latestOnCheckCanUseScan.value.invoke()
            }.getOrDefault(false)

            scanFabGateInFlight = false

            if (hasActiveAccess) {
                showQuickAddMenu = true
            } else {
                latestOnOpenSubscription.value.invoke()
            }
        }
    }

    Box(Modifier.fillMaxSize()) { LightHomeBackground() }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            HomeDetailTopBar(
                title = stringResource(R.string.settings_title),
                onBack = onBack
            )
        },
        floatingActionButton = {
            ScanFab(
                onClick = {
                    handleScanFabClick()
                }
            )
        },
        bottomBar = { MainBottomBar(current = currentTab, onOpenTab = onOpenTab) }
    ) { inner ->
        SettingsContent(
            modifier = Modifier
                .padding(inner)
                .fillMaxSize(),
            avatarUrl = avatarUrl,
            profileName = profileName,
            ageText = ageText,
            onOpenPersonalDetails = onOpenPersonalDetails,
            onOpenEditName = onOpenEditName,
            onOpenAdjustMacros = onOpenAdjustMacros,
            onOpenGoalAndCurrentWeight = onOpenGoalAndCurrentWeight,
            onOpenWeightHistory = onOpenWeightHistory,
            premiumStatusKind = premiumStatusKind,
            premiumStatusSubtitle = premiumStatusSubtitle,
            onOpenSubscription = onOpenSubscription,
            onFixPaymentIssue = onFixPaymentIssue,
            onOpenReferral = onOpenReferral,
            onOpenNotificationInbox = onOpenNotificationInbox,
            onOpenLanguage = { if (!languageSwitching) showLanguageDialog = true },
            onOpenTerms = onOpenTerms,
            onOpenPrivacy = onOpenPrivacy,
            onOpenSupportEmail = onOpenSupportEmail,
            onDeleteAccount = onDeleteAccount,
            onLogout = onLogout
        )
    }

    HomeQuickActionMenu(
        visible = showQuickAddMenu,
        onDismiss = { showQuickAddMenu = false },
        onSavedFoodsClick = {
            showQuickAddMenu = false
            onOpenSavedFoods()
        },
        onScanFoodClick = {
            showQuickAddMenu = false
            openScanFoodWithPermissionGate()
        }
    )

    if (showLanguageDialog) {
        LanguageDialog(
            title = stringResource(R.string.choose_language),
            currentTag = effectiveLanguageTag,
            onPick = { picked ->
                if (languageSwitching) return@LanguageDialog
                languageSwitching = true
                showLanguageDialog = false
                if (!picked.tag.equals(effectiveLanguageTag, ignoreCase = true)) {
                    latestOnLanguageSelected.value(picked.tag)
                }
                languageSwitching = false
            },
            onDismiss = {
                if (!languageSwitching) showLanguageDialog = false
            },
            widthFraction = 0.92f,
            maxHeightFraction = 0.60f
        )
    }
}

@Composable
private fun SettingsContent(
    modifier: Modifier = Modifier,
    avatarUrl: Uri?,
    profileName: String,
    ageText: String,
    onOpenPersonalDetails: () -> Unit,
    onOpenEditName: () -> Unit,
    onOpenAdjustMacros: () -> Unit,
    onOpenGoalAndCurrentWeight: () -> Unit,
    onOpenWeightHistory: () -> Unit,
    premiumStatusKind: MembershipDisplayKind,
    premiumStatusSubtitle: String,
    onOpenSubscription: () -> Unit,
    onFixPaymentIssue: () -> Unit,
    onOpenReferral: () -> Unit,
    onOpenNotificationInbox: () -> Unit,
    onOpenLanguage: () -> Unit,
    onOpenTerms: () -> Unit,
    onOpenPrivacy: () -> Unit,
    onOpenSupportEmail: () -> Unit,
    onDeleteAccount: () -> Unit,
    onLogout: () -> Unit
) {
    val scroll = rememberScrollState()
    val scope = rememberCoroutineScope()

    var showDeleteDialog by remember { mutableStateOf(false) }
    var showPaymentIssueDialog by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf(false) }

    // ✅ Dialog 放外層（不受 scroll 影響）
    DeleteAccountDialog(
        visible = showDeleteDialog,
        deleting = deleting,
        onDismiss = { if (!deleting) showDeleteDialog = false },
        onCancel = { if (!deleting) showDeleteDialog = false },
        onDelete = {
            if (deleting) return@DeleteAccountDialog
            // 先鎖 UI + 關 dialog（跟你截圖體感一致：按下去就收起來）
            deleting = true
            showDeleteDialog = false
            scope.launch {
                try {
                    // ✅ 交給上層（NavHost）去做：call API + navigate landing
                    onDeleteAccount()
                } finally {
                    // ✅ 無論成功失敗都解鎖（成功通常已導頁，看不到也沒差）
                    deleting = false
                }
            }
        }
    )

    PaymentIssueDialog(
        visible = showPaymentIssueDialog,
        onDismiss = {
            showPaymentIssueDialog = false
        },
        onUpdatePaymentMethod = {
            showPaymentIssueDialog = false
            onFixPaymentIssue()
        }
    )

    Column(
        modifier = modifier
            .verticalScroll(scroll)
            .padding(horizontal = 18.dp)
            .padding(top = 6.dp, bottom = 120.dp)
    ) {
        ProfileCard(
            avatarUrl = avatarUrl,
            name = profileName,
            subtitle = ageText,
            premiumStatusKind = premiumStatusKind,
            premiumSubtitle = premiumStatusSubtitle,
            onProfileClick = onOpenEditName,
            onSubscriptionClick = onOpenSubscription,
            onPaymentIssueClick = { showPaymentIssueDialog = true }
        )

        Spacer(Modifier.height(14.dp))
        InviteFriendsCard(onClick = onOpenReferral)
        Spacer(Modifier.height(16.dp))

        SettingsListCard {
            SettingsRow(icon = Icons.Outlined.Person, title = stringResource(R.string.settings_personal_details), onClick = onOpenPersonalDetails)
            DividerThin()
            SettingsRow(icon = Icons.Outlined.Tune, title = stringResource(R.string.settings_adjust_macronutrients), onClick = onOpenAdjustMacros)
            DividerThin()
            SettingsRow(icon = Icons.Outlined.Flag, title = stringResource(R.string.settings_goal_current_weight), onClick = onOpenGoalAndCurrentWeight)
            DividerThin()
            SettingsRow(icon = Icons.Outlined.Widgets, title = stringResource(R.string.settings_weight_history), onClick = onOpenWeightHistory)
            DividerThin()
            SettingsRow(icon = Icons.Outlined.Notifications, title = stringResource(R.string.settings_inbox), onClick = onOpenNotificationInbox)
        }

        Spacer(Modifier.height(18.dp))
        PreferencesCard(onOpenLanguage = onOpenLanguage)
        Spacer(Modifier.height(18.dp))
        WidgetsSection()
        Spacer(Modifier.height(18.dp))

        SettingsListCard {
            SettingsRow(icon = Icons.Outlined.Description, title = stringResource(R.string.settings_terms_conditions), onClick = onOpenTerms)
            DividerThin()
            SettingsRow(icon = Icons.Outlined.PrivacyTip, title = stringResource(R.string.settings_privacy_policy), onClick = onOpenPrivacy)
            DividerThin()
            SettingsRow(icon = Icons.Outlined.Email, title = stringResource(R.string.settings_support_email), onClick = onOpenSupportEmail)
            DividerThin()
            SettingsRow(
                icon = Icons.Outlined.Person,
                title = stringResource(R.string.settings_delete_account),
                onClick = { if (!deleting) showDeleteDialog = true }
            )
        }

        Spacer(Modifier.height(22.dp))

        LogoutButton(onLogout = onLogout)

        Spacer(Modifier.height(20.dp))
        Text(
            text = stringResource(R.string.settings_version, "1.0.150"),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.labelMedium.copy(color = Color(0xFF9CA3AF)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 6.dp),
        )
    }
}

@Composable
private fun ProfileCard(
    avatarUrl: Uri?,
    name: String,
    subtitle: String,
    premiumStatusKind: MembershipDisplayKind,
    premiumSubtitle: String,
    onProfileClick: () -> Unit,
    onSubscriptionClick: () -> Unit,
    onPaymentIssueClick: () -> Unit
) {
    val shape = RoundedCornerShape(22.dp)
    val displayName = remember(name) { name.trim().ifBlank { "Guest" } }
    val subscriptionBadgeClickableModifier =
        when (premiumStatusKind) {
            MembershipDisplayKind.FREE -> {
                Modifier.clickable(onClick = onSubscriptionClick)
            }

            MembershipDisplayKind.PAYMENT_ISSUE -> {
                Modifier.clickable(onClick = onPaymentIssueClick)
            }

            MembershipDisplayKind.TRIAL,
            MembershipDisplayKind.PREMIUM -> {
                Modifier
            }
        }

    Card(
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(18.dp))
                    .clickable(onClick = onProfileClick),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ProfileAvatar(url = avatarUrl)

                    Spacer(Modifier.size(12.dp))

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 2.dp, end = 8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = displayName,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(weight = 1f, fill = false),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF404A58),
                                    fontSize = 17.sp,
                                    lineHeight = 20.sp
                                )
                            )

                            Spacer(Modifier.size(9.dp))

                            Box(
                                modifier = Modifier
                                    .offset(y = (-1).dp)
                                    .size(22.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFF4F4F5))
                                    .clickable(onClick = onProfileClick),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Edit,
                                    contentDescription = "Edit name",
                                    tint = Color(0xFF52525B),
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }

                        Spacer(Modifier.height(4.dp))

                        Text(
                            text = subtitle,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color(0xFF1F2937)
                            )
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .offset(y = (-2).dp)
                    .padding(start = 8.dp, end = 12.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                ProfileSubscriptionBadge(
                    kind = premiumStatusKind,
                    subtitle = premiumSubtitle,
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .then(subscriptionBadgeClickableModifier)
                        .padding(horizontal = 2.dp, vertical = 2.dp)
                )
            }
        }
    }
}

@Composable
private fun ProfileSubscriptionBadge(
    kind: MembershipDisplayKind,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    val visual = remember(kind) {
        ProfileSubscriptionVisual.from(kind)
    }

    val dotSize = 8.dp
    val dotLabelGap = 7.dp
    val horizontalPadding = 11.dp

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier
                .height(30.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(
                    brush = Brush.linearGradient(visual.backgroundColors)
                )
                .border(
                    width = 1.dp,
                    color = visual.borderColor,
                    shape = RoundedCornerShape(999.dp)
                )
                .padding(horizontal = horizontalPadding),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (visual.showDot) {
                Box(
                    modifier = Modifier
                        .size(dotSize)
                        .clip(CircleShape)
                        .background(visual.dotColor)
                )

                Spacer(Modifier.size(dotLabelGap))
            }

            Text(
                text = visual.label,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.Black,
                    color = visual.textColor,
                    fontSize = 12.sp,
                    lineHeight = 14.sp,
                    letterSpacing = 0.35.sp
                )
            )
        }

        Spacer(Modifier.height(5.dp))

        Text(
            text = subtitle.ifBlank { visual.fallbackSubtitle },
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodySmall.copy(
                color = visual.subtitleColor,
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp,
                lineHeight = 14.sp
            )
        )
    }
}

private data class ProfileSubscriptionVisual(
    val label: String,
    val fallbackSubtitle: String,
    val backgroundColors: List<Color>,
    val borderColor: Color,
    val dotColor: Color,
    val textColor: Color,
    val subtitleColor: Color,
    val showDot: Boolean = true
) {
    companion object {
        fun from(kind: MembershipDisplayKind): ProfileSubscriptionVisual {
            return when (kind) {
                MembershipDisplayKind.PAYMENT_ISSUE -> {
                    ProfileSubscriptionVisual(
                        label = "PAYMENT",
                        fallbackSubtitle = "Update payment",
                        backgroundColors = listOf(
                            Color(0xFFFFF7F7),
                            Color(0xFFFFF1F2)
                        ),
                        borderColor = Color(0xFFFFD6DD),
                        dotColor = Color(0xFFE85D75),
                        textColor = Color(0xFFA94A58),
                        subtitleColor = Color(0xFFC06F7B)
                    )
                }

                MembershipDisplayKind.PREMIUM -> {
                    ProfileSubscriptionVisual(
                        label = "PREMIUM",
                        fallbackSubtitle = "Active member",
                        backgroundColors = listOf(
                            Color(0xFF111114),
                            Color(0xFF18181B)
                        ),
                        borderColor = Color(0xFF111114),
                        dotColor = Color(0xFFE7C873),
                        textColor = Color.White,
                        subtitleColor = Color(0xFF71717A)
                    )
                }

                MembershipDisplayKind.TRIAL -> {
                    ProfileSubscriptionVisual(
                        label = "TRIAL",
                        fallbackSubtitle = "Access active",
                        backgroundColors = listOf(
                            Color(0xFFF0FDF4),
                            Color(0xFFDCFCE7)
                        ),
                        borderColor = Color(0xFFBBF7D0),
                        dotColor = Color.Transparent,
                        textColor = Color(0xFF15803D),
                        subtitleColor = Color(0xFF2F9E5E),

                        // TRIAL 不顯示 dot，讓 pill 寬度更自然，文字也更容易跟副文字置中
                        showDot = false
                    )
                }
                MembershipDisplayKind.FREE -> {
                    ProfileSubscriptionVisual(
                        label = "FREE",
                        fallbackSubtitle = "Upgrade",
                        backgroundColors = listOf(
                            Color(0xFFF4F4F5),
                            Color(0xFFEFEFF1)
                        ),
                        borderColor = Color(0xFFE4E4E7),
                        dotColor = Color.Transparent,
                        textColor = Color(0xFF3F3F46),
                        subtitleColor = Color(0xFF52525B),
                        // FREE 不是 active 狀態，不顯示 dot，避免 FREE 文字跟 Upgrade 視覺不對齊
                        showDot = false
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfileAvatar(url: Uri?) {
    val ctx = LocalContext.current

    val avatarModifier = Modifier
        .size(46.dp)
        .clip(CircleShape)
        .background(Color(0xFFF1F2F4))

    if (url == null) {
        DefaultProfileAvatarPlaceholder(
            modifier = avatarModifier
        )
        return
    }

    val req = remember(url) {
        ImageRequest.Builder(ctx)
            .data(url)
            .crossfade(false)
            .allowHardware(true)
            .build()
    }

    SubcomposeAsyncImage(
        model = req,
        contentDescription = "Avatar",
        modifier = avatarModifier,
        contentScale = ContentScale.Crop,
        loading = {
            DefaultProfileAvatarPlaceholder(
                modifier = Modifier.fillMaxSize()
            )
        },
        error = {
            DefaultProfileAvatarPlaceholder(
                modifier = Modifier.fillMaxSize()
            )
        },
        success = {
            SubcomposeAsyncImageContent()
        }
    )
}

@Composable
private fun DefaultProfileAvatarPlaceholder(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(Color(0xFFF1F2F4)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Outlined.Person,
            contentDescription = "Default avatar",
            tint = Color(0xFF111114),
            modifier = Modifier.size(25.dp)
        )
    }
}


@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
private fun InviteFriendsCard(
    onClick: () -> Unit
) {
    val outerShape = RoundedCornerShape(24.dp)
    val panelShape = RoundedCornerShape(22.dp)

    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val cardScale by animateFloatAsState(
        targetValue = if (pressed) 0.985f else 1f,
        animationSpec = spring(
            dampingRatio = 0.78f,
            stiffness = 520f
        ),
        label = "InviteFriendsCardScale"
    )

    Card(
        shape = outerShape,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier
            .fillMaxWidth()
            .scale(cardScale)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.Group,
                    contentDescription = null,
                    tint = Color(0xFF111114),
                    modifier = Modifier.size(22.dp)
                )

                Spacer(Modifier.size(8.dp))

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "Invite friends",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF111114),
                            fontSize = 17.sp,
                            lineHeight = 22.sp
                        )
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(panelShape)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF252B55),
                                Color(0xFF3A2B55),
                                Color(0xFF633A4B),
                                Color(0xFF603844)
                            )
                        )
                    )
                    .border(
                        width = 1.dp,
                        color = Color.White.copy(alpha = 0.14f),
                        shape = panelShape
                    )
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "🎁",
                                    fontSize = 19.sp,
                                    lineHeight = 23.sp
                                )

                                Spacer(Modifier.size(8.dp))

                                Text(
                                    text = "Premium reward",
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        color = Color(0xFFFFE7A3),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        lineHeight = 15.sp
                                    ),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(999.dp))
                                        .background(Color.White.copy(alpha = 0.13f))
                                        .border(
                                            width = 1.dp,
                                            color = Color.White.copy(alpha = 0.16f),
                                            shape = RoundedCornerShape(999.dp)
                                        )
                                        .padding(horizontal = 9.dp, vertical = 5.dp)
                                )
                            }

                            Spacer(Modifier.height(10.dp))

                            Text(
                                text = "Share BiteCal AI\nwith your friends",
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    fontWeight = FontWeight.Black,
                                    color = Color.White,
                                    fontSize = 22.sp,
                                    lineHeight = 26.sp
                                )
                            )
                        }

                        Spacer(Modifier.size(12.dp))

                        InviteRewardVisual(
                            modifier = Modifier.size(86.dp)
                        )
                    }

                    Spacer(Modifier.height(12.dp))

                    BoxWithConstraints(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val rewardDescFontSize = when {
                            maxWidth < 300.dp -> 10.sp
                            maxWidth < 340.dp -> 11.sp
                            else -> 12.sp
                        }

                        Text(
                            text = "They subscribe. You get 30 days free.",
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Clip,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color.White.copy(alpha = 0.84f),
                                fontWeight = FontWeight.Medium,
                                fontSize = rewardDescFontSize,
                                lineHeight = 16.sp
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(Modifier.height(14.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .background(Color.White)
                            .padding(horizontal = 18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Get 30 days free",
                            style = MaterialTheme.typography.labelLarge.copy(
                                color = Color(0xFF111114),
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        )

                        Spacer(Modifier.size(8.dp))

                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF111114)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
@Composable
private fun InviteRewardVisual(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(86.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.16f))
                .border(
                    width = 1.dp,
                    color = Color.White.copy(alpha = 0.28f),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF111114))
                    .border(
                        width = 1.dp,
                        color = Color.White.copy(alpha = 0.18f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "30",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 29.sp,
                            lineHeight = 30.sp
                        )
                    )

                    Text(
                        text = "DAYS",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = Color.White.copy(alpha = 0.92f),
                            fontWeight = FontWeight.Black,
                            fontSize = 9.sp,
                            lineHeight = 11.sp
                        )
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .size(27.dp)
                .align(Alignment.BottomEnd)
                .offset(x = 1.dp, y = 1.dp)
                .clip(CircleShape)
                .background(Color(0xFFFFE7A3))
                .border(
                    width = 1.dp,
                    color = Color.White.copy(alpha = 0.75f),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "🎁",
                fontSize = 14.sp,
                lineHeight = 16.sp
            )
        }

        Box(
            modifier = Modifier
                .size(10.dp)
                .align(Alignment.TopEnd)
                .offset(x = (-5).dp, y = 8.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.34f))
        )
    }
}

@Composable
private fun PreferencesCard(
    onOpenLanguage: () -> Unit
) {
    val appearance = stringResource(R.string.settings_appearance_light)

    SettingsListCard {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Outlined.Settings, contentDescription = null)
            Spacer(Modifier.size(10.dp))
            Text(
                text = stringResource(R.string.settings_preferences),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
            )
        }

        DividerThin()

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { /* TODO dropdown */ }
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.Palette,
                contentDescription = null,
                tint = Color(0xFF111114),
                modifier = Modifier.size(24.dp)
            )

            Spacer(Modifier.size(12.dp))

            Column(Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.settings_appearance),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                )
                Text(
                    text = stringResource(R.string.settings_appearance_description),
                    style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF9CA3AF))
                )
            }

            Text(
                text = appearance,
                style = MaterialTheme.typography.titleMedium.copy(color = Color(0xFF111114))
            )
            Text(
                text = "  ▾",
                style = MaterialTheme.typography.titleMedium.copy(color = Color(0xFF6B7280))
            )
        }

        DividerThin()

        SettingsRow(
            icon = Icons.Outlined.Language,
            title = stringResource(R.string.settings_language),
            onClick = onOpenLanguage
        )
    }
}

@Composable
private fun WidgetsSection() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Widgets",
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.Medium,
                color = Color(0xFF6B7280),
                fontSize = 24.sp,
                lineHeight = 30.sp
            )
        )
        Text(
            text = "How to add?",
            style = MaterialTheme.typography.titleMedium.copy(
                color = Color(0xFF111114),
                fontWeight = FontWeight.Medium,
                fontSize = 18.sp,
                lineHeight = 24.sp
            ),
            modifier = Modifier.clickable { /* TODO */ }
        )
    }

    Spacer(Modifier.height(16.dp))

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        CaloriesWidgetPreviewCard()
        MacroActionsWidgetPreviewCard()
    }
}

@Composable
private fun CaloriesWidgetPreviewCard(
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.size(width = 148.dp, height = 155.dp),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp)
                .padding(top = 10.dp, bottom = 12.dp)
        ) {
            WidgetCaloriesRing(
                value = "1984",
                label = "calories left",
                modifier = Modifier
                    .size(94.dp)
                    .align(Alignment.TopCenter)
            )

            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(29.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(Color(0xFF111114)),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .offset(x = (-4).dp)
                        .size(21.dp)
                        .clip(CircleShape)
                        .background(Color.White),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "+",
                        color = Color(0xFF111114),
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Normal,
                            fontSize = 19.sp,
                            lineHeight = 19.sp
                        )
                    )
                }

                Spacer(Modifier.size(2.dp))

                Text(
                    text = "Log your food",
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        lineHeight = 18.sp
                    )
                )
            }
        }
    }
}

@Composable
private fun MacroActionsWidgetPreviewCard(
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.size(width = 368.dp, height = 155.dp),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 16.dp, top = 10.dp, end = 14.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                WidgetCaloriesRing(
                    value = "1519",
                    label = "Calories left",
                    modifier = Modifier.size(108.dp)
                )

                Spacer(Modifier.size(12.dp))

                Column(
                    verticalArrangement = Arrangement.spacedBy(15.dp)
                ) {
                    WidgetMacroStatRow(
                        icon = Icons.Outlined.EggAlt,
                        iconTint = Color(0xFFE56C6C),
                        iconBackground = Color(0xFFF7F5F7),
                        value = "121g",
                        label = "Protein left"
                    )
                    WidgetMacroStatRow(
                        icon = Icons.Outlined.BakeryDining,
                        iconTint = Color(0xFFD89A62),
                        iconBackground = Color(0xFFF8F6F3),
                        value = "164g",
                        label = "Carbs left"
                    )
                    WidgetMacroStatRow(
                        icon = Icons.Outlined.Opacity,
                        iconTint = Color(0xFF6C93D8),
                        iconBackground = Color(0xFFF3F6FB),
                        value = "42g",
                        label = "Fats left"
                    )
                }
            }

            Spacer(Modifier.size(4.dp))

            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(1.dp)
                    .background(Color(0xFFF1F2F4))
            )

            Spacer(Modifier.size(20.dp))

            Column(
                modifier = Modifier.width(88.dp),
                verticalArrangement = Arrangement.spacedBy(15.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                WidgetActionTile(
                    label = "Scan Food",
                    icon = { ScanFocusGlyph() }
                )
                WidgetActionTile(
                    label = "Barcode",
                    icon = { BarcodeGlyph() }
                )
            }
        }
    }
}

@Composable
private fun WidgetCaloriesRing(
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .border(
                    width = 7.dp,
                    color = Color(0xFFEAEAED),
                    shape = CircleShape
                )
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium.copy(
                    color = Color(0xFF111114),
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp,
                    lineHeight = 26.sp
                )
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = label,
                maxLines = 1,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = Color(0xFF8D9198),
                    fontWeight = FontWeight.Medium,
                    fontSize = 10.sp,
                    lineHeight = 12.sp
                )
            )
        }
    }
}

@Composable
private fun WidgetMacroStatRow(
    icon: ImageVector,
    iconTint: Color,
    iconBackground: Color,
    value: String,
    label: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(CircleShape)
                .background(iconBackground),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(18.dp)
            )
        }

        Spacer(Modifier.size(10.dp))

        Column(
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(
                    color = Color(0xFF111114),
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    lineHeight = 15.sp
                )
            )
            Spacer(Modifier.height(1.dp))
            Text(
                text = label,
                maxLines = 1,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = Color(0xFF2F3136),
                    fontWeight = FontWeight.Medium,
                    fontSize = 10.sp,
                    lineHeight = 12.sp
                )
            )
        }
    }
}

@Composable
private fun WidgetActionTile(
    label: String,
    icon: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0xFFF7F7F8))
            .padding(vertical = 6.dp, horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(Color(0xFFF1F2F4)),
            contentAlignment = Alignment.Center
        ) {
            icon()
        }

        Spacer(Modifier.height(2.dp))

        Text(
            text = label,
            maxLines = 1,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyMedium.copy(
                color = Color(0xFF111114),
                fontWeight = FontWeight.Medium,
                fontSize = 11.sp,
                lineHeight = 13.sp
            )
        )
    }
}
@Composable
private fun ScanFocusGlyph() {
    val ink = Color(0xFF111114)

    Box(
        modifier = Modifier.size(16.dp)
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .size(width = 5.8.dp, height = 1.8.dp)
                .background(ink)
        )
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .size(width = 1.8.dp, height = 5.8.dp)
                .background(ink)
        )
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(width = 5.8.dp, height = 1.8.dp)
                .background(ink)
        )
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(width = 1.8.dp, height = 5.8.dp)
                .background(ink)
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .size(width = 5.8.dp, height = 1.8.dp)
                .background(ink)
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .size(width = 1.8.dp, height = 5.8.dp)
                .background(ink)
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .size(width = 5.8.dp, height = 1.8.dp)
                .background(ink)
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .size(width = 1.8.dp, height = 5.8.dp)
                .background(ink)
        )
        Box(
            modifier = Modifier
                .size(4.6.dp)
                .clip(CircleShape)
                .background(ink)
                .align(Alignment.Center)
        )
    }
}

@Composable
private fun BarcodeGlyph() {
    val ink = Color(0xFF111114)

    Row(
        modifier = Modifier.size(width = 18.dp, height = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(1.2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(width = 1.7.dp, height = 10.dp)
                .background(ink)
        )
        Box(
            modifier = Modifier
                .size(width = 2.2.dp, height = 14.dp)
                .background(ink)
        )
        Box(
            modifier = Modifier
                .size(width = 1.7.dp, height = 10.dp)
                .background(ink)
        )
        Box(
            modifier = Modifier
                .size(width = 2.2.dp, height = 13.dp)
                .background(ink)
        )
        Box(
            modifier = Modifier
                .size(width = 1.7.dp, height = 10.dp)
                .background(ink)
        )
        Box(
            modifier = Modifier
                .size(width = 2.2.dp, height = 14.dp)
                .background(ink)
        )
    }
}


@Composable
private fun SettingsListCard(content: @Composable () -> Unit) {
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth()
    ) { content() }
}

@Composable
private fun SettingsRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null)
        Spacer(Modifier.size(12.dp))
        Text(title, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold))
    }
}

@Composable
private fun DividerThin() {
    HorizontalDivider(color = Color(0xFFE5E7EB), thickness = 1.dp)
}

@Composable
private fun LogoutButton(onLogout: () -> Unit) {
    OutlinedButton(
        onClick = onLogout,
        // ✅ 想「完全沒外框」：直接 border = null 最乾淨
        border = null,
        // ✅ Material3：用 ButtonDefaults
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = Color.White,
            contentColor = Color(0xFF111114)
        ),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
    ) {
        Icon(Icons.AutoMirrored.Outlined.Logout, contentDescription = null)
        Spacer(Modifier.size(10.dp))
        Text(
            stringResource(R.string.settings_logout),
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
        )
    }
}
