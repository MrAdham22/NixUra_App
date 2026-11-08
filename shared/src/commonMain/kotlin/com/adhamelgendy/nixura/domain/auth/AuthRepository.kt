package com.adhamelgendy.nixura.domain.auth

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * Account operations. This is the interface other features consume (sync, AI, integrations).
 * Every call returns a typed [AuthResult]; nothing throws for expected failures.
 */
interface AuthRepository {
    /**
     * Creates the account and emails a verification link. The user is signed in only after tapping it
     * ([checkEmailVerified]). An email that already has an account fails with [AuthErrorKind.EmailInUse]: Firebase's own
     * sign-up already tells anyone that, so hiding it would only confuse the user.
     */
    suspend fun register(email: String, password: String, name: String?): AuthResult<Unit>

    /** Signs the user in once the link from the verification email was tapped; [AuthErrorKind.EmailNotVerified] until then. */
    suspend fun checkEmailVerified(email: String): AuthResult<AuthUser>

    /** Sends the verification link again. */
    suspend fun resendVerification(email: String): AuthResult<Unit>

    /** Fails with [AuthErrorKind.EmailNotVerified] (and mails a fresh link) when the email was never confirmed. */
    suspend fun login(email: String, password: String): AuthResult<AuthUser>

    /** Emails a link that opens a page to choose a new password. Same answer for unknown emails. */
    suspend fun forgotPassword(email: String): AuthResult<Unit>
    suspend fun changePassword(currentPassword: String, newPassword: String): AuthResult<Unit>

    /** Whether [provider] can be offered on this phone and build. */
    fun isAvailable(provider: SocialProvider): Boolean
    suspend fun signInWith(provider: SocialProvider): AuthResult<AuthUser>

    /**
     * Deletes the account, then signs out locally. [password] confirms it for password accounts; accounts without
     * one pass null and may get [AuthErrorKind.RecentLoginRequired] if they signed in too long ago.
     */
    suspend fun deleteAccount(password: String?): AuthResult<Unit>

    /** Ends this device's session. Always signs out locally, even when offline. */
    suspend fun logout()
    suspend fun fetchProfile(): AuthResult<AuthUser>
    suspend fun updateProfile(name: String?, age: Int?): AuthResult<AuthUser>
}

/**
 * The phone's own Google / Facebook sign-in sheet. Returns the provider's token, which is exchanged for a NixUra
 * session. Implemented per platform; a platform without a provider reports it as unavailable.
 */
interface SocialSignIn {
    fun isAvailable(provider: SocialProvider): Boolean
    suspend fun token(provider: SocialProvider): SocialToken
}

sealed interface SocialToken {
    /** Google: an ID token. Facebook: an access token. */
    data class Ok(val value: String) : SocialToken
    data object Cancelled : SocialToken
    data object NotAvailable : SocialToken
    data object Offline : SocialToken
    data object Failed : SocialToken
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
