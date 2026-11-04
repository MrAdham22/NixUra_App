package com.adhamelgendy.nixura.presentation.components

import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Pure math behind the water-droplet navigation indicator. Positions are "item coordinates": `0f` is the
 * centre of the first item, `1f` the centre of the second, and so on (fractions while swiping).
 */
internal object DropletMath {
    /** The droplet never stretches by more than this many item widths (keeps long jumps readable). */
    const val MAX_STRETCH = 1.6f

    /** How much of its height the droplet loses at full stretch (it gets thinner as it stretches). */
    const val MAX_SQUASH = 0.16f

    /** How much smaller the trailing end becomes at full stretch (the teardrop tail). */
    const val MAX_TAIL_SHRINK = 0.38f

    /** Distance between the leading edge ([position]) and the lagging tail ([trailing]), in item widths. */
    fun stretch(position: Float, trailing: Float): Float = abs(position - trailing).coerceAtMost(MAX_STRETCH)

    /** 0..1 amount used for squash and tail shrink; saturates at one item width of stretch. */
    fun stretchFraction(stretch: Float): Float = stretch.coerceIn(0f, 1f)

    /** How strongly item [index] is selected right now: 1 when the droplet is centred on it, 0 one item away. */
    fun selection(position: Float, index: Int): Float = (1f - abs(position - index)).coerceIn(0f, 1f)

    /** Index of the item the droplet is closest to; used for labels and accessibility state. */
    fun nearestIndex(position: Float, itemCount: Int): Int =
        position.roundToInt().coerceIn(0, (itemCount - 1).coerceAtLeast(0))

    /** Maps an item-space x (in item widths) to pixels, mirroring for right-to-left layouts. */
    fun toPixels(itemX: Float, itemWidthPx: Float, totalWidthPx: Float, rtl: Boolean): Float {
        val x = (itemX + 0.5f) * itemWidthPx
        return if (rtl) totalWidthPx - x else x
    }
}
