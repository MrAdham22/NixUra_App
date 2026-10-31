package com.example.reclaimapp.di

import android.content.Context
import com.example.reclaimapp.data.local.NixUraDatabase
import com.example.reclaimapp.data.local.getDatabaseBuilder
import com.example.reclaimapp.data.local.getRoomDatabase
import com.example.reclaimapp.domain.notification.NotificationPermissionGateway
import com.example.reclaimapp.domain.notification.NotificationScheduler
import com.example.reclaimapp.domain.usecase.DecrementGoodHabitTodayUseCase
import com.example.reclaimapp.domain.usecase.IncrementGoodHabitTodayUseCase
import com.example.reclaimapp.domain.usecase.LoadWidgetStateUseCase
import com.example.reclaimapp.domain.usecase.ObserveWidgetDataChangesUseCase
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

    // Android home-screen widgets (registered here because AppModules.kt belongs to another agent this round).
    // Their repositories, Clock and DayCalculator come from appModule().
    factory { LoadWidgetStateUseCase(get(), get(), get(), get(), get(), get()) }
    factory { IncrementGoodHabitTodayUseCase(get(), get(), get()) }
    factory { DecrementGoodHabitTodayUseCase(get(), get(), get()) }
    factory { ObserveWidgetDataChangesUseCase(get(), get(), get(), get()) }
}
