package com.adhamelgendy.nixura.domain.reminder

import com.adhamelgendy.nixura.domain.theme.HabitTheme

/**
 * The words of good-habit reminders, in one language. Implemented by the app's `Strings` / `ArabicStrings`
 * (presentation), so every text lives with the rest of the UI text and is checked by the Arabic coverage test.
 * Tone (PLAN.md): warm, short, specific; never guilt ("don't break your streak", "you missed").
 *
 * [variant] is a small number that changes from one reminder to the next, so the wording is not the same every time.
 */
interface HabitReminderCopy {
    fun reminderPrayerTitle(prayer: Prayer): String
    /** [marked] = prayers marked today so far. */
    fun reminderPrayerBody(prayer: Prayer, marked: Int): String

    fun reminderWaterTitle(variant: Int): String
    fun reminderWaterBody(count: Int, target: Int): String
    /** One glass left for today's goal. */
    fun reminderWaterLastBody(count: Int, target: Int): String

    /** A daily or weekly habit with a chosen time: the title follows the habit's theme (reading, walk...). */
    fun reminderHabitTitle(theme: HabitTheme, name: String, variant: Int): String
    fun reminderDailyBody(name: String, count: Int, target: Int): String
    fun reminderWeeklyBody(name: String, doneDays: Int, goalDays: Int): String

    /** Button labels. */
    fun reminderActionGlass(servingMl: Int): String
    fun reminderActionPrayed(prayer: Prayer): String
    fun reminderActionDone(): String
    fun reminderActionSnooze(minutes: Int): String

    /** The notification right after a button was used (updated in place, then it goes away). */
    fun reminderLoggedWater(count: Int, target: Int): String
    fun reminderWaterGoalDone(): String
    fun reminderLoggedPrayer(prayer: Prayer, marked: Int): String
    fun reminderLoggedHabit(name: String, count: Int, target: Int): String
    fun reminderSnoozed(minutes: Int): String
}

/** Picks the copy for a resolved language ("en" / "ar"). Bound in DI to the app's strings. */
fun interface HabitReminderCopyProvider {
    fun forLanguage(language: String): HabitReminderCopy
}
