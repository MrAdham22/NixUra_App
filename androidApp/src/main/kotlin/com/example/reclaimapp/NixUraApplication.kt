package com.example.reclaimapp

import android.app.Application
import com.example.reclaimapp.data.auth.ApiConfig
import com.example.reclaimapp.di.initAndroidKoin
import com.example.reclaimapp.notifications.AndroidNotificationPermission
import com.example.reclaimapp.notifications.AndroidNotificationScheduler
import com.example.reclaimapp.notifications.ReminderReceiver
import com.example.reclaimapp.domain.sync.BackgroundSyncScheduler
import com.example.reclaimapp.domain.widget.WidgetPinner
import com.example.reclaimapp.sync.AndroidBackgroundSync
import com.example.reclaimapp.widgets.AndroidWidgetPinner
import com.example.reclaimapp.widgets.WidgetUpdater
import org.koin.core.context.loadKoinModules
import org.koin.dsl.module

class NixUraApplication : Application() {

    val notificationPermission by lazy { AndroidNotificationPermission(this) }

    override fun onCreate() {
        super.onCreate()
        ReminderReceiver.ensureChannel(this)
        initAndroidKoin(this, AndroidNotificationScheduler(this), notificationPermission)
        // Backend address comes from BuildConfig (the shared module cannot see it). Cleartext only in debug builds.
        loadKoinModules(
            module {
                single { ApiConfig(BuildConfig.NIXURA_API_BASE_URL, allowCleartext = BuildConfig.DEBUG) }
                single<BackgroundSyncScheduler> { AndroidBackgroundSync(applicationContext) }
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
