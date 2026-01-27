package com.calai.bitecal.data.fasting.notifications

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

/**
 * ✅ 注意：Receiver 不能用 suspend I/O，所以用 SharedPreferences（同步）最穩。
 * ✅ 多語系：key 加上 localeTag 後綴（例如 zh-TW / en-US）。
 */
object FastingNotificationTemplates {

    private const val SP = "fasting_notif_templates"

    private const val K_START_TITLE = "start_title"
    private const val K_START_BODY = "start_body"
    private const val K_ENDSOON_TITLE = "endsoon_title"
    private const val K_ENDSOON_BODY = "endsoon_body"

    private fun sp(ctx: Context): SharedPreferences =
        ctx.getSharedPreferences(SP, Context.MODE_PRIVATE)

    private fun localeTag(ctx: Context): String {
        val loc = ctx.resources.configuration.locales[0]
        return loc.toLanguageTag() // e.g. "zh-TW"
    }

    private fun k(base: String, tag: String) = "${base}_${tag}"

    fun getStartTitleOverride(ctx: Context): String? =
        sp(ctx).getString(k(K_START_TITLE, localeTag(ctx)), null)

    fun getStartBodyOverride(ctx: Context): String? =
        sp(ctx).getString(k(K_START_BODY, localeTag(ctx)), null)

    fun getEndSoonTitleOverride(ctx: Context): String? =
        sp(ctx).getString(k(K_ENDSOON_TITLE, localeTag(ctx)), null)

    fun getEndSoonBodyOverride(ctx: Context): String? =
        sp(ctx).getString(k(K_ENDSOON_BODY, localeTag(ctx)), null)

    fun setStartTemplates(ctx: Context, title: String?, body: String?) {
        val tag = localeTag(ctx)
        sp(ctx).edit {
            putString(k(K_START_TITLE, tag), title?.takeIf { it.isNotBlank() })
            putString(k(K_START_BODY, tag), body?.takeIf { it.isNotBlank() })
        }
    }

    fun setEndSoonTemplates(ctx: Context, title: String?, body: String?) {
        val tag = localeTag(ctx)
        sp(ctx).edit {
            putString(k(K_ENDSOON_TITLE, tag), title?.takeIf { it.isNotBlank() })
            putString(k(K_ENDSOON_BODY, tag), body?.takeIf { it.isNotBlank() })
        }
    }

    fun clearAllForCurrentLocale(ctx: Context) {
        val tag = localeTag(ctx)
        sp(ctx).edit {
            remove(k(K_START_TITLE, tag))
            remove(k(K_START_BODY, tag))
            remove(k(K_ENDSOON_TITLE, tag))
            remove(k(K_ENDSOON_BODY, tag))
        }
    }

    /**
     * 簡單模板：支援 {planCode} {startTime} {endTime}
     */
    fun render(tpl: String, planCode: String, startTime: String, endTime: String): String {
        return tpl
            .replace("{planCode}", planCode)
            .replace("{startTime}", startTime)
            .replace("{endTime}", endTime)
    }
}
