package com.adhamelgendy.nixura.presentation.deletion

import com.adhamelgendy.nixura.domain.clock.Clock
import com.adhamelgendy.nixura.domain.repository.DataMaintenanceRepository
import com.adhamelgendy.nixura.domain.repository.EventRepository
import com.adhamelgendy.nixura.domain.model.Event
import com.adhamelgendy.nixura.domain.model.EventCategory
import com.adhamelgendy.nixura.domain.usecase.DeletableItem
import com.adhamelgendy.nixura.domain.usecase.DeleteItemUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class PendingDeletionsTest {

    private class Recorder : DataMaintenanceRepository, EventRepository {
        val deleted = mutableListOf<String>()
        override suspend fun clearAllData() = Unit
        override suspend fun markAllDataDeleted(atEpochMillis: Long) = Unit
        override suspend fun deleteBadHabit(habitId: Long, atEpochMillis: Long) { deleted += "bad:$habitId" }
        override suspend fun deleteGoodHabit(habitId: Long, atEpochMillis: Long) { deleted += "good:$habitId" }
        override fun observeEvents(): Flow<List<Event>> = emptyFlow()
        override suspend fun create(title: String, category: EventCategory, targetAtEpochMillis: Long, reminderLeadMillis: List<Long>) = 0L
        override suspend fun update(id: Long, title: String, category: EventCategory, targetAtEpochMillis: Long, reminderLeadMillis: List<Long>) = Unit
        override suspend fun delete(id: Long) { deleted += "event:$id" }
    }

    private val clock = object : Clock { override fun nowEpochMillis() = 1_000L }

    private fun setup(scope: TestScope): Pair<Recorder, PendingDeletions> {
        val repo = Recorder()
        return repo to PendingDeletions(DeleteItemUseCase(repo, repo, clock), scope, undoWindowMillis = 5_000)
    }

    @Test
    fun deletesOnlyAfterTheUndoWindow() {
        val scope = TestScope()
        val (repo, pending) = setup(scope)
        pending.schedule(DeletableItem.BadHabit(3), "Smoking")
        assertTrue(pending.pending.value.hides(DeletableItem.BadHabit(3)))
        scope.advanceTimeBy(4_999)
        scope.runCurrent()
        assertEquals(emptyList(), repo.deleted)
        scope.advanceUntilIdle()
        assertEquals(listOf("bad:3"), repo.deleted)
        assertEquals(emptyList(), pending.pending.value)
    }

    @Test
    fun undoKeepsTheItem() {
        val scope = TestScope()
        val (repo, pending) = setup(scope)
        pending.schedule(DeletableItem.GoodHabit(7), "Drink water")
        scope.advanceTimeBy(2_000)
        pending.undo(DeletableItem.GoodHabit(7))
        scope.advanceUntilIdle()
        assertEquals(emptyList(), repo.deleted)
        assertEquals(emptyList(), pending.pending.value)
    }

    @Test
    fun eachKindDeletesItsOwnData() {
        val scope = TestScope()
        val (repo, pending) = setup(scope)
        pending.schedule(DeletableItem.Event(1), "Trip")
        pending.schedule(DeletableItem.GoodHabit(2), "Read")
        pending.schedule(DeletableItem.Event(1), "Trip") // a second long-press on the same card is ignored
        scope.advanceUntilIdle()
        assertEquals(listOf("event:1", "good:2"), repo.deleted)
    }
}
