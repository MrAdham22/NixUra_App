package com.adhamelgendy.nixura.presentation.screen

import kotlin.test.Test
import kotlin.test.assertEquals

class HomePhaseTest {

    @Test
    fun anyListStillLoadingMeansLoadingNeverEmpty() {
        assertEquals(HomePhase.Loading, homePhase(quitLoading = true, goodLoading = false, eventsLoading = false, hasAnything = false))
        assertEquals(HomePhase.Loading, homePhase(quitLoading = false, goodLoading = true, eventsLoading = false, hasAnything = true))
        assertEquals(HomePhase.Loading, homePhase(quitLoading = false, goodLoading = false, eventsLoading = true, hasAnything = false))
    }

    @Test
    fun loadedListsAreEmptyOrReady() {
        assertEquals(HomePhase.Empty, homePhase(quitLoading = false, goodLoading = false, eventsLoading = false, hasAnything = false))
        assertEquals(HomePhase.Ready, homePhase(quitLoading = false, goodLoading = false, eventsLoading = false, hasAnything = true))
    }
}
