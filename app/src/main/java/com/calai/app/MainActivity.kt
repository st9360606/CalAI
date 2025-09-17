package com.calai.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.lifecycleScope
import com.calai.app.i18n.LanguageManager
import com.calai.app.i18n.LanguageStore
import com.calai.app.ui.*
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        splash.setKeepOnScreenCondition { false }
        super.onCreate(savedInstanceState)

        // ⚠️ 若 AppCompat 已經有語系（前一次切換已生效且 autoStoreLocales=true），就不要再覆蓋
        val hasAppCompatLocale = !AppCompatDelegate.getApplicationLocales().isEmpty

        if (!hasAppCompatLocale) {
            // 第一次啟動或尚未設定 → 用你保存的偏好（若沒有就交給系統語言）
            runBlocking {
                LanguageStore(this@MainActivity).langFlow.firstOrNull()?.let { saved ->
                    if (!saved.isNullOrBlank()) LanguageManager.applyLanguage(saved)
                }
            }
        }
        setContent { BiteCalApp() } // ← 由這裡接手整個畫面導航 這樣一來，App 啟動（Splash 結束）會先進 landing，再由 NavHost 導到 signup/signin。
    }
}

