package com.example.reclaimapp.domain.auth

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * Account operations. This is the interface other features consume (sync, AI, integrations).
 * Every call returns a typed [AuthResult]; nothing throws for expected failures.
 */
interface AuthRepository {
    /** Creates the account (or restarts a pending registration) and emails a 6-digit code. Same outcome for existing emails. */
    suspend fun register(email: String, password: String, name: String?): AuthResult<Unit>

    /** Verifies the code and signs the user in on this device. */
    suspend fun verifyEmail(email: String, code: String): AuthResult<AuthUser>
    suspend fun resendCode(email: String): AuthResult<Unit>
    suspend fun login(email: String, password: String): AuthResult<AuthUser>
    suspend fun forgotPassword(email: String): AuthResult<Unit>
    suspend fun resetPassword(email: String, code: String, newPassword: String): AuthResult<Unit>
    suspend fun changePassword(currentPassword: String, newPassword: String): AuthResult<Unit>

    /** Deletes the account and its server data, then signs out locally. */
    suspend fun deleteAccount(password: String): AuthResult<Unit>

    /** Ends this device's session. Always signs out locally, even when offline. */
    suspend fun logout()
    suspend fun logoutAllDevices(): AuthResult<Unit>
    suspend fun listDevices(): AuthResult<List<DeviceSession>>
    suspend fun revokeDevice(sessionId: String): AuthResult<Unit>
    suspend fun fetchProfile(): AuthResult<AuthUser>
    suspend fun updateProfile(name: String?, age: Int?): AuthResult<AuthUser>
}

/**
 * Read-only view of "who is signed in" for the rest of the app.
 *
 * **Guest-data contract for cloud sync (Agent 3):** local habits/events are never touched by sign-in. After a
 * sign-in [pendingGuestHandoff] returns a [GuestDataHandoff]; the sync layer uploads (or, when
 * [GuestDataHandoff.previousUserId] differs from [GuestDataHandoff.userId], first clears) the local data and then
 * calls [markGuestHandoffHandled].
 */
interface SessionManager {
    val state: StateFlow<SessionState>

    /** Emits the signed-in user's id, or null when signed out / still loading. Distinct values only. */
    val currentUserId: Flow<String?>

    suspend fun pendingGuestHandoff(): GuestDataHandoff?
    suspend fun markGuestHandoffHandled()

    /** After a clean sign-out or account deletion, anything created next is guest data, not another account's. */
    suspend fun forgetLastUser()

    /** True when nobody is signed in but this phone still holds the last account's data (its session ended without a clean sign-out). */
    suspend fun hasLeftoverAccountData(): Boolean

    /** Clears the local session without calling the server (used when the server already ended it). */
    suspend fun signOutLocally()
}

/**
 * [userId] just signed in on this device. [previousUserId] is the account that was last signed in here
 * (null = nobody ever was, i.e. the local data is guest data to adopt).
 */
data class GuestDataHandoff(val userId: String, val previousUserId: String?)
