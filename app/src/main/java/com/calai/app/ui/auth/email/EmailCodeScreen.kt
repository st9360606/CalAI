package com.calai.app.ui.auth.email

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.calai.app.R

private fun maskEmail(email: String): String {
    val at = email.indexOf('@')
    if (at <= 1) return email
    val name = email.substring(0, at)
    val masked = name.take(2) + "•".repeat((name.length - 2).coerceAtLeast(1))
    return masked + email.substring(at)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmailCodeScreen(
    vm: EmailSignInViewModel,
    email: String,
    onBack: () -> Unit,
    onSuccess: () -> Unit
) {
    val state by vm.code.collectAsState()
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                }
            )
        }
    ) { pad ->
        val s = state ?: return@Scaffold
        Column(
            modifier = Modifier
                .padding(pad)
                .padding(horizontal = 20.dp)
                .fillMaxSize()
        ) {
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(R.string.email_confirm_title),
                style = MaterialTheme.typography.headlineLarge.copy(fontSize = 28.sp)
            )
            Spacer(Modifier.height(8.dp))
            Text(
                // "請輸入發送到 st••••@gmail.com 的 4 位數驗證碼"
                stringResource(R.string.email_confirm_desc, maskEmail(email)),
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(20.dp))

            Otp4Box(
                value = s.code,
                onValueChange = vm::onCodeChange
            )

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = { vm.verify(onSuccess) },
                enabled = s.code.length == 4 && !s.loading,
                shape = MaterialTheme.shapes.extraLarge,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Text(
                    if (s.loading) stringResource(R.string.verifying)
                    else stringResource(R.string.continue_btn)
                )
            }

            Spacer(Modifier.height(16.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.didnt_receive_code) + " ")
                val canResend = s.canResendInSec == 0 && !s.loading
                val resendText = if (canResend) {
                    stringResource(R.string.resend)
                } else {
                    stringResource(R.string.resend_with_seconds, s.canResendInSec)
                }
                Text(
                    resendText,
                    color = if (canResend) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable(enabled = canResend) { vm.resend() }
                        .padding(2.dp)
                )
            }

            s.error?.let {
                Spacer(Modifier.height(12.dp))
                Text(it, color = MaterialTheme.colorScheme.error)
            }

            Spacer(Modifier.weight(1f))
        }
    }
}

@Composable
private fun Otp4Box(
    value: String,
    onValueChange: (String) -> Unit
) {
    val shape = RoundedCornerShape(12.dp)
    androidx.compose.foundation.text.BasicTextField(
        value = value,
        onValueChange = { onValueChange(it.filter(Char::isDigit).take(4)) },
        textStyle = TextStyle(fontSize = 22.sp),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        decorationBox = { inner ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                repeat(4) { idx ->
                    val ch = value.getOrNull(idx)?.toString() ?: ""
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(64.dp)
                            .clip(shape)
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape)
                            .background(MaterialTheme.colorScheme.surface),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(ch, style = TextStyle(fontSize = 22.sp))
                    }
                }
            }
            inner()
        }
    )
}
