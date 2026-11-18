package com.adhamelgendy.nixura.presentation.platform

/**
 * The phone's own language as a BCP 47 tag ("ar-EG", "en-US"), ignoring the language picked for NixUra itself.
 * Used by the "System default" language choice. Null when the platform cannot tell.
 */
expect fun systemLanguageTag(): String?
