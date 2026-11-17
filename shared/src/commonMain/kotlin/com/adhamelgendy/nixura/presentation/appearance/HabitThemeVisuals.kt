package com.adhamelgendy.nixura.presentation.appearance

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import com.adhamelgendy.nixura.domain.theme.HabitTheme
import com.adhamelgendy.nixura.presentation.components.NixIcon
import com.adhamelgendy.nixura.presentation.components.icon
import com.adhamelgendy.nixura.presentation.theme.NixColors

/** The background animation a theme plays on the Add Good Habit screen (built in Session 2, see HANDOFF.md). */
enum class HabitThemeAnimationKind { WATER_FILL, GOLDEN_RAYS, FOOTPRINTS, ENERGY_PARTICLES, FLUTTERING_PAGES, NIGHT_SKY, NONE }

/** How a [HabitTheme] looks: the one colour, icon and animation every screen uses for it. */
data class HabitThemeVisual(
    /** Done days, progress and highlights. Water is blue, prayer is gold, every other theme keeps the app's green. */
    val accent: Color,
    val icon: NixIcon,
    val animation: HabitThemeAnimationKind,
)

/**
 * The single place that turns a stored [HabitTheme] into its look. Screens call this instead of checking for water or
 * prayer themselves, so a new theme is added here once.
 */
object HabitThemes {

    /**
     * Calendar and highlight tones. On the navy theme a bright sky blue and a warm honey gold; on the light theme
     * deeper tones, so a full day square stands out from the pale idle square by at least 3:1 (`HabitThemeShadesTest`).
     * The gold leans orange rather than green, so it reads as gold and not mustard.
     */
    val WaterDark = Color(0xFF5BB6EC)
    val WaterLight = Color(0xFF2E78C4)
    val GoldDark = Color(0xFFF1B451)
    val GoldLight = Color(0xFFB5761A)

    fun accentFor(theme: HabitTheme, c: NixColors): Color = when (theme) {
        HabitTheme.WATER -> if (c.isDark) WaterDark else WaterLight
        HabitTheme.PRAYER -> if (c.isDark) GoldDark else GoldLight
        else -> c.positive
    }

    fun visualFor(theme: HabitTheme, c: NixColors): HabitThemeVisual = HabitThemeVisual(
        accent = accentFor(theme, c),
        icon = theme.topic.icon(),
        animation = when (theme) {
            HabitTheme.WATER -> HabitThemeAnimationKind.WATER_FILL
            HabitTheme.PRAYER -> HabitThemeAnimationKind.GOLDEN_RAYS
            HabitTheme.WALK -> HabitThemeAnimationKind.FOOTPRINTS
            HabitTheme.WORKOUT -> HabitThemeAnimationKind.ENERGY_PARTICLES
            HabitTheme.READING -> HabitThemeAnimationKind.FLUTTERING_PAGES
            HabitTheme.SLEEP -> HabitThemeAnimationKind.NIGHT_SKY
            HabitTheme.DEFAULT -> HabitThemeAnimationKind.NONE
        },
    )
}

/**
 * How much of a day was done, as a few discrete steps instead of a continuous ramp (easier to tell apart at a glance):
 * level 0 = nothing, 1..[PARTIAL_LEVELS] = part of the target, [FULL] = target reached.
 * Prayer 1..5 of 5 gives levels 1, 2, 3, 4, 5; water 8 cups gives 1–2 → 1, 3–4 → 2, 5–6 → 3, 7 → 4, 8 → 5.
 */
object DayIntensity {
    const val PARTIAL_LEVELS = 4
    const val FULL = PARTIAL_LEVELS + 1

    /** Levels the legend shows, less → more. */
    val legendLevels: List<Int> = (0..FULL).toList()

    /** [progress] is the share of the day's target, 0..1 (water: `WaterGoal.progress(count)`). */
    fun level(progress: Float): Int = when {
        progress.isNaN() || progress <= 0f -> 0
        progress >= 1f -> FULL
        else -> kotlin.math.ceil(progress * PARTIAL_LEVELS - 1e-4f).toInt().coerceIn(1, PARTIAL_LEVELS)
    }

    /** How far toward the full colour a level is drawn. The first step is already clearly visible. */
    fun fraction(level: Int): Float = when {
        level <= 0 -> 0f
        level >= FULL -> 1f
        else -> 0.24f + (level - 1) * 0.18f // 0.24, 0.42, 0.60, 0.78
    }

    /**
     * The square's colour at a level. Without [partialFrom] the idle square is blended toward the accent (for the
     * small strip on a card, where the idle square is see-through); with it, the partial steps run from
     * [partialFrom] to the accent, so they stay in the accent's colour family.
     */
    fun shade(level: Int, accent: Color, idle: Color, partialFrom: Color? = null): Color = when {
        level <= 0 -> idle
        level >= FULL -> accent
        partialFrom != null -> lerp(partialFrom, accent, (level - 1).toFloat() / PARTIAL_LEVELS)
        else -> lerp(idle, accent, fraction(level))
    }

    /**
     * Where the calendar's partial steps start. On the dark theme the accent dimmed toward black: blending gold into
     * the purple idle square turned the low steps grey-brown. On the light theme the accent faded toward the idle square.
     */
    fun partialFloor(accent: Color, idle: Color, dark: Boolean): Color =
        if (dark) lerp(accent, Color.Black, 0.58f) else lerp(accent, idle, 0.75f)

    /** Day-number ink on a square: whichever of the card inks reads best on it. */
    fun inkOn(square: Color): Color = if (square.prefersLightContentOnCard()) CardInkLight else CardInkDark
}
