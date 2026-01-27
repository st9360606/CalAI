package com.calai.bitecal.ui.auth

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.calai.bitecal.R
import com.calai.bitecal.ui.common.OnboardingProgress

private enum class PrimaryAuthMethod { Google, Email }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RequireSignInScreen(
    onBack: () -> Unit,
    onGoogleClick: () -> Unit,
    onEmailClick: () -> Unit,
    onSkip: () -> Unit,
    snackBarHostState: SnackbarHostState,
    ctaVerticalOffset: Dp = (-24).dp
) {
    val ink = Color(0xFF111114)
    var primaryAuth by remember { mutableStateOf(PrimaryAuthMethod.Google) }

    BackHandler { onBack() }

    Scaffold(
        containerColor = Color.White,
        snackbarHost = { SnackbarHost(hostState = snackBarHostState) },
        topBar = {
            TopAppBar(modifier = Modifier.padding(start = 16.dp, end = 16.dp),
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White,
                    navigationIconContentColor = Color(0xFF111114)
                ),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color(0xFFF1F3F7)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = Color(0xFF111114)
                            )
                        }
                    }
                },
                title = {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(end = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OnboardingProgress(
                            stepIndex = 11,
                            totalSteps = 11,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            )
        }
    ) { inner ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Spacer(Modifier.height(6.dp))

            Text(
                text = stringResource(R.string.auth_sign_in_account_title),
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontSize = 34.sp,
                    fontWeight = FontWeight.ExtraBold,
                    lineHeight = 40.sp
                ),
                color = ink,
                textAlign = TextAlign.Start,
                modifier = Modifier
                    .padding(horizontal = 24.dp, vertical = 15.dp)
                    .fillMaxWidth()
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .offset(y = ctaVerticalOffset)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // --- Google ---
                    if (primaryAuth == PrimaryAuthMethod.Google) {
                        Button(
                            onClick = {
                                primaryAuth = PrimaryAuthMethod.Google
                                onGoogleClick()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(64.dp),
                            shape = RoundedCornerShape(100.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ink,
                                contentColor = Color.White
                            ),
                            contentPadding = PaddingValues(horizontal = 20.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(Color.White),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.google),
                                        contentDescription = "Google",
                                        tint = Color.Unspecified,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    text = stringResource(R.string.auth_sign_in_with_google),
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    } else {
                        OutlinedButton(
                            onClick = {
                                primaryAuth = PrimaryAuthMethod.Google
                                onGoogleClick()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(64.dp),
                            shape = RoundedCornerShape(100.dp),
                            contentPadding = PaddingValues(horizontal = 20.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = ink)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFF1F3F7)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.google),
                                        contentDescription = "Google",
                                        tint = Color.Unspecified,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    text = stringResource(R.string.auth_sign_in_with_google),
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    // --- Email ---
                    if (primaryAuth == PrimaryAuthMethod.Email) {
                        Button(
                            onClick = {
                                primaryAuth = PrimaryAuthMethod.Email
                                onEmailClick()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(64.dp),
                            shape = RoundedCornerShape(100.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ink,
                                contentColor = Color.White
                            ),
                            contentPadding = PaddingValues(horizontal = 20.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Email,
                                    contentDescription = "Email",
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    text = stringResource(R.string.signin_with_email),
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    } else {
                        OutlinedButton(
                            onClick = {
                                primaryAuth = PrimaryAuthMethod.Email
                                onEmailClick()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(64.dp),
                            shape = RoundedCornerShape(100.dp),
                            contentPadding = PaddingValues(horizontal = 20.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = ink)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Email,
                                    contentDescription = "Email",
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    text = stringResource(R.string.signin_with_email),
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    // --- Skip ---
                    OutlinedButton(
                        onClick = onSkip,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(64.dp),
                        shape = RoundedCornerShape(100.dp)
                    ) {
                        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Text(
                                text = stringResource(R.string.common_skip),
                                fontSize = 20.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = ink,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.navigationBarsPadding())
        }
    }
}
