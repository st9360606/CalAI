package com.calai.app.ui.auth.email


import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmailCodeScreen(
    vm: EmailSignInViewModel,
    email: String,                  // 由 Nav 傳入（記得用 Uri.encode 帶參數）
    onBack: () -> Unit,
    onSuccess: () -> Unit
) {
    val state = vm.code.collectAsState().value
    // 首次進來，VM 可能還沒初始化 code 狀態；這裡幫你初始化
    LaunchedEffect(email) {
        if (state == null && email.isNotBlank()) {
            // 新增到 VM：prepareCode(email)（見下方 VM 補強）
            vm.prepareCode(email)
        }
    }

    val ui = vm.code.collectAsState().value
    val scope = rememberCoroutineScope()
    val focus = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val kb = LocalSoftwareKeyboardController.current

    // 畫面出現後自動聚焦
    LaunchedEffect(ui?.email) {
        delay(120)
        focus.requestFocus()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Spacer(Modifier.height(12.dp))

            Text(
                "Confirm your email",
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 36.sp
            )

            Spacer(Modifier.height(8.dp))

            Text(
                "Please enter the 4-digit code we've just sent to",
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
                                        textAlign = TextAlign.Center
                                    )
                                )
                            }
                        }
                    }
                },
                textStyle = LocalTextStyle.current.copy(color = Color.Transparent) // 隱藏原輸入
            )

            // 輸入滿 4 碼就自動送驗
            LaunchedEffect(ui?.code) {
                if ((ui?.code?.length ?: 0) == 4) {
                    kb?.hide()
                    vm.verify {
                        onSuccess()
                    }
                }
            }

            Spacer(Modifier.height(28.dp))
            Divider(color = Color(0x1A000000))
            Spacer(Modifier.height(12.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Didn't receive the code?",
                    style = MaterialTheme.typography.bodyMedium.copy(color = Color.Gray)
                )
                Spacer(Modifier.width(6.dp))

                val left = ui?.canResendInSec ?: 0
                val canResend = left == 0
                val label = if (canResend) "Resend" else "Resend (${left}s)"

                TextButton(
                    enabled = canResend,
                    onClick = {
                        vm.resend()
                        scope.launch {
                            delay(80)
                            focus.requestFocus()
                        }
                    },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = if (canResend) MaterialTheme.colorScheme.onSurface
                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    )
                }
            }


            AnimatedVisibility(visible = ui?.loading == true) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 20.dp),
                    horizontalArrangement = Arrangement.Center
                ) { CircularProgressIndicator(strokeWidth = 3.dp) }
            }

            AnimatedVisibility(visible = ui?.error != null) {
                Text(
                    text = ui?.error ?: "",
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
    val head = email.substring(0, 2)
    val tail = email.substring(at)
    return buildString {
        append(head)
        append("•".repeat((at - 2).coerceAtLeast(1)))
        append(tail)
    }
}
