package com.example.reclaimapp.di

import com.example.reclaimapp.data.local.NixUraDatabase
import com.example.reclaimapp.data.local.getDatabaseBuilder
import com.example.reclaimapp.data.local.getRoomDatabase
import com.example.reclaimapp.domain.notification.AlwaysGrantedPermissionGateway
import com.example.reclaimapp.domain.notification.NoOpNotificationScheduler
import com.example.reclaimapp.domain.notification.NotificationPermissionGateway
import com.example.reclaimapp.domain.notification.NotificationScheduler
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
}
