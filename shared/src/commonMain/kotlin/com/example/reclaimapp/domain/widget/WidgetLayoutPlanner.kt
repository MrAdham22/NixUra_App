package com.example.reclaimapp.domain.widget

import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max

/** Which layout file family to use for the current widget size. */
enum class WidgetVariant {
    /** Stacked like the in-app card: header, counter and optional extra rows. Used from [WidgetLayoutPlanner.FULL_MIN_HEIGHT_DP]. */
    FULL,

    /** One horizontal row (badge, title, counter / buttons). Short but wide widgets (4x2-like). */
    ROW,

    /** Two compact lines (title, counter / buttons). Short and narrow widgets. */
    SLIM,
}

/**
 * What fits in a widget of a given size. The rule: **titles are never dropped**; optional rows are added in priority
 * order only while their height still fits, so nothing is clipped or drawn on top of anything else.
 * Heights are dp estimates of the rows in the layouts under `res/layout/widget_*`.
 *
 * For good habits the habit name has the highest priority of all: when the width is tight the glyph badge is dropped
 * first, then the controls move below the name, so the name is never squeezed or cut.
 */
data class WidgetLayoutPlan(
    val variant: WidgetVariant,
    val paddingDp: Int,
    val showUnitLabels: Boolean = false,
    /** "CLEAR FOR" / "STARTS IN" line above the counter. */
    val showCounterEyebrow: Boolean = false,
    val showCategory: Boolean = false,
    val showLongest: Boolean = false,
    val showWeek: Boolean = false,
    /** "N day streak" + "N times in total" row (good habits). */
    val showStreakRow: Boolean = false,
    /** Today's progress bar (good habits). */
    val showProgress: Boolean = true,
    val showDate: Boolean = false,
    val showMinus: Boolean = true,
    val showBadge: Boolean = true,
    /** Good habits: the +/- buttons sit on their own row below the name (layout `widget_good_full_narrow`). */
    val controlsBelow: Boolean = false,
    val titleMaxLines: Int = 1,
    /** Counter numeral size in dp (not sp, so a large system font cannot push it out of the widget). */
    val counterSizeDp: Int,
)

object WidgetLayoutPlanner {
    const val FULL_MIN_HEIGHT_DP = 120
    const val ROW_MIN_WIDTH_DP = 220
    private const val FULL_PADDING = 8 // events
    private const val TIGHT_PADDING = 8 // quit counter and good habit: less inner padding, so more rows fit (e.g. 2x2)
    /** "12:34:56" in a bold tabular font, in em, including the 0.1 em letter spacing of the counter (8 glyphs). */
    private const val CLOCK_EM = 5.2f
    private const val DIGIT_EM = 0.62f
    private const val DAYS_GAP_DP = 12

    // ---- habit-name sizes (sp) of the good layouts and an average glyph advance, used to estimate wrapping ----
    private const val NAME_FULL_SP = 20
    private const val NAME_ROW_SP = 18
    private const val NAME_COMPACT_SP = 17 // narrow / slim layouts
    private const val CHAR_EM = 0.56f
    private const val SPACE_EM = 0.28f

    // ---- horizontal space taken by the fixed parts of the good layouts (dp, including margins) ----
    private const val BADGE_FULL_DP = 52 // 40 + 12
    private const val BADGE_COMPACT_DP = 44 // 32 + 12
    private const val BADGE_ROW_DP = 48 // 36 + 12
    private const val CONTROLS_FULL_DP = 98 // minus 38+8, plus 44+8
    private const val PLUS_FULL_DP = 52
    private const val CONTROLS_ROW_DP = 90 // minus 34+8, plus 40+8
    private const val CONTROLS_SLIM_DP = 82 // minus 32+6, plus 38+6
    private const val PLUS_SLIM_DP = 44
    private const val PLUS_STACKED_DP = 46
    private const val MINUS_STACKED_DP = 38
    private const val MIN_COUNT_DP = 44 // room the "3/8" count needs beside the buttons

    /**
     * Largest numeral size (dp) so that "[days] [HH:MM:SS]" fits [contentWidthDp]; never above [maxDp] or below 12.
     * With no days block the clock gets the whole width.
     */
    fun counterSizeDp(contentWidthDp: Int, days: Long, maxDp: Int): Int {
        val digits = if (days > 0) days.toString().length else 0
        val gap = if (digits > 0) DAYS_GAP_DP else 0
        val em = CLOCK_EM + digits * DIGIT_EM
        val fit = floor((contentWidthDp - gap) / em).toInt()
        return fit.coerceIn(12, maxDp)
    }

    /**
     * Estimated number of lines [title] needs at [sizeSp] in [widthDp] (greedy word wrap; a word wider than the line
     * breaks across lines). An estimate, deliberately a little pessimistic, used to decide what to drop **before**
     * the habit name would have to be squeezed.
     */
    fun estimateTitleLines(title: String, widthDp: Int, sizeSp: Int): Int {
        val words = title.split(' ', '\n', '\t').filter { it.isNotEmpty() }
        if (words.isEmpty()) return 1
        val avail = max(widthDp, 1).toFloat()
        val charW = sizeSp * CHAR_EM
        val spaceW = sizeSp * SPACE_EM
        var lines = 1
        var used = 0f
        for (word in words) {
            val wordW = word.length * charW
            val needed = if (used == 0f) wordW else used + spaceW + wordW
            if (needed <= avail) {
                used = needed
            } else {
                if (used > 0f) lines++
                if (wordW <= avail) {
                    used = wordW
                } else {
                    val parts = ceil(wordW / avail).toInt()
                    lines += parts - 1
                    used = wordW - (parts - 1) * avail
                }
            }
        }
        return lines
    }

    fun plan(kind: WidgetKind, widthDp: Int, heightDp: Int, days: Long = 0, title: String = ""): WidgetLayoutPlan {
        val w = widthDp.coerceAtLeast(40)
        val h = heightDp.coerceAtLeast(40)
        return when {
            h >= FULL_MIN_HEIGHT_DP -> full(kind, w, h, days, title)
            w >= ROW_MIN_WIDTH_DP -> row(kind, w, h, days, title)
            else -> slim(kind, w, h, days, title)
        }
    }

    private fun full(kind: WidgetKind, w: Int, h: Int, days: Long, title: String): WidgetLayoutPlan {
        val pad = if (kind == WidgetKind.EVENT) FULL_PADDING else TIGHT_PADDING
        val budget = h - 2 * pad
        val contentW = w - 2 * pad
        val maxNumeral = when (kind) { WidgetKind.QUIT_COUNTER -> 32; else -> 28 }
        val size = counterSizeDp(contentW, days, maxNumeral)
        return when (kind) {
            WidgetKind.QUIT_COUNTER -> {
                // Everything is centered; no category line, no stage row.
                // header (badge 40 | name, up to 2 lines of 24) then eyebrow 23, counter 6 + 34 (+12 unit labels), longest 27
                val showBadge = w >= 150
                val nameLines = estimateTitleLines(title, contentW - (if (showBadge) BADGE_FULL_DP else 0), NAME_FULL_SP).coerceAtMost(2)
                val header = max(40, nameLines * 24)
                val labels = budget >= header + 56
                val eyebrow = budget >= header + 76
                val longest = eyebrow && budget >= header + 103
                WidgetLayoutPlan(WidgetVariant.FULL, pad, labels, eyebrow, showLongest = longest, showBadge = showBadge, titleMaxLines = nameLines, counterSizeDp = size)
            }
            WidgetKind.GOOD_QUICK_LOG -> goodFull(w, budget, contentW, title, size)
            WidgetKind.EVENT -> {
                // header 36 (category + title) + gap 10 + counter 34; unit labels 12, date 16, counter eyebrow 20
                val labels = budget >= 92
                val date = labels && budget >= 108
                val eyebrow = date && budget >= 128
                WidgetLayoutPlan(WidgetVariant.FULL, pad, labels, eyebrow, showDate = date, titleMaxLines = if (eyebrow) 2 else 1, showBadge = false, counterSizeDp = size)
            }
        }
    }

    /**
     * Good habit, tall widget. Name first: controls beside the name only while the name still fits in two lines
     * (badge dropped before that stops being true); otherwise the controls move below the name (up to 3 lines).
     */
    private fun goodFull(w: Int, budget: Int, contentW: Int, title: String, size: Int): WidgetLayoutPlan {
        val badgeAllowed = w >= 150
        val minusBeside = w >= 140
        val controls = if (minusBeside) CONTROLS_FULL_DP else PLUS_FULL_DP
        val besideWithBadge = estimateTitleLines(title, contentW - controls - BADGE_FULL_DP, NAME_FULL_SP)
        val besideNoBadge = estimateTitleLines(title, contentW - controls, NAME_FULL_SP)

        val plan: WidgetLayoutPlan
        val header: Int
        if (w >= 180 && besideNoBadge <= 2) {
            val badge = badgeAllowed && besideWithBadge <= 2
            val lines = if (badge) besideWithBadge else besideNoBadge
            header = max(44, lines * 24 + 16) // name lines + "today" line, or the 44 dp button
            plan = WidgetLayoutPlan(WidgetVariant.FULL, TIGHT_PADDING, showMinus = minusBeside, showBadge = badge, titleMaxLines = lines, counterSizeDp = size)
        } else {
            val badge = badgeAllowed && estimateTitleLines(title, contentW - BADGE_COMPACT_DP, NAME_COMPACT_SP) <= 2
            val lines = estimateTitleLines(title, contentW - (if (badge) BADGE_COMPACT_DP else 0), NAME_COMPACT_SP).coerceAtMost(3)
            val minus = contentW >= PLUS_STACKED_DP + MINUS_STACKED_DP + MIN_COUNT_DP
            header = lines * 21 + 8 + 40 // name, gap, count + buttons row
            plan = WidgetLayoutPlan(WidgetVariant.FULL, TIGHT_PADDING, showMinus = minus, showBadge = badge, controlsBelow = true, titleMaxLines = lines, counterSizeDp = size)
        }
        // week strip 52 (weekday letters + squares), streak row 28, progress 18 (gap + bar). The week strip has priority:
        // it is the first optional row added, then the day streak, then the progress bar. (When the streak row does not
        // fit, the renderer merges the streak into the line under the name, so it is visible at every size.)
        val week = budget >= header + 52
        val streak = budget >= header + (if (week) 52 else 0) + 28
        val progress = streak && budget >= header + (if (week) 52 else 0) + 28 + 18
        return plan.copy(showWeek = week, showStreakRow = streak, showProgress = progress)
    }

    private fun row(kind: WidgetKind, w: Int, h: Int, days: Long, title: String): WidgetLayoutPlan {
        if (kind == WidgetKind.GOOD_QUICK_LOG) return goodRow(w, h, title)
        val pad = if (h < 64) 8 else 12
        val budget = h - 2 * pad
        val counterW = (w - 2 * pad) / 2 // the counter column gets at most half of the width
        val size = counterSizeDp(counterW, days, if (budget >= 44) 26 else 20)
        return WidgetLayoutPlan(
            variant = WidgetVariant.ROW,
            paddingDp = pad,
            showUnitLabels = budget >= 44,
            showCategory = kind == WidgetKind.EVENT && budget >= 38, // the quit card shows no category
            showDate = kind == WidgetKind.EVENT && budget >= 38,
            showMinus = true,
            counterSizeDp = size,
        )
    }

    /**
     * Good habit, short and wide (about 4x2). The streak row ("N day streak", "N times in total") is kept: it comes
     * before the progress bar when height is scarce. The badge goes before the name would need more than two lines.
     */
    private fun goodRow(w: Int, h: Int, title: String): WidgetLayoutPlan {
        val pad = if (h < 110) 8 else 12 // tighter padding on short widgets keeps room for the streak row
        val budget = h - 2 * pad
        val contentW = w - 2 * pad
        val withBadge = estimateTitleLines(title, contentW - CONTROLS_ROW_DP - BADGE_ROW_DP, NAME_ROW_SP)
        val noBadge = estimateTitleLines(title, contentW - CONTROLS_ROW_DP, NAME_ROW_SP)
        val badge = withBadge <= 2
        val lines = (if (badge) withBadge else noBadge).coerceAtMost(3)
        val top = max(40, lines * 22 + 16) // name lines + "today" line, or the 40 dp button
        val streak = budget >= top + 22
        val progress = budget >= top + 10 + (if (streak) 22 else 0)
        return WidgetLayoutPlan(
            variant = WidgetVariant.ROW,
            paddingDp = pad,
            showStreakRow = streak,
            showProgress = progress,
            showMinus = true,
            showBadge = badge,
            titleMaxLines = lines,
            counterSizeDp = 20,
        )
    }

    private fun slim(kind: WidgetKind, w: Int, h: Int, days: Long, title: String): WidgetLayoutPlan {
        if (kind == WidgetKind.GOOD_QUICK_LOG) return goodSlim(w, h, title)
        val pad = if (h < 64) 8 else 10
        val budget = h - 2 * pad
        val size = counterSizeDp(w - 2 * pad, days, if (budget >= 56) 24 else 18)
        return WidgetLayoutPlan(
            variant = WidgetVariant.SLIM,
            paddingDp = pad,
            showUnitLabels = budget >= 66,
            showMinus = w >= 150,
            showBadge = w >= 90 && budget >= 40,
            counterSizeDp = size,
        )
    }

    /**
     * Good habit, short and narrow. No badge here. Name beside the buttons while it fits in two lines; otherwise
     * (and if the height allows) the buttons drop below the name; otherwise the minus button is given up for room.
     */
    private fun goodSlim(w: Int, h: Int, title: String): WidgetLayoutPlan {
        val pad = if (h < 64) 8 else 10
        val contentW = w - 2 * pad
        val minusBeside = w >= 150
        val beside = estimateTitleLines(title, contentW - (if (minusBeside) CONTROLS_SLIM_DP else PLUS_SLIM_DP), NAME_COMPACT_SP)
        if (beside <= 2) {
            return WidgetLayoutPlan(WidgetVariant.SLIM, pad, showMinus = minusBeside, showBadge = false, showProgress = false, titleMaxLines = beside, counterSizeDp = 18)
        }
        if (h >= 84) {
            val fit = ((h - 56) / 22).coerceIn(1, 3)
            val lines = estimateTitleLines(title, contentW, NAME_COMPACT_SP).coerceAtMost(fit)
            val minus = contentW >= PLUS_STACKED_DP + MINUS_STACKED_DP + MIN_COUNT_DP
            // name lines (21 each) + gap 8 + count/buttons row 40, then week strip 52 first, then streak row 28;
            // when the streak row does not fit it is merged into the sub line
            val top = lines * 21 + 8 + 40
            val week = h - 2 * pad >= top + 52
            val streak = h - 2 * pad >= top + (if (week) 52 else 0) + 28
            return WidgetLayoutPlan(WidgetVariant.SLIM, pad, showWeek = week, showStreakRow = streak, showMinus = minus, showBadge = false, showProgress = false, controlsBelow = true, titleMaxLines = lines, counterSizeDp = 18)
        }
        val lines = estimateTitleLines(title, contentW - PLUS_SLIM_DP, NAME_COMPACT_SP).coerceAtMost(3)
        return WidgetLayoutPlan(WidgetVariant.SLIM, pad, showMinus = false, showBadge = false, showProgress = false, titleMaxLines = lines, counterSizeDp = 18)
    }
}
