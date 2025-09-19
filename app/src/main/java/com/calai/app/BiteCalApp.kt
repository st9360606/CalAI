package com.calai.app.ui

import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import com.calai.app.i18n.LanguageStore
import com.calai.app.i18n.ProvideComposeLocale
import com.calai.app.ui.nav.BiteCalNavHost
import java.util.Locale
import kotlinx.coroutines.flow.map

// 以前（地雷）：runBlocking { store.langFlow.first() }  <-- 會卡主執行緒
// 改成：非阻塞、先用系統語言，資料來了再切
@Composable
fun BiteCalApp() {
    val context = LocalContext.current
    val store = remember(context) { LanguageStore(context) }

    // 非阻塞讀取預設語言
    val savedTag by store.langFlow.collectAsState(initial = "")
    val initialTag = remember(savedTag) {
        when {
            savedTag.isNotBlank() -> savedTag
            AppCompatDelegate.getApplicationLocales().toLanguageTags().isNotBlank() ->
                AppCompatDelegate.getApplicationLocales().toLanguageTags()
            else -> Locale.getDefault().toLanguageTag()
        }
    }

    var composeLocale by remember(initialTag) { mutableStateOf(initialTag) }

    // ✅ Side-effect 放這裡：每次語言狀態變更就儲存（不阻塞 UI）
    LaunchedEffect(composeLocale) {
        if (composeLocale.isNotBlank()) store.save(composeLocale)
    }

    ProvideComposeLocale(composeLocale) {
        BiteCalNavHost(
            onSetLocale = { tag -> composeLocale = tag } // 只改狀態
        )
    }
}


