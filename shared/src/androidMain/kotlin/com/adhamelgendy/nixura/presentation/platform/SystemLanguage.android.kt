package com.adhamelgendy.nixura.presentation.platform

import android.content.res.Resources

// Resources.getSystem() carries the device configuration, not the per-app language the app may have set.
actual fun systemLanguageTag(): String? =
    runCatching { Resources.getSystem().configuration.locales.get(0)?.toLanguageTag() }.getOrNull()
