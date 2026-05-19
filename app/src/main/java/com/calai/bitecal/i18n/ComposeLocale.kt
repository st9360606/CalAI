package com.calai.bitecal.i18n

import android.content.Context
import android.content.res.Configuration
import android.os.LocaleList
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import java.util.Locale

/** 控制器：在 Compose 裡可讀取目前語系、也可切換語系 */
class LocaleController internal constructor(
    private val state: MutableState<String>
) {
    val tag: String get() = state.value
    fun set(newTag: String) {
        if (newTag.isNotBlank() && newTag != state.value) state.value = newTag
    }
}

val LocalLocaleController = staticCompositionLocalOf<LocaleController> {
    error("LocalLocaleController not provided")
}

/** 依 tag 產生 localized Context，覆寫到 LocalContext，讓 stringResource 立即換語系 */
@Composable
fun ProvideComposeLocale(
    tag: String,
    content: @Composable () -> Unit
) {
    val outer = LocalContext.current
    val baseConf = LocalConfiguration.current

    // 內部狀態：可由外部 tag 或畫面內 controller.set() 觸發
    val state = remember { mutableStateOf(tag.ifBlank { Locale.getDefault().toLanguageTag() }) }
    LaunchedEffect(tag) { if (tag.isNotBlank() && tag != state.value) state.value = tag }

    // 語系或組態變動時重建 localized context / configuration。
    // stringResource 會讀 LocalContext 的 Resources，部分 Composable 也會讀 LocalConfiguration；
    // 兩者必須同步提供，否則畫面可能重組了但文字仍停在舊語系。
    val normalizedTag = remember(state.value) {
        LanguageManager.normalizeTag(state.value)
    }
    val localizedConfiguration = remember(normalizedTag, baseConf) {
        Configuration(baseConf).apply {
            setLocales(LocaleList(Locale.forLanguageTag(normalizedTag)))
            setLayoutDirection(Locale.forLanguageTag(normalizedTag))
        }
    }
    val localized: Context = remember(outer, localizedConfiguration) {
        outer.createConfigurationContext(localizedConfiguration)
    }

    CompositionLocalProvider(
        LocalContext provides localized,
        LocalConfiguration provides localizedConfiguration,
        LocalLocaleController provides remember { LocaleController(state) }
    ) {
        content()
    }
}

/** 目前資源的語系鍵（需要時可用來做 key） */
@Composable
fun currentLocaleKey(): String = LocalLocaleController.current.tag
