package com.example.reclaimapp.domain.widget

import kotlin.math.floor

/** Which layout file family to use for the current widget size. */
enum class WidgetVariant {
    /** Stacked like the in-app card: header, counter and optional extra rows. Used from [WidgetLayoutPlanner.FULL_MIN_HEIGHT_DP]. */
    FULL,

    /** One horizontal row (badge, title, counter). Short but wide widgets. */
    ROW,

    /** Two compact lines (title, counter). Short and narrow widgets. */
    SLIM,
}

/**
 * What fits in a widget of a given size. The rule: **titles are never dropped**; optional rows are added in priority
 * order only while their height still fits, so nothing is clipped or drawn on top of anything else.
 * Heights are dp estimates of the rows in the layouts under `res/layout/widget_*`.
 */
data class WidgetLayoutPlan(
    val variant: WidgetVariant,
    val paddingDp: Int,
    val showUnitLabels: Boolean = false,
    /** "CLEAR FOR" / "STARTS IN" line above the counter. */
    val showCounterEyebrow: Boolean = false,
    val showCategory: Boolean = false,
    val showStage: Boolean = false,
    val showLongest: Boolean = false,
    val showWeek: Boolean = false,
    val showStreakRow: Boolean = false,
    val showDate: Boolean = false,
    val showMinus: Boolean = true,
    val showBadge: Boolean = true,
    val titleMaxLines: Int = 1,
    /** Counter numeral size in dp (not sp, so a large system font cannot push it out of the widget). */
    val counterSizeDp: Int,
)

object WidgetLayoutPlanner {
    const val FULL_MIN_HEIGHT_DP = 120
    const val ROW_MIN_WIDTH_DP = 220
    private const val FULL_PADDING = 16
    private const val CLOCK_EM = 4.4f // "12:34:56" in a bold tabular font, in em
    private const val DIGIT_EM = 0.62f
    private const val DAYS_GAP_DP = 12

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

    fun plan(kind: WidgetKind, widthDp: Int, heightDp: Int, days: Long = 0): WidgetLayoutPlan {
        val w = widthDp.coerceAtLeast(40)
        val h = heightDp.coerceAtLeast(40)
        return when {
            h >= FULL_MIN_HEIGHT_DP -> full(kind, w, h, days)
            w >= ROW_MIN_WIDTH_DP -> row(kind, w, h, days)
            else -> slim(kind, w, h, days)
        }
    }

    private fun full(kind: WidgetKind, w: Int, h: Int, days: Long): WidgetLayoutPlan {
        val budget = h - 2 * FULL_PADDING
        val contentW = w - 2 * FULL_PADDING
        val maxNumeral = when (kind) { WidgetKind.QUIT_COUNTER -> 32; else -> 28 }
        val size = counterSizeDp(contentW, days, maxNumeral)
        return when (kind) {
            WidgetKind.QUIT_COUNTER -> {
                // header 40 + gap 10 + counter (numerals 34 | with unit labels 46 | with eyebrow 62), then stage 14, longest 17
                val labels = budget >= 96
                val eyebrow = budget >= 112
                val stage = eyebrow && budget >= 136
                val longest = stage && budget >= 163
                WidgetLayoutPlan(WidgetVariant.FULL, FULL_PADDING, labels, eyebrow, showCategory = true, showStage = stage, showLongest = longest, showBadge = w >= 150, counterSizeDp = size)
            }
            WidgetKind.GOOD_QUICK_LOG -> {
                // header 44 + gap 12 + progress 6, then week strip 38, streak row 18
                val week = budget >= 112
                val streak = week && budget >= 142
                WidgetLayoutPlan(WidgetVariant.FULL, FULL_PADDING, showWeek = week, showStreakRow = streak, showMinus = w >= 140, showBadge = w >= 150, counterSizeDp = size)
            }
            WidgetKind.EVENT -> {
                // header 36 (category + title) + gap 10 + counter 34; unit labels 12, date 16, counter eyebrow 20
                val labels = budget >= 92
                val date = labels && budget >= 108
                val eyebrow = date && budget >= 128
                WidgetLayoutPlan(WidgetVariant.FULL, FULL_PADDING, labels, eyebrow, showDate = date, titleMaxLines = if (eyebrow) 2 else 1, showBadge = false, counterSizeDp = size)
            }
        }
    }

    private fun row(kind: WidgetKind, w: Int, h: Int, days: Long): WidgetLayoutPlan {
        val pad = if (h < 64) 8 else 12
        val budget = h - 2 * pad
        val counterW = (w - 2 * pad) / 2 // the counter column gets at most half of the width
        val size = counterSizeDp(counterW, days, if (budget >= 44) 26 else 20)
        return WidgetLayoutPlan(
            variant = WidgetVariant.ROW,
            paddingDp = pad,
            showUnitLabels = budget >= 44,
            showCategory = budget >= 38,
            showDate = budget >= 38,
            showMinus = true,
            counterSizeDp = size,
        )
    }

    private fun slim(kind: WidgetKind, w: Int, h: Int, days: Long): WidgetLayoutPlan {
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
}
