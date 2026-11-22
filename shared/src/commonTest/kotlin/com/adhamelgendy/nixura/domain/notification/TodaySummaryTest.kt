package com.adhamelgendy.nixura.domain.notification

import com.adhamelgendy.nixura.domain.coach.HabitKnowledgeBase
import com.adhamelgendy.nixura.domain.model.AppSettings
import com.adhamelgendy.nixura.domain.model.DigitStyle
import com.adhamelgendy.nixura.domain.model.Event
import com.adhamelgendy.nixura.domain.model.EventCategory
import com.adhamelgendy.nixura.domain.model.GoodHabit
import com.adhamelgendy.nixura.domain.model.GoodHabitLog
import com.adhamelgendy.nixura.domain.model.GoodHabitWithLogs
import com.adhamelgendy.nixura.domain.model.Habit
import com.adhamelgendy.nixura.domain.model.HabitTracking
import com.adhamelgendy.nixura.domain.model.TrackingSnapshot
import com.adhamelgendy.nixura.domain.recognition.HabitTopicDetector
import com.adhamelgendy.nixura.domain.reminder.GeoPoint
import com.adhamelgendy.nixura.domain.reminder.HabitReminderCopyProvider
import com.adhamelgendy.nixura.domain.reminder.HabitReminderPlanner
import com.adhamelgendy.nixura.domain.reminder.HabitReminderSettings
import com.adhamelgendy.nixura.domain.reminder.Prayer
import com.adhamelgendy.nixura.domain.reminder.PrayerTimeCalculator
import com.adhamelgendy.nixura.domain.reminder.ReminderDeviceState
import com.adhamelgendy.nixura.domain.theme.HabitTheme
import com.adhamelgendy.nixura.domain.theme.WaterGoal
import com.adhamelgendy.nixura.domain.time.CivilCalendar
import com.adhamelgendy.nixura.domain.time.DayCalculator
import com.adhamelgendy.nixura.domain.time.TimeZoneOffsetProvider
import com.adhamelgendy.nixura.presentation.strings.stringsFor
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** D-lock-screen section 4, item 2: what the opt-in "Today" notification says, and what it never says. */
class TodaySummaryTest {
    private val hour = 3_600_000L
    private val minute = 60_000L
    private val day = 24 * hour
    private val days = DayCalculator(TimeZoneOffsetProvider { 2 * hour }) // Cairo in winter
    private val cairo = GeoPoint(30.0444, 31.2357)
    private val today = CivilCalendar.dayIndexFromCivil(2026, 11, 22)
    private val builder = TodaySummaryBuilder(days) { stringsFor(it) }
    private val reminders = HabitReminderPlanner(days, HabitReminderCopyProvider { stringsFor(it) }, HabitTopicDetector(HabitKnowledgeBase()))
    private val on = AppSettings(todayNotification = true)

    private fun at(h: Int, m: Int = 0, dayIndex: Long = today) = days.epochMillisOfLocal(dayIndex * day + h * hour + m * minute)

    private fun water(count: Int = 0) = logged(
        GoodHabit(1, "Drink water", 8, createdAtEpochMillis = 0, theme = HabitTheme.WATER, water = WaterGoal.fromCups(8)), count,
    )

    private fun prayer(count: Int = 0) = logged(GoodHabit(2, "Pray", 5, createdAtEpochMillis = 0, theme = HabitTheme.PRAYER), count)

    private fun reading(count: Int = 0) = logged(
        GoodHabit(3, "Reading", 1, createdAtEpochMillis = 0, reminder = HabitReminderSettings(enabled = true, timeMinutes = 21 * 60)), count,
    )

    private fun logged(habit: GoodHabit, count: Int) =
        GoodHabitWithLogs(habit, if (count > 0) listOf(GoodHabitLog(habit.id, today, count)) else emptyList())

    private fun quit(name: String, ageDays: Long, now: Long, hidden: Boolean = false) = HabitTracking(
        Habit(7, name, 0, hideNameOutside = hidden), emptyList(),
        TrackingSnapshot(now - ageDays * day - hour, ageDays * day + hour, ageDays * day + hour),
    )

    private fun build(
        now: Long,
        good: List<GoodHabitWithLogs> = emptyList(),
        bad: List<HabitTracking> = emptyList(),
        events: List<Event> = emptyList(),
        settings: AppSettings = on,
        device: ReminderDeviceState = ReminderDeviceState(location = cairo),
        language: String = "en",
    ): TodayState = builder.build(now, language, settings, good, bad, events, reminders.plan(now, language, good, device), device)

    private fun summary(state: TodayState) = assertIs<TodayState.Showing>(state).summary

    // ---- opt-in, hidden for the day ----

    @Test fun itIsOffUntilTheUserTurnsItOn() {
        assertFalse(AppSettings().todayNotification)
        assertFalse(AppSettings().todayShowStreak)
        assertEquals(TodayState.Off, build(at(8), listOf(water()), settings = AppSettings()))
    }

    @Test fun theAppWideSwitchTurnsItOffToo() {
        assertEquals(TodayState.Off, build(at(8), listOf(water()), settings = on.copy(notificationsEnabled = false)))
    }

    @Test fun nothingToSayMeansNoNotification() {
        assertEquals(TodayState.Off, build(at(8)))
    }

    @Test fun dismissedItStaysAwayForTheDayAndComesBackTomorrow() {
        val device = ReminderDeviceState(location = cairo, todayHiddenDay = today)
        assertEquals(TodayState.Hidden(days.startOfDayMillis(today + 1)), build(at(10), listOf(water()), device = device))
        assertIs<TodayState.Hidden>(build(at(23, 59), listOf(water()), device = device))
        assertIs<TodayState.Showing>(build(at(0, 1, today + 1), listOf(water()), device = device))
    }

    // ---- what it says ----

    @Test fun itShowsTodaysProgress() {
        val s = summary(build(at(10), listOf(water(3), prayer(2), reading(1))))
        assertEquals("Today: 1 of 3 done", s.title)
        assertTrue(s.text.startsWith("💧 3 of 8 · 🕌 2 of 5"), s.text)
        assertTrue("Drink water: 3 of 8" in s.lines, s.lines.toString())
        assertTrue("Reading: 1 of 1" in s.lines, s.lines.toString())
    }

    @Test fun manyHabitsStayFewLines() {
        val many = (10L..19L).map { logged(GoodHabit(it, "Habit $it", 1, createdAtEpochMillis = 0), 0) }
        val lines = summary(build(at(10), many)).lines
        assertTrue(lines.size <= 8, lines.toString())
        assertTrue("+6 more" in lines, lines.toString())
    }

    @Test fun theNextReminderHasACountdownTheSystemDraws() {
        val s = summary(build(at(8), listOf(reading())))
        assertEquals(at(21), s.countdown?.atEpochMillis)
        assertEquals("Reading", s.countdown?.label)
        assertTrue("Next: Reading 9:00 PM" in s.text, s.text)
    }

    @Test fun aPrayerIsNamedByItsOwnName() {
        val asr = PrayerTimeCalculator.forDay(today, cairo)[Prayer.ASR]!!
        val s = summary(build(asr - minute, listOf(prayer(2))))
        assertEquals("Asr", s.countdown?.label)
    }

    @Test fun aReminderWhoseTimePassedIsNotNext() {
        val s = summary(build(at(21, 5), listOf(reading())))
        assertEquals(at(21, 0, today + 1), s.countdown?.atEpochMillis)
    }

    @Test fun theNearestEventIsTimingOnly() {
        fun event(target: Long, allDay: Boolean = false) = Event(1, "Secret dinner", EventCategory.PERSONAL, target, emptyList(), 0, allDay = allDay)
        fun text(target: Long, allDay: Boolean = false, language: String = "en") =
            summary(build(at(10), events = listOf(event(target, allDay)), language = language)).text
        assertEquals("Event today, 3:30 PM", text(at(15, 30)))
        assertEquals("Event today", text(days.startOfDayMillis(today), allDay = true))
        assertEquals("Event tomorrow", text(at(9, 0, today + 1)))
        assertEquals("Event in 2 days", text(at(23, 0, today + 2)))
        assertEquals("مناسبة بعد يومين", text(at(9, 0, today + 2), language = "ar"))
        assertEquals("مناسبة بعد 3 أيام", text(at(9, 0, today + 3), language = "ar"))
        assertEquals("مناسبة بعد 11 يومًا", text(at(9, 0, today + 11), language = "ar"))
    }

    @Test fun aPastEventIsNotMentioned() {
        val past = Event(1, "Secret dinner", EventCategory.PERSONAL, at(9), emptyList(), 0)
        assertEquals(TodayState.Off, build(at(10), events = listOf(past)))
    }

    // ---- privacy ----

    @Test fun noQuitInformationWithoutTheOptIn() {
        val now = at(10)
        val with = build(now, listOf(water()), listOf(quit("Gambling", 12, now)))
        assertEquals(build(now, listOf(water())), with)
    }

    @Test fun theOptInAddsTheLongestStreakWithoutAName() {
        val now = at(10)
        val settings = on.copy(todayShowStreak = true)
        val s = summary(build(now, bad = listOf(quit("Smoking", 3, now), quit("Gambling", 12, now)), settings = settings))
        assertEquals("Quit streak: 12 days", s.text)
        assertEquals("Quit streak: 1 day", summary(build(now, bad = listOf(quit("Smoking", 1, now)), settings = settings)).text)
        assertEquals("Quit streak: under a day", summary(build(now, bad = listOf(quit("Smoking", 0, now)), settings = settings)).text)
        assertEquals("سلسلة الإقلاع: يومان", summary(build(now, bad = listOf(quit("Smoking", 2, now)), settings = settings, language = "ar")).text)
    }

    @Test fun neverAnEventOrAQuitHabitsName() {
        val now = at(10)
        val event = Event(1, "Secret dinner", EventCategory.PERSONAL, at(15), listOf(hour), 0)
        for (language in listOf("en", "ar")) for (streak in listOf(false, true)) for (hidden in listOf(false, true)) {
            val state = build(
                now, listOf(water(2), prayer(1)), listOf(quit("Gambling", 12, now, hidden)), listOf(event),
                on.copy(todayShowStreak = streak), language = language,
            )
            for (secret in listOf("Secret", "dinner", "Gambling", "القمار")) assertFalse(secret in state.toString(), "$language: $state")
        }
    }

    @Test fun itIsWrittenInArabicToo() {
        val s = summary(build(at(8), listOf(water(3), reading()), language = "ar"))
        assertEquals("اليوم: أنجزت 0 من 2", s.title)
        assertTrue("💧 3 من 8" in s.text, s.text)
        assertEquals("أخفِ لليوم", s.hideLabel)
    }

    @Test fun itFollowsTheDigitSetting() {
        val s = summary(build(at(8), listOf(water()), settings = on.copy(digitStyle = DigitStyle.ARABIC_INDIC)))
        assertEquals(DigitStyle.ARABIC_INDIC, s.digitStyle)
    }

    // ---- the button for what is next ----

    @Test fun waterOffersAGlassUntilTheGoalIsMet() {
        val action = summary(build(at(10), listOf(water(3)))).action
        assertEquals(NotificationActionType.LOG_ONE, action?.type)
        assertEquals("+1 glass · 250 ml", action?.label)
        assertNull(summary(build(at(10), listOf(water(8)))).action)
    }

    @Test fun aPrayerThatHasNotStartedCannotBeMarked() {
        val asr = PrayerTimeCalculator.forDay(today, cairo)[Prayer.ASR]!!
        assertNull(summary(build(asr - minute, listOf(prayer(2)))).action)
        val action = summary(build(asr + minute, listOf(prayer(2)))).action
        assertEquals(NotificationActionType.LOG_PRAYER, action?.type)
        assertEquals(Prayer.ASR.index, action?.prayerIndex)
        assertEquals("Asr prayed ✓", action?.label)
    }

    @Test fun theFirstUnmarkedPrayerIsOfferedByName() {
        // At 4 PM Asr has started, but Dhuhr is the one not marked: "Prayed" must not mark a prayer it does not name.
        val action = summary(build(at(16), listOf(prayer(1)))).action
        assertEquals(Prayer.DHUHR.index, action?.prayerIndex)
        assertEquals("Dhuhr prayed ✓", action?.label)
    }

    @Test fun theButtonFollowsWhatIsNext() {
        val asr = PrayerTimeCalculator.forDay(today, cairo)[Prayer.ASR]!!
        val s = summary(build(asr + minute, listOf(water(0), prayer(2)))) // next: the Asr reminder, 10 min after it starts
        assertEquals(2L, s.action?.habitId)
    }

    // ---- when it is drawn again ----

    @Test fun itIsRefreshedAtMidnight() {
        assertEquals(days.startOfDayMillis(today + 1), summary(build(at(22), listOf(logged(GoodHabit(9, "Stretch", 1, createdAtEpochMillis = 0), 0)))).refreshAtEpochMillis)
    }

    @Test fun itIsRefreshedWhenSomethingItShowsStopsBeingTrue() {
        assertEquals(at(21) + minute, summary(build(at(20), listOf(reading()))).refreshAtEpochMillis) // the countdown ran out
        val asr = PrayerTimeCalculator.forDay(today, cairo)[Prayer.ASR]!!
        assertEquals(asr, summary(build(asr - minute, listOf(prayer(2)))).refreshAtEpochMillis) // the button appears
        val event = Event(1, "Dinner", EventCategory.PERSONAL, at(15), emptyList(), 0)
        assertEquals(at(15), summary(build(at(10), events = listOf(event))).refreshAtEpochMillis)
        val now = at(10)
        val streak = summary(build(now, bad = listOf(quit("Smoking", 12, now).let { it.copy(tracking = it.tracking.copy(activePeriodStartEpochMillis = now - 13 * day + hour)) }), settings = on.copy(todayShowStreak = true)))
        assertEquals(now + hour, streak.refreshAtEpochMillis)
    }

    @Test fun theSameDataGivesTheSameNotificationSoItIsNotPostedAgain() {
        val habits = listOf(water(3), reading())
        assertEquals(build(at(20, 1), habits), build(at(20, 2), habits))
    }
}
