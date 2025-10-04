package com.calai.app.ui.nav

import androidx.navigation.NavController

fun NavController.navigateToOnboardAfterLogin() {
    // 進到性別頁，清掉登入流程，但保留 Landing → 可返回
    navigate(Routes.ONBOARD_GENDER) {
        popUpTo(Routes.LANDING) {
            inclusive = false   // ★ 不要把 Landing 移除
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }
}
