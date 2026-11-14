package com.adhamelgendy.nixura.data.sync

import com.adhamelgendy.nixura.data.auth.AuthorizedApi
import com.adhamelgendy.nixura.data.auth.TransportResponse
import com.adhamelgendy.nixura.domain.auth.SessionManager
import com.adhamelgendy.nixura.domain.auth.SessionState
import com.adhamelgendy.nixura.domain.clock.Clock
import com.adhamelgendy.nixura.domain.sync.SyncPage
import com.adhamelgendy.nixura.domain.sync.SyncRecord
import com.adhamelgendy.nixura.domain.sync.SyncRemote
import com.adhamelgendy.nixura.domain.sync.SyncRemoteResult
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/** Firestore's REST API, reached at this base; paths below start with `/projects/...`. */
internal const val FIRESTORE_BASE_URL = "https://firestore.googleapis.com/v1"

/**
 * Cloud sync on Firestore through its REST API, from shared code (Android and iOS, no Firebase SDK). Signed-in calls go
 * through [AuthorizedApi], which adds the Firebase ID token and renews it. The security rules in `firestore.rules` keep
 * each user inside `users/{uid}` and refuse an older edit overwriting a newer one; such a refused write is reported back
 * to [com.adhamelgendy.nixura.domain.sync.SyncEngine], which then fetches the newer version. Writes use `commit`
 * (Firestore's `batchWrite` is for server credentials only).
 */
internal class FirestoreSyncRemote(
    private val api: AuthorizedApi,
    private val projectId: String,
    private val sessions: SessionManager,
    private val clock: Clock,
) : SyncRemote {

    /** Server time minus this phone's time, from the last answer (null until the first pull). */
    private var serverClockOffsetMillis: Long? = null

    private val databasePath get() = "projects/$projectId/databases/(default)/documents"

    override suspend fun push(records: List<SyncRecord>): SyncRemoteResult<List<SyncRecord>> {
        val uid = userId() ?: return SyncRemoteResult.SessionEnded
        if (projectId.isBlank()) return SyncRemoteResult.Failed
        val capAt = serverClockOffsetMillis?.let { clock.nowEpochMillis() + it }
        val planned = FirestoreSyncMapping.writes(records, "$databasePath/users/$uid/${FirestoreSyncMapping.COLLECTION}", capAt)
        val refused = ArrayList<SyncRecord>()
        for (round in FirestoreSyncMapping.rounds(planned)) {
            for (chunk in round.chunked(MAX_WRITES_PER_REQUEST)) {
                commitFindingRefused(chunk, refused)?.let { return it }
            }
        }
        return SyncRemoteResult.Ok(refused)
    }

    /**
     * Commits [writes] at once. A commit is all or nothing, so when the rules refuse it (one item is older than the
     * server's version) it is split in halves until the refused items are found; the rest is then saved. Refusals are
     * rare, so this is normally one request. Null means done; otherwise the failure to report.
     */
    private suspend fun commitFindingRefused(writes: List<PlannedWrite>, refused: MutableList<SyncRecord>): SyncRemoteResult<Nothing>? {
        if (writes.isEmpty()) return null
        val response = post("/$databasePath:commit", FirestoreSyncMapping.commitBody(writes.map { it.write }))
        if (response.status == 403 && isPermissionDenied(response.body)) {
            if (writes.size == 1) {
                // The rules refused it: the server holds a newer version (fetched right after by the engine).
                refused += writes.single().records
                return null
            }
            val half = writes.size / 2
            commitFindingRefused(writes.subList(0, half), refused)?.let { return it }
            return commitFindingRefused(writes.subList(half, writes.size), refused)
        }
        return failureOf(response)
    }

    override suspend fun pull(cursor: String): SyncRemoteResult<SyncPage> {
        val uid = userId() ?: return SyncRemoteResult.SessionEnded
        if (projectId.isBlank()) return SyncRemoteResult.Failed
        val start = FirestoreCursor.decode(cursor)
        val response = post("/$databasePath/users/$uid:runQuery", FirestoreSyncMapping.pullQuery(start, PULL_PAGE))
        failureOf(response)?.let { return it }
        val rows = runCatching { SyncJson.parseToJsonElement(response.body) as JsonArray }.getOrNull()
            ?: return SyncRemoteResult.Failed
        val documents = rows.mapNotNull { (it as? JsonObject)?.get("document") as? JsonObject }
        val readMicros = rows.firstNotNullOfOrNull { row ->
            ((row as? JsonObject)?.get("readTime") as? JsonPrimitive)?.content?.let { FirestoreTime.parseMicros(it) }
        }
        readMicros?.let { serverClockOffsetMillis = it / 1_000 - clock.nowEpochMillis() }

        val hasMore = documents.size >= PULL_PAGE
        val next = nextCursor(start, documents, readMicros, lastPage = !hasMore)
        return SyncRemoteResult.Ok(
            SyncPage(
                records = documents.flatMap { FirestoreSyncMapping.records(it) },
                cursor = next?.encode() ?: cursor,
                hasMore = hasMore,
            ),
        )
    }

    override suspend fun fetch(records: List<SyncRecord>): SyncRemoteResult<List<SyncRecord>> {
        val uid = userId() ?: return SyncRemoteResult.SessionEnded
        if (projectId.isBlank()) return SyncRemoteResult.Failed
        val root = "$databasePath/users/$uid/${FirestoreSyncMapping.COLLECTION}"
        val names = FirestoreSyncMapping.writes(records, root, capAt = null).map { "$root/${it.docId}" }.distinct()
        val out = ArrayList<SyncRecord>()
        for (chunk in names.chunked(MAX_WRITES_PER_REQUEST)) {
            val body = JsonObject(mapOf("documents" to JsonArray(chunk.map { JsonPrimitive(it) })))
            val response = post("/$databasePath:batchGet", body)
            failureOf(response)?.let { return it }
            val rows = runCatching { SyncJson.parseToJsonElement(response.body) as JsonArray }.getOrNull()
                ?: return SyncRemoteResult.Failed
            rows.forEach { row -> ((row as? JsonObject)?.get("found") as? JsonObject)?.let { out += FirestoreSyncMapping.records(it) } }
        }
        return SyncRemoteResult.Ok(out)
    }

    /**
     * Deletes every synced document of the signed-in user (before the account itself is deleted). Returns false if
     * something could not be deleted; the rest is then left for a later clean-up.
     */
    suspend fun deleteEverything(): Boolean {
        val uid = userId() ?: return false
        if (projectId.isBlank()) return false
        repeat(MAX_DELETE_ROUNDS) {
            val listed = post("/$databasePath/users/$uid:runQuery", FirestoreSyncMapping.namesQuery(MAX_WRITES_PER_REQUEST))
            if (failureOf(listed) != null) return false
            val rows = runCatching { SyncJson.parseToJsonElement(listed.body) as JsonArray }.getOrNull() ?: return false
            val names = rows.mapNotNull { (((it as? JsonObject)?.get("document") as? JsonObject)?.get("name") as? JsonPrimitive)?.content }
            if (names.isEmpty()) return true
            val deleted = post("/$databasePath:commit", FirestoreSyncMapping.commitBody(names.map { FirestoreSyncMapping.deleteWrite(it) }))
            if (failureOf(deleted) != null) return false
        }
        return false
    }

    /**
     * After the last page the cursor never goes past [WINDOW_MICROS] before the server's read time: a write stamped
     * just before that time may still have been landing while the query ran, so those few documents are read again
     * next time (applying them twice changes nothing).
     */
    private fun nextCursor(start: FirestoreCursor?, documents: List<JsonObject>, readMicros: Long?, lastPage: Boolean): FirestoreCursor? {
        val last = documents.lastOrNull() ?: return start
        val lastName = (last["name"] as? JsonPrimitive)?.content ?: return start
        val lastTime = (((last["fields"] as? JsonObject)?.get("serverTime") as? JsonObject)?.get("timestampValue") as? JsonPrimitive)?.content
            ?: return start
        val exact = FirestoreCursor(lastTime, lastName)
        if (!lastPage || readMicros == null) return exact
        val lastMicros = FirestoreTime.parseMicros(lastTime) ?: return exact
        val safeMicros = readMicros - WINDOW_MICROS
        return if (lastMicros < safeMicros) exact else FirestoreCursor(FirestoreTime.formatMicros(safeMicros), null)
    }

    private suspend fun post(path: String, body: JsonObject): TransportResponse =
        api.call("POST", path, SyncJson.encodeToString(JsonElement.serializer(), body))

    private fun userId(): String? = (sessions.state.value as? SessionState.SignedIn)?.user?.id

    private fun failureOf(response: TransportResponse): SyncRemoteResult<Nothing>? = when {
        response.isNetworkFailure -> SyncRemoteResult.Offline
        response.status == 401 -> SyncRemoteResult.SessionEnded
        // Daily quota used up (Spark plan) or Firestore busy: like being offline, every change stays queued.
        response.status == 409 || response.status == 429 || response.status == 503 || response.status == 504 -> SyncRemoteResult.Offline
        response.status != 200 -> SyncRemoteResult.Failed
        else -> null
    }

    /** 403 also comes back for a wrong project or a disabled API; only PERMISSION_DENIED from the rules splits a commit. */
    private fun isPermissionDenied(body: String): Boolean = runCatching {
        ((SyncJson.parseToJsonElement(body) as JsonObject)["error"] as JsonObject)["status"].let { (it as JsonPrimitive).content } == "PERMISSION_DENIED"
    }.getOrDefault(false)

    private companion object {
        const val PULL_PAGE = 300
        const val MAX_WRITES_PER_REQUEST = 500
        const val MAX_DELETE_ROUNDS = 200
        const val WINDOW_MICROS = 10_000_000L
    }
}
