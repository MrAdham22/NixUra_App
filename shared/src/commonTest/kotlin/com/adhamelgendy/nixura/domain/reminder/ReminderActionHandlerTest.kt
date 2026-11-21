package com.adhamelgendy.nixura.domain.reminder

import com.adhamelgendy.nixura.data.local.dao.PreferenceDao
import com.adhamelgendy.nixura.data.local.entity.PreferenceEntity
import com.adhamelgendy.nixura.data.repository.ReminderDeviceRepositoryImpl
import com.adhamelgendy.nixura.domain.model.GoodHabit
import com.adhamelgendy.nixura.domain.model.GoodHabitWithLogs
import com.adhamelgendy.nixura.domain.notification.NotificationActionType
import com.adhamelgendy.nixura.domain.repository.GoodHabitDayCount
import com.adhamelgendy.nixura.domain.repository.GoodHabitRepository
import com.adhamelgendy.nixura.domain.theme.HabitTheme
import com.adhamelgendy.nixura.domain.theme.WaterGoal
import com.adhamelgendy.nixura.domain.usecase.IncrementGoodHabitTodayUseCase
import com.adhamelgendy.nixura.domain.usecase.MarkGoodHabitTodayAtLeastUseCase
import com.adhamelgendy.nixura.domain.widget.DAY
import com.adhamelgendy.nixura.domain.widget.FixedClock
import com.adhamelgendy.nixura.domain.widget.HOUR
import com.adhamelgendy.nixura.domain.widget.runSuspend
import com.adhamelgendy.nixura.domain.widget.utcDays
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Notification buttons write through the widget's own use cases and count once per press, even when Android delivers
 * the press twice. The device store is the real Room-backed implementation over an in-memory preferences table.
 */
class ReminderActionHandlerTest {
    private val today = 20L
    private val clock = FixedClock(today * DAY + 10 * HOUR)
    private val days = utcDays()

    private class Repo(vararg habits: GoodHabit) : GoodHabitRepository {
        val habits = habits.associateBy { it.id }.toMutableMap()
        val counts = mutableMapOf<Long, Int>()
        override fun observeGoodHabits(): Flow<List<GoodHabitWithLogs>> = error("unused")
        override suspend fun dayCountOf(id: Long, dayIndex: Long): GoodHabitDayCount? = habits[id]?.let { GoodHabitDayCount(it, counts[id] ?: 0) }
        override suspend fun setCount(id: Long, dayIndex: Long, count: Int) { counts[id] = count }
        override suspend fun create(name: String, dailyTarget: Int, colorArgb: Long?, weeklyTarget: Int?, theme: HabitTheme, water: WaterGoal?): Long = error("unused")
        override suspend fun update(id: Long, name: String, dailyTarget: Int, colorArgb: Long?, weeklyTarget: Int?, theme: HabitTheme, water: WaterGoal?) = error("unused")
        override suspend fun archive(id: Long) = error("unused")
        override suspend fun setReminder(id: Long, reminder: HabitReminderSettings) = error("unused")
    }

    /** The preferences table in memory. */
    private class Prefs : PreferenceDao {
        val rows = MutableStateFlow<Map<String, String>>(emptyMap())
        override fun observeAll(): Flow<List<PreferenceEntity>> = kotlinx.coroutines.flow.flow { emit(rows.value.map { PreferenceEntity(it.key, it.value) }) }
        override suspend fun getAll(): List<PreferenceEntity> = rows.value.map { PreferenceEntity(it.key, it.value) }
        override suspend fun upsertAll(items: List<PreferenceEntity>) { rows.value = rows.value + items.associate { it.key to it.value } }
        override suspend fun delete(key: String) { rows.value = rows.value - key }
    }

    private val water = GoodHabit(1, "Drink water", 8, createdAtEpochMillis = 0, theme = HabitTheme.WATER, water = WaterGoal.fromCups(8))
    private val prayer = GoodHabit(2, "Pray", 5, createdAtEpochMillis = 0, theme = HabitTheme.PRAYER)
    private val repo = Repo(water, prayer)
    private val device = ReminderDeviceRepositoryImpl(Prefs())
    private val handler = ReminderActionHandler(
        IncrementGoodHabitTodayUseCase(repo, clock, days),
        MarkGoodHabitTodayAtLeastUseCase(repo, clock, days),
        repo, device, clock, days,
    )

    @Test fun plusOneGlassCountsOncePerPress() = runSuspend {
        val token = ReminderActionHandler.tokenOf("good:1:20:w660", 1L, NotificationActionType.LOG_ONE)
        val first = handler.handle(token, NotificationActionType.LOG_ONE, 1)
        val again = handler.handle(token, NotificationActionType.LOG_ONE, 1) // re-delivered broadcast / double tap
        assertIs<ReminderActionResult.Logged>(first)
        assertEquals(1, first.count)
        assertEquals(ReminderActionResult.AlreadyHandled, again)
        assertEquals(1, repo.counts[1])
    }

    @Test fun twoRemindersAreTwoGlasses() = runSuspend {
        handler.handle(ReminderActionHandler.tokenOf("good:1:20:w660", 1L, NotificationActionType.LOG_ONE), NotificationActionType.LOG_ONE, 1)
        handler.handle(ReminderActionHandler.tokenOf("good:1:20:w780", 2L, NotificationActionType.LOG_ONE), NotificationActionType.LOG_ONE, 1)
        assertEquals(2, repo.counts[1])
    }

    @Test fun prayedMarksAtLeastThatPrayerAndNeverCountsTwice() = runSuspend {
        repo.counts[2] = 1 // Fajr marked in the app
        val asr = handler.handle("t-asr", NotificationActionType.LOG_PRAYER, 2, prayerIndex = 3)
        assertIs<ReminderActionResult.Logged>(asr)
        assertEquals(3, asr.count)
        assertEquals(Prayer.ASR, asr.prayer)
        // An older Dhuhr reminder pressed afterwards changes nothing (3 ≥ 2).
        handler.handle("t-dhuhr", NotificationActionType.LOG_PRAYER, 2, prayerIndex = 2)
        assertEquals(3, repo.counts[2])
    }

    @Test fun aFailedWriteCanBeRetried() = runSuspend {
        repo.habits.remove(1L)
        assertEquals(ReminderActionResult.Failed, handler.handle("t", NotificationActionType.LOG_ONE, 1))
        repo.habits[1] = water
        assertIs<ReminderActionResult.Logged>(handler.handle("t", NotificationActionType.LOG_ONE, 1))
        assertEquals(1, repo.counts[1])
    }

    @Test fun snoozeStoresWhenToShowItAgainAndLoggingClearsIt() = runSuspend {
        val result = handler.handle("s", NotificationActionType.SNOOZE, 1, snoozeMinutes = 60)
        assertEquals(ReminderActionResult.Snoozed(60), result)
        assertEquals(clock.now + HOUR, device.observe().first().snoozes[1])
        handler.handle("l", NotificationActionType.LOG_ONE, 1)
        assertNull(device.observe().first().snoozes[1])
    }

    @Test fun oldPressRecordsAreForgotten() = runSuspend {
        assertTrue(device.claimAction("old", 0L))
        assertTrue(device.claimAction("new", clock.now)) // prunes "old" (days ago)
        assertTrue(device.claimAction("old", clock.now))
    }

    @Test fun theLocationStaysOnThePhoneRoundTrip() = runSuspend {
        device.setLocation(GeoPoint(30.04, 31.24))
        assertEquals(GeoPoint(30.04, 31.24), device.observe().first().location)
        device.setLocation(null)
        assertNull(device.observe().first().location)
    }
}
