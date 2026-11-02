package com.example.reclaimapp.data.auth

import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.header
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.contentType
import kotlinx.coroutines.CancellationException

/**
 * Where the backend lives. [allowCleartext] is true only for debug builds talking to a local server;
 * release builds must use https and the transport refuses anything else.
 */
data class ApiConfig(val baseUrl: String, val allowCleartext: Boolean = false)

/** Device label sent when signing in so the user can recognise it in "Devices". Never contains personal data. */
interface DeviceInfoProvider {
    fun deviceName(): String
    fun platform(): String
}

/** [status] 0 means the request never got an HTTP answer (offline, timeout, TLS failure, refused cleartext). */
data class TransportResponse(val status: Int, val body: String = "", val retryAfterSeconds: Int? = null) {
    val isNetworkFailure: Boolean get() = status == 0
}

/** One HTTP call. A seam so the token-refresh logic and repository are testable without a network. */
interface ApiTransport {
    suspend fun execute(method: String, path: String, body: String?, bearer: String?): TransportResponse
}

class KtorApiTransport(
    private val config: ApiConfig,
    private val client: HttpClient = defaultClient(),
) : ApiTransport {

    override suspend fun execute(method: String, path: String, body: String?, bearer: String?): TransportResponse {
        val base = config.baseUrl.trimEnd('/')
        val secure = base.startsWith("https://")
        if (!secure && !(config.allowCleartext && base.startsWith("http://"))) {
            return TransportResponse(0) // never send credentials over cleartext in release builds
        }
        // Retry once, only for idempotent reads, and only when the network failed (not for a real HTTP answer).
        val attempts = if (method == "GET") 2 else 1
        var last = TransportResponse(0)
        repeat(attempts) {
            last = send(method, base + path, body, bearer)
            if (!last.isNetworkFailure) return last
        }
        return last
    }

    private suspend fun send(method: String, url: String, body: String?, bearer: String?): TransportResponse =
        try {
            val response = client.request(url) {
                this.method = HttpMethod.parse(method)
                header(HttpHeaders.Accept, "application/json")
                if (bearer != null) header(HttpHeaders.Authorization, "Bearer $bearer")
                if (body != null) {
                    contentType(ContentType.Application.Json)
                    setBody(body)
                }
            }
            TransportResponse(
                status = response.status.value,
                body = response.bodyAsText(),
                retryAfterSeconds = response.headers[HttpHeaders.RetryAfter]?.trim()?.toIntOrNull(),
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            TransportResponse(0) // timeout, DNS, TLS, connection refused...
        }

    companion object {
        fun defaultClient(): HttpClient = HttpClient {
            expectSuccess = false
            followRedirects = false
            install(HttpTimeout) {
                connectTimeoutMillis = 10_000
                requestTimeoutMillis = 25_000
                socketTimeoutMillis = 25_000
            }
        }
    }
}
