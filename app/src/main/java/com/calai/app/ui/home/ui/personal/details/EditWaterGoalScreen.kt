package com.calai.app.ui.home.ui.personal.details

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Opacity
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.calai.app.ui.home.ui.personal.details.model.EditWaterGoalViewModel
import com.calai.app.ui.home.ui.weight.components.WeightTopBar
import kotlinx.coroutines.flow.collectLatest

@Composable
fun EditWaterGoalScreen(
    vm: EditWaterGoalViewModel,
    onBack: () -> Unit,
    onSaved: () -> Unit
) {
    val ui by vm.ui.collectAsState()
    val focus = LocalFocusManager.current

    LaunchedEffect(Unit) {
        vm.events.collectLatest { e ->
            if (e is EditWaterGoalViewModel.UiEvent.Saved) {
                focus.clearFocus()
                onSaved()
            }
        }
    }

    val textMain = Color(0xFF111114)
    val borderLight = Color(0xFFE5E7EB)

    Scaffold(
        containerColor = Color(0xFFF5F5F5),
        topBar = {
            WeightTopBar(
                title = "Edit Water Goal",
                onBack = onBack
            )
        }
    ) { inner ->
        Column(
            modifier = Modifier
                .padding(inner)
                .fillMaxSize()
                .imePadding()
                .navigationBarsPadding()
                .padding(horizontal = 22.dp)
                .padding(top = 14.dp, bottom = 20.dp)
        ) {
            Spacer(Modifier.height(60.dp))

            // --- previous goal card ---
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                color = Color.White,
                border = BorderStroke(1.dp, borderLight),
                shadowElevation = 0.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 17.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    WaterRingIcon(
                        modifier = Modifier
                            .size(88.dp)
                            .offset(x = 6.dp)
                    )

                    Column {
                        Text(
                            text = ui.previousGoalMl.toString(),
                            fontSize = 19.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF2F3947),
                            modifier = Modifier.padding(start = 20.dp)
                        )
                        Spacer(Modifier.height(3.dp))
                        Text(
                            text = "Previous goal ${ui.previousGoalMl} ml",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Normal,
                            color = Color(0xFF6B7280),
                            modifier = Modifier.padding(start = 20.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(22.dp))

            WaterGoalInputBox(
                value = ui.input,
                onValueChange = vm::onInputChange,
                isError = ui.error != null,
                onImeDone = {
                    if (ui.canSave()) vm.save()
                    focus.clearFocus()
                }
            )

            Spacer(Modifier.height(22.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                OutlinedButton(
                    onClick = { vm.revert() },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(999.dp),
                    border = BorderStroke(1.5.dp, Color(0xFF9CA3AF)),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Color.White,
                        contentColor = textMain
                    )
                ) {
                    Text(
                        text = "Revert",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                val enabled = ui.canSave()
                Button(
                    onClick = { vm.save() },
                    enabled = enabled,
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp),
                    shape = RoundedCornerShape(999.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (enabled) textMain else Color(0xFFE5E7EB),
                        contentColor = Color.White,
                        disabledContainerColor = Color(0xFFE5E7EB),
                        disabledContentColor = Color.White.copy(alpha = 0.7f)
                    )
                ) {
                    Text(
                        text = "Save",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun WaterGoalInputBox(
    value: String,
    onValueChange: (String) -> Unit,
    isError: Boolean,
    onImeDone: () -> Unit
) {
    val border = if (isError) Color(0xFFEF4444) else Color(0xFF111114)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(width = 2.dp, color = border, shape = RoundedCornerShape(12.dp))
            .padding(horizontal = 16.dp, vertical = 18.dp)
            .padding(start = 3.dp)
    ) {
        Text(
            text = "Daily Water Goal (ml)",
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF6B7280)
        )
        Spacer(Modifier.height(6.dp))

        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = TextStyle(
                fontSize = 18.sp,
                fontWeight = FontWeight.Normal,
                color = Color(0xFF111114)
            ),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = { onImeDone() }
            ),
            modifier = Modifier.fillMaxWidth()
        ) { inner ->
            Box(Modifier.fillMaxWidth()) { inner() }
        }
    }
}

/**
 * 圓環 + 水滴 icon（結構比照 StepRingIcon）
 */
@Composable
private fun WaterRingIcon(modifier: Modifier = Modifier) {
    val ringGrey = Color(0xFFD1D5DB)
    val ringBlack = Color(0xFF111114)
    val innerBg = Color(0xFFF2F4F7)

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = Stroke(width = size.minDimension * 0.055f, cap = StrokeCap.Round)
            val pad = stroke.width / 2f
            val arcSize = Size(size.width - pad * 2, size.height - pad * 2)

            drawArc(
                color = ringGrey,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(pad, pad),
                size = arcSize,
                style = stroke
            )
            drawArc(
                color = ringBlack,
                startAngle = -90f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = Offset(pad, pad),
                size = arcSize,
                style = stroke
            )
        }

        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(innerBg),
            contentAlignment = Alignment.Center
        ) {
            androidx.compose.material3.Icon(
                imageVector = Icons.Filled.Opacity,
                contentDescription = null,
                tint = Color(0xFF111114),
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
