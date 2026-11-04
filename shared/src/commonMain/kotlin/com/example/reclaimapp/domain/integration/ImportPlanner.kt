package com.example.reclaimapp.domain.integration

/** What one import changes: rows to insert or update, and existing rows to delete. */
data class ImportPlan(val upserts: List<ExternalEvent>, val deleteIds: List<Long>) {
    val isEmpty: Boolean get() = upserts.isEmpty() && deleteIds.isEmpty()
}

/**
 * Decides how fetched events change the imported rows. Pure, so every rule is unit-tested:
 * - one row per external id: the same event fetched twice is updated, never duplicated;
 * - an edit at the source (newer update time) updates the row; an unchanged event writes nothing;
 * - cancelled or deleted at the source deletes the row;
 * - on a full fetch, imported events in the window that the source no longer has are deleted;
 * - an event the user deleted in NixUra stays deleted unless it was edited at the source afterwards;
 * - an event NixUra removed itself (entry turned off, Disconnect, missing for a while) comes back when it is listed again;
 * - events outside the window (long past, far future) are not imported.
 */
object ImportPlanner {

    fun plan(
        existing: List<ImportedRow>,
        incoming: List<ExternalEvent>,
        fullSync: Boolean,
        windowStartEpochMillis: Long,
        windowEndEpochMillis: Long,
    ): ImportPlan {
        val byId = existing.associateBy { it.externalId }
        // The newest version wins when a page lists the same event twice.
        val latest = incoming.groupBy { it.externalId }.mapValues { (_, v) -> v.maxBy { it.updatedAtEpochMillis } }
        val upserts = ArrayList<ExternalEvent>()
        val deletes = LinkedHashSet<Long>()

        for (e in latest.values) {
            val row = byId[e.externalId]
            val inWindow = e.startEpochMillis in windowStartEpochMillis..windowEndEpochMillis
            if (e.cancelled || !inWindow) {
                if (row != null && row.deletedAt == null) deletes += row.id
                continue
            }
            when {
                row == null -> upserts += e
                row.deletedAt != null -> if (row.removedByImport || e.updatedAtEpochMillis > row.deletedAt) upserts += e // or edited after the user deleted it
                (row.externalUpdatedAt ?: Long.MIN_VALUE) < e.updatedAtEpochMillis -> upserts += e
            }
        }
        if (fullSync) {
            for (row in existing) {
                val gone = row.externalId !in latest && row.deletedAt == null
                if (gone && row.startEpochMillis in windowStartEpochMillis..windowEndEpochMillis) deletes += row.id
            }
        }
        return ImportPlan(upserts, deletes.toList())
    }
}
