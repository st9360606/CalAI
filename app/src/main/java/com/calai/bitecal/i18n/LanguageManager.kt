package com.calai.bitecal.i18n

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import java.util.Locale

object LanguageManager {
    fun normalizeTag(raw: String?): String {
        val input = (raw ?: "").trim()
        if (input.isEmpty()) return "en-US"

        val lower = input.replace('_', '-').lowercase(Locale.ROOT)

        if (lower.startsWith("zh")) {
            return when {
                "-cn" in lower || "-sg" in lower || "-hans" in lower -> "zh-CN"
                else -> "zh-HK"
            }
        }

        if (lower.startsWith("en")) return "en-US"

        val tag = Locale.forLanguageTag(input).toLanguageTag()
        return if (tag.isBlank()) "en-US" else tag
    }

    fun applyLanguage(tag: String) {
        val normalized = normalizeTag(tag)
        AppCompatDelegate.setApplicationLocales(
            LocaleListCompat.forLanguageTags(normalized)
        )
    }
}
