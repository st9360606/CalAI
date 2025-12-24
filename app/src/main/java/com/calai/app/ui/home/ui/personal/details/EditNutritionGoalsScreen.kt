package com.calai.app.ui.home.ui.personal.details

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.Opacity
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.Spa
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.calai.app.ui.home.ui.personal.details.model.NutritionGoalsUiState
import com.calai.app.ui.home.ui.personal.details.model.NutritionGoalsViewModel

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

@Composable
private fun EditNutritionGoalsTopBar(onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(start = 20.dp, end = 20.dp)
            .padding(top = 10.dp, bottom = 6.dp)
    ) {
        Surface(
            modifier = Modifier.size(44.dp),
            shape = CircleShape,
            color = Color(0xFFF1F3F7)
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.size(44.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color(0xFF111114)
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        Text(
            text = "Edit nutrition goals",
            fontSize = 30.sp,
            lineHeight = 34.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF111114),
            style = MaterialTheme.typography.headlineLarge
        )
    }
}

private val GoalRowGap = 16.dp

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

    Scaffold(
        containerColor = Color(0xFFF7F7F7),
        topBar = { EditNutritionGoalsTopBar(onBack = { focusManager.clearFocus(); onBack() }) },
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
                .padding(horizontal = 18.dp)
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
                ringColor = Color(0xFF111114),
                icon = Icons.Outlined.LocalFireDepartment,
                label = "Calorie goal",
                value = ui.draft.kcal,
                errorText = ui.fieldErrors[NutritionGoalsUiState.Field.KCAL],
                onValueChange = onKcal
            )
            Spacer(Modifier.height(GoalRowGap))

            GoalRow(
                ringColor = Color(0xFFE07A7A),
                icon = Icons.Outlined.Restaurant,
                label = "Protein goal",
                value = ui.draft.proteinG,
                errorText = ui.fieldErrors[NutritionGoalsUiState.Field.PROTEIN],
                onValueChange = onProtein
            )
            Spacer(Modifier.height(GoalRowGap))

            GoalRow(
                ringColor = Color(0xFFDAA86B),
                icon = Icons.Outlined.Spa,
                label = "Carb goal",
                value = ui.draft.carbsG,
                errorText = ui.fieldErrors[NutritionGoalsUiState.Field.CARBS],
                onValueChange = onCarbs
            )
            Spacer(Modifier.height(GoalRowGap))

            GoalRow(
                ringColor = Color(0xFF6D8FD6),
                icon = Icons.Outlined.WaterDrop,
                label = "Fat goal",
                value = ui.draft.fatG,
                errorText = ui.fieldErrors[NutritionGoalsUiState.Field.FAT],
                onValueChange = onFat
            )

            Spacer(Modifier.height(8.dp))

            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable { onToggleMicros() }
                    .padding(vertical = 8.dp)
                    .padding(start = 18.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "View micronutrients",
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
                        ringColor = Color(0xFF8A78D6),
                        icon = Icons.Outlined.Favorite,
                        label = "Fiber goal",
                        value = ui.draft.fiberG,
                        errorText = ui.fieldErrors[NutritionGoalsUiState.Field.FIBER],
                        onValueChange = onFiber
                    )
                    Spacer(Modifier.height(GoalRowGap))

                    GoalRow(
                        ringColor = Color(0xFFE06FA0),
                        icon = Icons.Outlined.Opacity,
                        label = "Sugar goal",
                        value = ui.draft.sugarG,
                        errorText = ui.fieldErrors[NutritionGoalsUiState.Field.SUGAR],
                        onValueChange = onSugar
                    )
                    Spacer(Modifier.height(GoalRowGap))

                    GoalRow(
                        ringColor = Color(0xFFE0C46F),
                        icon = Icons.Outlined.Spa,
                        label = "Sodium goal",
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
                modifier = Modifier.size(56.dp)
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
                    .clickable { focusRequester.requestFocus() }
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
    val pill = RoundedCornerShape(999.dp)

    val horizontalPadding = 16.dp
    val buttonGap = 12.dp
    val buttonHeight = 46.dp
    val borderColor = Color(0xFF111114).copy(alpha = 0.45f)

    Surface(color = Color.Transparent) {
        Box(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
        ) {
            if (!dirty) {
                OutlinedButton(
                    onClick = onAutoGenerate,
                    shape = pill,
                    border = BorderStroke(1.dp, Color(0xFF111114).copy(alpha = 0.6f)),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Color.White,
                        contentColor = Color(0xFF111114)
                    ),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(start = 18.dp, end = 18.dp, bottom = 20.dp, top = 20.dp)
                        .height(50.dp)
                ) {
                    Text("Auto Generate Goals", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }
            } else {
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(
                            start = horizontalPadding,
                            end = horizontalPadding,
                            bottom = 20.dp,
                            top = 20.dp
                        ),
                    horizontalArrangement = Arrangement.spacedBy(buttonGap),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onRevert,
                        enabled = !saving,
                        shape = pill,
                        border = BorderStroke(1.dp, borderColor),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = Color.White,
                            contentColor = Color(0xFF111114)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(buttonHeight)
                    ) {
                        Text("Revert", fontSize = 16.sp, fontWeight = FontWeight.Medium)
                    }

                    Button(
                        onClick = onDone,
                        enabled = canDone,
                        shape = pill,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF111114),
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(buttonHeight)
                    ) {
                        if (saving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = Color.White
                            )
                            Spacer(Modifier.width(10.dp))
                        }
                        Text("Done", fontSize = 16.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    }
}
