package com.calai.bitecal.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.widget.RemoteViews
import com.calai.bitecal.R

class BiteCalCaloriesWidgetReceiver : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        appWidgetIds.forEach { appWidgetId ->
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    companion object {
        fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val appContext = context.applicationContext
            val localizedContext = BiteCalWidgetLocaleContext.resolve(appContext)
            val snapshot = BiteCalWidgetSnapshotStore.load(appContext)
            val views = RemoteViews(appContext.packageName, R.layout.widget_calories).apply {
                setTextViewText(R.id.widget_calories_value, snapshot.caloriesLeft.toString())
                setTextViewText(R.id.widget_calories_label, localizedContext.getString(R.string.widget_calories_left))
                setTextViewText(R.id.widget_calories_action_text, localizedContext.getString(R.string.widget_log_your_food))
                setImageViewBitmap(
                    R.id.widget_calories_ring,
                    BiteCalWidgetRingRenderer.render(
                        context = appContext,
                        progressPercent = snapshot.calorieProgress,
                        sizeDp = 106,
                        strokeDp = 7.5f
                    )
                )
                setOnClickPendingIntent(R.id.widget_calories_root, BiteCalWidgetPendingIntents.openHome(appContext))
                setOnClickPendingIntent(R.id.widget_calories_action, BiteCalWidgetPendingIntents.scanFood(appContext))
            }
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
