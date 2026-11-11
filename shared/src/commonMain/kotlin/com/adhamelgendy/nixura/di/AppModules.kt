package com.adhamelgendy.nixura.di

import com.adhamelgendy.nixura.data.local.NixUraDatabase
import com.adhamelgendy.nixura.data.repository.CoachRepositoryImpl
import com.adhamelgendy.nixura.data.repository.DataMaintenanceRepositoryImpl
import com.adhamelgendy.nixura.data.repository.EventRepositoryImpl
import com.adhamelgendy.nixura.data.repository.GoodHabitRepositoryImpl
import com.adhamelgendy.nixura.data.repository.HabitTrackingRepositoryImpl
import com.adhamelgendy.nixura.data.repository.PreferencesRepositoryImpl
import com.adhamelgendy.nixura.data.time.SystemTimeZoneOffsetProvider
import com.adhamelgendy.nixura.domain.clock.Clock
import com.adhamelgendy.nixura.domain.clock.SystemClock
import com.adhamelgendy.nixura.domain.coach.CoachEngine
import com.adhamelgendy.nixura.domain.coach.CoachResponder
import com.adhamelgendy.nixura.domain.coach.HabitKnowledgeBase
import com.adhamelgendy.nixura.domain.coach.RuleBasedCoachResponder
import com.adhamelgendy.nixura.domain.notification.NotificationPlanner
import com.adhamelgendy.nixura.domain.notification.NotificationSyncService
import com.adhamelgendy.nixura.domain.money.CountableHabits
import com.adhamelgendy.nixura.domain.recognition.GoodHabitFillStyleDetector
import com.adhamelgendy.nixura.domain.recognition.HabitNameHandoff
import com.adhamelgendy.nixura.domain.recognition.HabitRecognizer
import com.adhamelgendy.nixura.domain.repository.CoachRepository
import com.adhamelgendy.nixura.domain.repository.DataMaintenanceRepository
import com.adhamelgendy.nixura.domain.repository.EventRepository
import com.adhamelgendy.nixura.domain.repository.GoodHabitRepository
import com.adhamelgendy.nixura.domain.repository.HabitTrackingRepository
import com.adhamelgendy.nixura.domain.repository.PreferencesRepository
import com.adhamelgendy.nixura.domain.time.DayCalculator
import com.adhamelgendy.nixura.domain.time.TimeZoneOffsetProvider
import com.adhamelgendy.nixura.domain.usecase.BuildCoachSnapshotUseCase
import com.adhamelgendy.nixura.domain.usecase.ClearAllDataUseCase
import com.adhamelgendy.nixura.domain.usecase.DeleteCoachChatUseCase
import com.adhamelgendy.nixura.domain.usecase.ObserveCoachConversationsUseCase
import com.adhamelgendy.nixura.domain.usecase.CreateGoodHabitUseCase
import com.adhamelgendy.nixura.domain.usecase.CreateHabitAndStartTrackingUseCase
import com.adhamelgendy.nixura.domain.usecase.EventReminderEditor
import com.adhamelgendy.nixura.domain.usecase.EnsureCoachGreetingUseCase
import com.adhamelgendy.nixura.domain.usecase.GetCoachContextUseCase
import com.adhamelgendy.nixura.domain.usecase.ObserveActiveHabitTrackingUseCase
import com.adhamelgendy.nixura.domain.usecase.ObserveCoachMessagesUseCase
import com.adhamelgendy.nixura.domain.usecase.ObserveEventsUseCase
import com.adhamelgendy.nixura.domain.usecase.ObserveGoodHabitsUseCase
import com.adhamelgendy.nixura.domain.usecase.ObserveProfileUseCase
import com.adhamelgendy.nixura.domain.usecase.ObserveSettingsUseCase
import com.adhamelgendy.nixura.domain.usecase.RecordRelapseUseCase
import com.adhamelgendy.nixura.domain.usecase.SaveEventUseCase
import com.adhamelgendy.nixura.domain.usecase.SaveProfileUseCase
import com.adhamelgendy.nixura.domain.usecase.SendCoachMessageUseCase
import com.adhamelgendy.nixura.domain.usecase.SetGoodHabitCountUseCase
import com.adhamelgendy.nixura.domain.usecase.UpdateGoodHabitUseCase
import com.adhamelgendy.nixura.domain.usecase.UpdateHabitUseCase
import com.adhamelgendy.nixura.domain.usecase.UpdateSettingsUseCase
import com.adhamelgendy.nixura.presentation.coach.CoachViewModel
import com.adhamelgendy.nixura.presentation.events.EventsViewModel
import com.adhamelgendy.nixura.presentation.goodhabits.GoodHabitsViewModel
import com.adhamelgendy.nixura.presentation.habits.HabitsViewModel
import com.adhamelgendy.nixura.presentation.navigation.Navigator
import com.adhamelgendy.nixura.presentation.profile.ProfileViewModel
import com.adhamelgendy.nixura.presentation.settings.SettingsViewModel
import com.adhamelgendy.nixura.domain.usecase.DeleteItemUseCase
import com.adhamelgendy.nixura.presentation.deletion.PendingDeletions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
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
    single<PreferencesRepository> { PreferencesRepositoryImpl(get(), get()) }
    single<DataMaintenanceRepository> { DataMaintenanceRepositoryImpl(get()) }

    // Coach (offline, rule-based). Swap RuleBasedCoachResponder for an AI-backed one later.
    single { HabitKnowledgeBase() }
    single { HabitRecognizer(get()) }
    single { HabitNameHandoff() }
    single { GoodHabitFillStyleDetector(get()) }
    single { CountableHabits(get()) }
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
    // Use cases — good habits
    factory { ObserveGoodHabitsUseCase(get()) }
    factory { CreateGoodHabitUseCase(get()) }
    factory { UpdateGoodHabitUseCase(get()) }
    factory { SetGoodHabitCountUseCase(get()) }
    // Use cases — events
    factory { ObserveEventsUseCase(get()) }
    factory { SaveEventUseCase(get()) }
    factory { EventReminderEditor(get(), get()) }
    // Use cases — coach
    factory { BuildCoachSnapshotUseCase(get(), get(), get(), get(), get(), get()) }
    factory { ObserveCoachMessagesUseCase(get()) }
    factory { EnsureCoachGreetingUseCase(get(), get(), get()) }
    factory { SendCoachMessageUseCase(get(), get(), get(), get(), get(), get()) }
    // Real AI coach (through NixUra's Worker) and voice notes.
    factory { com.adhamelgendy.nixura.domain.usecase.BuildAiContextUseCase(get(), get(), get(), get(), get()) }
    factory { com.adhamelgendy.nixura.domain.usecase.ObserveAiModeUseCase(get(), get()) }
    factory { com.adhamelgendy.nixura.domain.usecase.SetAiConsentUseCase(get()) }
    factory { com.adhamelgendy.nixura.domain.usecase.GetAiQuotaUseCase(get()) }
    factory { com.adhamelgendy.nixura.domain.usecase.UnderstandVoiceNoteUseCase(get(), get()) }
    factory { com.adhamelgendy.nixura.domain.usecase.ApplyCoachActionsUseCase(get(), get(), get(), get(), get()) }
    factory { com.adhamelgendy.nixura.domain.usecase.BuildCoachVisualsUseCase(get(), get(), get(), get()) }
    factory { DeleteCoachChatUseCase(get()) }
    factory { ObserveCoachConversationsUseCase(get()) }
    factory { GetCoachContextUseCase(get(), get(), get()) }
    // Use cases — profile & settings
    factory { ObserveProfileUseCase(get()) }
    factory { ObserveSettingsUseCase(get()) }
    factory { SaveProfileUseCase(get()) }
    factory { UpdateSettingsUseCase(get()) }
    factory { ClearAllDataUseCase(get(), get(), get()) }
    factory { DeleteItemUseCase(get(), get(), get()) }
    // Outlives screens: a delete still happens (or is undone) after the user leaves the screen it came from.
    single { PendingDeletions(get(), CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)) }

    // View models
    viewModel {
        HabitsViewModel(
            observeActiveHabitTracking = get(),
            createHabitAndStartTracking = get(),
            recordRelapse = get(),
            clock = get(),
            updateHabit = get(),
            pendingDeletions = get(),
        )
    }
    viewModel { GoodHabitsViewModel(get(), get(), get(), get(), get(), get(), get(), get()) }
    viewModel { EventsViewModel(get(), get(), get(), get(), get()) }
    viewModel { CoachViewModel(get(), get(), get(), get(), get(), get(), get(), get(), get(), get()) }
    viewModel { com.adhamelgendy.nixura.presentation.voice.VoiceNoteViewModel(get(), get(), get(), get(), get()) }
    viewModel { ProfileViewModel(get(), get(), get(), get(), get(), get(), get(), get(), get()) }
    viewModel { SettingsViewModel(get(), get(), get(), get()) }
}
