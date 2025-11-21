// app/src/main/java/com/calai/app/ui/onboarding/gender/GenderSelectionScreen.kt
package com.calai.app.ui.onboarding.gender

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
                    // 與登入頁一致的語言膠囊
                    FlagChip(
                        flag = flagEmoji,
                        label = langLabel,
                        modifier = Modifier
                            .align(Alignment.CenterVertically)
                            .windowInsetsPadding(
                                WindowInsets.displayCutout.union(WindowInsets.statusBars)
                            )
                            .padding(top = 0.dp, end = 16.dp)
                            .offset(y = (-11).dp),
                        onClick = { if (!switching) showLang = true }
                    )
                }
            )
        },
        bottomBar = {
            Box {
                Button(
                    onClick = {
                        scope.launch {
                            vm.saveSelectedGender()                            // 寫入 DataStore
                            onNext(requireNotNull(state.selected))             // 保證非空再前進
                        }
                    },
                    enabled = state.selected != null,                          // ← 沒選就不能按
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
                .imePadding()
        ) {
            OnboardingProgress(
                stepIndex = 1,
                totalSteps = 11,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
            )

            Spacer(Modifier.height(6.dp))

            Text(
                text = stringResource(R.string.onb_gender_title),
                fontSize = 34.sp,
                fontWeight = FontWeight.ExtraBold,
                lineHeight = 40.sp,
                color = Color(0xFF111114),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 15.dp),
                textAlign = TextAlign.Center
            )

            Text(
                text = stringResource(R.string.onb_gender_subtitle),
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
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val widthFraction = 0.88f
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
                Spacer(Modifier.height(21.dp))
                GenderOption(
                    text = stringResource(R.string.female),
                    selected = state.selected == GenderKey.FEMALE,
                    onClick = { vm.select(GenderKey.FEMALE) },
                    widthFraction = widthFraction,
                    height = optionHeight,
                    corner = corner
                )
                Spacer(Modifier.height(21.dp))
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

    if (showLang) {
        LanguageDialog(
            title = stringResource(R.string.choose_language),
            currentTag = currentTag,
            onPick = { picked ->
                if (switching) return@LanguageDialog
                switching = true
                showLang = false
                scope.launch {
                    composeLocale.set(picked.tag)
                    LanguageManager.applyLanguage(picked.tag)
                    store.save(picked.tag)
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
