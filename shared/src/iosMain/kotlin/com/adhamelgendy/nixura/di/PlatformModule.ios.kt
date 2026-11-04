package com.adhamelgendy.nixura.di

import com.adhamelgendy.nixura.data.local.NixUraDatabase
import com.adhamelgendy.nixura.data.local.getDatabaseBuilder
import com.adhamelgendy.nixura.data.local.getRoomDatabase
import com.adhamelgendy.nixura.domain.notification.AlwaysGrantedPermissionGateway
import com.adhamelgendy.nixura.domain.notification.AlwaysAvailableReminderCapabilities
import com.adhamelgendy.nixura.domain.notification.NoOpNotificationScheduler
import com.adhamelgendy.nixura.domain.notification.NotificationPermissionGateway
import com.adhamelgendy.nixura.domain.notification.NotificationScheduler
import com.adhamelgendy.nixura.domain.notification.ReminderCapabilities
import com.adhamelgendy.nixura.data.auth.ApiConfig
import com.adhamelgendy.nixura.data.auth.DeviceInfoProvider
import com.adhamelgendy.nixura.data.auth.IosDeviceInfo
import com.adhamelgendy.nixura.data.integration.IosDeviceRegion
import com.adhamelgendy.nixura.domain.integration.DeviceRegion
import com.adhamelgendy.nixura.data.auth.KeychainSecureStore
import com.adhamelgendy.nixura.data.auth.SecureStore
import com.adhamelgendy.nixura.data.auth.iosApiConfig
import com.adhamelgendy.nixura.domain.integration.GoogleAuthResult
import com.adhamelgendy.nixura.domain.integration.GoogleAuthorizer
import com.adhamelgendy.nixura.domain.sync.BackgroundSyncScheduler
import com.adhamelgendy.nixura.domain.sync.NoOpBackgroundSyncScheduler
import org.koin.dsl.module

/**
 * iOS: local notifications are not implemented yet (no-op scheduler). Everything else works.
 * See docs/DEVELOPMENT_STATUS.md for the iOS follow-up.
 */
fun platformModule() = module {
    single<NixUraDatabase> {
        getRoomDatabase(getDatabaseBuilder())
    }
    single<NotificationScheduler> { NoOpNotificationScheduler }
    single<NotificationPermissionGateway> { AlwaysGrantedPermissionGateway }
    single<ReminderCapabilities> { AlwaysAvailableReminderCapabilities }

    // Accounts
    single<SecureStore> { KeychainSecureStore() }
    single<DeviceInfoProvider> { IosDeviceInfo() }
    single<DeviceRegion> { IosDeviceRegion() }
    single<ApiConfig> { iosApiConfig() }

    // iOS syncs while the app is open; background sync (BGTaskScheduler) is not implemented yet.
    single<BackgroundSyncScheduler> { NoOpBackgroundSyncScheduler }

    // Google Calendar on iOS needs Google's iOS sign-in (an iOS OAuth client + URL scheme): not built yet, so the
    // Integrations screen says it is not available here. Notion works (it is done by the server).
    single<GoogleAuthorizer> { NotAvailableGoogleAuthorizer }
}

private object NotAvailableGoogleAuthorizer : GoogleAuthorizer {
    override suspend fun authorize(interactive: Boolean): GoogleAuthResult = GoogleAuthResult.NotAvailable
    override suspend fun revoke() = Unit
    override suspend fun forget(accessToken: String) = Unit
}
