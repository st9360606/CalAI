package com.calai.bitecal.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.os.Handler
import android.os.Looper

object BiteCalHomeWidgetUpdater {
    private const val UPDATE_DEBOUNCE_MS = 350L

    private val mainHandler = Handler(Looper.getMainLooper())
    private var pendingUpdateRunnable: Runnable? = null

    /**
     * Debounce widget updates triggered from Compose screens.
     *
     * Android launchers re-apply RemoteViews on every updateAppWidget() call.
     * If Home and Settings trigger updates back-to-back, bitmap rings can look like
     * they jump or flicker. Debouncing keeps the latest request and applies it once.
     */
    fun updateAll(context: Context) {
        val appContext = context.applicationContext

        pendingUpdateRunnable?.let { previous ->
            mainHandler.removeCallbacks(previous)
        }

        val nextRunnable = object : Runnable {
            override fun run() {
                if (pendingUpdateRunnable !== this) return
                pendingUpdateRunnable = null
                updateAllNow(appContext)
            }
        }

        pendingUpdateRunnable = nextRunnable
        mainHandler.postDelayed(nextRunnable, UPDATE_DEBOUNCE_MS)
    }

    fun updateAllNow(context: Context) {
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
