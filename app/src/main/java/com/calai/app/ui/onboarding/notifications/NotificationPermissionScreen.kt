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
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.SignalCellular4Bar
import androidx.compose.material.icons.filled.Wifi
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import com.calai.app.R
import com.calai.app.ui.common.OnboardingProgress

private const val POST_NOTIFICATIONS = "android.permission.POST_NOTIFICATIONS"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationPermissionScreen(
    onBack: () -> Unit,
    onNext: () -> Unit,
    @DrawableRes appIconRes: Int = R.drawable.ic_focus_spoon_foreground
) {
    val ctx = LocalContext.current
    val activity = remember(ctx) { ctx.findActivity() as? ComponentActivity }

    var granted by remember { mutableStateOf(isNotificationsEnabled(ctx)) }
    val hasManifestDecl by remember {
        mutableStateOf(isPermissionInManifest(ctx, POST_NOTIFICATIONS))
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
                CompositionLocalProvider(LocalActivityResultRegistryOwner provides activity) {
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
                stepIndex = 9,
                totalSteps = 11,
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
                val panelWidth = maxWidth * 0.84f
                val panelHeight = (panelWidth * 1.20f).coerceIn(360.dp, 620.dp)

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    LockscreenPanel(
                        modifier = Modifier
                            .width(panelWidth)
                            .height(panelHeight),
                        bigClock = "8:30",
                        clockAlpha = 1f,
                        clockSizeSp = 110,
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

                    Spacer(Modifier.height(20.dp))

                    val titleStyle = MaterialTheme.typography.headlineLarge.copy(
                        fontSize = 42.sp,          // 你的標題字級
                        fontWeight = FontWeight.ExtraBold,
                        lineHeight = 45.sp
                    )

                    // 用 Inline 版本，讓圖示跟著文字排版
                    NotifTitleWithEndImageInline(
                        text = s(R.string.notif_title, "Make every meal count"),
                        tailRes = R.drawable.notifications,         // 你的彩色鈴鐺
                        modifier = Modifier.fillMaxWidth(0.82f),
                        tailSizeSp = titleStyle.fontSize,       // ★ 圖示大小=標題字級 → 幾乎一樣大
                        tailSpaceEm = 1.1f,                    // 與文字距離，想更緊可改 0.15f
                        textAlign = TextAlign.Start,
                        style = titleStyle
                    )

                    Spacer(Modifier.height(8.dp))

                    // 副標：與手機外框同寬（保持原樣）
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
                        modifier = Modifier.fillMaxWidth(0.80f),
                    )
                }
            }
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
                .padding(start = 20.dp, end = 20.dp, bottom = 75.dp)
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
    bigClock: String = "8:30",
    clockAlpha: Float = 1f,
    clockSizeSp: Int = 124,
    clockColor: Color = Color(0xFFD0D6DD),
    clockOffsetTop: Dp = 0.dp,
    clockContentGap: Dp = 16.dp,
    corner: Dp = 28.dp,
    frameBorderColor: Color = Color(0xFFD0D6DD),
    frameBorderWidth: Dp = 6.dp,
    showStatusIcons: Boolean = true,
    statusTint: Color = Color(0xFFD0D6DD),
    batteryPercent: Int = 87,
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
        if (showStatusIcons) {
            StatusBarIcons(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 10.dp, end = 12.dp),
                tint = statusTint,
                batteryPercent = batteryPercent
            )
        }

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
                color = clockColor.copy(alpha = clockAlpha)
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
        Icon(
            imageVector = Icons.Filled.SignalCellular4Bar,
            contentDescription = "Cellular Signal",
            tint = tint,
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(6.dp))
        Icon(
            imageVector = Icons.Filled.Wifi,
            contentDescription = "Wi-Fi",
            tint = tint,
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(8.dp))
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
    val shape = RoundedCornerShape(22.dp)
    val strokeColor = Color(0xFFB8C0CC) // 想再深可調 0xFFA5ADBA / 0xFF94A3B8
    Surface(
        shape = shape,
        color = Color.White,
        tonalElevation = 0.dp,
        shadowElevation = 14.dp,
        border = BorderStroke(0.dp, strokeColor),   // ← 用 Surface 的 border，圓角跟 shape 一致
        modifier = Modifier
            .fillMaxWidth(0.98f)
            .height(90.dp)
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
                    text = "CALORIE  COUNTER",
                    fontSize = 12.sp,
                    color = Color(0xFF9CA3AF),
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = stringResource(id = R.string.onb_notif_title_got_a_sec),
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF111114)
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = stringResource(id = R.string.onb_notif_subtitle_log_meal_goal),
                    fontSize = 12.sp,
                    color = Color(0xFF9CA3AF),
                    maxLines = 2
                )
            }
            Spacer(Modifier.width(6.dp))
            Text("8:30 AM", fontSize = 12.sp, color = Color(0xFF9CA3AF))
        }
    }
}

/* -------------------- 圓角矩形 icon -------------------- */
@Composable
private fun AppIconRounded(
    @DrawableRes resId: Int,
    size: Dp,
    corner: Dp = 10.dp,
    borderColor: Color = Color(0x4D000000),
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

/* -------------------- Title + 尾端圖片（相容版，不用 InlineTextContent） -------------------- */
@Composable
private fun NotifTitleWithEndImageInline(
    text: String,
    @DrawableRes tailRes: Int,
    modifier: Modifier = Modifier,
    // ★ 新增：用 dp 指定圖示大小（會換算成與 45dp 視覺一致的 sp）
    tailSizeDp: Dp? = null,
    // 備用：用 sp 指定（若 tailSizeDp 提供，會優先用 dp）
    tailSizeSp: TextUnit = 24.sp,
    tailSpaceEm: Float = 0.25f,           // 與文字間距（字寬倍數）
    textAlign: TextAlign = TextAlign.Center,
    style: TextStyle = MaterialTheme.typography.headlineLarge.copy(
        fontSize = 42.sp,
        fontWeight = FontWeight.ExtraBold,
        lineHeight = 45.sp
    )
) {
    val inlineId = "title_tail_icon"

    // 把 dp 轉成與 dp 視覺等效的 sp：sp_px = sp * density * fontScale
    // 希望 sp_px == dp_px = dp * density -> sp = dp / fontScale
    val sizeSp: TextUnit = if (tailSizeDp != null) {
        (tailSizeDp.value / LocalDensity.current.fontScale).sp
    } else {
        tailSizeSp
    }

    val rich = remember(text, tailSpaceEm) {
        buildAnnotatedString {
            append(text)
            // 加些空白，避免文字緊貼圖示
            append(" ")
            appendInlineContent(inlineId, "[icon]")
        }
    }

    val inlineContent = remember(tailRes, sizeSp) {
        mapOf(
            inlineId to InlineTextContent(
                Placeholder(
                    width = sizeSp,
                    height = sizeSp,
                    placeholderVerticalAlign = PlaceholderVerticalAlign.Center
                )
            ) {
                // 不加 tint -> 保留原本色彩
                Image(painter = painterResource(id = tailRes), contentDescription = null,
                    modifier = Modifier
                        .offset(y = 3.dp)) // ★ 關鍵：往下移)
            }
        )
    }

    Text(
        text = rich,
        inlineContent = inlineContent,
        modifier = modifier,
        style = style,
        textAlign = textAlign,
        softWrap = true,
        maxLines = Int.MAX_VALUE,
        overflow = TextOverflow.Clip
    )
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
