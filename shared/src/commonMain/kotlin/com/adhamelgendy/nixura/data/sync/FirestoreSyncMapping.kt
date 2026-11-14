package com.adhamelgendy.nixura.data.sync

import com.adhamelgendy.nixura.domain.sync.SyncRecord
import com.adhamelgendy.nixura.domain.sync.SyncType
import com.adhamelgendy.nixura.domain.time.CivilCalendar
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.longOrNull

/** One Firestore write and the synced records it carries (a good-habit day write carries one log record). */
internal data class PlannedWrite(val docId: String, val write: JsonObject, val records: List<SyncRecord>)

/** Where the pull continues: after ([serverTime], [docName]), or from [serverTime] included when [docName] is null. */
internal data class FirestoreCursor(val serverTime: String, val docName: String?) {
    fun encode(): String = "v1|$serverTime|${docName.orEmpty()}"

    companion object {
        /** "0" (or anything unreadable) means "from the start". */
        fun decode(text: String): FirestoreCursor? {
            val parts = text.split('|', limit = 3)
            if (parts.size != 3 || parts[0] != "v1" || parts[1].isEmpty()) return null
            return FirestoreCursor(parts[1], parts[2].ifEmpty { null })
        }
    }
}

/**
 * How synced records are laid out in Firestore (see docs/DEVELOPMENT_STATUS.md, "Firestore sync design"):
 *
 * - `users/{uid}/sync/{type~id}` holds one item: `type`, `id`, `updatedAt` (device edit time, decides the winner),
 *   `deleted`, `serverTime` (set by Firestore, the pull cursor) and `data` (the item's fields; emptied when deleted).
 * - `users/{uid}/sync/goodlog~{habitId}~{yyyy-MM}` holds a good habit's month: `days.dDD` = `{id, count, updatedAt,
 *   deleted}`, so a year is 12 documents per habit instead of 365. Each write changes exactly one day (`lastDay`), so
 *   the security rules can refuse an older day overwriting a newer one.
 */
internal object FirestoreSyncMapping {
    const val LOG_MONTH_TYPE = "good_habit_log_month"
    const val COLLECTION = "sync"
    private const val LOG_PREFIX = "goodlog~"

    fun itemDocId(type: SyncType, id: String) = "${type.wire}~$id"

    /**
     * The writes for [records] under [docsRoot] (`projects/p/databases/(default)/documents/users/{uid}/sync`).
     * Device times later than [capAt] (the server's clock as last seen) are lowered to it, so a phone whose clock runs
     * ahead is not refused by the rules and cannot win every later conflict. Records that cannot be mapped are skipped.
     */
    fun writes(records: List<SyncRecord>, docsRoot: String, capAt: Long?): List<PlannedWrite> = records.mapNotNull { r ->
        val updatedAt = if (capAt != null) minOf(r.updatedAt, capAt) else r.updatedAt
        if (r.type == SyncType.GoodHabitLog) logWrite(r, updatedAt, docsRoot) else itemWrite(r, updatedAt, docsRoot)
    }

    /**
     * One write per document per commit, so a month with two changed days needs two rounds: the rules check one changed
     * day per write, and that day's time against the stored one.
     */
    fun rounds(writes: List<PlannedWrite>): List<List<PlannedWrite>> {
        val byDoc = writes.groupBy { it.docId }.values
        val depth = byDoc.maxOfOrNull { it.size } ?: 0
        return (0 until depth).map { round -> byDoc.mapNotNull { it.getOrNull(round) } }
    }

    private fun itemWrite(r: SyncRecord, updatedAt: Long, docsRoot: String): PlannedWrite? {
        val data: JsonObject = if (r.deleted) {
            JsonObject(emptyMap()) // a delete keeps only a small marker, the content is gone
        } else {
            runCatching { SyncJson.parseToJsonElement(r.payload) as JsonObject }.getOrNull() ?: return null
        }
        val docId = itemDocId(r.type, r.id)
        val fields = obj(
            "type" to str(r.type.wire),
            "id" to str(r.id),
            "updatedAt" to int(updatedAt),
            "deleted" to bool(r.deleted),
            "data" to FirestoreValues.encode(data),
        )
        return PlannedWrite(docId, update("$docsRoot/$docId", fields, mask = null), listOf(r))
    }

    private fun logWrite(r: SyncRecord, updatedAt: Long, docsRoot: String): PlannedWrite? {
        val p = runCatching { SyncJson.decodeFromString(GoodHabitLogPayload.serializer(), r.payload) }.getOrNull() ?: return null
        val date = CivilCalendar.civilFromDayIndex(p.dayIndex)
        val month = "${date.year.toString().padStart(4, '0')}-${two(date.month)}"
        val dayKey = "d${two(date.day)}"
        val docId = "$LOG_PREFIX${p.goodHabitId}~$month"
        val day = obj(
            "id" to str(r.id),
            "count" to int(if (r.deleted) 0L else p.count.toLong()),
            "updatedAt" to int(updatedAt),
            "deleted" to bool(r.deleted),
        )
        val fields = obj(
            "type" to str(LOG_MONTH_TYPE),
            "habitId" to str(p.goodHabitId),
            "month" to str(month),
            "lastDay" to str(dayKey),
            "days" to mapValue(obj(dayKey to mapValue(day))),
        )
        val mask = listOf("type", "habitId", "month", "lastDay", "days.$dayKey")
        return PlannedWrite(docId, update("$docsRoot/$docId", fields, mask), listOf(r))
    }

    /** The records held by one pulled document. A type this version does not know (from a newer app) gives none. */
    fun records(document: JsonObject): List<SyncRecord> {
        val fields = document["fields"] as? JsonObject ?: return emptyList()
        val type = fields["type"].stringValue() ?: return emptyList()
        return if (type == LOG_MONTH_TYPE) logRecords(fields) else listOfNotNull(itemRecord(type, fields, document))
    }

    private fun itemRecord(type: String, fields: JsonObject, document: JsonObject): SyncRecord? {
        val syncType = SyncType.fromWire(type) ?: return null
        val id = fields["id"].stringValue()
            ?: document["name"].stringOrNull()?.substringAfterLast('/')?.substringAfter('~')
            ?: return null
        val updatedAt = fields["updatedAt"].longValue() ?: return null
        val deleted = fields["deleted"].boolValue() ?: false
        val data = FirestoreValues.decode(fields["data"] ?: mapValue(JsonObject(emptyMap())))
        return SyncRecord(syncType, id, updatedAt, deleted, SyncJson.encodeToString(JsonElement.serializer(), data))
    }

    private fun logRecords(fields: JsonObject): List<SyncRecord> {
        val habitId = fields["habitId"].stringValue() ?: return emptyList()
        val month = fields["month"].stringValue() ?: return emptyList()
        val year = month.substringBefore('-').toIntOrNull() ?: return emptyList()
        val monthNumber = month.substringAfter('-').toIntOrNull()?.takeIf { it in 1..12 } ?: return emptyList()
        val days = (fields["days"] as? JsonObject)?.get("mapValue")?.let { (it as? JsonObject)?.get("fields") } as? JsonObject
            ?: return emptyList()
        return days.mapNotNull { (key, value) ->
            val dayOfMonth = key.removePrefix("d").toIntOrNull()?.takeIf { key.startsWith("d") && it in 1..31 } ?: return@mapNotNull null
            val day = FirestoreValues.decode(value) as? JsonObject ?: return@mapNotNull null
            val dayIndex = CivilCalendar.dayIndexFromCivil(year, monthNumber, dayOfMonth)
            val updatedAt = day["updatedAt"].plainLong() ?: return@mapNotNull null
            val payload = GoodHabitLogPayload(habitId, dayIndex, day["count"].plainLong()?.toInt() ?: 0)
            SyncRecord(
                type = SyncType.GoodHabitLog,
                id = day["id"].stringOrNull() ?: "$habitId:$dayIndex",
                updatedAt = updatedAt,
                deleted = (day["deleted"] as? JsonPrimitive)?.booleanOrNull ?: false,
                payload = SyncJson.encodeToString(GoodHabitLogPayload.serializer(), payload),
            )
        }
    }

    /** The pull query for `users/{uid}/sync`, ordered by server time then document name. */
    fun pullQuery(cursor: FirestoreCursor?, limit: Int): JsonObject {
        val query = LinkedHashMap<String, JsonElement>()
        query["from"] = JsonArray(listOf(obj("collectionId" to JsonPrimitive(COLLECTION))))
        query["orderBy"] = JsonArray(
            listOf(
                obj("field" to obj("fieldPath" to JsonPrimitive("serverTime")), "direction" to JsonPrimitive("ASCENDING")),
                obj("field" to obj("fieldPath" to JsonPrimitive("__name__")), "direction" to JsonPrimitive("ASCENDING")),
            ),
        )
        if (cursor != null) {
            val values = buildList {
                add(obj("timestampValue" to JsonPrimitive(cursor.serverTime)))
                cursor.docName?.let { add(obj("referenceValue" to JsonPrimitive(it))) }
            }
            // With a document name: strictly after it. Without one: every document from that time on.
            query["startAt"] = obj("values" to JsonArray(values), "before" to JsonPrimitive(cursor.docName == null))
        }
        query["limit"] = JsonPrimitive(limit)
        return obj("structuredQuery" to JsonObject(query))
    }

    /** Lists document names only (for deleting an account's data), in name order. */
    fun namesQuery(limit: Int): JsonObject = obj(
        "structuredQuery" to obj(
            "select" to obj("fields" to JsonArray(listOf(obj("fieldPath" to JsonPrimitive("__name__"))))),
            "from" to JsonArray(listOf(obj("collectionId" to JsonPrimitive(COLLECTION)))),
            "orderBy" to JsonArray(listOf(obj("field" to obj("fieldPath" to JsonPrimitive("__name__")), "direction" to JsonPrimitive("ASCENDING")))),
            "limit" to JsonPrimitive(limit),
        ),
    )

    fun commitBody(writes: List<JsonObject>): JsonObject = obj("writes" to JsonArray(writes))

    fun deleteWrite(docName: String): JsonObject = obj("delete" to JsonPrimitive(docName))

    private fun update(name: String, fields: JsonObject, mask: List<String>?): JsonObject {
        val parts = LinkedHashMap<String, JsonElement>()
        parts["update"] = obj("name" to JsonPrimitive(name), "fields" to fields)
        if (mask != null) parts["updateMask"] = obj("fieldPaths" to JsonArray(mask.map { JsonPrimitive(it) }))
        parts["updateTransforms"] = JsonArray(
            listOf(obj("fieldPath" to JsonPrimitive("serverTime"), "setToServerValue" to JsonPrimitive("REQUEST_TIME"))),
        )
        return JsonObject(parts)
    }

    private fun str(value: String) = obj("stringValue" to JsonPrimitive(value))
    private fun int(value: Long) = obj("integerValue" to JsonPrimitive(value.toString()))
    private fun bool(value: Boolean) = obj("booleanValue" to JsonPrimitive(value))
    private fun mapValue(fields: JsonObject) = obj("mapValue" to obj("fields" to fields))
    private fun two(n: Int) = n.toString().padStart(2, '0')

    private fun JsonElement?.stringValue(): String? = ((this as? JsonObject)?.get("stringValue") as? JsonPrimitive)?.content
    private fun JsonElement?.longValue(): Long? = ((this as? JsonObject)?.get("integerValue") as? JsonPrimitive)?.content?.toLongOrNull()
    private fun JsonElement?.boolValue(): Boolean? = ((this as? JsonObject)?.get("booleanValue") as? JsonPrimitive)?.booleanOrNull
    private fun JsonElement?.plainLong(): Long? = (this as? JsonPrimitive)?.takeIf { !it.isString }?.longOrNull
        ?: (this as? JsonPrimitive)?.content?.toDoubleOrNull()?.toLong()
}
