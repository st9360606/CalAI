package com.calai.bitecal.i18n

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import java.util.Locale

object LanguageManager {
    const val DEFAULT_LANGUAGE_TAG = "en-US"

    private val canonicalTags = mapOf(
        "en" to "en-US",
        "en-us" to "en-US",

        "zh-cn" to "zh-CN",
        "zh-hans" to "zh-CN",
        "zh-hans-cn" to "zh-CN",
        "zh-sg" to "zh-CN",

        "zh-hk" to "zh-HK",
        "zh-tw" to "zh-HK",
        "zh-mo" to "zh-HK",
        "zh-hant" to "zh-HK",
        "zh-hant-hk" to "zh-HK",
        "zh-hant-tw" to "zh-HK",

        "ja" to "ja",
        "ko" to "ko",
        "de" to "de",
        "fr" to "fr",
        "nl" to "nl",
        "he" to "he",
        "sv" to "sv",
        "nb" to "nb",
        "da" to "da",
        "fi" to "fi",
        "it" to "it",
        "es" to "es",
        "ar" to "ar",
        "pt-br" to "pt-BR",
        "tr" to "tr",
        "pl" to "pl",
        "cs" to "cs",
        "ro" to "ro",
        "pt-pt" to "pt-PT",
        "ru" to "ru",
        "th" to "th",
        "ms" to "ms",
        "vi" to "vi",
        "fil" to "fil",
        "hi" to "hi",
        "jv" to "jv"
    )

    fun normalizeTag(raw: String?): String {
        val input = raw
            ?.trim()
            ?.replace('_', '-')
            .orEmpty()

        if (input.isBlank()) return DEFAULT_LANGUAGE_TAG

        val lower = input.lowercase(Locale.ROOT)
        canonicalTags[lower]?.let { return it }

        return runCatching {
            Locale.forLanguageTag(input)
                .toLanguageTag()
                .takeIf { it.isNotBlank() && it != "und" }
        }.getOrNull() ?: DEFAULT_LANGUAGE_TAG
    }

    /**
     * 這裡的 supported 指的是「語言列表允許使用者選取」，
     * 不是代表該語言已經有完整 strings.xml 翻譯。
     */
    fun isSupported(raw: String?): Boolean {
        val input = raw
            ?.trim()
            ?.replace('_', '-')
            .orEmpty()

        if (input.isBlank()) return false

        val lower = input.lowercase(Locale.ROOT)
        return canonicalTags.containsKey(lower)
    }

    /**
     * 只用 normalized tag 精準比對。
     *
     * 不要把 ja / ko / de / fr fallback 成 en-US 後再比對，
     * 否則會造成多個語言同時反黑。
     */
    fun isSelectedOption(optionTag: String?, currentTag: String?): Boolean {
        val normalizedOption = normalizeTag(optionTag)
        val normalizedCurrent = normalizeTag(currentTag)

        return normalizedOption.equals(
            normalizedCurrent,
            ignoreCase = true
        )
    }

    fun applyLanguage(tag: String) {
        val normalized = normalizeTag(tag)
        AppCompatDelegate.setApplicationLocales(
            LocaleListCompat.forLanguageTags(normalized)
        )
    }
}
