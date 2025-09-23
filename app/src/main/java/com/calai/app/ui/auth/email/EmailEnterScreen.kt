package com.calai.app.ui.auth.email

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.calai.app.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmailEnterScreen(
    vm: EmailSignInViewModel,
    onBack: () -> Unit,
    onSent: (String) -> Unit
) {
    val ui by vm.enter.collectAsState()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                }
            )
        }
    ) { pad ->
        Column(
            modifier = Modifier
                .padding(pad)
                .padding(horizontal = 20.dp)
                .fillMaxSize()
        ) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.sign_in_title),
                style = MaterialTheme.typography.headlineLarge.copy(fontSize = 34.sp)
            )
            Spacer(Modifier.height(24.dp))

            OutlinedTextField(
                value = ui.email,
                onValueChange = vm::onEmailChange,
                label = { Text(stringResource(R.string.email_label)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Done
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(28.dp))

            Button(
                onClick = { vm.sendCode(onSent) },
                enabled = ui.isValid && !ui.loading,
                shape = MaterialTheme.shapes.extraLarge,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Text(
                    if (ui.loading) stringResource(R.string.sending)
                    else stringResource(R.string.continue_btn)
                )
            }

            ui.error?.let {
                Spacer(Modifier.height(12.dp))
                Text(it, color = MaterialTheme.colorScheme.error)
            }

            Spacer(Modifier.weight(1f))
        }
    }
}
