package com.adhamelgendy.nixura.data.repository

import com.adhamelgendy.nixura.data.local.dao.CoachMessageDao
import com.adhamelgendy.nixura.data.local.dao.EventDao
import com.adhamelgendy.nixura.data.local.dao.GoodHabitDao
import com.adhamelgendy.nixura.data.local.dao.GoodHabitLogDao
import com.adhamelgendy.nixura.data.local.dao.MaintenanceDao
import com.adhamelgendy.nixura.data.local.dao.PreferenceDao
import com.adhamelgendy.nixura.data.local.entity.CoachConversationEntity
import com.adhamelgendy.nixura.data.local.entity.CoachMessageEntity
import com.adhamelgendy.nixura.data.local.entity.EventEntity
import com.adhamelgendy.nixura.data.local.entity.GoodHabitEntity
import com.adhamelgendy.nixura.data.local.entity.GoodHabitLogEntity
import com.adhamelgendy.nixura.data.local.entity.PreferenceEntity
import com.adhamelgendy.nixura.data.mapper.encodeReminders
import com.adhamelgendy.nixura.data.mapper.toDomain
import com.adhamelgendy.nixura.domain.clock.Clock
import com.adhamelgendy.nixura.domain.model.AppSettings
import com.adhamelgendy.nixura.domain.model.CoachConversation
import com.adhamelgendy.nixura.domain.model.CoachMessage
import com.adhamelgendy.nixura.domain.model.CoachRole
import com.adhamelgendy.nixura.domain.model.Event
import com.adhamelgendy.nixura.domain.model.EventCategory
import com.adhamelgendy.nixura.domain.model.GoodHabitLog
import com.adhamelgendy.nixura.domain.model.GoodHabitWithLogs
import com.adhamelgendy.nixura.domain.model.UserProfile
import com.adhamelgendy.nixura.domain.repository.CoachRepository
import com.adhamelgendy.nixura.domain.repository.DataMaintenanceRepository
import com.adhamelgendy.nixura.domain.repository.EventRepository
import com.adhamelgendy.nixura.domain.repository.GoodHabitRepository
import com.adhamelgendy.nixura.domain.repository.PreferencesRepository
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

    override suspend fun create(name: String, dailyTarget: Int, colorArgb: Long?, weeklyTarget: Int?): Long =
        goodHabitDao.insert(
            GoodHabitEntity(
                name = name,
                dailyTarget = dailyTarget,
                colorArgb = colorArgb,
                createdAtEpochMillis = clock.nowEpochMillis(),
                weeklyTarget = weeklyTarget,
            ),
        )

    override suspend fun update(id: Long, name: String, dailyTarget: Int, colorArgb: Long?, weeklyTarget: Int?) =
        goodHabitDao.updateDetails(id, name, dailyTarget, colorArgb, weeklyTarget, clock.nowEpochMillis())

    override suspend fun archive(id: Long) = goodHabitDao.archive(id, clock.nowEpochMillis())

    override suspend fun setCount(id: Long, dayIndex: Long, count: Int) {
        val now = clock.nowEpochMillis()
        if (count <= 0) {
            logDao.markDeleted(id, dayIndex, now)
        } else {
            val habitSyncId = goodHabitDao.syncIdOf(id) ?: return
            logDao.upsert(
                GoodHabitLogEntity(
                    goodHabitId = id,
                    dayIndex = dayIndex,
                    count = count,
                    syncId = "$habitSyncId:$dayIndex",
                    updatedAtEpochMillis = now,
                ),
            )
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
        dao.update(id, title, category.name, targetAtEpochMillis, encodeReminders(reminderLeadMillis), clock.nowEpochMillis())

    override suspend fun delete(id: Long) = dao.markDeleted(id, clock.nowEpochMillis())
}

class CoachRepositoryImpl(
    private val dao: CoachMessageDao,
    private val clock: Clock,
) : CoachRepository {

    override fun observeMessages(conversationId: Long): Flow<List<CoachMessage>> =
        dao.observeMessages(conversationId).map { list -> list.map { it.toDomain() } }

    override fun observeConversations(): Flow<List<CoachConversation>> =
        dao.observeConversations().map { rows -> rows.map { CoachConversation(it.id, it.title, it.preview.orEmpty(), it.updatedAtEpochMillis) } }

    override suspend fun exists(conversationId: Long): Boolean = dao.conversationExists(conversationId)

    override suspend fun latestConversationId(): Long? = dao.latestConversationId()

    override suspend fun unusedConversationId(): Long? = dao.unusedConversationId()

    override suspend fun hasMessages(conversationId: Long): Boolean = dao.messageCount(conversationId) > 0

    override suspend fun refreshGreeting(conversationId: Long, text: String) {
        dao.refreshUnusedGreeting(conversationId, text)
    }

    override suspend fun createConversation(): Long {
        val now = clock.nowEpochMillis()
        return dao.insertConversation(CoachConversationEntity(title = "", createdAtEpochMillis = now, updatedAtEpochMillis = now))
    }

    override suspend fun append(conversationId: Long, role: CoachRole, text: String, visuals: List<String>): Long = dao.append(
        CoachMessageEntity(
            conversationId = conversationId, role = role.name, text = text, createdAtEpochMillis = clock.nowEpochMillis(),
            visuals = visuals.takeIf { it.isNotEmpty() }?.joinToString(","),
        ),
        titleIfNew = if (role == CoachRole.USER) text.take(MAX_CHAT_TITLE) else "",
    )

    override suspend fun delete(conversationId: Long) = dao.deleteConversation(conversationId)

    private companion object {
        const val MAX_CHAT_TITLE = 60
    }
}

class PreferencesRepositoryImpl(
    private val dao: PreferenceDao,
    private val clock: Clock,
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
            guideCompleted = map[KEY_GUIDE_DONE]?.let { it == "1" } ?: defaults.guideCompleted,
            usageStatsEnabled = map[KEY_USAGE_STATS]?.let { it == "1" } ?: defaults.usageStatsEnabled,
            aiCoachConsent = map[KEY_AI_COACH]?.let { it == "1" },
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
        val before = dao.getAll().associate { it.key to it.value }
        val synced = listOf(
            PreferenceEntity(KEY_DARK, if (settings.darkTheme) "1" else "0"),
            PreferenceEntity(KEY_LANGUAGE, settings.languageCode),
            PreferenceEntity(KEY_NOTIFICATIONS, if (settings.notificationsEnabled) "1" else "0"),
            PreferenceEntity(KEY_CHECKIN, settings.dailyCheckInMinutes.toString()),
        )
        // The starter guide and the usage-stats choice are per device, so they do not count as a settings change to upload.
        val changed = synced.any { before[it.key] != it.value }
        val marks = if (changed) {
            listOf(PreferenceEntity(KEY_SETTINGS_DIRTY, "1"), PreferenceEntity(KEY_SETTINGS_UPDATED, clock.nowEpochMillis().toString()))
        } else {
            emptyList()
        }
        val local = listOf(
            PreferenceEntity(KEY_GUIDE_DONE, if (settings.guideCompleted) "1" else "0"),
            PreferenceEntity(KEY_USAGE_STATS, if (settings.usageStatsEnabled) "1" else "0"),
        ) + listOfNotNull(settings.aiCoachConsent?.let { PreferenceEntity(KEY_AI_COACH, if (it) "1" else "0") })
        dao.upsertAll(synced + local + marks)
    }

    internal companion object {
        const val KEY_SETTINGS_DIRTY = "sync.settings.dirty"
        const val KEY_SETTINGS_UPDATED = "sync.settings.updated_at"
        const val KEY_NAME = "profile.name"
        const val KEY_EMAIL = "profile.email"
        const val KEY_AGE = "profile.age"
        const val KEY_DARK = "settings.dark"
        const val KEY_LANGUAGE = "settings.language"
        const val KEY_NOTIFICATIONS = "settings.notifications"
        const val KEY_CHECKIN = "settings.checkin_minutes"
        const val KEY_GUIDE_DONE = "settings.guide_done"
        const val KEY_USAGE_STATS = "settings.usage_stats"
        const val KEY_AI_COACH = "settings.ai_coach"
    }
}

class DataMaintenanceRepositoryImpl(
    private val dao: MaintenanceDao,
) : DataMaintenanceRepository {
    override suspend fun clearAllData() = dao.clearAll()

    override suspend fun markAllDataDeleted(atEpochMillis: Long) = dao.markAllDeleted(atEpochMillis)

    override suspend fun deleteBadHabit(habitId: Long, atEpochMillis: Long) = dao.deleteBadHabit(habitId, atEpochMillis)

    override suspend fun deleteGoodHabit(habitId: Long, atEpochMillis: Long) = dao.deleteGoodHabit(habitId, atEpochMillis)
}
