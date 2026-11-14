package com.adhamelgendy.nixura.data.sync

import com.adhamelgendy.nixura.data.auth.ApiTransport
import com.adhamelgendy.nixura.data.auth.TransportResponse
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Firestore's REST API in memory, with the same checks as `firestore.rules`: only the token's own user, an older
 * `updatedAt` never overwrites a newer one (per day for good-habit months), one day per month write.
 * [serverMicros] is the server's clock; each request moves it on by one millisecond.
 */
internal class FakeFirestore(private val uidForToken: (String?) -> String?) {
    val docs = LinkedHashMap<String, JsonObject>()
    var serverMicros = 1_800_000_000_000_000L
    var status: Int? = null
    val calls = mutableListOf<String>()
    var refusedCommits = 0

    fun transport() = object : ApiTransport {
        override suspend fun execute(method: String, path: String, body: String?, bearer: String?): TransportResponse {
            calls += path
            status?.let { return TransportResponse(it, "{}") }
            val uid = uidForToken(bearer) ?: return TransportResponse(401, "{}")
            serverMicros += 1_000
            val request = SyncJson.parseToJsonElement(body ?: "{}").jsonObject
            return when {
                path.endsWith(":commit") -> commit(uid, request)
                path.endsWith(":runQuery") -> runQuery(uid, path, request)
                path.endsWith(":batchGet") -> TransportResponse(200, batchGet(uid, request).toString())
                else -> TransportResponse(404, "{}")
            }
        }
    }

    fun docsOf(uid: String) = docs.filterKeys { owner(it) == uid }

    private fun owner(name: String) = name.substringAfter("/users/").substringBefore('/').substringBefore(':')

    private fun now() = obj("timestampValue" to JsonPrimitive(FirestoreTime.formatMicros(serverMicros)))

    /** All or nothing, like Firestore's commit: one refused write refuses the whole request with 403. */
    private fun commit(uid: String, request: JsonObject): TransportResponse {
        val writes = request["writes"]!!.jsonArray
        val names = writes.map { (it.jsonObject["update"]?.jsonObject?.get("name") ?: it.jsonObject["delete"])!!.let(::nameOf) }
        require(names.size == names.toSet().size) { "one write per document per request" }
        val before = LinkedHashMap(docs)
        if (writes.any { w -> apply(uid, w.jsonObject) != 0 }) {
            docs.clear()
            docs.putAll(before)
            refusedCommits++
            return TransportResponse(403, """{"error":{"code":403,"message":"Missing or insufficient permissions.","status":"PERMISSION_DENIED"}}""")
        }
        return TransportResponse(200, """{"writeResults":[],"commitTime":"${FirestoreTime.formatMicros(serverMicros)}"}""")
    }

    private fun nameOf(e: JsonElement) = e.jsonPrimitive.content

    /** Returns the gRPC status code: 0 OK, 7 refused by the rules. */
    private fun apply(uid: String, write: JsonObject): Int {
        write["delete"]?.let { d ->
            val name = d.jsonPrimitive.content
            if (owner(name) != uid) return 7
            docs.remove(name)
            return 0
        }
        val update = write["update"]!!.jsonObject
        val name = update["name"]!!.jsonPrimitive.content
        if (owner(name) != uid) return 7
        val fields = update["fields"]!!.jsonObject
        val mask = write["updateMask"]?.jsonObject?.get("fieldPaths")?.jsonArray?.map { it.jsonPrimitive.content }
        val old = docs[name]
        val merged = if (mask == null) LinkedHashMap(fields) else mergeMasked(old ?: JsonObject(emptyMap()), fields, mask)
        val type = (merged["type"] as JsonObject)["stringValue"]!!.jsonPrimitive.content
        if (type == FirestoreSyncMapping.LOG_MONTH_TYPE) {
            val day = mask!!.single { it.startsWith("days.") }.removePrefix("days.")
            val newDay = dayOf(merged, day)!!
            val oldDay = old?.let { dayOf(it, day) }
            if (oldDay != null && long(newDay["updatedAt"]) < long(oldDay["updatedAt"])) return 7
        } else {
            if (old != null && long(merged["updatedAt"]) < long(old["updatedAt"])) return 7
            val docId = name.substringAfterLast('/')
            val id = (merged["id"] as JsonObject)["stringValue"]!!.jsonPrimitive.content
            if (docId != "$type~$id") return 7
        }
        merged["serverTime"] = now()
        docs[name] = JsonObject(merged)
        return 0
    }

    private fun mergeMasked(old: JsonObject, fields: JsonObject, mask: List<String>): LinkedHashMap<String, JsonElement> {
        val out = LinkedHashMap(old)
        for (path in mask) {
            if ('.' !in path) {
                fields[path]?.let { out[path] = it } ?: out.remove(path)
                continue
            }
            val (top, key) = path.split('.', limit = 2)
            val oldMap = (out[top] as? JsonObject)?.get("mapValue")?.jsonObject?.get("fields")?.jsonObject ?: JsonObject(emptyMap())
            val newValue = fields[top]!!.jsonObject["mapValue"]!!.jsonObject["fields"]!!.jsonObject[key]!!
            out[top] = obj("mapValue" to obj("fields" to JsonObject(oldMap + (key to newValue))))
        }
        return out
    }

    private fun dayOf(doc: Map<String, JsonElement>, day: String): JsonObject? =
        (doc["days"] as? JsonObject)?.get("mapValue")?.jsonObject?.get("fields")?.jsonObject?.get(day)
            ?.jsonObject?.get("mapValue")?.jsonObject?.get("fields")?.jsonObject

    private fun long(v: JsonElement?): Long = (v as JsonObject)["integerValue"]!!.jsonPrimitive.content.toLong()

    private fun runQuery(uid: String, path: String, request: JsonObject): TransportResponse {
        if (owner(path) != uid) return TransportResponse(403, "{}")
        val q = request["structuredQuery"]!!.jsonObject
        val limit = q["limit"]?.jsonPrimitive?.intOrNull ?: Int.MAX_VALUE
        val readTime = JsonPrimitive(FirestoreTime.formatMicros(serverMicros))
        val mine = docsOf(uid).entries
        if (q["select"] != null) {
            val names = mine.map { it.key }.sorted().take(limit)
            return ok(names.map { obj("document" to obj("name" to JsonPrimitive(it)), "readTime" to readTime) }, readTime)
        }
        val sorted = mine.sortedWith(compareBy({ time(it.value) }, { it.key }))
        val start = q["startAt"]?.jsonObject
        val after = sorted.filter { (name, fields) ->
            if (start == null) return@filter true
            val values = start["values"]!!.jsonArray
            val t = FirestoreTime.parseMicros(values[0].jsonObject["timestampValue"]!!.jsonPrimitive.content)!!
            val inclusive = start["before"]?.jsonPrimitive?.booleanOrNull == true
            val docTime = time(fields)
            if (values.size == 1) {
                if (inclusive) docTime >= t else docTime > t
            } else {
                val n = values[1].jsonObject["referenceValue"]!!.jsonPrimitive.content
                docTime > t || (docTime == t && (if (inclusive) name >= n else name > n))
            }
        }.take(limit)
        return ok(after.map { obj("document" to obj("name" to JsonPrimitive(it.key), "fields" to it.value), "readTime" to readTime) }, readTime)
    }

    private fun ok(rows: List<JsonObject>, readTime: JsonPrimitive) =
        TransportResponse(200, JsonArray(rows.ifEmpty { listOf(obj("readTime" to readTime)) }).toString())

    private fun time(fields: JsonObject) =
        FirestoreTime.parseMicros(fields["serverTime"]!!.jsonObject["timestampValue"]!!.jsonPrimitive.content)!!

    private fun batchGet(uid: String, request: JsonObject): JsonArray = JsonArray(
        request["documents"]!!.jsonArray.map { n ->
            val name = n.jsonPrimitive.content
            val doc = docs[name]?.takeIf { owner(name) == uid }
            if (doc == null) obj("missing" to JsonPrimitive(name)) else obj("found" to obj("name" to JsonPrimitive(name), "fields" to doc))
        },
    )
}
