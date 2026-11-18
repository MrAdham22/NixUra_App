package com.adhamelgendy.nixura.domain.theme

import com.adhamelgendy.nixura.domain.coach.HabitKnowledgeBase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class HabitThemeDetectorTest {
    private val detector = HabitThemeDetector(HabitKnowledgeBase())

    private fun assertTheme(theme: HabitTheme, vararg names: String) =
        names.forEach { assertEquals(theme, detector.themeFor(it), "\"$it\"") }

    @Test
    fun water() = assertTheme(HabitTheme.WATER, "drink water", "Drink Water", "watr", "drink watr", "hydrate", "مية", "مياه", "اشرب مياه", "ماء", "اشرب مية")

    @Test
    fun prayer() = assertTheme(HabitTheme.PRAYER, "pray", "prayer", "salah", "صلاة", "صلي", "الصلاة")

    @Test
    fun walk() = assertTheme(HabitTheme.WALK, "walk", "walking 30 min", "مشي", "المشي")

    @Test
    fun workout() = assertTheme(HabitTheme.WORKOUT, "workout", "gym", "exercise", "تمرين", "جيم")

    @Test
    fun reading() = assertTheme(HabitTheme.READING, "read", "reading", "read 10 pages", "قراءة", "القراءة")

    @Test
    fun sleep() = assertTheme(HabitTheme.SLEEP, "sleep early", "sleep 8 hours", "نوم", "النوم بدري")

    @Test
    fun everythingElseKeepsTheDefaultLook() = assertTheme(HabitTheme.DEFAULT, "", "call mom", "journal", "floss", "xyzzy")

    @Test
    fun storageKeysRoundTripAndUnknownKeysAreNull() {
        HabitTheme.entries.forEach { assertEquals(it, HabitTheme.fromStorageKey(it.storageKey)) }
        assertNull(HabitTheme.fromStorageKey("from-a-newer-build"))
        assertNull(HabitTheme.fromStorageKey(null))
    }
}
