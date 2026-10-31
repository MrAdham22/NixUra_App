package com.example.reclaimapp.widgets

import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.reclaimapp.domain.widget.WidgetKind

/**
 * Called by the system once the launcher has placed a pinned widget. If the config screen did not already bind the
 * widget (some launchers skip it), bind it to the habit/event the user pressed "Add to Home screen" on.
 */
class WidgetPinReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != WidgetIntents.ACTION_PINNED) return
        val widgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) return
        val kind = runCatching { WidgetKind.valueOf(intent.getStringExtra(WidgetIntents.EXTRA_KIND).orEmpty()) }.getOrNull() ?: return
        val id = intent.getLongExtra(WidgetIntents.EXTRA_ID, -1L).takeIf { it > 0 }
        if (WidgetPrefs.targetOf(context, widgetId) == null && (kind != WidgetKind.EVENT || id != null)) {
            WidgetPrefs.setTarget(context, widgetId, id)
        }
        WidgetPrefs.clearPendingPin(context, kind)
        WidgetUpdater.requestRefresh(context, intArrayOf(widgetId), goAsync())
    }
}
