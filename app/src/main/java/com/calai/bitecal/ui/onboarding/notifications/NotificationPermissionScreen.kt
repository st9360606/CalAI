package com.calai.bitecal.ui.onboarding.notifications

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultRegistryOwner
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.calai.bitecal.BuildConfig
import com.calai.bitecal.R
import com.calai.bitecal.ui.common.OnboardingProgress

private const val TAG_NOTIF = "NotifPerm"

/**
 * ✅ 不使用 Manifest.permission.POST_NOTIFICATIONS（API 33 才有）
 * 直接用字串避免 minSdk Lint 問題。
 */
private const val PERM_POST_NOTIFICATIONS = "android.permission.POST_NOTIFICATIONS"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationPermissionScreen(
    onBack: () -> Unit,
    onNext: () -> Unit,
    @DrawableRes appIconRes: Int = R.drawable.ic_focus_spoon_foreground
) {
    val ctx = LocalContext.current
    val activity = remember(ctx) { ctx.findActivity() as? ComponentActivity }

    if (BuildConfig.DEBUG) {
        Log.d(
            TAG_NOTIF,
            "sdk=${Build.VERSION.SDK_INT}, target=${ctx.applicationInfo.targetSdkVersion}, " +
                    "hasManifest=${isPermissionInManifest(ctx, PERM_POST_NOTIFICATIONS)}, " +
                    "granted=${isNotificationsEnabled(ctx)}, activity=${activity != null}"
        )
    }

    Scaffold(
        containerColor = Color.White,
        topBar = {
            TopAppBar(
                modifier = Modifier.padding(start = 16.dp, end = 16.dp),
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White,
                    navigationIconContentColor = Color(0xFF111114)
                ),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color(0xFFF1F3F7)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = Color(0xFF111114)
                            )
                        }
                    }
                },
                title = {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(end = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OnboardingProgress(
                            stepIndex = 9,
                            totalSteps = 11,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            )
        },
        bottomBar = {
            val bottomCtx = LocalContext.current
            val lifecycleOwner = LocalLifecycleOwner.current
            val bottomActivity = remember(bottomCtx) { bottomCtx.findActivity() as? ComponentActivity }

            val ownerFromLocal = LocalActivityResultRegistryOwner.current
            val ownerFromLifecycle = lifecycleOwner as? ActivityResultRegistryOwner
            val effectiveOwner: ActivityResultRegistryOwner? =
                ownerFromLocal ?: ownerFromLifecycle ?: bottomActivity

            val shouldRequest =
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                        isPermissionInManifest(bottomCtx, PERM_POST_NOTIFICATIONS) &&
                        ContextCompat.checkSelfPermission(bottomCtx, PERM_POST_NOTIFICATIONS) !=
                        PackageManager.PERMISSION_GRANTED

            if (BuildConfig.DEBUG) {
                Log.d(
                    TAG_NOTIF,
                    "shouldRequest=$shouldRequest, owner=${effectiveOwner != null}, " +
                            "granted=${isNotificationsEnabled(bottomCtx)}, sdk=${Build.VERSION.SDK_INT}"
                )
            }

            when {
                shouldRequest && effectiveOwner != null -> {
                    CompositionLocalProvider(LocalActivityResultRegistryOwner provides effectiveOwner) {
                        val launcher = rememberLauncherForActivityResult(
                            contract = ActivityResultContracts.RequestPermission()
                        ) { _ ->
                            onNext()
                        }

                        NotifBottomBar(
                            granted = isNotificationsEnabled(bottomCtx),
                            onClick = { launcher.launch(PERM_POST_NOTIFICATIONS) }
                        )
                    }
                }

                else -> {
                    NotifBottomBar(
                        granted = isNotificationsEnabled(bottomCtx),
                        onClick = { onNext() }
                    )
                }
            }
        }
    ) { inner ->
        val config = androidx.compose.ui.platform.LocalConfiguration.current
        val screenWidth = config.screenWidthDp.dp

        // 你這裡固定左右 padding 20.dp，所以可用寬度約等於 screenWidth - 40.dp
        val availableWidth = (screenWidth - 40.dp).coerceAtLeast(0.dp)
        val panelWidth = availableWidth * 0.84f
        val panelHeight = (panelWidth * 1.20f).coerceIn(360.dp, 620.dp)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(Modifier.height(20.dp))

            // ✅ 改成 Box + LocalConfiguration 計算，避免 BoxWithConstraints 的 UiComposable 問題
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.TopCenter
            ) {
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
                        fontSize = 42.sp,
                        fontWeight = FontWeight.ExtraBold,
                        lineHeight = 45.sp
                    )

                    NotifTitleWithEndImageInline(
                        text = verifyTitle(R.string.notif_title, "Make every meal count"),
                        tailRes = R.drawable.notifications,
                        modifier = Modifier.fillMaxWidth(0.82f),
                        tailSizeSp = titleStyle.fontSize,
                        tailSpaceEm = 1.1f,
                        textAlign = TextAlign.Start,
                        style = titleStyle
                    )

                    Spacer(Modifier.height(2.dp))

                    Text(
                        text = verifyTitle(
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
                .padding(start = 20.dp, end = 20.dp, bottom = 40.dp)
                .fillMaxWidth()
                .height(64.dp),
            shape = RoundedCornerShape(999.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Black,
                contentColor = Color.White
            )
        ) {
            Text(
                text = if (granted) verifyTitle(R.string.continue_text, "Continue")
                else verifyTitle(R.string.allow_notifications_cta, "Allow Notifications"),
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.2.sp
                ),
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * 只把時間字串中的 ':' 往上（或往下）移動一點。
 */
private fun buildClockAnnotated(
    text: String,
    colonShift: Float = 0.07f
): AnnotatedString {
    val shift = BaselineShift(colonShift)
    return buildAnnotatedString {
        text.forEach { ch ->
            if (ch == ':') {
                withStyle(SpanStyle(baselineShift = shift)) { append(ch) }
            } else append(ch)
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
    clockColor: Color = Color(0xFFC3CEDF),
    clockOffsetTop: Dp = 0.dp,
    clockContentGap: Dp = 16.dp,
    corner: Dp = 28.dp,
    frameBorderColor: Color = Color(0xFFD7E0EC),
    frameBorderWidth: Dp = 2.dp,
    showStatusIcons: Boolean = true,
    statusTint: Color = Color(0xFFAAB7CC),
    batteryPercent: Int = 87,
    content: @Composable ColumnScope.() -> Unit
) {
    Box(
        modifier = modifier
            .border(frameBorderWidth, frameBorderColor, RoundedCornerShape(corner))
            .clip(RoundedCornerShape(corner))
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFFFAFCFF), Color(0xFFEEF3FA))
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
                text = buildClockAnnotated(bigClock),
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
            imageVector = Icons.Filled.Wifi,
            contentDescription = "Wi-Fi",
            tint = tint,
            modifier = Modifier.size(18.dp)
        )

        Spacer(Modifier.width(6.dp))

        Text(
            text = "${batteryPercent.coerceIn(0, 100)}%",
            fontSize = 12.sp,
            color = tint,
            fontWeight = FontWeight.Medium
        )

        Spacer(Modifier.width(6.dp))

        BatteryGaugeHorizontal(
            percent = batteryPercent,
            tint = tint
        )
    }
}

@Composable
private fun BatteryGaugeHorizontal(
    percent: Int,
    tint: Color,
    width: Dp = 26.dp,
    height: Dp = 12.dp,
    strokeWidth: Dp = 1.6.dp,
    corner: Dp = 3.dp,
    tipWidth: Dp = 2.dp,
    tipGap: Dp = 1.dp
) {
    val p = percent.coerceIn(0, 100) / 100f

    Canvas(
        modifier = Modifier.size(width + tipWidth + tipGap, height)
    ) {
        val strokePx = strokeWidth.toPx()
        val cornerPx = corner.toPx()
        val tipW = tipWidth.toPx()
        val gap = tipGap.toPx()

        val bodyW = size.width - tipW - gap
        val bodyH = size.height

        drawRoundRect(
            color = tint,
            topLeft = Offset(0f, 0f),
            size = Size(bodyW, bodyH),
            cornerRadius = CornerRadius(cornerPx, cornerPx),
            style = Stroke(width = strokePx)
        )

        val innerPad = strokePx * 1.3f
        val fillMaxW = (bodyW - innerPad * 2).coerceAtLeast(0f)
        val fillW = (fillMaxW * p).coerceAtLeast(0f)
        val fillH = (bodyH - innerPad * 2).coerceAtLeast(0f)

        if (fillW > 0.5f && fillH > 0.5f) {
            drawRoundRect(
                color = tint,
                topLeft = Offset(innerPad, innerPad),
                size = Size(fillW, fillH),
                cornerRadius = CornerRadius(
                    (cornerPx - innerPad).coerceAtLeast(0f),
                    (cornerPx - innerPad).coerceAtLeast(0f)
                )
            )
        }

        val tipH = bodyH * 0.55f
        val tipY = (bodyH - tipH) / 2f
        drawRoundRect(
            color = tint,
            topLeft = Offset(bodyW + gap, tipY),
            size = Size(tipW, tipH),
            cornerRadius = CornerRadius(tipW, tipW)
        )
    }
}

/* -------------------- 單一卡片：iOS 通知樣式（對齊 & icon滿版版） -------------------- */
private object NotifCardSpec {
    val corner = 18.dp
    val padH = 16.dp
    val padV = 10.dp
    val iconSize = 22.dp
    val iconCorner = 11.dp
    val iconInnerPad = 0.dp
    const val ICONSCALE = 1.28f
    val gapIconText = 10.dp
    val gapMetaToContent = 5.dp
    val metaFont = 11.sp
    val metaLine = 12.sp
    val metaLetterSpacing = 0.6.sp

    val titleFont = 15.sp
    val titleLine = 18.sp

    val bodyFont = 12.sp
    val bodyLine = 16.sp
}

@Composable
private fun NotificationCardIOS(
    @DrawableRes appIconRes: Int,
    modifier: Modifier = Modifier
) {
    val metaColor = Color(0xFF748092)
    val bodyColor = Color(0xFF667085)
    val titleColor = Color(0xFF111114)

    Surface(
        shape = RoundedCornerShape(NotifCardSpec.corner),
        color = Color.White,
        tonalElevation = 0.dp,
        shadowElevation = 14.dp,
        modifier = modifier.fillMaxWidth(0.98f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = NotifCardSpec.padH, vertical = NotifCardSpec.padV)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AppIconIOSBadge(
                    resId = appIconRes,
                    size = NotifCardSpec.iconSize,
                    corner = NotifCardSpec.iconCorner,
                    contentPadding = NotifCardSpec.iconInnerPad,
                    iconScale = NotifCardSpec.ICONSCALE
                )

                Spacer(Modifier.width(NotifCardSpec.gapIconText))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "AI CALORIE COUNTER",
                        fontSize = NotifCardSpec.metaFont,
                        lineHeight = NotifCardSpec.metaLine,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = NotifCardSpec.metaLetterSpacing,
                        color = metaColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    Spacer(Modifier.width(10.dp))

                    Text(
                        text = "8:30 AM",
                        fontSize = NotifCardSpec.metaFont,
                        lineHeight = NotifCardSpec.metaLine,
                        fontWeight = FontWeight.Medium,
                        color = metaColor,
                        maxLines = 1
                    )
                }
            }

            Spacer(Modifier.height(NotifCardSpec.gapMetaToContent))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 3.dp)
            ) {
                Text(
                    text = stringResource(id = R.string.onb_notif_title_got_a_sec),
                    fontSize = NotifCardSpec.titleFont,
                    lineHeight = NotifCardSpec.titleLine,
                    fontWeight = FontWeight.SemiBold,
                    color = titleColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(Modifier.height(3.dp))

                Text(
                    text = stringResource(id = R.string.onb_notif_subtitle_log_meal_goal),
                    fontSize = NotifCardSpec.bodyFont,
                    lineHeight = NotifCardSpec.bodyLine,
                    color = bodyColor,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun AppIconIOSBadge(
    @DrawableRes resId: Int,
    size: Dp,
    corner: Dp,
    contentPadding: Dp,
    iconScale: Float,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.size(size),
        shape = RoundedCornerShape(corner),
        color = Color(0xFF111114),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = resId),
                contentDescription = "App Icon",
                tint = Color.White,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = iconScale
                        scaleY = iconScale
                    }
                    .clip(RoundedCornerShape(corner))
            )
        }
    }
}

/* -------------------- Title + 尾端圖片 -------------------- */
@Composable
private fun NotifTitleWithEndImageInline(
    text: String,
    @DrawableRes tailRes: Int,
    modifier: Modifier = Modifier,
    tailSizeDp: Dp? = null,
    tailSizeSp: TextUnit = 24.sp,
    tailSpaceEm: Float = 0.25f,
    textAlign: TextAlign = TextAlign.Center,
    style: TextStyle = MaterialTheme.typography.headlineLarge.copy(
        fontSize = 42.sp,
        fontWeight = FontWeight.ExtraBold,
        lineHeight = 45.sp
    )
) {
    val inlineId = "title_tail_icon"

    val sizeSp: TextUnit = if (tailSizeDp != null) {
        (tailSizeDp.value / LocalDensity.current.fontScale).sp
    } else {
        tailSizeSp
    }

    val rich = remember(text, tailSpaceEm) {
        buildAnnotatedString {
            append(text)
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
                Image(
                    painter = painterResource(id = tailRes),
                    contentDescription = null,
                    modifier = Modifier.offset(y = 3.dp)
                )
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
        ContextCompat.checkSelfPermission(context, PERM_POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
    } else {
        NotificationManagerCompat.from(context).areNotificationsEnabled()
    }
}

@Suppress("SameParameterValue")
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
private fun verifyTitle(id: Int, fallback: String): String =
    runCatching { stringResource(id) }.getOrElse { fallback }

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
