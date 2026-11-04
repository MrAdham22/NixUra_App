package com.adhamelgendy.nixura.presentation.navigation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MainTabsTest {
    @Test
    fun pagerHoldsTheFourSwipeableTabsInBarOrder() {
        assertEquals(listOf(Route.Home, Route.BadHabits, Route.GoodHabits, Route.Events), MainTabs.pagerTabs)
        assertEquals(MainTabs.pagerTabs + Route.Coach, MainTabs.barTabs)
    }

    @Test
    fun coachAndNonTabRoutesAreNotPages() {
        assertNull(MainTabs.pageOf(Route.Coach))
        assertNull(MainTabs.pageOf(Route.Profile))
        assertNull(MainTabs.pageOf(Route.EventForm(3)))
        assertFalse(MainTabs.isPagerTab(Route.Settings))
        assertTrue(MainTabs.isPagerTab(Route.Events))
    }

    @Test
    fun pageIndicesMatchTheBar() {
        MainTabs.pagerTabs.forEachIndexed { index, tab -> assertEquals(index, MainTabs.pageOf(tab)) }
    }

    @Test
    fun backFromAnotherTabGoesHomeAndBackFromHomeLeavesTheApp() {
        val navigator = Navigator()
        navigator.selectTab(Route.Events)
        assertTrue(navigator.back())
        assertEquals(Route.Home, navigator.current)
        assertFalse(navigator.back())
    }

    @Test
    fun detailScreenPoppedBackLandsOnTheTabItCameFrom() {
        val navigator = Navigator()
        navigator.selectTab(Route.Events)
        navigator.push(Route.EventForm(7))
        assertTrue(navigator.back())
        assertEquals(Route.Events, navigator.current)
    }
}
