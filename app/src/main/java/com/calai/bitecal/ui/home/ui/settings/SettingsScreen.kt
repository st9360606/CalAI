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
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PrivacyTip
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.Widgets
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.compose.ui.platform.LocalContext
import com.calai.bitecal.R
import com.calai.bitecal.ui.home.HomeTab
import com.calai.bitecal.ui.home.components.LightHomeBackground
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.unit.sp
import com.calai.bitecal.ui.home.components.MainBottomBar
import com.calai.bitecal.ui.home.components.scan.ScanFab
import com.calai.bitecal.ui.home.ui.settings.delete.DeleteAccountDialog
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
    onOpenCamera: () -> Unit,
    onOpenPersonalDetails: () -> Unit = {},
    onOpenEditName: () -> Unit = {},
    onOpenAdjustMacros: () -> Unit = {},
    onOpenGoalAndCurrentWeight: () -> Unit = {},
    onOpenWeightHistory: () -> Unit = {},
    onOpenLanguage: () -> Unit = {},
    onOpenTerms: () -> Unit = {},
    onOpenPrivacy: () -> Unit = {},
    onOpenSupportEmail: () -> Unit = {},
    onOpenFeatureRequest: () -> Unit = {},
    onDeleteAccount: () -> Unit = {},
    onLogout: () -> Unit = {}
) {
    Box(Modifier.fillMaxSize()) { LightHomeBackground() }

    Scaffold(
        containerColor = Color.Transparent,
        floatingActionButton = { ScanFab(onClick = onOpenCamera) },
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
            onOpenLanguage = onOpenLanguage,
            onOpenTerms = onOpenTerms,
            onOpenPrivacy = onOpenPrivacy,
            onOpenSupportEmail = onOpenSupportEmail,
            onOpenFeatureRequest = onOpenFeatureRequest,
            onDeleteAccount = onDeleteAccount,
            onLogout = onLogout
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
            onClick = onOpenEditName
        )

        Spacer(Modifier.height(14.dp))
        InviteFriendsCard()
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
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(22.dp)
    Card(
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ProfileAvatar(url = avatarUrl)

            // ✅ 1) 讓名字/年齡整體往右一點：把間距加大
            Spacer(Modifier.size(16.dp)) // 原本 12.dp

            Column(
                modifier = Modifier
                    .padding(start = 2.dp)
                    .offset(y = (-1).dp) // ✅ 名字+年齡整組往上 1dp（想更明顯就 -2.dp）
            ) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF404A58),
                        fontSize = 17.sp,     // ✅ 你要的「用 size 控制」
                        lineHeight = 22.sp    // ✅ 順便控制行高，排版更穩
                    )
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color(0xFF1F2937)
                    )
                )
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


@Composable
private fun InviteFriendsCard() {
    val shape = RoundedCornerShape(22.dp)
    Card(
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Group, contentDescription = null)
                Spacer(Modifier.size(10.dp))
                Text(
                    text = "Invite friends",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                )
            }

            Spacer(Modifier.height(12.dp))

            // ✅ Banner：我先用「純色漸層 + 文案」佔位，避免直接拷貝別人照片素材（上架也比較安全）
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(2.2f)
                    .clip(RoundedCornerShape(18.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(Color(0xFF111114), Color(0xFF36363A))
                        )
                    )
                    .padding(16.dp)
            ) {
                Column(
                    verticalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Column {
                        Text(
                            text = "The journey\nis easier together.",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                    }

                    Button(
                        onClick = { /* TODO: referral flow */ },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White,
                            contentColor = Color(0xFF111114)
                        ),
                        shape = RoundedCornerShape(999.dp)
                    ) {
                        Text("Refer a friend to earn $10")
                    }
                }
            }
        }
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
