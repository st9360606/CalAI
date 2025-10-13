package com.calai.app.ui.onboarding.referralsource

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import com.calai.app.R
import com.calai.app.ui.common.OnboardingProgress
import kotlinx.coroutines.launch

// 控制每個 item 佔用的寬度比例（置中）
private const val OPTION_WIDTH_FRACTION = 0.90f

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReferralSourceScreen(
    onBack: () -> Unit,
    onNext: () -> Unit,
    vm: ReferralSourceViewModel = hiltViewModel(),
) {
    val state by vm.uiState.collectAsState()
    val scope = rememberCoroutineScope()

    Scaffold(
        containerColor = Color.White,
        topBar = {
            TopAppBar(
                title = {},
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White,
                    navigationIconContentColor = Color(0xFF111114)
                ),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Box(
                            modifier = Modifier
                                .size(39.dp)
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
                }
            )
        },
        bottomBar = {
            Box {
                Button(
                    onClick = {
                        scope.launch {
                            vm.saveAndContinue()
                            onNext()
                        }
                    },
                    enabled = state.selected != null,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(start = 20.dp, end = 20.dp, bottom = 75.dp)
                        .fillMaxWidth()
                        .height(64.dp),
                    shape = RoundedCornerShape(28.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Black,
                        contentColor = Color.White
                    )
                ) {
                    Text(
                        text = stringResource(R.string.continue_text),
                        fontSize = 19.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    ) { inner ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(inner)
        ) {
            OnboardingProgress(
                stepIndex = 2,
                totalSteps = 11,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
            )

            Spacer(Modifier.height(6.dp))

            Text(
                text = stringResource(id = R.string.onboard_referral_title),
                fontSize = 34.sp,
                fontWeight = FontWeight.ExtraBold,
                lineHeight = 40.sp,
                color = Color(0xFF111114),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 15.dp),
                textAlign = TextAlign.Center
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) {
                items(
                    items = state.options,
                    key = { it.key }
                ) { opt ->
                    // 置中 + 縮窄寬度
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        ReferralOptionItem(
                            option = opt,
                            selected = state.selected == opt.key,
                            onClick = { vm.select(opt.key) },
                            modifier = Modifier
                                .fillMaxWidth(OPTION_WIDTH_FRACTION)
                        )
                    }
                    Spacer(Modifier.height(14.dp))
                }
            }
        }
    }
}

@Composable
private fun ReferralOptionItem(
    option: ReferralUiOption,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bg by animateColorAsState(
        targetValue = if (selected) Color.Black else Color(0xFFF1F3F7),
        label = "referral-bg"
    )
    val fg = if (selected) Color.White else Color.Black
    val interaction = remember { MutableInteractionSource() }

    Row(
        modifier = modifier
            .height(71.dp)
            .clip(RoundedCornerShape(26.dp))
            .background(bg)
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 固定純白圓底
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(Color.White)
                .zIndex(1f),
            contentAlignment = Alignment.Center
        ) {
            if (option.iconRes != null) {
                Icon(
                    painter = painterResource(option.iconRes),
                    contentDescription = option.label,
                    tint = Color.Unspecified,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Spacer(Modifier.width(16.dp))

        Text(
            text = option.label,
            color = fg,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}
