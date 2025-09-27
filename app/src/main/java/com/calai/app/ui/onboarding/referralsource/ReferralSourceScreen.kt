package com.calai.app.ui.onboarding.referralsource

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.calai.app.R
import com.calai.app.i18n.LanguageManager
import com.calai.app.i18n.LanguageStore
import com.calai.app.i18n.LocalLocaleController
import com.calai.app.i18n.flagAndLabelFromTag
import com.calai.app.ui.common.FlagChip
import com.calai.app.ui.common.OnboardingProgress
import com.calai.app.ui.landing.LanguageDialog
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReferralSourceScreen(
    onBack: () -> Unit,
    onNext: () -> Unit,
    vm: ReferralSourceViewModel = hiltViewModel(),
) {
    val state by vm.uiState.collectAsState()
    val scope = rememberCoroutineScope()

    // 與性別頁相同的語言切換狀態
    val ctx = LocalContext.current
    val store = remember(ctx) { LanguageStore(ctx) }
    val composeLocale = LocalLocaleController.current
    val currentTag = composeLocale.tag.ifBlank { Locale.getDefault().toLanguageTag() }
    val (flagEmoji, langLabel) = remember(currentTag) { flagAndLabelFromTag(currentTag) }
    var showLang by remember { mutableStateOf(false) }
    var switching by remember { mutableStateOf(false) }

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
                Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(16.dp)
            ) {
                Button(
                    onClick = {
                        scope.launch {
                            vm.saveAndContinue()
                            onNext()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(28.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Black)
                ) {
                    Text(
                        text = stringResource(id = R.string.common_continue),
                        color = Color.White,
                        fontSize = 18.sp,
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

            Text(
                text = stringResource(id = R.string.onboard_referral_title),
                fontSize = 34.sp,
                fontWeight = FontWeight.ExtraBold,
                lineHeight = 40.sp,
                modifier = Modifier
                    .padding(horizontal = 20.dp, vertical = 16.dp)
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
            .height(76.dp)
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
