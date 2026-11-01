package com.example.reclaimapp.domain.widget

import com.example.reclaimapp.domain.calculator.DayStatus
import com.example.reclaimapp.domain.model.EventCategory

/** The three Android home-screen widgets. */
enum class WidgetKind { QUIT_COUNTER, GOOD_QUICK_LOG, EVENT }

/** Theme and language the widgets should follow (read from the app settings). */
data class WidgetAppearance(
    val darkTheme: Boolean = true,
    val languageCode: String = "en",
)

private const val MILLIS_PER_DAY = 86_400_000L
private const val MILLIS_PER_HOUR = 3_600_000L
private const val TEN_HOURS = 10 * MILLIS_PER_HOUR

/**
 * A duration split for the widget clock: whole [days] shown as text plus [remainderMillis] (0 until one day)
 * handed to a system-driven chronometer, so the widget ticks without waking the app.
 */
data class WidgetClockSplit(val days: Long, val remainderMillis: Long) {
    /**
     * The Chronometer itself prints H:MM:SS from one hour on and MM:SS before. The widget always shows the hours
     * field: inside the first hour the renderer adds a "0:" prefix, so the HRS label is there at every moment.
     */
    val showsHours: Boolean get() = remainderMillis >= MILLIS_PER_HOUR

    /** Digits of the hours field: two from ten hours on, otherwise one (the HRS label is centered under them). */
    val hoursDigits: Int get() = if (remainderMillis >= TEN_HOURS) 2 else 1

    /**
     * Count-up clock: offset into the current day at which the printed text next changes: one hour (the "0:" prefix
     * goes), ten hours (a second hour digit) or the end of the day.
     */
    val nextCountUpOffsetMillis: Long
        get() = when {
            remainderMillis < MILLIS_PER_HOUR -> MILLIS_PER_HOUR
            remainderMillis < TEN_HOURS -> TEN_HOURS
            else -> MILLIS_PER_DAY
        }

    companion object {
        /** Count-up split (time since something). */
        fun ofElapsed(elapsedMillis: Long): WidgetClockSplit {
            val e = elapsedMillis.coerceAtLeast(0)
            return WidgetClockSplit(e / MILLIS_PER_DAY, e % MILLIS_PER_DAY)
        }

        /**
         * Countdown split (time until something). The remainder is in (0, 1 day]; an exact multiple of a day is shown
         * as `days - 1` plus a full day so the chronometer never starts at zero and goes negative.
         */
        fun ofRemaining(remainingMillis: Long): WidgetClockSplit {
            if (remainingMillis <= 0) return WidgetClockSplit(0, 0)
            val days = (remainingMillis - 1) / MILLIS_PER_DAY
            return WidgetClockSplit(days, remainingMillis - days * MILLIS_PER_DAY)
        }
    }
}

/** What the quit-counter widget shows. */
sealed interface QuitWidgetState {
    /** The user has no active quit habits. */
    data object NoHabits : QuitWidgetState

    /** Habits exist but this widget has no habit chosen yet. */
    data object NotConfigured : QuitWidgetState

    /** The chosen habit was deleted or archived. */
    data object HabitMissing : QuitWidgetState

    data class Active(
        val habitId: Long,
        val name: String,
        val elapsedMillis: Long,
        /** Start of the current clean period; `null` if tracking never started. */
        val periodStartEpochMillis: Long?,
        val colorArgb: Long?,
        val longestStreakMillis: Long = elapsedMillis,
    ) : QuitWidgetState {
        val clock: WidgetClockSplit get() = WidgetClockSplit.ofElapsed(elapsedMillis)

        /**
         * Instant at which what the widget prints must change, or `null` when there is no running period:
         * the "0:" hours prefix goes after the first hour of the day, the hours field gets a second digit at ten
         * hours, and the whole-day text changes every 24 h.
         */
        val nextChangeAtEpochMillis: Long?
            get() = periodStartEpochMillis?.let { start ->
                start + clock.days * MILLIS_PER_DAY + clock.nextCountUpOffsetMillis
            }
    }
}

/** What the good-habit quick-log widget shows. */
sealed interface GoodWidgetState {
    data object NoHabits : GoodWidgetState
    data object NotConfigured : GoodWidgetState
    data object HabitMissing : GoodWidgetState

    data class Active(
        val habitId: Long,
        val name: String,
        val count: Int,
        val target: Int,
        val colorArgb: Long?,
        /** Last 7 days, oldest first, ending today (same strip as the in-app card). */
        val week: List<WidgetDay> = emptyList(),
        val currentStreakDays: Int = 0,
        val totalTimesDone: Int = 0,
    ) : GoodWidgetState {
        val done: Boolean get() = count >= target
        val remaining: Int get() = (target - count).coerceAtLeast(0)
        val progress: Float get() = (count.toFloat() / target.coerceAtLeast(1)).coerceIn(0f, 1f)
        val canDecrement: Boolean get() = count > 0
    }
}

/** One square of the week strip. */
data class WidgetDay(
    val isoWeekday: Int,
    val status: DayStatus,
    val enabled: Boolean,
    val isToday: Boolean,
)

/** What the events widget shows. A `null` binding means "nearest event". */
sealed interface EventWidgetState {
    data object NoEvents : EventWidgetState

    /** The chosen event was deleted. */
    data object EventMissing : EventWidgetState

    data class Upcoming(
        val eventId: Long,
        val title: String,
        val targetAtEpochMillis: Long,
        val remainingMillis: Long,
        val category: EventCategory = EventCategory.OTHER,
        val reminderCount: Int = 0,
    ) : EventWidgetState {
        val clock: WidgetClockSplit get() = WidgetClockSplit.ofRemaining(remainingMillis)

        /**
         * Next instant the printed text must change: the hours field drops to one digit (below ten hours), the
         * Chronometer drops below one hour (the "0:" prefix is needed), and it reaches zero at a day rollover or at the
         * event itself. One second of slack so the re-render sees the smaller value.
         */
        val nextChangeAtEpochMillis: Long
            get() {
                val zeroAt = targetAtEpochMillis - clock.days * MILLIS_PER_DAY
                return when {
                    clock.remainderMillis >= TEN_HOURS -> zeroAt - TEN_HOURS + 1_000L
                    clock.remainderMillis > MILLIS_PER_HOUR -> zeroAt - MILLIS_PER_HOUR + 1_000L
                    else -> zeroAt
                }
            }
    }

    /** The event time has passed: count up. */
    data class Passed(
        val eventId: Long,
        val title: String,
        val targetAtEpochMillis: Long,
        val sinceMillis: Long,
        val category: EventCategory = EventCategory.OTHER,
        val reminderCount: Int = 0,
    ) : EventWidgetState {
        val clock: WidgetClockSplit get() = WidgetClockSplit.ofElapsed(sinceMillis)
        val nextChangeAtEpochMillis: Long
            get() = targetAtEpochMillis + clock.days * MILLIS_PER_DAY + clock.nextCountUpOffsetMillis
    }
}
