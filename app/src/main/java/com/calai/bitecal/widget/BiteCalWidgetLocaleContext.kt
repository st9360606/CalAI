package com.calai.bitecal.widget

import android.content.Context
import android.content.res.Configuration
import android.os.LocaleList
import java.util.Locale

/**
 * BiteCal home screen widgets intentionally use English copy for every user,
 * independent of the in-app language and the device system locale.
 */
object BiteCalWidgetLocaleContext {
    private val widgetLocale: Locale = Locale.ENGLISH

    fun resolve(context: Context): Context {
        val appContext = context.applicationContext
        val configuration = Configuration(appContext.resources.configuration).apply {
            setLocales(LocaleList(widgetLocale))
            setLayoutDirection(widgetLocale)
        }
        return appContext.createConfigurationContext(configuration)
    }
}
