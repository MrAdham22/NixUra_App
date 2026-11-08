package com.adhamelgendy.nixura

import android.app.Application
import com.adhamelgendy.nixura.auth.AndroidFirebaseConfig
import com.adhamelgendy.nixura.auth.AndroidSocialSignIn
import com.adhamelgendy.nixura.auth.FirebaseUsageStats
import com.adhamelgendy.nixura.domain.analytics.UsageStats
import com.adhamelgendy.nixura.data.auth.ApiConfig
import com.adhamelgendy.nixura.data.auth.FirebaseConfig
import com.adhamelgendy.nixura.domain.auth.SocialSignIn
import com.facebook.FacebookSdk
import com.adhamelgendy.nixura.di.initAndroidKoin
import com.adhamelgendy.nixura.notifications.AndroidNotificationPermission
import com.adhamelgendy.nixura.notifications.AndroidNotificationScheduler
import com.adhamelgendy.nixura.notifications.ReminderReceiver
import com.adhamelgendy.nixura.domain.sync.BackgroundSyncScheduler
import com.adhamelgendy.nixura.domain.widget.WidgetPinner
import com.adhamelgendy.nixura.domain.integration.GoogleAuthorizer
import com.adhamelgendy.nixura.integrations.AndroidGoogleAuthorizer
import com.adhamelgendy.nixura.sync.AndroidBackgroundSync
import com.adhamelgendy.nixura.widgets.AndroidWidgetPinner
import com.adhamelgendy.nixura.widgets.WidgetUpdater
import org.koin.core.context.loadKoinModules
import org.koin.dsl.module

class NixUraApplication : Application() {

    val notificationPermission by lazy { AndroidNotificationPermission(this) }

    override fun onCreate() {
        super.onCreate()
        ReminderReceiver.ensureChannel(this)
        // Facebook's own start-up provider is removed from the manifest so a build without a client token cannot crash.
        if (BuildConfig.FACEBOOK_ENABLED) runCatching { @Suppress("DEPRECATION") FacebookSdk.sdkInitialize(applicationContext) }
        initAndroidKoin(this, AndroidNotificationScheduler(this), notificationPermission)
        // Backend address comes from BuildConfig (the shared module cannot see it). Cleartext only in debug builds.
        loadKoinModules(
            module {
                single { ApiConfig(BuildConfig.NIXURA_API_BASE_URL, allowCleartext = BuildConfig.DEBUG) }
                // Accounts: Firebase project settings from google-services.json, and the Google / Facebook sheets.
                single<FirebaseConfig> { AndroidFirebaseConfig.of(applicationContext) }
                single { AndroidSocialSignIn(AndroidFirebaseConfig.webClientId(applicationContext), BuildConfig.FACEBOOK_ENABLED) }
                single<SocialSignIn> { get<AndroidSocialSignIn>() }
                single<UsageStats> { FirebaseUsageStats(applicationContext) }
                single<BackgroundSyncScheduler> { AndroidBackgroundSync(applicationContext) }
                single { AndroidGoogleAuthorizer(applicationContext) }
                single<GoogleAuthorizer> { get<AndroidGoogleAuthorizer>() }
            },
        )
        // Home-screen widgets: keep them in sync with data/time changes while the process is alive.
        // Never let a widget problem stop the app from starting.
        runCatching {
            // Android-only port used by the "Add to Home screen" buttons (iOS has no binding, so the button hides there).
            loadKoinModules(module { single<WidgetPinner> { AndroidWidgetPinner(applicationContext) } })
            WidgetUpdater.registerSystemReceiver(this)
            WidgetUpdater.startObserving(this)
        }
    }
}
