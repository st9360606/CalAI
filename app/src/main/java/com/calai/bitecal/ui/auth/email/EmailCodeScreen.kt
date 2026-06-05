package com.calai.bitecal.ui.auth.email

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.calai.bitecal.R
import com.calai.bitecal.ui.common.design.BiteCalPlainBackTopBar
import com.calai.bitecal.ui.common.design.BiteCalScreenFrame
import com.calai.bitecal.ui.common.haptic.rememberClickWithHaptic
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.semantics.Role
import kotlin.math.max
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmailCodeScreen(
    vm: EmailSignInViewModel,
    email: String,
    onBack: () -> Unit,
    onSuccess: () -> Unit
) {
    val state = vm.code.collectAsState().value
    LaunchedEffect(email) {
        if (state == null && email.isNotBlank()) vm.prepareCode(email)
    }

    val ui = vm.code.collectAsState().value
    val scope = rememberCoroutineScope()
    val focus = remember { FocusRequester() }
    val kb = LocalSoftwareKeyboardController.current

    // 進畫面稍等一下自動聚焦
    LaunchedEffect(ui?.email) {
        delay(120); focus.requestFocus()
    }
    // 出錯自動回焦（ViewModel 已把 code 清空）
    LaunchedEffect(ui?.error) {
        if (ui?.error != null) { delay(80); focus.requestFocus() }
    }

    // ===== 本地倒數 fallback（與 VM 的 canResendInSec 取最大值）=====
    var localLeft by remember { mutableIntStateOf(0) }
    LaunchedEffect(localLeft) {
        if (localLeft > 0) { delay(1000); localLeft -= 1 }
    }
    val vmLeft = ui?.canResendInSec ?: 0
    val left = max(vmLeft, localLeft)
    val canResend = left == 0
    val resendLabel = if (canResend) stringResource(R.string.resend) else stringResource(R.string.resend_countdown, left)

    // 錯誤訊息用 stringResource 對應
    val errorText: String? = when (ui?.error) {
        EmailCodeError.INVALID_CODE -> stringResource(R.string.err_otp_invalid)
        EmailCodeError.TOO_MANY_ATTEMPTS -> stringResource(R.string.err_otp_too_many_attempts)
        EmailCodeError.NETWORK -> stringResource(R.string.err_network_io)
        EmailCodeError.SERVER -> stringResource(R.string.err_server_unavailable)
        EmailCodeError.UNKNOWN -> ui?.errorMsg ?: stringResource(R.string.err_generic)
        null -> null
    }

    Scaffold(
        containerColor = Color.White,
        topBar = {
            BiteCalPlainBackTopBar(onBack = onBack)
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = BiteCalScreenFrame.onboardingHorizontal),
            horizontalAlignment = Alignment.Start
        ) {
            Spacer(Modifier.height(12.dp))

            Text(
                stringResource(R.string.confirm_email_title),
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 36.sp,
                color = Color(0xFF111114)
            )

            Spacer(Modifier.height(8.dp))

            Text(
                stringResource(R.string.confirm_email_hint),
                style = MaterialTheme.typography.bodyMedium.copy(color = Color.Gray)
            )
            Text(
                maskEmail(ui?.email ?: email),
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = Color.Black,
                    fontWeight = FontWeight.SemiBold
                )
            )

            Spacer(Modifier.height(24.dp))

            // === 4 格 OTP ===
            BasicTextField(
                value = ui?.code.orEmpty(),
                onValueChange = { vm.onCodeChange(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focus),
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Done,
                    keyboardType = KeyboardType.Number
                ),
                keyboardActions = KeyboardActions(onDone = {
                    vm.verify {
                        kb?.hide()
                        onSuccess()
                    }
                }),
                decorationBox = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        val code = ui?.code.orEmpty()
                        repeat(4) { idx ->
                            val char = code.getOrNull(idx)?.toString().orEmpty()
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(64.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Color.White)
                                    .border(
                                        width = 1.dp,
                                        color = if (char.isEmpty()) Color(0xFFDDDDDD) else Color.Black,
                                        shape = RoundedCornerShape(14.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = char,
                                    style = TextStyle(
                                        fontSize = 28.sp,
                                        textAlign = TextAlign.Center,
                                        color = Color(0xFF111114)
                                    )
                                )
                            }
                        }
                    }
                },
                textStyle = LocalTextStyle.current.copy(color = Color.Transparent)
            )

            // 輸入滿 4 碼就自動送驗
            LaunchedEffect(ui?.code) {
                if ((ui?.code?.length ?: 0) == 4) {
                    kb?.hide()
                    vm.verify { onSuccess() }
                }
            }

            Spacer(Modifier.height(28.dp))
            HorizontalDivider(color = Color(0x1A000000))
            Spacer(Modifier.height(12.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.didnt_receive_code),
                    style = MaterialTheme.typography.bodyMedium.copy(color = Color.Gray)
                )

                Spacer(Modifier.width(6.dp))

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(
                            enabled = canResend,
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            role = Role.Button,
                            onClick = rememberClickWithHaptic {
                                localLeft = 60
                                vm.resend()
                                scope.launch {
                                    delay(80)
                                    focus.requestFocus()
                                }
                            }
                        )
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = resendLabel,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = Color(0xFF111114),
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }
            }

            AnimatedVisibility(visible = errorText != null) {
                Text(
                    text = errorText ?: "",
                    color = Color(0xFFD32F2F),
                    modifier = Modifier.padding(top = 12.dp)
                )
            }
        }
    }
}

/** 把 aaa@bbb.com → aa•••@bbb.com（簡單遮罩） */
private fun maskEmail(email: String): String {
    val at = email.indexOf('@')
    if (at <= 1) return "••••"
    val head = email.take(2)
    val tail = email.substring(at)
    return buildString {
        append(head)
        append("•".repeat((at - 2).coerceAtLeast(1)))
        append(tail)
    }
}
