package com.adhamelgendy.nixura.data.sync

import com.adhamelgendy.nixura.domain.sync.SyncRecord
import com.adhamelgendy.nixura.domain.sync.SyncType
import com.adhamelgendy.nixura.domain.time.CivilCalendar
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FirestoreSyncMappingTest {
    private val root = "projects/nixura/databases/(default)/documents/users/u1/sync"

    @Test fun jsonRoundTripsThroughFirestoreValues() {
        val payload = SyncJson.encodeToString(
            HabitPayload.serializer(),
            HabitPayload("Smoking", 10, null, 0xFFFFFFFFL, HabitCostPayload("cig", "PIECE", 1.0, 2_500, 20, "EUR", true)),
        )
        val json = SyncJson.parseToJsonElement(payload)
        val back = FirestoreValues.decode(FirestoreValues.encode(json))
        assertEquals(json, back)
        assertEquals(
            SyncJson.decodeFromString(HabitPayload.serializer(), payload),
            SyncJson.decodeFromString(HabitPayload.serializer(), back.toString()),
        )
    }

    @Test fun numbersKeepTheirKind() {
        val encoded = FirestoreValues.encode(SyncJson.parseToJsonElement("""{"a":3,"b":2.5,"c":1.0,"d":"7","e":null,"f":[true]}""")).toString()
        assertTrue("\"integerValue\":\"3\"" in encoded, encoded)
        assertTrue("\"doubleValue\":2.5" in encoded && "\"doubleValue\":1.0" in encoded, encoded)
        assertTrue("\"stringValue\":\"7\"" in encoded && "\"nullValue\":null" in encoded && "\"booleanValue\":true" in encoded, encoded)
    }

    @Test fun timestampsParseAndFormatAtMicrosecondPrecision() {
        val micros = FirestoreTime.parseMicros("2026-11-14T09:30:05.123456Z")!!
        assertEquals("2026-11-14T09:30:05.123456Z", FirestoreTime.formatMicros(micros))
        assertEquals(micros / 1_000_000 * 1_000_000, FirestoreTime.parseMicros("2026-11-14T09:30:05Z"))
        assertEquals(FirestoreTime.parseMicros("2026-11-14T09:30:05.1Z"), FirestoreTime.parseMicros("2026-11-14T11:30:05.100+02:00"))
        assertEquals(0L, FirestoreTime.parseMicros("1970-01-01T00:00:00Z"))
        assertNull(FirestoreTime.parseMicros("yesterday"))
    }

    @Test fun cursorRoundTripsAndTheStartIsNull() {
        assertNull(FirestoreCursor.decode("0"))
        val exact = FirestoreCursor("2026-11-14T09:30:05.123456Z", "$root/habit~h1")
        assertEquals(exact, FirestoreCursor.decode(exact.encode()))
        val from = FirestoreCursor("2026-11-14T09:30:05.123456Z", null)
        assertEquals(from, FirestoreCursor.decode(from.encode()))
    }

    @Test fun pullQueryStartsAfterTheLastDocumentOrFromATimeIncluded() {
        fun startAt(c: FirestoreCursor?) = FirestoreSyncMapping.pullQuery(c, 300)["structuredQuery"]!!.jsonObject["startAt"]?.jsonObject
        val after = startAt(FirestoreCursor("t", "n"))!!
        assertEquals("false", after["before"].toString())
        assertEquals(2, after["values"]!!.jsonArray.size)
        assertEquals("true", startAt(FirestoreCursor("t", null))!!["before"].toString())
        assertNull(startAt(null))
    }

    @Test fun anItemWriteReplacesTheDocumentAndStampsServerTime() {
        val payload = SyncJson.encodeToString(GoodHabitPayload.serializer(), GoodHabitPayload("Run", 1, null, 1, null, 3))
        val w = FirestoreSyncMapping.writes(listOf(SyncRecord(SyncType.GoodHabit, "g1", 5, false, payload)), root, capAt = null).single()
        assertEquals("good_habit~g1", w.docId)
        val update = w.write["update"]!!.jsonObject
        assertEquals("$root/good_habit~g1", update["name"]!!.jsonPrimitive.content)
        assertNull(w.write["updateMask"])
        assertEquals("REQUEST_TIME", w.write["updateTransforms"]!!.jsonArray.single().jsonObject["setToServerValue"]!!.jsonPrimitive.content)
        val data = FirestoreValues.decode(update["fields"]!!.jsonObject["data"]!!).jsonObject
        assertEquals(JsonPrimitive(3L), data["weeklyTarget"])
    }

    @Test fun aDeleteKeepsNoContent() {
        val r = SyncRecord(SyncType.Habit, "h1", 5, true, SyncJson.encodeToString(HabitPayload.serializer(), HabitPayload("Secret", 1)))
        val fields = FirestoreSyncMapping.writes(listOf(r), root, null).single().write["update"]!!.jsonObject["fields"]!!.jsonObject
        assertEquals(JsonObject(emptyMap()), FirestoreValues.decode(fields["data"]!!))
        assertTrue("Secret" !in fields.toString())
        val back = FirestoreSyncMapping.records(JsonObject(mapOf("name" to JsonPrimitive("$root/habit~h1"), "fields" to fields))).single()
        assertTrue(back.deleted)
        assertEquals("h1", back.id)
        assertEquals(5L, back.updatedAt)
    }

    @Test fun aLogDayGoesIntoItsMonthWithOneDayPerWrite() {
        val day = CivilCalendar.dayIndexFromCivil(2026, 2, 3)
        val payload = SyncJson.encodeToString(GoodHabitLogPayload.serializer(), GoodHabitLogPayload("g1", day, 2))
        val r = SyncRecord(SyncType.GoodHabitLog, "g1:$day", 7, false, payload)
        val w = FirestoreSyncMapping.writes(listOf(r), root, null).single()
        assertEquals("goodlog~g1~2026-02", w.docId)
        assertEquals(
            listOf("type", "habitId", "month", "lastDay", "days.d03"),
            w.write["updateMask"]!!.jsonObject["fieldPaths"]!!.jsonArray.map { it.jsonPrimitive.content },
        )
        val back = FirestoreSyncMapping.records(JsonObject(mapOf("fields" to w.write["update"]!!.jsonObject["fields"]!!))).single()
        assertEquals(r.copy(payload = ""), back.copy(payload = ""))
        assertEquals(
            SyncJson.decodeFromString(GoodHabitLogPayload.serializer(), payload),
            SyncJson.decodeFromString(GoodHabitLogPayload.serializer(), back.payload),
        )
    }

    @Test fun twoDaysOfOneMonthGoInTwoRounds() {
        fun log(day: Long) = SyncRecord(SyncType.GoodHabitLog, "g1:$day", 1, false, SyncJson.encodeToString(GoodHabitLogPayload.serializer(), GoodHabitLogPayload("g1", day, 1)))
        val d = CivilCalendar.dayIndexFromCivil(2026, 11, 10)
        val habit = SyncRecord(SyncType.Habit, "h1", 1, false, SyncJson.encodeToString(HabitPayload.serializer(), HabitPayload("x", 1)))
        val rounds = FirestoreSyncMapping.rounds(FirestoreSyncMapping.writes(listOf(habit, log(d), log(d + 1), log(d + 30)), root, null))
        assertEquals(2, rounds.size)
        assertEquals(3, rounds[0].size) // the habit, November, December
        assertEquals(1, rounds[1].size)
        rounds.forEach { round -> assertEquals(round.size, round.map { it.docId }.toSet().size) }
    }

    @Test fun deviceTimesAheadOfTheServerAreCapped() {
        val r = SyncRecord(SyncType.Habit, "h1", 9_000, false, SyncJson.encodeToString(HabitPayload.serializer(), HabitPayload("x", 1)))
        val fields = FirestoreSyncMapping.writes(listOf(r), root, capAt = 5_000).single().write["update"]!!.jsonObject["fields"]!!.jsonObject
        assertEquals("5000", fields["updatedAt"]!!.jsonObject["integerValue"]!!.jsonPrimitive.content)
    }

    @Test fun unknownTypesFromANewerAppAreSkipped() {
        val doc = JsonObject(mapOf("fields" to JsonObject(mapOf("type" to JsonObject(mapOf("stringValue" to JsonPrimitive("journal")))))))
        assertTrue(FirestoreSyncMapping.records(doc).isEmpty())
    }
}
