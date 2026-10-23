package com.example.reclaimapp.data.repository

import com.example.reclaimapp.data.local.dao.CoachMessageDao
import com.example.reclaimapp.data.local.dao.EventDao
import com.example.reclaimapp.data.local.dao.GoodHabitDao
import com.example.reclaimapp.data.local.dao.GoodHabitLogDao
import com.example.reclaimapp.data.local.dao.MaintenanceDao
import com.example.reclaimapp.data.local.dao.PreferenceDao
import com.example.reclaimapp.data.local.entity.CoachMessageEntity
import com.example.reclaimapp.data.local.entity.EventEntity
import com.example.reclaimapp.data.local.entity.GoodHabitEntity
import com.example.reclaimapp.data.local.entity.GoodHabitLogEntity
import com.example.reclaimapp.data.local.entity.PreferenceEntity
import com.example.reclaimapp.data.mapper.encodeReminders
import com.example.reclaimapp.data.mapper.toDomain
import com.example.reclaimapp.domain.clock.Clock
import com.example.reclaimapp.domain.model.AppSettings
import com.example.reclaimapp.domain.model.CoachMessage
import com.example.reclaimapp.domain.model.CoachRole
import com.example.reclaimapp.domain.model.Event
import com.example.reclaimapp.domain.model.EventCategory
import com.example.reclaimapp.domain.model.GoodHabitLog
import com.example.reclaimapp.domain.model.GoodHabitWithLogs
import com.example.reclaimapp.domain.model.UserProfile
import com.example.reclaimapp.domain.repository.CoachRepository
import com.example.reclaimapp.domain.repository.DataMaintenanceRepository
import com.example.reclaimapp.domain.repository.EventRepository
import com.example.reclaimapp.domain.repository.GoodHabitRepository
import com.example.reclaimapp.domain.repository.PreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

class GoodHabitRepositoryImpl(
    private val goodHabitDao: GoodHabitDao,
    private val logDao: GoodHabitLogDao,
    private val clock: Clock,
) : GoodHabitRepository {

    override fun observeGoodHabits(): Flow<List<GoodHabitWithLogs>> =
        goodHabitDao.observeActive().combine(logDao.observeAll()) { habits, logs ->
            val byHabit = logs.groupBy { it.goodHabitId }
            habits.map { entity ->
                GoodHabitWithLogs(
                    habit = entity.toDomain(),
                    logs = byHabit[entity.id].orEmpty().map { it.toDomain() },
                )
            }
        }

    override suspend fun create(name: String, dailyTarget: Int, colorArgb: Long?): Long =
        goodHabitDao.insert(
            GoodHabitEntity(
                name = name,
                dailyTarget = dailyTarget,
                colorArgb = colorArgb,
                createdAtEpochMillis = clock.nowEpochMillis(),
            ),
        )

    override suspend fun update(id: Long, name: String, dailyTarget: Int, colorArgb: Long?) =
        goodHabitDao.updateDetails(id, name, dailyTarget, colorArgb)

    override suspend fun archive(id: Long) = goodHabitDao.archive(id, clock.nowEpochMillis())

    override suspend fun setCount(id: Long, dayIndex: Long, count: Int) {
        if (count <= 0) {
            logDao.delete(id, dayIndex)
        } else {
            logDao.upsert(GoodHabitLogEntity(goodHabitId = id, dayIndex = dayIndex, count = count))
        }
    }
}

class EventRepositoryImpl(
    private val dao: EventDao,
    private val clock: Clock,
) : EventRepository {

    override fun observeEvents(): Flow<List<Event>> = dao.observeAll().map { list -> list.map { it.toDomain() } }

    override suspend fun create(title: String, category: EventCategory, targetAtEpochMillis: Long, reminderLeadMillis: List<Long>): Long =
        dao.insert(
            EventEntity(
                title = title,
                category = category.name,
                targetAtEpochMillis = targetAtEpochMillis,
                reminders = encodeReminders(reminderLeadMillis),
                createdAtEpochMillis = clock.nowEpochMillis(),
            ),
        )

    override suspend fun update(id: Long, title: String, category: EventCategory, targetAtEpochMillis: Long, reminderLeadMillis: List<Long>) =
        dao.update(id, title, category.name, targetAtEpochMillis, encodeReminders(reminderLeadMillis))

    override suspend fun delete(id: Long) = dao.delete(id)
}

class CoachRepositoryImpl(
    private val dao: CoachMessageDao,
    private val clock: Clock,
) : CoachRepository {

    override fun observeMessages(): Flow<List<CoachMessage>> = dao.observeAll().map { list -> list.map { it.toDomain() } }

    override suspend fun append(role: CoachRole, text: String): Long =
        dao.insert(CoachMessageEntity(role = role.name, text = text, createdAtEpochMillis = clock.nowEpochMillis()))

    override suspend fun clear() = dao.clear()
}

class PreferencesRepositoryImpl(
    private val dao: PreferenceDao,
) : PreferencesRepository {

    override fun observeProfile(): Flow<UserProfile> = dao.observeAll().map { items ->
        val map = items.associate { it.key to it.value }
        UserProfile(
            name = map[KEY_NAME].orEmpty(),
            email = map[KEY_EMAIL].orEmpty(),
            age = map[KEY_AGE]?.toIntOrNull(),
        )
    }

    override fun observeSettings(): Flow<AppSettings> = dao.observeAll().map { items ->
        val map = items.associate { it.key to it.value }
        val defaults = AppSettings()
        AppSettings(
            darkTheme = map[KEY_DARK]?.let { it == "1" } ?: defaults.darkTheme,
            languageCode = map[KEY_LANGUAGE] ?: defaults.languageCode,
            notificationsEnabled = map[KEY_NOTIFICATIONS]?.let { it == "1" } ?: defaults.notificationsEnabled,
            dailyCheckInMinutes = map[KEY_CHECKIN]?.toIntOrNull() ?: defaults.dailyCheckInMinutes,
            onboardingCompleted = map[KEY_ONBOARDED]?.let { it == "1" } ?: defaults.onboardingCompleted,
        )
    }

    override suspend fun saveProfile(profile: UserProfile) {
        dao.upsertAll(
            listOf(
                PreferenceEntity(KEY_NAME, profile.name),
                PreferenceEntity(KEY_EMAIL, profile.email),
            ),
        )
        val age = profile.age
        if (age == null) dao.delete(KEY_AGE) else dao.upsertAll(listOf(PreferenceEntity(KEY_AGE, age.toString())))
    }

    override suspend fun saveSettings(settings: AppSettings) {
        dao.upsertAll(
            listOf(
                PreferenceEntity(KEY_DARK, if (settings.darkTheme) "1" else "0"),
                PreferenceEntity(KEY_LANGUAGE, settings.languageCode),
                PreferenceEntity(KEY_NOTIFICATIONS, if (settings.notificationsEnabled) "1" else "0"),
                PreferenceEntity(KEY_CHECKIN, settings.dailyCheckInMinutes.toString()),
                PreferenceEntity(KEY_ONBOARDED, if (settings.onboardingCompleted) "1" else "0"),
            ),
        )
    }

    private companion object {
        const val KEY_NAME = "profile.name"
        const val KEY_EMAIL = "profile.email"
        const val KEY_AGE = "profile.age"
        const val KEY_DARK = "settings.dark"
        const val KEY_LANGUAGE = "settings.language"
        const val KEY_NOTIFICATIONS = "settings.notifications"
        const val KEY_CHECKIN = "settings.checkin_minutes"
        const val KEY_ONBOARDED = "settings.onboarded"
    }
}

class DataMaintenanceRepositoryImpl(
    private val dao: MaintenanceDao,
) : DataMaintenanceRepository {
    override suspend fun clearAllData() = dao.clearAll()
}
