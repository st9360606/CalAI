// app/src/main/java/com/calai/app/ui/auth/SignInSheet.kt
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
import androidx.compose.runtime.remember
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
import com.calai.app.i18n.ProvideComposeLocale  // ✅ 重點：引入你自家的覆蓋 Composable

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignInSheet(
    // ✅ 新增：由呼叫端傳入目前語言（例如 "zh-TW"）
    localeTag: String,
    onApple: () -> Unit,
    onGoogle: () -> Unit,
    onEmail: () -> Unit,
    onTerms: () -> Unit = {},
    onPrivacy: () -> Unit = {},
    onDismiss: () -> Unit
) {
    val sheetShape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        dragHandle = null,
        containerColor = Color.White,
        shape = sheetShape,
        tonalElevation = 0.dp
    ) {
        // ✅ 關鍵：在 BottomSheet「內容」裡重新覆蓋 LocalContext 為目標語系
        ProvideComposeLocale(tag = localeTag) {
            Column(
                modifier = Modifier
                    .navigationBarsPadding()
                    .imePadding()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                // 標題：置中 + 右上關閉
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Spacer(Modifier.width(48.dp)) // 與右側關閉鍵等寬，確保文字真正置中
                    Text(
                        text = stringResource(R.string.signin_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(R.string.close)
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))

//                // Apple（黑底白字）
//                Button(
//                    onClick = onApple,
//                    modifier = Modifier
//                        .fillMaxWidth()
//                        .height(56.dp),
//                    shape = RoundedCornerShape(28.dp),
//                    colors = ButtonDefaults.buttonColors(
//                        containerColor = Color.Black,
//                        contentColor = Color.White
//                    ),
//                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
//                ) {
//                    Icon(
//                        painter = painterResource(R.drawable.ic_apple),
//                        contentDescription = null,
//                        modifier = Modifier
//                            .size(20.dp)
//                            .offset(x = (-6).dp), // 圖示更靠左一點
//                        tint = Color.Unspecified
//                    )
//                    Spacer(Modifier.width(10.dp))
//                    Text(
//                        text = stringResource(R.string.signin_with_apple),
//                        fontSize = 18.sp,
//                        fontWeight = FontWeight.SemiBold
//                    )
//                }
//
//                Spacer(Modifier.height(12.dp))

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
                        modifier = Modifier.size(20.dp),
                        tint = Color.Unspecified
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = stringResource(R.string.signin_with_google),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(Modifier.height(12.dp))

                // Email（白底描邊）
                OutlinedButton(
                    onClick = onEmail,
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
                        imageVector = Icons.Outlined.Email,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp) // 信封再大一點
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = stringResource(R.string.signin_with_email),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(Modifier.height(14.dp))

                // 條款提示
                val hintColor = MaterialTheme.colorScheme.onSurfaceVariant
                val legalFontSize = 12.sp
                val legalLineHeight = 16.sp
                Text(
                    text = stringResource(R.string.signin_legal_hint),
                    color = hintColor,
                    fontSize = legalFontSize,
                    lineHeight = legalLineHeight,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.bodySmall.copy(
                        platformStyle = PlatformTextStyle(includeFontPadding = false),
                        lineHeightStyle = LineHeightStyle(
                            alignment = LineHeightStyle.Alignment.Center,
                            trim = LineHeightStyle.Trim.Both
                        )
                    )
                )

                // Terms / and / Privacy（可點）
                val uriHandler = LocalUriHandler.current
                val termsUrl = stringResource(R.string.signin_url_terms)
                val privacyUrl = stringResource(R.string.signin_url_privacy)
                val linkStyle = MaterialTheme.typography.bodySmall.copy(
                    fontSize = legalFontSize,
                    lineHeight = legalLineHeight,
                    platformStyle = PlatformTextStyle(includeFontPadding = false),
                    lineHeightStyle = LineHeightStyle(
                        alignment = LineHeightStyle.Alignment.Center,
                        trim = LineHeightStyle.Trim.Both
                    )
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = stringResource(R.string.signin_terms),
                        style = linkStyle,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .alignBy(LastBaseline)
                            .padding(horizontal = 6.dp)
                            .clickable(role = Role.Button) {
                                onTerms()
                                uriHandler.openUri(termsUrl)
                            }
                    )
                    Text(
                        text = stringResource(R.string.signin_and),
                        style = linkStyle,
                        color = hintColor,
                        modifier = Modifier.alignBy(LastBaseline)
                    )
                    Text(
                        text = stringResource(R.string.signin_privacy),
                        style = linkStyle,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .alignBy(LastBaseline)
                            .padding(horizontal = 6.dp)
                            .clickable(role = Role.Button) {
                                onPrivacy()
                                uriHandler.openUri(privacyUrl)
                            }
                    )
                }
            }
        }
    }
}
