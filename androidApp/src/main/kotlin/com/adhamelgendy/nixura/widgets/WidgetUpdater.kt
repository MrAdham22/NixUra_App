package com.adhamelgendy.nixura.widgets

import android.app.AlarmManager
import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import androidx.core.content.ContextCompat
import com.adhamelgendy.nixura.domain.clock.Clock
import com.adhamelgendy.nixura.domain.time.DayCalculator
import com.adhamelgendy.nixura.domain.usecase.ObserveWidgetDataChangesUseCase
import com.adhamelgendy.nixura.domain.widget.RefreshCoalescer
import com.adhamelgendy.nixura.domain.widget.WidgetKind
import com.adhamelgendy.nixura.domain.widget.WidgetRefreshPlanner
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.koin.mp.KoinPlatform
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Renders the widgets and keeps them fresh.
 *
 * Refresh strategy (battery-friendly, no polling):
 *  - the running clock is a system Chronometer, so it ticks with zero wake-ups;
 *  - ONE inexact alarm is armed for the earliest moment something a Chronometer cannot show changes
 *    (whole-day rollover of a counter, an event passing, or local midnight for the good-habit count);
 *  - a "+" / "-" tap redraws only the widgets that show that habit, right after its write ([redrawAfterTap]);
 *    redraws asked while one is running are merged into one (writes are never merged: every tap counts);
 *  - data changes while the process is alive redraw, after a 500 ms debounce (quit counters at once: their name may
 *    just have been hidden), only the kinds whose data changed,
 *    and skip a quick-log widget whose content is the same as already drawn (so a tap is not drawn twice);
 *  - boot, app update, time / date / time-zone changes and the launcher's own update requests redraw everything.
 */
internal object WidgetUpdater {
    private const val TAG = "NixUraWidgets"
    private const val DEBOUNCE_MS = 500L

    private val handler = CoroutineExceptionHandler { _, e -> Log.w(TAG, "widget coroutine failed", e) }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default + handler)

    /** One drawing pass at a time (they share the alarm and the [nextChanges] map). */
    private val lock = Mutex()
    private val observing = AtomicBoolean(false)
    private val receiverRegistered = AtomicBoolean(false)

    /** Next re-render instant of each drawn widget, so a partial redraw can still arm the alarm for all of them. */
    private val nextChanges = ConcurrentHashMap<Int, Long>()

    /** Widgets drawn by this process: until all are known, a partial redraw leaves the armed alarm alone. */
    private val drawn: MutableSet<Int> = ConcurrentHashMap.newKeySet()

    @Volatile
    private var appContext: Context? = null

    private val tapRedraws = RefreshCoalescer<Int> { ids ->
        val context = appContext ?: return@RefreshCoalescer
        draw(context, ids.toList(), skipUnchanged = false)
    }

    private val providers: List<Pair<Class<*>, WidgetKind>> = listOf(
        QuitCounterWidgetProvider::class.java to WidgetKind.QUIT_COUNTER,
        GoodHabitWidgetProvider::class.java to WidgetKind.GOOD_QUICK_LOG,
        EventWidgetProvider::class.java to WidgetKind.EVENT,
    )

    /** Runs [block] on the widget scope (uncaught errors are logged, never crash the app). */
    fun launch(block: suspend CoroutineScope.() -> Unit) = scope.launch(block = block)

    /** Fire-and-forget full refresh. [firstIds] are rendered first (the widget the user just touched). */
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

    /**
     * After a "+" / "-" write: redraws [touchedWidgetId] first, then every other quick-log widget bound to [habitId].
     * Returns once a redraw that started after this call is on screen, so what is shown is what was saved.
     */
    suspend fun redrawAfterTap(context: Context, touchedWidgetId: Int, habitId: Long) {
        val app = context.applicationContext
        appContext = app
        val ids = LinkedHashSet<Int>()
        if (touchedWidgetId > 0) ids += touchedWidgetId
        idsOf(app, WidgetKind.GOOD_QUICK_LOG).filterTo(ids) { WidgetPrefs.targetOf(app, it) == habitId }
        if (ids.isNotEmpty()) tapRedraws.request(ids)
    }

    /** Redraws every widget (system events, launcher requests). [firstIds] go first. */
    suspend fun refresh(context: Context, firstIds: IntArray? = null) {
        val all = allWidgets(context)
        nextChanges.keys.retainAll(all.keys) // forget removed widgets
        drawn.retainAll(all.keys)
        if (all.isEmpty()) {
            cancelAlarm(context)
            return
        }
        val priority = firstIds?.toSet().orEmpty()
        draw(context, all.keys.sortedBy { if (it in priority) 0 else 1 }, skipUnchanged = false)
    }

    /** Redraws the widgets of [kinds] whose content changed (data observer). */
    private suspend fun refreshKinds(context: Context, kinds: Set<WidgetKind>) {
        val ids = kinds.flatMap { idsOf(context, it) }
        if (ids.isNotEmpty()) draw(context, ids, skipUnchanged = true)
    }

    private suspend fun draw(context: Context, ids: List<Int>, skipUnchanged: Boolean) {
        lock.withLock {
            val manager = AppWidgetManager.getInstance(context)
            val kinds = allWidgets(context)
            for (id in ids) {
                val kind = kinds[id] ?: continue // removed meanwhile
                try {
                    val rendered = WidgetRenderer.render(context, id, kind, skipUnchanged)
                        ?: continue // unchanged since it was last drawn
                    manager.updateAppWidget(id, rendered.views)
                    rendered.nextChangeAtEpochMillis?.let { nextChanges[id] = it } ?: nextChanges.remove(id)
                    drawn += id
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.w(TAG, "render failed for widget $id", e)
                    nextChanges.remove(id)
                    drawn += id // no instant of its own: the alarm still covers midnight and the other widgets
                }
            }
            // A skipped widget keeps its stored instant. Re-arm only when every widget's instant is known: right after
            // a cold start a tap draws one widget, and the alarm armed by the last full refresh must stay valid.
            if (drawn.containsAll(kinds.keys)) scheduleNext(context, kinds.keys.map { nextChanges[it] })
        }
    }

    private fun allWidgets(context: Context): Map<Int, WidgetKind> {
        val manager = AppWidgetManager.getInstance(context)
        return providers.flatMap { (cls, kind) -> manager.getAppWidgetIds(ComponentName(context, cls)).map { it to kind } }.toMap()
    }

    private fun idsOf(context: Context, kind: WidgetKind): List<Int> {
        val cls = providers.first { it.second == kind }.first
        return AppWidgetManager.getInstance(context).getAppWidgetIds(ComponentName(context, cls)).toList()
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

    /** While the app process is alive, redraw the widgets whose habits, logs, events or theme changed. */
    fun startObserving(context: Context) {
        if (!observing.compareAndSet(false, true)) return
        val app = context.applicationContext
        appContext = app
        scope.launch {
            val changes = KoinPlatform.getKoin().get<ObserveWidgetDataChangesUseCase>()
            // The use case already skips the first value: widgets are drawn for the current state.
            val due = mutableSetOf<WidgetKind>()
            changes().collectLatest { kinds ->
                // A newer change cancels this pass, so keep what is still to draw: a quit widget must not go on
                // showing a name the user just hid because an event changed a moment later.
                due += kinds
                if (WidgetKind.QUIT_COUNTER !in due) delay(DEBOUNCE_MS) // quit changes are rare and may be a hidden name
                val drawing = due.toSet()
                refreshKinds(app, drawing)
                due -= drawing
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
