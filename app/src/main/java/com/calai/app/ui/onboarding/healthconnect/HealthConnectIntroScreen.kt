package com.calai.app.ui.onboarding.healthconnect

import androidx.compose.foundation.layout.width
import android.R.attr.maxWidth
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
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord
import com.calai.app.R

// 新增：畫粗白色勾勾需要的 Canvas 相關 import
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.calai.app.ui.common.OnboardingProgress

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HealthConnectIntroScreen(
    onBack: () -> Unit,
    onSkip: () -> Unit,
    onConnected: () -> Unit,
    // 中央圖片（換成你的 drawable）
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
                        if (setOf(
                                HealthPermission.getReadPermission(StepsRecord::class),
                                HealthPermission.getReadPermission(ExerciseSessionRecord::class),
                                HealthPermission.getReadPermission(SleepSessionRecord::class)
                            ).all { it in granted }
                        ) onConnected()
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
                                launcher.launch(
                                    setOf(
                                        HealthPermission.getReadPermission(StepsRecord::class),
                                        HealthPermission.getReadPermission(ExerciseSessionRecord::class),
                                        HealthPermission.getReadPermission(SleepSessionRecord::class)
                                    )
                                )
                            }
                        },
                        onSkip = onSkip
                    )
                }
            } else {
                // 沒有 ActivityResultRegistryOwner 時，直接觸發 onConnected（方便預覽/測試）
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

            Spacer(Modifier.height(100.dp))

            // ===== Hero 區：白色圓角方形（更窄＋更粗外框）+ 中央圖片 + 方框外下方綠勾 =====
            val cardCorner = 28.dp
            val cardHeight = 148.dp
            val cardWidthFraction = 0.42f          // 更窄
            val cardBorderWidth = 2.3.dp           // 更粗外框

            // 勾勾大小與粗細（你要再調，改這兩個值）
            val checkSize = 30.dp                  // 綠圓更小
            val checkStroke = 4.dp                 // 白色勾更粗
            val checkGap = 20.dp                    // 與方框底邊的間距

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .height(300.dp),
                contentAlignment = Alignment.Center
            ) {
                // 白色圓角方形
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
                        contentDescription = "Center Image",
                        modifier = Modifier.size(135.dp),
                        contentScale = ContentScale.Fit
                    )
                }

                // 綠圓＋「粗白勾」：在方框「外」的下方中央
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        // 置於外側：方框半高 + 間距 + 綠圓半徑
                        .offset(y = (cardHeight / 2) + checkGap + (checkSize / 2))
                        .size(checkSize)
                        .clip(CircleShape)
                        .background(Color(0xFF2BB673)),
                    contentAlignment = Alignment.Center
                ) {
                    // 用 Canvas 畫兩段線的 ✓，可調粗細
                    Canvas(modifier = Modifier.fillMaxSize().padding((checkSize * 0.18f))) {
                        val w = size.width
                        val h = size.height
                        // ✓ 的三個關鍵點（比例坐標）
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

            Spacer(Modifier.height(10.dp))

            // 放在你原本的 `Spacer(Modifier.height(30.dp))` 之後，直接替換原本的 Column 區塊
            Column(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 自訂一個更窄的比例（原 0.75f → 0.66f）
                val titleWidthFraction = 0.78f
                val bodyWidthFraction = 0.76f  // 內文可以略寬一點點，易讀

                Text(
                    text = "連接到",
                    modifier = Modifier.fillMaxWidth(titleWidthFraction),
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontSize = 42.sp,
                        fontWeight = FontWeight.ExtraBold,
                        lineHeight = 45.sp
                    ),
                    color = Color(0xFF111114),
                    textAlign = TextAlign.Start,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = "Health Connect",
                    modifier = Modifier.fillMaxWidth(titleWidthFraction),
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontSize = 42.sp,
                        fontWeight = FontWeight.ExtraBold,
                        lineHeight = 45.sp
                    ),
                    color = Color(0xFF111114),
                    textAlign = TextAlign.Start,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(Modifier.height(8.dp))

                Text(
                    text = "在 BiteCal AI 和健康應用之間同步你的日常活動，以獲得最全面的數據。",
                    modifier = Modifier.fillMaxWidth(bodyWidthFraction),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 16.sp,
                        lineHeight = 22.sp
                    ),
                    color = Color(0xFF8F98A3),
                    textAlign = TextAlign.Start
                )
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun HCBottomBar(
    onPrimary: () -> Unit,
    onSkip: () -> Unit,
) {
    Box {
        // 主按鈕（黑底白字、64dp 高、28dp 圓角、底部 59dp）— 與 Allow Notifications 同款
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
                text = "繼續",
                fontSize = 19.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        // 次要動作「暫不」— 與 NotificationPermissionScreen 配對的純文字樣式
        TextButton(
            onClick = onSkip,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 18.dp) // 與主按鈕距離自然；可依需求微調
        ) {
            Text(
                text = "暫不",
                color = Color.Black,
                fontSize = 16.sp ,
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
