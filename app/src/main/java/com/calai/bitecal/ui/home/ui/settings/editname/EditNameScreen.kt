package com.calai.bitecal.ui.home.ui.settings.editname

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.calai.bitecal.ui.home.ui.weight.components.WeightTopBar

@Composable
fun EditNameScreen(
    input: String,
    canSave: Boolean,
    isSaving: Boolean,
    errorText: String?,
    onBack: () -> Unit,
    onInputChange: (String) -> Unit,
    onSaved: () -> Unit
) {
    val focusManager = LocalFocusManager.current

    Scaffold(
        containerColor = Color(0xFFF5F5F5),
        topBar = {
            WeightTopBar(
                title = "Edit name",
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
            Spacer(Modifier.height(30.dp))

            NameField(
                value = input,
                onValueChange = onInputChange,
                onImeDone = {
                    if (canSave && !isSaving) {
                        focusManager.clearFocus()
                        onSaved()
                    }
                }
            )

            if (!errorText.isNullOrBlank()) {
                Spacer(Modifier.height(10.dp))
                Text(
                    text = errorText,
                    color = Color(0xFFEF4444),
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(Modifier.weight(1f))

            Button(
                onClick = onSaved,
                enabled = canSave && !isSaving,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF111114),
                    disabledContainerColor = Color(0xFFE5E7EB),
                    contentColor = Color.White,
                    disabledContentColor = Color.White.copy(alpha = 0.7f)
                ),
                shape = RoundedCornerShape(28.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .semantics { testTag = "doneButton" }

            ) {
                Text(
                    text = "Save",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 0.2.sp
                    )
                )
            }

            Spacer(Modifier.height(18.dp))
        }
    }
}

@Composable
private fun NameField(
    value: String,
    onValueChange: (String) -> Unit,
    onImeDone: () -> Unit
) {
    val shape = RoundedCornerShape(14.dp)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .clip(shape)
            .border(width = 2.dp, color = Color(0xFF111114), shape = shape)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = TextStyle(
                fontSize = 18.sp,
                color = Color(0xFF2A3440),
                fontWeight = FontWeight.Normal
            ),
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Words,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(onDone = { onImeDone() }),
            modifier = Modifier
                .fillMaxWidth()
                .semantics { testTag = "nameField" },
            decorationBox = { inner ->
                if (value.isBlank()) {
                    Text(
                        text = "Enter name here",
                        fontSize = 18.sp,
                        color = Color(0xFF737B88),
                        fontWeight = FontWeight.Normal
                    )
                }
                inner()
            }
        )
    }
}
