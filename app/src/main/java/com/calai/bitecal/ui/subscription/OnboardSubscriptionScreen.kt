package com.calai.bitecal.ui.subscription

import android.app.Activity
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.calai.bitecal.data.billing.BiteCalBillingProducts
import com.calai.bitecal.data.entitlement.api.EntitlementSyncResponse

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

    var step by remember { mutableStateOf(OnboardPaywallStep.Intro) }
    var trialEnabled by remember { mutableStateOf(true) }

    Box(modifier = Modifier.fillMaxSize()) {
        when (step) {
            OnboardPaywallStep.Intro -> {
                OnboardSubscriptionIntro(
                    purchasing = ui.purchasing,
                    onClose = onCloseToSignIn,
                    onContinue = {
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
            .navigationBarsPadding()
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
                .padding(top = 118.dp, bottom = 26.dp),
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

            Spacer(Modifier.weight(1f))

            PrimaryBlackButton(
                text = "Continue",
                loading = purchasing,
                onClick = onContinue
            )

            Spacer(Modifier.height(22.dp))

            Text(
                text = "Just NT$1,020.00 per 年 (NT$85.00/mo)",
                color = Color(0xFF71717A),
                fontSize = 21.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun OnboardDiscountSpinScreen(
    onContinue: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .clickable(onClick = onContinue)
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp)
                .padding(top = 130.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Spin to unlock an\nexclusive discount",
                color = Color.Black,
                fontSize = 38.sp,
                lineHeight = 46.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Start,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(150.dp))

            DiscountWheelMock()
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
                    text = "NT$1,010.00",
                    color = Color(0xFF9CA3AF),
                    fontSize = 25.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "  NT$50.83 /mo",
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
private fun DiscountWheelMock() {
    Canvas(modifier = Modifier.size(330.dp)) {
        val radius = size.minDimension / 2f
        val center = Offset(size.width / 2f, size.height / 2f)
        val colors = listOf(
            Color(0xFF1C1923),
            Color.White,
            Color(0xFF1C1923),
            Color.White,
            Color(0xFF1C1923),
            Color.White
        )

        repeat(6) { index ->
            drawArc(
                color = colors[index],
                startAngle = index * 60f - 90f,
                sweepAngle = 60f,
                useCenter = true
            )
        }

        drawCircle(
            color = Color(0xFF1C1923),
            radius = radius,
            center = center,
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 18f)
        )

        drawCircle(
            color = Color(0xFF1C1923),
            radius = 42f,
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
                    text = "12mo · NT$610.00",
                    color = Color(0xFF71717A),
                    fontSize = 22.sp
                )
            }

            Text(
                text = "NT$50.83 /mo",
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
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(86.dp)
            .background(Color.Black, RoundedCornerShape(24.dp))
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
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold
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
            .height(76.dp)
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
