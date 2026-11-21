package com.adhamelgendy.nixura.domain.reminder

import com.adhamelgendy.nixura.domain.model.GoodHabit
import com.adhamelgendy.nixura.domain.model.GoodHabitLog
import com.adhamelgendy.nixura.domain.model.GoodHabitWithLogs
import com.adhamelgendy.nixura.domain.notification.NotificationActionType
import com.adhamelgendy.nixura.domain.notification.NotificationKind
import com.adhamelgendy.nixura.domain.notification.NotificationTiming
import com.adhamelgendy.nixura.domain.notification.PlannedNotification
import com.adhamelgendy.nixura.domain.theme.HabitTheme
import com.adhamelgendy.nixura.domain.theme.WaterGoal
import com.adhamelgendy.nixura.domain.time.CivilCalendar
import com.adhamelgendy.nixura.domain.time.DayCalculator
import com.adhamelgendy.nixura.domain.time.TimeZoneOffsetProvider
import com.adhamelgendy.nixura.presentation.strings.EnglishStrings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class HabitReminderPlannerTest {

    private val hour = 3_600_000L
    private val minute = 60_000L
    private val copy = HabitReminderCopyProvider { EnglishStrings }

    /** A Thursday (2026-01-15), so weekly tests have a known place in the Saturday-to-Friday week. */
    private val today = CivilCalendar.dayIndexFromCivil(2026, 1, 15)

    private fun days(offsetHours: Long = 0) = DayCalculator(TimeZoneOffsetProvider { offsetHours * hour })
    private fun planner(d: DayCalculator = days()) = HabitReminderPlanner(d, copy)
    private fun local(d: DayCalculator, day: Long, h: Int, m: Int = 0) = d.epochMillisOfLocal(day * DayCalculator.MILLIS_PER_DAY + h * hour + m * minute)

    private fun water(id: Long = 1, reminder: HabitReminderSettings = HabitReminderSettings()) =
        GoodHabit(id, "Drink water", 8, createdAtEpochMillis = 0, theme = HabitTheme.WATER, water = WaterGoal.fromCups(8), reminder = reminder)

    private fun prayer(id: Long = 2) = GoodHabit(id, "Pray", 5, createdAtEpochMillis = 0, theme = HabitTheme.PRAYER)

    private fun daily(id: Long, time: Int, target: Int = 1, enabled: Boolean? = true, theme: HabitTheme = HabitTheme.DEFAULT) =
        GoodHabit(id, "Habit $id", target, createdAtEpochMillis = 0, theme = theme, reminder = HabitReminderSettings(enabled = enabled, timeMinutes = time))

    private fun with(habit: GoodHabit, todayCount: Int = 0, vararg extra: Pair<Long, Int>) =
        GoodHabitWithLogs(habit, (listOf(today to todayCount) + extra).filter { it.second > 0 }.map { GoodHabitLog(habit.id, it.first, it.second) })

    private fun List<PlannedNotification>.on(d: DayCalculator, day: Long) = filter { d.dayIndexOf(it.triggerAtEpochMillis) == day }

    // ---- water ----

    @Test fun waterRemindsEveryTwoHoursOnlyWhileBehindPace() {
        val d = days()
        val plan = planner(d).plan(local(d, today, 8), "en", listOf(with(water(), todayCount = 3)), ReminderDeviceState())
        // 8 glasses over 09:00-21:00: due 1 by 11:00, 2 by 13:00, 4 by 15:00... 3 done: behind from 15:00 on.
        assertEquals(listOf(15, 17, 19, 21).map { local(d, today, it) }, plan.on(d, today).map { it.triggerAtEpochMillis })
        val first = plan.first()
        assertEquals(NotificationKind.GOOD_HABIT, first.kind)
        assertEquals("good:1", first.postKey) // one notification on screen per habit
        assertEquals(NotificationTiming.FLEXIBLE, first.timing)
        assertEquals(3, first.progress?.current)
        assertEquals(8, first.progress?.max)
        assertEquals(listOf(NotificationActionType.LOG_ONE, NotificationActionType.SNOOZE), first.actions.map { it.type })
        assertEquals("+1 glass · 250 ml", first.actions.first().label)
    }

    @Test fun waterStopsForTheDayOnceTheGoalIsMet() {
        val d = days()
        val plan = planner(d).plan(local(d, today, 8), "en", listOf(with(water(), todayCount = 8)), ReminderDeviceState())
        assertTrue(plan.on(d, today).isEmpty())
        assertEquals(6, plan.on(d, today + 1).size) // tomorrow starts from zero: every slot, at most 6
    }

    @Test fun theLastGlassHasItsOwnWords() {
        val d = days()
        val plan = planner(d).plan(local(d, today, 8), "en", listOf(with(water(), todayCount = 7)), ReminderDeviceState())
        val last = plan.on(d, today).single()
        assertEquals(local(d, today, 21), last.triggerAtEpochMillis)
        assertEquals(EnglishStrings.reminderWaterLastBody(7, 8), last.body)
    }

    @Test fun aCustomWaterWindowIsUsed() {
        val d = days()
        val habit = water(reminder = HabitReminderSettings(windowStartMinutes = 10 * 60, windowEndMinutes = 14 * 60))
        val plan = planner(d).plan(local(d, today, 8), "en", listOf(with(habit)), ReminderDeviceState())
        assertEquals(listOf(12, 14).map { local(d, today, it) }, plan.on(d, today).map { it.triggerAtEpochMillis })
    }

    // ---- prayer ----

    private val cairo = GeoPoint(30.0444, 31.2357)

    @Test fun prayerRemindsTenMinutesAfterEachUnmarkedPrayer() {
        val d = days(2) // Cairo, no DST in January
        val plan = planner(d).plan(local(d, today, 0), "en", listOf(with(prayer(), todayCount = 2)), ReminderDeviceState(location = cairo))
        val keys = plan.on(d, today).map { it.key.substringAfterLast(':') }
        assertEquals(listOf("p3", "p4", "p5"), keys) // Fajr and Dhuhr are marked
        val starts = PrayerTimeCalculator.forDay(today, cairo)
        assertEquals(starts[Prayer.ASR]!! + 10 * minute, plan.first().triggerAtEpochMillis)
        assertEquals(NotificationTiming.ON_TIME, plan.first().timing)
        val prayed = plan.first().actions.first()
        assertEquals(NotificationActionType.LOG_PRAYER, prayed.type)
        assertEquals(3, prayed.prayerIndex)
    }

    @Test fun fajrIsTheOnlyReminderAllowedInQuietHours() {
        val d = days(2)
        val plan = planner(d).plan(local(d, today, 0), "en", listOf(with(prayer())), ReminderDeviceState(location = cairo))
        val fajr = plan.on(d, today).first()
        assertTrue(fajr.key.endsWith("p1"))
        assertTrue(HabitReminderRules.isQuiet((d.millisOfDay(fajr.triggerAtEpochMillis) / minute).toInt()), "Fajr is before 08:00")
    }

    @Test fun ishaAfterTenAtNightIsDropped() {
        val d = days(1) // London summer time
        val june = CivilCalendar.dayIndexFromCivil(2026, 6, 21)
        val plan = planner(d).plan(local(d, june, 0), "en", listOf(GoodHabitWithLogs(prayer(), emptyList())), ReminderDeviceState(location = GeoPoint(51.5074, -0.1278)))
        val keys = plan.on(d, june).map { it.key.substringAfterLast(':') }
        assertTrue("p4" in keys, "Maghrib (~21:31) stays")
        assertTrue("p5" !in keys, "Isha is after 22:00 there in June")
    }

    @Test fun prayerNeedsASavedLocation() {
        val d = days(2)
        assertTrue(planner(d).plan(local(d, today, 0), "en", listOf(with(prayer())), ReminderDeviceState()).isEmpty())
    }

    // ---- daily / weekly ----

    @Test fun otherHabitsAreOffUntilTurnedOn() {
        val d = days()
        val habit = daily(3, time = 18 * 60, enabled = null)
        assertTrue(planner(d).plan(local(d, today, 8), "en", listOf(with(habit)), ReminderDeviceState()).isEmpty())
    }

    @Test fun aDailyHabitGetsItsTimeAndASecondOneThreeHoursLater() {
        val d = days()
        val plan = planner(d).plan(local(d, today, 8), "en", listOf(with(daily(3, time = 18 * 60, target = 3))), ReminderDeviceState())
        assertEquals(listOf(18, 21).map { local(d, today, it) }, plan.on(d, today).map { it.triggerAtEpochMillis })
        assertEquals(listOf(NotificationActionType.LOG_ONE, NotificationActionType.SNOOZE), plan.first().actions.map { it.type })
    }

    @Test fun aDoneHabitIsSkipped() {
        val d = days()
        val plan = planner(d).plan(local(d, today, 8), "en", listOf(with(daily(3, time = 18 * 60, target = 3), todayCount = 3)), ReminderDeviceState())
        assertTrue(plan.on(d, today).isEmpty())
    }

    @Test fun aChosenTimeInQuietHoursDoesNotRing() {
        val d = days()
        val plan = planner(d).plan(local(d, today, 8), "en", listOf(with(daily(3, time = 23 * 60))), ReminderDeviceState())
        assertTrue(plan.isEmpty())
    }

    @Test fun weeklyHabitRemindsOnlyWhileTheWeekIsReachable() {
        val d = days()
        val weekly = GoodHabit(4, "Gym", 1, createdAtEpochMillis = 0, weeklyTarget = 3, reminder = HabitReminderSettings(enabled = true, timeMinutes = 19 * 60))
        // Thursday, nothing done this week: Thursday + Friday cannot make 3.
        assertTrue(planner(d).plan(local(d, today, 8), "en", listOf(with(weekly)), ReminderDeviceState()).isEmpty())
        // One day done on Saturday: 1 + Thursday + Friday = 3, reachable today; tomorrow alone (1 + Friday) is not.
        val saturday = CivilCalendar.weekStartOf(today)
        val plan = planner(d).plan(local(d, today, 8), "en", listOf(with(weekly, 0, saturday to 1)), ReminderDeviceState())
        assertEquals(listOf(local(d, today, 19)), plan.map { it.triggerAtEpochMillis })
        assertEquals(EnglishStrings.reminderWeeklyBody("Gym", 1, 3), plan.single().body)
    }

    // ---- limits across habits ----

    @Test fun atMostEightADayAndNeverTwoWithinThirtyMinutes() {
        val d = days()
        // Ten habits, 40 minutes apart from 09:00: only the first 8 by time fit the daily cap.
        val habits = (0 until 10).map { i -> with(daily(10L + i, time = 9 * 60 + 40 * i)) }
        val todays = planner(d).plan(local(d, today, 8), "en", habits, ReminderDeviceState()).on(d, today)
        assertEquals(HabitReminderRules.MAX_PER_DAY, todays.size)
        val times = todays.map { it.triggerAtEpochMillis }.sorted()
        assertTrue(times.zipWithNext().all { (a, b) -> b - a >= 30 * minute })
    }

    @Test fun aChosenTimeWinsOverANearbyWaterSlot() {
        val d = days()
        val plan = planner(d).plan(local(d, today, 8), "en", listOf(with(water()), with(daily(3, time = 12 * 60 + 45))), ReminderDeviceState())
        val todays = plan.on(d, today).map { it.triggerAtEpochMillis }
        assertTrue(local(d, today, 12, 45) in todays)
        assertTrue(local(d, today, 13) !in todays, "13:00 water is 15 min away from the 12:45 reminder")
    }

    // ---- snooze and pause ----

    @Test fun snoozeShowsItAgainLaterEvenInQuietHours() {
        val d = days()
        val now = local(d, today, 10, 30)
        val snoozeAt = local(d, today, 22, 15)
        val plan = planner(d).plan(now, "en", listOf(with(water())), ReminderDeviceState(snoozes = mapOf(1L to snoozeAt)))
        assertEquals(listOf(snoozeAt), plan.on(d, today).map { it.triggerAtEpochMillis }) // the regular slots before it are gone
    }

    @Test fun aPausedHabitAndAPausedPhoneStaySilent() {
        val d = days()
        val now = local(d, today, 8)
        val untilTomorrow = d.startOfDayMillis(today + 1)
        val paused = water(reminder = HabitReminderSettings(pausedUntilEpochMillis = untilTomorrow))
        val plan = planner(d).plan(now, "en", listOf(with(paused)), ReminderDeviceState())
        assertTrue(plan.on(d, today).isEmpty())
        assertEquals(6, plan.on(d, today + 1).size)
        val phonePaused = planner(d).plan(now, "en", listOf(with(water())), ReminderDeviceState(pausedUntilEpochMillis = untilTomorrow))
        assertTrue(phonePaused.on(d, today).isEmpty())
    }

    @Test fun plansTodayAndTomorrowOnly() {
        val d = days()
        val plan = planner(d).plan(local(d, today, 8), "en", listOf(with(water())), ReminderDeviceState())
        assertEquals(setOf(today, today + 1), plan.map { d.dayIndexOf(it.triggerAtEpochMillis) }.toSet())
        assertEquals(plan.size, plan.map { it.key }.toSet().size) // every moment its own alarm key
    }

    // ---- time zones and DST ----

    @Test fun localTimesFollowTheTimeZone() {
        val cairoDays = days(2)
        val newYork = days(-5)
        val habit = listOf(with(daily(3, time = 19 * 60)))
        val inCairo = planner(cairoDays).plan(local(cairoDays, today, 8), "en", habit, ReminderDeviceState()).first()
        val inNewYork = planner(newYork).plan(local(newYork, today, 8), "en", habit, ReminderDeviceState()).first()
        assertEquals(19, (cairoDays.millisOfDay(inCairo.triggerAtEpochMillis) / hour).toInt())
        assertEquals(19, (newYork.millisOfDay(inNewYork.triggerAtEpochMillis) / hour).toInt())
        assertEquals(7 * hour, inNewYork.triggerAtEpochMillis - inCairo.triggerAtEpochMillis)
    }

    @Test fun waterSlotsKeepTheirLocalTimeOnADstDay() {
        // Egypt moves its clocks from UTC+2 to UTC+3 at local midnight on the DST day.
        val dstDay = CivilCalendar.dayIndexFromCivil(2026, 4, 24)
        val switchAt = dstDay * DayCalculator.MILLIS_PER_DAY - 2 * hour
        val d = DayCalculator(TimeZoneOffsetProvider { at -> if (at >= switchAt) 3 * hour else 2 * hour })
        val plan = planner(d).plan(switchAt - 2 * hour, "en", listOf(GoodHabitWithLogs(water(), emptyList())), ReminderDeviceState())
        val onDstDay = plan.filter { d.dayIndexOf(it.triggerAtEpochMillis) == dstDay }
        assertEquals(listOf(11, 13, 15, 17, 19, 21), onDstDay.map { (d.millisOfDay(it.triggerAtEpochMillis) / hour).toInt() })
        // 11:00 at UTC+3 is 08:00 UTC: computed with the new offset, not "yesterday's 11:00 + 24 h".
        assertEquals(dstDay * DayCalculator.MILLIS_PER_DAY + 8 * hour, onDstDay.first().triggerAtEpochMillis)
    }
}
