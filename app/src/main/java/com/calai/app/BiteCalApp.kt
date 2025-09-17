// app/src/main/java/com/calai/app/ui/BiteCal.kt  (你的 BiteCalApp 定義處)
package com.calai.app.ui

import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.calai.app.i18n.ProvideComposeLocale
import com.calai.app.i18n.LanguageStore
import com.calai.app.ui.nav.BiteCalNavHost
import kotlinx.coroutines.flow.firstOrNull
import java.util.Locale
import kotlinx.coroutines.runBlocking

@Composable
fun BiteCalApp() {
    // 啟動時決定 Compose 的初始語系：已保存 > AppCompat > 系統
    val context = LocalContext.current
    val store = remember(context) { LanguageStore(context) }

    val initialTag = remember {
        // 讀 DataStore（同步一次即可；也可改用 collectAsState）
        val saved = runBlocking { store.langFlow.firstOrNull() }.orEmpty()
        when {
            saved.isNotBlank() -> saved
            AppCompatDelegate.getApplicationLocales().toLanguageTags().isNotBlank() ->
                AppCompatDelegate.getApplicationLocales().toLanguageTags()
            else -> Locale.getDefault().toLanguageTag()
        }
    }

    var composeLocale by remember { mutableStateOf(initialTag) }

    ProvideComposeLocale(composeLocale) {
        // 你的 NavHost / 整個 UI
        BiteCalNavHost(
            // 把 setter 暴露出去，好讓 Landing 直接切 Compose 語系
            onSetLocale = { newTag -> composeLocale = newTag }
        )
    }
}
