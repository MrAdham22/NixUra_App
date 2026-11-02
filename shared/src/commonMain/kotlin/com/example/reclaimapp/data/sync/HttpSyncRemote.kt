package com.example.reclaimapp.data.sync

import com.example.reclaimapp.data.auth.AuthorizedApi
import com.example.reclaimapp.data.auth.TransportResponse
import com.example.reclaimapp.domain.sync.SyncPage
import com.example.reclaimapp.domain.sync.SyncRecord
import com.example.reclaimapp.domain.sync.SyncRemote
import com.example.reclaimapp.domain.sync.SyncRemoteResult
import com.example.reclaimapp.domain.sync.SyncType
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

internal val SyncJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
}

@Serializable
internal data class SyncChangeDto(val type: String, val id: String, val updatedAt: Long, val deleted: Boolean, val payload: String)

@Serializable
internal data class SyncPushRequest(val changes: List<SyncChangeDto>)

@Serializable
internal data class SyncPushResultDto(val type: String, val id: String, val applied: Boolean)

@Serializable
internal data class SyncPushResponse(val results: List<SyncPushResultDto>)

@Serializable
internal data class SyncPullResponse(val items: List<SyncChangeDto>, val cursor: String, val hasMore: Boolean)

/** Signed-in calls go through [AuthorizedApi], which renews expired tokens. */
internal class HttpSyncRemote(private val api: AuthorizedApi) : SyncRemote {

    override suspend fun push(records: List<SyncRecord>): SyncRemoteResult<List<SyncRecord>> {
        val body = SyncJson.encodeToString(
            SyncPushRequest.serializer(),
            SyncPushRequest(records.map { SyncChangeDto(it.type.wire, it.id, it.updatedAt, it.deleted, it.payload) }),
        )
        return parse(api.call("POST", "/v1/sync/push", body)) { text ->
            val refused = SyncJson.decodeFromString(SyncPushResponse.serializer(), text).results
                .filter { !it.applied }
                .map { it.type to it.id }
                .toSet()
            records.filter { (it.type.wire to it.id) in refused }
        }
    }

    override suspend fun pull(cursor: String): SyncRemoteResult<SyncPage> =
        parse(api.call("GET", "/v1/sync/pull?cursor=$cursor&limit=$PULL_PAGE", null)) { text ->
            val page = SyncJson.decodeFromString(SyncPullResponse.serializer(), text)
            SyncPage(
                // A type this version does not know yet (from a newer app on another device) is skipped, not an error.
                records = page.items.mapNotNull { dto ->
                    SyncType.fromWire(dto.type)?.let { SyncRecord(it, dto.id, dto.updatedAt, dto.deleted, dto.payload) }
                },
                cursor = page.cursor,
                hasMore = page.hasMore,
            )
        }

    private fun <T> parse(response: TransportResponse, read: (String) -> T): SyncRemoteResult<T> = when {
        response.isNetworkFailure -> SyncRemoteResult.Offline
        response.status == 401 -> SyncRemoteResult.SessionEnded
        response.status != 200 -> SyncRemoteResult.Failed
        else -> runCatching { read(response.body) }
            .fold({ SyncRemoteResult.Ok(it) }, { SyncRemoteResult.Failed })
    }

    private companion object {
        const val PULL_PAGE = 300
    }
}
