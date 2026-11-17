package com.adhamelgendy.nixura.presentation.platform

import androidx.compose.runtime.Composable

/** The two moments of the theme backgrounds that the hand should feel. */
interface ThemeHaptics {
    /** A light tick: the water starts pouring. */
    fun pourStart()

    /** A firmer confirm: the page is full. */
    fun filled()
}

/** Platform haptics for the theme backgrounds (Android view haptics, iOS UIFeedbackGenerator). */
@Composable
expect fun rememberThemeHaptics(): ThemeHaptics
