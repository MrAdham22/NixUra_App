package com.adhamelgendy.nixura.data.auth

import com.adhamelgendy.nixura.domain.auth.AuthError
import com.adhamelgendy.nixura.domain.auth.AuthErrorKind

/** Turns a Firebase error code into a typed [AuthError]. Never exposes Firebase's text to the UI (strings are localised there). */
internal object FirebaseErrorMapper {

    fun map(result: FbResult<*>): AuthError = when (result) {
        FbResult.Offline -> AuthError(AuthErrorKind.Network)
        is FbResult.Err -> map(result.code)
        is FbResult.Ok -> AuthError(AuthErrorKind.Unknown)
    }

    fun map(code: String): AuthError = when (code) {
        "INVALID_LOGIN_CREDENTIALS", "INVALID_PASSWORD", "EMAIL_NOT_FOUND" -> AuthError(AuthErrorKind.InvalidCredentials)
        "USER_DISABLED" -> AuthError(AuthErrorKind.AccountLocked)
        "TOO_MANY_ATTEMPTS_TRY_LATER", "QUOTA_EXCEEDED" -> AuthError(AuthErrorKind.RateLimited)
        "WEAK_PASSWORD" -> AuthError(AuthErrorKind.WeakPassword, passwordProblems = listOf("too_short"))
        "INVALID_EMAIL", "MISSING_PASSWORD", "MISSING_EMAIL" -> AuthError(AuthErrorKind.InvalidInput)
        "CREDENTIAL_TOO_OLD_LOGIN_AGAIN" -> AuthError(AuthErrorKind.RecentLoginRequired)
        "INVALID_ID_TOKEN", "TOKEN_EXPIRED", "USER_NOT_FOUND", "INVALID_REFRESH_TOKEN" -> AuthError(AuthErrorKind.SessionExpired)
        "FEDERATED_USER_ID_ALREADY_LINKED", "EMAIL_EXISTS" -> AuthError(AuthErrorKind.AccountExistsWithOtherMethod)
        // The provider is switched off in the Firebase console, or this build's key is not allowed to use it.
        "OPERATION_NOT_ALLOWED", "INVALID_IDP_RESPONSE", "API_KEY_INVALID" -> AuthError(AuthErrorKind.NotAvailable)
        else -> if (code.startsWith("HTTP_5")) AuthError(AuthErrorKind.Server) else AuthError(AuthErrorKind.Unknown)
    }
}
