package com.example.reclaimapp.di

import com.example.reclaimapp.data.local.NixUraDatabase
import com.example.reclaimapp.data.repository.CoachRepositoryImpl
import com.example.reclaimapp.data.repository.DataMaintenanceRepositoryImpl
import com.example.reclaimapp.data.repository.EventRepositoryImpl
import com.example.reclaimapp.data.repository.GoodHabitRepositoryImpl
import com.example.reclaimapp.data.repository.HabitTrackingRepositoryImpl
import com.example.reclaimapp.data.repository.PreferencesRepositoryImpl
import com.example.reclaimapp.data.time.SystemTimeZoneOffsetProvider
import com.example.reclaimapp.domain.clock.Clock
import com.example.reclaimapp.domain.clock.SystemClock
import com.example.reclaimapp.domain.coach.CoachEngine
import com.example.reclaimapp.domain.coach.CoachResponder
import com.example.reclaimapp.domain.coach.HabitKnowledgeBase
import com.example.reclaimapp.domain.coach.RuleBasedCoachResponder
import com.example.reclaimapp.domain.notification.NotificationPlanner
import com.example.reclaimapp.domain.notification.NotificationSyncService
import com.example.reclaimapp.domain.recognition.HabitNameHandoff
import com.example.reclaimapp.domain.recognition.HabitRecognizer
import com.example.reclaimapp.domain.repository.CoachRepository
import com.example.reclaimapp.domain.repository.DataMaintenanceRepository
import com.example.reclaimapp.domain.repository.EventRepository
import com.example.reclaimapp.domain.repository.GoodHabitRepository
import com.example.reclaimapp.domain.repository.HabitTrackingRepository
import com.example.reclaimapp.domain.repository.PreferencesRepository
import com.example.reclaimapp.domain.time.DayCalculator
import com.example.reclaimapp.domain.time.TimeZoneOffsetProvider
import com.example.reclaimapp.domain.usecase.ArchiveGoodHabitUseCase
import com.example.reclaimapp.domain.usecase.ArchiveHabitUseCase
import com.example.reclaimapp.domain.usecase.BuildCoachSnapshotUseCase
import com.example.reclaimapp.domain.usecase.ClearAllDataUseCase
import com.example.reclaimapp.domain.usecase.ClearCoachChatUseCase
import com.example.reclaimapp.domain.usecase.CreateGoodHabitUseCase
import com.example.reclaimapp.domain.usecase.CreateHabitAndStartTrackingUseCase
import com.example.reclaimapp.domain.usecase.DeleteEventUseCase
import com.example.reclaimapp.domain.usecase.EnsureCoachGreetingUseCase
import com.example.reclaimapp.domain.usecase.GetCoachContextUseCase
import com.example.reclaimapp.domain.usecase.ObserveActiveHabitTrackingUseCase
import com.example.reclaimapp.domain.usecase.ObserveCoachMessagesUseCase
import com.example.reclaimapp.domain.usecase.ObserveEventsUseCase
import com.example.reclaimapp.domain.usecase.ObserveGoodHabitsUseCase
import com.example.reclaimapp.domain.usecase.ObserveProfileUseCase
import com.example.reclaimapp.domain.usecase.ObserveSettingsUseCase
import com.example.reclaimapp.domain.usecase.RecordRelapseUseCase
import com.example.reclaimapp.domain.usecase.SaveEventUseCase
import com.example.reclaimapp.domain.usecase.SaveProfileUseCase
import com.example.reclaimapp.domain.usecase.SendCoachMessageUseCase
import com.example.reclaimapp.domain.usecase.SetGoodHabitCountUseCase
import com.example.reclaimapp.domain.usecase.UpdateGoodHabitUseCase
import com.example.reclaimapp.domain.usecase.UpdateHabitUseCase
import com.example.reclaimapp.domain.usecase.UpdateSettingsUseCase
import com.example.reclaimapp.presentation.coach.CoachViewModel
import com.example.reclaimapp.presentation.events.EventsViewModel
import com.example.reclaimapp.presentation.goodhabits.GoodHabitsViewModel
import com.example.reclaimapp.presentation.habits.HabitsViewModel
import com.example.reclaimapp.presentation.navigation.Navigator
import com.example.reclaimapp.presentation.profile.ProfileViewModel
import com.example.reclaimapp.presentation.settings.SettingsViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

/**
 * Shared dependency graph. Platform modules additionally provide [NixUraDatabase],
 * `NotificationScheduler` and `NotificationPermissionGateway`.
 */
fun appModule() = module {
    // Core
    single<Clock> { SystemClock() }
    single<TimeZoneOffsetProvider> { SystemTimeZoneOffsetProvider() }
    single { DayCalculator(get()) }
    single { Navigator() }

    // Database access
    single { get<NixUraDatabase>().habitDao() }
    single { get<NixUraDatabase>().habitEventDao() }
    single { get<NixUraDatabase>().habitWriteDao() }
    single { get<NixUraDatabase>().goodHabitDao() }
    single { get<NixUraDatabase>().goodHabitLogDao() }
    single { get<NixUraDatabase>().eventDao() }
    single { get<NixUraDatabase>().coachMessageDao() }
    single { get<NixUraDatabase>().preferenceDao() }
    single { get<NixUraDatabase>().maintenanceDao() }

    // Repositories
    single<HabitTrackingRepository> {
        HabitTrackingRepositoryImpl(habitDao = get(), habitEventDao = get(), habitWriteDao = get(), clock = get())
    }
    single<GoodHabitRepository> { GoodHabitRepositoryImpl(get(), get(), get()) }
    single<EventRepository> { EventRepositoryImpl(get(), get()) }
    single<CoachRepository> { CoachRepositoryImpl(get(), get()) }
    single<PreferencesRepository> { PreferencesRepositoryImpl(get()) }
    single<DataMaintenanceRepository> { DataMaintenanceRepositoryImpl(get()) }

    // Coach (offline, rule-based). Swap RuleBasedCoachResponder for an AI-backed one later.
    single { HabitKnowledgeBase() }
    single { HabitRecognizer(get()) }
    single { HabitNameHandoff() }
    single { CoachEngine(get()) }
    single<CoachResponder> { RuleBasedCoachResponder(get(), get()) }

    // Notifications
    single { NotificationPlanner(get(), get()) }
    single { NotificationSyncService(get(), get(), get(), get(), get(), get(), get()) }

    // Use cases — bad habits
    factory { ObserveActiveHabitTrackingUseCase(get()) }
    factory { CreateHabitAndStartTrackingUseCase(get()) }
    factory { RecordRelapseUseCase(get()) }
    factory { UpdateHabitUseCase(get()) }
    factory { ArchiveHabitUseCase(get(), get()) }
    // Use cases — good habits
    factory { ObserveGoodHabitsUseCase(get()) }
    factory { CreateGoodHabitUseCase(get()) }
    factory { UpdateGoodHabitUseCase(get()) }
    factory { ArchiveGoodHabitUseCase(get()) }
    factory { SetGoodHabitCountUseCase(get()) }
    // Use cases — events
    factory { ObserveEventsUseCase(get()) }
    factory { SaveEventUseCase(get()) }
    factory { DeleteEventUseCase(get()) }
    // Use cases — coach
    factory { BuildCoachSnapshotUseCase(get(), get(), get(), get(), get(), get()) }
    factory { ObserveCoachMessagesUseCase(get()) }
    factory { EnsureCoachGreetingUseCase(get(), get(), get()) }
    factory { SendCoachMessageUseCase(get(), get(), get()) }
    factory { ClearCoachChatUseCase(get()) }
    factory { GetCoachContextUseCase(get(), get(), get()) }
    // Use cases — profile & settings
    factory { ObserveProfileUseCase(get()) }
    factory { ObserveSettingsUseCase(get()) }
    factory { SaveProfileUseCase(get()) }
    factory { UpdateSettingsUseCase(get()) }
    factory { ClearAllDataUseCase(get()) }

    // View models
    viewModel {
        HabitsViewModel(
            observeActiveHabitTracking = get(),
            createHabitAndStartTracking = get(),
            recordRelapse = get(),
            clock = get(),
            updateHabit = get(),
            archiveHabit = get(),
        )
    }
    viewModel { GoodHabitsViewModel(get(), get(), get(), get(), get(), get(), get()) }
    viewModel { EventsViewModel(get(), get(), get()) }
    viewModel { CoachViewModel(get(), get(), get(), get(), get()) }
    viewModel { ProfileViewModel(get(), get(), get(), get(), get(), get(), get()) }
    viewModel { SettingsViewModel(get(), get(), get(), get()) }
}
