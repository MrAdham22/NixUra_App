package com.example.reclaimapp.domain.widget

import com.example.reclaimapp.domain.calculator.DayStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class WidgetStateMapperTest {
    private val days = utcDays()

    // ---- clock split ----
    @Test fun elapsedSplitsIntoDaysAndRemainder() {
        val s = WidgetClockSplit.ofElapsed(3 * DAY + 5 * HOUR + 7)
        assertEquals(3L, s.days); assertEquals(5 * HOUR + 7, s.remainderMillis)
    }

    @Test fun negativeElapsedIsZero() = assertEquals(WidgetClockSplit(0, 0), WidgetClockSplit.ofElapsed(-5))

    @Test fun remainingSplitKeepsRemainderPositive() {
        val s = WidgetClockSplit.ofRemaining(2 * DAY + HOUR)
        assertEquals(2L, s.days); assertEquals(HOUR, s.remainderMillis)
    }

    @Test fun remainingExactDayMultipleUsesFullDayRemainder() {
        val s = WidgetClockSplit.ofRemaining(2 * DAY)
        assertEquals(1L, s.days); assertEquals(DAY, s.remainderMillis)
    }

    // ---- quit ----
    @Test fun quitNoHabits() = assertEquals(QuitWidgetState.NoHabits, WidgetStateMapper.quit(1, emptyList(), 100))

    @Test fun quitUnconfigured() =
        assertEquals(QuitWidgetState.NotConfigured, WidgetStateMapper.quit(null, listOf(habitTracking(1)), 100))

    @Test fun quitMissingHabit() =
        assertEquals(QuitWidgetState.HabitMissing, WidgetStateMapper.quit(7, listOf(habitTracking(1)), 100))

    @Test fun quitArchivedHabitIsMissing() =
        assertEquals(QuitWidgetState.HabitMissing, WidgetStateMapper.quit(1, listOf(habitTracking(1, archived = true), habitTracking(2)), 100))

    @Test fun quitActiveElapsedEqualsCalculator() {
        val s = assertIs<QuitWidgetState.Active>(WidgetStateMapper.quit(1, listOf(habitTracking(1, startAt = 1_000)), 1_000 + 2 * DAY + 3 * HOUR))
        assertEquals(2 * DAY + 3 * HOUR, s.elapsedMillis)
        assertEquals(2L, s.clock.days)
        assertEquals(3 * HOUR, s.clock.remainderMillis)
    }

    @Test fun quitRelapseResetsClock() {
        val tracking = habitTracking(1, "Smoking", 0L, false, 5 * DAY)
        val s = assertIs<QuitWidgetState.Active>(WidgetStateMapper.quit(1, listOf(tracking), 5 * DAY + 2 * HOUR))
        assertEquals(2 * HOUR, s.elapsedMillis)
        assertEquals(5 * DAY, s.periodStartEpochMillis)
    }

    @Test fun quitInFirstHourOfDayChangesWhenTheHourIsReached() {
        val s = assertIs<QuitWidgetState.Active>(WidgetStateMapper.quit(1, listOf(habitTracking(1, startAt = 500)), 500 + DAY + 10))
        assertEquals(500 + DAY + HOUR, s.nextChangeAtEpochMillis) // MM:SS -> H:MM:SS
        assertFalse(s.clock.showsHours)
    }

    @Test fun quitAfterFirstHourChangesAtTheNextWholeDay() {
        val s = assertIs<QuitWidgetState.Active>(WidgetStateMapper.quit(1, listOf(habitTracking(1, startAt = 500)), 500 + DAY + 3 * HOUR))
        assertEquals(500 + 2 * DAY, s.nextChangeAtEpochMillis)
        assertTrue(s.clock.showsHours)
    }

    @Test fun quitCarriesLongestStreak() {
        val tracking = habitTracking(1, "Smoking", 0L, false, 5 * DAY) // 5 days, then a relapse
        val s = assertIs<QuitWidgetState.Active>(WidgetStateMapper.quit(1, listOf(tracking), 5 * DAY + HOUR))
        assertEquals(5 * DAY, s.longestStreakMillis)
    }

    @Test fun quitKeepsColor() {
        val base = habitTracking(1)
        val colored = base.copy(habit = base.habit.copy(colorArgb = 0xFF112233))
        assertEquals(0xFF112233, assertIs<QuitWidgetState.Active>(WidgetStateMapper.quit(1, listOf(colored), 10)).colorArgb)
    }

    // ---- good ----
    @Test fun goodNoHabits() = assertEquals(GoodWidgetState.NoHabits, WidgetStateMapper.good(1, emptyList(), 0, days))

    @Test fun goodOnlyArchivedMeansNoHabits() =
        assertEquals(GoodWidgetState.NoHabits, WidgetStateMapper.good(1, listOf(goodHabit(1, 3, archived = true)), 0, days))

    @Test fun goodUnconfigured() =
        assertEquals(GoodWidgetState.NotConfigured, WidgetStateMapper.good(null, listOf(goodHabit(1, 3)), 0, days))

    @Test fun goodMissing() =
        assertEquals(GoodWidgetState.HabitMissing, WidgetStateMapper.good(9, listOf(goodHabit(1, 3)), 0, days))

    @Test fun goodTodayCountAndProgress() {
        val now = 10 * DAY + 4 * HOUR
        val s = assertIs<GoodWidgetState.Active>(WidgetStateMapper.good(1, listOf(goodHabit(1, 4, 10L to 1, 9L to 4)), now, days))
        assertEquals(1, s.count); assertEquals(4, s.target)
        assertEquals(0.25f, s.progress); assertEquals(3, s.remaining)
        assertFalse(s.done); assertTrue(s.canDecrement)
    }

    @Test fun goodDoneAndOverTargetClampsProgress() {
        val s = assertIs<GoodWidgetState.Active>(WidgetStateMapper.good(1, listOf(goodHabit(1, 2, 10L to 5)), 10 * DAY, days))
        assertTrue(s.done); assertEquals(1f, s.progress); assertEquals(0, s.remaining)
    }

    @Test fun goodZeroCountCannotDecrement() {
        val s = assertIs<GoodWidgetState.Active>(WidgetStateMapper.good(1, listOf(goodHabit(1, 2)), 10 * DAY, days))
        assertFalse(s.canDecrement); assertEquals(0, s.count)
    }

    @Test fun goodUsesLocalDayNotUtcDay() {
        // 22:30 UTC on day 10 is already day 11 in UTC+3.
        val now = 10 * DAY + 22 * HOUR + 30 * 60_000L
        val habit = goodHabit(1, 3, 10L to 2, 11L to 1)
        assertEquals(2, assertIs<GoodWidgetState.Active>(WidgetStateMapper.good(1, listOf(habit), now, utcDays(0))).count)
        assertEquals(1, assertIs<GoodWidgetState.Active>(WidgetStateMapper.good(1, listOf(habit), now, utcDays(3 * HOUR))).count)
    }

    // ---- events ----
    @Test fun eventNoEvents() = assertEquals(EventWidgetState.NoEvents, WidgetStateMapper.event(null, emptyList(), 0))

    @Test fun eventNearestUpcoming() {
        val s = assertIs<EventWidgetState.Upcoming>(WidgetStateMapper.event(null, listOf(event(1, 9 * DAY), event(2, 3 * DAY)), 0))
        assertEquals(2L, s.eventId); assertEquals(3 * DAY, s.remainingMillis)
    }

    @Test fun eventNearestFallsBackToMostRecentPast() {
        val s = assertIs<EventWidgetState.Passed>(WidgetStateMapper.event(null, listOf(event(1, 100), event(2, 500)), 1_000))
        assertEquals(2L, s.eventId); assertEquals(500L, s.sinceMillis)
    }

    @Test fun eventBoundMissing() =
        assertEquals(EventWidgetState.EventMissing, WidgetStateMapper.event(5, listOf(event(1, 100)), 0))

    @Test fun eventBoundPastEventCountsUp() {
        val s = assertIs<EventWidgetState.Passed>(WidgetStateMapper.event(1, listOf(event(1, 0), event(2, 10 * DAY)), 2 * DAY + HOUR))
        assertEquals(1L, s.eventId)
        assertEquals(2L, s.clock.days); assertEquals(HOUR, s.clock.remainderMillis)
        assertEquals(3 * DAY, s.nextChangeAtEpochMillis)
    }

    @Test fun eventUpcomingNextChangeIsChronometerZero() {
        val s = assertIs<EventWidgetState.Upcoming>(WidgetStateMapper.event(1, listOf(event(1, 5 * DAY + 6 * HOUR)), 0))
        assertEquals(5L, s.clock.days)
        assertEquals(5 * HOUR + 1_000L, s.nextChangeAtEpochMillis) // chronometer drops below 1 h (MM:SS)
    }

    @Test fun eventLessThanADayChangesAtTheEvent() {
        val s = assertIs<EventWidgetState.Upcoming>(WidgetStateMapper.event(1, listOf(event(1, 5 * HOUR)), 0))
        assertEquals(0L, s.clock.days); assertEquals(4 * HOUR + 1_000L, s.nextChangeAtEpochMillis)
    }

    @Test fun eventExactlyNowIsPassedWithZeroElapsed() {
        val s = assertIs<EventWidgetState.Passed>(WidgetStateMapper.event(1, listOf(event(1, 777)), 777))
        assertEquals(0L, s.sinceMillis)
    }

    @Test fun eventUnderOneHourChangesAtTheEventItself() {
        val s = assertIs<EventWidgetState.Upcoming>(WidgetStateMapper.event(1, listOf(event(1, 30 * 60_000L)), 0))
        assertFalse(s.clock.showsHours)
        assertEquals(30 * 60_000L, s.nextChangeAtEpochMillis)
    }

    @Test fun eventPassedHourBoundary() {
        val soon = assertIs<EventWidgetState.Passed>(WidgetStateMapper.event(1, listOf(event(1, 0)), 10 * 60_000L))
        assertEquals(HOUR, soon.nextChangeAtEpochMillis)
        val later = assertIs<EventWidgetState.Passed>(WidgetStateMapper.event(1, listOf(event(1, 0)), 2 * HOUR))
        assertEquals(DAY, later.nextChangeAtEpochMillis)
    }

    @Test fun eventCarriesCategoryAndReminderCount() {
        val e = event(1, 5 * DAY).copy(reminderLeadMillis = listOf(0L, HOUR, DAY))
        val s = assertIs<EventWidgetState.Upcoming>(WidgetStateMapper.event(1, listOf(e), 0))
        assertEquals(e.category, s.category); assertEquals(3, s.reminderCount)
    }

    // ---- good habit card extras ----
    @Test fun goodWeekStripHasSevenDaysEndingToday() {
        val now = 10 * DAY + 4 * HOUR
        val s = assertIs<GoodWidgetState.Active>(WidgetStateMapper.good(1, listOf(goodHabit(1, 2, 10L to 2, 9L to 1, created = 0)), now, days))
        assertEquals(7, s.week.size)
        assertTrue(s.week.last().isToday); assertEquals(1, s.week.count { it.isToday })
        assertEquals(DayStatus.DONE, s.week.last().status)
        assertEquals(DayStatus.PARTIAL, s.week[5].status)
        assertEquals(DayStatus.NONE, s.week[0].status)
        // consecutive ISO weekdays 1..7
        for (i in 1 until 7) assertEquals(s.week[i - 1].isoWeekday % 7 + 1, s.week[i].isoWeekday)
    }

    @Test fun goodWeekStripDisablesDaysBeforeTheHabitExisted() {
        val now = 100 * DAY
        val s = assertIs<GoodWidgetState.Active>(WidgetStateMapper.good(1, listOf(goodHabit(1, 2, created = 100 * DAY)), now, days))
        assertTrue(s.week.all { it.enabled }) // app rule: createdDay - 6 is still enabled
        val recent = assertIs<GoodWidgetState.Active>(WidgetStateMapper.good(1, listOf(goodHabit(1, 2, created = 100 * DAY)), now + 0, days))
        assertEquals(7, recent.week.count { it.enabled })
    }

    @Test fun goodStreakAndTotalsMatchTheCalculator() {
        val now = 10 * DAY
        val s = assertIs<GoodWidgetState.Active>(WidgetStateMapper.good(1, listOf(goodHabit(1, 2, 10L to 2, 9L to 2, 8L to 3)), now, days))
        assertEquals(3, s.currentStreakDays); assertEquals(7, s.totalTimesDone)
    }
}
