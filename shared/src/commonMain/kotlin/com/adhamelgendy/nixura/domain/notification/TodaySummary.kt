package com.adhamelgendy.nixura.domain.notification

import com.adhamelgendy.nixura.domain.calculator.GoodHabitStats
import com.adhamelgendy.nixura.domain.calculator.GoodHabitStatsCalculator
import com.adhamelgendy.nixura.domain.model.AppSettings
import com.adhamelgendy.nixura.domain.model.DigitStyle
import com.adhamelgendy.nixura.domain.model.Event
import com.adhamelgendy.nixura.domain.model.GoodHabit
import com.adhamelgendy.nixura.domain.model.GoodHabitWithLogs
import com.adhamelgendy.nixura.domain.model.HabitTracking
import com.adhamelgendy.nixura.domain.reminder.HabitReminderRules
import com.adhamelgendy.nixura.domain.reminder.Prayer
import com.adhamelgendy.nixura.domain.reminder.PrayerTimeCalculator
import com.adhamelgendy.nixura.domain.reminder.ReminderDeviceState
import com.adhamelgendy.nixura.domain.reminder.ReminderStrategy
import com.adhamelgendy.nixura.domain.theme.HabitTheme
import com.adhamelgendy.nixura.domain.theme.WaterGoal
import com.adhamelgendy.nixura.domain.time.DayCalculator

/** One value of the metric layout (Android 17 `MetricStyle`): "Drink water", "3/8". */
data class TodayMetric(val label: String, val value: String)

/** The next reminder. The system draws the countdown, so nothing wakes up to tick. */
data class TodayCountdown(val label: String, val atEpochMillis: Long, val metricLabel: String, val clock: TodayClock)

/** A time of day in two parts ("8:50", "PM"), so a narrow column keeps the hour and minute whole. */
data class TodayClock(val time: String, val suffix: String)

/**
 * Everything the "Today" notification shows. Shown to anyone on a locked screen, so it is written to be safe: it
 * never holds an event's name or a quit habit's name. Holds no "now", so equal data gives an equal summary and is
 * not posted again.
 *
 * @param lines the expanded text, at most [TodaySummaryBuilder.MAX_LINES].
 * @param action "+1 glass" or "Asr prayed", for what is next; `null` = nothing to log right now.
 * @param day the local day it is about: a button pressed on an older one must not log on the new day.
 * @param refreshAtEpochMillis midnight, or sooner when something shown stops being true.
 */
data class TodaySummary(
    val title: String,
    val text: String,
    val lines: List<String>,
    val metrics: List<TodayMetric>,
    val countdown: TodayCountdown?,
    val action: NotificationAction?,
    val hideLabel: String,
    val day: Long,
    val refreshAtEpochMillis: Long,
    val digitStyle: DigitStyle,
)

/** What should be on screen for "Today". */
sealed interface TodayState {
    /** Not turned on, or nothing to say. */
    data object Off : TodayState

    /** The user dismissed it: it stays away until [refreshAtEpochMillis], the next midnight. */
    data class Hidden(val refreshAtEpochMillis: Long) : TodayState

    data class Showing(val summary: TodaySummary) : TodayState
}

/**
 * Puts [TodayState] on screen. Android: an ongoing notification (no foreground service, no Live Update).
 * iOS has no ongoing notification, so it is unsupported there and the UI does not offer it.
 */
interface TodayPresenter {
    val isSupported: Boolean get() = true

    fun show(state: TodayState)
}

object NoOpTodayPresenter : TodayPresenter {
    override val isSupported: Boolean get() = false
    override fun show(state: TodayState) = Unit
}

/** The words of "Today". Implemented by `Strings`. */
interface TodayCopy {
    fun todayTitle(done: Int, total: Int): String

    fun todayTitleNoHabits(): String

    fun todayWater(count: Int, target: Int): String

    fun todayPrayers(count: Int, target: Int): String

    fun todayHabit(name: String, count: Int, target: Int): String

    fun todayMore(count: Int): String

    fun todayNext(label: String, minutesOfDay: Int): String

    /** Timing only. [minutesOfDay] is set only for a timed event today. */
    fun todayEvent(daysAway: Long, minutesOfDay: Int?): String

    fun todayStreak(days: Long): String

    fun todayMetricDone(): String

    /** Beside the next reminder's time in the metric layout, where a habit's own progress has the same name. */
    fun todayMetricNext(label: String): String

    fun todayMetricClock(minutesOfDay: Int): TodayClock

    fun todayHide(): String

    fun todayActionPrayed(prayer: Prayer): String

    fun prayerName(prayer: Prayer): String

    fun reminderActionGlass(servingMl: Int): String
}

/** The wording for a resolved language ("en" / "ar"). */
fun interface TodayCopyProvider {
    fun forLanguage(language: String): TodayCopy
}

/**
 * Pure builder of the opt-in "Today" notification (owner decision 1, the one exception to "no summaries").
 * Progress uses the cards' own calculator; "next" is the next reminder that is really planned.
 *
 * The button is for what is next, but never lets the user log something that has not happened: a prayer is offered
 * only once it has started, and by name, because marking prayer N also marks the ones before it. So the first
 * unmarked prayer is the one offered.
 */
class TodaySummaryBuilder(private val days: DayCalculator, private val copies: TodayCopyProvider) {

    private class Row(val habit: GoodHabit, val stats: GoodHabitStats, val strategy: ReminderStrategy)

    fun build(
        nowMillis: Long,
        language: String,
        settings: AppSettings,
        goodHabits: List<GoodHabitWithLogs>,
        quitHabits: List<HabitTracking>,
        events: List<Event>,
        reminders: List<PlannedNotification>,
        device: ReminderDeviceState,
    ): TodayState {
        if (!settings.todayNotification || !settings.notificationsEnabled) return TodayState.Off
        val today = days.dayIndexOf(nowMillis)
        val midnight = days.startOfDayMillis(today + 1)
        if (device.todayHiddenDay == today) return TodayState.Hidden(midnight)

        val copy = copies.forLanguage(language)
        val rows = goodHabits.map {
            Row(
                habit = it.habit,
                stats = GoodHabitStatsCalculator.compute(it, today, days.dayIndexOf(it.habit.createdAtEpochMillis)),
                strategy = HabitReminderRules.strategyOf(it.habit),
            )
        }
        val event = events
            .filter { it.targetAtEpochMillis > nowMillis || (it.allDay && days.dayIndexOf(it.targetAtEpochMillis) == today) }
            .minByOrNull { it.targetAtEpochMillis }
        // The longest running streak, and only with the opt-in. Its habit's name is never read.
        val streakStart = if (settings.todayShowStreak) quitHabits.mapNotNull { it.tracking.activePeriodStartEpochMillis }.minOrNull() else null
        if (rows.isEmpty() && event == null && streakStart == null) return TodayState.Off

        val next = reminders
            .filter { it.kind == NotificationKind.GOOD_HABIT && it.triggerAtEpochMillis > nowMillis }
            .minByOrNull { it.triggerAtEpochMillis }
        val countdown = next?.let { item ->
            val prayer = item.actions.firstNotNullOfOrNull { it.prayerIndex }?.let(Prayer::ofIndex)
            val label = prayer?.let(copy::prayerName) ?: rows.firstOrNull { it.habit.id == item.targetId }?.habit?.name
            label?.let {
                val at = item.triggerAtEpochMillis
                TodayCountdown(it, at, copy.todayMetricNext(it), copy.todayMetricClock(minutesOfDay(at)))
            }
        }

        val water = rows.firstOrNull { it.strategy == ReminderStrategy.WATER }
        val prayers = rows.firstOrNull { it.strategy == ReminderStrategy.PRAYER }
        // The collapsed line: water and prayer, else the first two habits, so it is never the title alone.
        val highlights = listOfNotNull(
            water?.let { copy.todayWater(it.stats.progressCount, it.stats.progressTarget) },
            prayers?.let { copy.todayPrayers(it.stats.progressCount, it.stats.progressTarget) },
        ).ifEmpty { rows.take(2).map { copy.todayHabit(it.habit.name, it.stats.progressCount, it.stats.progressTarget) } }
        val nextLine = countdown?.let { copy.todayNext(it.label, minutesOfDay(it.atEpochMillis)) }
        val eventLine = event?.let {
            val away = days.dayIndexOf(it.targetAtEpochMillis) - today
            copy.todayEvent(away, if (away == 0L && !it.allDay) minutesOfDay(it.targetAtEpochMillis) else null)
        }
        val streakDays = streakStart?.let { (nowMillis - it).coerceAtLeast(0) / DayCalculator.MILLIS_PER_DAY }
        val streakLine = streakDays?.let(copy::todayStreak)
        val settled = rows.count { it.stats.settledToday }

        val prayerTimes = if (prayers != null) device.location?.let { PrayerTimeCalculator.forDay(today, it) } else null
        val unmarked = prayers?.let { Prayer.ofIndex(it.stats.todayCount + 1) }
        val unmarkedStart = unmarked?.let { prayerTimes?.get(it) }
        val markPrayer = if (prayers != null && unmarked != null && unmarkedStart != null && unmarkedStart <= nowMillis) {
            NotificationAction(NotificationActionType.LOG_PRAYER, copy.todayActionPrayed(unmarked), prayers.habit.id, prayerIndex = unmarked.index)
        } else {
            null
        }
        val addGlass = water?.takeUnless { it.stats.doneToday }?.let {
            val serving = it.habit.water?.servingMl ?: WaterGoal.DEFAULT_SERVING_ML
            NotificationAction(NotificationActionType.LOG_ONE, copy.reminderActionGlass(serving), it.habit.id)
        }
        val action = if (next?.theme == HabitTheme.WATER) addGlass ?: markPrayer else markPrayer ?: addGlass

        val refreshAt = listOfNotNull(
            midnight,
            countdown?.let { it.atEpochMillis + COUNTDOWN_GRACE_MILLIS }, // else it would count below zero
            unmarkedStart?.takeIf { it > nowMillis }, // the "prayed" button appears
            event?.targetAtEpochMillis?.takeIf { it > nowMillis },
            streakStart?.let { it + ((streakDays ?: 0) + 1) * DayCalculator.MILLIS_PER_DAY },
        ).min()

        val shownRows = rows.take(MAX_HABIT_LINES)
        val lines = shownRows.map { copy.todayHabit(it.habit.name, it.stats.progressCount, it.stats.progressTarget) } +
            listOfNotNull(
                (rows.size - shownRows.size).takeIf { it > 0 }?.let(copy::todayMore),
                nextLine,
                eventLine,
                streakLine,
            )
        val doneMetric = TodayMetric(copy.todayMetricDone(), "$settled/${rows.size}").takeIf { rows.isNotEmpty() }
        val metrics = listOfNotNull(water, prayers)
            .map { TodayMetric(it.habit.name, "${it.stats.progressCount}/${it.stats.progressTarget}") }
            .let { if (it.size < 2) it + listOfNotNull(doneMetric) else it }

        return TodayState.Showing(
            TodaySummary(
                title = if (rows.isEmpty()) copy.todayTitleNoHabits() else copy.todayTitle(settled, rows.size),
                text = (highlights + listOfNotNull(nextLine, eventLine, streakLine)).joinToString(" · "),
                lines = lines,
                metrics = metrics,
                countdown = countdown,
                action = action,
                hideLabel = copy.todayHide(),
                day = today,
                refreshAtEpochMillis = refreshAt,
                digitStyle = settings.digitStyle,
            ),
        )
    }

    private fun minutesOfDay(epochMillis: Long): Int = (days.millisOfDay(epochMillis) / 60_000L).toInt()

    companion object {
        private const val MAX_HABIT_LINES = 4

        /** Habits, "+N more", next, event, streak: what still fits an expanded notification. */
        const val MAX_LINES = MAX_HABIT_LINES + 4

        private const val COUNTDOWN_GRACE_MILLIS = 60_000L
    }
}
