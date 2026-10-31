package com.example.reclaimapp.domain.widget

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class NearestEventSelectorTest {
    private val now = 1_000_000L

    @Test fun emptyListGivesNull() = assertNull(NearestEventSelector.select(emptyList(), now))

    @Test fun singleUpcomingIsChosen() =
        assertEquals(1L, NearestEventSelector.select(listOf(event(1, now + 5)), now)?.id)

    @Test fun soonestUpcomingWins() {
        val events = listOf(event(1, now + 500), event(2, now + 100), event(3, now + 900))
        assertEquals(2L, NearestEventSelector.select(events, now)?.id)
    }

    @Test fun upcomingBeatsMoreRecentPast() {
        val events = listOf(event(1, now - 1), event(2, now + DAY * 30))
        assertEquals(2L, NearestEventSelector.select(events, now)?.id)
    }

    @Test fun allPastGivesMostRecentPast() {
        val events = listOf(event(1, now - 500), event(2, now - 10), event(3, now - 900))
        assertEquals(2L, NearestEventSelector.select(events, now)?.id)
    }

    @Test fun eventExactlyNowCountsAsPast() {
        val events = listOf(event(1, now), event(2, now + 1))
        assertEquals(2L, NearestEventSelector.select(events, now)?.id)
        assertEquals(1L, NearestEventSelector.select(listOf(event(1, now)), now)?.id)
    }

    @Test fun tieOnSameTimePicksLowestId() {
        val events = listOf(event(9, now + 50), event(4, now + 50))
        assertEquals(4L, NearestEventSelector.select(events, now)?.id)
    }
}
