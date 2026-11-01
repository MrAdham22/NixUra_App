package com.example.reclaimapp.domain.auth

/** The signed-in account as the server knows it. Never contains a password or token. */
data class AuthUser(
    val id: String,
    val email: String,
    val name: String,
    val age: Int?,
    val emailVerified: Boolean,
)

/** One signed-in device, as listed in Settings > Account > Devices. */
data class DeviceSession(
    val id: String,
    val deviceName: String,
    val platform: String,
    val lastUsedAtIso: String,
    val isCurrent: Boolean,
)

sealed interface SessionState {
    /** Stored session not read yet (app start). */
    data object Loading : SessionState
    data object SignedOut : SessionState
    data class SignedIn(val user: AuthUser) : SessionState
}

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
    Unknown,
}

data class AuthError(
    val kind: AuthErrorKind,
    /** Seconds to wait before retrying, when the server said so (lockout, rate limit). */
    val retryAfterSeconds: Int? = null,
    /** Server-side password problems (same ids as [PasswordProblem]) for [AuthErrorKind.WeakPassword]. */
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
