package com.example.reclaimapp.domain.recognition

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HabitTextNormalizerTest {

    private val normalizeCases: List<Pair<String, String>> = listOf(
        "Smoking" to "smoking",
        "  SMOKING  " to "smoking",
        "smoking!!!" to "smoking",
        "Coooffee" to "coffee",
        "smookiiing" to "smooking",
        "café" to "cafe",
        "don't" to "dont",
        "ok!!! 123 😀" to "ok",
        "التَّدخين" to "التدخين",
        "سجايييير" to "سجاير",
        "إنستجرام" to "انستجرام",
        "شوكولاتة" to "شوكولاته",
        "مَدرَسَة" to "مدرسه",
        "تـــدخـين" to "تدخين",
        "١٢٣ تدخين" to "تدخين",
        "على" to "علي",
        "Sooo" to "so",
        "a  b\tc" to "a b c",
        "naïve" to "naive",
        "Ünïcödé" to "unicode",
        "cool" to "cool",
        "coffee" to "coffee",
        "smoking-cigarettes" to "smoking cigarettes",
        "ÉCOLE" to "ecole",
        "وزارة الصحة؟" to "وزاره الصحه",
        "aaaa bbbbb" to "a b",
    )

    @Test
    fun normalizeTable() {
        val failures = normalizeCases.filter { (input, expected) -> HabitTextNormalizer.normalize(input) != expected }
            .map { (input, expected) -> "'$input' expected '$expected' but got '${HabitTextNormalizer.normalize(input)}'" }
        assertTrue(failures.isEmpty(), failures.joinToString("; "))
    }

    @Test
    fun squeezeCollapsesEveryRun() {
        assertEquals("cofe", HabitTextNormalizer.squeeze("coffee"))
        assertEquals("smoking", HabitTextNormalizer.squeeze("smooking"))
        assertEquals("a", HabitTextNormalizer.squeeze("a"))
        assertEquals("", HabitTextNormalizer.squeeze(""))
    }

    @Test
    fun analyzeStripsWrappersAndFillers() {
        val a = HabitTextNormalizer.analyze("I want to stop smoking cigarettes")
        assertEquals(listOf("smoking", "cigarettes"), a.coreTokens)
        assertEquals(IntentLean.BAD, a.intent)

        val b = HabitTextNormalizer.analyze("start walking daily")
        assertEquals(listOf("walking"), b.coreTokens)
        assertEquals(IntentLean.GOOD, b.intent)

        val c = HabitTextNormalizer.analyze("no more sugar")
        assertEquals(listOf("sugar"), c.coreTokens)
        assertEquals(IntentLean.BAD, c.intent)

        val d = HabitTextNormalizer.analyze("I'm trying to cut down on sugar")
        assertEquals(listOf("sugar"), d.coreTokens)
        assertEquals(IntentLean.BAD, d.intent)
    }

    @Test
    fun analyzeHandlesArabicWrappers() {
        val a = HabitTextNormalizer.analyze("عايز ابطل سجاير")
        assertEquals(listOf("سجاير"), a.coreTokens)
        assertEquals(IntentLean.BAD, a.intent)

        val b = HabitTextNormalizer.analyze("اقلع عن التدخين")
        assertEquals(listOf("التدخين"), b.coreTokens)
        assertEquals(IntentLean.BAD, b.intent)

        val c = HabitTextNormalizer.analyze("كل يوم قراءة")
        assertEquals(listOf("قراءه"), c.coreTokens)
        assertEquals(IntentLean.GOOD, c.intent)
    }

    @Test
    fun negatedSkipVerbIsDetected() {
        val negated = HabitTextNormalizer.analyze("don't skip workout")
        assertEquals(listOf("workout"), negated.coreTokens)
        assertTrue(negated.hasSkipVerb)
        assertTrue(negated.isSkipNegated)

        val plain = HabitTextNormalizer.analyze("skipping breakfast")
        assertTrue(plain.hasSkipVerb)
        assertFalse(plain.isSkipNegated)
    }

    @Test
    fun emptyAndWrapperOnlyInputsAreSafe() {
        val empty = HabitTextNormalizer.analyze("")
        assertTrue(empty.coreTokens.isEmpty())
        assertEquals(IntentLean.NONE, empty.intent)
        assertEquals("", empty.coreText)

        val wrapperOnly = HabitTextNormalizer.analyze("quit")
        assertTrue(wrapperOnly.coreTokens.isEmpty())
        assertEquals("quit", wrapperOnly.coreText)
    }
}
