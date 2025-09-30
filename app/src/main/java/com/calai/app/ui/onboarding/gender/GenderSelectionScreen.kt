// app/src/main/java/com/calai/app/ui/onboarding/gender/GenderSelectionScreen.kt
package com.calai.app.ui.onboarding.gender

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
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
fun GenderSelectionScreen(
    onBack: () -> Unit,
    onNext: (GenderKey) -> Unit,
    vm: GenderSelectionViewModel = hiltViewModel()
) {
    val state by vm.uiState.collectAsState()
    val scope = rememberCoroutineScope()

    // 語言切換所需
    val ctx = LocalContext.current
    val store = remember(ctx) { LanguageStore(ctx) }
    val composeLocale = LocalLocaleController.current
    val currentTag = composeLocale.tag.ifBlank { Locale.getDefault().toLanguageTag() }
    val (flagEmoji, langLabel) = remember(currentTag) { flagAndLabelFromTag(currentTag) }

    var showLang by remember { mutableStateOf(false) }
    var switching by remember { mutableStateOf(false) }

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
                },
                actions = {
                    // ★ 與登入頁一致的語言膠囊
                    FlagChip(
                        flag = flagEmoji,
                        label = langLabel,
                        modifier = Modifier
                            .align(Alignment.CenterVertically) // ✅ 正確用法
                            // 1) 先吃安全區：狀態列 + 瀏海（有瀏海的機種更穩）
                            .windowInsetsPadding(
                                WindowInsets.displayCutout.union(WindowInsets.statusBars)
                            )
                            // 2) 再做視覺微調：往內與往下各一些
                            .padding(top = 0.dp, end = 16.dp)
                            .offset(y = (-11).dp), // 或 (-4).dp 視覺微調；請確認不會被狀態列遮住
                        onClick = { if (!switching) showLang = true }
                    )
                }
            )
        },
        bottomBar = {
            Box(
            ) {
                Button(
                    onClick = {
                        scope.launch {
                            vm.saveSelectedGender()      // 寫入 DataStore（gender）
                            onNext(state.selected)       // 回傳選到的 GenderKey
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
                .background(Color.White)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp)
            ) {
                // 進度條（統一 1/6；依你實際流程調整）
                OnboardingProgress(
                    stepIndex = 1,
                    totalSteps = 11,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp, bottom = 12.dp)
                )

                // 標題（更大）
                Text(
                    text = stringResource(R.string.onb_gender_title),
                    fontSize = 34.sp,
                    lineHeight = 36.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF111114)
                )
                Spacer(Modifier.height(6.dp))
                // 副標（更大）
                Text(
                    text = stringResource(R.string.onb_gender_subtitle),
                    fontSize = 17.sp,
                    color = Color(0xFF6B7280),
                    lineHeight = 22.sp
                )

                Spacer(Modifier.height(120.dp)) // 讓三個選項更靠下

                // 選項群組（置中）
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val widthFraction = 0.94f
                    val optionHeight = 72.dp
                    val corner = 26.dp

                    GenderOption(
                        text = stringResource(R.string.male_simple),
                        selected = state.selected == GenderKey.MALE,
                        onClick = { vm.select(GenderKey.MALE) },
                        widthFraction = widthFraction,
                        height = optionHeight,
                        corner = corner
                    )
                    Spacer(Modifier.height(16.dp))
                    GenderOption(
                        text = stringResource(R.string.female),
                        selected = state.selected == GenderKey.FEMALE,
                        onClick = { vm.select(GenderKey.FEMALE) },
                        widthFraction = widthFraction,
                        height = optionHeight,
                        corner = corner
                    )
                    Spacer(Modifier.height(16.dp))
                    GenderOption(
                        text = stringResource(R.string.other),
                        selected = state.selected == GenderKey.OTHER,
                        onClick = { vm.select(GenderKey.OTHER) },
                        widthFraction = widthFraction,
                        height = optionHeight,
                        corner = corner
                    )
                }
            }
        }
    }

    // 語言對話框
    if (showLang) {
        LanguageDialog(
            title = stringResource(R.string.choose_language),
            currentTag = currentTag,
            onPick = { picked ->
                if (switching) return@LanguageDialog
                switching = true
                showLang = false
                scope.launch {
                    composeLocale.set(picked.tag)              // Compose 層立即切
                    LanguageManager.applyLanguage(picked.tag)  // AppCompat 層同步
                    store.save(picked.tag)                     // 持久化
                    switching = false
                }
            },
            onDismiss = { showLang = false },
            maxWidth = 320.dp
        )
    }
}

@Composable
private fun GenderOption(
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

    // 移除 ripple/拖移感
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
