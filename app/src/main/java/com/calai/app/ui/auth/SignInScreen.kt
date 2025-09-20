package com.calai.app.ui.auth

import android.app.Activity
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignInScreen(
    onBack: () -> Unit,
    onSignedIn: () -> Unit,
    vm: SignInViewModel = hiltViewModel()
) {
    val ctx = LocalContext.current
    val activity = ctx as Activity

    var errorText by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        vm.events.collect { e ->
            when (e) {
                is SignInViewModel.Event.Loading -> {
                    loading = true
                    errorText = null
                }
                is SignInViewModel.Event.Error -> {
                    loading = false
                    errorText = e.message
                }
                is SignInViewModel.Event.SignedIn -> {
                    loading = false
                    onSignedIn()
                }
                SignInViewModel.Event.Idle -> Unit
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Sign In") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Text("←") }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (loading) {
                CircularProgressIndicator()
            }

            Button(
                onClick = { vm.signInWithGoogle(activity) },
                enabled = !loading,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("以 Google 繼續")
            }

            // 預留 Apple / Email（之後可接）
            OutlinedButton(
                onClick = { /* TODO: Apple */ },
                enabled = !loading,
                modifier = Modifier.fillMaxWidth()
            ) { Text("以 Apple 繼續（稍後）") }

            OutlinedButton(
                onClick = { /* TODO: Email */ },
                enabled = !loading,
                modifier = Modifier.fillMaxWidth()
            ) { Text("以 Email 繼續（稍後）") }

            errorText?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        }
    }
}
