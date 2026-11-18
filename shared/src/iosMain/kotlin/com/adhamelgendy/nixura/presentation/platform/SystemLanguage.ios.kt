package com.adhamelgendy.nixura.presentation.platform

import platform.Foundation.NSLocale
import platform.Foundation.preferredLanguages

// The first preferred language. iOS puts the per-app language (Settings → NixUra → Language) here as well,
// so "System default" also follows that choice. UNVERIFIED: compiled and run only on a Mac.
actual fun systemLanguageTag(): String? = NSLocale.preferredLanguages.firstOrNull() as? String
