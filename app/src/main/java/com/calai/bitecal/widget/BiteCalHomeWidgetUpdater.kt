package com.calai.bitecal.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context

object BiteCalHomeWidgetUpdater {
    fun updateAll(context: Context) {
        val appContext = context.applicationContext
        val manager = AppWidgetManager.getInstance(appContext)

        manager.getAppWidgetIds(ComponentName(appContext, BiteCalCaloriesWidgetReceiver::class.java))
            .forEach { appWidgetId ->
                BiteCalCaloriesWidgetReceiver.updateAppWidget(appContext, manager, appWidgetId)
            }

        manager.getAppWidgetIds(ComponentName(appContext, BiteCalMacroActionsWidgetReceiver::class.java))
            .forEach { appWidgetId ->
                BiteCalMacroActionsWidgetReceiver.updateAppWidget(appContext, manager, appWidgetId)
            }
    }
}
