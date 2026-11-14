package com.adhamelgendy.nixura.domain.widget

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** The cut-off texts from the owner's home-screen screenshots (2026-11-14). */
class WidgetTextFitTest {
    @Test
    fun anEventNameShrinksToShowWholeOnASmallSquare() {
        // 2x2 event: "Bablo concert" used to end in "...".
        val plan = WidgetLayoutPlanner.plan(WidgetKind.EVENT, 170, 160, days = 0, title = "Bablo concert")
        assertTrue(WidgetLayoutPlanner.titleFitsWhole("Bablo concert", 170 - 16, plan.titleSizeDp, plan.titleMaxLines))
    }

    @Test
    fun aFourByOneEventKeepsItsName() {
        val plan = WidgetLayoutPlanner.plan(WidgetKind.EVENT, 300, 70, days = 2, title = "Bablo concert")
        assertEquals(WidgetVariant.ROW, plan.variant)
        assertTrue(plan.titleSizeDp in 14..18)
    }

    @Test
    fun aShortNameBesideTheButtonsNeverSplitsMidWord() {
        // 3x2 good habit: "Workout" was drawn as "Worko / ut".
        val plan = WidgetLayoutPlanner.plan(WidgetKind.GOOD_QUICK_LOG, 250, 160, title = "Workout")
        if (!plan.controlsBelow) {
            val textW = 250 - 16 - 98 - (if (plan.showBadge) 52 else 0)
            assertTrue(WidgetLayoutPlanner.titleFitsWhole("Workout", textW, 20, plan.titleMaxLines))
            assertTrue(textW >= 96, "room for the '2 / 3 today' line")
        }
    }

    @Test
    fun aNarrowPrayerWidgetGivesUpTheMinusStarBeforeCuttingTheCount() {
        // 2x2 prayer: the "0/5" count showed as "0...".
        val plan = WidgetLayoutPlanner.plan(WidgetKind.GOOD_QUICK_LOG, 170, 160, title = "Pray", prayerMarks = 5)
        assertTrue(plan.controlsBelow)
        assertFalse(plan.showMinus)
    }

    @Test
    fun aQuitRowPrefersOneLineForItsName() {
        val plan = WidgetLayoutPlanner.plan(WidgetKind.QUIT_COUNTER, 300, 80, days = 0, title = "Junk food")
        assertEquals(1, plan.titleMaxLines)
    }
}
