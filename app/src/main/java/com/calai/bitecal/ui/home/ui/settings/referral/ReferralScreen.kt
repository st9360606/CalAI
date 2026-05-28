package com.calai.bitecal.ui.home.ui.settings.referral

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.calai.bitecal.R
import com.calai.bitecal.ui.common.haptic.biteCalClickable
import com.calai.bitecal.data.referral.api.ReferralClaimItemDto
import com.calai.bitecal.ui.home.components.LightHomeBackground
import com.calai.bitecal.ui.home.ui.components.ProfileEditTopBar
import kotlinx.coroutines.delay
private val ReferralPageText = Color(0xFF111114)
private val ReferralMutedText = Color(0xFF7C8490)
private val ReferralCardWhite = Color.White
private val ReferralBlack = Color(0xFF111114)
private val ReferralGold = Color(0xFFFFE7A3)

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
    var showCopyTopToast by remember { mutableStateOf(false) }
    var copyToastTick by remember { mutableIntStateOf(0) }

    LaunchedEffect(copyToastTick) {
        if (copyToastTick > 0) {
            showCopyTopToast = true
            delay(1600)
            showCopyTopToast = false
        }
    }

    Box(Modifier.fillMaxSize()) {
        LightHomeBackground()

        Scaffold(
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            topBar = {
                ProfileEditTopBar(
                    title = stringResource(R.string.referral_title),
                    onBack = onBack
                )
            }
        ) { inner ->
            LazyColumn(
                modifier = Modifier
                    .padding(inner)
                    .fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 20.dp,
                    end = 20.dp,
                    top = 8.dp,
                    bottom = 32.dp
                ),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    ReferralHeroCard(
                        promoCode = promoCode,
                        onCopied = {
                            copyToastTick += 1
                        }
                    )
                }

                item {
                    ShareReferralButton(
                        modifier = Modifier.padding(top = 10.dp, bottom = 12.dp),
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
            }
        }

        if (showCopyTopToast) {
            CopyTopToast(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(top = 50.dp)
            )
        }
    }
}
@Composable
private fun ReferralHeroCard(
    promoCode: String,
    onCopied: () -> Unit
) {
    val clipboardManager = LocalClipboardManager.current

    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth()
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
                                text = stringResource(R.string.referral_premium_reward),
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
                            text = stringResource(R.string.referral_invite_friends),
                            style = MaterialTheme.typography.headlineMedium.copy(
                                color = Color.White,
                                fontWeight = FontWeight.Black,
                                fontSize = 30.sp,
                                lineHeight = 34.sp
                            )
                        )

                        Spacer(Modifier.height(3.dp))

                        Text(
                            text = stringResource(R.string.referral_earn_30_days_free),
                            style = MaterialTheme.typography.headlineSmall.copy(
                                color = Color.White,
                                fontWeight = FontWeight.Black,
                                fontSize = 24.sp,
                                lineHeight = 29.sp
                            )
                        )

                        Spacer(Modifier.height(8.dp))

                        Text(
                            text = stringResource(R.string.referral_reward_unlocks_after_no_refund),
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

                Spacer(Modifier.height(16.dp))

                PromoCodePanel(
                    promoCode = promoCode,
                    onCopy = {
                        copyReferralCode(
                            clipboardManager = clipboardManager,
                            promoCode = promoCode
                        )
                        onCopied()
                    }
                )
            }
        }
    }
}

private fun copyReferralCode(
    clipboardManager: ClipboardManager,
    promoCode: String
) {
    val safePromoCode = promoCode.trim().ifBlank { "BITE-CAL" }

    clipboardManager.setText(
        AnnotatedString(safePromoCode)
    )
}

@Composable
private fun CopyTopToast(
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(ReferralBlack.copy(alpha = 0.94f))
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.12f),
                shape = RoundedCornerShape(999.dp)
            )
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Outlined.ContentCopy,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(16.dp)
        )

        Spacer(Modifier.size(8.dp))

        Text(
            text = stringResource(R.string.referral_code_copied),
            style = MaterialTheme.typography.labelLarge.copy(
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                lineHeight = 16.sp
            )
        )
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
            .padding(horizontal = 14.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = stringResource(R.string.referral_your_promo_code),
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
                .biteCalClickable(onClick = onCopy)
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.ContentCopy,
                contentDescription = stringResource(R.string.referral_copy),
                tint = ReferralBlack,
                modifier = Modifier.size(17.dp)
            )

            Spacer(Modifier.size(7.dp))

            Text(
                text = stringResource(R.string.referral_copy),
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
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = RoundedCornerShape(999.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = ReferralBlack,
            contentColor = Color.White
        ),
        contentPadding = PaddingValues(horizontal = 16.dp)
    ) {
        Text(
            text = stringResource(R.string.referral_share),
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
                text = stringResource(R.string.referral_how_it_works),
                style = MaterialTheme.typography.titleMedium.copy(
                    color = ReferralPageText,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    lineHeight = 22.sp
                )
            )

            Spacer(Modifier.height(15.dp))

            ReferralStepRow(
                number = "1",
                title = stringResource(R.string.referral_step_1_title),
                subtitle = stringResource(R.string.referral_step_1_subtitle)
            )

            Spacer(Modifier.height(15.dp))

            ReferralStepRow(
                number = "2",
                title = stringResource(R.string.referral_step_2_title),
                subtitle = stringResource(R.string.referral_step_2_subtitle)
            )

            Spacer(Modifier.height(15.dp))

            ReferralStepRow(
                number = "3",
                title = stringResource(R.string.referral_step_3_title),
                subtitle = stringResource(R.string.referral_step_3_subtitle)
            )
        }
    }
}



@Composable
private fun ReferralStepRow(
    number: String,
    title: String,
    subtitle: String,
    circleColor: Color = ReferralBlack
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(circleColor),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = number,
                style = MaterialTheme.typography.labelMedium.copy(
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 11.sp,
                    lineHeight = 13.sp
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
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            )
        }
    }
}
private fun shareReferral(
    context: Context,
    promoCode: String
) {
    val safePromoCode = promoCode.trim().ifBlank { "BITE-CAL" }

    val appUrl = "https://play.google.com/store/apps/details?id=${context.packageName}"

    val shareText = context.getString(
        R.string.referral_share_text,
        safePromoCode,
        appUrl
    )

    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(
            Intent.EXTRA_SUBJECT,
            context.getString(R.string.referral_share_subject)
        )
        putExtra(Intent.EXTRA_TEXT, shareText)
    }

    val chooserIntent = Intent.createChooser(
        shareIntent,
        context.getString(R.string.referral_share_chooser_title)
    ).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    runCatching {
        context.startActivity(chooserIntent)
    }.onFailure {
        Toast.makeText(
            context.applicationContext,
            context.getString(R.string.referral_unable_to_open_share_options),
            Toast.LENGTH_SHORT
        ).show()
    }
}
