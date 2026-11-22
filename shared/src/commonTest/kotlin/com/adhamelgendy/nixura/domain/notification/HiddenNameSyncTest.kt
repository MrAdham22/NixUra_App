package com.adhamelgendy.nixura.domain.notification

import com.adhamelgendy.nixura.domain.coach.CoachEngine
import com.adhamelgendy.nixura.domain.clock.Clock
import com.adhamelgendy.nixura.domain.model.AppSettings
import com.adhamelgendy.nixura.domain.model.Habit
import com.adhamelgendy.nixura.domain.model.HabitTracking
import com.adhamelgendy.nixura.domain.model.TrackingSnapshot
import com.adhamelgendy.nixura.domain.model.UserProfile
import com.adhamelgendy.nixura.domain.money.HabitCost
import com.adhamelgendy.nixura.domain.repository.HabitTrackingRepository
import com.adhamelgendy.nixura.domain.repository.PreferencesRepository
import com.adhamelgendy.nixura.domain.time.DayCalculator
import com.adhamelgendy.nixura.domain.widget.FakeEventRepository
import com.adhamelgendy.nixura.domain.widget.FakeGoodHabitRepository
import com.adhamelgendy.nixura.presentation.strings.stringsFor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** A change of "hide name" re-arms the alarms with the new words and withdraws what is showing the name. */
class HiddenNameSyncTest {
    private val days = DayCalculator { 0L }
    private val now = days.epochMillisOf(2026, 11, 22, 8, 0)

    private inner class Habits : HabitTrackingRepository {
        val state = MutableStateFlow(listOf(HabitTracking(Habit(7, "Smoking", 0), emptyList(), TrackingSnapshot(now, 0, 0))))
        override fun observeActiveHabitTracking() = state
        override suspend fun createHabitAndStartTracking(name: String, startAtEpochMillis: Long, colorArgb: Long?, cost: HabitCost?, hideNameOutside: Boolean): Long = error("unused")
        override suspend fun recordRelapse(habitId: Long, atEpochMillis: Long) = error("unused")
        override suspend fun updateHabit(habitId: Long, name: String, colorArgb: Long?, hideNameOutside: Boolean) = error("unused")
        override suspend fun setHideNameOutside(habitId: Long, hidden: Boolean) {
            state.value = state.value.map { it.copy(habit = it.habit.copy(hideNameOutside = hidden)) }
        }
        override suspend fun updateCost(habitId: Long, cost: HabitCost?) = error("unused")
        override suspend fun archiveHabit(habitId: Long, atEpochMillis: Long) = error("unused")
    }

    private class Prefs : PreferencesRepository {
        override fun observeProfile() = flowOf(UserProfile())
        override fun observeSettings() = flowOf(AppSettings())
        override suspend fun saveProfile(profile: UserProfile) = error("unused")
        override suspend fun saveSettings(settings: AppSettings) = error("unused")
    }

    private class Scheduler : NotificationScheduler {
        val calls = mutableListOf<String>()
        var armed = emptyList<PlannedNotification>()
        var withdrawn = emptySet<Long>()
        override fun replaceAll(planned: List<PlannedNotification>) {
            calls += "arm"
            armed = planned
        }
        override fun withdrawNamesOf(hiddenHabitIds: Set<Long>) {
            calls += "withdraw"
            withdrawn = hiddenHabitIds
        }
    }

    @Test fun hidingReArmsFirstThenWithdraws() = runTest {
        val habits = Habits()
        val scheduler = Scheduler()
        val service = NotificationSyncService(
            habits, FakeGoodHabitRepository(emptyList()), FakeEventRepository(emptyList()), Prefs(),
            NotificationPlanner(CoachEngine(), days, { stringsFor(it) }), scheduler,
            object : Clock { override fun nowEpochMillis() = now },
        )
        service.syncOnce()
        assertTrue(scheduler.armed.any { "Smoking" in it.title })

        habits.setHideNameOutside(7, true)
        scheduler.calls.clear()
        service.syncOnce()
        assertTrue(scheduler.armed.none { "Smoking" in it.title + it.body })
        assertEquals(setOf(7L), scheduler.withdrawn)
        assertEquals(listOf("arm", "withdraw"), scheduler.calls) // no alarm with the old words is left to post it again
    }

    @Test fun everyReplanRefreshesTodayAndARefreshAloneArmsNothing() = runTest {
        val scheduler = Scheduler()
        val shown = mutableListOf<TodayState>()
        val service = NotificationSyncService(
            Habits(), FakeGoodHabitRepository(emptyList()), FakeEventRepository(emptyList()), Prefs(),
            NotificationPlanner(CoachEngine(), days, { stringsFor(it) }), scheduler,
            object : Clock { override fun nowEpochMillis() = now },
            todayBuilder = TodaySummaryBuilder(days) { stringsFor(it) },
            today = object : TodayPresenter { override fun show(state: TodayState) { shown += state } },
        )
        service.syncOnce()
        assertEquals(listOf<TodayState>(TodayState.Off), shown)
        scheduler.calls.clear()
        service.refreshToday()
        assertEquals(2, shown.size)
        assertEquals(emptyList(), scheduler.calls)
    }
}
