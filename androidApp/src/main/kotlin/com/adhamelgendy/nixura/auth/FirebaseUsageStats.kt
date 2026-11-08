package com.adhamelgendy.nixura.auth

import android.content.Context
import com.adhamelgendy.nixura.domain.analytics.UsageStats
import com.google.firebase.analytics.FirebaseAnalytics

/** Firebase Analytics, off by default (manifest); the choice made here is kept by Firebase across restarts. */
class FirebaseUsageStats(private val context: Context) : UsageStats {
    override fun setCollectionEnabled(enabled: Boolean) {
        FirebaseAnalytics.getInstance(context).setAnalyticsCollectionEnabled(enabled)
    }
}
