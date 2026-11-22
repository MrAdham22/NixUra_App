package com.adhamelgendy.nixura.presentation.strings

import com.adhamelgendy.nixura.domain.theme.HabitTheme
import kotlin.test.Test
import kotlin.test.assertTrue

/** An Arabic reminder line that can start with a typed (maybe Latin) name starts with an RLM, so it reads right to left. */
class ArabicReminderDirectionTest {
    private val rlm = '\u200F'
    private val name = "Reading"

    private fun assertRtl(text: String) = assertTrue(text.startsWith(rlm), "no leading RLM: $text")

    @Test
    fun dailyBodyStartsWithRlm() {
        assertRtl(ArabicStrings.reminderDailyBody(name, 0, 2))
        assertRtl(ArabicStrings.reminderDailyBody(name, 0, 1))
        assertTrue("\u200F0 من 2" in ArabicStrings.reminderDailyBody(name, 0, 2))
    }

    @Test
    fun weeklyBodyStartsWithRlm() {
        assertRtl(ArabicStrings.reminderWeeklyBody(name, 1, 3))
    }

    @Test
    fun loggedHabitStartsWithRlm() {
        assertRtl(ArabicStrings.reminderLoggedHabit(name, 1, 2))
        assertRtl(ArabicStrings.reminderLoggedHabit(name, 2, 2))
    }

    @Test
    fun titlesThatStartWithANameStartWithRlm() {
        assertRtl(ArabicStrings.reminderHabitTitle(HabitTheme.WATER, name, 0))
        assertRtl(ArabicStrings.reminderHabitTitle(HabitTheme.PRAYER, name, 0))
        assertRtl(ArabicStrings.reminderHabitTitle(HabitTheme.DEFAULT, name, 1))
    }
}
