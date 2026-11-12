package com.adhamelgendy.nixura.domain.recognition

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PrayerHabitDetectorTest {

    @Test
    fun recognizesEnglishPrayerNames() {
        listOf("pray", "Pray 5 times", "daily prayers", "Praying on time", "Salah", "salat", "Fajr", "prayr", "Namaz")
            .forEach { assertTrue(PrayerHabitDetector.isPrayer(it), it) }
    }

    @Test
    fun recognizesArabicPrayerNames() {
        listOf("صلاة", "الصلاة", "الصلوات الخمس", "اصلي الفجر", "صلاتي", "والصلاة").forEach {
            assertTrue(PrayerHabitDetector.isPrayer(it), it)
        }
    }

    @Test
    fun ignoresOtherHabits() {
        listOf("", "  ", "Drink water", "Read 10 pages", "Morning walk", "Meditate", "صلاح", "عصير", "Workout", "Sleep before midnight")
            .forEach { assertFalse(PrayerHabitDetector.isPrayer(it), it) }
    }

    @Test
    fun findsTheWordPosition() {
        assertEquals(6..9, PrayerHabitDetector.wordRange("Daily pray 5x"))
        assertEquals(0..3, PrayerHabitDetector.wordRange("pray"))
        assertNull(PrayerHabitDetector.wordRange("Drink water"))
    }

    @Test
    fun fillStyleDetectorPicksPrayerFirst() {
        val detector = GoodHabitFillStyleDetector(com.adhamelgendy.nixura.domain.coach.HabitKnowledgeBase())
        assertEquals(GoodHabitFillStyle.PRAYER, detector.styleFor("pray"))
        assertEquals(GoodHabitFillStyle.WATER, detector.styleFor("drink water"))
    }
}
