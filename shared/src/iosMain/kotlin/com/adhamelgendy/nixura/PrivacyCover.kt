package com.adhamelgendy.nixura

import com.adhamelgendy.nixura.domain.usecase.ObserveAnyNameHiddenUseCase

/**
 * iOS takes the app switcher's picture when the app goes inactive: `ContentView` covers the screen then while any
 * quit habit's name is hidden (the iOS side of Android's recents preview). UNVERIFIED: written on Windows.
 */
object PrivacyCover {
    /** On until the first read, like Android. */
    var coversAppSwitcher: Boolean = true
        private set

    internal suspend fun follow(anyNameHidden: ObserveAnyNameHiddenUseCase) {
        anyNameHidden().collect { coversAppSwitcher = it }
    }
}
