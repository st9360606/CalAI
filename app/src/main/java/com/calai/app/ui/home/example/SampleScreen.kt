package com.calai.app.ui.home.example

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun HomeScreen(
    onSignOut: (() -> Unit)? = null
) {
    Scaffold { inner ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Welcome to BiteCal 👋",
                    style = MaterialTheme.typography.headlineMedium
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "這是暫時的主畫面（佔位），之後換成真實首頁。",
                    style = MaterialTheme.typography.bodyMedium
                )
                if (onSignOut != null) {
                    Spacer(Modifier.height(24.dp))
                    Button(onClick = onSignOut) { Text("Sign out (debug)") }
                }
            }
        }
    }
}
