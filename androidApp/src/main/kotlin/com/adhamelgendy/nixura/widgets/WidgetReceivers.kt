package com.adhamelgendy.nixura.widgets

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.adhamelgendy.nixura.domain.usecase.DecrementGoodHabitTodayUseCase
import com.adhamelgendy.nixura.domain.usecase.IncrementGoodHabitTodayUseCase
import kotlinx.coroutines.CancellationException
import org.koin.mp.KoinPlatform

/**
 * Handles the "+" / "-" buttons. Works with the app closed: Android starts the process, `NixUraApplication.onCreate`
 * starts Koin, then this runs the write and redraws the widgets that show this habit right away (the touched one
 * first; no waiting for the debounce). Every tap is its own write: taps are serialized by the use case's lock,
 * never dropped or merged; only the redraws of taps that arrive while one is drawing are merged.
 */
class WidgetActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val increment = when (intent.action) {
            WidgetIntents.ACTION_INCREMENT -> true
            WidgetIntents.ACTION_DECREMENT -> false
            else -> return
        }
        val habitId = intent.getLongExtra(WidgetIntents.EXTRA_ID, -1L)
        val widgetId = intent.getIntExtra(WidgetIntents.EXTRA_WIDGET_ID, -1)
        val app = context.applicationContext
        val pending = goAsync()
        WidgetUpdater.launch {
            try {
                val koin = KoinPlatform.getKoin()
                val result = if (increment) koin.get<IncrementGoodHabitTodayUseCase>()(habitId)
                else koin.get<DecrementGoodHabitTodayUseCase>()(habitId)
                result.exceptionOrNull()?.let { Log.w("NixUraWidgets", "quick log failed", it) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w("NixUraWidgets", "quick log failed", e)
            } finally {
                // Always re-render: on success to show the new count, on failure (e.g. habit deleted) to show the safe state.
                try {
                    WidgetUpdater.redrawAfterTap(app, widgetId, habitId)
                } finally {
                    pending.finish()
                }
            }
        }
    }
}

/** Boot, app update, clock/time-zone changes and the refresh alarm: re-render everything and re-arm the alarm. */
class WidgetSystemReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_DATE_CHANGED,
            Intent.ACTION_TIME_CHANGED,
            WidgetIntents.ACTION_TICK,
            -> WidgetUpdater.requestRefresh(context, null, goAsync())
        }
    }
}
