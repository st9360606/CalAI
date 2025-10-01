package com.calai.app.ui.auth.email

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
        delay(120)
        focus.requestFocus()
    }

    // ===== 本地倒數 fallback（與 VM 的 canResendInSec 取最大值）=====
    var localLeft by remember { mutableStateOf(0) }
    // 每秒遞減（僅當 localLeft > 0）
    LaunchedEffect(localLeft) {
        if (localLeft > 0) {
            delay(1000)
            localLeft -= 1
        }
    }
    val vmLeft = ui?.canResendInSec ?: 0
    val left = max(vmLeft, localLeft)
    val canResend = left == 0
    val resendLabel = if (canResend) "Resend" else "Resend (${left}s)"

    Scaffold(
        containerColor = Color.White,
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
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White,
                    navigationIconContentColor = Color(0xFF111114),
                    titleContentColor = Color(0xFF111114)
                )
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
                lineHeight = 36.sp,
                color = Color(0xFF111114)
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
                                        textAlign = TextAlign.Center,
                                        color = Color(0xFF111114)
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
                    vm.verify { onSuccess() }
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

                TextButton(
                    enabled = canResend,
                    onClick = {
                        // 1) 立刻啟動本地 30 秒倒數（讓使用者有反饋）
                        localLeft = 30
                        // 2) 呼叫 VM 發送（VM 裡可再啟動自己的 cooldown／打 API）
                        vm.resend()
                        // 3) 稍微延遲再回焦到輸入（鍵盤保持開啟）
                        scope.launch {
                            delay(80)
                            focus.requestFocus()
                        }
                    },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = Color.Black,
                        disabledContentColor = Color(0x61000000)
                    )
                ) {
                    Text(
                        text = resendLabel,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                }
            }

            AnimatedVisibility(visible = ui?.loading == true) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 20.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(
                        strokeWidth = 3.dp,
                        color = Color.Black
                    )
                }
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
