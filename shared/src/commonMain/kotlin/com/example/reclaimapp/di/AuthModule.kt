package com.example.reclaimapp.di

import com.example.reclaimapp.data.auth.ApiTransport
import com.example.reclaimapp.data.auth.AuthRepositoryImpl
import com.example.reclaimapp.data.auth.AuthorizedApi
import com.example.reclaimapp.data.auth.KtorApiTransport
import com.example.reclaimapp.data.auth.SessionManagerImpl
import com.example.reclaimapp.data.local.NixUraDatabase
import com.example.reclaimapp.data.sync.HttpSyncRemote
import com.example.reclaimapp.data.sync.RoomSyncLocalStore
import com.example.reclaimapp.domain.auth.AccountProfileSync
import com.example.reclaimapp.domain.auth.AuthRepository
import com.example.reclaimapp.domain.auth.SessionManager
import com.example.reclaimapp.domain.sync.DetachDataAfterAccountDeletionUseCase
import com.example.reclaimapp.domain.sync.SignOutUseCase
import com.example.reclaimapp.domain.sync.SyncCoordinator
import com.example.reclaimapp.domain.sync.SyncEngine
import com.example.reclaimapp.domain.sync.SyncLocalStore
import com.example.reclaimapp.domain.sync.SyncRemote
import com.example.reclaimapp.presentation.auth.AccountViewModel
import com.example.reclaimapp.presentation.auth.ChangePasswordViewModel
import com.example.reclaimapp.presentation.auth.DeleteAccountViewModel
import com.example.reclaimapp.presentation.auth.DevicesViewModel
import com.example.reclaimapp.presentation.auth.ForgotPasswordViewModel
import com.example.reclaimapp.presentation.auth.LoginViewModel
import com.example.reclaimapp.presentation.auth.RegisterViewModel
import com.example.reclaimapp.presentation.auth.ResetPasswordViewModel
import com.example.reclaimapp.presentation.auth.VerifyOtpViewModel
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
    factory { com.example.reclaimapp.domain.sync.StartFreshAsGuestUseCase(get(), get(), get()) }

    viewModel { RegisterViewModel(get()) }
    viewModel { LoginViewModel(get()) }
    viewModel { VerifyOtpViewModel(get()) }
    viewModel { ForgotPasswordViewModel(get()) }
    viewModel { ResetPasswordViewModel(get()) }
    viewModel { ChangePasswordViewModel(get(), get()) }
    viewModel { DeleteAccountViewModel(get(), get()) }
    viewModel { DevicesViewModel(get(), get()) }
    viewModel { AccountViewModel(get(), get(), get()) }
    viewModel { com.example.reclaimapp.presentation.auth.GuestEntryViewModel(get(), get()) }
}
