package com.adhamelgendy.nixura.di

import android.content.Context
import com.adhamelgendy.nixura.data.local.NixUraDatabase
import com.adhamelgendy.nixura.data.local.getDatabaseBuilder
import com.adhamelgendy.nixura.data.local.getRoomDatabase
import com.adhamelgendy.nixura.domain.notification.NotificationPermissionGateway
import com.adhamelgendy.nixura.domain.notification.AlwaysAvailableReminderCapabilities
import com.adhamelgendy.nixura.domain.notification.NotificationScheduler
import com.adhamelgendy.nixura.domain.notification.ReminderCapabilities
import com.adhamelgendy.nixura.domain.usecase.DecrementGoodHabitTodayUseCase
import com.adhamelgendy.nixura.domain.usecase.IncrementGoodHabitTodayUseCase
import com.adhamelgendy.nixura.domain.usecase.LoadWidgetStateUseCase
import com.adhamelgendy.nixura.domain.usecase.ObserveWidgetDataChangesUseCase
import com.adhamelgendy.nixura.data.auth.AndroidDeviceInfo
import com.adhamelgendy.nixura.data.integration.AndroidDeviceRegion
import com.adhamelgendy.nixura.domain.integration.DeviceRegion
import com.adhamelgendy.nixura.data.auth.AndroidSecureStore
import com.adhamelgendy.nixura.data.auth.DeviceInfoProvider
import com.adhamelgendy.nixura.data.auth.SecureStore
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
    // The Android gateway also reports blocked notifications / exact alarms; any other gateway reports "all allowed".
    single<ReminderCapabilities> { permissionGateway as? ReminderCapabilities ?: AlwaysAvailableReminderCapabilities }

    // Android home-screen widgets (registered here because AppModules.kt belongs to another agent this round).
    // Their repositories, Clock and DayCalculator come from appModule().
    factory { LoadWidgetStateUseCase(get(), get(), get(), get(), get(), get(), get()) }
    factory { IncrementGoodHabitTodayUseCase(get(), get(), get()) }
    factory { DecrementGoodHabitTodayUseCase(get(), get(), get()) }
    factory { ObserveWidgetDataChangesUseCase(get(), get(), get(), get()) }

    // Accounts: encrypted token storage + device label. ApiConfig is bound by NixUraApplication (needs BuildConfig).
    single<SecureStore> { AndroidSecureStore(context) }
    single<DeviceInfoProvider> { AndroidDeviceInfo() }
    single<DeviceRegion> { AndroidDeviceRegion(context) }
}
