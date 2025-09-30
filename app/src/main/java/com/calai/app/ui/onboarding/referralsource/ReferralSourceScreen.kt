package com.calai.app.ui.onboarding.referralsource

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import androidx.hilt.navigation.compose.hiltViewModel
import com.calai.app.R
import com.calai.app.ui.common.OnboardingProgress
import kotlinx.coroutines.launch

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
            Box(
            ) {
                Button(
                    onClick = {
                        scope.launch {
                            vm.saveAndContinue()
                            onNext()
                        }
                    },
                    enabled = true,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        // 1) 讓 CTA 永遠避開系統導覽列（手勢列）
                        .navigationBarsPadding() // 先避開手勢列
                        // 2) 額外再往上推一點（你要的「再往上一點」）
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
            Modifier
                .fillMaxSize()
                .padding(inner)
                .imePadding()
        ) {
            // ✅ 與性別頁相同位置與邊距的進度條
            OnboardingProgress(
                stepIndex = 2,       // 推薦來源 = 第 2 步
                totalSteps = 11,     // 與性別頁一致
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
                color = Color(0xFF111114),              // 可留可不留，與其他頁一致即可
                modifier = Modifier
                    .fillMaxWidth()                     // ✅ 給足寬度
                    .padding(horizontal = 20.dp, vertical = 15.dp),
                textAlign = TextAlign.Center            // ✅ 置中
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) {
                items(state.options) { opt ->
                    ReferralOptionItem(
                        option = opt,
                        selected = state.selected == opt.key,
                        onClick = { vm.select(opt.key) }
                    )
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
    onClick: () -> Unit
) {
    val bg = if (selected) Color.Black else Color(0xFFF1F3F7)
    val fg = if (selected) Color.White else Color.Black

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .clip(RoundedCornerShape(26.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            color = if (selected) Color.White.copy(alpha = 0.1f) else Color.White,
            shape = CircleShape,
            modifier = Modifier.size(44.dp)
        ) {
            if (option.iconRes != null) {
                Icon(
                    painter = painterResource(option.iconRes),
                    contentDescription = option.label,
                    tint = Color.Unspecified,
                    modifier = Modifier.padding(8.dp)
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
