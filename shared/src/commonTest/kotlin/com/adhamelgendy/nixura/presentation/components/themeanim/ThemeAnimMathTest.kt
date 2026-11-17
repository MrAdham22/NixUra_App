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

    @Test fun footprintTrailAlternatesFeetAndWalksUpThePage() {
        val trail = ThemeAnimMath.footprintTrail(width = 1080f, height = 2400f, top = 0f, stride = 90f, gait = 20f)
        assertTrue(trail.size > 20)
        trail.forEachIndexed { i, print -> assertEquals(i % 2 == 0, print.left) }
        assertTrue(trail.first().y > 2000f) // starts at the bottom
        assertTrue(trail.last().y < 200f) // leaves at the top
        assertTrue(trail.first().x < trail.last().x) // from the start side to the end side
    }

    @Test fun footprintStepsAreEvenAndFeetSitEitherSideOfThePath() {
        val gait = 20f
        val trail = ThemeAnimMath.footprintTrail(width = 1080f, height = 2400f, top = 0f, stride = 90f, gait = gait)
        for (i in 1 until trail.size) {
            val a = trail[i - 1]
            val b = trail[i]
            val d = kotlin.math.sqrt((b.x - a.x) * (b.x - a.x) + (b.y - a.y) * (b.y - a.y))
            // Consecutive prints: one stride along the path, plus at most the step across between the feet.
            assertTrue(d > 90f - 15f && d < 90f + 2 * gait + 5f, "step $i is $d px")
        }
        // Walking straight up (heading 0 degrees), the left foot is left of the right foot.
        val upward = trail.zipWithNext().first { (a, b) -> abs(a.angle) < 25f && abs(b.angle) < 25f }
        val (l, r) = if (upward.first.left) upward else upward.second to upward.first
        assertTrue(l.x < r.x)
    }

    @Test fun printsPressInHoldThenFade() {
        assertEquals(0f, ThemeAnimMath.printAlpha(-0.1f))
        assertEquals(1f, ThemeAnimMath.printAlpha(ThemeAnimMath.PRINT_HOLD_S))
        assertTrue(ThemeAnimMath.printAlpha(ThemeAnimMath.PRINT_HOLD_S + ThemeAnimMath.PRINT_FADE_S / 2) in 0.4f..0.6f)
        assertEquals(0f, ThemeAnimMath.printAlpha(ThemeAnimMath.PRINT_HOLD_S + ThemeAnimMath.PRINT_FADE_S + 0.1f))
        assertEquals(emptyList(), ThemeAnimMath.footprintTrail(0f, 100f, 0f, 10f, 2f))
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
