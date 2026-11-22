package com.adhamelgendy.nixura.domain.reminder

import com.adhamelgendy.nixura.domain.clock.Clock
import com.adhamelgendy.nixura.domain.model.GoodHabit
import com.adhamelgendy.nixura.domain.notification.NotificationActionType
import com.adhamelgendy.nixura.domain.repository.GoodHabitRepository
import com.adhamelgendy.nixura.domain.time.DayCalculator
import com.adhamelgendy.nixura.domain.usecase.IncrementGoodHabitTodayUseCase
import com.adhamelgendy.nixura.domain.usecase.MarkGoodHabitTodayAtLeastUseCase
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** What a notification button press changed, so the platform can update the notification in place. */
sealed interface ReminderActionResult {
    /** Logged through the same use case as the widget. [prayer] is set for "Prayed". */
    data class Logged(val habit: GoodHabit, val count: Int, val prayer: Prayer? = null) : ReminderActionResult

    data class Snoozed(val minutes: Int) : ReminderActionResult

    /** The same press arrived again (double tap, re-delivered broadcast): nothing was written. */
    data object AlreadyHandled : ReminderActionResult

    /** The habit no longer exists or the write failed. */
    data object Failed : ReminderActionResult
}

/**
 * Runs a reminder button without opening the app (phase C). It writes through the **same** use cases as the
 * home-screen widget ([IncrementGoodHabitTodayUseCase] for "+1 glass" / "Done", the same lock for "Prayed"), so the
 * app, the widget and the notification can never disagree.
 *
 * Exactly once: each press carries a [token] (the reminder's alarm key + time + button). The token is claimed before
 * writing and released if the write fails, so a re-delivered press is ignored and a failed one can be retried.
 * "Prayed" is idempotent on its own (count ≥ N).
 */
class ReminderActionHandler(
    private val increment: IncrementGoodHabitTodayUseCase,
    private val markAtLeast: MarkGoodHabitTodayAtLeastUseCase,
    private val habits: GoodHabitRepository,
    private val device: ReminderDeviceRepository,
    private val clock: Clock,
    private val days: DayCalculator,
) {
    private val lock = Mutex()

    suspend fun handle(
        token: String,
        type: NotificationActionType,
        habitId: Long,
        prayerIndex: Int? = null,
        snoozeMinutes: Int? = null,
    ): ReminderActionResult = lock.withLock {
        val now = clock.nowEpochMillis()
        if (!device.claimAction(token, now)) return@withLock ReminderActionResult.AlreadyHandled
        val result = runCatching { perform(type, habitId, prayerIndex, snoozeMinutes, now) }.getOrNull() ?: ReminderActionResult.Failed
        if (result == ReminderActionResult.Failed) device.releaseAction(token)
        result
    }

    private suspend fun perform(type: NotificationActionType, habitId: Long, prayerIndex: Int?, snoozeMinutes: Int?, now: Long): ReminderActionResult =
        when (type) {
            NotificationActionType.SNOOZE -> {
                val minutes = (snoozeMinutes ?: HabitReminderRules.SNOOZE_MINUTES).coerceIn(1, MAX_SNOOZE_MINUTES)
                device.snooze(habitId, now + minutes * 60_000L)
                ReminderActionResult.Snoozed(minutes)
            }
            NotificationActionType.LOG_ONE -> logged(habitId, increment(habitId), prayer = null)
            NotificationActionType.LOG_PRAYER -> {
                val prayer = prayerIndex?.let(Prayer::ofIndex) ?: return ReminderActionResult.Failed
                logged(habitId, markAtLeast(habitId, prayer.index), prayer)
            }
        }

    private suspend fun logged(habitId: Long, write: Result<Int>, prayer: Prayer?): ReminderActionResult {
        val count = write.getOrNull() ?: return ReminderActionResult.Failed
        device.clearSnooze(habitId) // a snooze for something now done would only re-plan to nothing
        val now = clock.nowEpochMillis()
        val today = days.dayIndexOf(now)
        val habit = habits.dayCountOf(habitId, today)?.habit ?: return ReminderActionResult.Failed
        // After midnight in a late water window, confirm with the evening's glasses too, like the reminder said.
        val windowDay = HabitReminderRules.windowDayOf(habit, days, now)
        val evening = if (windowDay < today) habits.dayCountOf(habitId, windowDay)?.count ?: 0 else 0
        return ReminderActionResult.Logged(habit, count + evening, prayer)
    }

    companion object {
        private const val MAX_SNOOZE_MINUTES = 24 * 60

        /** The once-only identity of one press of one button on one reminder. */
        fun tokenOf(alarmKey: String, triggerAt: Long, type: NotificationActionType): String = "$alarmKey@$triggerAt:${type.name}"
    }
}
