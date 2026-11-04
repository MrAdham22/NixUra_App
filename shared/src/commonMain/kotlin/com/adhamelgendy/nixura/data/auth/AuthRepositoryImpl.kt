package com.adhamelgendy.nixura.data.auth

import com.adhamelgendy.nixura.domain.auth.AuthError
import com.adhamelgendy.nixura.domain.auth.AuthErrorKind
import com.adhamelgendy.nixura.domain.auth.AuthRepository
import com.adhamelgendy.nixura.domain.auth.AuthResult
import com.adhamelgendy.nixura.domain.auth.AuthUser
import com.adhamelgendy.nixura.domain.auth.DeviceSession
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject

class AuthRepositoryImpl internal constructor(
    private val transport: ApiTransport,
    private val api: AuthorizedApi,
    private val sessions: SessionManagerImpl,
    private val device: DeviceInfoProvider,
) : AuthRepository {

    override suspend fun register(email: String, password: String, name: String?): AuthResult<Unit> =
        publicPost("/v1/auth/register", AuthJson.encodeToString(RegisterRequest.serializer(), RegisterRequest(email, password, name?.takeIf { it.isNotBlank() })), setOf(202)) { }

    override suspend fun verifyEmail(email: String, code: String): AuthResult<AuthUser> {
        val body = AuthJson.encodeToString(VerifyRequest.serializer(), VerifyRequest(email, code, device.deviceName(), device.platform()))
        return signInCall("/v1/auth/verify-email", body)
    }

    override suspend fun resendCode(email: String): AuthResult<Unit> =
        publicPost("/v1/auth/resend-otp", AuthJson.encodeToString(EmailRequest.serializer(), EmailRequest(email)), setOf(202)) { }

    override suspend fun login(email: String, password: String): AuthResult<AuthUser> {
        val body = AuthJson.encodeToString(LoginRequest.serializer(), LoginRequest(email, password, device.deviceName(), device.platform()))
        return signInCall("/v1/auth/login", body)
    }

    override suspend fun forgotPassword(email: String): AuthResult<Unit> =
        publicPost("/v1/auth/forgot-password", AuthJson.encodeToString(EmailRequest.serializer(), EmailRequest(email)), setOf(202)) { }

    override suspend fun resetPassword(email: String, code: String, newPassword: String): AuthResult<Unit> =
        publicPost("/v1/auth/reset-password", AuthJson.encodeToString(ResetRequest.serializer(), ResetRequest(email, code, newPassword)), setOf(204)) { }

    override suspend fun changePassword(currentPassword: String, newPassword: String): AuthResult<Unit> =
        authorized("POST", "/v1/auth/change-password", AuthJson.encodeToString(ChangePasswordRequest.serializer(), ChangePasswordRequest(currentPassword, newPassword)), setOf(204)) { }

    override suspend fun deleteAccount(password: String): AuthResult<Unit> {
        val r = authorized("POST", "/v1/auth/delete-account", AuthJson.encodeToString(PasswordRequest.serializer(), PasswordRequest(password)), setOf(204)) { }
        if (r is AuthResult.Success) sessions.signOutLocally()
        return r
    }

    override suspend fun logout() {
        // Best effort: the user is signed out on this device even if the server cannot be reached.
        api.call("POST", "/v1/auth/logout", "{}")
        sessions.signOutLocally()
    }

    override suspend fun logoutAllDevices(): AuthResult<Unit> {
        val r = authorized("POST", "/v1/auth/logout-all", "{}", setOf(204)) { }
        if (r is AuthResult.Success) sessions.signOutLocally()
        return r
    }

    override suspend fun listDevices(): AuthResult<List<DeviceSession>> =
        authorized("GET", "/v1/auth/sessions", null, setOf(200)) { body ->
            AuthJson.decodeFromString(SessionsEnvelope.serializer(), body).sessions.map {
                DeviceSession(it.id, it.deviceName, it.platform, it.lastUsedAt, it.current)
            }
        }

    override suspend fun revokeDevice(sessionId: String): AuthResult<Unit> =
        authorized("DELETE", "/v1/auth/sessions/$sessionId", null, setOf(204)) { }

    override suspend fun fetchProfile(): AuthResult<AuthUser> =
        authorized("GET", "/v1/me", null, setOf(200)) { parseUser(it) }.alsoStoreUser()

    override suspend fun updateProfile(name: String?, age: Int?): AuthResult<AuthUser> {
        val body = buildJsonObject {
            if (name != null) put("name", JsonPrimitive(name))
            put("age", age?.let { JsonPrimitive(it) } ?: JsonNull) // null clears the age
        }.toString()
        return authorized("PATCH", "/v1/me", body, setOf(200)) { parseUser(it) }.alsoStoreUser()
    }

    // ------------------------------------------------------------------ helpers

    private suspend fun signInCall(path: String, body: String): AuthResult<AuthUser> {
        val res = transport.execute("POST", path, body, null)
        if (res.status != 200) return AuthResult.Failure(AuthErrorMapper.map(res))
        val dto = runCatching { AuthJson.decodeFromString(AuthResultDto.serializer(), res.body) }.getOrNull()
            ?: return AuthResult.Failure(AuthError(AuthErrorKind.Server))
        val u = dto.user
        sessions.signIn(StoredSession(dto.tokens.accessToken, dto.tokens.refreshToken, dto.tokens.sessionId, StoredUser(u.id, u.email, u.name, u.age, u.emailVerified)))
        return AuthResult.Success(u.toDomain())
    }

    private suspend fun <T> publicPost(path: String, body: String, ok: Set<Int>, parse: (String) -> T): AuthResult<T> =
        toResult(transport.execute("POST", path, body, null), ok, parse)

    private suspend fun <T> authorized(method: String, path: String, body: String?, ok: Set<Int>, parse: (String) -> T): AuthResult<T> =
        toResult(api.call(method, path, body), ok, parse)

    private fun <T> toResult(res: TransportResponse, ok: Set<Int>, parse: (String) -> T): AuthResult<T> {
        if (res.status !in ok) return AuthResult.Failure(AuthErrorMapper.map(res))
        return try {
            AuthResult.Success(parse(res.body))
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            AuthResult.Failure(AuthError(AuthErrorKind.Server))
        }
    }

    private suspend fun AuthResult<AuthUser>.alsoStoreUser(): AuthResult<AuthUser> {
        if (this is AuthResult.Success) sessions.updateUser(value)
        return this
    }

    private fun parseUser(body: String): AuthUser = AuthJson.decodeFromString(UserEnvelope.serializer(), body).user.toDomain()

    private fun UserDto.toDomain() = AuthUser(id, email, name, age, emailVerified)
}
