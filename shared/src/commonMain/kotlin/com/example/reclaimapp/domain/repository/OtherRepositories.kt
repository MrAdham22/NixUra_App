package com.example.reclaimapp.domain.repository

import com.example.reclaimapp.domain.model.AppSettings
import com.example.reclaimapp.domain.model.CoachMessage
import com.example.reclaimapp.domain.model.CoachRole
import com.example.reclaimapp.domain.model.Event
import com.example.reclaimapp.domain.model.EventCategory
import com.example.reclaimapp.domain.model.GoodHabitWithLogs
import com.example.reclaimapp.domain.model.UserProfile
import kotlinx.coroutines.flow.Flow

interface GoodHabitRepository {
    fun observeGoodHabits(): Flow<List<GoodHabitWithLogs>>
    suspend fun create(name: String, dailyTarget: Int, colorArgb: Long?): Long
    suspend fun update(id: Long, name: String, dailyTarget: Int, colorArgb: Long?)
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
    fun observeMessages(): Flow<List<CoachMessage>>
    suspend fun append(role: CoachRole, text: String): Long
    suspend fun clear()
}

interface PreferencesRepository {
    fun observeProfile(): Flow<UserProfile>
    fun observeSettings(): Flow<AppSettings>
    suspend fun saveProfile(profile: UserProfile)
    suspend fun saveSettings(settings: AppSettings)
}

interface DataMaintenanceRepository {
    suspend fun clearAllData()
}
