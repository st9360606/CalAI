package com.calai.bitecal.ui.subscription

import android.app.Activity
import android.graphics.Paint
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.calai.bitecal.BuildConfig
import com.calai.bitecal.R
import com.calai.bitecal.ui.common.haptic.biteCalClickable
import com.calai.bitecal.data.billing.BiteCalBillingProducts
import com.calai.bitecal.data.entitlement.api.EntitlementSyncResponse
import com.calai.bitecal.ui.landing.LandingSlideshow
import com.calai.bitecal.ui.landing.SlideItem
import com.calai.bitecal.ui.landing.device.DeviceFrameIPhone
import kotlinx.coroutines.delay
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.zIndex
import com.calai.bitecal.ui.common.haptic.rememberClickWithHaptic

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

    LaunchedEffect(Unit) {
        vm.loadTrialEligibility()
        vm.loadSubscriptionOfferPrices()
    }

    var step by rememberSaveable { mutableStateOf(OnboardPaywallStep.Intro) }
    var trialEnabled by rememberSaveable { mutableStateOf(false) }
    var trialToggleTouched by rememberSaveable { mutableStateOf(false) }
    var restoreRequiredDismissedThisSession by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(ui.trialEligibilityLoaded, ui.trialEligible) {
        if (!ui.trialEligibilityLoaded) return@LaunchedEffect

        if (!ui.trialEligible) {
            trialEnabled = false
            trialToggleTouched = false
            return@LaunchedEffect
        }

        if (!trialToggleTouched) {
            trialEnabled = true
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when (step) {
            OnboardPaywallStep.Intro -> {
                OnboardSubscriptionIntro(
                    purchasing = ui.purchasing,
                    helperText = "Just ${ui.yearlyBasePrice} per 年 (${ui.yearlyBaseMonthlyEquivalent})",
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
                    trialEligible = ui.trialEligible,
                    trialEligibilityLoaded = ui.trialEligibilityLoaded,
                    originalYearlyPrice = ui.yearlyBasePrice,
                    offerYearlyPrice = if (trialEnabled && ui.trialEligible) {
                        ui.yearlyTrialDiscountPrice
                    } else {
                        ui.yearlyDiscountPrice
                    },
                    monthlyEquivalent = if (trialEnabled && ui.trialEligible) {
                        ui.yearlyTrialDiscountMonthlyEquivalent
                    } else {
                        ui.yearlyDiscountMonthlyEquivalent
                    },
                    onTrialEnabledChange = { enabled ->
                        if (!ui.trialEligible) {
                            trialEnabled = false
                            trialToggleTouched = false
                        } else {
                            trialEnabled = enabled
                            trialToggleTouched = true
                        }
                    },
                    onClose = onCloseToSignIn,
                    onContinue = {
                        val offerTag = if (trialEnabled && ui.trialEligible) {
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

        val errorText = when (ui.errorKind) {
            SubscriptionErrorKind.AlreadyOwnedRestoreRequired -> {
                stringResource(R.string.subscription_already_owned_restore_required)
            }

            null -> ui.error
        }

        val showRestoreRequiredDialog =
            ui.errorKind == SubscriptionErrorKind.AlreadyOwnedRestoreRequired &&
                    !errorText.isNullOrBlank() &&
                    !restoreRequiredDismissedThisSession

        val showGenericErrorBanner =
            ui.errorKind != SubscriptionErrorKind.AlreadyOwnedRestoreRequired &&
                    !errorText.isNullOrBlank()

        if (showRestoreRequiredDialog) {
            RestoreSubscriptionRequiredDialog(
                title = stringResource(R.string.restore_subscription_dialog_title),
                body = errorText.orEmpty(),
                restoreText = stringResource(R.string.settings_restore_subscription),
                restoringText = stringResource(R.string.restore_subscription_dialog_restoring),
                maybeLaterText = stringResource(R.string.common_maybe_later),
                purchasing = ui.purchasing,
                canRestorePurchase = ui.canRestorePurchase,
                onRestore = {
                    vm.restorePurchase(onPurchased)
                },
                onMaybeLater = {
                    restoreRequiredDismissedThisSession = true
                    onCloseToSignIn()
                }
            )
        }

        if (showGenericErrorBanner) {
            OnboardSubscriptionErrorBanner(
                errorText = errorText.orEmpty()
            )
        }
    }
}

@Composable
private fun BoxScope.RestoreSubscriptionRequiredDialog(
    title: String,
    body: String,
    restoreText: String,
    restoringText: String,
    maybeLaterText: String,
    purchasing: Boolean,
    canRestorePurchase: Boolean,
    onRestore: () -> Unit,
    onMaybeLater: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .align(Alignment.Center)
            .zIndex(80f),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.42f))
                .biteCalClickable(enabled = !purchasing) {
                    // 只吃掉背景點擊事件，不關閉 Dialog。
                    // 這樣可以避免使用者誤觸遮罩，導致 restore 提示消失。
                }
        )

        Column(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .fillMaxWidth()
                .shadow(
                    elevation = 22.dp,
                    shape = RoundedCornerShape(28.dp),
                    ambientColor = Color(0x22000000),
                    spotColor = Color(0x33000000)
                )
                .clip(RoundedCornerShape(28.dp))
                .background(Color.White)
                .border(
                    width = 1.dp,
                    color = Color(0xFFE5E7EB),
                    shape = RoundedCornerShape(28.dp)
                )
                .padding(horizontal = 22.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .background(
                        color = Color(0xFF111111),
                        shape = RoundedCornerShape(18.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Restore,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(29.dp)
                )
            }

            Spacer(Modifier.height(18.dp))

            Text(
                text = title,
                color = Color(0xFF111111),
                fontSize = 21.sp,
                lineHeight = 26.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
                letterSpacing = (-0.25).sp,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(10.dp))

            Text(
                text = body,
                color = Color(0xFF52525B),
                fontSize = 15.sp,
                lineHeight = 21.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 2.dp)
            )

            Spacer(Modifier.height(22.dp))

            Button(
                onClick = rememberClickWithHaptic(onClick = onRestore),
                enabled = canRestorePurchase && !purchasing,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF111111),
                    contentColor = Color.White,
                    disabledContainerColor = Color(0xFF111111).copy(alpha = 0.42f),
                    disabledContentColor = Color.White.copy(alpha = 0.82f)
                ),
                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 0.dp)
            ) {
                if (purchasing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )

                    Spacer(Modifier.width(10.dp))

                    Text(
                        text = restoringText,
                        fontSize = 16.sp,
                        lineHeight = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    Text(
                        text = restoreText,
                        fontSize = 16.sp,
                        lineHeight = 20.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            Button(
                onClick = rememberClickWithHaptic(onClick = onMaybeLater),
                enabled = !purchasing,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent,
                    contentColor = Color(0xFF52525B),
                    disabledContainerColor = Color.Transparent,
                    disabledContentColor = Color(0xFFA1A1AA)
                ),
                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 0.dp)
            ) {
                Text(
                    text = maybeLaterText,
                    fontSize = 15.sp,
                    lineHeight = 19.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun BoxScope.OnboardSubscriptionErrorBanner(
    errorText: String
) {
    Column(
        modifier = Modifier
            .align(Alignment.TopCenter)
            .padding(top = 78.dp)
            .padding(horizontal = 18.dp)
            .background(
                color = Color(0xFFFFEBEE),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = errorText,
            color = Color(0xFFB91C1C),
            fontSize = 13.sp,
            lineHeight = 17.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center
        )
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
    helperText: String,
    onClose: () -> Unit,
    onContinue: () -> Unit
) {
    val heroGap = 32.dp

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .padding(top = 110.dp, bottom = 170.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Unlock BiteCal AI to\nreach your goals faster.",
                color = Color.Black,
                fontSize = 32.sp,
                lineHeight = 38.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
                letterSpacing = (-0.6).sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp)
            )

            Spacer(Modifier.height(heroGap))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.TopCenter
            ) {
                PhonePreviewMock(
                    modifier = Modifier
                        .fillMaxWidth(0.75f)
                        .aspectRatio(10.5f / 19.5f)
                )
            }
        }

        OnboardPaywallBottomCta(
            buttonText = "Continue",
            helperText = helperText,
            loading = purchasing,
            onClick = onContinue
        )

        IconButton(
            onClick = rememberClickWithHaptic(onClick = onClose),
            enabled = !purchasing,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 66.dp, end = 28.dp)
                .size(42.dp)
                .zIndex(10f)
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Close",
                tint = Color(0xFFA1A1AA),
                modifier = Modifier.size(28.dp)
            )
        }
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

        OnboardPaywallBottomCta(
            buttonText = buttonText,
            helperText = helperText,
            loading = buttonLoading,
            onClick = buttonOnClick
        )
    }
}
@Composable
private fun OnboardOneTimeOfferScreen(
    purchasing: Boolean,
    trialEnabled: Boolean,
    trialEligible: Boolean,
    trialEligibilityLoaded: Boolean,
    originalYearlyPrice: String,
    offerYearlyPrice: String,
    monthlyEquivalent: String,
    onTrialEnabledChange: (Boolean) -> Unit,
    onClose: () -> Unit,
    onContinue: () -> Unit
) {
    val scrollState = rememberScrollState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 22.dp)
                .padding(top = 110.dp, bottom = 170.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Gift Unlocked 🎁",
                color = Color(0xFF111111),
                fontSize = 30.sp,
                lineHeight = 34.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                letterSpacing = (-0.5).sp,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(20.dp))

            OneTimeOfferHeroCard(
                originalYearlyPrice = originalYearlyPrice,
                monthlyEquivalent = monthlyEquivalent
            )

            Spacer(Modifier.height(18.dp))

            OneTimeOfferUrgencyCard()

            Spacer(Modifier.height(12.dp))

            OneTimeOfferTrialCard(
                trialEnabled = trialEnabled,
                trialEligible = trialEligible,
                trialEligibilityLoaded = trialEligibilityLoaded,
                purchasing = purchasing,
                offerYearlyPrice = offerYearlyPrice,
                monthlyEquivalent = monthlyEquivalent,
                onTrialEnabledChange = onTrialEnabledChange
            )
        }

        OnboardPaywallBottomCta(
            buttonText = if (trialEnabled && trialEligible) {
                "Start Free Trial"
            } else {
                "Continue"
            },
            helperText = "No Commitment - Cancel Anytime",
            helperTextColor = Color(0xFF52525B),
            buttonShape = RoundedCornerShape(14.dp),
            loading = purchasing,
            onClick = onContinue
        )

        IconButton(
            onClick = rememberClickWithHaptic(onClick = onClose),
            enabled = !purchasing,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(top = 60.dp, start = 18.dp)
                .size(42.dp)
                .zIndex(10f)
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Close",
                tint = Color(0xFF71717A),
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

@Composable
private fun OneTimeOfferHeroCard(
    originalYearlyPrice: String,
    monthlyEquivalent: String
) {
    val monthlyPrice = monthlyEquivalent.substringBefore("/")
    val monthlyUnit = "/" + monthlyEquivalent.substringAfter("/", missingDelimiterValue = "mo")
    val shape = RoundedCornerShape(32.dp)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 18.dp,
                shape = shape,
                ambientColor = Color(0x22000000),
                spotColor = Color(0x33000000)
            )
            .clip(shape)
            .drawWithCache {
                val cornerRadius = CornerRadius(32.dp.toPx(), 32.dp.toPx())
                val maxRadius = maxOf(size.width, size.height)

                /**
                 * 更接近你給的圖：
                 * - 左上偏深藍黑
                 * - 中間偏紫黑
                 * - 右下偏暖紅棕
                 */
                val baseGradient = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF121521),
                        Color(0xFF1B1827),
                        Color(0xFF292232),
                        Color(0xFF3A2A31)
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(size.width, size.height)
                )

                /**
                 * 左上冷色光暈，讓卡片不要死黑。
                 */
                val topLeftBlueGlow = Brush.radialGradient(
                    colors = listOf(
                        Color(0x3D5868FF),
                        Color(0x1A5868FF),
                        Color.Transparent
                    ),
                    center = Offset(size.width * 0.10f, size.height * 0.12f),
                    radius = maxRadius * 0.85f
                )

                /**
                 * 右下暖色光暈，呼應優惠價 0xFFE45F69。
                 */
                val bottomRightRoseGlow = Brush.radialGradient(
                    colors = listOf(
                        Color(0x4DE45F69),
                        Color(0x22E45F69),
                        Color.Transparent
                    ),
                    center = Offset(size.width * 0.96f, size.height * 0.94f),
                    radius = maxRadius * 0.85f
                )

                /**
                 * 右上淡暖光，模擬你截圖右上那種棕紅霧面感。
                 */
                val topRightWarmGlow = Brush.radialGradient(
                    colors = listOf(
                        Color(0x2AF59E8B),
                        Color.Transparent
                    ),
                    center = Offset(size.width * 1.02f, size.height * 0.02f),
                    radius = maxRadius * 0.75f
                )

                /**
                 * 斜向柔光，讓整張卡有玻璃霧面層次。
                 */
                val diagonalHighlight = Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.08f),
                        Color.White.copy(alpha = 0.025f),
                        Color.Transparent
                    ),
                    start = Offset(size.width * 0.08f, 0f),
                    end = Offset(size.width * 0.80f, size.height * 0.58f)
                )

                onDrawBehind {
                    drawRoundRect(
                        brush = baseGradient,
                        cornerRadius = cornerRadius
                    )

                    drawRoundRect(
                        brush = topLeftBlueGlow,
                        cornerRadius = cornerRadius
                    )

                    drawRoundRect(
                        brush = bottomRightRoseGlow,
                        cornerRadius = cornerRadius
                    )

                    drawRoundRect(
                        brush = topRightWarmGlow,
                        cornerRadius = cornerRadius
                    )

                    drawRoundRect(
                        brush = diagonalHighlight,
                        cornerRadius = cornerRadius
                    )
                }
            }
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.11f),
                shape = shape
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp, vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .background(
                        color = Color.White.copy(alpha = 0.10f),
                        shape = RoundedCornerShape(999.dp)
                    )
                    .border(
                        width = 1.dp,
                        color = Color.White.copy(alpha = 0.13f),
                        shape = RoundedCornerShape(999.dp)
                    )
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "GIFT PRICE UNLOCKED",
                    color = Color(0xFFFFE8A3),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.8.sp
                )
            }

            Spacer(Modifier.height(8.dp))

            Text(
                text = "Unlock Premium for only",
                color = Color(0xFFFFE8A3),
                fontSize = 18.sp,
                lineHeight = 23.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(5.dp))

            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = monthlyPrice,
                    color = Color(0xFFE45F69),
                    fontSize = 50.sp,
                    lineHeight = 54.sp,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center,
                    letterSpacing = (-1.4).sp
                )

                Spacer(Modifier.size(4.dp))

                Text(
                    text = monthlyUnit,
                    color = Color(0xFFE45F69),
                    fontSize = 23.sp,
                    lineHeight = 30.sp,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.padding(bottom = 5.dp)
                )
            }

            Spacer(Modifier.height(4.dp))

            Text(
                text = "Scan meals faster. Track macros smarter.\nStay on plan daily.",
                color = Color(0xFFEDE7FF),
                fontSize = 13.sp,
                lineHeight = 17.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
            )

            Spacer(Modifier.height(6.dp))

            Text(
                text = "$originalYearlyPrice/year",
                color = Color.White,
                fontSize = 16.sp,
                lineHeight = 21.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                modifier = Modifier.drawBehind {
                    val strikeY = size.height * 0.55f

                    drawLine(
                        color = Color(0xFFE45F69),
                        start = Offset(0f, strikeY),
                        end = Offset(size.width, strikeY),
                        strokeWidth = 3.dp.toPx()
                    )
                }
            )
        }
    }
}
@Composable
private fun OneTimeOfferUrgencyCard() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = Color.White,
                shape = RoundedCornerShape(20.dp)
            )
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        OneTimeOfferUrgencyRow(
            emoji = "☕",
            text = "Less than a coffee."
        )

        OneTimeOfferUrgencyRow(
            emoji = "⚠️",
            text = "Close this screen? This price is gone"
        )

        OneTimeOfferUrgencyRow(
            emoji = "\uD83D\uDE4B",
            text = "What are you waiting for?"
        )
    }
}
@Composable
private fun OneTimeOfferUrgencyRow(
    emoji: String,
    text: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = emoji,
            fontSize = 24.sp,
            modifier = Modifier.width(34.dp)
        )

        Spacer(modifier = Modifier.width(18.dp))

        Text(
            text = text,
            color = Color(0xFF111111),
            fontSize = 17.sp,
            lineHeight = 22.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
@Composable
private fun OneTimeOfferTrialCard(
    trialEnabled: Boolean,
    trialEligible: Boolean,
    trialEligibilityLoaded: Boolean,
    purchasing: Boolean,
    offerYearlyPrice: String,
    monthlyEquivalent: String,
    onTrialEnabledChange: (Boolean) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp)
                .biteCalClickable(enabled = !purchasing && trialEligible && trialEligibilityLoaded) {
                    onTrialEnabledChange(!trialEnabled)
                },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = when {
                    !trialEligibilityLoaded -> "Checking trial eligibility..."
                    !trialEligible -> "Trial already used"
                    trialEnabled -> "Free Trial Enabled"
                    else -> "Not Sure? Enable Free Trial"
                },
                color = Color(0xFF111111),
                fontSize = 21.sp,
                lineHeight = 26.sp,
                fontWeight = FontWeight.Normal,
                modifier = Modifier.weight(1f)
            )

            Switch(
                checked = trialEnabled,
                enabled = !purchasing && trialEligible && trialEligibilityLoaded,
                onCheckedChange = {
                    if (!purchasing && trialEligible && trialEligibilityLoaded) {
                        onTrialEnabledChange(it)
                    }
                },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = Color.Black,
                    checkedBorderColor = Color.Transparent,

                    uncheckedThumbColor = Color.White,
                    uncheckedTrackColor = Color(0xFFD4D4D8),
                    uncheckedBorderColor = Color.Transparent,

                    disabledCheckedThumbColor = Color.White,
                    disabledCheckedTrackColor = Color(0xFF18181B),
                    disabledCheckedBorderColor = Color.Transparent,

                    disabledUncheckedThumbColor = Color(0xFFA1A1AA),
                    disabledUncheckedTrackColor = Color(0xFFE5E7EB),
                    disabledUncheckedBorderColor = Color.Transparent
                )
            )
        }

        Spacer(Modifier.height(10.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White, RoundedCornerShape(18.dp))
                .border(
                    width = 2.dp,
                    color = Color(0xFF1C1923),
                    shape = RoundedCornerShape(18.dp)
                )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(28.dp)
                    .background(
                        color = Color(0xFF1C1923),
                        shape = RoundedCornerShape(
                            topStart = 18.dp,
                            topEnd = 18.dp
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (trialEnabled && trialEligible) "FREE TRIAL" else "LOWEST PRICE EVER",
                    color = Color.White,
                    fontSize = 14.sp,
                    lineHeight = 17.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    letterSpacing = 0.4.sp
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 22.dp, vertical = 15.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "Yearly Plan",
                        color = Color(0xFF111111),
                        fontSize = 19.sp,
                        lineHeight = 23.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(Modifier.height(4.dp))

                    Text(
                        text = "12mo · $offerYearlyPrice",
                        color = Color(0xFF71717A),
                        fontSize = 16.sp,
                        lineHeight = 20.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(Modifier.width(12.dp))

                Text(
                    text = monthlyEquivalent,
                    color = Color(0xFF111111),
                    fontSize = 20.sp,
                    lineHeight = 24.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.End
                )
            }
        }
    }
}
@Composable
private fun PhonePreviewMock(
    modifier: Modifier = Modifier
) {
    DeviceFrameIPhone(
        modifier = modifier,
        frameColor = Color(0xFFE3E5EA),
        bezelColor = Color(0xFF09090B),
        cornerRadius = 42.dp,
        frameThickness = 3.dp,
        bezelThickness = 8.dp,
        islandWidthFraction = 0.32f,
        islandHeight = 17.dp,
        islandTopOffset = 0.dp,
        islandStrokeWidth = 1.dp,
        islandStrokeAlpha = 0.16f,
        islandStrokeColor = Color.White,
        frontCameraDotAlignRight = true,
        frontCameraDotRightInset = 5.dp,
        contentTopExtraPadding = 9.dp,
        contentBottomExtraPadding = 2.dp,
        powerButtonLengthFraction = 0.10f,
        volumeButtonsCenterBias = 0.20f,
        powerButtonCenterBias = 0.20f,
        showFrontCameraDot = true
    ) {
        LandingSlideshow(
            modifier = Modifier.fillMaxSize(),
            slides = listOf(
                SlideItem(R.drawable.meal_1, contentDescription = "onboard_intro_slide_1"),
                SlideItem(R.drawable.meal_2, contentDescription = "onboard_intro_slide_2"),
                SlideItem(R.drawable.meal_3, contentDescription = "onboard_intro_slide_3")
            ),
            autoPlay = true,
            autoPlayIntervalMs = 2800L
        )
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
private fun BoxScope.OnboardPaywallBottomCta(
    buttonText: String,
    helperText: String,
    loading: Boolean,
    onClick: () -> Unit,
    helperTextColor: Color = Color(0xFF71717A),
    buttonShape: Shape = RoundedCornerShape(999.dp)
) {
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
            loading = loading,
            onClick = onClick,
            shape = buttonShape,
            modifier = Modifier
                .fillMaxWidth()
                .height(68.dp)
        )

        Spacer(Modifier.height(14.dp))

        Text(
            text = helperText,
            color = helperTextColor,
            fontSize = 14.sp,
            lineHeight = 17.sp,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.height(17.dp)
        )
    }
}
@Composable
private fun PrimaryBlackButton(
    text: String,
    loading: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(999.dp)
) {
    Button(
        onClick = rememberClickWithHaptic(onClick = onClick),
        enabled = !loading,
        modifier = modifier,
        shape = shape,
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
                fontSize = 19.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )
        }
    }
}
