package com.calai.bitecal.ui.home.ui.settings.details

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BakeryDining
import androidx.compose.material.icons.filled.EggAlt
import androidx.compose.material.icons.filled.Icecream
import androidx.compose.material.icons.filled.Opacity
import androidx.compose.material.icons.filled.RiceBowl
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.calai.bitecal.R
import com.calai.bitecal.ui.common.design.BiteCalTopBar
import com.calai.bitecal.ui.home.ui.settings.details.model.NutritionGoalsUiState
import com.calai.bitecal.ui.home.ui.settings.details.model.NutritionGoalsViewModel
import com.calai.bitecal.ui.common.haptic.biteCalClickable
import com.calai.bitecal.ui.common.haptic.clickWithoutHaptic
import com.calai.bitecal.ui.common.haptic.hapticOnFocus
import com.calai.bitecal.ui.common.design.BiteCalScreenFrame
import com.calai.bitecal.ui.common.design.BiteCalEditDualActionRow
import com.calai.bitecal.ui.common.design.BiteCalSecondaryOutlinedButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditNutritionGoalsRoute(
    onBack: () -> Unit,
    onAutoGenerate: () -> Unit,
    onSaved: () -> Unit, // ✅ 新增
    vm: NutritionGoalsViewModel
) {
    val ui by vm.ui.collectAsState()
    val focusManager = LocalFocusManager.current

    LaunchedEffect(Unit) { vm.loadIfNeeded() }

    // ✅ 儲存成功 → 跟 EditDailyStepGoalScreen 一樣：回上一頁讓上一頁跳 toast
    LaunchedEffect(Unit) {
        vm.events.collect { e ->
            if (e is NutritionGoalsViewModel.UiEvent.Saved) {
                focusManager.clearFocus(force = true)
                onSaved()
            }
        }
    }

    val handleBack: () -> Unit = {
        focusManager.clearFocus(force = true)
        vm.revert()
        onBack()
    }

    BackHandler(onBack = handleBack)

    DisposableEffect(Unit) {
        onDispose { vm.revert() }
    }

    EditNutritionGoalsScreen(
        ui = ui,
        onBack = handleBack,
        onAutoGenerate = onAutoGenerate,
        onToggleMicros = vm::toggleMicros,
        onRevert = vm::revert,
        onDone = vm::done,
        onKcal = vm::onKcal,
        onProtein = vm::onProtein,
        onCarbs = vm::onCarbs,
        onFat = vm::onFat,
        onFiber = vm::onFiber,
        onSugar = vm::onSugar,
        onSodium = vm::onSodium
    )
}
private val GoalRowGap = 16.dp

@Composable
private fun EditNutritionGoalsNoImePanEffect() {
    val view = LocalView.current

    DisposableEffect(view) {
        val window = view.context.findActivity()?.window

        if (window == null) {
            onDispose { }
        } else {
            val originalSoftInputMode = window.attributes.softInputMode
            val originalState = originalSoftInputMode and WindowManager.LayoutParams.SOFT_INPUT_MASK_STATE

            window.setSoftInputMode(
                originalState or WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING
            )

            onDispose {
                window.setSoftInputMode(originalSoftInputMode)
            }
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditNutritionGoalsScreen(
    ui: NutritionGoalsUiState,
    onBack: () -> Unit,
    onAutoGenerate: () -> Unit,
    onToggleMicros: () -> Unit,
    onRevert: () -> Unit,
    onDone: () -> Unit,
    onKcal: (String) -> Unit,
    onProtein: (String) -> Unit,
    onCarbs: (String) -> Unit,
    onFat: (String) -> Unit,
    onFiber: (String) -> Unit,
    onSugar: (String) -> Unit,
    onSodium: (String) -> Unit
) {
    val focusManager = LocalFocusManager.current

    EditNutritionGoalsNoImePanEffect()

    Scaffold(
        containerColor = Color(0xFFF7F7F7),
        topBar = {
            BiteCalTopBar(
                title = "Edit Nutrition Goals",
                onBack = {
                    focusManager.clearFocus()
                    onBack()
                }
            )
        },
        bottomBar = {
            BottomActionBar(
                dirty = ui.isDirty,
                saving = ui.saving,
                canDone = ui.canDone,
                onAutoGenerate = { focusManager.clearFocus(); onAutoGenerate() },
                onRevert = { focusManager.clearFocus(); onRevert() },
                onDone = { focusManager.clearFocus(); onDone() }
            )
        }
    ) { inner ->
        if (ui.loading) {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(inner),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .padding(inner)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = BiteCalScreenFrame.contentHorizontalMedium)
                .padding(bottom = 110.dp)
        ) {
            Spacer(Modifier.height(24.dp))

            // 全域錯誤（網路/未知）
            if (!ui.error.isNullOrBlank()) {
                Text(
                    text = ui.error,
                    color = Color(0xFFB91C1C),
                    fontSize = 13.sp,
                    modifier = Modifier.padding(start = 6.dp, bottom = 12.dp)
                )
            }

            GoalRow(
                ringColor = Color(0xFF1F1A17),
                icon = Icons.Outlined.LocalFireDepartment,
                label = stringResource(R.string.edit_nutrition_calorie_goal),
                value = ui.draft.kcal,
                errorText = ui.fieldErrors[NutritionGoalsUiState.Field.KCAL],
                onValueChange = onKcal
            )
            Spacer(Modifier.height(GoalRowGap))

            GoalRow(
                ringColor = Color(0xFFE56C6C),
                icon = Icons.Filled.EggAlt,
                label = stringResource(R.string.edit_nutrition_protein_goal),
                value = ui.draft.proteinG,
                errorText = ui.fieldErrors[NutritionGoalsUiState.Field.PROTEIN],
                onValueChange = onProtein
            )
            Spacer(Modifier.height(GoalRowGap))

            GoalRow(
                ringColor = Color(0xFFD89A62),
                icon = Icons.Filled.BakeryDining,
                label = stringResource(R.string.edit_nutrition_carb_goal),
                value = ui.draft.carbsG,
                errorText = ui.fieldErrors[NutritionGoalsUiState.Field.CARBS],
                iconSize = 20.dp,
                onValueChange = onCarbs
            )
            Spacer(Modifier.height(GoalRowGap))

            GoalRow(
                ringColor = Color(0xFF6C93D8),
                icon = Icons.Filled.Opacity,
                label = stringResource(R.string.edit_nutrition_fat_goal),
                value = ui.draft.fatG,
                errorText = ui.fieldErrors[NutritionGoalsUiState.Field.FAT],
                onValueChange = onFat
            )

            Spacer(Modifier.height(8.dp))

            Row(
                Modifier
                    .fillMaxWidth()
                    .biteCalClickable { onToggleMicros() }
                    .padding(vertical = 8.dp)
                    .padding(start = 18.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.edit_nutrition_view_micronutrients),
                    fontSize = 15.sp,
                    color = Color(0xFF606A78),
                    fontWeight = FontWeight.Normal
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = if (ui.expandedMicros) "▴" else "▾",
                    fontSize = 22.sp,
                    color = Color(0xFF606A78),
                    fontWeight = FontWeight.Bold
                )
            }

            AnimatedVisibility(
                visible = ui.expandedMicros,
                enter = fadeIn(tween(150)) + expandVertically(tween(150)),
                exit = fadeOut(tween(150)) + shrinkVertically(tween(150))
            ) {
                Column {
                    Spacer(Modifier.height(6.dp))

                    GoalRow(
                        ringColor = Color(0xFFA78BFA),
                        icon = Icons.Filled.Spa,
                        label = stringResource(R.string.edit_nutrition_fiber_goal),
                        value = ui.draft.fiberG,
                        errorText = ui.fieldErrors[NutritionGoalsUiState.Field.FIBER],
                        onValueChange = onFiber
                    )
                    Spacer(Modifier.height(GoalRowGap))

                    GoalRow(
                        ringColor = Color(0xFFF08AAF),
                        icon = Icons.Filled.Icecream,
                        label = stringResource(R.string.edit_nutrition_sugar_goal),
                        value = ui.draft.sugarG,
                        errorText = ui.fieldErrors[NutritionGoalsUiState.Field.SUGAR],
                        onValueChange = onSugar
                    )
                    Spacer(Modifier.height(GoalRowGap))

                    GoalRow(
                        ringColor = Color(0xFF73B6E6),
                        icon = Icons.Filled.RiceBowl,
                        label = stringResource(R.string.edit_nutrition_sodium_goal),
                        value = ui.draft.sodiumMg,
                        errorText = ui.fieldErrors[NutritionGoalsUiState.Field.SODIUM],
                        onValueChange = onSodium
                    )
                }
            }
        }
    }
}

@Composable
private fun GoalRow(
    ringColor: Color,
    icon: ImageVector,
    label: String,
    value: String,
    errorText: String?,
    iconSize: Dp = 16.dp,
    onValueChange: (String) -> Unit
) {
    val focusRequester = remember { FocusRequester() }
    var focused by remember { mutableStateOf(false) }

    val hasError = !errorText.isNullOrBlank()

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RingIcon(
                color = ringColor,
                icon = icon,
                modifier = Modifier.size(56.dp),
                iconSize = iconSize
            )

            Spacer(Modifier.width(14.dp))

            val bg = if (focused) Color.White else Color(0xFFECECF0).copy(alpha = 0.9f)

            // ✅ 規則：有錯誤 → 永遠紅框；沒錯誤 → focus 黑框、非 focus 無框
            val borderColor = when {
                hasError -> Color(0xFFB91C1C)
                focused -> Color(0xFF111114)
                else -> Color.Transparent
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(64.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(bg)
                    .border(1.5.dp, borderColor, RoundedCornerShape(16.dp))
                    .clickWithoutHaptic { focusRequester.requestFocus() }
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Column {
                    Text(
                        text = label,
                        fontSize = 14.sp,
                        color = Color(0xFF566171),
                        fontWeight = FontWeight.Normal
                    )
                    Spacer(Modifier.height(4.dp))

                    BasicTextField(
                        value = value,
                        onValueChange = onValueChange,
                        singleLine = true,
                        textStyle = TextStyle(
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF111114).copy(alpha = 0.9f)
                        ),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 1.dp)
                            .focusRequester(focusRequester)
                            .hapticOnFocus()
                            .onFocusChanged { focused = it.isFocused }
                    )
                }
            }
        }

        val err = errorText?.takeIf { it.isNotBlank() }
        if (err != null) {
            Spacer(Modifier.height(6.dp))
            Text(
                text = err,
                color = Color(0xFFB91C1C),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(start = 56.dp + 14.dp + 6.dp)
            )
        }
    }
}

@Composable
private fun RingIcon(
    color: Color,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    progress: Float = 0.5f,
    innerCircleSize: Dp = 30.dp,
    strokeWidth: Dp = 6.dp,
    iconSize: Dp = 16.dp
) {
    val p = progress.coerceIn(0f, 1f)

    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)
            drawArc(
                color = Color(0xFFE5E7EB),
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                style = stroke
            )
            drawArc(
                color = color,
                startAngle = -90f,
                sweepAngle = 360f * p,
                useCenter = false,
                style = stroke
            )
        }

        Box(
            modifier = Modifier
                .size(innerCircleSize)
                .clip(CircleShape)
                .background(Color(0xFFECECF0)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(iconSize)
            )
        }
    }
}

@Composable
private fun BottomActionBar(
    dirty: Boolean,
    saving: Boolean,
    canDone: Boolean,
    onAutoGenerate: () -> Unit,
    onRevert: () -> Unit,
    onDone: () -> Unit
) {
    Surface(color = Color.Transparent) {
        Box(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
        ) {
            if (!dirty) {
                BiteCalSecondaryOutlinedButton(
                    text = stringResource(R.string.edit_nutrition_auto_generate_goals),
                    onClick = onAutoGenerate,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(
                            start = BiteCalScreenFrame.contentHorizontalMedium,
                            end = BiteCalScreenFrame.contentHorizontalMedium,
                            top = BiteCalScreenFrame.detailBottom,
                            bottom = BiteCalScreenFrame.detailBottom,
                        ),
                    height = 55.dp,
                    borderColor = Color(0xFF111114).copy(alpha = 0.6f),
                    contentColor = Color(0xFF111114),
                )
            } else {
                BiteCalEditDualActionRow(
                    secondaryText = stringResource(R.string.common_revert),
                    onSecondaryClick = onRevert,
                    primaryText = stringResource(R.string.done),
                    onPrimaryClick = onDone,
                    primaryEnabled = canDone,
                    secondaryEnabled = !saving,
                    primaryLoading = saving,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(
                            start = BiteCalScreenFrame.contentHorizontalMedium,
                            end = BiteCalScreenFrame.contentHorizontalMedium,
                            top = BiteCalScreenFrame.detailBottom,
                            bottom = BiteCalScreenFrame.detailBottom,
                        ),
                )
            }
        }
    }
}
