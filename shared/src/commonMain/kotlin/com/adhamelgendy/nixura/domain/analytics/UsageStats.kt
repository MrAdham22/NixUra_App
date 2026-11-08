package com.adhamelgendy.nixura.domain.analytics

/**
 * Anonymous app-usage statistics (Firebase Analytics on Android). Collection is off until the user turns it on in
 * Settings > Privacy, and nothing the user typed (habit names, notes, chats) is ever sent.
 */
interface UsageStats {
    fun setCollectionEnabled(enabled: Boolean)
}

/** Platforms without an analytics SDK (iOS for now) collect nothing. */
object NoUsageStats : UsageStats {
    override fun setCollectionEnabled(enabled: Boolean) = Unit
}
