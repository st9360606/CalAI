package com.calai.bitecal.ui.home.ui.settings.widgetguide

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DragIndicator
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.TouchApp
import androidx.compose.material.icons.outlined.Widgets
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.calai.bitecal.R
import com.calai.bitecal.ui.common.design.BiteCalPrimaryButton
import com.calai.bitecal.ui.home.components.LightHomeBackground
import com.calai.bitecal.ui.common.design.BiteCalTopBar

@Composable
fun WidgetGuideScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(Modifier.fillMaxSize()) { LightHomeBackground() }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            BiteCalTopBar(
                title = stringResource(R.string.widget_guide_title),
                onBack = onBack
            )
        }
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp)
                .padding(top = 0.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            WidgetGuideHeroCard()
            WidgetGuideStepsCard()
            WidgetGuideTipCard()

            BiteCalPrimaryButton(
                text = stringResource(R.string.widget_guide_done),
                enabled = true,
                loading = false,
                onClick = onBack,
                modifier = Modifier.padding(top = 32.dp)
            )
        }
    }
}

@Composable
private fun WidgetGuideHeroCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(30.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.White, Color(0xFFF7F8FA))
                    )
                )
                .padding(horizontal = 20.dp, vertical = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            WidgetGuidePhoneMock()

            Spacer(Modifier.height(18.dp))

            Text(
                text = stringResource(R.string.widget_guide_hero_title),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.headlineSmall.copy(
                    color = Color(0xFF111114),
                    fontWeight = FontWeight.Black,
                    fontSize = 25.sp,
                    lineHeight = 30.sp
                )
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.widget_guide_hero_body),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = Color(0xFF6B7280),
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
            )
        }
    }
}

@Composable
private fun WidgetGuidePhoneMock() {
    Box(
        modifier = Modifier
            .width(168.dp)
            .height(210.dp)
            .clip(RoundedCornerShape(34.dp))
            .background(Color(0xFF111114))
            .padding(8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(27.dp))
                .background(Color(0xFFF3F4F6))
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.widget_guide_mock_home),
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = Color(0xFF4B5563),
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp
                    )
                )
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFD1D5DB))
                )
            }

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(74.dp),
                shape = RoundedCornerShape(22.dp),
                color = Color.White,
                shadowElevation = 0.dp
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF111114)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "B",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = Color.White,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 11.sp
                                )
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            MockLine(width = 58.dp, height = 8.dp, color = Color(0xFF111114))
                            MockLine(width = 42.dp, height = 6.dp, color = Color(0xFFD1D5DB))
                        }
                    }
                    MockLine(width = 112.dp, height = 9.dp, color = Color(0xFFE5E7EB))
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                repeat(4) { index ->
                    Box(
                        modifier = Modifier
                            .size(25.dp)
                            .clip(RoundedCornerShape(9.dp))
                            .background(if (index == 1) Color(0xFF111114) else Color.White)
                    )
                }
            }

            Spacer(Modifier.weight(1f))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                color = Color.White,
                shadowElevation = 0.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Widgets,
                        contentDescription = null,
                        tint = Color(0xFF111114),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.widget_guide_mock_widgets),
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = Color(0xFF111114),
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun MockLine(
    width: androidx.compose.ui.unit.Dp,
    height: androidx.compose.ui.unit.Dp,
    color: Color
) {
    Box(
        modifier = Modifier
            .width(width)
            .height(height)
            .clip(CircleShape)
            .background(color)
    )
}

@Composable
private fun WidgetGuideStepsCard() {
    val cardShape = RoundedCornerShape(26.dp)

    val steps = listOf(
        WidgetGuideStep(
            icon = Icons.Outlined.TouchApp,
            titleRes = R.string.widget_guide_step_1_title,
            bodyRes = R.string.widget_guide_step_1_body
        ),
        WidgetGuideStep(
            icon = Icons.Outlined.Widgets,
            titleRes = R.string.widget_guide_step_2_title,
            bodyRes = R.string.widget_guide_step_2_body
        ),
        WidgetGuideStep(
            icon = Icons.Outlined.Search,
            titleRes = R.string.widget_guide_step_3_title,
            bodyRes = R.string.widget_guide_step_3_body
        ),
        WidgetGuideStep(
            icon = Icons.Outlined.DragIndicator,
            titleRes = R.string.widget_guide_step_4_title,
            bodyRes = R.string.widget_guide_step_4_body
        )
    )

    Card(
        modifier = Modifier
            .fillMaxWidth(),
        shape = cardShape,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = stringResource(R.string.widget_guide_steps_title),
                style = MaterialTheme.typography.titleLarge.copy(
                    color = Color(0xFF111114),
                    fontWeight = FontWeight.Black,
                    fontSize = 20.sp,
                    lineHeight = 24.sp
                )
            )

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                steps.forEachIndexed { index, step ->
                    WidgetGuideStepRow(
                        number = index + 1,
                        step = step
                    )
                }
            }
        }
    }
}

@Composable
private fun WidgetGuideStepRow(
    number: Int,
    step: WidgetGuideStep
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(Color(0xFFF7F8FA))
            .border(1.dp, Color(0xFFE7E9EE), RoundedCornerShape(22.dp))
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(Color(0xFF111114)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = step.icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(21.dp)
            )
        }

        Spacer(Modifier.width(12.dp))

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = number.toString().padStart(2, '0'),
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = Color(0xFF9CA3AF),
                        fontWeight = FontWeight.Black,
                        fontSize = 11.sp,
                        lineHeight = 14.sp
                    )
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = stringResource(step.titleRes),
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = Color(0xFF111114),
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        lineHeight = 19.sp
                    )
                )
            }

            Text(
                text = stringResource(step.bodyRes),
                style = MaterialTheme.typography.bodySmall.copy(
                    color = Color(0xFF6B7280),
                    fontWeight = FontWeight.Medium,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            )
        }
    }
}

@Composable
private fun WidgetGuideTipCard() {
    val cardShape = RoundedCornerShape(24.dp)

    Card(
        modifier = Modifier
            .fillMaxWidth(),
        shape = cardShape,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 98.dp)
                .padding(horizontal = 14.dp, vertical = 17.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF111114)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Home,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Text(
                    text = stringResource(R.string.widget_guide_tip_title),
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = Color(0xFF111114),
                        fontWeight = FontWeight.Black,
                        fontSize = 16.sp,
                        lineHeight = 20.sp
                    )
                )

                Text(
                    text = stringResource(R.string.widget_guide_tip_body),
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color(0xFF5F6672),
                        fontWeight = FontWeight.Medium,
                        fontSize = 13.sp,
                        lineHeight = 19.sp
                    )
                )
            }
        }
    }
}

private data class WidgetGuideStep(
    val icon: ImageVector,
    @StringRes val titleRes: Int,
    @StringRes val bodyRes: Int
)
