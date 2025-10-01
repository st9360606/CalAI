package com.calai.app.ui.onboarding.exercise

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.calai.app.R
import com.calai.app.ui.common.OnboardingProgress

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseFrequencyScreen(
    vm: ExerciseFrequencyViewModel,
    onBack: () -> Unit,
    onNext: () -> Unit
) {
    val state by vm.uiState.collectAsState()

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
                        vm.saveSelected()              // 寫入 DataStore（僅在非空時）
                        onNext()
                    },
                    enabled = state.selected != null,  // ← 未選擇前不能按
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(start = 20.dp, end = 20.dp, bottom = 59.dp)
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
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
        ) {
            OnboardingProgress(
                stepIndex = 6,
                totalSteps = 11,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
            )

            Spacer(Modifier.height(14.dp))

            Text(
                text = stringResource(id = R.string.onboard_ex_freq_title),
                style = MaterialTheme.typography.headlineLarge.copy(fontSize = 34.sp),
                fontWeight = FontWeight.ExtraBold,
                lineHeight = 40.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                textAlign = TextAlign.Start
            )

            Spacer(Modifier.height(6.dp))

            Text(
                text = stringResource(id = R.string.onboard_ex_freq_subtitle),
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 16.sp,
                    lineHeight = 22.sp
                ),
                color = Color(0xFF8F98A3),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
            )

            Spacer(Modifier.height(55.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
            ) {
                FreqOptionCard(
                    iconRes = R.drawable.running,   // 0–2
                    selected = state.selected == 2,
                    title = stringResource(R.string.ex_freq_0_2_title),
                    subtitle = stringResource(R.string.ex_freq_0_2_sub),
                    onClick = { vm.select(2) }
                )

                Spacer(Modifier.height(25.dp))

                FreqOptionCard(
                    iconRes = R.drawable.cycling,   // 3–5
                    selected = state.selected == 5,
                    title = stringResource(R.string.ex_freq_3_5_title),
                    subtitle = stringResource(R.string.ex_freq_3_5_sub),
                    onClick = { vm.select(5) }
                )

                Spacer(Modifier.height(25.dp))

                FreqOptionCard(
                    iconRes = R.drawable.muscle,    // 6+
                    selected = state.selected == 7,
                    title = stringResource(R.string.ex_freq_6_plus_title),
                    subtitle = stringResource(R.string.ex_freq_6_plus_sub),
                    onClick = { vm.select(7) }
                )
            }
        }
    }
}

@Composable
private fun FreqOptionCard(
    @DrawableRes iconRes: Int,
    selected: Boolean,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    cornerRadius: Dp = 32.dp,
    titleSize: androidx.compose.ui.unit.TextUnit = 18.sp,
    subtitleSize: androidx.compose.ui.unit.TextUnit = 16.sp
) {
    val shape = RoundedCornerShape(cornerRadius)
    val bg = if (selected) Color.Black else Color(0xFFF1F3F7)
    val fg = if (selected) Color.White else Color(0xFF111114)

    Card(
        onClick = onClick,
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = bg),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(92.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(44.dp),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(iconRes),
                    contentDescription = null,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(Modifier.width(14.dp))

            Column(Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = fg,
                    fontSize = titleSize,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    color = fg.copy(alpha = 0.72f),
                    fontSize = subtitleSize,
                    lineHeight = (subtitleSize.value + 4).sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
