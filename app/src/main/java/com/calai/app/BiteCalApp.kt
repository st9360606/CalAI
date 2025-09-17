package com.calai.app

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.calai.app.ui.landing.LandingScreen

@Composable
fun BiteCalApp() {
    val nav = rememberNavController()
    NavHost(navController = nav, startDestination = "landing") {
        composable("landing") {
            LandingScreen(           // ← 不要再寫 ui.LandingScreen，直接用已 import 的名稱
                onStart = { nav.navigate("signup") },
                onLogin = { nav.navigate("signin") }
            )
        }
        composable("signup") { /* TODO: SignUpScreen() */ }
        composable("signin") { /* TODO: SignInScreen() */ }
    }
}
