package com.example.reclaimapp.widgets

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import com.example.reclaimapp.domain.widget.WidgetKind

/** Maps between [WidgetKind] and the provider classes registered in the manifest. */
internal object WidgetKinds {
    fun providerOf(context: Context, kind: WidgetKind): ComponentName = ComponentName(
        context,
        when (kind) {
            WidgetKind.QUIT_COUNTER -> QuitCounterWidgetProvider::class.java
            WidgetKind.GOOD_QUICK_LOG -> GoodHabitWidgetProvider::class.java
            WidgetKind.EVENT -> EventWidgetProvider::class.java
        },
    )

    fun of(context: Context, appWidgetId: Int): WidgetKind? {
        val provider = AppWidgetManager.getInstance(context).getAppWidgetInfo(appWidgetId)?.provider?.className ?: return null
        return when (provider) {
            QuitCounterWidgetProvider::class.java.name -> WidgetKind.QUIT_COUNTER
            GoodHabitWidgetProvider::class.java.name -> WidgetKind.GOOD_QUICK_LOG
            EventWidgetProvider::class.java.name -> WidgetKind.EVENT
            else -> null
        }
    }
}
