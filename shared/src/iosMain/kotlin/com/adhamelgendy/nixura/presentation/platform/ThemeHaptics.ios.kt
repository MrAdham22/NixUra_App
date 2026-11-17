package com.adhamelgendy.nixura.presentation.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import platform.UIKit.UIImpactFeedbackGenerator
import platform.UIKit.UIImpactFeedbackStyle
import platform.UIKit.UINotificationFeedbackGenerator
import platform.UIKit.UINotificationFeedbackType

// Unverified: iOS is not built on the Windows machine this was written on.
@Composable
actual fun rememberThemeHaptics(): ThemeHaptics = remember { IosThemeHaptics() }

private class IosThemeHaptics : ThemeHaptics {
    private val light = UIImpactFeedbackGenerator(UIImpactFeedbackStyle.UIImpactFeedbackStyleLight)
    private val notify = UINotificationFeedbackGenerator()

    override fun pourStart() {
        light.impactOccurred()
    }

    override fun filled() {
        notify.notificationOccurred(UINotificationFeedbackType.UINotificationFeedbackTypeSuccess)
    }
}
