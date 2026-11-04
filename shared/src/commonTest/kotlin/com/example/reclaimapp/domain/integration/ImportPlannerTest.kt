package com.example.reclaimapp.domain.integration

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ImportPlannerTest {
    private val day = 86_400_000L
    private val now = 1_000 * day
    private val from = now - day
    private val to = now + 366 * day

    private fun ev(id: String, start: Long = now + day, updated: Long = 10, cancelled: Boolean = false) =
        ExternalEvent(id, "Trip $id", start, allDay = false, updatedAtEpochMillis = updated, cancelled = cancelled)

    private fun row(id: Long, ext: String, updated: Long = 10, start: Long = now + day, deletedAt: Long? = null) =
        ImportedRow(id, ext, updated, start, deletedAt)

    private fun plan(existing: List<ImportedRow>, incoming: List<ExternalEvent>, full: Boolean = false) =
        ImportPlanner.plan(existing, incoming, full, from, to)

    @Test
    fun newEventsAreImportedOnce() {
        val p = plan(emptyList(), listOf(ev("a"), ev("a", updated = 12), ev("b")))
        assertEquals(listOf("a", "b"), p.upserts.map { it.externalId }) // duplicate in one page: one row, newest wins
        assertEquals(12, p.upserts.first().updatedAtEpochMillis)
    }

    @Test
    fun theSameEventFetchedAgainChangesNothing() {
        assertTrue(plan(listOf(row(1, "a")), listOf(ev("a"))).isEmpty)
    }

    @Test
    fun anEditAtTheSourceUpdatesTheRow() {
        val p = plan(listOf(row(1, "a", updated = 10)), listOf(ev("a", updated = 20)))
        assertEquals(listOf("a"), p.upserts.map { it.externalId })
    }

    @Test
    fun cancelledAtTheSourceIsDeleted() {
        assertEquals(listOf(1L), plan(listOf(row(1, "a")), listOf(ev("a", cancelled = true))).deleteIds)
    }

    @Test
    fun aFullFetchRemovesWhatTheSourceNoLongerHas() {
        val existing = listOf(row(1, "a"), row(2, "gone"), row(3, "old", start = now - 30 * day))
        val p = plan(existing, listOf(ev("a")), full = true)
        assertEquals(listOf(2L), p.deleteIds) // the long-past one is outside the window and is left alone
        assertTrue(plan(existing, listOf(ev("a")), full = false).deleteIds.isEmpty()) // incremental: absence means nothing
    }

    @Test
    fun aUserDeleteIsRemembered() {
        val deleted = listOf(row(1, "a", deletedAt = 50))
        assertTrue(plan(deleted, listOf(ev("a", updated = 40))).isEmpty)
        assertEquals(listOf("a"), plan(deleted, listOf(ev("a", updated = 60))).upserts.map { it.externalId })
    }

    @Test
    fun aRowNixUraRemovedComesBackWhenListedAgain() {
        // Entry turned off (or Disconnect), then on again: the same unchanged event must be imported again.
        val removed = row(1, "a", updated = 10, deletedAt = now - 5).copy(externalUpdatedAt = null, removedByImport = true)
        assertEquals(listOf("a"), plan(listOf(removed), listOf(ev("a", updated = 10))).upserts.map { it.externalId })
        // A delete by the user is still remembered.
        val userDeleted = row(2, "b", updated = 10, deletedAt = now - 5)
        assertTrue(plan(listOf(userDeleted), listOf(ev("b", updated = 10))).upserts.isEmpty())
    }

    @Test
    fun eventsOutsideTheWindowAreNotImported() {
        val p = plan(emptyList(), listOf(ev("past", start = now - 5 * day), ev("far", start = now + 400 * day)))
        assertTrue(p.isEmpty)
    }
}
