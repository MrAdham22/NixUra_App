package com.adhamelgendy.nixura.di

import com.adhamelgendy.nixura.data.auth.ApiTransport
import com.adhamelgendy.nixura.data.auth.AuthorizedApi
import com.adhamelgendy.nixura.data.auth.FirebaseAuthApi
import com.adhamelgendy.nixura.data.auth.FirebaseAuthRepository
import com.adhamelgendy.nixura.data.auth.FirebaseConfig
import com.adhamelgendy.nixura.data.auth.FirebaseSession
import com.adhamelgendy.nixura.data.auth.KtorApiTransport
import com.adhamelgendy.nixura.data.auth.KtorFirebaseTransport
import com.adhamelgendy.nixura.data.auth.SessionManagerImpl
import com.adhamelgendy.nixura.data.local.NixUraDatabase
import com.adhamelgendy.nixura.data.sync.PausedSyncRemote
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
import com.adhamelgendy.nixura.presentation.auth.ForgotPasswordViewModel
import com.adhamelgendy.nixura.presentation.auth.LoginViewModel
import com.adhamelgendy.nixura.presentation.auth.RegisterViewModel
import com.adhamelgendy.nixura.presentation.auth.SocialSignInViewModel
import com.adhamelgendy.nixura.presentation.auth.VerifyEmailViewModel
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
 * Accounts on Firebase Authentication. Platform modules provide `SecureStore`, `FirebaseConfig`, `SocialSignIn` and
 * `ApiConfig` (NixUra's own server, still used by the Notion link). Other features depend only on [AuthRepository]
 * and [SessionManager].
 */
fun authModule() = module {
    single { SessionManagerImpl(get()) }
    single<SessionManager> { get<SessionManagerImpl>() }
    single { FirebaseAuthApi(get(), KtorFirebaseTransport(get<FirebaseConfig>())) }
    // One shared instance: its single-flight token refresh only works if every signed-in call goes through it.
    single { FirebaseSession(get(), get(), get()) }
    single<AuthRepository> { FirebaseAuthRepository(get(), get(), get(), get(), eraseServerData = { get<NotionBackendClient>().disconnect() }) }
    single<ApiTransport> { KtorApiTransport(get()) }
    single { AuthorizedApi(get(), get()) }
    factory { AccountProfileSync(get(), get()) }

    // Cloud sync. Platform modules provide BackgroundSyncScheduler. Paused until it moves to Firestore (next step):
    // NixUra's old server does not accept Firebase sign-ins.
    single<SyncRemote> { PausedSyncRemote }
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
    viewModel { VerifyEmailViewModel(get()) }
    viewModel { ForgotPasswordViewModel(get()) }
    viewModel { SocialSignInViewModel(get()) }
    viewModel { ChangePasswordViewModel(get(), get()) }
    viewModel { DeleteAccountViewModel(get(), get()) }
    viewModel { AccountViewModel(get(), get(), get()) }
    viewModel { com.adhamelgendy.nixura.presentation.auth.GuestEntryViewModel(get(), get()) }
}
