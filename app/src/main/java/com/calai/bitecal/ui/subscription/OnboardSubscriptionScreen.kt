package com.calai.bitecal.ui.subscription

import android.app.Activity
import android.graphics.Paint
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.calai.bitecal.BuildConfig
import com.calai.bitecal.data.billing.BiteCalBillingProducts
import com.calai.bitecal.data.entitlement.api.EntitlementSyncResponse
import kotlinx.coroutines.delay
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.ButtonDefaults
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.material3.Button
import androidx.compose.ui.graphics.Path

private enum class OnboardPaywallStep {
    Intro,
    Spin,
    OneTimeOffer
}

@Composable
fun OnboardSubscriptionScreen(
    vm: SubscriptionViewModel,
    activity: Activity,
    onCloseToSignIn: () -> Unit,
    onPurchased: (EntitlementSyncResponse) -> Unit
) {
    val ui by vm.ui.collectAsState()

    var step by rememberSaveable { mutableStateOf(OnboardPaywallStep.Intro) }
    var trialEnabled by rememberSaveable { mutableStateOf(true) }

    Box(modifier = Modifier.fillMaxSize()) {
        when (step) {
            OnboardPaywallStep.Intro -> {
                OnboardSubscriptionIntro(
                    purchasing = ui.purchasing,
                    onClose = onCloseToSignIn,
                    onContinue = {
                        if (shouldBypassInitialGooglePlaywallForDev()) {
                            step = OnboardPaywallStep.Spin
                        } else {
                            /**
                             * 圖2：Google Play 原價 yearly plan
                             *
                             * 這裡不要傳 DEFAULT_YEARLY offerTag。
                             * 原價 base plan 通常是 regular base plan，
                             * 由 PlayBillingGateway 在 offerTag=null 時自動選。
                             */
                            vm.purchaseProduct(
                                activity = activity,
                                productId = BiteCalBillingProducts.YEARLY,
                                offerTag = null,
                                onSuccess = onPurchased,
                                onCancelled = {
                                    step = OnboardPaywallStep.Spin
                                }
                            )
                        }
                    }
                )
            }

            OnboardPaywallStep.Spin -> {
                OnboardDiscountSpinScreen(
                    onContinue = {
                        step = OnboardPaywallStep.OneTimeOffer
                    }
                )
            }

            OnboardPaywallStep.OneTimeOffer -> {
                OnboardOneTimeOfferScreen(
                    purchasing = ui.purchasing,
                    trialEnabled = trialEnabled,
                    onTrialEnabledChange = { trialEnabled = it },
                    onClose = onCloseToSignIn,
                    onContinue = {
                        val offerTag = if (trialEnabled) {
                            BiteCalBillingProducts.OfferTags.ONBOARD_TRIAL_DISCOUNT_YEARLY
                        } else {
                            BiteCalBillingProducts.OfferTags.ONBOARD_DISCOUNT_YEARLY
                        }

                        /**
                         * 圖6：Google Play discount / trial offer
                         *
                         * 這裡才需要傳 offerTag。
                         */
                        vm.purchaseProduct(
                            activity = activity,
                            productId = BiteCalBillingProducts.YEARLY,
                            offerTag = offerTag,
                            onSuccess = onPurchased,
                            onCancelled = {
                                // 使用者關閉圖6，停留在 one-time offer 頁面。
                            }
                        )
                    }
                )
            }
        }

        if (!ui.error.isNullOrBlank()) {
            Text(
                text = ui.error!!,
                color = Color(0xFFB91C1C),
                fontSize = 13.sp,
                lineHeight = 17.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 78.dp)
                    .padding(horizontal = 18.dp)
                    .background(
                        color = Color(0xFFFFEBEE),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            )
        }
    }
}

private fun shouldBypassInitialGooglePlaywallForDev(): Boolean {
    return BuildConfig.DEBUG && (
        BuildConfig.APPLICATION_ID.endsWith(".dev") ||
            BuildConfig.APPLICATION_ID.endsWith(".devwifi") ||
            BuildConfig.APPLICATION_ID.endsWith(".devusb")
        )
}

@Composable
private fun OnboardSubscriptionIntro(
    purchasing: Boolean,
    onClose: () -> Unit,
    onContinue: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        IconButton(
            onClick = onClose,
            enabled = !purchasing,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 42.dp, end = 24.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Close",
                tint = Color(0xFF9CA3AF),
                modifier = Modifier.size(34.dp)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp)
                .padding(top = 110.dp, bottom = 160.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Unlock CalAI to reach\nyour goals faster.",
                color = Color.Black,
                fontSize = 35.sp,
                lineHeight = 41.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(92.dp))

            PhonePreviewMock()
        }

        Text(
            text = "Just NT$999.00 per 年 (NT$83.25/mo)",
            color = Color(0xFF71717A),
            fontSize = 18.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(start = 20.dp, end = 20.dp, bottom = 116.dp)
                .fillMaxWidth()
        )

        PrimaryBlackButton(
            text = "Continue",
            loading = purchasing,
            onClick = onContinue,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(start = 20.dp, end = 20.dp, bottom = 40.dp)
                .fillMaxWidth()
                .height(68.dp)
        )
    }
}

@Composable
private fun OnboardDiscountSpinScreen(
    onContinue: () -> Unit
) {
    val initialRotationDegrees = -120f

    /**
     * Wheel angle convention:
     * - 0 degrees   = right
     * - 90 degrees  = bottom
     * - 180 degrees = left
     * - -90 degrees = top
     *
     * Gift segment is currently index 1 in segmentLabels:
     * listOf("80% off", "🎁", "50% off", "35% off", "No luck", "20% off")
     *
     * With drawArc(startAngle = index * 60f - 90f, sweepAngle = 60f),
     * segment center = index * 60f - 90f + 30f.
     * Gift index 1 center = 1 * 60 - 90 + 30 = 0 degrees.
     *
     * Since the pointer is now at the top (-90 degrees),
     * final rotation must move the gift center from 0 degrees to -90 degrees.
     */
    val fullTurns = 6f
    val segmentSweepDegrees = 60f
    val wheelStartOffsetDegrees = -90f
    val topPointerAngleDegrees = -90f
    val giftSegmentIndex = 1f

    val giftNaturalCenterDegrees =
        giftSegmentIndex * segmentSweepDegrees +
                wheelStartOffsetDegrees +
                segmentSweepDegrees / 2f

    val finalGiftRotationDegrees =
        360f * fullTurns + (topPointerAngleDegrees - giftNaturalCenterDegrees)

    val rotation = remember { Animatable(initialRotationDegrees) }
    var spinStarted by rememberSaveable { mutableStateOf(false) }
    var spinFinished by rememberSaveable { mutableStateOf(false) }
    val unlockedDiscountText = "Gift offer unlocked"

    LaunchedEffect(spinStarted) {
        if (spinStarted && !spinFinished) {
            rotation.snapTo(initialRotationDegrees)
            delay(180)
            rotation.animateTo(
                targetValue = finalGiftRotationDegrees,
                animationSpec = tween(
                    durationMillis = 4200,
                    easing = FastOutSlowInEasing
                )
            )
            spinFinished = true
        }
    }

    val helperText = when {
        !spinStarted -> "Tap Spin to reveal your exclusive gift."
        !spinFinished -> "Please wait until the wheel stops."
        else -> "This offer is revealed after the wheel stops."
    }

    val buttonText = when {
        !spinStarted -> "Spin"
        !spinFinished -> "Spinning..."
        else -> "Continue"
    }

    val buttonLoading = spinStarted && !spinFinished

    val buttonOnClick: () -> Unit = when {
        !spinStarted -> {
            { spinStarted = true }
        }
        !spinFinished -> {
            { }
        }
        else -> onContinue
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp)
                .padding(top = 110.dp, bottom = 170.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Spin to unlock an\nexclusive discount",
                color = Color.Black,
                fontSize = 32.sp,
                lineHeight = 38.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(36.dp))

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(390.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(330.dp)
                        .offset(y = 30.dp),
                    contentAlignment = Alignment.Center
                ) {
                    DiscountWheelMock(
                        rotationDegrees = rotation.value,
                        modifier = Modifier.fillMaxSize()
                    )

                    DiscountWheelPointerTop(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .offset(y = (-38).dp)
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            if (spinStarted) {
                Text(
                    text = if (spinFinished) unlockedDiscountText else "Spinning...",
                    color = if (spinFinished) Color(0xFFE45F69) else Color(0xFF71717A),
                    fontSize = 22.sp,
                    lineHeight = 26.sp,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center
                )
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(start = 20.dp, end = 20.dp, bottom = 28.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            PrimaryBlackButton(
                text = buttonText,
                loading = buttonLoading,
                onClick = buttonOnClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(68.dp)
            )

            Spacer(Modifier.height(14.dp))

            Text(
                text = helperText,
                color = Color(0xFF71717A),
                fontSize = 16.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun OnboardOneTimeOfferScreen(
    purchasing: Boolean,
    trialEnabled: Boolean,
    onTrialEnabledChange: (Boolean) -> Unit,
    onClose: () -> Unit,
    onContinue: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .navigationBarsPadding()
    ) {
        IconButton(
            onClick = onClose,
            enabled = !purchasing,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(top = 42.dp, start = 20.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Close",
                tint = Color.Black,
                modifier = Modifier.size(34.dp)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 22.dp)
                .padding(top = 100.dp, bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Your one-time offer",
                color = Color.Black,
                fontSize = 39.sp,
                lineHeight = 43.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(34.dp))

            DiscountCardMock()

            Spacer(Modifier.height(34.dp))

            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "NT$999.00",
                    color = Color(0xFF9CA3AF),
                    fontSize = 25.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "  NT$54.08 /mo",
                    color = Color(0xFFE45F69),
                    fontSize = 39.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }

            Spacer(Modifier.height(34.dp))

            OfferBullet("☕", "Less than a coffee.")
            OfferBullet("⚠️", "Cancel anytime in Google Play.")
            OfferBullet("🙋", "Start tracking smarter today.")

            Spacer(Modifier.weight(1f))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (trialEnabled) {
                        "Free Trial Enabled"
                    } else {
                        "Not Sure? Enable Free Trial"
                    },
                    color = Color.Black,
                    fontSize = 27.sp,
                    fontWeight = FontWeight.Normal,
                    modifier = Modifier.weight(1f)
                )

                Switch(
                    checked = trialEnabled,
                    onCheckedChange = {
                        if (!purchasing) onTrialEnabledChange(it)
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = Color.Black,
                        uncheckedThumbColor = Color.White,
                        uncheckedTrackColor = Color(0xFFE5E7EB)
                    )
                )
            }

            Spacer(Modifier.height(14.dp))

            OfferPlanCard(
                header = if (trialEnabled) "FREE TRIAL" else "LOWEST PRICE EVER"
            )

            Spacer(Modifier.height(22.dp))

            PrimaryDarkPurpleButton(
                text = if (trialEnabled) "Start Free Trial" else "Continue",
                loading = purchasing,
                onClick = onContinue
            )

            Spacer(Modifier.height(20.dp))

            Text(
                text = "✓  No Commitment - Cancel Anytime",
                color = Color.Black,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun PhonePreviewMock() {
    Box(
        modifier = Modifier
            .size(width = 290.dp, height = 430.dp)
            .border(8.dp, Color(0xFF18181B), RoundedCornerShape(42.dp))
            .padding(22.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "🍎 Cal AI",
                color = Color(0xFF18181B),
                fontWeight = FontWeight.Bold,
                fontSize = 21.sp
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(78.dp)
                    .background(Color(0xFFF8F8FA), RoundedCornerShape(18.dp))
                    .padding(18.dp)
            ) {
                Text(
                    text = "2199\nCalories left",
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp,
                    lineHeight = 23.sp
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(3) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(92.dp)
                            .background(Color(0xFFF8F8FA), RoundedCornerShape(16.dp))
                    )
                }
            }

            Text(
                text = "Recently eaten",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(88.dp)
                    .background(Color(0xFFF8F8FA), RoundedCornerShape(16.dp))
            ) {
                Text(
                    text = "Analyzing food...",
                    modifier = Modifier.align(Alignment.Center),
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
private fun DiscountWheelPointerTop(
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier.size(width = 44.dp, height = 30.dp)
    ) {
        val path = Path().apply {
            // Top pointer. The bottom tip points to the wheel.
            moveTo(size.width / 2f, size.height)
            lineTo(0f, 0f)
            lineTo(size.width, 0f)
            close()
        }

        drawPath(
            path = path,
            color = Color(0xFF1C1923)
        )
    }
}

@Composable
private fun DiscountWheelMock(
    rotationDegrees: Float,
    modifier: Modifier = Modifier
) {
    /**
     * Important:
     * - Gift segment is index 1.
     * - With the arc formula below, index 1 is centered at 0 degrees,
     *   which is exactly the right-side pointer position.
     * - OnboardDiscountSpinScreen animates to 360 * N, so the wheel always
     *   stops with the gift under the pointer.
     */
    val segmentLabels = listOf(
        "80% off",
        "🎁",
        "50% off",
        "35% off",
        "No luck",
        "20% off"
    )
    val segmentColors = listOf(
        Color.White,
        Color(0xFF1C1923),
        Color.White,
        Color(0xFF1C1923),
        Color.White,
        Color(0xFF1C1923)
    )

    Canvas(
        modifier = modifier.graphicsLayer {
            rotationZ = rotationDegrees
        }
    ) {
        val radius = size.minDimension / 2f
        val center = Offset(size.width / 2f, size.height / 2f)

        repeat(6) { index ->
            drawArc(
                color = segmentColors[index],
                startAngle = index * 60f - 90f,
                sweepAngle = 60f,
                useCenter = true
            )
        }

        drawCircle(
            color = Color(0xFF1C1923),
            radius = radius,
            center = center,
            style = Stroke(width = 18f)
        )

        val textPaint = Paint().apply {
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
            textSize = size.minDimension * 0.08f
            typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
        }

        repeat(6) { index ->
            val midAngle = Math.toRadians((index * 60f - 60f).toDouble())
            val textRadius = radius * 0.68f
            val x = center.x + (kotlin.math.cos(midAngle) * textRadius).toFloat()
            val y = center.y + (kotlin.math.sin(midAngle) * textRadius).toFloat()
            textPaint.color = if (segmentColors[index] == Color.White) {
                android.graphics.Color.BLACK
            } else {
                android.graphics.Color.WHITE
            }
            textPaint.textSize = if (segmentLabels[index] == "🎁") {
                size.minDimension * 0.11f
            } else {
                size.minDimension * 0.07f
            }
            drawContext.canvas.nativeCanvas.save()
            drawContext.canvas.nativeCanvas.rotate((index * 60f).toFloat(), x, y)
            drawContext.canvas.nativeCanvas.drawText(segmentLabels[index], x, y, textPaint)
            drawContext.canvas.nativeCanvas.restore()
        }

        drawCircle(
            color = Color(0xFF1C1923),
            radius = 42f,
            center = center
        )

        drawCircle(
            color = Color.White,
            radius = 13f,
            center = center
        )
    }
}

@Composable
private fun DiscountCardMock() {
    Box(
        modifier = Modifier
            .fillMaxWidth(0.62f)
            .height(138.dp)
            .background(Color(0xFF1C1923), RoundedCornerShape(28.dp))
            .border(2.dp, Color.White, RoundedCornerShape(28.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "80% OFF\nFOREVER",
            color = Color(0xFFE5E7EB),
            fontSize = 38.sp,
            lineHeight = 42.sp,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun OfferBullet(
    emoji: String,
    text: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 52.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = emoji,
            fontSize = 28.sp,
            modifier = Modifier.size(42.dp)
        )

        Text(
            text = text,
            color = Color.Black,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun OfferPlanCard(
    header: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp)
            .border(2.dp, Color(0xFF1C1923), RoundedCornerShape(18.dp))
            .background(Color.White, RoundedCornerShape(18.dp))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(28.dp)
                .background(
                    color = Color(0xFF1C1923),
                    shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = header,
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = "Yearly Plan",
                    color = Color.Black,
                    fontSize = 25.sp,
                    fontWeight = FontWeight.ExtraBold
                )

                Spacer(Modifier.height(8.dp))

                Text(
                    text = "12mo · NT$649.00",
                    color = Color(0xFF71717A),
                    fontSize = 22.sp
                )
            }

            Text(
                text = "NT$54.08 /mo",
                color = Color.Black,
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold
            )
        }
    }
}

@Composable
private fun PrimaryBlackButton(
    text: String,
    loading: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        enabled = !loading,
        modifier = modifier,
        shape = RoundedCornerShape(999.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Black,
            contentColor = Color.White,
            disabledContainerColor = Color.Black,
            disabledContentColor = Color.White
        ),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 0.dp)
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = Color.White,
                strokeWidth = 2.dp
            )
        } else {
            Text(
                text = text,
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun PrimaryDarkPurpleButton(
    text: String,
    loading: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(68.dp)
            .padding(horizontal = 18.dp)
            .background(Color(0xFF1C1923), RoundedCornerShape(18.dp))
            .clickable(enabled = !loading, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = Color.White,
                strokeWidth = 2.dp
            )
        } else {
            Text(
                text = text,
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold
            )
        }
    }
}
