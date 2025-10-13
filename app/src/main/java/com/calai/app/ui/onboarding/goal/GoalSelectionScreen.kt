package com.calai.app.ui.onboarding.goal

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.calai.app.R
import com.calai.app.ui.common.OnboardingProgress

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalSelectionScreen(
    onBack: () -> Unit,
    onNext: () -> Unit,
    vm: GoalSelectionViewModel = hiltViewModel()
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
                    onClick = { vm.saveSelected(); onNext() },
                    enabled = state.selected != null, // 未選不能按
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
                stepIndex = 7,
                totalSteps = 11,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
            )
            Spacer(Modifier.height(14.dp))

            Text(
                text = stringResource(R.string.onboard_goal_title),
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
                text = stringResource(R.string.onboard_goal_subtitle),
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
                    .padding(horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val w = 0.88f
                val h = 72.dp
                val r = 26.dp

                GoalOption(
                    text = stringResource(R.string.goal_lose),
                    selected = state.selected == GoalKey.LOSE,
                    onClick = { vm.select(GoalKey.LOSE) },
                    widthFraction = w, height = h, corner = r
                )
                Spacer(Modifier.height(25.dp))
                GoalOption(
                    text = stringResource(R.string.goal_maintain),
                    selected = state.selected == GoalKey.MAINTAIN,
                    onClick = { vm.select(GoalKey.MAINTAIN) },
                    widthFraction = w, height = h, corner = r
                )
                Spacer(Modifier.height(25.dp))
                GoalOption(
                    text = stringResource(R.string.goal_gain),
                    selected = state.selected == GoalKey.GAIN,
                    onClick = { vm.select(GoalKey.GAIN) },
                    widthFraction = w, height = h, corner = r
                )
                Spacer(Modifier.height(25.dp))
                // ★ 新增：健康飲食
                GoalOption(
                    text = stringResource(R.string.goal_healthy_eating),
                    selected = state.selected == GoalKey.HEALTHY_EATING,
                    onClick = { vm.select(GoalKey.HEALTHY_EATING) },
                    widthFraction = w, height = h, corner = r
                )
            }
        }
    }
}

@Composable
private fun GoalOption(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    widthFraction: Float,
    height: Dp,
    corner: Dp
) {
    val shape = RoundedCornerShape(corner)
    val container = if (selected) Color(0xFF111114) else Color(0xFFF1F3F7)
    val content = if (selected) Color.White else Color(0xFF111114)
    val interaction = remember { MutableInteractionSource() }

    Box(
        modifier = Modifier
            .fillMaxWidth(widthFraction)
            .height(height)
            .clip(shape)
            .background(container)
            .clickable(
                interactionSource = interaction,
                indication = null,
                role = Role.Button
            ) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = content,
            fontSize = 19.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center
        )
    }
}
