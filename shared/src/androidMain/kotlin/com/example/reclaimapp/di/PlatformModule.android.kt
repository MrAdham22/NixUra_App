package com.example.reclaimapp.di

import android.content.Context
import com.example.reclaimapp.data.local.NixUraDatabase
import com.example.reclaimapp.data.local.getDatabaseBuilder
import com.example.reclaimapp.data.local.getRoomDatabase
import com.example.reclaimapp.domain.notification.NotificationPermissionGateway
import com.example.reclaimapp.domain.notification.NotificationScheduler
import org.koin.dsl.module

fun platformModule(
    context: Context,
    scheduler: NotificationScheduler,
    permissionGateway: NotificationPermissionGateway,
) = module {
    single<NixUraDatabase> {
        getRoomDatabase(getDatabaseBuilder(context))
    }
    single<NotificationScheduler> { scheduler }
    single<NotificationPermissionGateway> { permissionGateway }
}
