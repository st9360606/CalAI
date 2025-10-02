package com.calai.app.ui.onboarding.notifications

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import com.calai.app.R
import com.calai.app.ui.common.OnboardingProgress
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.SignalCellular4Bar
import androidx.compose.material.icons.filled.Wifi

private const val POST_NOTIFICATIONS = "android.permission.POST_NOTIFICATIONS"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationPermissionScreen(
    onBack: () -> Unit,
    onNext: () -> Unit,
    stepIndex: Int = 8,
    totalSteps: Int = 11,
    @DrawableRes appIconRes: Int = R.drawable.ic_focus_spoon_foreground
) {
    val ctx = LocalContext.current
    val activity = remember(ctx) { ctx.findActivity() as? ComponentActivity }

    var granted by remember { mutableStateOf(isNotificationsEnabled(ctx)) }
    val hasManifestDecl by remember {
        mutableStateOf(
            isPermissionInManifest(
                ctx,
                POST_NOTIFICATIONS
            )
        )
    }

    val canUseLauncher = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            hasManifestDecl && activity != null

    Scaffold(
        containerColor = Color.White,
        topBar = {
            TopAppBar(
                title = {},
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White,
                    navigationIconContentColor = Color(0xFF111114).copy(alpha = 0.85f)
                ),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Box(
                            modifier = Modifier
                                .size(39.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color(0xFFF1F3F7)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = Color(0xFF111114).copy(alpha = 0.85f)
                            )
                        }
                    }
                }
            )
        },
        bottomBar = {
            if (canUseLauncher) {
                CompositionLocalProvider(LocalActivityResultRegistryOwner provides activity!!) {
                    val launcher = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.RequestPermission()
                    ) { ok ->
                        granted = ok || isNotificationsEnabled(ctx)
                        if (granted) onNext()
                    }
                    NotifBottomBar(
                        granted = granted,
                        onClick = { if (granted) onNext() else launcher.launch(POST_NOTIFICATIONS) }
                    )
                }
            } else {
                NotifBottomBar(granted = granted, onClick = { onNext() })
            }
        }
    ) { inner ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
                .verticalScroll(rememberScrollState())
        ) {
            OnboardingProgress(
                stepIndex = stepIndex,
                totalSteps = totalSteps,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
            )

            Spacer(Modifier.height(20.dp))

            // ===== iOS 鎖屏風格 + 與外框同寬的標題/副標 =====
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
            ) {
                val panelWidth = maxWidth * 0.88f
                val panelHeight = (panelWidth * 1.27f).coerceIn(360.dp, 620.dp)

                // 用 Column 置中，所有區塊寬度統一使用 panelWidth
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // 手機外框
                    LockscreenPanel(
                        modifier = Modifier
                            .width(panelWidth)
                            .height(panelHeight),
                        bigClock = "9:41",
                        clockAlpha = 1f,
                        clockSizeSp = 124,
                        clockOffsetTop = 18.dp,
                        clockContentGap = 22.dp
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp),
                            contentAlignment = Alignment.TopCenter
                        ) {
                            NotificationCardIOS(appIconRes = appIconRes)
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    // ★ notif_title：寬度與手機外框一致、置中
                    Text(
                        text = s(R.string.notif_title, "Make every meal count"),
                        style = MaterialTheme.typography.headlineLarge.copy(fontSize = 42.sp),
                        fontWeight = FontWeight.ExtraBold,
                        lineHeight = 45.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.width(panelWidth)       // 與外框同寬
                    )

                    Spacer(Modifier.height(9.dp))

                    // ★ notif_subtitle：寬度與手機外框一致（對齊方式維持預設）
                    Text(
                        text = s(
                            R.string.notif_subtitle,
                            "Enable notifications—we’ll nudge you at the right moments so the occasional slip doesn’t set you back!"
                        ),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 16.sp,
                            lineHeight = 22.sp
                        ),
                        color = Color(0xFF8F98A3),
                        modifier = Modifier.width(panelWidth)      // 與外框同寬
                    )
                }
            }
            // ======================================

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun NotifBottomBar(
    granted: Boolean,
    onClick: () -> Unit
) {
    Box {
        Button(
            onClick = onClick,
            enabled = true,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(start = 20.dp, end = 20.dp, bottom = 59.dp)
                .fillMaxWidth()
                .height(64.dp),
            shape = RoundedCornerShape(28.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Black,
                contentColor = Color.White
            )
        ) {
            Text(
                text = if (granted) s(R.string.continue_text, "Continue")
                else s(R.string.allow_notifications_cta, "Allow Notifications"),
                fontSize = 19.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

/* -------------------- 鎖屏面板（灰白漸層＋大時間） -------------------- */
@Composable
private fun LockscreenPanel(
    modifier: Modifier = Modifier,
    bigClock: String = "9:41",
    clockAlpha: Float = 1f,
    clockSizeSp: Int = 124,
    clockColor: Color = Color(0xFFDFE3E8),
    clockOffsetTop: Dp = 0.dp,
    clockContentGap: Dp = 16.dp,
    corner: Dp = 28.dp,
    // 外框：超淺灰（可再淡：0xFFF1F3F7 或 0x1A000000）
    frameBorderColor: Color = Color(0xFFDFE3E8),
    frameBorderWidth: Dp = 6.dp,
    // ▼ 狀態列參數
    showStatusIcons: Boolean = true,
    statusTint: Color = Color(0xFFDFE3E8),  // 圖示顏色（灰）
    batteryPercent: Int = 87,               // 顯示的電量（僅做圖示/文字，不讀系統）
    content: @Composable ColumnScope.() -> Unit
) {
    Box(
        modifier = modifier
            .border(frameBorderWidth, frameBorderColor, RoundedCornerShape(corner))
            .clip(RoundedCornerShape(corner))
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFFF6F7F9), Color(0xFFF0F2F5))
                )
            )
    ) {
        // 右上角：訊號 / Wi-Fi / 電量
        if (showStatusIcons) {
            StatusBarIcons(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 10.dp, end = 12.dp),
                tint = statusTint,
                batteryPercent = batteryPercent
            )
        }

        // 主要內容（大時間 + 訊息卡）
        Column(
            modifier = Modifier
                .matchParentSize()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(clockOffsetTop))
            Text(
                text = bigClock,
                fontSize = clockSizeSp.sp,
                fontWeight = FontWeight.ExtraBold,
                color = clockColor.copy(alpha = clockAlpha)   // ★ 套用你指定的顏色
            )
            Spacer(Modifier.height(clockContentGap))
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                content = content
            )
        }
    }
}

@Composable
private fun StatusBarIcons(
    modifier: Modifier = Modifier,
    tint: Color = Color(0xFFE5E7EB),
    batteryPercent: Int = 80,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 行動網路訊號
        Icon(
            imageVector = Icons.Filled.SignalCellular4Bar,
            contentDescription = "Cellular Signal",
            tint = tint,
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(6.dp))
        // Wi-Fi
        Icon(
            imageVector = Icons.Filled.Wifi,
            contentDescription = "Wi-Fi",
            tint = tint,
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(8.dp))
        // 電量（圖示 + 百分比）
        Icon(
            imageVector = Icons.Filled.BatteryFull,
            contentDescription = "Battery",
            tint = tint,
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(2.dp))
        Text(
            text = "${batteryPercent}%",
            fontSize = 12.sp,
            color = tint,
            fontWeight = FontWeight.Medium
        )
    }
}


/* -------------------- 單一卡片：iOS 通知樣式 -------------------- */
@Composable
private fun NotificationCardIOS(
    @DrawableRes appIconRes: Int
) {
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = Color.White,
        tonalElevation = 0.dp,
        shadowElevation = 14.dp,
        modifier = Modifier
            .fillMaxWidth(0.96f)
            .height(104.dp)
    ) {
        Row(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AppIconRounded(resId = appIconRes, size = 38.dp, corner = 10.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = "AI CALORIE COUNTER",
                    fontSize = 12.sp,
                    color = Color(0xFF6B7280),
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "Got a sec to log your meal?",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF111114)
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "Take a moment to log what you had — every meal you track brings you closer to your goal.",
                    fontSize = 13.sp,
                    color = Color(0xFF6B7280),
                    maxLines = 2
                )
            }
            Spacer(Modifier.width(6.dp))
            Text("9:41", fontSize = 12.sp, color = Color(0xFF9CA3AF))
        }
    }
}

/* -------------------- 圓角矩形 icon -------------------- */
@Composable
private fun AppIconRounded(
    @DrawableRes resId: Int,
    size: Dp,
    corner: Dp = 10.dp,
    borderColor: Color = Color(0x1A000000),
    borderWidth: Dp = 0.5.dp,
    bg: Color = Color.White
) {
    val ctx = LocalContext.current
    val density = LocalDensity.current
    val innerPx = with(density) { (size - borderWidth * 2).coerceAtLeast(0.dp).toPx().toInt() }

    val bitmap: Bitmap? = remember(resId, innerPx) {
        runCatching {
            val d = ContextCompat.getDrawable(ctx, resId) ?: return@runCatching null
            d.toBitmap(innerPx, innerPx, Bitmap.Config.ARGB_8888)
        }.getOrNull()
    }

    Box(
        modifier = Modifier
            .size(size)
            .border(borderWidth, borderColor, RoundedCornerShape(corner))
            .clip(RoundedCornerShape(corner))
            .background(bg)
            .padding(borderWidth),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "App Icon",
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(corner))
            )
        } else {
            Box(
                Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(corner))
                    .background(Color(0xFF111114))
            )
        }
    }
}

/* -------------------- Utilities -------------------- */

private fun isNotificationsEnabled(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ContextCompat.checkSelfPermission(
            context,
            POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    } else {
        NotificationManagerCompat.from(context).areNotificationsEnabled()
    }
}

private fun isPermissionInManifest(context: Context, permission: String): Boolean = try {
    val pm = context.packageManager
    val pkg = context.packageName
    val info = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        pm.getPackageInfo(
            pkg,
            PackageManager.PackageInfoFlags.of(PackageManager.GET_PERMISSIONS.toLong())
        )
    } else {
        @Suppress("DEPRECATION")
        pm.getPackageInfo(pkg, PackageManager.GET_PERMISSIONS)
    }
    info.requestedPermissions?.contains(permission) == true
} catch (_: Exception) {
    false
}

@Composable
private fun s(id: Int, fallback: String): String =
    runCatching { stringResource(id) }.getOrElse { fallback }

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
