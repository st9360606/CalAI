package com.calai.bitecal

import androidx.activity.ComponentActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.calai.bitecal.i18n.LanguageManager
import com.calai.bitecal.i18n.LanguageStore
import com.calai.bitecal.i18n.ProvideComposeLocale
import com.calai.bitecal.ui.nav.BiteCalNavHost
import com.calai.bitecal.widget.BiteCalHomeWidgetUpdater
import com.calai.bitecal.widget.BiteCalWidgetNavigationRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

/**
 * 單一語言權威來源：
 * - 等 DataStore 第一次 emit 後再決定 initialTag（避免先用裝置語言造成啟動閃動）
 * - NavHost 不再做語言初始化，只處理導航與邏輯
 */
@Composable
fun BiteCalApp(
    hostActivity: ComponentActivity,
    widgetNavigationRequest: BiteCalWidgetNavigationRequest? = null
) {
    val context = LocalContext.current
    val store = remember(context) { LanguageStore(context) }

    // ✅ 關鍵：等待 DataStore 第一次有值再渲染，避免先用系統語言再跳成使用者語言
    val savedTagOrNull: String? by store.langFlow.collectAsState(initial = null)

    if (savedTagOrNull == null) {
        // 首次讀取期間顯示極簡白底（也可替換成你的開場畫面）
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = Color.Black)
        }
        return
    }

    // 一旦有值：DataStore 優先；再用 AppCompatDelegate；最後用裝置語言
    val initialTag = LanguageManager.normalizeTag(when {
        !savedTagOrNull.isNullOrBlank() -> savedTagOrNull!!
        AppCompatDelegate.getApplicationLocales().toLanguageTags().isNotBlank() ->
            AppCompatDelegate.getApplicationLocales().toLanguageTags()
        else -> Locale.getDefault().toLanguageTag()
    })

    var composeLocale by remember(initialTag) { mutableStateOf(initialTag) }

    // 語系變更時持久化（不阻塞 UI），並同步刷新桌面小工具文字。
    LaunchedEffect(composeLocale) {
        if (composeLocale.isNotBlank()) {
            store.save(composeLocale)
            withContext(Dispatchers.IO) {
                BiteCalHomeWidgetUpdater.updateAll(context.applicationContext)
            }
        }
    }

    // 只透過 ProvideComposeLocale 提供語系；不覆寫 LocalContext
    ProvideComposeLocale(composeLocale) {
        BiteCalNavHost(
            hostActivity = hostActivity,
            widgetNavigationRequest = widgetNavigationRequest,
            onSetLocale = { tag -> composeLocale = tag } // 切語言只改這個 state
        )
    }
}
