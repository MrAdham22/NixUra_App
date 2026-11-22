package com.adhamelgendy.nixura.data.repository

import com.adhamelgendy.nixura.data.local.dao.PreferenceDao
import com.adhamelgendy.nixura.data.local.entity.PreferenceEntity
import com.adhamelgendy.nixura.domain.reminder.GeoPoint
import com.adhamelgendy.nixura.domain.reminder.ReminderDeviceRepository
import com.adhamelgendy.nixura.domain.reminder.ReminderDeviceState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * This phone's reminder state in the local `preferences` table. None of these keys is in the synced settings set
 * ([PreferencesRepositoryImpl.saveSettings] uploads only its own keys), so the location never leaves the device.
 */
class ReminderDeviceRepositoryImpl(private val dao: PreferenceDao) : ReminderDeviceRepository {

    /** One writer at a time for the read-check-write of [claimAction] (receivers can run concurrently). */
    private val lock = Mutex()

    override fun observe(): Flow<ReminderDeviceState> = dao.observeAll()
        .map { rows -> stateOf(rows.associate { it.key to it.value }) }
        .distinctUntilChanged() // handled-button records also live in this table; they do not change the state

    override suspend fun setLocation(point: GeoPoint?) {
        if (point == null) dao.delete(KEY_LOCATION) else dao.upsertAll(listOf(PreferenceEntity(KEY_LOCATION, "${point.latitude},${point.longitude}")))
    }

    override suspend fun setPausedUntil(epochMillis: Long?) {
        if (epochMillis == null) dao.delete(KEY_PAUSED) else dao.upsertAll(listOf(PreferenceEntity(KEY_PAUSED, epochMillis.toString())))
    }

    override suspend fun snooze(habitId: Long, untilEpochMillis: Long) =
        dao.upsertAll(listOf(PreferenceEntity(SNOOZE_PREFIX + habitId, untilEpochMillis.toString())))

    override suspend fun clearSnooze(habitId: Long) = dao.delete(SNOOZE_PREFIX + habitId)

    override suspend fun setTodayHiddenDay(day: Long?) {
        if (day == null) dao.delete(KEY_TODAY_HIDDEN) else dao.upsertAll(listOf(PreferenceEntity(KEY_TODAY_HIDDEN, day.toString())))
    }

    override suspend fun claimAction(token: String, nowEpochMillis: Long): Boolean = lock.withLock {
        val rows = dao.getAll()
        if (rows.any { it.key == HANDLED_PREFIX + token }) return@withLock false
        // Forget presses (and snoozes) older than two days: a reminder is never re-delivered that late.
        val old = nowEpochMillis - KEEP_MILLIS
        rows.filter { (it.key.startsWith(HANDLED_PREFIX) || it.key.startsWith(SNOOZE_PREFIX)) && (it.value.toLongOrNull() ?: 0L) < old }
            .forEach { dao.delete(it.key) }
        dao.upsertAll(listOf(PreferenceEntity(HANDLED_PREFIX + token, nowEpochMillis.toString())))
        true
    }

    override suspend fun releaseAction(token: String) = lock.withLock { dao.delete(HANDLED_PREFIX + token) }

    private fun stateOf(map: Map<String, String>): ReminderDeviceState = ReminderDeviceState(
        location = map[KEY_LOCATION]?.split(',')?.takeIf { it.size == 2 }?.let { (lat, lon) ->
            runCatching { GeoPoint(lat.toDouble(), lon.toDouble()) }.getOrNull()
        },
        pausedUntilEpochMillis = map[KEY_PAUSED]?.toLongOrNull(),
        todayHiddenDay = map[KEY_TODAY_HIDDEN]?.toLongOrNull(),
        snoozes = map.mapNotNull { (key, value) ->
            if (!key.startsWith(SNOOZE_PREFIX)) return@mapNotNull null
            val id = key.removePrefix(SNOOZE_PREFIX).toLongOrNull() ?: return@mapNotNull null
            value.toLongOrNull()?.let { id to it }
        }.toMap(),
    )

    internal companion object {
        const val KEY_LOCATION = "device.reminders.location"
        const val KEY_PAUSED = "device.reminders.paused_until"
        const val KEY_TODAY_HIDDEN = "device.today.hidden_day"
        const val SNOOZE_PREFIX = "device.reminders.snooze."
        const val HANDLED_PREFIX = "device.reminders.handled."
        const val KEEP_MILLIS = 2 * 24 * 60 * 60_000L
    }
}
