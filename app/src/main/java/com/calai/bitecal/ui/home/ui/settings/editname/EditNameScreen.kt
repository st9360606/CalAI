package com.calai.bitecal.ui.home.ui.settings.editname

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.calai.bitecal.R
import com.calai.bitecal.ui.common.design.BiteCalTopBar
import com.calai.bitecal.ui.common.haptic.hapticOnFocus
import com.calai.bitecal.ui.common.design.BiteCalScreenFrame
import com.calai.bitecal.ui.common.design.BiteCalEditBottomActionBar

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
    val scrollState = rememberScrollState()

    Scaffold(
        containerColor = Color(0xFFF5F5F5),
        topBar = {
            BiteCalTopBar(
                title = stringResource(R.string.settings_edit_your_name_title),
                onBack = onBack
            )
        },
        bottomBar = {
            BiteCalEditBottomActionBar(
                primaryText = stringResource(R.string.common_close),
                onPrimaryClick = onSaved,
                primaryEnabled = canSave && !isSaving,
                primaryLoading = isSaving,
                useImePadding = true,
                modifier = Modifier.semantics { testTag = "doneButton" }
            )
        }
    ) { inner ->
        Column(
            modifier = Modifier
                .padding(inner)
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = BiteCalScreenFrame.contentHorizontalComfort)
                .padding(top = BiteCalScreenFrame.detailTop, bottom = BiteCalScreenFrame.detailBottom)
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

            Spacer(Modifier.height(24.dp))
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
            .padding(horizontal = BiteCalScreenFrame.contentHorizontalCompact),
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
                .hapticOnFocus()
                .semantics { testTag = "nameField" },
            decorationBox = { inner ->
                if (value.isBlank()) {
                    Text(
                        text = stringResource(R.string.edit_name_placeholder),
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
