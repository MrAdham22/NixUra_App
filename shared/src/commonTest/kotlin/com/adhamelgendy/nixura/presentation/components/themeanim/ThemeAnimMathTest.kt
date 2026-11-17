package com.adhamelgendy.nixura.presentation.components.themeanim

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ThemeAnimMathTest {

    @Test fun waveStaysWithinItsAmplitude() {
        val amp = 10f
        for (i in 0..200) {
            val x = i * 5f
            for (t in listOf(0f, 0.7f, 3.3f, 120f)) {
                // Main wave plus one at 45 percent: never more than 1.45 times the amplitude when level.
                assertTrue(abs(ThemeAnimMath.waveOffset(x, 1000f, t, amp)) <= amp * 1.45f + 0.001f)
            }
        }
        assertEquals(0f, ThemeAnimMath.waveOffset(10f, 0f, 1f, amp))
    }

    @Test fun tiltSlopesTheSurfaceTowardsTheLowerSide() {
        val right = ThemeAnimMath.waveOffset(1000f, 1000f, 0f, 10f, tilt = 1f) - ThemeAnimMath.waveOffset(1000f, 1000f, 0f, 10f)
        val left = ThemeAnimMath.waveOffset(0f, 1000f, 0f, 10f, tilt = 1f) - ThemeAnimMath.waveOffset(0f, 1000f, 0f, 10f)
        assertTrue(right > 0f) // lower on screen (larger y) on the right
        assertTrue(left < 0f)
    }

    @Test fun surfaceRisesFromBottomToTopWithHeadroom() {
        assertEquals(1000f, ThemeAnimMath.surfaceY(0f, 0f, 1000f, 10f))
        assertEquals(10f, ThemeAnimMath.surfaceY(1f, 0f, 1000f, 10f))
        assertEquals(505f, ThemeAnimMath.surfaceY(0.5f, 0f, 1000f, 10f))
        assertEquals(10f, ThemeAnimMath.surfaceY(2f, 0f, 1000f, 10f))
    }

    @Test fun particlesStayInTheirRanges() {
        for (i in 0 until 40) {
            for (t in listOf(0f, 1.3f, 17f, 999f)) {
                val b = ThemeAnimMath.bubble(i, t)
                assertTrue(b.rise in 0f..1f && b.x in 0f..1f && b.radius in 0f..1f)
                val s = ThemeAnimMath.spark(i, t)
                assertTrue(s.x in 0f..1f && s.life in 0f..1f)
                assertTrue(ThemeAnimMath.droplet(i, t).alpha in 0f..1f)
                assertTrue(ThemeAnimMath.twinkle(i, t) in 0.25f..1f)
                assertTrue(ThemeAnimMath.seeded(i, 3) in 0f..1f)
            }
        }
    }

    @Test fun particlesAreStableForTheSameTime() {
        assertEquals(ThemeAnimMath.bubble(3, 2.5f), ThemeAnimMath.bubble(3, 2.5f))
        assertEquals(ThemeAnimMath.page(5, 9f), ThemeAnimMath.page(5, 9f))
    }

    @Test fun footprintsAlternateFeetAndWalkUpThePage() {
        val first = ThemeAnimMath.footprint(0, 14)
        val second = ThemeAnimMath.footprint(1, 14)
        val last = ThemeAnimMath.footprint(13, 14)
        assertTrue(first.left)
        assertTrue(!second.left)
        assertTrue(last.y < first.y)
        assertEquals(first, ThemeAnimMath.footprint(14, 14)) // the walk loops
    }

    @Test fun fishSwimsOnceThenLeaves() {
        assertNull(ThemeAnimMath.fishProgress(0f))
        val mid = assertNotNull(ThemeAnimMath.fishProgress(ThemeAnimMath.FISH_DELAY_S + ThemeAnimMath.FISH_SWIM_S / 2))
        assertTrue(abs(mid - 0.5f) < 0.01f)
        assertNull(ThemeAnimMath.fishProgress(ThemeAnimMath.FISH_DELAY_S + ThemeAnimMath.FISH_SWIM_S + 1f))
    }

    @Test fun ripplesGrowAndFadeThenEnd() {
        val (grow0, fade0) = assertNotNull(ThemeAnimMath.ripple(0f))
        assertEquals(0f, grow0)
        assertEquals(1f, fade0)
        val (growLate, fadeLate) = assertNotNull(ThemeAnimMath.ripple(ThemeAnimMath.RIPPLE_LIFE_S * 0.9f))
        assertTrue(growLate > 0.9f && fadeLate < 0.2f)
        assertNull(ThemeAnimMath.ripple(ThemeAnimMath.RIPPLE_LIFE_S + 0.1f))
    }
}
