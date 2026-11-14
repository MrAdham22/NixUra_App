package com.adhamelgendy.nixura.data.sync

import com.adhamelgendy.nixura.domain.time.CivilCalendar
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put

internal val SyncJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
}

/**
 * Plain JSON to Firestore's typed REST values and back (`{"stringValue": "x"}`, `{"integerValue": "3"}`, ...), so
 * synced items are stored as readable fields that a server can query, not as an opaque string.
 */
internal object FirestoreValues {

    fun encode(value: JsonElement): JsonObject = when (value) {
        is JsonNull -> buildJsonObject { put("nullValue", JsonNull) }
        is JsonObject -> buildJsonObject { put("mapValue", buildJsonObject { put("fields", encodeFields(value)) }) }
        is JsonArray -> buildJsonObject { put("arrayValue", buildJsonObject { put("values", JsonArray(value.map { encode(it) })) }) }
        is JsonPrimitive -> when {
            value.isString -> buildJsonObject { put("stringValue", value.content) }
            value.booleanOrNull != null -> buildJsonObject { put("booleanValue", value.booleanOrNull!!) }
            isWholeNumber(value.content) && value.longOrNull != null -> buildJsonObject { put("integerValue", value.content) }
            else -> buildJsonObject { put("doubleValue", value.doubleOrNull ?: 0.0) }
        }
    }

    fun encodeFields(obj: JsonObject): JsonObject = JsonObject(obj.mapValues { (_, v) -> encode(v) })

    /** Firestore's value back to plain JSON. Timestamps and references become strings; unknown kinds become null. */
    fun decode(value: JsonElement): JsonElement {
        val obj = value as? JsonObject ?: return JsonNull
        val (kind, inner) = obj.entries.firstOrNull() ?: return JsonNull
        return when (kind) {
            "stringValue", "timestampValue", "referenceValue" -> JsonPrimitive(inner.jsonPrimitive.content)
            "integerValue" -> inner.jsonPrimitive.content.toLongOrNull()?.let { JsonPrimitive(it) } ?: JsonNull
            "doubleValue" -> inner.jsonPrimitive.doubleOrNull?.let { JsonPrimitive(it) } ?: JsonNull
            "booleanValue" -> JsonPrimitive(inner.jsonPrimitive.booleanOrNull == true)
            "mapValue" -> decodeFields((inner as? JsonObject)?.get("fields"))
            "arrayValue" -> buildJsonArray {
                ((inner as? JsonObject)?.get("values") as? JsonArray)?.forEach { add(decode(it)) }
            }
            else -> JsonNull
        }
    }

    fun decodeFields(fields: JsonElement?): JsonObject =
        JsonObject((fields as? JsonObject)?.mapValues { (_, v) -> decode(v) } ?: emptyMap())

    private fun isWholeNumber(text: String) = text.none { it == '.' || it == 'e' || it == 'E' }
}

/** RFC 3339 times as Firestore writes them (`2026-11-14T09:30:00.123456Z`), kept at microsecond precision. */
internal object FirestoreTime {

    fun parseMicros(text: String): Long? = runCatching {
        val t = text.trim()
        require(t.length >= 20 && t[4] == '-' && t[7] == '-' && t[10] == 'T' && t[13] == ':' && t[16] == ':')
        val day = CivilCalendar.dayIndexFromCivil(t.substring(0, 4).toInt(), t.substring(5, 7).toInt(), t.substring(8, 10).toInt())
        val seconds = day * 86_400L + t.substring(11, 13).toInt() * 3_600L + t.substring(14, 16).toInt() * 60L + t.substring(17, 19).toInt()
        var rest = t.substring(19)
        var micros = 0L
        if (rest.startsWith(".")) {
            val digits = rest.drop(1).takeWhile { it.isDigit() }
            micros = digits.take(6).padEnd(6, '0').toLong()
            rest = rest.drop(1 + digits.length)
        }
        val offsetSeconds = when {
            rest == "Z" || rest == "z" -> 0L
            rest.length == 6 && (rest[0] == '+' || rest[0] == '-') -> {
                val sign = if (rest[0] == '-') -1L else 1L
                sign * (rest.substring(1, 3).toLong() * 3_600L + rest.substring(4, 6).toLong() * 60L)
            }
            else -> error("bad zone")
        }
        (seconds - offsetSeconds) * 1_000_000L + micros
    }.getOrNull()

    fun formatMicros(epochMicros: Long): String {
        val seconds = epochMicros.floorDiv(1_000_000L)
        val micros = epochMicros.mod(1_000_000L)
        val day = seconds.floorDiv(86_400L)
        val secOfDay = seconds.mod(86_400L)
        val date = CivilCalendar.civilFromDayIndex(day)
        return buildString {
            append(date.year.toString().padStart(4, '0')).append('-')
            append(two(date.month)).append('-').append(two(date.day)).append('T')
            append(two((secOfDay / 3_600L).toInt())).append(':')
            append(two((secOfDay % 3_600L / 60L).toInt())).append(':')
            append(two((secOfDay % 60L).toInt())).append('.')
            append(micros.toString().padStart(6, '0')).append('Z')
        }
    }

    private fun two(n: Int) = n.toString().padStart(2, '0')
}

/** A tiny JSON builder helper: `obj("a" to JsonPrimitive(1))`. */
internal fun obj(vararg pairs: Pair<String, JsonElement>): JsonObject = JsonObject(mapOf(*pairs))

internal fun JsonElement?.stringOrNull(): String? = (this as? JsonPrimitive)?.takeIf { it.isString }?.content
