package com.example.reclaimapp.widgets

import android.content.Context
import com.example.reclaimapp.domain.widget.WidgetKind

/**
 * `appWidgetId -> habit/event id` bindings, in plain SharedPreferences (no new dependency, no Room change).
 * No entry means "not chosen yet" (quit / good widgets) or "nearest event" (events widget).
 */
internal object WidgetPrefs {
    private const val FILE = "nixura_widgets"

    private fun prefs(context: Context) = context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)
    private fun key(widgetId: Int) = "target_$widgetId"

    fun targetOf(context: Context, widgetId: Int): Long? {
        val p = prefs(context)
        if (!p.contains(key(widgetId))) return null
        return p.getLong(key(widgetId), -1L).takeIf { it > 0 }
    }

    fun setTarget(context: Context, widgetId: Int, targetId: Long?) {
        val edit = prefs(context).edit()
        if (targetId == null) edit.remove(key(widgetId)) else edit.putLong(key(widgetId), targetId)
        edit.apply()
    }

    fun remove(context: Context, widgetIds: IntArray) {
        val edit = prefs(context).edit()
        widgetIds.forEach { edit.remove(key(it)) }
        edit.apply()
    }

    /** After a backup restore the system assigns new widget ids; move the bindings over. */
    fun remap(context: Context, oldIds: IntArray, newIds: IntArray) {
        val p = prefs(context)
        val edit = p.edit()
        oldIds.zip(newIds.toList()).forEach { (old, new) ->
            if (p.contains(key(old))) {
                edit.putLong(key(new), p.getLong(key(old), -1L))
                edit.remove(key(old))
            }
        }
        edit.apply()
    }

    // ---- "Add to Home screen": the habit/event to bind to the next widget of this kind ----

    internal class PendingPin(val targetId: Long?)

    private const val PIN_TTL_MS = 3 * 60_000L

    fun setPendingPin(context: Context, kind: WidgetKind, targetId: Long?) {
        prefs(context).edit()
            .putLong("pin_target_${kind.name}", targetId ?: -1L)
            .putLong("pin_time_${kind.name}", System.currentTimeMillis())
            .apply()
    }

    /** Returns and clears a fresh pending pin for [kind] (older than a few minutes counts as abandoned). */
    fun takePendingPin(context: Context, kind: WidgetKind): PendingPin? {
        val p = prefs(context)
        val at = p.getLong("pin_time_${kind.name}", 0L)
        if (at == 0L) return null
        val target = p.getLong("pin_target_${kind.name}", -1L)
        clearPendingPin(context, kind)
        if (System.currentTimeMillis() - at > PIN_TTL_MS) return null
        return PendingPin(target.takeIf { it > 0 })
    }

    fun clearPendingPin(context: Context, kind: WidgetKind) {
        prefs(context).edit().remove("pin_target_${kind.name}").remove("pin_time_${kind.name}").apply()
    }
}
