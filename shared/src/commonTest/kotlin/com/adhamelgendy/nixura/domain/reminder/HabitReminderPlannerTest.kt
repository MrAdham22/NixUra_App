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
import kotlin.math.abs
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

    @Test fun fajrRemindsBeforeDawn() {
        val d = days(2)
        val fajr = planner(d).plan(local(d, today, 0), "en", listOf(with(prayer())), ReminderDeviceState(location = cairo)).on(d, today).first()
        assertTrue(fajr.key.endsWith("p1"))
        assertTrue(d.millisOfDay(fajr.triggerAtEpochMillis) < 8 * hour)
    }

    @Test fun ishaAfterTenAtNightStillRings() {
        val d = days(1) // London summer time
        val june = CivilCalendar.dayIndexFromCivil(2026, 6, 21)
        val plan = planner(d).plan(local(d, june, 0), "en", listOf(GoodHabitWithLogs(prayer(), emptyList())), ReminderDeviceState(location = GeoPoint(51.5074, -0.1278)))
        assertTrue(plan.any { it.key == "good:2:$june:p5" }, "there are no quiet hours")
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

    @Test fun aChosenTimeRingsAtAnyHour() {
        val d = days()
        for (h in listOf(0, 3, 7, 22, 23)) {
            val plan = planner(d).plan(local(d, today, 0) - minute, "en", listOf(GoodHabitWithLogs(daily(3, time = h * 60), emptyList())), ReminderDeviceState())
            assertTrue(local(d, today, h) in plan.map { it.triggerAtEpochMillis }, "$h:00")
        }
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

    // ---- chosen times at night ----

    /** A flexible alarm is held by deep sleep and then dropped when 45 min late: a time the user picked must not be. */
    @Test fun chosenTimesAndTheirSnoozesRingOnTime() {
        val d = days()
        val weekly = GoodHabit(4, "Gym", 1, createdAtEpochMillis = 0, weeklyTarget = 1, reminder = HabitReminderSettings(enabled = true, timeMinutes = 6 * 60 + 30))
        val habits = listOf(with(daily(3, time = 50, target = 2)), GoodHabitWithLogs(weekly, emptyList()))
        val plan = planner(d).plan(local(d, today, 0), "en", habits, ReminderDeviceState(snoozes = mapOf(3L to local(d, today, 2))))
        assertTrue(plan.any { it.key.contains(":s") }, "the snooze is planned")
        assertEquals(setOf(3L, 4L), plan.mapNotNull { it.targetId }.toSet())
        assertTrue(plan.all { it.timing == NotificationTiming.ON_TIME })
    }

    @Test fun theSecondReminderMayPassMidnight() {
        val d = days()
        val plan = planner(d).plan(local(d, today, 20), "en", listOf(with(daily(3, time = 22 * 60 + 50, target = 2))), ReminderDeviceState())
        val expected = listOf(local(d, today, 22, 50), local(d, today + 1, 1, 50))
        assertEquals(expected, plan.filter { it.key.startsWith("good:3:$today:") }.map { it.triggerAtEpochMillis })
    }

    @Test fun theSecondReminderAfterMidnightCountsTheEvening() {
        val d = days()
        val now = local(d, today, 0, 30)
        val habit = daily(3, time = 22 * 60 + 50, target = 2)
        fun second(evening: Int, night: Int) = planner(d)
            .plan(now, "en", listOf(with(habit, night, today - 1 to evening)), ReminderDeviceState())
            .filter { it.key == "good:3:${today - 1}:t2" }
        val one = second(evening = 1, night = 0).single()
        assertEquals(local(d, today, 1, 50), one.triggerAtEpochMillis)
        assertEquals(1, one.progress?.current)
        assertTrue(second(evening = 2, night = 0).isEmpty(), "done in the evening")
        assertTrue(second(evening = 1, night = 1).isEmpty(), "finished after midnight")
    }

    @Test fun aNightSnoozeRemembersTheEvening() {
        val d = days()
        val at = local(d, today, 2, 50) // snoozed from 01:50
        val habit = daily(3, time = 22 * 60 + 50, target = 2)
        val plan = planner(d).plan(local(d, today, 1, 51), "en", listOf(with(habit, 0, today - 1 to 1)), ReminderDeviceState(snoozes = mapOf(3L to at)))
        assertEquals(1, plan.single { it.triggerAtEpochMillis == at }.progress?.current)
    }

    // ---- habits next to each other ----

    private fun List<PlannedNotification>.waterSlots(day: Long, id: Long = 1) = filter { it.key.startsWith("good:$id:$day:w") }

    private fun timesOf(vararg items: PlannedNotification) = items.map { it.triggerAtEpochMillis }

    /** The owner's day on 2026-11-22: five prayers, a workout at 15:30 (3 a day) and one glass of eight. */
    @Test fun waterKeepsComingNextToFivePrayersAndAWorkout() {
        val d = days(2)
        val november = CivilCalendar.dayIndexFromCivil(2026, 11, 22)
        val habits = listOf(
            GoodHabitWithLogs(water(), listOf(GoodHabitLog(1, november, 1))),
            GoodHabitWithLogs(prayer(), emptyList()),
            GoodHabitWithLogs(daily(3, time = 15 * 60 + 30, target = 3), emptyList()),
        )
        val plan = planner(d).plan(local(d, november, 0, 1), "en", habits, ReminderDeviceState(location = cairo))
        val water = plan.waterSlots(november)
        assertEquals(listOf("w780", "w900", "w1020", "w1140", "w1260"), water.map { it.key.substringAfterLast(':') }, "every slot still behind pace")
        assertEquals(6, plan.waterSlots(november + 1).size, "tomorrow has all six")
        val todays = plan.on(d, november)
        assertEquals(5, todays.count { it.targetId == 2L }, "every prayer")
        assertEquals(2, todays.count { it.targetId == 3L }, "both workout reminders")
        for (w in water) {
            assertTrue(w.triggerAtEpochMillis <= local(d, november, 21), "inside the window: ${w.key}")
            for (other in todays.filter { it !== w }) {
                assertTrue(abs(w.triggerAtEpochMillis - other.triggerAtEpochMillis) >= 30 * minute, "${w.key} next to ${other.key}")
            }
        }
    }

    @Test fun thereIsNoSharedDailyLimit() {
        val d = days()
        val habits = (0 until 12).map { i -> with(daily(10L + i, time = 9 * 60 + 40 * i)) }
        assertEquals(12, planner(d).plan(local(d, today, 8), "en", habits, ReminderDeviceState()).on(d, today).size)
    }

    @Test fun chosenTimesAreNeitherMovedNorDropped() {
        val d = days()
        val habits = listOf(with(daily(3, time = 18 * 60)), with(daily(4, time = 18 * 60 + 5)))
        val todays = planner(d).plan(local(d, today, 8), "en", habits, ReminderDeviceState()).on(d, today)
        assertEquals(listOf(local(d, today, 18), local(d, today, 18, 5)), todays.map { it.triggerAtEpochMillis })
    }

    @Test fun waterMovesAfterANearbyChosenTime() {
        val d = days()
        val todays = planner(d).plan(local(d, today, 8), "en", listOf(with(water()), with(daily(3, time = 12 * 60 + 45))), ReminderDeviceState()).on(d, today)
        val times = timesOf(*todays.toTypedArray())
        assertTrue(local(d, today, 12, 45) in times)
        assertTrue(local(d, today, 13, 15) in times, "13:00 water moves to 30 min after the 12:45 reminder")
        assertEquals(6, todays.waterSlots(today).size)
    }

    @Test fun theLastWaterMovesBackInsideTheWindow() {
        val d = days()
        val plan = planner(d).plan(local(d, today, 8), "en", listOf(with(water()), with(daily(3, time = 20 * 60 + 50))), ReminderDeviceState())
        assertEquals(local(d, today, 20, 20), plan.waterSlots(today).last().triggerAtEpochMillis)
    }

    /** If logging the other habit let water jump back to a time already past, that reminder would be lost. */
    @Test fun loggingTheOtherHabitDoesNotMoveWaterBackIntoThePast() {
        val d = days()
        val other = daily(3, time = 16 * 60 + 55)
        fun at1700(count: Int) = planner(d).plan(local(d, today, 8), "en", listOf(with(water()), with(other, count)), ReminderDeviceState())
            .single { it.key == "good:1:$today:w1020" }.triggerAtEpochMillis
        assertEquals(local(d, today, 17, 25), at1700(0))
        assertEquals(at1700(0), at1700(1))
    }

    // ---- water past midnight ----

    private fun lateWater() = water(reminder = HabitReminderSettings(windowStartMinutes = 14 * 60, windowEndMinutes = 2 * 60))

    @Test fun aWindowPastMidnightRemindsUntilItsEnd() {
        val d = days()
        val plan = planner(d).plan(local(d, today, 13), "en", listOf(with(lateWater())), ReminderDeviceState())
        val expected = listOf(16, 18, 20, 22).map { local(d, today, it) } + listOf(0, 2).map { local(d, today + 1, it) }
        assertEquals(expected, plan.waterSlots(today).map { it.triggerAtEpochMillis })
    }

    @Test fun afterMidnightTheEveningsGlassesStillCount() {
        val d = days()
        val afterMidnight = local(d, today, 0, 30)
        fun plan(evening: Int, night: Int) =
            planner(d).plan(afterMidnight, "en", listOf(with(lateWater(), night, today - 1 to evening)), ReminderDeviceState()).waterSlots(today - 1)
                .filter { it.triggerAtEpochMillis > afterMidnight }
        val two = plan(evening = 5, night = 1).single()
        assertEquals(local(d, today, 2), two.triggerAtEpochMillis)
        assertEquals(6, two.progress?.current)
        assertTrue(plan(evening = 7, night = 1).isEmpty(), "7 in the evening and 1 after midnight meet the goal")
    }

    @Test fun aLongWindowSpreadsItsSixRemindersToTheEnd() {
        val d = days()
        val habit = water(reminder = HabitReminderSettings(windowStartMinutes = 8 * 60, windowEndMinutes = 2 * 60)) // 18 h
        val slots = planner(d).plan(local(d, today, 7), "en", listOf(with(habit)), ReminderDeviceState()).waterSlots(today)
        val expected = listOf(11, 14, 17, 20, 23).map { local(d, today, it) } + local(d, today + 1, 2)
        assertEquals(expected, slots.map { it.triggerAtEpochMillis })
    }

    @Test fun aSnoozeAcrossMidnightHoldsBackTheSlotsBeforeIt() {
        val d = days()
        val snoozeAt = local(d, today + 1, 0, 20)
        val plan = planner(d).plan(local(d, today, 23, 20), "en", listOf(with(lateWater())), ReminderDeviceState(snoozes = mapOf(1L to snoozeAt)))
        assertEquals(snoozeAt, plan.first().triggerAtEpochMillis)
        assertTrue(plan.none { it.triggerAtEpochMillis == local(d, today + 1, 0) })
    }

    // ---- snooze and pause ----

    @Test fun snoozeShowsItAgainLaterEvenLateAtNight() {
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
