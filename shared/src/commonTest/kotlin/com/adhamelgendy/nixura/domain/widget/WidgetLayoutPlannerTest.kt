package com.adhamelgendy.nixura.domain.widget

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
        assertFalse(p(120).showCounterEyebrow)
        assertTrue(p(130).showUnitLabels)
        assertTrue(p(150).showCounterEyebrow); assertFalse(p(150).showLongest)
        assertFalse(p(155).showLongest)
        assertTrue(p(170).showLongest)
        assertTrue(p(180).showLongest)
    }

    @Test fun quitWidgetNeverShowsTheCategory() {
        for (h in sizes) for (w in sizes) {
            val p = WidgetLayoutPlanner.plan(WidgetKind.QUIT_COUNTER, w, h, 3, "Smoking")
            assertFalse(p.showCategory, "quit category at ${w}x$h")
        }
    }

    @Test fun quitRowNeverSplitsAWordOfTheName() {
        // a 3x1 widget: before, "smoking" was broken into "smok / ing" beside the counter
        for (w in listOf(220, 240, 260, 280, 320)) for (h in listOf(56, 64, 72, 90)) for (days in listOf(1L, 12L, 365L)) {
            for (name in listOf("smoking", "Junk Food", "Social media")) {
                val p = WidgetLayoutPlanner.plan(WidgetKind.QUIT_COUNTER, w, h, days, name)
                assertEquals(WidgetVariant.ROW, p.variant)
                val pad = p.paddingDp
                val nameW = w - 2 * pad - WidgetLayoutPlanner.counterWidthDp(p.counterSizeDp, days) - 8 - if (p.showBadge) 48 else 0
                assertTrue(WidgetLayoutPlanner.titleFitsWhole(name, nameW, p.titleSizeDp, 2), "$name split at ${w}x$h, $days days")
            }
        }
    }

    @Test fun prayerWidgetShowsItsArchesBeforeTheWeek() {
        val p = WidgetLayoutPlanner.plan(WidgetKind.GOOD_QUICK_LOG, 320, 200, title = "pray", prayerMarks = 5)
        assertTrue(p.showPrayerMarks)
        assertFalse(p.showProgress)
        val tight = WidgetLayoutPlanner.plan(WidgetKind.GOOD_QUICK_LOG, 320, 130, title = "pray", prayerMarks = 5)
        if (tight.showWeek) assertTrue(tight.showPrayerMarks)
        assertTrue(p.usedHeightDp <= 200 - 2 * p.paddingDp)
    }

    @Test fun nothingIsShownThatNeedsAnEarlierRow() {
        for (kind in WidgetKind.entries) for (h in sizes) for (w in sizes) {
            val p = WidgetLayoutPlanner.plan(kind, w, h)
            if (p.showLongest) assertTrue(p.showCounterEyebrow, "longest needs eyebrow at ${w}x$h")
            // the week strip outranks the day streak, which outranks the progress bar
            if (kind == WidgetKind.GOOD_QUICK_LOG && p.variant == WidgetVariant.FULL && p.showProgress) assertTrue(p.showStreakRow, "progress needs streak at ${w}x$h")
        }
    }

    @Test fun goodWeekStripNeedsRoom() {
        // header is 44 dp here; the week strip (52) is the first optional row, inside the 16 dp of padding
        assertTrue(WidgetLayoutPlanner.plan(WidgetKind.GOOD_QUICK_LOG, 180, 120).showWeek)
        assertTrue(WidgetLayoutPlanner.plan(WidgetKind.GOOD_QUICK_LOG, 180, 180).showWeek)
    }

    @Test fun goodWeekStripComesBeforeStreakRowAndProgress() {
        val p = WidgetLayoutPlanner.plan(WidgetKind.GOOD_QUICK_LOG, 180, 120)
        assertTrue(p.showWeek); assertFalse(p.showStreakRow); assertFalse(p.showProgress)
        val q = WidgetLayoutPlanner.plan(WidgetKind.GOOD_QUICK_LOG, 180, 150)
        assertTrue(q.showWeek && q.showStreakRow); assertFalse(q.showProgress)
        val r = WidgetLayoutPlanner.plan(WidgetKind.GOOD_QUICK_LOG, 180, 170)
        assertTrue(r.showWeek && r.showStreakRow && r.showProgress)
    }

    @Test fun fullWidgetsUseEightDpInnerPadding() {
        assertEquals(8, WidgetLayoutPlanner.plan(WidgetKind.QUIT_COUNTER, 160, 150).paddingDp)
        assertEquals(8, WidgetLayoutPlanner.plan(WidgetKind.GOOD_QUICK_LOG, 180, 150).paddingDp)
        assertEquals(8, WidgetLayoutPlanner.plan(WidgetKind.EVENT, 160, 150).paddingDp)
    }

    @Test fun quitTwoByTwoKeepsTheClearForLine() {
        assertTrue(WidgetLayoutPlanner.plan(WidgetKind.QUIT_COUNTER, 160, 140, 3, "Smoking").showCounterEyebrow)
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
            val needed = size * (5.2f + digits * 0.62f) + if (digits > 0) 12 else 0
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

    // ---- habit-name estimate ----
    @Test fun titleEstimateWrapsWords() {
        assertEquals(1, WidgetLayoutPlanner.estimateTitleLines("", 100, 18))
        assertEquals(1, WidgetLayoutPlanner.estimateTitleLines("Gym", 100, 18))
        assertEquals(2, WidgetLayoutPlanner.estimateTitleLines("Read twenty pages", 120, 18))
        assertTrue(WidgetLayoutPlanner.estimateTitleLines("Read twenty pages", 60, 18) >= 3)
    }

    @Test fun titleEstimateBreaksAWordWiderThanTheLine() {
        assertTrue(WidgetLayoutPlanner.estimateTitleLines("Extraordinarily", 60, 18) >= 3)
    }

    @Test fun titleEstimateNeverDropsBelowOneLine() {
        assertEquals(1, WidgetLayoutPlanner.estimateTitleLines("   ", 0, 18))
    }

    // ---- good habit: the name has priority over the badge and the controls ----
    @Test fun goodBadgeGoesBeforeTheNameIsSqueezed() {
        // 180 wide: beside the buttons there is room for a short name only once the badge is dropped
        val short = WidgetLayoutPlanner.plan(WidgetKind.GOOD_QUICK_LOG, 180, 200, title = "Gym")
        assertFalse(short.showBadge); assertFalse(short.controlsBelow)
        val roomy = WidgetLayoutPlanner.plan(WidgetKind.GOOD_QUICK_LOG, 340, 200, title = "Gym")
        assertTrue(roomy.showBadge)
    }

    @Test fun goodControlsMoveBelowALongNameInsteadOfSqueezingIt() {
        val p = WidgetLayoutPlanner.plan(WidgetKind.GOOD_QUICK_LOG, 200, 200, title = "Read 20 pages of a book")
        assertTrue(p.controlsBelow)
        assertTrue(p.titleMaxLines >= 2)
    }

    @Test fun goodNameLinesAreEnoughForTheEstimate() {
        val title = "Meditation and breathing"
        for (h in sizes) for (w in sizes) {
            val p = WidgetLayoutPlanner.plan(WidgetKind.GOOD_QUICK_LOG, w, h, title = title)
            assertTrue(p.titleMaxLines in 1..3, "lines ${p.titleMaxLines} at ${w}x$h")
        }
    }

    @Test fun goodShortNarrowWidgetStacksTheControlsForALongName() {
        val p = WidgetLayoutPlanner.plan(WidgetKind.GOOD_QUICK_LOG, 110, 110, title = "Drink water")
        assertEquals(WidgetVariant.SLIM, p.variant)
        assertTrue(p.controlsBelow); assertFalse(p.showBadge)
    }

    // ---- good habit at 4x2: the streak row stays ----
    @Test fun goodRowKeepsTheStreakRowAtAboutFourByTwo() {
        for (h in listOf(100, 105, 110, 118)) for (w in listOf(250, 300, 340)) {
            val p = WidgetLayoutPlanner.plan(WidgetKind.GOOD_QUICK_LOG, w, h, title = "Read 20 pages of a book")
            assertEquals(WidgetVariant.ROW, p.variant)
            assertTrue(p.showStreakRow, "streak row at ${w}x$h")
        }
    }

    @Test fun goodRowDropsTheProgressBarBeforeTheStreakRow() {
        // 85 dp high: there is room for the streak row (62) but not for streak row + progress bar (72)
        val p = WidgetLayoutPlanner.plan(WidgetKind.GOOD_QUICK_LOG, 300, 85, title = "Gym")
        assertEquals(WidgetVariant.ROW, p.variant)
        assertTrue(p.showStreakRow); assertFalse(p.showProgress)
    }

    // ---- event / counter ----
    @Test fun counterSizeStillFitsWithTheWiderSpacing() {
        // 8 glyphs x 0.1 em letter spacing are part of CLOCK_EM: a 30 dp clock needs about 156 dp, not 132
        assertEquals(30, WidgetLayoutPlanner.counterSizeDp(160, 0, 30))
        assertTrue(WidgetLayoutPlanner.counterSizeDp(132, 0, 30) < 30)
    }
}
