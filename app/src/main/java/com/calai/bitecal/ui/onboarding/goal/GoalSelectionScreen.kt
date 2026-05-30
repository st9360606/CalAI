package com.calai.bitecal.ui.onboarding.goal

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import com.calai.bitecal.R
import com.calai.bitecal.ui.common.design.BiteCalOnboardingBottomBar
import com.calai.bitecal.ui.common.design.BiteCalOnboardingTopBar
import com.calai.bitecal.ui.common.haptic.biteCalClickable

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
            BiteCalOnboardingTopBar(
                stepIndex = 7,
                totalSteps = 12,
                onBack = onBack
            )
        },
        bottomBar = {
            BiteCalOnboardingBottomBar(
                primaryText = stringResource(R.string.continue_text),
                primaryEnabled = state.selected != null,
                onPrimaryClick = {
                    vm.saveSelected()
                    onNext()
                }
            )
        },
    ) { inner ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(inner),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.onboard_goal_title),
                fontSize = 34.sp,
                fontWeight = FontWeight.ExtraBold,
                lineHeight = 40.sp,
                color = Color(0xFF111114),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 22.dp),
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.onboard_goal_subtitle),
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = Color(0xFF9AA3AF),
                    lineHeight = 20.sp
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(80.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val w = 0.86f
                val h = 80.dp
                val r = 14.dp

                GoalOption(
                    text = stringResource(R.string.goal_lose),
                    selected = state.selected == GoalKey.LOSE,
                    onClick = { vm.select(GoalKey.LOSE) },
                    widthFraction = w, height = h, corner = r
                )
                Spacer(Modifier.height(18.dp))
                GoalOption(
                    text = stringResource(R.string.goal_maintain),
                    selected = state.selected == GoalKey.MAINTAIN,
                    onClick = { vm.select(GoalKey.MAINTAIN) },
                    widthFraction = w, height = h, corner = r
                )
                Spacer(Modifier.height(18.dp))
                GoalOption(
                    text = stringResource(R.string.goal_gain),
                    selected = state.selected == GoalKey.GAIN,
                    onClick = { vm.select(GoalKey.GAIN) },
                    widthFraction = w, height = h, corner = r
                )
                Spacer(Modifier.height(18.dp))
                // 健康飲食
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
            .biteCalClickable(
                interactionSource = interaction,
                indication = null,
                role = Role.Button
            ) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = content,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontWeight = FontWeight.SemiBold,
                fontSize = 19.sp,
                letterSpacing = 0.2.sp
            ),
            textAlign = TextAlign.Center
        )
    }
}
