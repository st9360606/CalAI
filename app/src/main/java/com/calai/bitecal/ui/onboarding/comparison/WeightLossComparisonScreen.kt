package com.calai.bitecal.ui.onboarding.comparison

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.calai.bitecal.R
import com.calai.bitecal.ui.common.design.BiteCalOnboardingBottomBar
import com.calai.bitecal.ui.common.design.BiteCalOnboardingTopBar
import com.calai.bitecal.ui.common.design.BiteCalScreenFrame

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeightLossComparisonScreen(
    onBack: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier,
        containerColor = Color.White,
        topBar = {
            BiteCalOnboardingTopBar(
                stepIndex = 9,
                totalSteps = 12,
                onBack = onBack
            )
        },
        bottomBar = {
            BiteCalOnboardingBottomBar(
                primaryText = stringResource(R.string.common_continue_btn),
                onPrimaryClick = onNext
            )
        },
    ) { inner ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner),
            horizontalAlignment = Alignment.Start
        ) {

            Spacer(Modifier.height(10.dp))

            Text(
                text = stringResource(R.string.onboard_weight_loss_comparison_title),
                color = Color(0xFF111114),
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                lineHeight = 38.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 36.dp, end = 24.dp),
                textAlign = TextAlign.Start
            )

            Spacer(Modifier.height(42.dp))

            ComparisonCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = BiteCalScreenFrame.contentHorizontalExtraWide)
            )
        }
    }
}

@Composable
private fun ComparisonCard(
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .height(380.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xFFF4F5F5),
                        Color(0xFFF4F4F4),
                        Color(0xFFFFF4F9)
                    )
                )
            )
            .padding(horizontal = 28.dp, vertical = 42.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.Bottom
            ) {
                ComparisonPillar(
                    title = stringResource(R.string.onboard_weight_loss_comparison_without_ai),
                    value = stringResource(R.string.onboard_weight_loss_comparison_twenty_percent),
                    valueContainerColor = Color(0xFFE1E1E1),
                    valueTextColor = Color(0xFF111114),
                    bottomBlockHeight = 51.dp
                )

                Spacer(Modifier.width(36.dp))

                ComparisonPillar(
                    title = stringResource(R.string.onboard_weight_loss_comparison_with_ai),
                    value = stringResource(R.string.onboard_weight_loss_comparison_two_times),
                    valueContainerColor = Color(0xFF1C1822),
                    valueTextColor = Color.White,
                    bottomBlockHeight = 122.dp
                )
            }

            Spacer(Modifier.height(24.dp))

            Text(
                text = stringResource(R.string.onboard_weight_loss_comparison_caption),
                color = Color(0xFF57575D),
                fontSize = 17.sp,
                fontWeight = FontWeight.Medium,
                lineHeight = 24.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun ComparisonPillar(
    title: String,
    value: String,
    valueContainerColor: Color,
    valueTextColor: Color,
    bottomBlockHeight: Dp,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .width(104.dp)
            .height(202.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Bottom
    ) {
        Text(
            text = title,
            color = Color(0xFF111114),
            fontSize = 17.sp,
            fontWeight = FontWeight.ExtraBold,
            lineHeight = 24.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 8.dp, end = 8.dp, top = 14.dp)
                .weight(1f),
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(bottomBlockHeight)
                .clip(RoundedCornerShape(18.dp))
                .background(valueContainerColor),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = value,
                color = valueTextColor,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )
        }
    }
}
