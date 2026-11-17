package com.adhamelgendy.nixura.presentation.appearance

import androidx.compose.ui.graphics.Color
import com.adhamelgendy.nixura.domain.theme.HabitTheme
import com.adhamelgendy.nixura.presentation.components.NixIcon
import com.adhamelgendy.nixura.presentation.components.icon
import com.adhamelgendy.nixura.presentation.theme.NixColors
import com.adhamelgendy.nixura.presentation.theme.PrayerPalette

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

    fun visualFor(theme: HabitTheme, c: NixColors): HabitThemeVisual = HabitThemeVisual(
        accent = when (theme) {
            HabitTheme.WATER -> c.water
            // Warm gold on the night background; the deeper gold keeps contrast on the light theme's white.
            HabitTheme.PRAYER -> if (c.isDark) PrayerPalette.Gold else PrayerPalette.GoldDeep
            else -> c.positive
        },
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
