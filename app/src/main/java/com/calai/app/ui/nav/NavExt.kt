// app/src/main/java/com/calai/app/ui/nav/NavExt.kt
package com.calai.app.ui.nav

import androidx.navigation.NavController

fun NavController.navigateToOnboardAfterLogin() {
    navigate(Routes.ONBOARD_GENDER) {
        popUpTo(Routes.LANDING) { inclusive = true } // 正確寫法：在 popUpTo 的 lambda 內
        launchSingleTop = true
    }
}
