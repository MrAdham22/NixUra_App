package com.adhamelgendy.nixura.notifications

import com.adhamelgendy.nixura.domain.notification.NotificationAction
import com.adhamelgendy.nixura.domain.notification.NotificationActionType
import com.adhamelgendy.nixura.domain.notification.NotificationKind
import com.adhamelgendy.nixura.domain.notification.NotificationProgress
import com.adhamelgendy.nixura.domain.notification.NotificationTiming
import com.adhamelgendy.nixura.domain.notification.PlannedNotification
import com.adhamelgendy.nixura.domain.theme.HabitTheme
import org.json.JSONArray
import org.json.JSONObject
import com.adhamelgendy.nixura.domain.recognition.HabitTopic

/**
 * One planned notification as JSON: the stored plan (re-armed after reboot) and the alarm's own extra (so the
 * receiver posts exactly what was planned, buttons included). Fields added later are optional when read.
 */
internal object PlannedNotificationJson {

    fun encode(item: PlannedNotification): JSONObject = JSONObject()
        .put("key", item.key)
        .put("at", item.triggerAtEpochMillis)
        .put("title", item.title)
        .put("body", item.body)
        .put("kind", item.kind.name)
        .put("timing", item.timing.name)
        .apply {
            item.targetId?.let { put("target", it) }
            item.theme?.let { put("theme", it.name) }
            item.topic?.let { put("topic", it.name) }
            if (item.postKey != item.key) put("post", item.postKey)
            item.publicTitle?.let { put("public", it) }
            item.progress?.let { put("progress", JSONArray().put(it.current).put(it.max)) }
            if (item.actions.isNotEmpty()) {
                put(
                    "actions",
                    JSONArray().apply {
                        item.actions.forEach { action ->
                            put(
                                JSONObject()
                                    .put("type", action.type.name)
                                    .put("label", action.label)
                                    .put("habit", action.habitId)
                                    .apply {
                                        action.prayerIndex?.let { put("prayer", it) }
                                        action.snoozeMinutes?.let { put("snooze", it) }
                                    },
                            )
                        }
                    },
                )
            }
        }

    /** Entries written before phase B have no kind / target / theme: the key prefix decides the kind. */
    fun decode(o: JSONObject): PlannedNotification {
        val key = o.getString("key")
        return PlannedNotification(
            key = key,
            triggerAtEpochMillis = o.getLong("at"),
            title = o.getString("title"),
            body = o.getString("body"),
            kind = enumOrNull<NotificationKind>(o.optString("kind")) ?: NotificationKind.fromKey(key),
            targetId = if (o.has("target")) o.getLong("target") else null,
            theme = enumOrNull<HabitTheme>(o.optString("theme")),
            topic = enumOrNull<HabitTopic>(o.optString("topic")),
            postKey = o.optString("post").ifEmpty { key },
            publicTitle = o.optString("public").ifEmpty { null },
            actions = o.optJSONArray("actions")?.let { array ->
                (0 until array.length()).mapNotNull { i ->
                    val a = array.getJSONObject(i)
                    val type = enumOrNull<NotificationActionType>(a.optString("type")) ?: return@mapNotNull null
                    NotificationAction(
                        type = type,
                        label = a.getString("label"),
                        habitId = a.getLong("habit"),
                        prayerIndex = if (a.has("prayer")) a.getInt("prayer") else null,
                        snoozeMinutes = if (a.has("snooze")) a.getInt("snooze") else null,
                    )
                }
            }.orEmpty(),
            progress = o.optJSONArray("progress")?.let { NotificationProgress(it.getInt(0), it.getInt(1)) },
            timing = enumOrNull<NotificationTiming>(o.optString("timing")) ?: NotificationTiming.ON_TIME,
        )
    }

    fun encodeAll(items: List<PlannedNotification>): String = JSONArray().apply { items.forEach { put(encode(it)) } }.toString()

    fun decodeAll(raw: String?): List<PlannedNotification> {
        if (raw.isNullOrEmpty()) return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            (0 until array.length()).map { decode(array.getJSONObject(it)) }
        }.getOrDefault(emptyList())
    }

    private inline fun <reified T : Enum<T>> enumOrNull(name: String?): T? =
        name?.takeIf { it.isNotEmpty() }?.let { n -> enumValues<T>().firstOrNull { it.name == n } }
}
