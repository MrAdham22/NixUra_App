package com.example.reclaimapp.widgets

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.Context
import android.os.SystemClock
import android.util.TypedValue
import android.view.View
import android.widget.RemoteViews
import androidx.compose.ui.graphics.toArgb
import com.example.reclaimapp.R
import com.example.reclaimapp.domain.calculator.DayStatus
import com.example.reclaimapp.domain.coach.CoachEngine
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
import com.example.reclaimapp.domain.widget.WidgetStrings
import com.example.reclaimapp.domain.widget.WidgetVariant
import com.example.reclaimapp.presentation.appearance.StreakColorMapper
import com.example.reclaimapp.presentation.appearance.StreakColorProgressionConfig
import com.example.reclaimapp.presentation.appearance.prefersLightContentOnCard
import com.example.reclaimapp.presentation.components.NixIcon
import com.example.reclaimapp.presentation.components.badGlyphFor
import com.example.reclaimapp.presentation.components.goodGlyphFor
import com.example.reclaimapp.presentation.strings.Strings
import com.example.reclaimapp.presentation.strings.stringsFor
import com.example.reclaimapp.presentation.util.formatDateTime
import com.example.reclaimapp.presentation.util.formatElapsed
import kotlinx.coroutines.CancellationException
import org.koin.mp.KoinPlatform
import kotlin.time.Duration.Companion.milliseconds

/** A finished widget view plus the instant at which it must be re-rendered (`null` = only on data change / midnight). */
internal class RenderedWidget(val views: RemoteViews, val nextChangeAtEpochMillis: Long?)

/**
 * Builds the RemoteViews for one widget instance so that it looks like the matching in-app card
 * (`BadHabitCard`, `GoodHabitCard`, `EventCard`): same colors, corner radius, glyph badge, counter blocks with unit
 * labels, stage dots, progress bar, week strip and streak row.
 *
 * The running clock is a system-driven Chronometer, so it ticks with no process and no alarm. A re-render is only
 * needed at [RenderedWidget.nextChangeAtEpochMillis], at local midnight, or when the data changes.
 *
 * Size handling: [WidgetLayoutPlanner] decides the layout family and which optional rows fit; titles are never dropped.
 */
internal object WidgetRenderer {

    private class Env(
        val context: Context,
        val widgetId: Int,
        val s: Strings,
        val ws: WidgetStrings,
        val c: WidgetColors,
        val widthDp: Int,
        val heightDp: Int,
    )

    suspend fun render(context: Context, widgetId: Int, kind: WidgetKind): RenderedWidget {
        val appearance = loadAppearance()
        val s = stringsFor(appearance.languageCode)
        val ws = WidgetStrings.forLanguage(appearance.languageCode)
        val c = WidgetTheme.of(appearance.darkTheme)
        val (w, h) = sizeDp(context, widgetId)
        val env = Env(context, widgetId, s, ws, c, w, h)
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
            message(env, ws.widgetError, ws.widgetTapToOpen, WidgetIntents.open(context, widgetId, WidgetIntents.TYPE_HOME))
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
        QuitWidgetState.NoHabits -> message(env, env.ws.widgetNoQuitHabits, env.ws.widgetTapToAdd, WidgetIntents.open(env.context, env.widgetId, WidgetIntents.TYPE_ADD_QUIT))
        QuitWidgetState.NotConfigured -> message(env, env.ws.widgetTitleQuit, env.ws.widgetTapToChoose, WidgetIntents.configure(env.context, env.widgetId))
        QuitWidgetState.HabitMissing -> message(env, env.ws.widgetHabitRemoved, env.ws.widgetTapToChooseAnother, WidgetIntents.configure(env.context, env.widgetId))
        is QuitWidgetState.Active -> quitCard(env, state)
    }

    private fun quitCard(env: Env, st: QuitWidgetState.Active): RenderedWidget {
        val plan = WidgetLayoutPlanner.plan(WidgetKind.QUIT_COUNTER, env.widthDp, env.heightDp, st.clock.days)
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
            val category = runCatching { KoinPlatform.getKoin().get<CoachEngine>().categoryFor(st.name).label }.getOrDefault("")
            text(rv, R.id.w_sub, category, muted)
            show(rv, R.id.w_sub, plan.showCategory && category.isNotEmpty())
            show(rv, R.id.w_chevron, plan.showBadge)
            rv.setInt(R.id.w_chevron, "setColorFilter", muted)
            text(rv, R.id.w_eyebrow, env.s.clearFor.uppercase(), muted)
            show(rv, R.id.w_eyebrow, plan.showCounterEyebrow)
            // stage dots, like ProgressStageIndicator
            val stageCount = StreakColorProgressionConfig.Default.stopCount
            val stageIndex = mapper.stageIndexFor(elapsed)
            val dots = intArrayOf(R.id.w_dot0, R.id.w_dot1, R.id.w_dot2, R.id.w_dot3, R.id.w_dot4, R.id.w_dot5)
            dots.forEachIndexed { i, id ->
                show(rv, id, i < stageCount)
                if (i <= stageIndex) tint(rv, id, primary) else tint(rv, id, mutedBase, (Color255(muted) * 0.45f).toInt())
            }
            text(rv, R.id.w_stage_text, "Stage ${stageIndex + 1}/$stageCount", muted)
            show(rv, R.id.w_stage_row, plan.showStage)
            text(rv, R.id.w_longest, env.s.longestLabel(formatElapsed(st.longestStreakMillis.milliseconds)), muted)
            show(rv, R.id.w_longest, plan.showLongest)
        } else if (plan.variant == WidgetVariant.ROW) {
            val category = runCatching { KoinPlatform.getKoin().get<CoachEngine>().categoryFor(st.name).label }.getOrDefault("")
            text(rv, R.id.w_sub, category, muted)
            show(rv, R.id.w_sub, plan.showCategory && category.isNotEmpty())
        }
        counter(env, rv, plan, st.clock, countDown = false, valueColor = primary, labelColor = muted)
        rv.setOnClickPendingIntent(R.id.w_root, WidgetIntents.open(env.context, env.widgetId, WidgetIntents.TYPE_QUIT, st.habitId))
        return RenderedWidget(rv, st.nextChangeAtEpochMillis)
    }

    // ===================== good habit quick log (GoodHabitCard) =====================

    private fun good(env: Env, state: GoodWidgetState): RenderedWidget = when (state) {
        GoodWidgetState.NoHabits -> message(env, env.ws.widgetNoGoodHabits, env.ws.widgetTapToAdd, WidgetIntents.open(env.context, env.widgetId, WidgetIntents.TYPE_ADD_GOOD))
        GoodWidgetState.NotConfigured -> message(env, env.ws.widgetTitleBuild, env.ws.widgetTapToChoose, WidgetIntents.configure(env.context, env.widgetId))
        GoodWidgetState.HabitMissing -> message(env, env.ws.widgetHabitRemoved, env.ws.widgetTapToChooseAnother, WidgetIntents.configure(env.context, env.widgetId))
        is GoodWidgetState.Active -> goodCard(env, state)
    }

    private fun goodCard(env: Env, st: GoodWidgetState.Active): RenderedWidget {
        val c = env.c
        val plan = WidgetLayoutPlanner.plan(WidgetKind.GOOD_QUICK_LOG, env.widthDp, env.heightDp)
        val narrowFull = plan.variant == WidgetVariant.FULL && env.widthDp < 180
        val layout = when (plan.variant) {
            WidgetVariant.FULL -> if (narrowFull) R.layout.widget_good_full_narrow else R.layout.widget_good_full
            WidgetVariant.ROW -> R.layout.widget_good_row
            WidgetVariant.SLIM -> R.layout.widget_good_slim
        }
        val rv = RemoteViews(env.context.packageName, layout)
        val tone = if (st.done) c.positive else c.accent

        tint(rv, R.id.w_bg, c.surface)
        tint(rv, R.id.w_border, c.border, c.borderAlpha)
        val pad = px(env, plan.paddingDp)
        rv.setViewPadding(R.id.w_content, pad, pad, pad, pad)

        text(rv, R.id.w_name, st.name, c.textPrimary)
        if (plan.variant != WidgetVariant.FULL || !narrowFull) {
            text(rv, R.id.w_sub, env.s.todayProgress(st.count, st.target), if (st.done) c.positive else c.textSecondary)
        } else {
            text(rv, R.id.w_sub, env.s.todayProgress(st.count, st.target), if (st.done) c.positive else c.textSecondary)
            text(rv, R.id.w_count, "${st.count}/${st.target}", if (st.done) c.positive else c.textPrimary)
            show(rv, R.id.w_sub, false) // narrow layout shows the count on the button row instead
        }
        if (plan.variant == WidgetVariant.FULL || plan.variant == WidgetVariant.ROW) {
            badge(env, rv, plan.copy(showBadge = plan.variant == WidgetVariant.ROW || plan.showBadge), glyphRes(goodGlyphFor(st.habitId)), tone, 36)
        }

        // buttons: minus (outlined circle) and plus (filled circle, tick when a once-a-day habit is done)
        tint(rv, R.id.w_minus_bg, c.surface)
        tint(rv, R.id.w_minus_bd, c.border, c.borderAlpha)
        tint(rv, R.id.w_minus_icon, c.textSecondary)
        show(rv, R.id.w_minus, plan.showMinus)
        tint(rv, R.id.w_plus_bg, tone)
        rv.setImageViewResource(R.id.w_plus_icon, if (st.done && st.target == 1) R.drawable.ic_w_tick else R.drawable.ic_w_plus)
        tint(rv, R.id.w_plus_icon, c.onAccent)
        rv.setContentDescription(R.id.w_plus, env.ws.widgetAddOne)
        rv.setContentDescription(R.id.w_minus, env.ws.widgetRemoveOne)
        rv.setOnClickPendingIntent(R.id.w_plus, WidgetIntents.adjust(env.context, env.widgetId, st.habitId, increment = true))
        rv.setOnClickPendingIntent(R.id.w_minus, WidgetIntents.adjust(env.context, env.widgetId, st.habitId, increment = false))

        // progress bar (FULL always; ROW when there is a spare line)
        if (plan.variant != WidgetVariant.SLIM) {
            val showProgress = plan.variant == WidgetVariant.FULL || plan.showUnitLabels
            show(rv, R.id.w_prog, showProgress)
            tint(rv, R.id.w_prog_track, c.surfaceRaised)
            tint(rv, R.id.w_prog_fill, tone)
            rv.setInt(R.id.w_prog_fill, "setImageLevel", (st.progress * 10_000).toInt())
        }
        if (plan.variant == WidgetVariant.FULL) {
            // week strip: same colors as DaySquare
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
                    day.isToday -> tint(rv, bd[i], c.textSecondary)
                    day.status == DayStatus.PARTIAL && day.enabled -> tint(rv, bd[i], c.positive, 140)
                    !day.enabled -> tint(rv, bd[i], c.border, c.borderAlpha / 2)
                    else -> tint(rv, bd[i], c.border, 0)
                }
            }
            show(rv, R.id.w_week, plan.showWeek)
            // streak row
            tint(rv, R.id.w_flame, c.accentWarm)
            text(rv, R.id.w_streak_text, env.s.streakDays(st.currentStreakDays), c.textSecondary)
            text(rv, R.id.w_total_text, env.s.timesDoneTotal(st.totalTimesDone), c.textMuted)
            show(rv, R.id.w_streak_row, plan.showStreakRow)
        }
        rv.setOnClickPendingIntent(R.id.w_root, WidgetIntents.open(env.context, env.widgetId, WidgetIntents.TYPE_GOOD, st.habitId))
        return RenderedWidget(rv, null) // changes only with data or at local midnight
    }

    // ===================== events (EventCard) =====================

    private fun event(env: Env, state: EventWidgetState): RenderedWidget = when (state) {
        EventWidgetState.NoEvents -> message(env, env.ws.widgetNoEvents, env.ws.widgetTapToAdd, WidgetIntents.open(env.context, env.widgetId, WidgetIntents.TYPE_ADD_EVENT))
        EventWidgetState.EventMissing -> message(env, env.ws.widgetEventRemoved, env.ws.widgetTapToChooseAnother, WidgetIntents.configure(env.context, env.widgetId))
        is EventWidgetState.Upcoming -> eventCard(env, state.title, state.category, state.targetAtEpochMillis, state.reminderCount, state.clock, false, state.nextChangeAtEpochMillis)
        is EventWidgetState.Passed -> eventCard(env, state.title, state.category, state.targetAtEpochMillis, state.reminderCount, state.clock, true, state.nextChangeAtEpochMillis)
    }

    private fun eventCard(
        env: Env,
        title: String,
        category: com.example.reclaimapp.domain.model.EventCategory,
        targetAt: Long,
        reminders: Int,
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
            val date = dayCalc?.let { formatDateTime(targetAt, it, env.s) } ?: ""
            text(rv, R.id.w_eyebrow_cat, env.s.categoryLabel(category).uppercase(), if (passed) c.textMuted else c.accent)
            show(rv, R.id.w_eyebrow_cat, plan.variant == WidgetVariant.FULL || plan.showCategory)
            text(rv, R.id.w_date, date, c.textMuted)
            show(rv, R.id.w_date, plan.showDate && date.isNotEmpty())
        }
        if (plan.variant == WidgetVariant.FULL) {
            val showBell = reminders > 0 && env.widthDp >= 180
            show(rv, R.id.w_bell_pill, showBell)
            tint(rv, R.id.w_bell_bg, c.surfaceRaised)
            tint(rv, R.id.w_bell_icon, c.accentWarm)
            text(rv, R.id.w_bell_count, reminders.toString(), c.textSecondary)
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
        rv.setChronometer(R.id.w_clock, base, null, true)

        show(rv, R.id.w_clock_labels, plan.showUnitLabels)
        show(rv, R.id.w_lbl_h, clock.showsHours)
        text(rv, R.id.w_lbl_h, env.s.unitHours.uppercase(), labelColor)
        text(rv, R.id.w_lbl_m, env.s.unitMinutes.uppercase(), labelColor)
        text(rv, R.id.w_lbl_s, env.s.unitSeconds.uppercase(), labelColor)
    }

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

    private fun Color255(argb: Int): Int = (argb ushr 24) and 0xFF
}
