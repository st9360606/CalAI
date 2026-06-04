package com.calai.bitecal.ui.auth

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.calai.bitecal.R

@Composable
fun SignUpScreen(onBack: () -> Unit, onSignedUp: () -> Unit) {
    Text(stringResource(R.string.signup_placeholder))
}
