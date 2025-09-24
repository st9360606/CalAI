// app/src/main/java/com/calai/app/ui/onboarding/GenderSelectionScreen.kt
package com.calai.app.ui.onboarding

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.calai.app.R
import com.calai.app.i18n.LocalLocaleController
import com.calai.app.i18n.LanguageManager
import com.calai.app.i18n.LanguageStore
import com.calai.app.i18n.flagAndLabelFromTag
import com.calai.app.ui.landing.LanguageDialog   // ← 直接沿用 Landing 的語言選單
import kotlinx.coroutines.launch
import java.util.Locale

enum class Gender { MALE, FEMALE, OTHER }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GenderSelectionScreen(
    onBack: () -> Unit,
    onNext: (Gender) -> Unit
) {
    var selected by remember { mutableStateOf<Gender?>(null) }

    // 語言切換所需
    val ctx = LocalContext.current
    val store = remember(ctx) { LanguageStore(ctx) }
    val scope = rememberCoroutineScope()
    val composeLocale = LocalLocaleController.current
    val currentTag = composeLocale.tag.ifBlank { Locale.getDefault().toLanguageTag() }
    val (flagEmoji, langLabel) = remember<Pair<String, String>>(currentTag) {
        flagAndLabelFromTag(currentTag)
    }

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
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    // ✅ 可點擊語言膠囊：打開 LanguageDialog 切換
                    LanguageBadge(
                        flag = flagEmoji,
                        label = langLabel,
                        modifier = Modifier.padding(end = 16.dp),
                        onClick = { if (!switching) showLang = true }
                    )
                }
            )
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
                // 進度條
                StepProgress(
                    totalSteps = 6,
                    currentStep = 1,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp, bottom = 12.dp)
                )

                // 標題與副標
                Text(
                    text = stringResource(R.string.onb_gender_title),
                    fontSize = 28.sp,
                    lineHeight = 34.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF111114)
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = stringResource(R.string.onb_gender_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF6B7280)
                )

                Spacer(Modifier.height(72.dp)) // 三個選項再往下

                // 選項群組（置中）
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val widthFraction = 0.94f
                    val optionHeight = 64.dp
                    val corner = 22.dp

                    GenderOption(
                        text = stringResource(R.string.male_simple),
                        selected = selected == Gender.MALE,
                        onClick = { selected = Gender.MALE },
                        widthFraction = widthFraction,
                        height = optionHeight,
                        corner = corner
                    )
                    Spacer(Modifier.height(16.dp))
                    GenderOption(
                        text = stringResource(R.string.female),
                        selected = selected == Gender.FEMALE,
                        onClick = { selected = Gender.FEMALE },
                        widthFraction = widthFraction,
                        height = optionHeight,
                        corner = corner
                    )
                    Spacer(Modifier.height(16.dp))
                    GenderOption(
                        text = stringResource(R.string.other),
                        selected = selected == Gender.OTHER,
                        onClick = { selected = Gender.OTHER },
                        widthFraction = widthFraction,
                        height = optionHeight,
                        corner = corner
                    )
                }

                Spacer(Modifier.weight(1f))
                Spacer(Modifier.height(12.dp))
            }

            // 底部「Continue」
            Button(
                onClick = { selected?.let(onNext) },
                enabled = selected != null,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 20.dp, vertical = 20.dp)
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(RoundedCornerShape(28.dp)),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Black,
                    contentColor = Color.White,
                    disabledContainerColor = Color(0xFFE5E7EB),
                    disabledContentColor = Color(0xFF9CA3AF)
                )
            ) {
                Text(
                    text = stringResource(R.string.continue_text),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }

    // 語言對話框（與 Landing 一致）
    if (showLang) {
        LanguageDialog(
            title = stringResource(R.string.choose_language),
            currentTag = currentTag,
            onPick = { picked ->
                if (switching) return@LanguageDialog
                switching = true
                showLang = false
                scope.launch {
                    // 1) Compose 層立即切
                    composeLocale.set(picked.tag)
                    // 2) AppCompat 層同步
                    LanguageManager.applyLanguage(picked.tag)
                    // 3) 持久化
                    store.save(picked.tag)
                    switching = false
                }
            },
            onDismiss = { showLang = false },
            maxWidth = 320.dp
        )
    }
}

/* ---------- 小組件 ---------- */

@Composable
private fun LanguageBadge(
    flag: String,
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick, role = Role.Button),
        color = Color.White,
        border = BorderStroke(1.dp, Color(0xFFE5E7EB)),
        shape = RoundedCornerShape(10.dp),
        shadowElevation = 0.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = flag, fontSize = 12.sp)
            Spacer(Modifier.width(6.dp))
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF111114)
            )
        }
    }
}

@Composable
private fun StepProgress(
    totalSteps: Int,
    currentStep: Int,
    modifier: Modifier = Modifier
) {
    val track = Color(0xFFEDEEF0)
    val bar = Color(0xFF111114)
    val fraction = (currentStep.coerceIn(0, totalSteps)).toFloat()
        .div(totalSteps.coerceAtLeast(1))

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(4.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(track),
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(fraction)
                .height(4.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(bar)
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
    val container = if (selected) Color(0xFF111114) else Color(0xFFF7F8FC)
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
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center
        )
    }
}
