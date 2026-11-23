package com.adhamelgendy.nixura.auth

import android.content.Context
import android.os.Bundle
import com.adhamelgendy.nixura.domain.analytics.UsageAction
import com.adhamelgendy.nixura.domain.analytics.UsageStats
import com.google.firebase.analytics.FirebaseAnalytics

/**
 * Firebase Analytics. The manifest keeps it off until [NixUraApplication] reads the stored setting (on by default) and
 * calls this at every start, so nothing is sent before the user's choice is known. While collection is off, Firebase
 * drops logged events. Firebase batches events and sends them about once an hour.
 */
class FirebaseUsageStats(private val context: Context) : UsageStats {
    private val analytics by lazy { FirebaseAnalytics.getInstance(context) }

    override fun setCollectionEnabled(enabled: Boolean) {
        analytics.setAnalyticsCollectionEnabled(enabled)
    }

    override fun logScreen(screen: String) {
        // Compose runs in one Activity, so Firebase's automatic screen tracking only ever sees MainActivity.
        analytics.logEvent(
            FirebaseAnalytics.Event.SCREEN_VIEW,
            Bundle().apply {
                putString(FirebaseAnalytics.Param.SCREEN_NAME, screen)
                putString(FirebaseAnalytics.Param.SCREEN_CLASS, screen)
            },
        )
    }

    override fun logAction(action: UsageAction) {
        analytics.logEvent(action.eventName, null)
    }
}
