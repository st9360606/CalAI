package com.calai.app.ui.auth

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.LastBaseline
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.calai.app.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignInSheet(
    onApple: () -> Unit,
    onGoogle: () -> Unit,
    onEmail: () -> Unit,
    onTerms: () -> Unit = {},
    onPrivacy: () -> Unit = {},
    onDismiss: () -> Unit
) {
    val shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        dragHandle = null,
        containerColor = Color.White,
        shape = shape,
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            // 標題列（置中標題 + 右上關閉），保留 Row 寫法
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 為了讓標題真正置中，左側預留與關閉鍵等寬的空間
                Spacer(Modifier.width(48.dp)) // 關閉鍵大約 48dp 區域

                Text(
                    text = stringResource(R.string.signin_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f) // ★ 置中關鍵：中間權重撐滿
                )

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(48.dp) // 與左側 Spacer 對稱
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }

            Spacer(Modifier.height(8.dp))

            // Apple（黑底白字 圓角 pill）
            Button(
                onClick = onApple,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Black,
                    contentColor = Color.White
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_apple),
                    contentDescription = null,
                    modifier = Modifier
                        .size(34.dp)
                        .offset(x = (-4).dp),
                    tint = Color.Unspecified
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = stringResource(R.string.signin_with_apple), // ← 改
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(Modifier.height(12.dp))

            // Google（白底描邊）
            OutlinedButton(
                onClick = onGoogle,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(28.dp),
                border = BorderStroke(1.dp, Color(0xFFE5E5EA)),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color(0xFF111114)
                )
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_google),
                    contentDescription = null,
                    modifier = Modifier.size(34.dp),
                    tint = Color.Unspecified
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = stringResource(R.string.signin_with_google), // ← 改
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(Modifier.height(12.dp))

            // Email（白底描邊 + 信封）
            OutlinedButton(
                onClick = onEmail,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(32.dp),
                border = BorderStroke(1.dp, Color(0xFFE5E5EA)),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color(0xFF111114)
                )
            ) {
                Icon(
                    imageVector = Icons.Outlined.Email,
                    contentDescription = null,
                    modifier = Modifier
                        .size(28.dp)
                        .offset(x = (8).dp),
                )
                Spacer(Modifier.width(18.dp))
                Text(
                    text = stringResource(R.string.signin_with_email), // ← 改
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(Modifier.height(15.dp))

            // 條款提示（置中、次要色）
            val hintColor = MaterialTheme.colorScheme.onSurfaceVariant
            Text(
                text = stringResource(R.string.signin_legal_hint), // ← 改
                fontSize = 13.sp,
                color = hintColor,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            val uriHandler = LocalUriHandler.current
            val termsUrl = stringResource(R.string.signin_url_terms)
            val privacyUrl = stringResource(R.string.signin_url_privacy)
            // 條款 / and / 隱私：用 baseline 對齊，大小一致
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                val hintColor = MaterialTheme.colorScheme.onSurfaceVariant
                val linkColor = MaterialTheme.colorScheme.primary
                val baseStyle = MaterialTheme.typography.bodySmall.copy(
                    // 讓上下不多補白，避免視覺偏差
                    platformStyle = PlatformTextStyle(includeFontPadding = false)
                )

                Text(
                    text = stringResource(R.string.signin_terms),
                    style = baseStyle,
                    color = linkColor,
                    modifier = Modifier
                        .alignBy(LastBaseline)     // ★ 用 baseline 對齊
                        .padding(horizontal = 5.dp)
                        .clickable(role = Role.Button) {
                            uriHandler.openUri(termsUrl)     // ← 用上面先取好的值
                        }
                )
                Text(
                    text = stringResource(R.string.signin_and),
                    style = baseStyle,
                    color = hintColor,
                    modifier = Modifier.alignBy(LastBaseline)   // ★ 一樣對齊
                )
                Text(
                    text = stringResource(R.string.signin_privacy),
                    style = baseStyle,
                    color = linkColor,
                    modifier = Modifier
                        .alignBy(LastBaseline)     // ★ 一樣對齊
                        .padding(horizontal = 5.dp)
                        .clickable(role = Role.Button) {
                            uriHandler.openUri(privacyUrl)   // ← 同理
                        }
                )
            }
            val baseStyle = MaterialTheme.typography.bodySmall.copy(
                platformStyle = PlatformTextStyle(includeFontPadding = false),
                lineHeightStyle = LineHeightStyle(
                    alignment = LineHeightStyle.Alignment.Center,
                    trim = LineHeightStyle.Trim.Both
                )
            )

        }
    }
}
