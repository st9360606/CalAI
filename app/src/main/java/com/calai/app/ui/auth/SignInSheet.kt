package com.calai.app.ui.auth

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import com.calai.app.i18n.ProvideComposeLocale
import kotlinx.coroutines.delay

private enum class SheetAuthProvider {
    Google,
    Email
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignInSheet(
    localeTag: String,
    onApple: () -> Unit,
    onGoogle: () -> Unit,
    onEmail: () -> Unit,
    onTerms: () -> Unit = {},
    onPrivacy: () -> Unit = {},
    onDismiss: () -> Unit
) {
    val sheetShape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    val closeTint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        dragHandle = null,
        containerColor = Color.White,
        shape = sheetShape,
        tonalElevation = 0.dp
    ) {
        ProvideComposeLocale(tag = localeTag) {
            // ★ 目前選擇的登入方式（初始都不是黑底）
            var selectedProvider by remember { mutableStateOf<SheetAuthProvider?>(null) }

            // ★ 當選擇改變時，晚一點點再呼叫對應的 callback
            LaunchedEffect(selectedProvider) {
                when (selectedProvider) {
                    SheetAuthProvider.Google -> {
                        // 讓黑底狀態顯示一下下
                        onGoogle()
                    }
                    SheetAuthProvider.Email -> {
                        delay(120)
                        onEmail()
                    }
                    null -> Unit
                }
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.30f)    // 30% 螢幕高度，你可以視覺再調整
                    .navigationBarsPadding()
                    .imePadding()
                    .padding(horizontal = 20.dp)
                    .padding(top = 20.dp, bottom = 12.dp) // ★ 整體內容往下移一點
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // ★ SIGN IN 標題：真正置中（不受右邊 X 影響）
                    Text(
                        text = stringResource(R.string.signin_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.align(Alignment.Center)
                    )
                    // ★ 右上角 X：淡灰色，固定在最右側
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .size(16.dp) // 建議 24–40.dp，點擊比較好點
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(R.string.close),
                            tint = closeTint
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                // === Google 按鈕 ===
                val ink = Color(0xFF111114)

                if (selectedProvider == SheetAuthProvider.Google) {
                    // 已選 Google：黑底白字
                    Button(
                        onClick = {
                            // ★ 只改 state，不直接呼叫 onGoogle()
                            selectedProvider = SheetAuthProvider.Google
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(28.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ink,
                            contentColor = Color.White
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
                } else {
                    // 未選 Google：Outlined（原本樣式）
                    OutlinedButton(
                        onClick = {
                            selectedProvider = SheetAuthProvider.Google
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(28.dp),
                        border = BorderStroke(1.dp, Color(0xFFE5E5EA)),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = ink
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
                }

                Spacer(Modifier.height(12.dp))

                // === Email 按鈕 ===
                if (selectedProvider == SheetAuthProvider.Email) {
                    // 已選 Email：黑底白字
                    Button(
                        onClick = {
                            // ★ 只改 state，不直接呼叫 onEmail()
                            selectedProvider = SheetAuthProvider.Email
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(28.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ink,
                            contentColor = Color.White
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Email,
                            contentDescription = null,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = stringResource(R.string.signin_with_email),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                } else {
                    // 未選 Email：Outlined（原本樣式）
                    OutlinedButton(
                        onClick = {
                            selectedProvider = SheetAuthProvider.Email
                        },
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
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = stringResource(R.string.signin_with_email),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer (Modifier.height(20.dp))

                val hintColor = MaterialTheme.colorScheme.onSurfaceVariant
                val legalFontSize = 12.sp
                val legalLineHeight = 16.sp

                // ★ 1) 法務說明文字：置中 + 限制最大寬度
                Box(
                    modifier = Modifier
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.signin_legal_hint),
                        color = hintColor,
                        fontSize = legalFontSize,
                        lineHeight = legalLineHeight,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.widthIn(max = 360.dp),
                        style = MaterialTheme.typography.bodySmall.copy(
                            platformStyle = PlatformTextStyle(includeFontPadding = false),
                            lineHeightStyle = LineHeightStyle(
                                alignment = LineHeightStyle.Alignment.Center,
                                trim = LineHeightStyle.Trim.Both
                            )
                        )
                    )
                }

                // ★ 在 hint 跟連結中間加一段明顯間距
                Spacer(Modifier.height(3.dp))   // 你可以改成 16.dp 看實機感覺

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

                // ★ 2) Terms / and / Privacy：整塊置中 + 限寬
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 0.dp, bottom = 10.dp), // 如果還是覺得擠，可以把 top 再調成 4.dp
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        modifier = Modifier.widthIn(max = 360.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.signin_terms),
                            style = linkStyle,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .padding(horizontal = 6.dp)
                                .clickable(role = Role.Button) {
                                    onTerms()
                                    uriHandler.openUri(termsUrl)
                                }
                        )
                        Text(
                            text = stringResource(R.string.signin_and),
                            style = linkStyle,
                            color = hintColor
                        )
                        Text(
                            text = stringResource(R.string.signin_privacy),
                            style = linkStyle,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
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
}
