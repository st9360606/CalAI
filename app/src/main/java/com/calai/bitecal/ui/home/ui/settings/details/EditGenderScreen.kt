package com.calai.bitecal.ui.home.ui.settings.details

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
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
import com.calai.bitecal.ui.home.ui.settings.details.model.EditGenderViewModel
import com.calai.bitecal.ui.home.ui.weight.components.WeightTopBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditGenderScreen(
    vm: EditGenderViewModel,
    onBack: () -> Unit,
    onSaved: () -> Unit,
) {
    val ui by vm.ui.collectAsState()
    val savedGender by vm.genderState.collectAsState() // ✅ 可能是 null
    LaunchedEffect(Unit) { vm.refreshGenderFromServerIfNeeded() }

    // ✅ 關鍵：不要用預設 OTHER 當初始，先用 null
    var selected by rememberSaveable { mutableStateOf<EditGenderViewModel.GenderKey?>(null) }

    // ✅ 只在「尚未選過」時，才吃進資料層的 savedGender，避免覆蓋使用者手動點選
    LaunchedEffect(savedGender) {
        if (selected == null && savedGender != null) {
            selected = savedGender
        }
    }

    Scaffold(
        containerColor = Color(0xFFF5F5F5),
        topBar = {
            WeightTopBar(
                title = "Edit Your Gender",
                onBack = onBack
            )
        },
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, end = 20.dp, bottom = 40.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = {
                            val sel = selected ?: return@Button
                            vm.saveAndSyncGender(selected = sel, onSuccess = onSaved)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        enabled = !ui.saving && selected != null, // ✅ 沒載入完成不讓存
                        shape = RoundedCornerShape(28.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Black,
                            contentColor = Color.White
                        )
                    ) {
                        if (ui.saving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = Color.White
                            )
                        } else {
                            Text(
                                text = "Save",
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontWeight = FontWeight.Medium,
                                    letterSpacing = 0.2.sp
                                )
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(170.dp))

            ui.error?.let {
                Spacer(Modifier.height(10.dp))
                Text(
                    text = it,
                    color = Color(0xFFEF4444),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    textAlign = TextAlign.Center
                )
            }

            // ✅ 載入中：不選任何項目（不會先亮 OTHER）
            GenderSegmented(
                selected = selected,
                onSelect = { selected = it },
                modifier = Modifier.fillMaxWidth(0.88f)
            )

            Spacer(Modifier.height(22.dp))

            Text(
                text = "Used only to personalize and improve estimate accuracy.",
                fontSize = 12.sp,
                color = Color(0xFF9AA3AE),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(0.9f)
            )
        }
    }
}

@Composable
private fun GenderSegmented(
    selected: EditGenderViewModel.GenderKey?, // ✅ nullable
    onSelect: (EditGenderViewModel.GenderKey) -> Unit,
    modifier: Modifier = Modifier,
) {
    val widthFraction = 1f   // 原本 0.90f → 更寬（最多到 1f）
    val optionHeight = 65.dp    // 原本 68.dp → 更矮
    val corner = 32.dp          // 你原本 36.dp，縮一點比較順（可不改）

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        GenderOptionCard(
            text = "Male",
            selected = selected == EditGenderViewModel.GenderKey.MALE,
            onClick = { onSelect(EditGenderViewModel.GenderKey.MALE) },
            widthFraction = widthFraction,
            height = optionHeight,
            corner = corner
        )
        Spacer(Modifier.height(21.dp))
        GenderOptionCard(
            text = "Female",
            selected = selected == EditGenderViewModel.GenderKey.FEMALE,
            onClick = { onSelect(EditGenderViewModel.GenderKey.FEMALE) },
            widthFraction = widthFraction,
            height = optionHeight,
            corner = corner
        )
        Spacer(Modifier.height(21.dp))
        GenderOptionCard(
            text = "Other",
            selected = selected == EditGenderViewModel.GenderKey.OTHER,
            onClick = { onSelect(EditGenderViewModel.GenderKey.OTHER) },
            widthFraction = widthFraction,
            height = optionHeight,
            corner = corner
        )
    }
}

@Composable
private fun GenderOptionCard(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    widthFraction: Float,
    height: Dp,
    corner: Dp,
) {
    val shape = RoundedCornerShape(corner)
    val container = if (selected) Color(0xFF111114) else Color(0xFFE2E5EA)
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
            style = MaterialTheme.typography.bodyLarge.copy(
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.2.sp
            ),
            textAlign = TextAlign.Center
        )
    }
}
