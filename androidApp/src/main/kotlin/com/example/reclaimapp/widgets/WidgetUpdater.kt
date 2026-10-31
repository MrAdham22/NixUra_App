package com.example.reclaimapp.widgets

import android.app.AlarmManager
import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.reclaimapp.domain.clock.Clock
import com.example.reclaimapp.domain.time.DayCalculator
import com.example.reclaimapp.domain.usecase.ObserveWidgetDataChangesUseCase
import com.example.reclaimapp.domain.widget.WidgetKind
import com.example.reclaimapp.domain.widget.WidgetRefreshPlanner
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.koin.mp.KoinPlatform
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Renders all widgets and keeps them fresh.
 *
 * Refresh strategy (battery-friendly, no polling):
 *  - the running clock is a system Chronometer, so it ticks with zero wake-ups;
 *  - ONE inexact alarm is armed for the earliest moment something a Chronometer cannot show changes
 *    (whole-day rollover of a counter, an event passing, or local midnight for the good-habit count);
 *  - data changes while the process is alive re-render after a 500 ms debounce;
 *  - boot, app update, time / date / time-zone changes re-render and re-arm the alarm.
 */
internal object WidgetUpdater {
    private const val TAG = "NixUraWidgets"
    private const val DEBOUNCE_MS = 500L

    private val handler = CoroutineExceptionHandler { _, e -> Log.w(TAG, "widget coroutine failed", e) }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default + handler)
    private val lock = Mutex()
    private val observing = AtomicBoolean(false)
    private val receiverRegistered = AtomicBoolean(false)

    private val providers: List<Pair<Class<*>, WidgetKind>> = listOf(
        QuitCounterWidgetProvider::class.java to WidgetKind.QUIT_COUNTER,
        GoodHabitWidgetProvider::class.java to WidgetKind.GOOD_QUICK_LOG,
        EventWidgetProvider::class.java to WidgetKind.EVENT,
    )

    /** Runs [block] on the widget scope (uncaught errors are logged, never crash the app). */
    fun launch(block: suspend CoroutineScope.() -> Unit) = scope.launch(block = block)

    /** Fire-and-forget refresh. [firstIds] are rendered first (the widget the user just touched). */
    fun requestRefresh(context: Context, firstIds: IntArray? = null, pending: BroadcastReceiver.PendingResult? = null) {
        val app = context.applicationContext
        scope.launch {
            try {
                refresh(app, firstIds)
            } finally {
                pending?.finish()
            }
        }
    }

    suspend fun refresh(context: Context, firstIds: IntArray? = null) = lock.withLock {
        val manager = AppWidgetManager.getInstance(context)
        val all = providers.flatMap { (cls, kind) ->
            manager.getAppWidgetIds(ComponentName(context, cls)).map { it to kind }
        }
        if (all.isEmpty()) {
            cancelAlarm(context)
            return@withLock
        }
        val priority = firstIds?.toSet().orEmpty()
        val ordered = all.sortedBy { if (it.first in priority) 0 else 1 }
        val changes = mutableListOf<Long?>()
        for ((id, kind) in ordered) {
            try {
                val rendered = WidgetRenderer.render(context, id, kind)
                manager.updateAppWidget(id, rendered.views)
                changes += rendered.nextChangeAtEpochMillis
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "render failed for widget $id", e)
            }
        }
        scheduleNext(context, changes)
    }

    private fun scheduleNext(context: Context, changes: List<Long?>) {
        try {
            val koin = KoinPlatform.getKoin()
            val now = koin.get<Clock>().nowEpochMillis()
            val at = WidgetRefreshPlanner.nextRefreshAt(now, koin.get<DayCalculator>(), changes)
            val alarms = context.getSystemService(AlarmManager::class.java) ?: return
            // Inexact on purpose: no SCHEDULE_EXACT_ALARM permission, and a few minutes of delay is harmless
            // (non-wakeup RTC: if the screen is off nobody is looking at the widget anyway).
            alarms.setAndAllowWhileIdle(AlarmManager.RTC, at, WidgetIntents.tick(context))
        } catch (e: Exception) {
            Log.w(TAG, "could not schedule widget refresh", e)
        }
    }

    private fun cancelAlarm(context: Context) {
        try {
            context.getSystemService(AlarmManager::class.java)?.cancel(WidgetIntents.tick(context))
        } catch (e: Exception) {
            Log.w(TAG, "could not cancel widget refresh", e)
        }
    }

    /** While the app process is alive, re-render whenever habits, logs, events or theme change. */
    fun startObserving(context: Context) {
        if (!observing.compareAndSet(false, true)) return
        val app = context.applicationContext
        scope.launch {
            val changes = KoinPlatform.getKoin().get<ObserveWidgetDataChangesUseCase>()
            // drop(1): the first emission is just the current state, widgets are already drawn for it.
            changes().drop(1).collectLatest {
                delay(DEBOUNCE_MS)
                refresh(app)
            }
        }
    }

    /**
     * Time / date / time-zone changes are also declared in the manifest; this registration covers launchers and
     * OS versions that do not deliver them to manifest receivers while the app process is running.
     */
    fun registerSystemReceiver(context: Context) {
        if (!receiverRegistered.compareAndSet(false, true)) return
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_TIMEZONE_CHANGED)
            addAction(Intent.ACTION_DATE_CHANGED)
            addAction(Intent.ACTION_TIME_CHANGED)
        }
        ContextCompat.registerReceiver(context.applicationContext, WidgetSystemReceiver(), filter, ContextCompat.RECEIVER_NOT_EXPORTED)
    }
}
