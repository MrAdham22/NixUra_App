package com.adhamelgendy.nixura.presentation.appearance

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import com.adhamelgendy.nixura.domain.theme.HabitTheme
import com.adhamelgendy.nixura.presentation.theme.NixColors
import com.adhamelgendy.nixura.presentation.theme.NixUraPalette

/**
 * Colors of a good-habit card at a fill level (0..1). One function for the in-app card and the home-screen widget,
 * so both always match. [background] is the card's average tone (what the content must read on).
 */
data class GoodFillColors(
    val background: Color,
    val content: Color,
    val contentMuted: Color,
    /** Color of a finished day square. */
    val doneSquare: Color,
    /** The liquid, for water habits: surface and depth. */
    val liquidSurface: Color,
    val liquidDepth: Color,
    /**
     * A plain habit's card, top-left to bottom-right: a diagonal emerald gradient that deepens as today's count
     * grows, like the quit cards' streak gradient (DESIGN.md: lit from within). Equal to [background] for water.
     */
    val gradientStart: Color = background,
    val gradientEnd: Color = background,
)

/**
 * The emerald family of a plain good habit: deep, rich and calm, instead of a flat mint slab. A finished day ends on
 * a deep emerald gradient that carries warm-white text, as a lit quit card does.
 */
val EmeraldDeep = Color(0xFF0F5A42)
val EmeraldMid = Color(0xFF1B7F5B)
val EmeraldLight = Color(0xFF248A61)

fun goodFillColors(fill: Float, water: Boolean, c: NixColors): GoodFillColors {
    val f = fill.coerceIn(0f, 1f)
    if (!water) {
        // Light theme keeps the gradient's two ends close in brightness, so one ink reads on all of it while the card
        // passes from white to emerald; dark theme can afford the deeper corner.
        val start = lerp(c.surface, if (c.isDark) EmeraldDeep else EmeraldMid, f)
        val middle = lerp(c.surface, if (c.isDark) EmeraldMid else lerp(EmeraldMid, EmeraldLight, 0.5f), f)
        val end = lerp(c.surface, EmeraldLight, f)
        // Text takes the ink that reads best on the whole gradient (its weakest stop), not only on the middle:
        // in light theme a half-full card crosses the point where light and dark text tie.
        val stops = listOf(start, middle, end)
        val lightWorst = stops.minOf { contrastRatio(CardInkLight, it) }
        val darkWorst = stops.minOf { contrastRatio(CardInkDark, it) }
        val ink = if (lightWorst >= darkWorst) {
            CardInk(CardInkLight, CardInkLight.copy(alpha = 0.86f))
        } else {
            CardInk(CardInkDark, CardInkDark.copy(alpha = 0.80f))
        }
        val done = if (f >= 0.6f) ink.primary else c.positive
        return GoodFillColors(middle, ink.primary, ink.muted, done, c.water, c.waterDeep, start, end)
    }
    val full = lerp(c.water, c.waterDeep, 0.55f)
    val background = lerp(c.surface, full, f)
    val onDark = background.prefersLightContentOnCard()
    // On water the blush text loses contrast against blue, so light text there is near-white.
    val content = if (onDark) WaterInk else NixUraPalette.onLightPrimary
    val muted = content.copy(alpha = if (onDark) 0.78f else 0.66f)
    // Same blue as the calendar's water days (deeper on the light theme, so a day square reads on white).
    val done = if (f >= 0.6f) content else HabitThemes.accentFor(HabitTheme.WATER, c)
    return GoodFillColors(background, content, muted, done, c.water, c.waterDeep)
}

private val WaterInk = Color(0xFFF7FAFF)
