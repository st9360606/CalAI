package com.calai.app

import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.calai.app.i18n.LanguageManager
import com.calai.app.i18n.LanguageStore
import com.calai.app.ui.BiteCalApp

import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.runBlocking

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        // 系統 Splash
        val splash = installSplashScreen()
        splash.setKeepOnScreenCondition { false }

        super.onCreate(savedInstanceState)

        // 若 AppCompat 已有語系（前一次切換已儲存且生效），就不要覆蓋
        val hasAppCompatLocale = !AppCompatDelegate.getApplicationLocales().isEmpty
        if (!hasAppCompatLocale) {
            // 第一次啟動或尚未設定：讀取你保存的偏好（沒有就交給系統語言）
            runBlocking {
                LanguageStore(this@MainActivity).langFlow.firstOrNull()?.let { saved ->
                    if (saved.isNotBlank()) LanguageManager.applyLanguage(saved)
                }
            }
        }

        // 可選：關閉這次啟動轉場（與語言切換的無動畫重啟搭配，更順）
         if (Build.VERSION.SDK_INT >= 34)
             overrideActivityTransition(OVERRIDE_TRANSITION_OPEN, 0, 0)
         else @Suppress("DEPRECATION") overridePendingTransition(0, 0)

        setContent { BiteCalApp() } // 進入 Compose NavHost（Landing → SignUp/SignIn）
    }
}
