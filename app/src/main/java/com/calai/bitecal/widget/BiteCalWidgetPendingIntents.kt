package com.calai.bitecal.widget

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.calai.bitecal.MainActivity

object BiteCalWidgetPendingIntents {
    private const val ACTION_OPEN_HOME = "com.calai.bitecal.widget.OPEN_HOME"
    private const val ACTION_SCAN_FOOD = "com.calai.bitecal.widget.SCAN_FOOD"
    private const val ACTION_SCAN_BARCODE = "com.calai.bitecal.widget.SCAN_BARCODE"

    fun openHome(context: Context): PendingIntent {
        return activityPendingIntent(
            context = context,
            requestCode = 10,
            action = ACTION_OPEN_HOME,
            destination = "home"
        )
    }

    fun scanFood(context: Context): PendingIntent {
        return activityPendingIntent(
            context = context,
            requestCode = 11,
            action = ACTION_SCAN_FOOD,
            destination = "scan_food"
        )
    }

    fun scanBarcode(context: Context): PendingIntent {
        return activityPendingIntent(
            context = context,
            requestCode = 12,
            action = ACTION_SCAN_BARCODE,
            destination = "scan_barcode"
        )
    }

    private fun activityPendingIntent(
        context: Context,
        requestCode: Int,
        action: String,
        destination: String
    ): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            this.action = action
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("bitecal_widget_destination", destination)
        }
        return PendingIntent.getActivity(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
