package com.calai.app.ui.onboarding.exercise

import androidx.annotation.DrawableRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.calai.app.R
import com.calai.app.ui.common.OnboardingProgress

// 與推薦來源頁一致：item 寬度佔螢幕 90%
private const val OPTION_WIDTH_FRACTION = 0.90f

private data class ExerciseUiOption(
    val value: Int,                 // 0 / 2 / 4 / 6 / 7
    @DrawableRes val iconRes: Int,
    val titleRes: Int,
    val subRes: Int
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseFrequencyScreen(
    vm: ExerciseFrequencyViewModel,
    onBack: () -> Unit,
    onNext: () -> Unit
) {
    val state by vm.uiState.collectAsState()

    val options = listOf(
        ExerciseUiOption(0, R.drawable.working,        R.string.ex_freq_0_title,         R.string.ex_freq_0_sub),
        ExerciseUiOption(2, R.drawable.running,        R.string.ex_freq_1_3_title,       R.string.ex_freq_1_3_sub),
        ExerciseUiOption(4, R.drawable.cycling,         R.string.ex_freq_3_5_title,       R.string.ex_freq_3_5_sub),
        ExerciseUiOption(6, R.drawable.weight_lifting,  R.string.ex_freq_6_7_plus_title,  R.string.ex_freq_6_7_plus_sub),
        ExerciseUiOption(7, R.drawable.muscle,          R.string.ex_freq_7_plus_title,    R.string.ex_freq_7_plus_sub)
    )

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
                        vm.saveSelected()
                        onNext()
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
                stepIndex = 6,
                totalSteps = 11,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
            )

            Spacer(Modifier.height(6.dp))

            Text(
                text = stringResource(id = R.string.onboard_ex_freq_title),
                fontSize = 34.sp,
                fontWeight = FontWeight.ExtraBold,
                lineHeight = 40.sp,
                color = Color(0xFF111114),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 15.dp),
                textAlign = TextAlign.Center
            )

            // 內容清單：置中 + 縮窄寬度（與推薦來源頁一致）
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp) // ← 加這行
            ) {
                items(options, key = { it.value }) { opt ->
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        ExerciseOptionItem(
                            option = opt,
                            selected = state.selected == opt.value,
                            onClick = { vm.select(opt.value) },
                            modifier = Modifier.fillMaxWidth(OPTION_WIDTH_FRACTION)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ExerciseOptionItem(
    option: ExerciseUiOption,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bg by animateColorAsState(
        targetValue = if (selected) Color.Black else Color(0xFFF1F3F7),
        label = "exercise-bg"
    )
    val fg = if (selected) Color.White else Color.Black
    val interaction = remember { MutableInteractionSource() }

    Row(
        modifier = modifier
            .height(80.dp)
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
        // 左側固定純白圓底 + 彩色圖示
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(Color.White),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(option.iconRes),
                contentDescription = null,
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(Modifier.width(16.dp))

        Column(Modifier.weight(1f)) {
            Text(
                text = stringResource(option.titleRes),
                color = fg,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = stringResource(option.subRes),
                color = fg.copy(alpha = 0.8f),
                fontSize = 14.sp,
                lineHeight = 18.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
