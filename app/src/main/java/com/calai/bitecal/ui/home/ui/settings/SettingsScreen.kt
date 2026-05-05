package com.calai.bitecal.ui.home.ui.settings

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PrivacyTip
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.Widgets
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.calai.bitecal.R
import com.calai.bitecal.ui.home.HomeTab
import com.calai.bitecal.ui.home.components.LightHomeBackground
import com.calai.bitecal.ui.home.components.MainBottomBar
import com.calai.bitecal.ui.home.components.scan.ScanFab
import com.calai.bitecal.ui.home.ui.settings.delete.DeleteAccountDialog
import kotlinx.coroutines.launch
import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.style.TextOverflow
import androidx.core.content.ContextCompat
import com.calai.bitecal.ui.home.components.menu.HomeQuickActionMenu
import com.calai.bitecal.ui.home.ui.camera.components.CameraPermissionPrefs
import com.calai.bitecal.ui.home.ui.camera.components.CameraPermissionProxyActivity
import com.calai.bitecal.ui.home.ui.camera.components.openCameraPermissionSettings
import com.calai.bitecal.ui.home.ui.membership.MembershipDisplayKind

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
    onCheckCanUseScan: suspend () -> Boolean = { canUseScan },
    onOpenSavedFoods: () -> Unit = {},
    onOpenReferral: () -> Unit = {},
    onOpenNotificationInbox: () -> Unit = {},
    onOpenLanguage: () -> Unit = {},
    onOpenTerms: () -> Unit = {},
    onOpenPrivacy: () -> Unit = {},
    onOpenSupportEmail: () -> Unit = {},
    onOpenFeatureRequest: () -> Unit = {},
    onDeleteAccount: () -> Unit = {},
    onLogout: () -> Unit = {}
) {
    val ctx = LocalContext.current
    val registryOwner = LocalActivityResultRegistryOwner.current
    val scope = rememberCoroutineScope()

    var showQuickAddMenu by rememberSaveable { mutableStateOf(false) }
    var scanFabGateInFlight by rememberSaveable { mutableStateOf(false) }

    val latestOnOpenCamera = rememberUpdatedState(onOpenCamera)
    val latestOnOpenSubscription = rememberUpdatedState(onOpenSubscription)
    val latestOnCheckCanUseScan = rememberUpdatedState(onCheckCanUseScan)

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
            onOpenReferral = onOpenReferral,
            onOpenNotificationInbox = onOpenNotificationInbox,
            onOpenLanguage = onOpenLanguage,
            onOpenTerms = onOpenTerms,
            onOpenPrivacy = onOpenPrivacy,
            onOpenSupportEmail = onOpenSupportEmail,
            onOpenFeatureRequest = onOpenFeatureRequest,
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
    onOpenReferral: () -> Unit,
    onOpenNotificationInbox: () -> Unit,
    onOpenLanguage: () -> Unit,
    onOpenTerms: () -> Unit,
    onOpenPrivacy: () -> Unit,
    onOpenSupportEmail: () -> Unit,
    onOpenFeatureRequest: () -> Unit,
    onDeleteAccount: () -> Unit,
    onLogout: () -> Unit
) {
    val scroll = rememberScrollState()
    val scope = rememberCoroutineScope()

    var showDeleteDialog by remember { mutableStateOf(false) }
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

    Column(
        modifier = modifier
            .verticalScroll(scroll)
            .padding(horizontal = 20.dp)
            .padding(top = 14.dp, bottom = 120.dp)
    ) {
        Text(
            text = "Settings",
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.SemiBold),
            modifier = Modifier
                .offset(x = 7.dp, y = (-6).dp)
                .padding(vertical = 10.dp)
        )

        ProfileCard(
            avatarUrl = avatarUrl,
            name = profileName,
            subtitle = ageText,
            premiumStatusKind = premiumStatusKind,
            premiumSubtitle = premiumStatusSubtitle,
            onProfileClick = onOpenEditName,
            onSubscriptionClick = onOpenSubscription
        )

        Spacer(Modifier.height(14.dp))
        InviteFriendsCard(onClick = onOpenReferral)
        Spacer(Modifier.height(16.dp))

        SettingsListCard {
            SettingsRow(icon = Icons.Outlined.Person, title = "Personal details", onClick = onOpenPersonalDetails)
            DividerThin()
            SettingsRow(icon = Icons.Outlined.Tune, title = "Adjust macronutrients", onClick = onOpenAdjustMacros)
            DividerThin()
            SettingsRow(icon = Icons.Outlined.Flag, title = "Goal & current weight", onClick = onOpenGoalAndCurrentWeight)
            DividerThin()
            SettingsRow(icon = Icons.Outlined.Widgets, title = "Weight history", onClick = onOpenWeightHistory)
            DividerThin()
            SettingsRow(icon = Icons.Outlined.Language, title = "Language", onClick = onOpenLanguage)
            DividerThin()
            SettingsRow(icon = Icons.Outlined.Notifications, title = "Inbox", onClick = onOpenNotificationInbox)
        }

        Spacer(Modifier.height(16.dp))
        PreferencesCard()
        Spacer(Modifier.height(18.dp))
        WidgetsSection()
        Spacer(Modifier.height(18.dp))

        SettingsListCard {
            SettingsRow(icon = Icons.Outlined.Description, title = "Terms and Conditions", onClick = onOpenTerms)
            DividerThin()
            SettingsRow(icon = Icons.Outlined.PrivacyTip, title = "Privacy Policy", onClick = onOpenPrivacy)
            DividerThin()
            SettingsRow(icon = Icons.Outlined.Email, title = "Support Email", onClick = onOpenSupportEmail)
            DividerThin()
            SettingsRow(icon = Icons.Outlined.Group, title = "Feature Request", onClick = onOpenFeatureRequest)
            DividerThin()
            SettingsRow(
                icon = Icons.Outlined.Person,
                title = "Delete Account?",
                onClick = { if (!deleting) showDeleteDialog = true }
            )
        }

        Spacer(Modifier.height(16.dp))
        LogoutButton(onLogout = onLogout)

        Spacer(Modifier.height(12.dp))
        Text(
            text = "VERSION 1.0.150",
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
    onSubscriptionClick: () -> Unit
) {
    val shape = RoundedCornerShape(22.dp)
    val subscriptionBadgeClickableModifier =
        if (premiumStatusKind == MembershipDisplayKind.FREE) {
            Modifier.clickable(onClick = onSubscriptionClick)
        } else {
            Modifier
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
                .padding(16.dp),
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

                    Spacer(Modifier.size(16.dp))

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 2.dp, end = 8.dp)
                            .offset(y = (-1).dp)
                    ) {
                        Text(
                            text = name,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF404A58),
                                fontSize = 17.sp,
                                lineHeight = 22.sp
                            )
                        )

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
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center
            ) {
                ProfileSubscriptionBadge(
                    kind = premiumStatusKind,
                    subtitle = premiumSubtitle,
                    modifier = Modifier
                        .offset(x = 35.dp)
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

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .background(
                    brush = Brush.linearGradient(visual.backgroundColors)
                )
                .border(
                    width = 1.dp,
                    color = visual.borderColor,
                    shape = RoundedCornerShape(999.dp)
                )
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(visual.dotColor)
            )

            Spacer(Modifier.size(6.dp))

            Text(
                text = visual.label,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.Black,
                    color = visual.textColor,
                    fontSize = 11.sp,
                    lineHeight = 13.sp,
                    letterSpacing = 0.3.sp
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
                fontSize = 11.sp,
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
    val subtitleColor: Color
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
                        dotColor = Color(0xFF16A34A),
                        textColor = Color(0xFF15803D),
                        subtitleColor = Color(0xFF2F9E5E)
                    )
                }

                MembershipDisplayKind.FREE -> {
                    ProfileSubscriptionVisual(
                        label = "FREE",
                        fallbackSubtitle = "Upgrade",
                        backgroundColors = listOf(
                            Color.White,
                            Color.White
                        ),
                        borderColor = Color(0xFFD4D4D8),
                        dotColor = Color(0xFFA1A1AA),
                        textColor = Color(0xFF3F3F46),
                        subtitleColor = Color(0xFF71717A)
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfileAvatar(url: Uri?) {
    val ctx = LocalContext.current
    val modifier = Modifier.size(46.dp).clip(CircleShape)

    if (url == null) {
        AsyncImage(
            model = ImageRequest.Builder(ctx).data(R.drawable.profile).build(),
            contentDescription = "avatar_default",
            modifier = modifier,
            contentScale = ContentScale.Crop
        )
    } else {
        val req = remember(url) {
            ImageRequest.Builder(ctx)
                .data(url)                 // ✅ url 在這裡一定 non-null
                .crossfade(false)
                .allowHardware(true)
                .build()
        }
        AsyncImage(
            model = req,
            contentDescription = "avatar",
            modifier = modifier,
            contentScale = ContentScale.Crop,
            error = androidx.compose.ui.res.painterResource(R.drawable.profile)
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
private fun PreferencesCard() {
    var appearance by remember { mutableStateOf("Light") }
    var addBurned by remember { mutableStateOf(false) }
    var rollover by remember { mutableStateOf(true) }

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
                text = "Preferences",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
            )
        }

        DividerThin()

        // Appearance
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { /* TODO dropdown */ }
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text("Appearance", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold))
                Text(
                    "Choose light, dark, or system appearance",
                    style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF9CA3AF))
                )
            }
            Text(
                text = appearance,
                style = MaterialTheme.typography.titleMedium.copy(color = Color(0xFF111114))
            )
            Text(text = "  ▾", style = MaterialTheme.typography.titleMedium.copy(color = Color(0xFF6B7280)))
        }

        DividerThin()

        // Add burned calories
        ToggleRow(
            title = "Add Burned Calories",
            subtitle = "Add burned calories to daily goal",
            checked = addBurned,
            onCheckedChange = { addBurned = it }
        )

        DividerThin()

        ToggleRow(
            title = "Rollover calories",
            subtitle = "Add up to 200 left over calories from yesterday into\ntoday's daily goal",
            checked = rollover,
            onCheckedChange = { rollover = it }
        )
    }
}

@Composable
private fun WidgetsSection() {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Widgets", style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold))
        Text(
            "How to add?",
            style = MaterialTheme.typography.titleMedium.copy(color = Color(0xFF111114)),
            modifier = Modifier.clickable { /* TODO */ }
        )
    }

    Spacer(Modifier.height(14.dp))

    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        WidgetCardLeft(modifier = Modifier.weight(1f))
        WidgetCardRight(modifier = Modifier.weight(1f))
    }
}

@Composable
private fun WidgetCardLeft(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            RingMock(value = "1429", label = "Calories left")
            Spacer(Modifier.height(12.dp))

            // 黑色 pill + 左側白圈加號
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(999.dp))
                    .background(Color(0xFF111114))
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(Color.White),
                    contentAlignment = Alignment.Center
                ) {
                    Text("+", color = Color(0xFF111114), style = MaterialTheme.typography.titleMedium)
                }
                Spacer(Modifier.size(10.dp))
                Text(
                    "Log your food",
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                )
            }
        }
    }
}

@Composable
private fun WidgetCardRight(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                RingMock(value = "1429", label = "Calories left")
            }
            Spacer(Modifier.size(10.dp))
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                MacroMiniRow("117g", "Protein left")
                MacroMiniRow("152g", "Carbs left")
                MacroMiniRow("40g", "Fats left")
            }
        }
    }
}

/** 這裡用「假環」先把 1:1 版型做出來；後續要接你真 macro ring 直接替換即可 */
@Composable
private fun RingMock(value: String, label: String) {
    Box(
        modifier = Modifier
            .size(110.dp)
            .clip(CircleShape)
            .background(Color(0xFFF3F4F6)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold))
            Text(label, style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF9CA3AF)))
        }
    }
}

@Composable
private fun MacroMiniRow(value: String, label: String) {
    Column {
        Text(value, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
        Text(label, style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF9CA3AF)))
    }
}

@Composable
private fun ToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold))
            Text(subtitle, style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF9CA3AF)))
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedTrackColor = Color(0xFF111114),
                uncheckedTrackColor = Color(0xFFE5E7EB)
            )
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
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(Icons.AutoMirrored.Outlined.Logout, contentDescription = null)
        Spacer(Modifier.size(10.dp))
        Text(
            "Logout",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
        )
    }
}
