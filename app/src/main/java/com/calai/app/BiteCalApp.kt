package com.calai.app.ui

import androidx.activity.ComponentActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.calai.app.i18n.LanguageStore
import com.calai.app.i18n.ProvideComposeLocale
import com.calai.app.ui.nav.BiteCalNavHost
import java.util.Locale

@Composable
fun BiteCalApp(hostActivity: ComponentActivity) {
    val context = LocalContext.current
    val store = remember(context) { LanguageStore(context) }

    // 非阻塞取得預設語系
    val savedTag by store.langFlow.collectAsState(initial = "")
    val initialTag = remember(savedTag) {
        when {
            savedTag.isNotBlank() ->
                savedTag
            AppCompatDelegate.getApplicationLocales().toLanguageTags().isNotBlank() ->
                AppCompatDelegate.getApplicationLocales().toLanguageTags()
            else ->
                Locale.getDefault().toLanguageTag()
        }
    }

    var composeLocale by remember(initialTag) { mutableStateOf(initialTag) }

    // 語系變更時持久化
    LaunchedEffect(composeLocale) {
        if (composeLocale.isNotBlank()) store.save(composeLocale)
    }

    // ✨ 關鍵：不要再覆寫 LocalContext！
    ProvideComposeLocale(composeLocale) {
        BiteCalNavHost(
            hostActivity = hostActivity,                 // 直接用參數傳下去
            onSetLocale = { tag -> composeLocale = tag } // 切語言只改這個 state
        )
    }
}
