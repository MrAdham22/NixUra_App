package com.adhamelgendy.nixura.domain.reminder

import kotlinx.coroutines.flow.Flow

/**
 * Reminder state that belongs to this phone only (never synced, never sent anywhere).
 *
 * @param location where prayer times are computed for (read once when the user allowed it; PLAN.md decision 1).
 * @param pausedUntilEpochMillis "Pause habit reminders" in Settings: none before this moment, on this phone.
 * @param snoozes habit id → when the snoozed reminder shows again.
 * @param todayHiddenDay the day "Today" was dismissed on: it stays away for that day.
 */
data class ReminderDeviceState(
    val location: GeoPoint? = null,
    val pausedUntilEpochMillis: Long? = null,
    val snoozes: Map<Long, Long> = emptyMap(),
    val todayHiddenDay: Long? = null,
)

/** Stores [ReminderDeviceState] and the record of handled notification buttons. */
interface ReminderDeviceRepository {
    fun observe(): Flow<ReminderDeviceState>

    suspend fun setLocation(point: GeoPoint?)

    suspend fun setPausedUntil(epochMillis: Long?)

    suspend fun snooze(habitId: Long, untilEpochMillis: Long)

    suspend fun clearSnooze(habitId: Long)

    /** "Hide today" or a swipe: the Today notification stays away for [day]. `null` brings it back. */
    suspend fun setTodayHiddenDay(day: Long?)

    /**
     * Records that the button [token] was handled. Returns false when it already was: Android can deliver the same
     * button press twice (a double tap, a re-delivered broadcast after the process died), and it must count once.
     */
    suspend fun claimAction(token: String, nowEpochMillis: Long): Boolean

    /** Forgets [token] again (the write it guarded failed, so a new press may retry). */
    suspend fun releaseAction(token: String)
}
