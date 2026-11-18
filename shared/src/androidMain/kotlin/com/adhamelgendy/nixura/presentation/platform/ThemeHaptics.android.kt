package com.adhamelgendy.nixura.presentation.platform

import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalView

@Composable
actual fun rememberThemeHaptics(): ThemeHaptics {
    val view = LocalView.current
    return remember(view) { AndroidThemeHaptics(view) }
}

private class AndroidThemeHaptics(private val view: View) : ThemeHaptics {
    override fun pourStart() {
        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
    }

    override fun filled() {
        val constant = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) HapticFeedbackConstants.CONFIRM else HapticFeedbackConstants.LONG_PRESS
        view.performHapticFeedback(constant)
    }
}
