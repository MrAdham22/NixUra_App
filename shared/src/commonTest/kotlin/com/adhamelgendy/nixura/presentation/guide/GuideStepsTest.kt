package com.adhamelgendy.nixura.presentation.guide

import com.adhamelgendy.nixura.presentation.strings.Strings
import kotlin.test.Test
import kotlin.test.assertEquals

class GuideStepsTest {
    private val s = Strings()

    @Test
    fun newUserSeesTheStartCardStepNotTheRings() {
        val step = guideSteps(s, setOf(GuideTarget.StartHere))[1]
        assertEquals(GuideTarget.StartHere, step.target)
        assertEquals(s.guideStartHereTitle, step.title)
    }

    @Test
    fun userWithHabitsSeesTheRingsStep() {
        val step = guideSteps(s, setOf(GuideTarget.Progress))[1]
        assertEquals(GuideTarget.Progress, step.target)
        assertEquals(s.guideScoresTitle, step.title)
    }

    @Test
    fun noTargetIsUsedByTwoSteps() {
        val targets = guideSteps(s, emptySet()).mapNotNull { it.target }
        assertEquals(targets.size, targets.toSet().size)
    }

    @Test
    fun theTourKeepsItsLengthWhicheverHomeCardShows() {
        assertEquals(guideSteps(s, setOf(GuideTarget.Progress)).size, guideSteps(s, setOf(GuideTarget.StartHere)).size)
    }
}
