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
import com.adhamelgendy.nixura.notifications.NixNotifications
import com.adhamelgendy.nixura.domain.sync.BackgroundSyncScheduler
import com.adhamelgendy.nixura.domain.widget.WidgetPinner
import com.adhamelgendy.nixura.domain.integration.GoogleAuthorizer
import com.adhamelgendy.nixura.integrations.AndroidGoogleAuthorizer
import com.adhamelgendy.nixura.sync.AndroidBackgroundSync
import com.adhamelgendy.nixura.widgets.AndroidWidgetPinner
import com.adhamelgendy.nixura.widgets.WidgetUpdater
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.core.context.loadKoinModules
import org.koin.dsl.module
import com.adhamelgendy.nixura.notifications.AndroidTodayNotification

class NixUraApplication : Application() {

    val notificationPermission by lazy { AndroidNotificationPermission(this) }
    /** Prayer-time location (phase C); MainActivity attaches its permission launcher. */
    val deviceLocation by lazy { com.adhamelgendy.nixura.notifications.AndroidDeviceLocation(this) }
    val voiceRecorder by lazy { com.adhamelgendy.nixura.voice.AndroidVoiceRecorder(this) }

    /**
     * Facebook Login is only used from the app screens, so its SDK starts with [MainActivity], not here: a widget tap,
     * a reminder alarm or a boot also start this process and must not pay ~120 ms for it (phase A1).
     * Facebook's own start-up provider is removed from the manifest so a build without a client token cannot crash.
     */
    fun ensureFacebookSdk() {
        if (BuildConfig.FACEBOOK_ENABLED && !FacebookSdk.isInitialized()) {
            runCatching { @Suppress("DEPRECATION") FacebookSdk.sdkInitialize(applicationContext) }
        }
    }

    private fun applyUsageStatsSetting() {
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.SupervisorJob() + kotlinx.coroutines.Dispatchers.IO).launch {
            runCatching {
                val koin = org.koin.core.context.GlobalContext.get()
                val settings = koin.get<com.adhamelgendy.nixura.domain.usecase.ObserveSettingsUseCase>()().first()
                koin.get<UsageStats>().setCollectionEnabled(settings.usageStatsEnabled)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        NixNotifications.ensureChannels(this) // channels and groups; names follow the app language
        initAndroidKoin(this, AndroidNotificationScheduler(this), notificationPermission, AndroidTodayNotification(this))
        // Backend address comes from BuildConfig (the shared module cannot see it). Cleartext only in debug builds.
        loadKoinModules(
            module {
                single { ApiConfig(BuildConfig.NIXURA_API_BASE_URL, allowCleartext = BuildConfig.DEBUG) }
                // Accounts: Firebase project settings from google-services.json, and the Google / Facebook sheets.
                single<FirebaseConfig> { AndroidFirebaseConfig.of(applicationContext) }
                single { AndroidSocialSignIn(AndroidFirebaseConfig.webClientId(applicationContext), BuildConfig.FACEBOOK_ENABLED) }
                single<SocialSignIn> { get<AndroidSocialSignIn>() }
                single<UsageStats> { FirebaseUsageStats(applicationContext) }
                single<com.adhamelgendy.nixura.domain.coach.ai.VoiceRecorder> { voiceRecorder }
                single<BackgroundSyncScheduler> { AndroidBackgroundSync(applicationContext) }
                single { AndroidGoogleAuthorizer(applicationContext) }
                single<GoogleAuthorizer> { get<AndroidGoogleAuthorizer>() }
                single<com.adhamelgendy.nixura.domain.reminder.DeviceLocationGateway> { deviceLocation }
            },
        )
        // Usage stats: apply the stored choice (on by default, can be turned off in Settings > Privacy).
        applyUsageStatsSetting()
        // Good-habit reminders: a daily re-plan as a safety net (phase C). Never let it stop the app from starting.
        runCatching { com.adhamelgendy.nixura.notifications.ReminderRefreshWorker.schedule(this) }
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
