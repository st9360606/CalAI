package com.calai.bitecal.ui.home.ui.settings.referral

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.calai.bitecal.data.referral.api.ReferralClaimItemDto
import com.calai.bitecal.ui.home.components.LightHomeBackground

private val ReferralPageText = Color(0xFF111114)
private val ReferralMutedText = Color(0xFF7C8490)
private val ReferralDivider = Color(0xFFE5E7EB)
private val ReferralCardWhite = Color.White
private val ReferralBlack = Color(0xFF111114)
private val ReferralGold = Color(0xFFFFE7A3)
private val ReferralSoftGold = Color(0xFFFFF7D6)

@Suppress("UNUSED_PARAMETER")
@Composable
fun ReferralScreen(
    promoCode: String,
    successCount: Long,
    pendingCount: Long,
    rejectedCount: Long,
    recentClaims: List<ReferralClaimItemDto>,
    claimInFlight: Boolean,
    error: String?,
    onBack: () -> Unit,
    onSubmitClaim: (String) -> Unit
) {
    val context = LocalContext.current

    Box(Modifier.fillMaxSize()) {
        LightHomeBackground()

        Scaffold(
            containerColor = Color.Transparent
        ) { inner ->
            LazyColumn(
                modifier = Modifier
                    .padding(inner)
                    .fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 20.dp,
                    end = 20.dp,
                    top = 10.dp,
                    bottom = 32.dp
                ),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    ReferralTopBar(
                        title = "Referral",
                        onBack = onBack
                    )
                }

                item {
                    ReferralHeroCard(
                        promoCode = promoCode
                    )
                }

                item {
                    ShareReferralButton(
                        onClick = {
                            shareReferral(
                                context = context,
                                promoCode = promoCode
                            )
                        }
                    )
                }

                item {
                    HowReferralWorksCard()
                }

                item {
                    RewardRuleNoticeCard()
                }
            }
        }
    }
}

@Composable
private fun ReferralTopBar(
    title: String,
    onBack: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(Color.White)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = ReferralPageText
            )
        }

        Spacer(Modifier.size(12.dp))

        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall.copy(
                color = ReferralPageText,
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
                lineHeight = 30.sp
            )
        )
    }
}

@Composable
private fun ReferralHeroCard(
    promoCode: String
) {
    val clipboardManager = LocalClipboardManager.current
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val cardScale by animateFloatAsState(
        targetValue = if (pressed) 0.985f else 1f,
        animationSpec = spring(
            dampingRatio = 0.78f,
            stiffness = 520f
        ),
        label = "ReferralHeroCardScale"
    )

    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier
            .fillMaxWidth()
            .scale(cardScale)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = {
                    clipboardManager.setText(AnnotatedString(promoCode))
                }
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF24294F),
                            Color(0xFF352855),
                            Color(0xFF633A4B),
                            Color(0xFF603844)
                        )
                    )
                )
                .border(
                    width = 1.dp,
                    color = Color.White.copy(alpha = 0.14f),
                    shape = RoundedCornerShape(28.dp)
                )
                .padding(18.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top
                ) {
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.14f))
                                    .border(
                                        width = 1.dp,
                                        color = Color.White.copy(alpha = 0.18f),
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "🎁",
                                    fontSize = 17.sp,
                                    lineHeight = 20.sp
                                )
                            }

                            Spacer(Modifier.size(8.dp))

                            Text(
                                text = "Premium reward",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    color = ReferralGold,
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
                                    .padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }

                        Spacer(Modifier.height(14.dp))

                        Text(
                            text = "Invite friends",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                color = Color.White,
                                fontWeight = FontWeight.Black,
                                fontSize = 30.sp,
                                lineHeight = 34.sp
                            )
                        )

                        Spacer(Modifier.height(3.dp))

                        Text(
                            text = "Get 30 days free",
                            style = MaterialTheme.typography.headlineSmall.copy(
                                color = Color.White,
                                fontWeight = FontWeight.Black,
                                fontSize = 24.sp,
                                lineHeight = 29.sp
                            )
                        )

                        Spacer(Modifier.height(8.dp))

                        Text(
                            text = "Reward unlocks after your friend subscribes with no refund.",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = Color.White.copy(alpha = 0.82f),
                                fontWeight = FontWeight.Medium,
                                fontSize = 13.sp,
                                lineHeight = 18.sp
                            )
                        )
                    }

                    ReferralRewardBadge(
                        modifier = Modifier.offset(y = 2.dp)
                    )
                }

                Spacer(Modifier.height(18.dp))

                PromoCodePanel(
                    promoCode = promoCode,
                    onCopy = {
                        clipboardManager.setText(AnnotatedString(promoCode))
                    }
                )
            }
        }
    }
}

@Composable
private fun ReferralRewardBadge(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.size(88.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(88.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.15f))
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
                    .background(ReferralBlack)
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
                .size(26.dp)
                .align(Alignment.BottomEnd)
                .offset(x = 1.dp, y = 1.dp)
                .clip(CircleShape)
                .background(ReferralGold)
                .border(
                    width = 1.dp,
                    color = Color.White.copy(alpha = 0.75f),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "🎁",
                fontSize = 13.sp,
                lineHeight = 15.sp
            )
        }
    }
}

@Composable
private fun PromoCodePanel(
    promoCode: String,
    onCopy: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(Color.White.copy(alpha = 0.13f))
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.16f),
                shape = RoundedCornerShape(22.dp)
            )
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = "Your promo code",
                style = MaterialTheme.typography.labelMedium.copy(
                    color = Color.White.copy(alpha = 0.72f),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.sp,
                    lineHeight = 14.sp
                )
            )

            Spacer(Modifier.height(4.dp))

            Text(
                text = promoCode.ifBlank { "—" },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.headlineSmall.copy(
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 22.sp,
                    lineHeight = 27.sp
                )
            )
        }

        Spacer(Modifier.size(12.dp))

        Row(
            modifier = Modifier
                .height(40.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(Color.White)
                .clickable(onClick = onCopy)
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.ContentCopy,
                contentDescription = "Copy",
                tint = ReferralBlack,
                modifier = Modifier.size(17.dp)
            )

            Spacer(Modifier.size(7.dp))

            Text(
                text = "Copy",
                style = MaterialTheme.typography.labelLarge.copy(
                    color = ReferralBlack,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            )
        }
    }
}

@Composable
private fun ShareReferralButton(
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = RoundedCornerShape(999.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = ReferralBlack,
            contentColor = Color.White
        )
    ) {
        Text(
            text = "Share",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        )
    }
}

@Composable
private fun HowReferralWorksCard() {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = ReferralCardWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "How it works",
                style = MaterialTheme.typography.titleMedium.copy(
                    color = ReferralPageText,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    lineHeight = 22.sp
                )
            )

            Spacer(Modifier.height(12.dp))

            ReferralStepRow(
                number = "1",
                title = "Share your promo code",
                subtitle = "Send your code to a friend who wants to track food smarter."
            )

            StepDivider()

            ReferralStepRow(
                number = "2",
                title = "Your friend subscribes",
                subtitle = "They start a paid BiteCal AI subscription using your invite."
            )

            StepDivider()

            ReferralStepRow(
                number = "3",
                title = "No refund, then reward",
                subtitle = "After the subscription is verified with no refund, your premium period is extended."
            )
        }
    }
}

@Composable
private fun RewardRuleNoticeCard() {
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = ReferralSoftGold),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.78f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "i",
                    style = MaterialTheme.typography.labelLarge.copy(
                        color = ReferralBlack,
                        fontWeight = FontWeight.Black,
                        fontSize = 15.sp
                    )
                )
            }

            Spacer(Modifier.size(12.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "Reward rule",
                    style = MaterialTheme.typography.titleSmall.copy(
                        color = ReferralPageText,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        lineHeight = 18.sp
                    )
                )

                Spacer(Modifier.height(3.dp))

                Text(
                    text = "You’ll receive the reward only after your invited friend subscribes and does not request a refund.",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color(0xFF6B5A24),
                        fontWeight = FontWeight.Medium,
                        fontSize = 12.sp,
                        lineHeight = 17.sp
                    )
                )
            }
        }
    }
}

@Composable
private fun ReferralStepRow(
    number: String,
    title: String,
    subtitle: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(ReferralBlack),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = number,
                style = MaterialTheme.typography.labelMedium.copy(
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 12.sp,
                    lineHeight = 14.sp
                )
            )
        }

        Spacer(Modifier.size(12.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = ReferralPageText,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    lineHeight = 18.sp
                )
            )

            Spacer(Modifier.height(2.dp))

            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = ReferralMutedText,
                    fontWeight = FontWeight.Medium,
                    fontSize = 12.sp,
                    lineHeight = 17.sp
                )
            )
        }
    }
}

@Composable
private fun StepDivider() {
    Row(
        modifier = Modifier.fillMaxWidth()
    ) {
        Spacer(Modifier.size(14.dp))

        Box(
            modifier = Modifier
                .padding(vertical = 3.dp)
                .height(8.dp)
                .border(
                    width = 1.dp,
                    color = ReferralDivider,
                    shape = RoundedCornerShape(999.dp)
                )
        )

        Spacer(Modifier.size(26.dp))
    }
}

private fun shareReferral(
    context: Context,
    promoCode: String
) {
    val safePromoCode = promoCode.trim().ifBlank { "BITE-CAL" }

    val appUrl = "https://play.google.com/store/apps/details?id=${context.packageName}"

    val shareText = buildString {
        appendLine("Join me on BiteCal AI! My referral code is:")
        appendLine(safePromoCode)
        appendLine()
        append(appUrl)
    }

    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, "Join me on BiteCal AI")
        putExtra(Intent.EXTRA_TEXT, shareText)
    }

    val chooserIntent = Intent.createChooser(shareIntent, "Share via").apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    runCatching {
        context.startActivity(chooserIntent)
    }.onFailure {
        Toast.makeText(
            context.applicationContext,
            "Unable to open share options.",
            Toast.LENGTH_SHORT
        ).show()
    }
}

