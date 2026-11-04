package com.adhamelgendy.nixura.presentation.components

import kotlin.test.Test
import kotlin.test.assertEquals

class DropletMathTest {
    @Test
    fun stretchIsDistanceBetweenLeadingEdgeAndTailCappedAtMax() {
        assertEquals(0f, DropletMath.stretch(2f, 2f))
        assertEquals(0.5f, DropletMath.stretch(1.5f, 1f))
        assertEquals(0.5f, DropletMath.stretch(1f, 1.5f)) // direction does not matter
        assertEquals(DropletMath.MAX_STRETCH, DropletMath.stretch(4f, 0f))
    }

    @Test
    fun stretchFractionSaturatesAtOneItem() {
        assertEquals(0.25f, DropletMath.stretchFraction(0.25f))
        assertEquals(1f, DropletMath.stretchFraction(1.4f))
    }

    @Test
    fun selectionIsOneOnTheItemAndFadesToZeroOneItemAway() {
        assertEquals(1f, DropletMath.selection(2f, 2))
        assertEquals(0.5f, DropletMath.selection(2.5f, 2))
        assertEquals(0.5f, DropletMath.selection(2.5f, 3))
        assertEquals(0f, DropletMath.selection(2f, 4))
    }

    @Test
    fun nearestIndexRoundsAndStaysInRange() {
        assertEquals(1, DropletMath.nearestIndex(1.4f, 4))
        assertEquals(2, DropletMath.nearestIndex(1.6f, 4))
        assertEquals(0, DropletMath.nearestIndex(-0.3f, 4))
        assertEquals(3, DropletMath.nearestIndex(7f, 4))
        assertEquals(0, DropletMath.nearestIndex(2f, 0))
    }

    @Test
    fun pixelsMirrorInRightToLeftLayouts() {
        // 4 items in 400px: item 0 centre at 50px (LTR) and 350px (RTL).
        assertEquals(50f, DropletMath.toPixels(0f, 100f, 400f, rtl = false))
        assertEquals(350f, DropletMath.toPixels(0f, 100f, 400f, rtl = true))
        assertEquals(150f, DropletMath.toPixels(1f, 100f, 400f, rtl = false))
    }
}
