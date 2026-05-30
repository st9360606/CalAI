package com.calai.bitecal.ui.onboarding.referralcode

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.calai.bitecal.R
import com.calai.bitecal.ui.common.OnboardingProgress
import com.calai.bitecal.ui.common.haptic.rememberClickWithHaptic

private val ReferralText = Color(0xFF111114)
private val ReferralMuted = Color(0xFFB8BAC2)
private val ReferralBorder = Color(0xFF111114)
private val ReferralSoftBorder = Color(0xFFF1F1F4)
private val ReferralInputBg = Color(0xFFFAFAFC)
private val ReferralDisabledButton = Color(0xFFDADBE2)

@Composable
fun OnboardReferralCodeRoute(
    vm: OnboardReferralCodeViewModel,
    onBack: () -> Unit,
    onNext: () -> Unit,
) {
    val ui by vm.uiState.collectAsState()

    OnboardReferralCodeScreen(
        ui = ui,
        onCodeChanged = vm::onCodeChanged,
        onSubmit = { vm.submit() },
        onContinue = { vm.continueNext(onNext) },
        onSkipAndContinue = { vm.skipAndContinue(onNext) },
        onBack = onBack,
    )
}

@Composable
fun OnboardReferralCodeScreen(
    ui: OnboardReferralCodeUiState,
    onCodeChanged: (String) -> Unit,
    onSubmit: () -> Unit,
    onContinue: () -> Unit,
    onSkipAndContinue: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        containerColor = Color.White,
        topBar = {
            ReferralTopBar(onBack = onBack)
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .imePadding()
                    .navigationBarsPadding()
                    .padding(
                        start = 20.dp,
                        end = 20.dp,
                        bottom = 8.dp
                    ),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Button(
                    onClick = rememberClickWithHaptic(onClick = onContinue),
                    enabled = !ui.submitting,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(68.dp),
                    shape = RoundedCornerShape(999.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Black,
                        contentColor = Color.White,
                        disabledContainerColor = Color.Black,
                        disabledContentColor = Color.White,
                    ),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 0.dp),
                ) {
                    Text(
                        text = stringResource(R.string.continue_text),
                        color = Color.White,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                    )
                }

                Spacer(Modifier.height(8.dp))

                TextButton(
                    onClick = rememberClickWithHaptic(onClick = onSkipAndContinue),
                    enabled = !ui.submitting,
                    modifier = Modifier.height(44.dp),
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = Color(0xFF8F98A3),
                        disabledContentColor = Color(0xFF8F98A3).copy(alpha = 0.45f)
                    )
                ) {
                    Text(
                        text = stringResource(R.string.onboard_referral_code_skip),
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Medium,
                            letterSpacing = 0.1.sp
                        ),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    ) { inner ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
                .padding(horizontal = 30.dp),
        ) {
            Spacer(Modifier.height(24.dp))

            Text(
                text = stringResource(R.string.onboard_referral_code_input),
                fontSize = 34.sp,
                fontWeight = FontWeight.ExtraBold,
                lineHeight = 41.sp,
                color = ReferralText,
                letterSpacing = 0.1.sp,
            )

            Spacer(Modifier.height(12.dp))

            Text(
                text = stringResource(R.string.onboard_referral_code_subtitle),
                fontSize = 19.sp,
                fontWeight = FontWeight.Normal,
                lineHeight = 26.sp,
                color = ReferralText.copy(alpha = 0.88f),
            )

            Spacer(Modifier.weight(0.78f))

            ReferralCodeInputCard(
                code = ui.code,
                applied = ui.applied,
                submitting = ui.submitting,
                submitEnabled = ui.submitEnabled,
                onCodeChanged = onCodeChanged,
                onSubmit = onSubmit,
            )

            if (ui.applied) {
                Spacer(Modifier.height(28.dp))
                ReferralAppliedRow()
            }

            ui.errorCode?.let { errorCode ->
                Spacer(Modifier.height(14.dp))
                Text(
                    text = referralErrorMessage(errorCode),
                    color = Color(0xFFCC3D3D),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    lineHeight = 19.sp,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                )
            }

            Spacer(Modifier.weight(1.37f))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReferralTopBar(onBack: () -> Unit) {
    TopAppBar(
        modifier = Modifier.padding(start = 16.dp, end = 16.dp),
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.White,
            navigationIconContentColor = ReferralText,
        ),
        navigationIcon = {
            IconButton(onClick = rememberClickWithHaptic(onClick = onBack)) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFFF1F3F7)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.common_back),
                        tint = ReferralText,
                    )
                }
            }
        },
        title = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(end = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OnboardingProgress(
                    stepIndex = 12,
                    totalSteps = 12,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
    )
}

@Composable
private fun ReferralCodeInputCard(
    code: String,
    applied: Boolean,
    submitting: Boolean,
    submitEnabled: Boolean,
    onCodeChanged: (String) -> Unit,
    onSubmit: () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }

    val expanded = focused || code.isNotBlank() || applied
    val shape = RoundedCornerShape(20.dp)

    val borderColor = when {
        applied -> ReferralSoftBorder
        expanded -> ReferralBorder
        else -> Color.Transparent
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .clip(shape)
            .background(
                if (applied || !expanded) {
                    ReferralInputBg
                } else {
                    Color.White
                }
            )
            .border(
                width = if (expanded && !applied) 2.5.dp else 1.dp,
                color = borderColor,
                shape = shape,
            )
            .padding(start = 20.dp, end = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BasicTextField(
            value = code,
            onValueChange = onCodeChanged,
            enabled = !applied && !submitting,
            singleLine = true,
            textStyle = TextStyle(
                color = if (applied) {
                    ReferralText.copy(alpha = 0.56f)
                } else {
                    ReferralText
                },
                fontSize = if (expanded) 20.sp else 19.sp,
                fontWeight = FontWeight.Normal,
                letterSpacing = 1.sp,
            ),
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Characters,
                keyboardType = KeyboardType.Ascii,
            ),
            modifier = Modifier
                .weight(1f)
                .padding(end = 10.dp)
                .onFocusChanged { focusState ->
                    focused = focusState.isFocused
                },
            decorationBox = { innerTextField ->
                if (expanded) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text(
                            text = stringResource(R.string.onboard_referral_code_input_label),
                            color = ReferralMuted,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            lineHeight = 18.sp,
                        )

                        Spacer(Modifier.height(2.dp))

                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.CenterStart,
                        ) {
                            innerTextField()
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        Text(
                            text = stringResource(R.string.onboard_referral_code_placeholder),
                            color = ReferralMuted.copy(alpha = 0.72f),
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Medium,
                        )

                        innerTextField()
                    }
                }
            },
        )

        if (!applied) {
            Button(
                onClick = rememberClickWithHaptic(onClick = onSubmit),
                enabled = submitEnabled,
                modifier = Modifier
                    .width(120.dp)
                    .height(48.dp),
                shape = RoundedCornerShape(999.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ReferralText,
                    contentColor = Color.White,
                    disabledContainerColor = ReferralDisabledButton,
                    disabledContentColor = Color.White,
                ),
                contentPadding = ButtonDefaults.ContentPadding,
            ) {
                Text(
                    text = if (submitting) {
                        stringResource(R.string.onboard_referral_code_submitting)
                    } else {
                        stringResource(R.string.onboard_referral_code_submit)
                    },
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.1.sp,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun ReferralAppliedRow() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(CircleShape)
                .background(ReferralText),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(15.dp),
            )
        }

        Spacer(Modifier.width(10.dp))

        Text(
            text = stringResource(R.string.onboard_referral_code_applied),
            color = ReferralText,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.2.sp,
        )
    }
}

@Composable
private fun referralErrorMessage(errorCode: String): String {
    return when (errorCode) {
        OnboardReferralCodeViewModel.ERROR_INVALID_FORMAT -> stringResource(R.string.onboard_referral_code_error_invalid_format)
        "INVALID_PROMO_CODE" -> stringResource(R.string.onboard_referral_code_error_invalid)
        "SELF_REFERRAL" -> stringResource(R.string.onboard_referral_code_error_self_referral)
        "INVITEE_ALREADY_CLAIMED" -> stringResource(R.string.onboard_referral_code_error_already_claimed)
        "INVITEE_ALREADY_SUBSCRIBED",
        "PREMIUM_ACTIVE",
        "TRIAL_ACTIVE",
        "PAYMENT_ISSUE",
        "PAYMENT_RECOVERY_REQUIRED",
        "HAS_PAID_HISTORY" -> stringResource(R.string.onboard_referral_code_error_already_subscribed)
        "REFERRAL_DISABLED" -> stringResource(R.string.onboard_referral_code_error_disabled)
        "RISK_REJECTED", "ABUSE_RISK" -> stringResource(R.string.onboard_referral_code_error_risk)
        OnboardReferralCodeViewModel.ERROR_NETWORK -> stringResource(R.string.onboard_referral_code_error_network)
        else -> stringResource(R.string.onboard_referral_code_error_unknown)
    }
}
