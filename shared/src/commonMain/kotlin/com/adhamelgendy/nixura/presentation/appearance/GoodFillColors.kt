package com.adhamelgendy.nixura.presentation.appearance

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
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
)

fun goodFillColors(fill: Float, water: Boolean, c: NixColors): GoodFillColors {
    val full = if (water) lerp(c.water, c.waterDeep, 0.55f) else c.positive
    val background = lerp(c.surface, full, fill.coerceIn(0f, 1f))
    val onDark = background.prefersLightContentOnCard()
    // On water the blush text loses contrast against blue, so light text there is near-white.
    val content = when {
        onDark && water -> WaterInk
        onDark -> NixUraPalette.onDarkPrimary
        else -> NixUraPalette.onLightPrimary
    }
    // The brand's light-card muted tone is plum, which reads pink on green or blue, so use dimmed ink instead
    val muted = if (onDark && !water) NixUraPalette.onDarkMuted else content.copy(alpha = if (onDark) 0.78f else 0.66f)
    val done = if (fill >= 0.6f) content else if (water) c.water else c.positive
    return GoodFillColors(background, content, muted, done, c.water, c.waterDeep)
}

private val WaterInk = Color(0xFFF7FAFF)
