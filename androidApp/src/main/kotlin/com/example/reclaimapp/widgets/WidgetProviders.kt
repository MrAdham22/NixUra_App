package com.example.reclaimapp.widgets

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.os.Bundle

/** Shared behaviour of the three home-screen widgets. All rendering lives in [WidgetRenderer]. */
abstract class NixWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        WidgetUpdater.requestRefresh(context, appWidgetIds, goAsync())
    }

    /** Resize: pick the compact or wide layout again. */
    override fun onAppWidgetOptionsChanged(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int, newOptions: Bundle) {
        WidgetUpdater.requestRefresh(context, intArrayOf(appWidgetId), goAsync())
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        WidgetPrefs.remove(context, appWidgetIds)
        WidgetUpdater.requestRefresh(context, null, goAsync()) // re-arms or cancels the alarm
    }

    override fun onDisabled(context: Context) {
        WidgetUpdater.requestRefresh(context, null, goAsync())
    }

    override fun onRestored(context: Context, oldWidgetIds: IntArray, newWidgetIds: IntArray) {
        WidgetPrefs.remap(context, oldWidgetIds, newWidgetIds)
    }
}

/** Quit counter: time since the last slip of one bad habit. */
class QuitCounterWidgetProvider : NixWidgetProvider()

/** Good habit quick log: today's count with "+" and "-" buttons. */
class GoodHabitWidgetProvider : NixWidgetProvider()

/** Events: nearest upcoming event (default) or one chosen event. */
class EventWidgetProvider : NixWidgetProvider()
