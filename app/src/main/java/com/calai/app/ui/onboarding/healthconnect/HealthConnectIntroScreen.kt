package com.calai.app.ui.onboarding.healthconnect

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord
import com.calai.app.R
import com.calai.app.ui.common.OnboardingProgress

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HealthConnectIntroScreen(
    onBack: () -> Unit,
    onSkip: () -> Unit,
    onConnected: () -> Unit,
    @DrawableRes centerImageRes: Int = R.drawable.health_connect_logo
) {
    val ctx = LocalContext.current
    val activity = remember(ctx) { ctx.findActivity() as? ComponentActivity }

    val requiredPermissions = remember {
        setOf(
            HealthPermission.getReadPermission(StepsRecord::class),
            HealthPermission.getReadPermission(ExerciseSessionRecord::class),
            HealthPermission.getReadPermission(SleepSessionRecord::class)
        )
    }

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
            if (activity != null) {
                CompositionLocalProvider(LocalActivityResultRegistryOwner provides activity) {
                    val launcher = rememberLauncherForActivityResult(
                        contract = PermissionController.createRequestPermissionResultContract()
                    ) { granted ->
                        if (requiredPermissions.all { it in granted }) onConnected()
                    }
                    HCBottomBar(
                        onPrimary = {
                            val status = HealthConnectClient.getSdkStatus(ctx)
                            if (
                                status == HealthConnectClient.SDK_UNAVAILABLE ||
                                status == HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED
                            ) {
                                openHealthConnectStore(ctx)
                            } else {
                                launcher.launch(requiredPermissions)
                            }
                        },
                        onSkip = onSkip
                    )
                }
            } else {
                HCBottomBar(
                    onPrimary = onConnected,
                    onSkip = onSkip
                )
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
                stepIndex = 10,
                totalSteps = 11,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
            )

            Spacer(Modifier.height(105.dp))

            // ===== Hero：白色圓角框 + 圖 + 下方綠勾 =====
            val cardCorner = 28.dp
            val cardHeight = 148.dp
            val cardWidthFraction = 0.42f
            val cardBorderWidth = 2.3.dp

            val checkSize = 30.dp
            val checkStroke = 4.dp
            val checkGap = 20.dp

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .height(300.dp),
                contentAlignment = Alignment.Center
            ) {
                val cardModifier = Modifier
                    .fillMaxWidth(cardWidthFraction)
                    .height(cardHeight)
                    .clip(RoundedCornerShape(cardCorner))
                    .background(Color.White)
                    .border(
                        width = cardBorderWidth,
                        color = Color(0xFFE2E8F0),
                        shape = RoundedCornerShape(cardCorner)
                    )

                Box(cardModifier, contentAlignment = Alignment.Center) {
                    Image(
                        painter = painterResource(centerImageRes),
                        contentDescription = "logo", // ← 使用字串資源
                        modifier = Modifier.size(135.dp),
                        contentScale = ContentScale.Fit
                    )
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .offset(y = (cardHeight / 2) + checkGap + (checkSize / 2))
                        .size(checkSize)
                        .clip(CircleShape)
                        .background(Color(0xFF2BB673)),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier
                        .fillMaxSize()
                        .padding((checkSize * 0.18f))) {
                        val w = size.width
                        val h = size.height
                        val p1 = Offset(w * 0.20f, h * 0.55f)
                        val p2 = Offset(w * 0.43f, h * 0.75f)
                        val p3 = Offset(w * 0.82f, h * 0.30f)
                        val strokePx = checkStroke.toPx()
                        drawLine(
                            color = Color.White,
                            start = p1, end = p2,
                            strokeWidth = strokePx,
                            cap = StrokeCap.Round
                        )
                        drawLine(
                            color = Color.White,
                            start = p2, end = p3,
                            strokeWidth = strokePx,
                            cap = StrokeCap.Round
                        )
                    }
                }
            }

            Spacer(Modifier.height(6.dp))

            // 標題＋內文（全部改用 stringResource）
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val titleWidthFraction = 0.74f
                val bodyWidthFraction = 0.72f

                Text(
                    text = stringResource(R.string.hc_connect_title_prefix), // 「連接到」
                    modifier = Modifier.fillMaxWidth(titleWidthFraction),
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontSize = 42.sp,
                        fontWeight = FontWeight.ExtraBold,
                        lineHeight = 42.sp
                    ),
                    color = Color(0xFF111114),
                    textAlign = TextAlign.Start,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = stringResource(R.string.hc_connect_title_service), // 「Health Connect」
                    modifier = Modifier.fillMaxWidth(titleWidthFraction),
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontSize = 42.sp,
                        fontWeight = FontWeight.ExtraBold,
                        lineHeight = 42.sp
                    ),
                    color = Color(0xFF111114),
                    textAlign = TextAlign.Start,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(Modifier.height(9.dp))

                Text(
                    text = stringResource(R.string.hc_connect_body),
                    modifier = Modifier.fillMaxWidth(bodyWidthFraction),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 16.sp,
                        lineHeight = 22.sp
                    ),
                    color = Color(0xFF8F98A3),
                    textAlign = TextAlign.Start
                )
            }
        }
    }
}

@Composable
private fun HCBottomBar(
    onPrimary: () -> Unit,
    onSkip: () -> Unit,
) {
    Box {
        Button(
            onClick = onPrimary,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(start = 20.dp, end = 20.dp, bottom = 72.dp)
                .fillMaxWidth()
                .height(64.dp),
            shape = RoundedCornerShape(28.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Black,
                contentColor = Color.White
            )
        ) {
            Text(
                text = stringResource(R.string.continue_btn), // ← 使用既有「繼續」
                fontSize = 19.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        TextButton(
            onClick = onSkip,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 18.dp)
        ) {
            Text(
                text = stringResource(R.string.skip_text), // ← 新增「暫不」
                color = Color.Black,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

/* ---------- 其他 ---------- */
private fun openHealthConnectStore(ctx: Context) {
    val market = Uri.parse("market://details?id=com.google.android.healthconnect")
    val web = Uri.parse("https://play.google.com/store/apps/details?id=com.google.android.healthconnect")
    try { ctx.startActivity(Intent(Intent.ACTION_VIEW, market).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
    catch (_: ActivityNotFoundException) { ctx.startActivity(Intent(Intent.ACTION_VIEW, web).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
