package com.adhamelgendy.nixura.data.auth

import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.encodeURLParameter
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.KSerializer

/**
 * The Firebase project this build talks to. [apiKey] is the project's Web API key (from `google-services.json` /
 * `GoogleService-Info.plist`); it identifies the project and is not a secret, but Google may restrict it to this app,
 * so [platformHeaders] carry the app's identity (Android package + signing SHA-1, or the iOS bundle id).
 */
data class FirebaseConfig(val apiKey: String, val platformHeaders: Map<String, String> = emptyMap())

/** One POST to a Firebase REST endpoint. A seam so the account logic is testable without a network. */
internal interface FirebaseTransport {
    /** [form] sends `application/x-www-form-urlencoded` (the token endpoint), otherwise JSON. */
    suspend fun post(url: String, body: String, form: Boolean = false): TransportResponse
}

internal class KtorFirebaseTransport(
    private val config: FirebaseConfig,
    private val client: HttpClient = KtorApiTransport.defaultClient(),
) : FirebaseTransport {
    override suspend fun post(url: String, body: String, form: Boolean): TransportResponse = try {
        val response = client.post(url) {
            header(HttpHeaders.Accept, "application/json")
            config.platformHeaders.forEach { (k, v) -> header(k, v) }
            contentType(if (form) ContentType.Application.FormUrlEncoded else ContentType.Application.Json)
            setBody(body)
        }
        TransportResponse(response.status.value, response.bodyAsText())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Throwable) {
        TransportResponse(0) // offline, timeout, DNS, TLS
    }
}

/** Answer of one Firebase call: the parsed reply, Firebase's error code (e.g. "EMAIL_EXISTS"), or no answer at all. */
internal sealed interface FbResult<out T> {
    data class Ok<T>(val value: T) : FbResult<T>
    data class Err(val code: String) : FbResult<Nothing>
    data object Offline : FbResult<Nothing>
}

/**
 * Firebase Authentication over its documented REST API (Identity Toolkit v1 + Secure Token). Shared by Android and
 * iOS, so no Firebase SDK is needed for accounts. Never logs tokens, passwords or emails.
 */
internal class FirebaseAuthApi(private val config: FirebaseConfig, private val transport: FirebaseTransport) {

    suspend fun signUp(email: String, password: String) =
        call("accounts:signUp", FirebaseRequestJson.encodeToString(FbPasswordRequest.serializer(), FbPasswordRequest(email, password)), FbSignInReply.serializer())

    suspend fun signInWithPassword(email: String, password: String) =
        call("accounts:signInWithPassword", FirebaseRequestJson.encodeToString(FbPasswordRequest.serializer(), FbPasswordRequest(email, password)), FbSignInReply.serializer())

    /** [postBody] is `id_token=…&providerId=google.com` or `access_token=…&providerId=facebook.com`. */
    suspend fun signInWithIdp(postBody: String) =
        call("accounts:signInWithIdp", FirebaseRequestJson.encodeToString(FbIdpRequest.serializer(), FbIdpRequest(postBody)), FbSignInReply.serializer())

    suspend fun sendVerificationEmail(idToken: String) =
        call("accounts:sendOobCode", FirebaseRequestJson.encodeToString(FbOobRequest.serializer(), FbOobRequest("VERIFY_EMAIL", idToken = idToken)))

    suspend fun sendPasswordReset(email: String) =
        call("accounts:sendOobCode", FirebaseRequestJson.encodeToString(FbOobRequest.serializer(), FbOobRequest("PASSWORD_RESET", email = email)))

    suspend fun lookup(idToken: String): FbResult<FbUser> =
        when (val r = call("accounts:lookup", FirebaseRequestJson.encodeToString(FbIdTokenRequest.serializer(), FbIdTokenRequest(idToken)), FbLookupReply.serializer())) {
            is FbResult.Ok -> r.value.users.firstOrNull()?.let { FbResult.Ok(it) } ?: FbResult.Err("USER_NOT_FOUND")
            is FbResult.Err -> r
            FbResult.Offline -> FbResult.Offline
        }

    /** Changing the password returns new tokens and ends every other session of the account. */
    suspend fun update(idToken: String, displayName: String? = null, password: String? = null) =
        call("accounts:update", FirebaseRequestJson.encodeToString(FbUpdateRequest.serializer(), FbUpdateRequest(idToken, displayName, password)), FbSignInReply.serializer())

    suspend fun delete(idToken: String) =
        call("accounts:delete", FirebaseRequestJson.encodeToString(FbIdTokenRequest.serializer(), FbIdTokenRequest(idToken)))

    suspend fun refresh(refreshToken: String): FbResult<FbRefreshReply> {
        val body = "grant_type=refresh_token&refresh_token=" + refreshToken.encodeURLParameter()
        return parse(transport.post("$TOKEN_URL?key=${config.apiKey}", body, form = true), FbRefreshReply.serializer()).also { log("token", it) }
    }

    private suspend fun <T> call(method: String, body: String, reply: KSerializer<T>): FbResult<T> =
        parse(transport.post("$IDENTITY_URL/$method?key=${config.apiKey}", body), reply).also { log(method, it) }

    /** For calls whose reply body does not matter. */
    private suspend fun call(method: String, body: String): FbResult<Unit> {
        val res = transport.post("$IDENTITY_URL/$method?key=${config.apiKey}", body)
        return when {
            res.isNetworkFailure -> FbResult.Offline
            res.status !in 200..299 -> FbResult.Err(errorCode(res))
            else -> FbResult.Ok(Unit)
        }.also { log(method, it) }
    }

    /** Firebase's error code only (e.g. "INVALID_IDP_RESPONSE"): never the request, the reply or a token. */
    private fun log(method: String, result: FbResult<*>) {
        when (result) {
            is FbResult.Err -> logAuthDiagnostic("$method failed: ${result.code}")
            FbResult.Offline -> logAuthDiagnostic("$method failed: no network")
            is FbResult.Ok -> Unit
        }
    }

    private fun <T> parse(res: TransportResponse, reply: KSerializer<T>): FbResult<T> {
        if (res.isNetworkFailure) return FbResult.Offline
        if (res.status !in 200..299) return FbResult.Err(errorCode(res))
        return runCatching { FbResult.Ok(AuthJson.decodeFromString(reply, res.body)) }.getOrElse { FbResult.Err("BAD_REPLY") }
    }

    private companion object {
        const val IDENTITY_URL = "https://identitytoolkit.googleapis.com/v1"
        const val TOKEN_URL = "https://securetoken.googleapis.com/v1/token"

        /** "WEAK_PASSWORD : Password should be at least 6 characters" -> "WEAK_PASSWORD". */
        fun errorCode(res: TransportResponse): String {
            val message = runCatching { AuthJson.decodeFromString(FbErrorEnvelope.serializer(), res.body).error?.message }.getOrNull().orEmpty()
            val code = message.substringBefore(" ").substringBefore(":").trim()
            // Firebase codes look like "EMAIL_EXISTS"; anything else (a Google API key or quota message) keeps its
            // status and a short, email-free excerpt so the log says what happened.
            return if (code.isNotEmpty() && code.all { it.isUpperCase() || it == '_' || it.isDigit() }) code
            else "HTTP_${res.status} " + message.filterNot { it == '@' }.take(120)
        }
    }
}
