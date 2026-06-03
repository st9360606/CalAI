package com.calai.bitecal.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.graphics.Color
import android.widget.RemoteViews
import com.calai.bitecal.R

class BiteCalMacroActionsWidgetReceiver : AppWidgetProvider() {
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
            val views = RemoteViews(appContext.packageName, R.layout.widget_macro_actions).apply {
                setTextViewText(R.id.widget_macro_calories_value, snapshot.caloriesLeft.toString())
                setTextViewText(R.id.widget_macro_calories_label, localizedContext.getString(R.string.widget_calories_left))
                setImageViewBitmap(
                    R.id.widget_macro_calories_ring,
                    BiteCalWidgetRingRenderer.render(
                        context = appContext,
                        progressPercent = snapshot.calorieProgress,
                        sizeDp = 114,
                        strokeDp = 7.5f
                    )
                )

                setTextViewText(R.id.widget_protein_value, localizedContext.getString(R.string.widget_grams_format, snapshot.proteinLeftG))
                setTextViewText(R.id.widget_protein_label, localizedContext.getString(R.string.widget_protein_left))
                setImageViewBitmap(
                    R.id.widget_protein_ring,
                    BiteCalWidgetRingRenderer.render(
                        context = appContext,
                        progressPercent = snapshot.proteinProgress,
                        sizeDp = 34,
                        strokeDp = 2.7f,
                        progressColor = Color.rgb(229, 108, 108),
                        tickRadiusScale = 0.55f
                    )
                )

                setTextViewText(R.id.widget_carbs_value, localizedContext.getString(R.string.widget_grams_format, snapshot.carbsLeftG))
                setTextViewText(R.id.widget_carbs_label, localizedContext.getString(R.string.widget_carbs_left))
                setImageViewBitmap(
                    R.id.widget_carbs_ring,
                    BiteCalWidgetRingRenderer.render(
                        context = appContext,
                        progressPercent = snapshot.carbsProgress,
                        sizeDp = 34,
                        strokeDp = 2.7f,
                        progressColor = Color.rgb(216, 154, 98),
                        tickRadiusScale = 0.55f
                    )
                )

                setTextViewText(R.id.widget_fats_value, localizedContext.getString(R.string.widget_grams_format, snapshot.fatsLeftG))
                setTextViewText(R.id.widget_fats_label, localizedContext.getString(R.string.widget_fats_left))
                setImageViewBitmap(
                    R.id.widget_fats_ring,
                    BiteCalWidgetRingRenderer.render(
                        context = appContext,
                        progressPercent = snapshot.fatsProgress,
                        sizeDp = 34,
                        strokeDp = 2.7f,
                        progressColor = Color.rgb(108, 147, 216),
                        tickRadiusScale = 0.55f
                    )
                )

                setTextViewText(R.id.widget_scan_food_text, localizedContext.getString(R.string.widget_scan_food))
                setTextViewText(R.id.widget_barcode_text, localizedContext.getString(R.string.widget_barcode))

                setOnClickPendingIntent(R.id.widget_macro_root, BiteCalWidgetPendingIntents.openHome(appContext))
                setOnClickPendingIntent(R.id.widget_scan_food_tile, BiteCalWidgetPendingIntents.scanFood(appContext))
                setOnClickPendingIntent(R.id.widget_barcode_tile, BiteCalWidgetPendingIntents.scanBarcode(appContext))
            }
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
