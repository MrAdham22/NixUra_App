package com.adhamelgendy.nixura.presentation.components

import kotlin.test.Test
import kotlin.test.assertEquals

class LoopMathTest {
    private fun near(expected: Float, actual: Float) = assertEquals(expected, actual, 0.0001f)

    @Test
    fun restartLoopsFromZero() {
        near(0f, LoopMath.fraction(0, 1000))
        near(0.25f, LoopMath.fraction(250, 1000))
        near(0.5f, LoopMath.fraction(1500, 1000)) // second pass starts again at 0
    }

    @Test
    fun reverseRunsEverySecondPassBackwards() {
        near(0.25f, LoopMath.fraction(250, 1000, reverse = true))
        near(0.75f, LoopMath.fraction(1250, 1000, reverse = true))
        near(0.25f, LoopMath.fraction(2250, 1000, reverse = true))
    }

    @Test
    fun delayHoldsTheStartOfEachPass() {
        near(0f, LoopMath.fraction(100, 600, delayMillis = 150))
        near(0.5f, LoopMath.fraction(450, 600, delayMillis = 150))
        // Reverse pass: holds at 1 during its delay, then runs back.
        near(1f, LoopMath.fraction(750 + 100, 600, delayMillis = 150, reverse = true))
    }

    @Test
    fun badInputStaysInRange() {
        near(1f, LoopMath.fraction(500, 0))
        near(0f, LoopMath.fraction(-20, 1000))
    }
}
