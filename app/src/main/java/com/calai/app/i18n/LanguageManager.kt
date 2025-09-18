// app/src/main/java/com/calai/app/i18n/LanguageManager.kt
package com.calai.app.i18n

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import java.util.Locale

object LanguageManager {

    /** 將輸入標籤正規化為 Android 資源友善形式（zh-TW/zh-CN/en-US…） */
    private fun normalizeTag(raw: String?): String {
        val input = (raw ?: "").trim()
        if (input.isEmpty()) return "en-US"

        val lower = input.replace('_', '-').lowercase(Locale.ROOT)

        // 中文：以「地區」對應 values-zh-rXX
        if (lower.startsWith("zh")) {
            return when {
                "-tw" in lower -> "zh-TW"
                "-hk" in lower -> "zh-HK"
                "-mo" in lower -> "zh-MO"
                "-cn" in lower || "-sg" in lower -> "zh-CN"
                else -> "zh-TW" // 只有 zh → 依你的市場預設台灣
            }
        }

        // 英文慣例
        if (lower.startsWith("en")) return "en-US"

        // 其他語言交給 Locale 正規化
        val tag = Locale.forLanguageTag(input).toLanguageTag()
        return if (tag.isBlank()) "en-US" else tag
    }

    /** 套用全域語言（會觸發 Activity 重建） */
    fun applyLanguage(tag: String) {
        val normalized = normalizeTag(tag)
        AppCompatDelegate.setApplicationLocales(
            LocaleListCompat.forLanguageTags(normalized)
        )
    }
}
