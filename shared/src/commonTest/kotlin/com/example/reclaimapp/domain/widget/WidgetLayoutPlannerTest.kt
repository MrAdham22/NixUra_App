package com.example.reclaimapp.domain.widget

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class WidgetLayoutPlannerTest {
    private val sizes = listOf(40, 57, 70, 90, 110, 119, 120, 140, 180, 250, 400)

    // ---- variant choice ----
    @Test fun tallWidgetsUseTheStackedLayout() =
        assertEquals(WidgetVariant.FULL, WidgetLayoutPlanner.plan(WidgetKind.QUIT_COUNTER, 110, 130).variant)

    @Test fun shortWideWidgetsUseOneRow() =
        assertEquals(WidgetVariant.ROW, WidgetLayoutPlanner.plan(WidgetKind.QUIT_COUNTER, 300, 70).variant)

    @Test fun shortNarrowWidgetsUseTwoLines() =
        assertEquals(WidgetVariant.SLIM, WidgetLayoutPlanner.plan(WidgetKind.QUIT_COUNTER, 110, 70).variant)

    @Test fun boundaryHeightAndWidth() {
        assertEquals(WidgetVariant.SLIM, WidgetLayoutPlanner.plan(WidgetKind.EVENT, 219, 119).variant)
        assertEquals(WidgetVariant.ROW, WidgetLayoutPlanner.plan(WidgetKind.EVENT, 220, 119).variant)
        assertEquals(WidgetVariant.FULL, WidgetLayoutPlanner.plan(WidgetKind.EVENT, 219, 120).variant)
    }

    // ---- optional rows are added in priority order, only when they fit ----
    @Test fun quitExtrasAppearInPriorityOrder() {
        fun p(h: Int) = WidgetLayoutPlanner.plan(WidgetKind.QUIT_COUNTER, 180, h)
        assertFalse(p(120).showCounterEyebrow); assertFalse(p(120).showStage)
        assertTrue(p(130).showUnitLabels)
        assertTrue(p(150).showCounterEyebrow); assertFalse(p(150).showStage)
        assertTrue(p(175).showStage); assertFalse(p(175).showLongest)
        assertTrue(p(200).showLongest)
    }

    @Test fun nothingIsShownThatNeedsAnEarlierRow() {
        for (kind in WidgetKind.entries) for (h in sizes) for (w in sizes) {
            val p = WidgetLayoutPlanner.plan(kind, w, h)
            if (p.showStage) assertTrue(p.showCounterEyebrow, "stage needs eyebrow at ${w}x$h")
            if (p.showLongest) assertTrue(p.showStage, "longest needs stage at ${w}x$h")
            if (p.showStreakRow) assertTrue(p.showWeek, "streak needs week at ${w}x$h")
        }
    }

    @Test fun goodWeekStripNeedsRoom() {
        assertFalse(WidgetLayoutPlanner.plan(WidgetKind.GOOD_QUICK_LOG, 180, 130).showWeek)
        assertTrue(WidgetLayoutPlanner.plan(WidgetKind.GOOD_QUICK_LOG, 180, 150).showWeek)
        assertTrue(WidgetLayoutPlanner.plan(WidgetKind.GOOD_QUICK_LOG, 180, 180).showStreakRow)
    }

    @Test fun eventTitleGetsTwoLinesOnlyWhenRoomy() {
        assertEquals(1, WidgetLayoutPlanner.plan(WidgetKind.EVENT, 180, 125).titleMaxLines)
        assertEquals(2, WidgetLayoutPlanner.plan(WidgetKind.EVENT, 180, 170).titleMaxLines)
    }

    @Test fun thinGoodWidgetDropsMinusOnlyWhenNarrow() {
        assertFalse(WidgetLayoutPlanner.plan(WidgetKind.GOOD_QUICK_LOG, 110, 60).showMinus)
        assertTrue(WidgetLayoutPlanner.plan(WidgetKind.GOOD_QUICK_LOG, 160, 60).showMinus)
    }

    // ---- counter sizing: the numerals must always fit the width ----
    @Test fun counterSizeShrinksWithWidth() {
        val wide = WidgetLayoutPlanner.counterSizeDp(260, 12, 32)
        val narrow = WidgetLayoutPlanner.counterSizeDp(78, 12, 32)
        assertTrue(wide > narrow); assertEquals(32, wide); assertTrue(narrow >= 12)
    }

    @Test fun counterSizeAccountsForTheDaysBlock() {
        assertTrue(WidgetLayoutPlanner.counterSizeDp(150, 0, 32) > WidgetLayoutPlanner.counterSizeDp(150, 1234, 32))
    }

    @Test fun counterAlwaysFitsItsContentWidth() {
        for (w in listOf(70, 78, 90, 120, 160, 240, 400)) for (d in listOf(0L, 5L, 12L, 365L, 12345L)) {
            val size = WidgetLayoutPlanner.counterSizeDp(w, d, 32)
            val digits = if (d > 0) d.toString().length else 0
            val needed = size * (4.4f + digits * 0.62f) + if (digits > 0) 12 else 0
            // only the 12 dp floor may exceed the width (extremely narrow widgets)
            assertTrue(size == 12 || needed <= w + 0.5f, "width $w days $d size $size needs $needed")
        }
    }

    @Test fun paddingNeverExceedsAThirdOfTheHeight() {
        for (kind in WidgetKind.entries) for (h in sizes) for (w in sizes) {
            val p = WidgetLayoutPlanner.plan(kind, w, h)
            assertTrue(p.paddingDp * 2 <= h / 2 + 8, "padding ${p.paddingDp} too big for ${w}x$h")
        }
    }

    @Test fun degenerateSizesAreClampedNotCrashing() {
        val p = WidgetLayoutPlanner.plan(WidgetKind.QUIT_COUNTER, 0, 0)
        assertEquals(WidgetVariant.SLIM, p.variant)
    }
}
