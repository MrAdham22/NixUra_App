package com.adhamelgendy.nixura.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.adhamelgendy.nixura.domain.auth.AuthError
import com.adhamelgendy.nixura.domain.auth.AuthErrorKind
import com.adhamelgendy.nixura.domain.auth.AuthRepository
import com.adhamelgendy.nixura.domain.auth.AuthResult
import com.adhamelgendy.nixura.domain.auth.AuthValidation
import com.adhamelgendy.nixura.domain.auth.AuthUser
import com.adhamelgendy.nixura.domain.auth.DeviceSession
import com.adhamelgendy.nixura.domain.auth.SessionManager
import com.adhamelgendy.nixura.domain.auth.SessionState
import com.adhamelgendy.nixura.domain.sync.DetachDataAfterAccountDeletionUseCase
import com.adhamelgendy.nixura.domain.sync.SignOutResult
import com.adhamelgendy.nixura.domain.sync.SignOutUseCase
import com.adhamelgendy.nixura.domain.sync.StartFreshAsGuestUseCase
import com.adhamelgendy.nixura.domain.sync.SyncEngine
import com.adhamelgendy.nixura.domain.sync.SyncStatus
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private fun invalid() = AuthError(AuthErrorKind.InvalidInput)

class RegisterViewModel(private val auth: AuthRepository) : FormViewModel() {
    fun register(email: String, password: String, name: String) {
        if (AuthValidation.emailProblem(email) != null || AuthValidation.passwordProblems(password, email).isNotEmpty()) return fail(invalid())
        submit({ auth.register(AuthValidation.normalizeEmail(email), password, name.trim().take(AuthValidation.MAX_NAME_LENGTH)) })
    }
}

/** On success the session becomes SignedIn and the root gate swaps the UI; [done] is not needed. */
class LoginViewModel(private val auth: AuthRepository) : FormViewModel() {
    fun login(email: String, password: String) {
        if (AuthValidation.emailProblem(email) != null || password.isEmpty()) return fail(invalid())
        submit({ auth.login(AuthValidation.normalizeEmail(email), password) })
    }
}

class VerifyOtpViewModel(private val auth: AuthRepository) : FormViewModel() {
    private val _resendSeconds = MutableStateFlow(RESEND_COOLDOWN_SECONDS)
    val resendSeconds: StateFlow<Int> = _resendSeconds.asStateFlow()
    private val _resent = MutableStateFlow(false)
    val resentNotice: StateFlow<Boolean> = _resent.asStateFlow()
    private var timer: Job? = null

    init { startTimer() }

    fun verify(email: String, code: String) {
        if (!AuthValidation.isCompleteOtp(code)) return fail(AuthError(AuthErrorKind.InvalidCode))
        submit({ auth.verifyEmail(AuthValidation.normalizeEmail(email), code) })
    }

    fun resend(email: String) {
        if (_resendSeconds.value > 0) return
        _resent.value = false
        startTimer()
        viewModelScope.launch {
            when (val r = auth.resendCode(AuthValidation.normalizeEmail(email))) {
                is AuthResult.Success -> _resent.value = true
                is AuthResult.Failure -> fail(r.error)
            }
        }
    }

    private fun startTimer() {
        timer?.cancel()
        _resendSeconds.value = RESEND_COOLDOWN_SECONDS
        timer = viewModelScope.launch {
            while (_resendSeconds.value > 0) {
                delay(1_000)
                _resendSeconds.update { it - 1 }
            }
        }
    }

    private companion object { const val RESEND_COOLDOWN_SECONDS = 60 }
}

class ForgotPasswordViewModel(private val auth: AuthRepository) : FormViewModel() {
    fun send(email: String) {
        if (AuthValidation.emailProblem(email) != null) return fail(invalid())
        submit({ auth.forgotPassword(AuthValidation.normalizeEmail(email)) })
    }
}

class ResetPasswordViewModel(private val auth: AuthRepository) : FormViewModel() {
    fun reset(email: String, code: String, newPassword: String) {
        if (!AuthValidation.isCompleteOtp(code)) return fail(AuthError(AuthErrorKind.InvalidCode))
        if (AuthValidation.passwordProblems(newPassword, email).isNotEmpty()) return fail(invalid())
        submit({ auth.resetPassword(AuthValidation.normalizeEmail(email), code, newPassword) })
    }

    fun resend(email: String) {
        viewModelScope.launch { auth.forgotPassword(AuthValidation.normalizeEmail(email)) }
    }
}

class ChangePasswordViewModel(private val auth: AuthRepository, private val sessions: SessionManager) : FormViewModel() {
    fun change(current: String, new: String) {
        val email = (sessions.state.value as? SessionState.SignedIn)?.user?.email.orEmpty()
        if (current.isEmpty() || AuthValidation.passwordProblems(new, email).isNotEmpty()) return fail(invalid())
        submit({ auth.changePassword(current, new) })
    }
}

class DeleteAccountViewModel(
    private val auth: AuthRepository,
    private val detachData: DetachDataAfterAccountDeletionUseCase,
) : FormViewModel() {
    /** After success the session is gone and the root gate leaves the account screens. */
    fun delete(password: String) {
        if (password.isEmpty()) return fail(invalid())
        submit({
            val result = auth.deleteAccount(password)
            if (result is AuthResult.Success) detachData()
            result
        })
    }
}

data class DevicesUiState(
    val loading: Boolean = true,
    val devices: List<DeviceSession> = emptyList(),
    val error: AuthError? = null,
)

class DevicesViewModel(private val auth: AuthRepository, private val signOut: SignOutUseCase) : ViewModel() {
    private val _state = MutableStateFlow(DevicesUiState())
    val state: StateFlow<DevicesUiState> = _state.asStateFlow()

    init { refresh() }

    fun refresh() {
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            when (val r = auth.listDevices()) {
                is AuthResult.Success -> _state.value = DevicesUiState(loading = false, devices = r.value)
                is AuthResult.Failure -> _state.update { it.copy(loading = false, error = r.error) }
            }
        }
    }

    fun revoke(id: String) {
        viewModelScope.launch {
            when (val r = auth.revokeDevice(id)) {
                is AuthResult.Success -> refresh()
                is AuthResult.Failure -> _state.update { it.copy(error = r.error) }
            }
        }
    }

    /** Signs out everywhere including this device; the root gate reacts to the session change. */
    fun logoutAll() {
        viewModelScope.launch {
            val r = signOut.everywhere()
            if (r is AuthResult.Failure) _state.update { it.copy(error = r.error) }
        }
    }

    fun dismissError() = _state.update { it.copy(error = null) }
}

enum class GuestDialog { OfflineWarning, LeftoverAccountData }

/** "Continue as guest": warns about offline-only data, and never lets a guest walk into a previous account's data. */
class GuestEntryViewModel(
    private val sessions: SessionManager,
    private val startFresh: StartFreshAsGuestUseCase,
) : ViewModel() {
    private val _dialog = MutableStateFlow<GuestDialog?>(null)
    val dialog: StateFlow<GuestDialog?> = _dialog.asStateFlow()

    fun onContinueAsGuest() {
        viewModelScope.launch {
            _dialog.value = if (sessions.hasLeftoverAccountData()) GuestDialog.LeftoverAccountData else GuestDialog.OfflineWarning
        }
    }

    fun onStartFresh(onDone: () -> Unit) {
        viewModelScope.launch {
            startFresh()
            _dialog.value = null
            onDone()
        }
    }

    fun onDismiss() {
        _dialog.value = null
    }
}

data class SignOutUiState(val inProgress: Boolean = false, val changesNotUploaded: Boolean = false)

/** Hub screen: who is signed in, sync status, sign out. */
class AccountViewModel(
    private val signOutUseCase: SignOutUseCase,
    private val engine: SyncEngine,
    sessions: SessionManager,
) : ViewModel() {
    val user: StateFlow<AuthUser?> = sessions.state
        .map { (it as? SessionState.SignedIn)?.user }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), (sessions.state.value as? SessionState.SignedIn)?.user)
    val syncStatus: StateFlow<SyncStatus> = engine.status

    private val _signOut = MutableStateFlow(SignOutUiState())
    val signOutState: StateFlow<SignOutUiState> = _signOut.asStateFlow()

    fun syncNow() {
        viewModelScope.launch { engine.syncNow() }
    }

    /** [discardUnsaved] is true only after the user was told some changes could not be uploaded. */
    fun signOut(discardUnsaved: Boolean = false) {
        if (_signOut.value.inProgress) return
        _signOut.value = SignOutUiState(inProgress = true)
        viewModelScope.launch {
            val result = signOutUseCase(discardUnsaved)
            _signOut.value = SignOutUiState(changesNotUploaded = result == SignOutResult.ChangesNotUploaded)
        }
    }

    fun dismissSignOutWarning() = _signOut.update { it.copy(changesNotUploaded = false) }
}
