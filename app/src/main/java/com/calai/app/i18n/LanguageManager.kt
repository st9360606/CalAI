package com.calai.app.i18n

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

object LanguageManager {
    /** 例如 tag="zh-TW"、"en"、"zh-CN" */
    fun applyLanguage(tag: String) {
        val normalized = tag.trim().ifEmpty { "en" }
        AppCompatDelegate.setApplicationLocales(
            LocaleListCompat.forLanguageTags(normalized)
        )
    }
}
