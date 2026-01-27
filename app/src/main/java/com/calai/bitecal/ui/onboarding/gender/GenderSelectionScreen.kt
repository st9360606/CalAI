package com.calai.bitecal.ui.onboarding.gender

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
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
import com.calai.bitecal.R
import com.calai.bitecal.i18n.LanguageManager
import com.calai.bitecal.i18n.LanguageStore
import com.calai.bitecal.i18n.LocalLocaleController
import com.calai.bitecal.i18n.flagAndLabelFromTag
import com.calai.bitecal.ui.common.FlagChip
import com.calai.bitecal.ui.common.OnboardingProgress
import com.calai.bitecal.ui.landing.LanguageDialog
import kotlinx.coroutines.launch
import java.util.Locale
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.saveable.rememberSaveable

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

    var showLang by rememberSaveable { mutableStateOf(false) }
    var switching by rememberSaveable { mutableStateOf(false) }
    val closeLangDialog = remember { { showLang = false } }
    Scaffold(
        containerColor = Color.White,
        topBar = {
            TopAppBar(modifier = Modifier.padding(start = 16.dp, end = 16.dp),
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White,
                    navigationIconContentColor = Color(0xFF111114)
                ),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Box(
                            modifier = Modifier
                                .size(46.dp)
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
                title = {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(end = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OnboardingProgress(
                            stepIndex = 1,
                            totalSteps = 11,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                actions = {
                    FlagChip(
                        flag = flagEmoji,
                        label = langLabel,
                        modifier = Modifier.offset(y = (-8).dp),
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
                            vm.saveSelectedGender()
                            onNext(requireNotNull(state.selected)) // 保證非空再前進
                        }
                    },
                    enabled = state.selected != null, // 沒選就不能按
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(start = 20.dp, end = 20.dp, bottom = 40.dp)
                        .fillMaxWidth()
                        .height(64.dp),
                    shape = RoundedCornerShape(999.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Black,
                        contentColor = Color.White
                    )
                ) {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.continue_text),
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Medium,
                                letterSpacing = 0.2.sp
                            ),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    ) { inner ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(inner),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.onb_gender_title),
                fontSize = 34.sp,
                fontWeight = FontWeight.ExtraBold,
                lineHeight = 40.sp,
                color = Color(0xFF111114),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp),
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.onb_gender_subtitle),
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = Color(0xFF9AA3AF),
                    lineHeight = 20.sp
                ),
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(130.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val widthFraction = 0.88f
                val optionHeight = 70.dp
                val corner = 999.dp

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
                closeLangDialog()
                scope.launch {
                    composeLocale.set(picked.tag)
                    LanguageManager.applyLanguage(picked.tag)
                    store.save(picked.tag)
                    switching = false
                }
            },
            onDismiss = closeLangDialog,
            widthFraction = 0.92f,     // 92% 的螢幕寬
            maxHeightFraction = 0.60f  // 60% 的螢幕高
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
    val content = if (selected) Color.White else Color.Black

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
            style = MaterialTheme.typography.bodyLarge.copy(
                fontWeight = FontWeight.SemiBold,
                fontSize = 19.sp,
                letterSpacing = 0.2.sp
            ),
            textAlign = TextAlign.Center
        )
    }
}
