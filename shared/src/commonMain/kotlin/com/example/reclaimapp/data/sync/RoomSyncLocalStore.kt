package com.example.reclaimapp.data.sync

import com.example.reclaimapp.data.local.dao.MaintenanceDao
import com.example.reclaimapp.data.local.dao.PreferenceDao
import com.example.reclaimapp.data.local.dao.SyncDao
import com.example.reclaimapp.data.local.entity.EventEntity
import com.example.reclaimapp.data.local.entity.GoodHabitEntity
import com.example.reclaimapp.data.local.entity.GoodHabitLogEntity
import com.example.reclaimapp.data.local.entity.HabitEntity
import com.example.reclaimapp.data.local.entity.HabitEventEntity
import com.example.reclaimapp.data.local.entity.PreferenceEntity
import com.example.reclaimapp.data.mapper.costOrNull
import com.example.reclaimapp.data.repository.PreferencesRepositoryImpl
import com.example.reclaimapp.domain.clock.Clock
import com.example.reclaimapp.domain.sync.SyncLocalStore
import com.example.reclaimapp.domain.sync.SyncRecord
import com.example.reclaimapp.domain.sync.SyncRules
import com.example.reclaimapp.domain.sync.SyncType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable

@Serializable
internal data class HabitPayload(
    val name: String,
    val createdAt: Long,
    val archivedAt: Long? = null,
    val colorArgb: Long? = null,
    /** Money-saved tracker; absent in records from older app versions (= no tracker). */
    val cost: HabitCostPayload? = null,
)

@Serializable
internal data class HabitCostPayload(
    val item: String,
    val unit: String,
    val unitsPerDay: Double,
    val priceMinor: Long,
    val unitsPerPack: Int,
    val currency: String,
    val perDayInPacks: Boolean = false,
)

private fun HabitEntity.costPayload(): HabitCostPayload? = costOrNull()?.let {
    HabitCostPayload(it.item, it.unit.name, it.unitsPerDay, it.pricePerPackMinor, it.unitsPerPack, it.currency, it.perDayInPacks)
}

private fun HabitEntity.withCostPayload(p: HabitCostPayload?): HabitEntity = copy(
    costItem = p?.item, costUnit = p?.unit, costUnitsPerDay = p?.unitsPerDay, costPriceMinor = p?.priceMinor,
    costUnitsPerPack = p?.unitsPerPack, costCurrency = p?.currency, costPerDayInPacks = p?.perDayInPacks,
)

@Serializable
internal data class HabitEventPayload(val habitId: String, val type: String, val occurredAt: Long)

@Serializable
internal data class GoodHabitPayload(
    val name: String,
    val dailyTarget: Int,
    val colorArgb: Long? = null,
    val createdAt: Long,
    val archivedAt: Long? = null,
)

@Serializable
internal data class GoodHabitLogPayload(val goodHabitId: String, val dayIndex: Long, val count: Int)

@Serializable
internal data class EventPayload(
    val title: String,
    val category: String,
    val targetAt: Long,
    val reminders: String,
    val createdAt: Long,
    /** Imported events: where from and their id there (absent from older app versions = a local event). */
    val source: String = "LOCAL",
    val externalId: String? = null,
    val externalUpdatedAt: Long? = null,
    val allDay: Boolean = false,
)

@Serializable
internal data class SettingsPayload(
    val darkTheme: Boolean,
    val languageCode: String,
    val notificationsEnabled: Boolean,
    val dailyCheckInMinutes: Int,
)

internal class RoomSyncLocalStore(
    private val dao: SyncDao,
    private val maintenance: MaintenanceDao,
    private val preferences: PreferenceDao,
    private val clock: Clock,
) : SyncLocalStore {

    override fun observePendingCount(): Flow<Int> =
        combine(dao.observeDirtyCount(), preferences.observeAll()) { rows, prefs ->
            rows + if (prefs.any { it.key == PreferencesRepositoryImpl.KEY_SETTINGS_DIRTY && it.value == "1" }) 1 else 0
        }

    override suspend fun pendingCount(): Int = observePendingCount().first()

    override suspend fun pendingRecords(limit: Int): List<SyncRecord> {
        val out = ArrayList<SyncRecord>()
        val room = { limit - out.size }
        dao.dirtyHabits(limit).forEach {
            out += record(SyncType.Habit, it.syncId, it.updatedAtEpochMillis, it.deletedAtEpochMillis, HabitPayload.serializer(), HabitPayload(it.name, it.createdAtEpochMillis, it.archivedAtEpochMillis, it.colorArgb, it.costPayload()))
        }
        if (room() > 0) dao.dirtyGoodHabits(room()).forEach {
            out += record(SyncType.GoodHabit, it.syncId, it.updatedAtEpochMillis, it.deletedAtEpochMillis, GoodHabitPayload.serializer(), GoodHabitPayload(it.name, it.dailyTarget, it.colorArgb, it.createdAtEpochMillis, it.archivedAtEpochMillis))
        }
        if (room() > 0) dao.dirtyHabitEvents(room()).forEach {
            val parent = dao.habitById(it.habitId)
            if (parent == null) {
                dao.cleanHabitEvent(it.syncId, it.updatedAtEpochMillis)
            } else {
                out += record(SyncType.HabitEvent, it.syncId, it.updatedAtEpochMillis, it.deletedAtEpochMillis, HabitEventPayload.serializer(), HabitEventPayload(parent.syncId, it.eventType, it.occurredAtEpochMillis))
            }
        }
        if (room() > 0) dao.dirtyGoodHabitLogs(room()).forEach {
            val parent = dao.goodHabitById(it.goodHabitId)
            if (parent == null) {
                dao.cleanGoodHabitLog(it.syncId, it.updatedAtEpochMillis) // an old row left behind by a removed habit
            } else {
                out += record(SyncType.GoodHabitLog, it.syncId, it.updatedAtEpochMillis, it.deletedAtEpochMillis, GoodHabitLogPayload.serializer(), GoodHabitLogPayload(parent.syncId, it.dayIndex, it.count))
            }
        }
        if (room() > 0) dao.dirtyEvents(room()).forEach {
            out += record(SyncType.Event, it.syncId, it.updatedAtEpochMillis, it.deletedAtEpochMillis, EventPayload.serializer(), EventPayload(it.title, it.category, it.targetAtEpochMillis, it.reminders, it.createdAtEpochMillis, it.source, it.externalId, it.externalUpdatedAtEpochMillis, it.allDay))
        }
        if (room() > 0) settingsRecord()?.let { out += it }
        return out
    }

    override suspend fun markUploaded(records: List<SyncRecord>) {
        dao.inTransaction {
            for (r in records) {
                when (r.type) {
                    SyncType.Habit -> dao.cleanHabit(r.id, r.updatedAt)
                    SyncType.GoodHabit -> dao.cleanGoodHabit(r.id, r.updatedAt)
                    SyncType.HabitEvent -> dao.cleanHabitEvent(r.id, r.updatedAt)
                    SyncType.GoodHabitLog -> dao.cleanGoodHabitLog(r.id, r.updatedAt)
                    SyncType.Event -> dao.cleanEvent(r.id, r.updatedAt)
                    SyncType.Settings -> if (prefs()[PreferencesRepositoryImpl.KEY_SETTINGS_UPDATED]?.toLongOrNull() == r.updatedAt) {
                        preferences.delete(PreferencesRepositoryImpl.KEY_SETTINGS_DIRTY)
                    }
                }
            }
        }
    }

    override suspend fun applyRemote(records: List<SyncRecord>, cursor: String, adoptGuestData: Boolean) {
        dao.inTransaction {
            val mergedHabits = ArrayList<Long>()
            // Parents first: a child may have arrived before an edit of its parent.
            for (r in records.sortedBy { it.type.ordinal }) {
                when (r.type) {
                    SyncType.Habit -> applyHabit(r, adoptGuestData, mergedHabits)
                    SyncType.GoodHabit -> applyGoodHabit(r, adoptGuestData)
                    SyncType.HabitEvent -> applyHabitEvent(r)
                    SyncType.GoodHabitLog -> applyGoodHabitLog(r)
                    SyncType.Event -> applyEvent(r, adoptGuestData)
                    SyncType.Settings -> applySettings(r)
                }
            }
            mergedHabits.forEach { dao.keepEarliestTrackingStart(it, clock.nowEpochMillis()) }
            if (cursor != cursor()) preferences.upsertAll(listOf(PreferenceEntity(KEY_CURSOR, cursor)))
        }
    }

    override suspend fun cursor(): String = prefs()[KEY_CURSOR] ?: "0"

    override suspend fun lastSyncedAt(): Long? = prefs()[KEY_LAST_SYNCED]?.toLongOrNull()

    override suspend fun setLastSyncedAt(atEpochMillis: Long) {
        preferences.upsertAll(listOf(PreferenceEntity(KEY_LAST_SYNCED, atEpochMillis.toString())))
    }

    override suspend fun clearAccountData() {
        maintenance.clearAll()
        forgetSyncPosition()
        preferences.delete(PreferencesRepositoryImpl.KEY_SETTINGS_DIRTY)
        preferences.delete(PreferencesRepositoryImpl.KEY_NAME)
        preferences.delete(PreferencesRepositoryImpl.KEY_EMAIL)
        preferences.delete(PreferencesRepositoryImpl.KEY_AGE)
    }

    override suspend fun detachFromAccount() {
        maintenance.markAllDirty()
        forgetSyncPosition()
        preferences.delete(PreferencesRepositoryImpl.KEY_EMAIL)
    }

    private suspend fun forgetSyncPosition() {
        preferences.delete(KEY_CURSOR)
        preferences.delete(KEY_LAST_SYNCED)
    }

    private suspend fun prefs(): Map<String, String> = preferences.getAll().associate { it.key to it.value }

    private fun <T> record(type: SyncType, id: String, updatedAt: Long, deletedAt: Long?, serializer: KSerializer<T>, payload: T) =
        SyncRecord(type, id, updatedAt, deletedAt != null, SyncJson.encodeToString(serializer, payload))

    /** Null for a payload this version cannot read; the item is skipped rather than failing the whole sync. */
    private fun <T> decode(serializer: KSerializer<T>, r: SyncRecord): T? =
        runCatching { SyncJson.decodeFromString(serializer, r.payload) }.getOrNull()

    private fun deletedAt(r: SyncRecord): Long? = if (r.deleted) r.updatedAt else null

    private suspend fun applyHabit(r: SyncRecord, adopt: Boolean, merged: MutableList<Long>) {
        val p = decode(HabitPayload.serializer(), r) ?: return
        var existing = dao.habitBySyncId(r.id)
        if (existing == null && adopt && !r.deleted && p.archivedAt == null) {
            existing = dao.guestHabitNamed(normalize(p.name))?.also {
                dao.rekeyHabit(it.id, r.id)
                merged += it.id
            }?.copy(syncId = r.id)
        }
        if (existing == null) {
            if (!r.deleted) dao.insertHabit(HabitEntity(name = p.name, createdAtEpochMillis = p.createdAt, archivedAtEpochMillis = p.archivedAt, colorArgb = p.colorArgb, syncId = r.id, updatedAtEpochMillis = r.updatedAt, dirty = false).withCostPayload(p.cost))
            return
        }
        if (!SyncRules.remoteWins(existing.dirty, existing.updatedAtEpochMillis, r.updatedAt)) return
        val updated = existing.copy(name = p.name, createdAtEpochMillis = p.createdAt, archivedAtEpochMillis = p.archivedAt, colorArgb = p.colorArgb, updatedAtEpochMillis = r.updatedAt, deletedAtEpochMillis = deletedAt(r), dirty = false).withCostPayload(p.cost)
        if (updated != existing) dao.updateHabit(updated)
    }

    private suspend fun applyGoodHabit(r: SyncRecord, adopt: Boolean) {
        val p = decode(GoodHabitPayload.serializer(), r) ?: return
        var existing = dao.goodHabitBySyncId(r.id)
        if (existing == null && adopt && !r.deleted && p.archivedAt == null) {
            existing = dao.guestGoodHabitNamed(normalize(p.name))?.also {
                dao.rekeyGoodHabit(it.id, r.id)
                dao.rekeyGoodHabitLogs(it.id, r.id)
            }?.copy(syncId = r.id)
        }
        if (existing == null) {
            if (!r.deleted) dao.insertGoodHabit(GoodHabitEntity(name = p.name, dailyTarget = p.dailyTarget, colorArgb = p.colorArgb, createdAtEpochMillis = p.createdAt, archivedAtEpochMillis = p.archivedAt, syncId = r.id, updatedAtEpochMillis = r.updatedAt, dirty = false))
            return
        }
        if (!SyncRules.remoteWins(existing.dirty, existing.updatedAtEpochMillis, r.updatedAt)) return
        val updated = existing.copy(name = p.name, dailyTarget = p.dailyTarget, colorArgb = p.colorArgb, createdAtEpochMillis = p.createdAt, archivedAtEpochMillis = p.archivedAt, updatedAtEpochMillis = r.updatedAt, deletedAtEpochMillis = deletedAt(r), dirty = false)
        if (updated != existing) dao.updateGoodHabit(updated)
    }

    private suspend fun applyHabitEvent(r: SyncRecord) {
        val p = decode(HabitEventPayload.serializer(), r) ?: return
        val existing = dao.habitEventBySyncId(r.id)
        if (existing == null) {
            val parent = dao.habitBySyncId(p.habitId) ?: return
            if (!r.deleted) dao.insertHabitEvent(HabitEventEntity(habitId = parent.id, eventType = p.type, occurredAtEpochMillis = p.occurredAt, syncId = r.id, updatedAtEpochMillis = r.updatedAt, dirty = false))
            return
        }
        if (!SyncRules.remoteWins(existing.dirty, existing.updatedAtEpochMillis, r.updatedAt)) return
        val updated = existing.copy(eventType = p.type, occurredAtEpochMillis = p.occurredAt, updatedAtEpochMillis = r.updatedAt, deletedAtEpochMillis = deletedAt(r), dirty = false)
        if (updated != existing) dao.updateHabitEvent(updated)
    }

    private suspend fun applyGoodHabitLog(r: SyncRecord) {
        val p = decode(GoodHabitLogPayload.serializer(), r) ?: return
        val existing = dao.goodHabitLogBySyncId(r.id)
        if (existing != null && !SyncRules.remoteWins(existing.dirty, existing.updatedAtEpochMillis, r.updatedAt)) return
        if (existing == null && r.deleted) return
        val parent = existing?.goodHabitId ?: dao.goodHabitBySyncId(p.goodHabitId)?.id ?: return
        val updated = GoodHabitLogEntity(
            id = existing?.id ?: 0,
            goodHabitId = parent,
            dayIndex = p.dayIndex,
            count = p.count,
            syncId = r.id,
            updatedAtEpochMillis = r.updatedAt,
            deletedAtEpochMillis = deletedAt(r),
            dirty = false,
        )
        if (updated != existing) dao.upsertGoodHabitLog(updated)
    }

    private suspend fun applyEvent(r: SyncRecord, adopt: Boolean) {
        val p = decode(EventPayload.serializer(), r) ?: return
        var existing = dao.eventBySyncId(r.id)
        if (existing == null && adopt && !r.deleted) {
            existing = dao.guestEventMatching(normalize(p.title), p.targetAt)?.also { dao.rekeyEvent(it.id, r.id) }?.copy(syncId = r.id)
        }
        // The same calendar event imported on this device too: one row, keyed by the account's id.
        if (existing == null && p.externalId != null) {
            existing = dao.eventByExternal(p.source, p.externalId)?.also { dao.rekeyEvent(it.id, r.id) }?.copy(syncId = r.id)
        }
        if (existing == null) {
            if (!r.deleted) dao.insertEvent(EventEntity(title = p.title, category = p.category, targetAtEpochMillis = p.targetAt, reminders = p.reminders, createdAtEpochMillis = p.createdAt, syncId = r.id, updatedAtEpochMillis = r.updatedAt, dirty = false, source = p.source, externalId = p.externalId, externalUpdatedAtEpochMillis = p.externalUpdatedAt, allDay = p.allDay))
            return
        }
        if (!SyncRules.remoteWins(existing.dirty, existing.updatedAtEpochMillis, r.updatedAt)) return
        val updated = existing.copy(title = p.title, category = p.category, targetAtEpochMillis = p.targetAt, reminders = p.reminders, createdAtEpochMillis = p.createdAt, updatedAtEpochMillis = r.updatedAt, deletedAtEpochMillis = deletedAt(r), dirty = false, source = p.source, externalId = p.externalId, externalUpdatedAtEpochMillis = p.externalUpdatedAt, allDay = p.allDay)
        if (updated != existing) dao.updateEvent(updated)
    }

    private suspend fun applySettings(r: SyncRecord) {
        val p = decode(SettingsPayload.serializer(), r) ?: return
        val now = prefs()
        val dirty = now[PreferencesRepositoryImpl.KEY_SETTINGS_DIRTY] == "1"
        val updatedAt = now[PreferencesRepositoryImpl.KEY_SETTINGS_UPDATED]?.toLongOrNull() ?: 0L
        if (!SyncRules.remoteWins(dirty, updatedAt, r.updatedAt)) return
        preferences.upsertAll(
            listOf(
                PreferenceEntity(PreferencesRepositoryImpl.KEY_DARK, if (p.darkTheme) "1" else "0"),
                PreferenceEntity(PreferencesRepositoryImpl.KEY_LANGUAGE, p.languageCode),
                PreferenceEntity(PreferencesRepositoryImpl.KEY_NOTIFICATIONS, if (p.notificationsEnabled) "1" else "0"),
                PreferenceEntity(PreferencesRepositoryImpl.KEY_CHECKIN, p.dailyCheckInMinutes.toString()),
                PreferenceEntity(PreferencesRepositoryImpl.KEY_SETTINGS_UPDATED, r.updatedAt.toString()),
            ),
        )
        preferences.delete(PreferencesRepositoryImpl.KEY_SETTINGS_DIRTY)
    }

    private suspend fun settingsRecord(): SyncRecord? {
        val now = prefs()
        if (now[PreferencesRepositoryImpl.KEY_SETTINGS_DIRTY] != "1") return null
        val payload = SettingsPayload(
            darkTheme = now[PreferencesRepositoryImpl.KEY_DARK] == "1",
            languageCode = now[PreferencesRepositoryImpl.KEY_LANGUAGE] ?: "en",
            notificationsEnabled = now[PreferencesRepositoryImpl.KEY_NOTIFICATIONS] == "1",
            dailyCheckInMinutes = now[PreferencesRepositoryImpl.KEY_CHECKIN]?.toIntOrNull() ?: 0,
        )
        return record(SyncType.Settings, SETTINGS_ID, now[PreferencesRepositoryImpl.KEY_SETTINGS_UPDATED]?.toLongOrNull() ?: 0L, null, SettingsPayload.serializer(), payload)
    }

    private fun normalize(text: String) = text.trim().lowercase()

    private companion object {
        const val SETTINGS_ID = "settings"
        const val KEY_CURSOR = "sync.cursor"
        const val KEY_LAST_SYNCED = "sync.last_synced_at"
    }
}
