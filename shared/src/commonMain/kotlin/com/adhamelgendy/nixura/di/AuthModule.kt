package com.adhamelgendy.nixura.di

import com.adhamelgendy.nixura.data.auth.ApiTransport
import com.adhamelgendy.nixura.data.auth.AuthRepositoryImpl
import com.adhamelgendy.nixura.data.auth.AuthorizedApi
import com.adhamelgendy.nixura.data.auth.KtorApiTransport
import com.adhamelgendy.nixura.data.auth.SessionManagerImpl
import com.adhamelgendy.nixura.data.local.NixUraDatabase
import com.adhamelgendy.nixura.data.sync.HttpSyncRemote
import com.adhamelgendy.nixura.data.sync.RoomSyncLocalStore
import com.adhamelgendy.nixura.domain.auth.AccountProfileSync
import com.adhamelgendy.nixura.domain.auth.AuthRepository
import com.adhamelgendy.nixura.domain.auth.SessionManager
import com.adhamelgendy.nixura.domain.sync.DetachDataAfterAccountDeletionUseCase
import com.adhamelgendy.nixura.domain.sync.SignOutUseCase
import com.adhamelgendy.nixura.domain.sync.SyncCoordinator
import com.adhamelgendy.nixura.domain.sync.SyncEngine
import com.adhamelgendy.nixura.domain.sync.SyncLocalStore
import com.adhamelgendy.nixura.domain.sync.SyncRemote
import com.adhamelgendy.nixura.presentation.auth.AccountViewModel
import com.adhamelgendy.nixura.presentation.auth.ChangePasswordViewModel
import com.adhamelgendy.nixura.presentation.auth.DeleteAccountViewModel
import com.adhamelgendy.nixura.presentation.auth.DevicesViewModel
import com.adhamelgendy.nixura.presentation.auth.ForgotPasswordViewModel
import com.adhamelgendy.nixura.presentation.auth.LoginViewModel
import com.adhamelgendy.nixura.presentation.auth.RegisterViewModel
import com.adhamelgendy.nixura.presentation.auth.ResetPasswordViewModel
import com.adhamelgendy.nixura.presentation.auth.VerifyOtpViewModel
import com.adhamelgendy.nixura.data.integration.GoogleCalendarApi
import com.adhamelgendy.nixura.data.integration.GoogleCalendarSource
import com.adhamelgendy.nixura.data.integration.IntegrationRevokerImpl
import com.adhamelgendy.nixura.data.integration.NotionBackendClient
import com.adhamelgendy.nixura.data.integration.NotionCalendarSource
import com.adhamelgendy.nixura.data.integration.PreferenceIntegrationStateStore
import com.adhamelgendy.nixura.data.integration.RoomImportedEventStore
import com.adhamelgendy.nixura.domain.integration.DisconnectIntegrationUseCase
import com.adhamelgendy.nixura.domain.integration.GoogleCalendarCatalog
import com.adhamelgendy.nixura.domain.integration.ImportedEventStore
import com.adhamelgendy.nixura.domain.integration.IntegrationRevoker
import com.adhamelgendy.nixura.domain.integration.IntegrationStateStore
import com.adhamelgendy.nixura.domain.integration.NotionLink
import com.adhamelgendy.nixura.domain.integration.SyncIntegrationsUseCase
import com.adhamelgendy.nixura.presentation.integrations.IntegrationsViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

/**
 * Accounts (Agent 2). Platform modules provide `SecureStore`, `DeviceInfoProvider` and `ApiConfig`.
 * Other features depend only on [AuthRepository] and [SessionManager].
 */
fun authModule() = module {
    single { SessionManagerImpl(get()) }
    single<SessionManager> { get<SessionManagerImpl>() }
    single<ApiTransport> { KtorApiTransport(get()) }
    // One shared instance: its single-flight token refresh only works if every signed-in call goes through it.
    single { AuthorizedApi(get(), get()) }
    single<AuthRepository> { AuthRepositoryImpl(get<ApiTransport>(), get<AuthorizedApi>(), get<SessionManagerImpl>(), get()) }
    factory { AccountProfileSync(get(), get()) }

    // Cloud sync. Platform modules provide BackgroundSyncScheduler.
    single<SyncRemote> { HttpSyncRemote(get()) }
    single<SyncLocalStore> { RoomSyncLocalStore(get<NixUraDatabase>().syncDao(), get(), get(), get()) }
    single { SyncEngine(get(), get(), get(), get()) }
    single { SyncCoordinator(get(), get(), get(), get()) }
    factory { SignOutUseCase(get(), get(), get(), get()) }
    factory { DetachDataAfterAccountDeletionUseCase(get(), get(), get()) }
    factory { com.adhamelgendy.nixura.domain.sync.StartFreshAsGuestUseCase(get(), get(), get()) }

    // Calendar imports (Google Calendar, Notion). Platform modules provide GoogleAuthorizer.
    single<IntegrationStateStore> { PreferenceIntegrationStateStore(get<NixUraDatabase>().preferenceDao()) }
    single<ImportedEventStore> { RoomImportedEventStore(get<NixUraDatabase>().eventDao()) }
    single { GoogleCalendarSource(get(), GoogleCalendarApi(KtorApiTransport.defaultClient()), get(), get(), get()) }
    single<GoogleCalendarCatalog> { get<GoogleCalendarSource>() }
    single { NotionBackendClient(get()) }
    single<NotionLink> { get<NotionBackendClient>() }
    single { NotionCalendarSource(get(), get(), get()) }
    single<IntegrationRevoker> { IntegrationRevokerImpl(get(), get()) }
    single { SyncIntegrationsUseCase(listOf(get<GoogleCalendarSource>(), get<NotionCalendarSource>()), get(), get(), get(), get()) }
    factory { DisconnectIntegrationUseCase(get(), get(), get(), get()) }
    viewModel { IntegrationsViewModel(get(), get(), get(), get(), get(), get(), get()) }

    viewModel { RegisterViewModel(get()) }
    viewModel { LoginViewModel(get()) }
    viewModel { VerifyOtpViewModel(get()) }
    viewModel { ForgotPasswordViewModel(get()) }
    viewModel { ResetPasswordViewModel(get()) }
    viewModel { ChangePasswordViewModel(get(), get()) }
    viewModel { DeleteAccountViewModel(get(), get()) }
    viewModel { DevicesViewModel(get(), get()) }
    viewModel { AccountViewModel(get(), get(), get()) }
    viewModel { com.adhamelgendy.nixura.presentation.auth.GuestEntryViewModel(get(), get()) }
}
