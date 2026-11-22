package com.adhamelgendy.nixura.presentation.strings

import com.adhamelgendy.nixura.domain.model.EventCategory
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * Every plain text in [Strings] has an Arabic version: a getter that returns the same text in Arabic and English is
 * either a brand/product name (listed below) or a string someone forgot to translate. JVM-only (uses reflection).
 */
class ArabicCoverageTest {

    /** Same in both languages on purpose: brand and product names, symbols, the digit samples. */
    private val sameOnPurpose = setOf(
        "getLandingAuraWord", "getNotion", "getSourceGoogle", "getSourceNotion", "getDigitsWestern", "getDigitsArabic",
        "getTimelineEarlierCount",
    )

    @Test
    fun everyPlainStringIsTranslated() {
        val untranslated = Strings::class.java.methods
            .filter { it.parameterCount == 0 && it.name.startsWith("get") && it.name !in sameOnPurpose }
            .filter { it.returnType == String::class.java || it.returnType == List::class.java }
            .filter { m ->
                val en = m.invoke(EnglishStrings)
                val ar = m.invoke(ArabicStrings)
                en == ar && en != ""
            }
            .map { it.name.removePrefix("get").replaceFirstChar(Char::lowercase) }
        if (untranslated.isNotEmpty()) fail("Not translated into Arabic: ${untranslated.sorted()}")
    }

    @Test
    fun lambdasAndFunctionsAreTranslated() {
        val a = ArabicStrings
        val e = EnglishStrings
        val samples = listOf(
            a.daysCount(3) to e.daysCount(3),
            a.streakDays(11) to e.streakDays(11),
            a.categoryLabel(EventCategory.TRAVEL) to e.categoryLabel(EventCategory.TRAVEL),
            a.milestoneName(14) to e.milestoneName(14),
            a.voiceAdd(2) to e.voiceAdd(2),
            a.coachModeAiLeft(5) to e.coachModeAiLeft(5),
            a.errorText("Habit name cannot be empty") to e.errorText("Habit name cannot be empty"),
        ) + reminderSamples(a, e)
        samples.forEach { (ar, en) -> assertTrue(ar != en, "still English: $en") }
    }

    /** Phase C: every reminder text the notifications use (they are functions, so the getter scan misses them). */
    private fun reminderSamples(a: Strings, e: Strings): List<Pair<String, String>> {
        val p = com.adhamelgendy.nixura.domain.reminder.Prayer.ASR
        val t = com.adhamelgendy.nixura.domain.theme.HabitTheme.entries
        return buildList {
            com.adhamelgendy.nixura.domain.reminder.Prayer.entries.forEach {
                add(a.prayerName(it) to e.prayerName(it))
                add(a.reminderPrayerTitle(it) to e.reminderPrayerTitle(it))
            }
            add(a.reminderPrayerBody(p, 0) to e.reminderPrayerBody(p, 0))
            add(a.reminderPrayerBody(p, 2) to e.reminderPrayerBody(p, 2))
            (0..2).forEach { add(a.reminderWaterTitle(it) to e.reminderWaterTitle(it)) }
            add(a.reminderWaterBody(3, 8) to e.reminderWaterBody(3, 8))
            add(a.reminderWaterLastBody(7, 8) to e.reminderWaterLastBody(7, 8))
            // Water and prayer titles are the emoji and the habit's own name in both languages, on purpose.
            add(a.lockScreenEvent(null) to e.lockScreenEvent(null))
            add(a.lockScreenEvent("2") to e.lockScreenEvent("2"))
            add(a.lockScreenMilestone() to e.lockScreenMilestone())
            add(a.lockScreenCoach() to e.lockScreenCoach())
            add(a.hideNameSheetRow(true) to e.hideNameSheetRow(true))
            add(a.hideNameSheetRow(false) to e.hideNameSheetRow(false))
            add(a.todayTitle(1, 3) to e.todayTitle(1, 3))
            add(a.todayTitleNoHabits() to e.todayTitleNoHabits())
            add(a.todayWater(1, 8) to e.todayWater(1, 8))
            add(a.todayPrayers(1, 5) to e.todayPrayers(1, 5))
            add(a.todayHabit("Gym", 1, 2) to e.todayHabit("Gym", 1, 2))
            add(a.todayMore(3) to e.todayMore(3))
            add(a.todayNext("Asr", 900) to e.todayNext("Asr", 900))
            add(a.todayEvent(0, 900) to e.todayEvent(0, 900))
            add(a.todayEvent(0, null) to e.todayEvent(0, null))
            add(a.todayEvent(1, null) to e.todayEvent(1, null))
            add(a.todayEvent(4, null) to e.todayEvent(4, null))
            add(a.todayStreak(0) to e.todayStreak(0))
            add(a.todayStreak(4) to e.todayStreak(4))
            add(a.todayMetricDone() to e.todayMetricDone())
            add(a.todayMetricNext("Asr") to e.todayMetricNext("Asr"))
            add(a.todayHide() to e.todayHide())
            add(a.todayActionPrayed(p) to e.todayActionPrayed(p))
            add(a.todaySheetRow(true) to e.todaySheetRow(true))
            t.filter { it != com.adhamelgendy.nixura.domain.theme.HabitTheme.WATER && it != com.adhamelgendy.nixura.domain.theme.HabitTheme.PRAYER }
                .forEach { theme -> add(a.reminderHabitTitle(theme, "Gym", 0) to e.reminderHabitTitle(theme, "Gym", 0)) }
            add(a.reminderDailyBody("Gym", 1, 3) to e.reminderDailyBody("Gym", 1, 3))
            add(a.reminderDailyBody("Gym", 0, 1) to e.reminderDailyBody("Gym", 0, 1))
            add(a.reminderWeeklyBody("Gym", 1, 3) to e.reminderWeeklyBody("Gym", 1, 3))
            add(a.reminderActionGlass(250) to e.reminderActionGlass(250))
            add(a.reminderActionPrayed(p) to e.reminderActionPrayed(p))
            add(a.reminderActionDone() to e.reminderActionDone())
            add(a.reminderActionSnooze(15) to e.reminderActionSnooze(15))
            add(a.reminderActionSnooze(60) to e.reminderActionSnooze(60))
            add(a.reminderLoggedWater(5, 8) to e.reminderLoggedWater(5, 8))
            add(a.reminderWaterGoalDone() to e.reminderWaterGoalDone())
            add(a.reminderLoggedPrayer(p, 3) to e.reminderLoggedPrayer(p, 3))
            add(a.reminderLoggedHabit("Gym", 1, 3) to e.reminderLoggedHabit("Gym", 1, 3))
            add(a.reminderLoggedHabit("Gym", 3, 3) to e.reminderLoggedHabit("Gym", 3, 3))
            add(a.reminderSnoozed(60) to e.reminderSnoozed(60))
            add(a.reminderSheetRow("x") to e.reminderSheetRow("x"))
            add(a.reminderStatusAt("7:00") to e.reminderStatusAt("7:00"))
            add(a.reminderStatusPausedUntil("Sat") to e.reminderStatusPausedUntil("Sat"))
        }
    }

    @Test
    fun arabicSnoozeAndPrayerCountsReadNaturally() {
        val a = ArabicStrings
        assertTrue(a.reminderActionSnooze(60) == "أجّل ساعة")
        assertTrue(a.reminderActionSnooze(15) == "أجّل 15 دقيقة")
        assertTrue(a.reminderPrayerBody(com.adhamelgendy.nixura.domain.reminder.Prayer.ASR, 2).startsWith("سجّلت صلاتين"))
    }

    @Test
    fun arabicCountsUseThePluralForms() {
        val a = ArabicStrings
        assertTrue(a.daysCount(1) == "يوم واحد")
        assertTrue(a.daysCount(2) == "يومان")
        assertTrue(a.daysCount(3) == "3 أيام")
        assertTrue(a.daysCount(11) == "11 يومًا")
        assertTrue(a.daysCount(100) == "100 يوم")
        assertTrue(a.milestoneName(7) == "أسبوع")
        assertTrue(a.milestoneName(60) == "شهران")
    }

    @Test
    fun arabicDatesReadInArabicOrder() {
        val a = ArabicStrings
        assertTrue(a.dateText("الثلاثاء", 20, "أكتوبر", 2026) == "الثلاثاء، 20 أكتوبر 2026")
        assertTrue(a.clockText(8, "30", a.pm) == "8:30 م")
        assertTrue(EnglishStrings.dateText("Tue", 20, "Oct", null) == "Tue, 20 Oct")
    }
}
