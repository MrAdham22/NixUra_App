package com.adhamelgendy.nixura.domain.reminder

import com.adhamelgendy.nixura.domain.time.CivilCalendar
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Checked against published timetables (Egyptian General Authority of Survey method, times in UTC), as served by the
 * aladhan.com timings API on 2026-11-21 (method 5, standard Asr, angle-based high-latitude rule). A minute either way
 * is the usual rounding difference between timetables.
 */
class PrayerTimeCalculatorTest {

    private val cairo = GeoPoint(30.0444, 31.2357)
    private val mecca = GeoPoint(21.4225, 39.8262)
    private val london = GeoPoint(51.5074, -0.1278)

    private fun assertTimes(place: GeoPoint, year: Int, month: Int, day: Int, vararg expected: Pair<Prayer, String>) {
        val dayIndex = CivilCalendar.dayIndexFromCivil(year, month, day)
        val times = PrayerTimeCalculator.forDay(dayIndex, place)
        for ((prayer, hhmm) in expected) {
            val (h, m) = hhmm.split(':').map { it.toInt() }
            val want = dayIndex * 86_400_000L + (h * 60 + m) * 60_000L
            val got = times[prayer] ?: error("$prayer missing")
            val diffMinutes = abs(got - want) / 60_000.0
            assertTrue(diffMinutes <= 2.0, "$prayer on $year-$month-$day: off by $diffMinutes min")
        }
    }

    @Test fun cairoInWinter() = assertTimes(
        cairo, 2026, 1, 15,
        Prayer.FAJR to "03:21", Prayer.DHUHR to "10:04", Prayer.ASR to "12:57", Prayer.MAGHRIB to "15:17", Prayer.ISHA to "16:39",
    )

    @Test fun cairoInSummer() = assertTimes(
        cairo, 2026, 7, 15,
        Prayer.FAJR to "01:21", Prayer.DHUHR to "10:01", Prayer.ASR to "13:38", Prayer.MAGHRIB to "16:58", Prayer.ISHA to "18:29",
    )

    @Test fun meccaAtTheEquinox() = assertTimes(
        mecca, 2026, 3, 21,
        Prayer.FAJR to "02:04", Prayer.DHUHR to "09:28", Prayer.ASR to "12:52", Prayer.MAGHRIB to "15:32", Prayer.ISHA to "16:44",
    )

    @Test fun londonInAutumn() = assertTimes(
        london, 2026, 10, 15,
        Prayer.FAJR to "04:23", Prayer.DHUHR to "11:46", Prayer.ASR to "14:34", Prayer.MAGHRIB to "17:07", Prayer.ISHA to "18:55",
    )

    @Test fun highLatitudeSummerStillHasFiveOrderedPrayers() {
        // London at midsummer: the sun never gets 19.5° down, so Fajr / Isha come from the angle-based night portion.
        val times = PrayerTimeCalculator.forDay(CivilCalendar.dayIndexFromCivil(2026, 6, 21), london)
        val ordered = Prayer.entries.map { times[it] ?: error("$it missing") }
        assertEquals(ordered.sorted(), ordered)
    }

    @Test fun prayersAreInOrderAcrossTheYear() {
        val start = CivilCalendar.dayIndexFromCivil(2026, 1, 1)
        for (offset in 0 until 365 step 7) {
            val times = PrayerTimeCalculator.forDay(start + offset, cairo)
            val ordered = Prayer.entries.map { times[it] ?: error("$it missing on +$offset") }
            assertEquals(ordered.sorted(), ordered, "order on day +$offset")
        }
    }
}
