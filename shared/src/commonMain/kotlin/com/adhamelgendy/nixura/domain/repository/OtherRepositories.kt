package com.adhamelgendy.nixura.domain.repository

import com.adhamelgendy.nixura.domain.model.AppSettings
import com.adhamelgendy.nixura.domain.model.CoachConversation
import com.adhamelgendy.nixura.domain.model.CoachMessage
import com.adhamelgendy.nixura.domain.model.CoachRole
import com.adhamelgendy.nixura.domain.model.Event
import com.adhamelgendy.nixura.domain.model.EventCategory
import com.adhamelgendy.nixura.domain.model.GoodHabitWithLogs
import com.adhamelgendy.nixura.domain.model.UserProfile
import kotlinx.coroutines.flow.Flow

interface GoodHabitRepository {
    fun observeGoodHabits(): Flow<List<GoodHabitWithLogs>>
    /** [weeklyTarget] `null` = daily habit; otherwise days a week (then [dailyTarget] is 1). */
    suspend fun create(name: String, dailyTarget: Int, colorArgb: Long?, weeklyTarget: Int? = null): Long
    suspend fun update(id: Long, name: String, dailyTarget: Int, colorArgb: Long?, weeklyTarget: Int? = null)
    suspend fun archive(id: Long)

    /** Sets the number of times the habit was done on [dayIndex]. `count <= 0` clears that day. */
    suspend fun setCount(id: Long, dayIndex: Long, count: Int)
}

interface EventRepository {
    fun observeEvents(): Flow<List<Event>>
    suspend fun create(title: String, category: EventCategory, targetAtEpochMillis: Long, reminderLeadMillis: List<Long>): Long
    suspend fun update(id: Long, title: String, category: EventCategory, targetAtEpochMillis: Long, reminderLeadMillis: List<Long>)
    suspend fun delete(id: Long)
}

interface CoachRepository {
    fun observeMessages(conversationId: Long): Flow<List<CoachMessage>>

    /** Chats the user took part in, newest first. */
    fun observeConversations(): Flow<List<CoachConversation>>
    suspend fun exists(conversationId: Long): Boolean
    suspend fun latestConversationId(): Long?

    /** A chat the user has not written in yet, if any. */
    suspend fun unusedConversationId(): Long?
    suspend fun hasMessages(conversationId: Long): Boolean

    /** Replaces the greeting of a chat the user has not written in yet, so it matches today's habits and name. */
    suspend fun refreshGreeting(conversationId: Long, text: String)
    suspend fun createConversation(): Long
    suspend fun append(conversationId: Long, role: CoachRole, text: String, visuals: List<String> = emptyList()): Long
    suspend fun delete(conversationId: Long)
}

interface PreferencesRepository {
    fun observeProfile(): Flow<UserProfile>
    fun observeSettings(): Flow<AppSettings>
    suspend fun saveProfile(profile: UserProfile)
    suspend fun saveSettings(settings: AppSettings)
}

interface DataMaintenanceRepository {
    /** Removes habits, events and chats from this device only. */
    suspend fun clearAllData()

    /** Marks habits and events as deleted, so the next sync removes them from the account and other devices too; chats go right away. */
    suspend fun markAllDataDeleted(atEpochMillis: Long)

    /** Deletes one quit habit and its history (here, on the account and on other devices after the next sync). */
    suspend fun deleteBadHabit(habitId: Long, atEpochMillis: Long)

    /** Deletes one good habit and all its logged days. */
    suspend fun deleteGoodHabit(habitId: Long, atEpochMillis: Long)
}
