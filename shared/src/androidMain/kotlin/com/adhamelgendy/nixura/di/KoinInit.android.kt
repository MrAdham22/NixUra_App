package com.adhamelgendy.nixura.di

import android.content.Context
import com.adhamelgendy.nixura.domain.notification.NotificationPermissionGateway
import com.adhamelgendy.nixura.domain.notification.NotificationScheduler
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.mp.KoinPlatformTools

fun initAndroidKoin(
    context: Context,
    scheduler: NotificationScheduler,
    permissionGateway: NotificationPermissionGateway,
) {
    if (KoinPlatformTools.defaultContext().getOrNull() == null) {
        startKoin {
            androidContext(context.applicationContext)
            modules(platformModule(context.applicationContext, scheduler, permissionGateway), appModule(), authModule())
        }
        warmUpInBackground()
    }
}
