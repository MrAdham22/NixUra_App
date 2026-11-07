package com.adhamelgendy.nixura.domain.auth

/** The signed-in account. Never contains a password or token. */
data class AuthUser(
    val id: String,
    val email: String,
    val name: String,
    val age: Int?,
    val emailVerified: Boolean,
    /** False for accounts that only sign in with Google or Facebook (no password to change or confirm with). */
    val hasPassword: Boolean = true,
)

sealed interface SessionState {
    /** Stored session not read yet (app start). */
    data object Loading : SessionState
    data object SignedOut : SessionState
    data class SignedIn(val user: AuthUser) : SessionState
}

/** Sign-in methods other than email + password. */
enum class SocialProvider { Google, Facebook }

enum class AuthErrorKind {
    InvalidCredentials,
    EmailNotVerified,
    InvalidCode,
    AccountLocked,
    RateLimited,
    WeakPassword,
    InvalidInput,

    /** Could not reach the server (offline, timeout, TLS, refused cleartext). */
    Network,
    Server,

    /** The session ended (revoked, expired, token reuse). The user has been signed out locally. */
    SessionExpired,

    /** The user closed the Google / Facebook sign-in sheet. Not shown as an error. */
    Cancelled,

    /** This sign-in method is not set up on this phone or build. */
    NotAvailable,

    /** The email already has an account that signs in another way (for example with a password). */
    AccountExistsWithOtherMethod,

    /** Deleting the account or changing the password needs a fresh sign-in first. */
    RecentLoginRequired,
    Unknown,
}

data class AuthError(
    val kind: AuthErrorKind,
    /** Seconds to wait before retrying, when the server said so (lockout, rate limit). */
    val retryAfterSeconds: Int? = null,
    /** Password problems (same ids as [PasswordProblem]) for [AuthErrorKind.WeakPassword]. */
    val passwordProblems: List<String> = emptyList(),
)

sealed interface AuthResult<out T> {
    data class Success<T>(val value: T) : AuthResult<T>
    data class Failure(val error: AuthError) : AuthResult<Nothing>
}

inline fun <T, R> AuthResult<T>.map(transform: (T) -> R): AuthResult<R> = when (this) {
    is AuthResult.Success -> AuthResult.Success(transform(value))
    is AuthResult.Failure -> this
}
