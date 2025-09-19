// app/src/main/java/com/calai/app/i18n/ComposeLocale.kt
package com.calai.app.i18n

import android.content.Context
import android.content.res.Configuration
import android.os.LocaleList
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import java.util.Locale

// 對外暴露的控制器（任何 Composable 都能 set）
class LocaleController internal constructor(initial: String) {
    var tag by mutableStateOf(initial)
        private set
    fun set(tag: String) { this.tag = tag }
}

val LocalLocaleController = staticCompositionLocalOf<LocaleController> {
    error("LocalLocaleController not provided")
}

/** 以 tag 建一個帶新語系的 Context，再覆蓋給整個 Compose 樹 */
@Composable
fun ProvideComposeLocale(
    tag: String,
    content: @Composable () -> Unit
) {
    val base = LocalContext.current
    val localized: Context = remember(tag, base) {
        val cfg = Configuration(base.resources.configuration).apply {
            setLocales(LocaleList(Locale.forLanguageTag(tag)))
        }
        base.createConfigurationContext(cfg)
    }
    CompositionLocalProvider(
        LocalContext provides localized,
        LocalLocaleController provides remember { LocaleController(tag) }.also { it.set(tag) }
    ) {
        content()
    }
}
// 取得「實際資源」目前的語言 key（用來當 key 觸發重建最準）
@Composable
fun currentLocaleKey(): String =
    LocalContext.current.resources.configuration.locales.toLanguageTags()
