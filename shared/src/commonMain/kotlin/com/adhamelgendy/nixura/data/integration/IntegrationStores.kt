package com.adhamelgendy.nixura.data.integration

import com.adhamelgendy.nixura.data.local.dao.EventDao
import com.adhamelgendy.nixura.data.local.dao.PreferenceDao
import com.adhamelgendy.nixura.data.local.dao.REMOVED_BY_IMPORT
import com.adhamelgendy.nixura.data.local.entity.EventEntity
import com.adhamelgendy.nixura.data.local.entity.PreferenceEntity
import com.adhamelgendy.nixura.domain.integration.ImportPlan
import com.adhamelgendy.nixura.domain.integration.ImportedEventStore
import com.adhamelgendy.nixura.domain.integration.ImportedRow
import com.adhamelgendy.nixura.domain.integration.IntegrationError
import com.adhamelgendy.nixura.domain.integration.IntegrationProvider
import com.adhamelgendy.nixura.domain.integration.IntegrationState
import com.adhamelgendy.nixura.domain.integration.IntegrationStateStore
import com.adhamelgendy.nixura.domain.model.EventCategory
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Imported events live in the normal events table (source + external id), so countdowns, widgets and sync just work. */
internal class RoomImportedEventStore(private val dao: EventDao) : ImportedEventStore {

    override suspend fun rows(provider: IntegrationProvider): List<ImportedRow> =
        dao.importedRows(provider.source.name).mapNotNull { e ->
            e.externalId?.let {
                val removedByImport = e.externalUpdatedAtEpochMillis == REMOVED_BY_IMPORT
                ImportedRow(e.id, it, e.externalUpdatedAtEpochMillis.takeUnless { removedByImport }, e.targetAtEpochMillis, e.deletedAtEpochMillis, removedByImport)
            }
        }

    override suspend fun apply(provider: IntegrationProvider, plan: ImportPlan, nowEpochMillis: Long) {
        val source = provider.source.name
        for (e in plan.upserts) {
            val title = ImportedText.clean(e.title)
            val existing = dao.importedRow(source, e.externalId)
            if (existing == null) {
                dao.insert(
                    EventEntity(
                        title = title,
                        category = (e.category ?: EventCategory.OTHER).name,
                        targetAtEpochMillis = e.startEpochMillis,
                        reminders = "",
                        createdAtEpochMillis = nowEpochMillis,
                        source = source,
                        externalId = e.externalId,
                        externalUpdatedAtEpochMillis = e.updatedAtEpochMillis,
                        allDay = e.allDay,
                    ),
                )
            } else {
                dao.updateImported(existing.id, title, e.startEpochMillis, e.allDay, e.updatedAtEpochMillis, e.category?.name, nowEpochMillis)
            }
        }
        plan.deleteIds.forEach { dao.markRemovedByImport(it, nowEpochMillis) }
    }

    /** Disconnect also forgets what the user deleted: a new link starts fresh. */
    override suspend fun removeAll(provider: IntegrationProvider, nowEpochMillis: Long) =
        dao.markAllFromSourceRemovedByImport(provider.source.name, nowEpochMillis)
}

/** Text from another service is untrusted: keep it to one short line of printable characters. */
internal object ImportedText {
    private const val MAX_TITLE = 120

    fun clean(raw: String): String {
        val printable = raw.filter { it == ' ' || !it.isISOControl() }.replace(Regex("\\s+"), " ").trim()
        return printable.take(MAX_TITLE).ifBlank { "Untitled" }
    }
}

@Serializable
private data class StoredState(
    val connected: Boolean = false,
    val accountLabel: String = "",
    val selection: List<String> = emptyList(),
    val mapping: Map<String, String> = emptyMap(),
    val cursor: String? = null,
    val lastSyncedAt: Long? = null,
    val lastError: String? = null,
)

/**
 * Link settings per provider in the local preferences table (never synced: a Google grant is per device). No tokens:
 * nothing secret is stored here.
 */
internal class PreferenceIntegrationStateStore(private val dao: PreferenceDao) : IntegrationStateStore {
    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun get(provider: IntegrationProvider): IntegrationState {
        val raw = dao.getAll().firstOrNull { it.key == key(provider) }?.value ?: return IntegrationState()
        val s = runCatching { json.decodeFromString(StoredState.serializer(), raw) }.getOrNull() ?: return IntegrationState()
        return IntegrationState(
            s.connected, s.accountLabel, s.selection, s.mapping, s.cursor, s.lastSyncedAt,
            IntegrationError.entries.firstOrNull { it.name == s.lastError },
        )
    }

    override suspend fun save(provider: IntegrationProvider, state: IntegrationState) {
        val s = StoredState(
            state.connected, state.accountLabel, state.selection, state.mapping, state.cursor, state.lastSyncedAt, state.lastError?.name,
        )
        dao.upsertAll(listOf(PreferenceEntity(key(provider), json.encodeToString(StoredState.serializer(), s))))
    }

    private fun key(provider: IntegrationProvider) = "integration.${provider.name.lowercase()}"
}
