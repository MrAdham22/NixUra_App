package com.example.reclaimapp.di

import com.example.reclaimapp.data.local.NixUraDatabase
import com.example.reclaimapp.data.repository.HabitTrackingRepositoryImpl
import com.example.reclaimapp.domain.clock.Clock
import com.example.reclaimapp.domain.clock.SystemClock
import com.example.reclaimapp.domain.repository.HabitTrackingRepository
import com.example.reclaimapp.domain.usecase.CreateHabitAndStartTrackingUseCase
import com.example.reclaimapp.domain.usecase.ObserveActiveHabitTrackingUseCase
import com.example.reclaimapp.domain.usecase.RecordRelapseUseCase
import com.example.reclaimapp.presentation.habits.HabitsViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

fun appModule() = module {
    single<Clock> { SystemClock() }
    single { get<NixUraDatabase>().habitDao() }
    single { get<NixUraDatabase>().habitEventDao() }
    single { get<NixUraDatabase>().habitWriteDao() }
    single<HabitTrackingRepository> {
        HabitTrackingRepositoryImpl(
            habitDao = get(),
            habitEventDao = get(),
            habitWriteDao = get(),
            clock = get(),
        )
    }
    factory { ObserveActiveHabitTrackingUseCase(get()) }
    factory { CreateHabitAndStartTrackingUseCase(get()) }
    factory { RecordRelapseUseCase(get()) }
    viewModel {
        HabitsViewModel(
            observeActiveHabitTracking = get(),
            createHabitAndStartTracking = get(),
            recordRelapse = get(),
            clock = get(),
        )
    }
}
