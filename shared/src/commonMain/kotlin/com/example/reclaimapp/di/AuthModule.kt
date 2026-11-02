package com.example.reclaimapp.di

import com.example.reclaimapp.data.auth.ApiTransport
import com.example.reclaimapp.data.auth.AuthRepositoryImpl
import com.example.reclaimapp.data.auth.KtorApiTransport
import com.example.reclaimapp.data.auth.SessionManagerImpl
import com.example.reclaimapp.domain.auth.AccountProfileSync
import com.example.reclaimapp.domain.auth.AuthRepository
import com.example.reclaimapp.domain.auth.SessionManager
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
    single<AuthRepository> { AuthRepositoryImpl(get<ApiTransport>(), get<SessionManagerImpl>(), get()) }
    factory { AccountProfileSync(get(), get()) }

    viewModel { RegisterViewModel(get()) }
    viewModel { LoginViewModel(get()) }
    viewModel { VerifyOtpViewModel(get()) }
    viewModel { ForgotPasswordViewModel(get()) }
    viewModel { ResetPasswordViewModel(get()) }
    viewModel { ChangePasswordViewModel(get(), get()) }
    viewModel { DeleteAccountViewModel(get()) }
    viewModel { DevicesViewModel(get()) }
    viewModel { AccountViewModel(get(), get()) }
}
