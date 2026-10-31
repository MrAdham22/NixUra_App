package com.example.reclaimapp.domain.coach

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

class DurationParserTest {

    private fun check(cases: List<Pair<String, Duration?>>) {
        cases.forEach { (text, expected) -> assertEquals(expected, DurationParser.parse(text), "parse(\"$text\")") }
    }

    @Test
    fun englishUnitsAndAbbreviations() = check(
        listOf(
            "6 hours" to 6.hours,
            "6 hour" to 6.hours,
            "6h" to 6.hours,
            "6 hrs" to 6.hours,
            "90 minutes" to 90.minutes,
            "90 min" to 90.minutes,
            "45 mins" to 45.minutes,
            "20m" to 20.minutes,
            "3d" to 3.days,
            "3 days" to 3.days,
            "1 day" to 1.days,
            "2w" to 14.days,
            "2 weeks" to 14.days,
            "3 wks" to 21.days,
            "1 month" to 30.days,
            "2 months" to 60.days,
            "3mo" to 90.days,
            "1 year" to 365.days,
            "2 yrs" to 730.days,
            "6hours" to 6.hours,
            "12 HOURS" to 12.hours,
            "  7   days  " to 7.days,
        ),
    )

    @Test
    fun articlesAndHalves() = check(
        listOf(
            "a day" to 1.days,
            "an hour" to 1.hours,
            "a week" to 7.days,
            "a month" to 30.days,
            "half a day" to 12.hours,
            "half an hour" to 30.minutes,
            "half day" to 12.hours,
            "a day and a half" to 36.hours,
            "an hour and a half" to 90.minutes,
            "2 and a half days" to 60.hours,
            "one and a half hours" to 90.minutes,
            "a couple of days" to 2.days,
        ),
    )

    @Test
    fun decimalsAndNumberWords() = check(
        listOf(
            "1.5 days" to 36.hours,
            "1,5 days" to 36.hours,
            "0.5 hours" to 30.minutes,
            "2.5h" to 150.minutes,
            "one day" to 1.days,
            "two weeks" to 14.days,
            "three hours" to 3.hours,
            "twelve hours" to 12.hours,
            "twenty days" to 20.days,
            "twenty one days" to 21.days,
            "thirty days" to 30.days,
            "sixty minutes" to 60.minutes,
            "ninety minutes" to 90.minutes,
        ),
    )

    @Test
    fun compoundAndRanges() = check(
        listOf(
            "2 days 3 hours" to 51.hours,
            "1d 6h" to 30.hours,
            "1h30m" to 90.minutes,
            "2 hours and 30 minutes" to 150.minutes,
            "1 week and 2 days" to 9.days,
            "2 days 2 days" to 2.days,
            "3-5 days" to 3.days,
            "3 to 5 days" to 3.days,
            "2 or 3 weeks" to 14.days,
            "1-2 hours" to 1.hours,
        ),
    )

    @Test
    fun typosInUnitWords() = check(
        listOf(
            "6 housr" to 6.hours,
            "6 horus" to 6.hours,
            "2 houres" to 2.hours,
            "3 wekk" to 21.days,
            "2 weaks" to 14.days,
            "2 dayz" to 2.days,
            "5 mnths" to 150.days,
            "1 mounth" to 30.days,
            "10 minuts" to 10.minutes,
            "4 weks" to 28.days,
        ),
    )

    @Test
    fun arabicAndEgyptianArabic() = check(
        listOf(
            "3 ساعات" to 3.hours,
            "٣ أيام" to 3.days,
            "٣ ايام" to 3.days,
            "يومين" to 2.days,
            "يوم" to 1.days,
            "اسبوع" to 7.days,
            "أسبوع" to 7.days,
            "اسبوعين" to 14.days,
            "شهر" to 30.days,
            "شهرين" to 60.days,
            "ساعتين" to 2.hours,
            "ساعة" to 1.hours,
            "دقيقتين" to 2.minutes,
            "ثلاث ساعات" to 3.hours,
            "خمس ايام" to 5.days,
            "نص يوم" to 12.hours,
            "نصف يوم" to 12.hours,
            "يوم ونص" to 36.hours,
            "ساعة ونص" to 90.minutes,
            "10 دقائق" to 10.minutes,
            "٢ اسابيع" to 14.days,
            "ماذا يحدث لو بطلت يومين" to 2.days,
            "بعد 3 days" to 3.days,
        ),
    )

    @Test
    fun durationIsFoundInsideASentence() = check(
        listOf(
            "what happens if I stop smoking for 6 hours?" to 6.hours,
            "what will I feel after 3 days without it" to 3.days,
            "I quit a month ago" to 30.days,
            "how about two weeks?" to 14.days,
            "and after a week?" to 7.days,
        ),
    )

    @Test
    fun nonsenseIsRejected() = check(
        listOf(
            "" to null,
            "   " to null,
            "hello" to null,
            "6" to null,
            "0 hours" to null,
            "0 days" to null,
            "-3 days" to null,
            "20 years" to null,
            "11 years" to null,
            "5000 days" to null,
            "30 seconds" to null,
            "every day" to null,
            "per week" to null,
            "3 times a day" to null,
            "كل يوم" to null,
            "3 your friends" to null,
            "2 dogs" to null,
        ),
    )

    @Test
    fun tenYearsIsTheUpperLimit() {
        assertEquals(3650.days, DurationParser.parse("10 years"))
        assertNull(DurationParser.parse("11 years"))
    }

    @Test
    fun spanCoversTheMatchedWordsOnly() {
        val text = "stop for 6 hours please"
        val found = assertNotNull(DurationParser.find(text))
        assertEquals(6.hours, found.duration)
        assertEquals("stop for  please", text.removeRange(found.start, found.end))
    }

    @Test
    fun spanOfACompoundAndOfAnArticle() {
        val compound = "after 2 days 3 hours of nothing"
        val a = assertNotNull(DurationParser.find(compound))
        assertEquals(51.hours, a.duration)
        assertEquals("after  of nothing", compound.removeRange(a.start, a.end))

        val article = "give me a month"
        val b = assertNotNull(DurationParser.find(article))
        assertEquals("give me ", article.removeRange(b.start, b.end))
    }

    @Test
    fun firstValidDurationWins() {
        assertEquals(2.days, DurationParser.parse("0 hours then 2 days"))
        assertEquals(3.hours, DurationParser.parse("3 hours or 2 days"))
    }
}
