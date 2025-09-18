// app/src/main/java/com/calai/app/i18n/LocaleUtils.kt
package com.calai.app.i18n

import android.util.Log
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import java.util.Locale

object LocaleUtils {

    /**
     * 將輸入的語言標籤正規化成 Android 資源最友善的形式：
     * - 中文：輸出 zh-TW / zh-CN / zh-HK / zh-MO
     * - 英文：en-US
     * - 其他語言：交給 Locale.forLanguageTag 再 toLanguageTag()
     */
    fun normalizeTag(raw: String?): String {
        val input = (raw ?: "").trim()
        if (input.isEmpty()) return "en-US"

        val lower = input.replace('_', '-').lowercase(Locale.ROOT)

        // 中文：以地區為主，對應 values-zh-rXX
        if (lower.startsWith("zh")) {
            return when {
                "-tw" in lower -> "zh-TW"
                "-hk" in lower -> "zh-HK"
                "-mo" in lower -> "zh-MO"
                "-cn" in lower || "-sg" in lower -> "zh-CN"
                else -> "zh-TW" // 只有 zh 時，依你產品面向台灣，預設 zh-TW
            }
        }

        // 英文慣例
        if (lower.startsWith("en")) return "en-US"

        // 其他語言交給 Locale 正規化
        val tag = Locale.forLanguageTag(input).toLanguageTag()
        return tag.ifBlank { "en-US" }
    }


    /** 套用 AppCompat 多語（會觸發重建）。 */
    fun setAppLocales(rawTag: String?) {
        val normalized = normalizeTag(rawTag)
        val locales = LocaleListCompat.forLanguageTags(normalized)
        Log.d("LocaleUtils", "setAppLocales raw=$rawTag -> normalized=$normalized ; locales=$locales")
        AppCompatDelegate.setApplicationLocales(locales)
    }

    /** 目前 AppCompat 生效的語言（除錯用）。 */
    fun currentAppLocales(): String {
        return AppCompatDelegate.getApplicationLocales().toLanguageTags()
    }
}
