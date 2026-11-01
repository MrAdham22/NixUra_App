package com.example.reclaimapp.widgets

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.Context
import android.graphics.Paint
import android.graphics.Typeface
import android.os.SystemClock
import android.util.TypedValue
import android.view.View
import android.widget.RemoteViews
import androidx.compose.ui.graphics.toArgb
import com.example.reclaimapp.R
import com.example.reclaimapp.domain.calculator.DayStatus
import com.example.reclaimapp.domain.time.DayCalculator
import com.example.reclaimapp.domain.usecase.LoadWidgetStateUseCase
import com.example.reclaimapp.domain.widget.EventWidgetState
import com.example.reclaimapp.domain.widget.GoodWidgetState
import com.example.reclaimapp.domain.widget.QuitWidgetState
import com.example.reclaimapp.domain.widget.WidgetAppearance
import com.example.reclaimapp.domain.widget.WidgetClockSplit
import com.example.reclaimapp.domain.widget.WidgetKind
import com.example.reclaimapp.domain.widget.WidgetLayoutPlan
import com.example.reclaimapp.domain.widget.WidgetLayoutPlanner
import com.example.reclaimapp.domain.widget.WidgetVariant
import com.example.reclaimapp.presentation.appearance.StreakColorMapper
import com.example.reclaimapp.presentation.appearance.prefersLightContentOnCard
import com.example.reclaimapp.presentation.components.NixIcon
import com.example.reclaimapp.presentation.components.badGlyphFor
import com.example.reclaimapp.presentation.components.goodGlyphFor
import com.example.reclaimapp.presentation.strings.Strings
import com.example.reclaimapp.presentation.strings.stringsFor
import com.example.reclaimapp.presentation.util.formatDateTime
import com.example.reclaimapp.presentation.util.formatElapsed
import kotlinx.coroutines.CancellationException
import kotlin.math.roundToInt
import org.koin.mp.KoinPlatform
import kotlin.time.Duration.Companion.milliseconds

/** A finished widget view plus the instant at which it must be re-rendered (`null` = only on data change / midnight). */
internal class RenderedWidget(val views: RemoteViews, val nextChangeAtEpochMillis: Long?)

/**
 * Builds the RemoteViews for one widget instance so that it looks like the matching in-app card
 * (`BadHabitCard`, `GoodHabitCard`, `EventCard`): same colors, corner radius, glyph badge, counter blocks with unit
 * labels, progress bar, week strip and streak row. The quit card is centered and shows no category or stage row;
 * the events card shows no reminder count; the good-habit week strip and streak row are shown at every size.
 *
 * The running clock is a system-driven Chronometer, so it ticks with no process and no alarm. A re-render is only
 * needed at [RenderedWidget.nextChangeAtEpochMillis], at local midnight, or when the data changes.
 *
 * Size handling: [WidgetLayoutPlanner] decides the layout family and which optional rows fit; titles are never dropped.
 */
internal object WidgetRenderer {
    /** Must match android:letterSpacing of the Chronometer in the counter layouts (tools/gen_widget_layouts.py). */
    private const val CLOCK_LETTER_SPACING = 0.1f


    private class Env(
        val context: Context,
        val widgetId: Int,
        val s: Strings,
        val c: WidgetColors,
        val widthDp: Int,
        val heightDp: Int,
        val darkTheme: Boolean,
    )

    suspend fun render(context: Context, widgetId: Int, kind: WidgetKind): RenderedWidget {
        val appearance = loadAppearance()
        val s = stringsFor(appearance.languageCode)
        val c = WidgetTheme.of(appearance.darkTheme)
        val (w, h) = sizeDp(context, widgetId)
        val env = Env(context, widgetId, s, c, w, h, appearance.darkTheme)
        return try {
            val loader = KoinPlatform.getKoin().get<LoadWidgetStateUseCase>()
            val target = WidgetPrefs.targetOf(context, widgetId)
            when (kind) {
                WidgetKind.QUIT_COUNTER -> quit(env, loader.quit(target))
                WidgetKind.GOOD_QUICK_LOG -> good(env, loader.good(target))
                WidgetKind.EVENT -> event(env, loader.event(target))
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            message(env, s.widgetError, s.widgetTapToOpen, WidgetIntents.open(context, widgetId, WidgetIntents.TYPE_HOME))
        }
    }

    private suspend fun loadAppearance(): WidgetAppearance = try {
        KoinPlatform.getKoin().get<LoadWidgetStateUseCase>().appearance()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        WidgetAppearance()
    }

    /** Conservative (minimum) size, so the layout fits in both portrait and landscape. */
    private fun sizeDp(context: Context, widgetId: Int): Pair<Int, Int> {
        val o = AppWidgetManager.getInstance(context).getAppWidgetOptions(widgetId)
        val w = o.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 0).takeIf { it > 0 } ?: 160
        val h = o.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 0).takeIf { it > 0 } ?: 120
        return w to h
    }

    // ===================== quit counter (BadHabitCard) =====================

    private fun quit(env: Env, state: QuitWidgetState): RenderedWidget = when (state) {
        QuitWidgetState.NoHabits -> message(env, env.s.widgetNoQuitHabits, env.s.widgetTapToAdd, WidgetIntents.open(env.context, env.widgetId, WidgetIntents.TYPE_ADD_QUIT))
        QuitWidgetState.NotConfigured -> message(env, env.s.widgetTitleQuit, env.s.widgetTapToChoose, WidgetIntents.configure(env.context, env.widgetId))
        QuitWidgetState.HabitMissing -> message(env, env.s.widgetHabitRemoved, env.s.widgetTapToChooseAnother, WidgetIntents.configure(env.context, env.widgetId))
        is QuitWidgetState.Active -> quitCard(env, state)
    }

    private fun quitCard(env: Env, st: QuitWidgetState.Active): RenderedWidget {
        val plan = WidgetLayoutPlanner.plan(WidgetKind.QUIT_COUNTER, env.widthDp, env.heightDp, st.clock.days, st.name)
        val layout = when (plan.variant) {
            WidgetVariant.FULL -> R.layout.widget_bad_full
            WidgetVariant.ROW -> R.layout.widget_bad_row
            WidgetVariant.SLIM -> R.layout.widget_bad_slim
        }
        val rv = RemoteViews(env.context.packageName, layout)
        val elapsed = st.elapsedMillis.milliseconds
        val mapper = StreakColorMapper()
        val card = mapper.cardBackground(elapsed, st.colorArgb)
        val lightText = card.prefersLightContentOnCard()
        val primary = if (lightText) WidgetTheme.ON_DARK_CARD_PRIMARY else WidgetTheme.ON_LIGHT_CARD_PRIMARY
        val mutedBase = if (lightText) WidgetTheme.ON_DARK_CARD_MUTED else WidgetTheme.ON_LIGHT_CARD_MUTED
        val muted = WidgetTheme.withAlpha(mutedBase, if (lightText) WidgetTheme.MUTED_ON_DARK_ALPHA else WidgetTheme.MUTED_ON_LIGHT_ALPHA)

        tint(rv, R.id.w_bg, card.toArgb())
        tint(rv, R.id.w_border, if (lightText) 0xFFFFFFFF.toInt() else 0xFF000000.toInt(), if (lightText) 36 else 26)
        rv.setViewPadding(R.id.w_content, px(env, plan.paddingDp), px(env, plan.paddingDp), px(env, plan.paddingDp), px(env, plan.paddingDp))

        badge(env, rv, plan, glyphRes(badGlyphFor(st.habitId)), primary, 36)
        text(rv, R.id.w_name, st.name, primary)
        if (plan.variant == WidgetVariant.FULL) {
            rv.setInt(R.id.w_name, "setMaxLines", plan.titleMaxLines)
            text(rv, R.id.w_eyebrow, env.s.clearFor.uppercase(), muted)
            show(rv, R.id.w_eyebrow, plan.showCounterEyebrow)
            text(rv, R.id.w_longest, env.s.longestLabel(formatElapsed(st.longestStreakMillis.milliseconds)), muted)
            show(rv, R.id.w_longest, plan.showLongest)
        }
        counter(env, rv, plan, st.clock, countDown = false, valueColor = primary, labelColor = muted)
        rv.setOnClickPendingIntent(R.id.w_root, WidgetIntents.open(env.context, env.widgetId, WidgetIntents.TYPE_QUIT, st.habitId))
        return RenderedWidget(rv, st.nextChangeAtEpochMillis)
    }

    // ===================== good habit quick log (GoodHabitCard) =====================

    private fun good(env: Env, state: GoodWidgetState): RenderedWidget = when (state) {
        GoodWidgetState.NoHabits -> message(env, env.s.widgetNoGoodHabits, env.s.widgetTapToAdd, WidgetIntents.open(env.context, env.widgetId, WidgetIntents.TYPE_ADD_GOOD))
        GoodWidgetState.NotConfigured -> message(env, env.s.widgetTitleBuild, env.s.widgetTapToChoose, WidgetIntents.configure(env.context, env.widgetId))
        GoodWidgetState.HabitMissing -> message(env, env.s.widgetHabitRemoved, env.s.widgetTapToChooseAnother, WidgetIntents.configure(env.context, env.widgetId))
        is GoodWidgetState.Active -> goodCard(env, state)
    }

    private fun goodCard(env: Env, st: GoodWidgetState.Active): RenderedWidget {
        val c = env.c
        // The planner puts the habit name first: it drops the badge, then moves the buttons below the name, before the
        // name would be squeezed; on short wide widgets (about 4x2) it keeps the streak row.
        val plan = WidgetLayoutPlanner.plan(WidgetKind.GOOD_QUICK_LOG, env.widthDp, env.heightDp, title = st.name)
        val stacked = plan.controlsBelow
        val layout = when {
            stacked -> R.layout.widget_good_full_narrow // buttons under the name; rows the plan hides stay hidden
            plan.variant == WidgetVariant.FULL -> R.layout.widget_good_full
            plan.variant == WidgetVariant.ROW -> R.layout.widget_good_row
            else -> R.layout.widget_good_slim
        }
        val hasBadge = layout != R.layout.widget_good_slim
        val hasProgress = layout != R.layout.widget_good_slim
        val hasWeek = layout == R.layout.widget_good_full || layout == R.layout.widget_good_full_narrow // narrow is also used by SLIM when stacked; the plan then hides the strip
        val hasStreak = layout == R.layout.widget_good_full || layout == R.layout.widget_good_full_narrow || layout == R.layout.widget_good_row
        val rv = RemoteViews(env.context.packageName, layout)
        val tone = if (st.done) c.positive else c.accent

        tint(rv, R.id.w_bg, c.surface)
        tint(rv, R.id.w_border, c.border, c.borderAlpha)
        val pad = px(env, plan.paddingDp)
        rv.setViewPadding(R.id.w_content, pad, pad, pad, pad)

        text(rv, R.id.w_name, st.name, c.textPrimary)
        rv.setInt(R.id.w_name, "setMaxLines", plan.titleMaxLines)
        val progressColor = if (st.done) c.positive else c.textSecondary
        val progressText = env.s.todayProgress(st.count, st.target)
        val streakText = env.s.streakDays(st.currentStreakDays)
        // The day streak is always visible: in its own row when the plan has room for it, otherwise merged into the
        // sub line under the name (the stacked layout shows its count on the button row, so there the sub line is the streak alone).
        val streakInRow = hasStreak && plan.showStreakRow
        text(rv, R.id.w_sub, if (streakInRow) progressText else if (stacked) streakText else "$streakText · $progressText", progressColor)
        if (stacked) {
            text(rv, R.id.w_count, "${st.count}/${st.target}", if (st.done) c.positive else c.textPrimary)
            rv.setTextViewTextSize(R.id.w_count, TypedValue.COMPLEX_UNIT_DIP, if (plan.variant == WidgetVariant.FULL) 24f else 20f)
            show(rv, R.id.w_sub, !streakInRow) // the count is on the button row; the sub line only carries the streak when there is no streak row
        }
        if (hasBadge) {
            badge(env, rv, plan, glyphRes(goodGlyphFor(st.habitId)), tone, 36)
        }

        // buttons: minus (outlined circle) and plus (filled circle, tick when a once-a-day habit is done)
        tint(rv, R.id.w_minus_bg, c.surface)
        tint(rv, R.id.w_minus_bd, c.border, c.borderAlpha)
        tint(rv, R.id.w_minus_icon, c.textSecondary)
        show(rv, R.id.w_minus, plan.showMinus)
        tint(rv, R.id.w_plus_bg, tone)
        rv.setImageViewResource(R.id.w_plus_icon, if (st.done && st.target == 1) R.drawable.ic_w_tick else R.drawable.ic_w_plus)
        tint(rv, R.id.w_plus_icon, c.onAccent)
        rv.setContentDescription(R.id.w_plus, env.s.widgetAddOne)
        rv.setContentDescription(R.id.w_minus, env.s.widgetRemoveOne)
        rv.setOnClickPendingIntent(R.id.w_plus, WidgetIntents.adjust(env.context, env.widgetId, st.habitId, increment = true))
        rv.setOnClickPendingIntent(R.id.w_minus, WidgetIntents.adjust(env.context, env.widgetId, st.habitId, increment = false))

        if (hasProgress) {
            show(rv, R.id.w_prog, plan.showProgress)
            tint(rv, R.id.w_prog_track, c.surfaceRaised)
            tint(rv, R.id.w_prog_fill, tone)
            rv.setInt(R.id.w_prog_fill, "setImageLevel", (st.progress * 10_000).toInt())
        }
        if (hasWeek) {
            // week strip: same colors as DaySquare; today gets a real outline (dedicated drawable, not a tinted stroke)
            val todayFrame = if (env.darkTheme) R.drawable.widget_square_today_on_dark else R.drawable.widget_square_today_on_light
            val letters = env.s.weekdayLetters
            val sq = intArrayOf(R.id.w_wk_sq0, R.id.w_wk_sq1, R.id.w_wk_sq2, R.id.w_wk_sq3, R.id.w_wk_sq4, R.id.w_wk_sq5, R.id.w_wk_sq6)
            val bd = intArrayOf(R.id.w_wk_bd0, R.id.w_wk_bd1, R.id.w_wk_bd2, R.id.w_wk_bd3, R.id.w_wk_bd4, R.id.w_wk_bd5, R.id.w_wk_bd6)
            val lt = intArrayOf(R.id.w_wk_letter0, R.id.w_wk_letter1, R.id.w_wk_letter2, R.id.w_wk_letter3, R.id.w_wk_letter4, R.id.w_wk_letter5, R.id.w_wk_letter6)
            st.week.forEachIndexed { i, day ->
                text(rv, lt[i], letters.getOrElse(day.isoWeekday - 1) { "" }, c.textMuted)
                when {
                    !day.enabled -> tint(rv, sq[i], c.surface, 0)
                    day.status == DayStatus.DONE -> tint(rv, sq[i], c.positive)
                    day.status == DayStatus.PARTIAL -> tint(rv, sq[i], c.positive, c.positiveSoftAlpha)
                    else -> tint(rv, sq[i], c.surfaceRaised)
                }
                when {
                    day.isToday -> {
                        rv.setImageViewResource(bd[i], todayFrame)
                        rv.setViewVisibility(bd[i], View.VISIBLE)
                    }
                    day.status == DayStatus.PARTIAL && day.enabled -> tint(rv, bd[i], c.positive, 140)
                    !day.enabled -> tint(rv, bd[i], c.border, c.borderAlpha / 2)
                    else -> tint(rv, bd[i], c.border, 0)
                }
            }
            show(rv, R.id.w_week, plan.showWeek)
        }
        if (hasStreak) {
            // streak row: "N day streak" + "N times in total"
            tint(rv, R.id.w_flame, c.accentWarm)
            text(rv, R.id.w_streak_text, streakText, c.textSecondary)
            text(rv, R.id.w_total_text, env.s.timesDoneTotal(st.totalTimesDone), c.textMuted)
            show(rv, R.id.w_total_text, env.widthDp >= 200) // on narrow widgets the streak keeps the whole row
            show(rv, R.id.w_streak_row, plan.showStreakRow)
        }
        rv.setOnClickPendingIntent(R.id.w_root, WidgetIntents.open(env.context, env.widgetId, WidgetIntents.TYPE_GOOD, st.habitId))
        return RenderedWidget(rv, null) // changes only with data or at local midnight
    }

    // ===================== events (EventCard) =====================

    private fun event(env: Env, state: EventWidgetState): RenderedWidget = when (state) {
        EventWidgetState.NoEvents -> message(env, env.s.widgetNoEvents, env.s.widgetTapToAdd, WidgetIntents.open(env.context, env.widgetId, WidgetIntents.TYPE_ADD_EVENT))
        EventWidgetState.EventMissing -> message(env, env.s.widgetEventRemoved, env.s.widgetTapToChooseAnother, WidgetIntents.configure(env.context, env.widgetId))
        is EventWidgetState.Upcoming -> eventCard(env, state.title, state.category, state.targetAtEpochMillis, state.clock, false, state.nextChangeAtEpochMillis)
        is EventWidgetState.Passed -> eventCard(env, state.title, state.category, state.targetAtEpochMillis, state.clock, true, state.nextChangeAtEpochMillis)
    }

    private fun eventCard(
        env: Env,
        title: String,
        category: com.example.reclaimapp.domain.model.EventCategory,
        targetAt: Long,
        clock: WidgetClockSplit,
        passed: Boolean,
        nextChange: Long,
    ): RenderedWidget {
        val c = env.c
        val plan = WidgetLayoutPlanner.plan(WidgetKind.EVENT, env.widthDp, env.heightDp, clock.days)
        val layout = when (plan.variant) {
            WidgetVariant.FULL -> R.layout.widget_event_full
            WidgetVariant.ROW -> R.layout.widget_event_row
            WidgetVariant.SLIM -> R.layout.widget_event_slim
        }
        val rv = RemoteViews(env.context.packageName, layout)
        tint(rv, R.id.w_bg, c.surface)
        tint(rv, R.id.w_border, c.border, c.borderAlpha)
        val pad = px(env, plan.paddingDp)
        rv.setViewPadding(R.id.w_content, pad, pad, pad, pad)

        text(rv, R.id.w_name, title, c.textPrimary)
        rv.setInt(R.id.w_name, "setMaxLines", plan.titleMaxLines)
        if (plan.variant != WidgetVariant.SLIM) {
            val dayCalc = runCatching { KoinPlatform.getKoin().get<DayCalculator>() }.getOrNull()
            val date = dayCalc?.let { formatDateTime(targetAt, it, env.s, includeYear = false) } ?: ""
            text(rv, R.id.w_eyebrow_cat, env.s.categoryLabel(category).uppercase(), if (passed) c.textMuted else c.accent)
            show(rv, R.id.w_eyebrow_cat, plan.variant == WidgetVariant.FULL || plan.showCategory)
            text(rv, R.id.w_date, date, c.textMuted)
            show(rv, R.id.w_date, plan.showDate && date.isNotEmpty())
        }
        if (plan.variant == WidgetVariant.FULL) {
            text(rv, R.id.w_eyebrow, (if (passed) env.s.passedSince else env.s.startsIn).uppercase(), c.textMuted)
            show(rv, R.id.w_eyebrow, plan.showCounterEyebrow)
        }
        counter(env, rv, plan, clock, countDown = !passed, valueColor = if (passed) c.textSecondary else c.textPrimary, labelColor = c.textMuted)
        rv.setOnClickPendingIntent(R.id.w_root, WidgetIntents.open(env.context, env.widgetId, WidgetIntents.TYPE_EVENTS))
        return RenderedWidget(rv, nextChange)
    }

    // ===================== shared pieces =====================

    /**
     * Days / hours / minutes / seconds like `CounterBlocks`: whole days are a block of their own; hours:minutes:seconds
     * is one Chronometer (system-driven, ticks by itself) with the unit labels laid out under it. The label row
     * follows what the Chronometer prints (MM:SS in the first hour); `nextChangeAtEpochMillis` re-renders at the switch.
     */
    private fun counter(env: Env, rv: RemoteViews, plan: WidgetLayoutPlan, clock: WidgetClockSplit, countDown: Boolean, valueColor: Int, labelColor: Int) {
        val size = plan.counterSizeDp.toFloat()
        val hasDays = clock.days > 0
        show(rv, R.id.w_days_block, hasDays)
        rv.setTextViewText(R.id.w_days, clock.days.toString())
        rv.setTextColor(R.id.w_days, valueColor)
        rv.setTextViewTextSize(R.id.w_days, TypedValue.COMPLEX_UNIT_DIP, size)
        text(rv, R.id.w_days_label, env.s.unitDays.uppercase(), labelColor)
        show(rv, R.id.w_days_label, plan.showUnitLabels)

        rv.setTextViewTextSize(R.id.w_clock, TypedValue.COMPLEX_UNIT_DIP, size)
        rv.setTextColor(R.id.w_clock, valueColor)
        // Chronometer bases are on the elapsedRealtime clock: count-up started `remainder` ago, countdown ends `remainder` from now.
        val base = SystemClock.elapsedRealtime() + if (countDown) clock.remainderMillis else -clock.remainderMillis
        rv.setChronometerCountDown(R.id.w_clock, countDown)
        // The Chronometer prints "MM:SS" inside the first hour; the "0:" prefix keeps it "H:MM:SS", so the hours field (and
        // its HRS label) is always there, like the in-app counter. From one hour on it prints H:MM:SS by itself (no prefix).
        rv.setChronometer(R.id.w_clock, base, if (clock.showsHours) null else "0:%s", true)

        show(rv, R.id.w_clock_labels, plan.showUnitLabels)
        // The unit labels are centered under their digits using the REAL layout of the clock text: the cells' widths are
        // measured with a Paint set up like the Chronometer (bold sans, tnum, 0.1 em letter spacing, same size) on the
        // string "H:MM:SS", at the boundaries between digits and colons. A colon is wider in context than on its own, so
        // measuring ghost ":" text (the old way) put HRS and MIN too far right. The ghosts in the layout are empty
        // TextViews whose width is their left padding. Falls back to ghost text if the measurement is not possible.
        val ghosts = intArrayOf(R.id.w_gh_h, R.id.w_sp_hm, R.id.w_gh_m, R.id.w_sp_ms, R.id.w_gh_s)
        val widths = clockCellWidthsPx(env, size, clock.hoursDigits)
        if (widths != null) {
            for (i in ghosts.indices) {
                rv.setTextViewText(ghosts[i], "")
                rv.setViewPadding(ghosts[i], widths[i], 0, 0, 0)
            }
            // every digit cell ends with the letter spacing; shift each label left by it so it is centered under the ink
            val ls = (size * CLOCK_LETTER_SPACING * env.context.resources.displayMetrics.density).roundToInt()
            for (id in intArrayOf(R.id.w_lbl_h, R.id.w_lbl_m, R.id.w_lbl_s)) rv.setViewPadding(id, 0, 0, ls, 0)
        } else {
            val fallback = arrayOf("0".repeat(clock.hoursDigits), ":", "00", ":", "00")
            for (i in ghosts.indices) {
                rv.setTextViewTextSize(ghosts[i], TypedValue.COMPLEX_UNIT_DIP, size)
                rv.setTextViewText(ghosts[i], fallback[i])
            }
        }
        text(rv, R.id.w_lbl_h, env.s.unitHours.uppercase(), labelColor)
        text(rv, R.id.w_lbl_m, env.s.unitMinutes.uppercase(), labelColor)
        text(rv, R.id.w_lbl_s, env.s.unitSeconds.uppercase(), labelColor)
    }

    /**
     * Widths in px of the five label cells under the clock text "H:MM:SS" (hours digits, colon, minutes, colon, seconds),
     * taken from the cumulative advances of the shaped string, so colons and digits have their real in-context width.
     */
    private fun clockCellWidthsPx(env: Env, sizeDp: Float, hoursDigits: Int): IntArray? = runCatching {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = Typeface.DEFAULT_BOLD
            textSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, sizeDp, env.context.resources.displayMetrics)
            letterSpacing = CLOCK_LETTER_SPACING
            fontFeatureSettings = "tnum"
        }
        val text = "0".repeat(hoursDigits) + ":00:00"
        val n = text.length
        fun adv(offset: Int) = paint.getRunAdvance(text, 0, n, 0, n, false, offset)
        val edges = floatArrayOf(0f, adv(hoursDigits), adv(hoursDigits + 1), adv(hoursDigits + 3), adv(hoursDigits + 4), adv(n))
        IntArray(5) { i -> (edges[i + 1] - edges[i]).roundToInt().coerceAtLeast(0) }
    }.getOrNull()

    /** Glyph badge: tinted icon on a rounded square of the same color at 14% (as `GlyphBadge`). */
    private fun badge(env: Env, rv: RemoteViews, plan: WidgetLayoutPlan, iconRes: Int, color: Int, @Suppress("UNUSED_PARAMETER") sizeDp: Int) {
        show(rv, R.id.w_badge, plan.showBadge)
        rv.setImageViewResource(R.id.w_badge_icon, iconRes)
        tint(rv, R.id.w_badge_icon, color)
        tint(rv, R.id.w_badge_bg, color, 36)
    }

    private fun glyphRes(icon: NixIcon): Int = when (icon) {
        NixIcon.Flame -> R.drawable.ic_w_flame
        NixIcon.Shield -> R.drawable.ic_w_shield
        NixIcon.Moon -> R.drawable.ic_w_moon
        NixIcon.Sparkle -> R.drawable.ic_w_sparkle
        NixIcon.Leaf -> R.drawable.ic_w_leaf
        NixIcon.Sun -> R.drawable.ic_w_sun
        NixIcon.CircleCheck -> R.drawable.ic_w_check
        NixIcon.Clock -> R.drawable.ic_w_clock
        else -> R.drawable.ic_w_sparkle
    }

    /** Empty, "tap to choose" and error states, drawn as a small card in the same style. */
    fun message(env: Any, title: String, subtitle: String, onClick: PendingIntent): RenderedWidget {
        val e = env as Env
        val rv = RemoteViews(e.context.packageName, R.layout.widget_message)
        tint(rv, R.id.w_bg, e.c.surface)
        tint(rv, R.id.w_border, e.c.border, e.c.borderAlpha)
        text(rv, R.id.w_name, title, e.c.textPrimary)
        text(rv, R.id.w_caption, subtitle, e.c.textSecondary)
        rv.setOnClickPendingIntent(R.id.w_root, onClick)
        return RenderedWidget(rv, null)
    }

    // ---- tiny RemoteViews helpers ----

    private fun tint(rv: RemoteViews, id: Int, color: Int, alpha: Int = 255) {
        rv.setInt(id, "setColorFilter", color or (0xFF shl 24))
        rv.setInt(id, "setImageAlpha", alpha)
    }

    private fun text(rv: RemoteViews, id: Int, value: String, color: Int) {
        rv.setTextViewText(id, value)
        rv.setTextColor(id, color)
    }

    private fun show(rv: RemoteViews, id: Int, visible: Boolean) = rv.setViewVisibility(id, if (visible) View.VISIBLE else View.GONE)

    private fun px(env: Env, dp: Int): Int = (dp * env.context.resources.displayMetrics.density).toInt()

}
