package com.adhamelgendy.nixura.domain.sync

import com.adhamelgendy.nixura.domain.auth.AuthRepository
import com.adhamelgendy.nixura.domain.auth.AuthResult
import com.adhamelgendy.nixura.domain.auth.SessionManager
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter

/**
 * Decides when to sync while the app runs: on sign-in, on app start and resume, and shortly after any local change.
 * Does nothing at all for guests, who stay completely offline.
 */
class SyncCoordinator(
    private val engine: SyncEngine,
    private val sessions: SessionManager,
    private val local: SyncLocalStore,
    private val background: BackgroundSyncScheduler,
) {
    @OptIn(FlowPreview::class)
    suspend fun run() {
        sessions.currentUserId.collectLatest { userId ->
            if (userId == null) {
                background.cancelAll()
                return@collectLatest
            }
            background.schedulePeriodic()
            syncOrRetryLater()
            local.observePendingCount()
                .distinctUntilChanged()
                .filter { it > 0 }
                .debounce(CHANGE_DEBOUNCE_MILLIS)
                .collect { syncOrRetryLater() }
        }
    }

    suspend fun onAppForeground() {
        syncOrRetryLater()
    }

    private suspend fun syncOrRetryLater() {
        when (engine.syncNow()) {
            SyncOutcome.Offline, SyncOutcome.Failed -> background.scheduleRetry()
            else -> Unit
        }
    }

    private companion object {
        const val CHANGE_DEBOUNCE_MILLIS = 3_000L
    }
}

sealed interface SignOutResult {
    data object Done : SignOutResult

    /** Some changes could not be uploaded. Signing out anyway would lose them, so the user decides. */
    data object ChangesNotUploaded : SignOutResult
}

/**
 * Sign-out: upload what is pending, then remove the account's data from this phone. It all comes back at the next
 * sign-in, and the next person using the phone sees nothing.
 */
class SignOutUseCase(
    private val engine: SyncEngine,
    private val local: SyncLocalStore,
    private val auth: AuthRepository,
    private val sessions: SessionManager,
) {
    suspend operator fun invoke(discardUnsaved: Boolean = false): SignOutResult {
        if (!discardUnsaved) {
            engine.syncNow()
            if (local.pendingCount() > 0) return SignOutResult.ChangesNotUploaded
        }
        engine.exclusive {
            local.clearAccountData()
            sessions.forgetLastUser()
            auth.logout()
        }
        return SignOutResult.Done
    }

    /** Devices > sign out everywhere: same clean-up, once the server has agreed. */
    suspend fun everywhere(): AuthResult<Unit> {
        engine.syncNow()
        return engine.exclusive {
            val result = auth.logoutAllDevices()
            if (result is AuthResult.Success) {
                local.clearAccountData()
                sessions.forgetLastUser()
            }
            result
        }
    }
}

/**
 * A session ended without a clean sign-out, so the last account's data is still on the phone, and the user chose to
 * continue as a guest instead of signing back in. The guest starts empty and never sees that account's data.
 */
class StartFreshAsGuestUseCase(
    private val engine: SyncEngine,
    private val local: SyncLocalStore,
    private val sessions: SessionManager,
) {
    suspend operator fun invoke() = engine.exclusive {
        local.clearAccountData()
        sessions.forgetLastUser()
    }
}

/** After the account is deleted, the phone keeps the data as guest data, ready to upload if another account signs in. */
class DetachDataAfterAccountDeletionUseCase(
    private val engine: SyncEngine,
    private val local: SyncLocalStore,
    private val sessions: SessionManager,
) {
    suspend operator fun invoke() = engine.exclusive {
        local.detachFromAccount()
        sessions.forgetLastUser()
    }
}
