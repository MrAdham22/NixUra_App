package com.example.reclaimapp.domain.widget

import com.example.reclaimapp.domain.time.DayCalculator

/**
 * Decides when widgets must next be re-rendered.
 *
 * Widgets do not tick every second: the running clock is a system-driven chronometer. A re-render is only needed when
 * something the chronometer cannot express changes: a whole-day rollover of a counter, an event passing, or local
 * midnight (today's good-habit count resets). That is at most one alarm in the future, never a periodic poll.
 */
object WidgetRefreshPlanner {
    /** Never schedule closer than this to avoid tight alarm loops. */
    const val MIN_GAP_MILLIS = 1_000L

    fun nextLocalMidnight(nowEpochMillis: Long, days: DayCalculator): Long =
        days.startOfDayMillis(days.dayIndexOf(nowEpochMillis) + 1)

    /**
     * Earliest of local midnight and every future instant in [changeInstants] (null/past entries are ignored),
     * at least [MIN_GAP_MILLIS] after [nowEpochMillis].
     */
    fun nextRefreshAt(nowEpochMillis: Long, days: DayCalculator, changeInstants: Collection<Long?>): Long {
        var next = nextLocalMidnight(nowEpochMillis, days)
        for (instant in changeInstants) {
            if (instant != null && instant > nowEpochMillis && instant < next) next = instant
        }
        return next.coerceAtLeast(nowEpochMillis + MIN_GAP_MILLIS)
    }
}
