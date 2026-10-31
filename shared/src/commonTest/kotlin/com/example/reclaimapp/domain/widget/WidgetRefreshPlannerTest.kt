package com.example.reclaimapp.domain.widget

import com.example.reclaimapp.domain.time.DayCalculator
import com.example.reclaimapp.domain.time.TimeZoneOffsetProvider
import kotlin.test.Test
import kotlin.test.assertEquals

class WidgetRefreshPlannerTest {
    private val days = utcDays()

    @Test fun midnightIsDefault() =
        assertEquals(11 * DAY, WidgetRefreshPlanner.nextRefreshAt(10 * DAY + 5 * HOUR, days, emptyList()))

    @Test fun earlierChangeWins() =
        assertEquals(10 * DAY + 6 * HOUR, WidgetRefreshPlanner.nextRefreshAt(10 * DAY + 5 * HOUR, days, listOf(10 * DAY + 6 * HOUR, 10 * DAY + 9 * HOUR)))

    @Test fun laterChangeIsIgnored() =
        assertEquals(11 * DAY, WidgetRefreshPlanner.nextRefreshAt(10 * DAY, days, listOf(13 * DAY)))

    @Test fun nullAndPastChangesAreIgnored() =
        assertEquals(11 * DAY, WidgetRefreshPlanner.nextRefreshAt(10 * DAY + HOUR, days, listOf(null, 10 * DAY, 10 * DAY + HOUR)))

    @Test fun neverCloserThanMinGap() {
        val now = 11 * DAY - 200
        assertEquals(now + WidgetRefreshPlanner.MIN_GAP_MILLIS, WidgetRefreshPlanner.nextRefreshAt(now, days, emptyList()))
    }

    @Test fun midnightFollowsTheTimeZoneOffset() {
        val now = 10 * DAY + 22 * HOUR
        assertEquals(11 * DAY, WidgetRefreshPlanner.nextLocalMidnight(now, utcDays(0)))
        // UTC+3: local day 11 already started at 21:00 UTC, so the next midnight is 21:00 UTC on day 11.
        assertEquals(11 * DAY + 21 * HOUR, WidgetRefreshPlanner.nextLocalMidnight(now, utcDays(3 * HOUR)))
    }

    @Test fun timeZoneChangeMovesTheNextMidnight() {
        val now = 10 * DAY + 12 * HOUR
        val before = WidgetRefreshPlanner.nextLocalMidnight(now, utcDays(0))
        val after = WidgetRefreshPlanner.nextLocalMidnight(now, utcDays(-5 * HOUR))
        assertEquals(11 * DAY, before)
        assertEquals(11 * DAY + 5 * HOUR, after)
    }

    @Test fun springForwardDayMidnightIsTwentyThreeHoursAway() {
        // Offset jumps from +0 to +1h at instant T (day 20, 02:00 UTC). Local day 20 is 23h long.
        val t = 20 * DAY + 2 * HOUR
        val dst = DayCalculator(TimeZoneOffsetProvider { if (it >= t) HOUR else 0L })
        val now = 20 * DAY + HOUR / 2 // 00:30 local before the jump
        val midnight = WidgetRefreshPlanner.nextLocalMidnight(now, dst)
        assertEquals(21 * DAY - HOUR, midnight) // local midnight of day 21 = 23:00 UTC on day 20
        assertEquals(21L, dst.dayIndexOf(midnight))
        assertEquals(20L, dst.dayIndexOf(midnight - 1))
    }

    @Test fun fallBackDayMidnightIsTwentyFiveHoursAway() {
        val t = 30 * DAY + 2 * HOUR
        val dst = DayCalculator(TimeZoneOffsetProvider { if (it >= t) 0L else HOUR })
        val now = 30 * DAY - HOUR / 2 // 00:30 local (offset +1) on day 30
        val midnight = WidgetRefreshPlanner.nextLocalMidnight(now, dst)
        assertEquals(31 * DAY, midnight)
        assertEquals(31L, dst.dayIndexOf(midnight))
    }
}
