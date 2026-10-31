package com.example.reclaimapp.widgets

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.example.reclaimapp.MainActivity

/** Every PendingIntent a widget uses. All are explicit and immutable (no exported surface, no mutable intents). */
internal object WidgetIntents {
    const val ACTION_OPEN = "com.example.reclaimapp.widget.OPEN"
    const val ACTION_INCREMENT = "com.example.reclaimapp.widget.INCREMENT"
    const val ACTION_DECREMENT = "com.example.reclaimapp.widget.DECREMENT"
    const val ACTION_TICK = "com.example.reclaimapp.widget.TICK"
    const val ACTION_PINNED = "com.example.reclaimapp.widget.PINNED"
    const val EXTRA_KIND = "nixura.widget.kind"

    const val EXTRA_TYPE = "nixura.widget.type"
    const val EXTRA_ID = "nixura.widget.id"
    const val EXTRA_WIDGET_ID = "nixura.widget.widgetId"

    const val TYPE_HOME = "home"
    const val TYPE_QUIT = "quit"
    const val TYPE_GOOD = "good"
    const val TYPE_EVENTS = "events"
    const val TYPE_ADD_QUIT = "add_quit"
    const val TYPE_ADD_GOOD = "add_good"
    const val TYPE_ADD_EVENT = "add_event"

    private const val FLAGS = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    private const val TICK_REQUEST_CODE = 7001

    /** Opens the app and asks it to navigate (see [WidgetDeepLink]). */
    fun open(context: Context, widgetId: Int, type: String, id: Long = -1L): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            action = ACTION_OPEN
            data = Uri.parse("nixura://widget/$widgetId/$type")
            putExtra(EXTRA_TYPE, type)
            putExtra(EXTRA_ID, id)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        return PendingIntent.getActivity(context, widgetId, intent, FLAGS)
    }

    /** Opens the chooser for this widget again ("tap to choose another"). */
    fun configure(context: Context, widgetId: Int): PendingIntent {
        val intent = Intent(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE).apply {
            setClass(context, WidgetConfigActivity::class.java)
            data = Uri.parse("nixura://widget/$widgetId/configure")
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return PendingIntent.getActivity(context, widgetId, intent, FLAGS)
    }

    /** The "+" / "-" buttons: a broadcast to [WidgetActionReceiver], so the app UI never opens. */
    fun adjust(context: Context, widgetId: Int, habitId: Long, increment: Boolean): PendingIntent {
        val name = if (increment) "inc" else "dec"
        val intent = Intent(context, WidgetActionReceiver::class.java).apply {
            action = if (increment) ACTION_INCREMENT else ACTION_DECREMENT
            data = Uri.parse("nixura://widget/$widgetId/$name")
            putExtra(EXTRA_WIDGET_ID, widgetId)
            putExtra(EXTRA_ID, habitId)
        }
        return PendingIntent.getBroadcast(context, widgetId * 2 + if (increment) 1 else 0, intent, FLAGS)
    }

    fun tick(context: Context): PendingIntent {
        val intent = Intent(context, WidgetSystemReceiver::class.java).setAction(ACTION_TICK)
        return PendingIntent.getBroadcast(context, TICK_REQUEST_CODE, intent, FLAGS)
    }
}
