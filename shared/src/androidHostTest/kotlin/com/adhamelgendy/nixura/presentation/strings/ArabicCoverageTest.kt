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
        )
        samples.forEach { (ar, en) -> assertTrue(ar != en, "still English: $en") }
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
