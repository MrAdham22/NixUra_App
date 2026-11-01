package com.example.reclaimapp.domain.usecase

import com.example.reclaimapp.domain.clock.Clock
import com.example.reclaimapp.domain.model.Event
import com.example.reclaimapp.domain.model.EventCategory
import com.example.reclaimapp.domain.notification.ReminderError
import com.example.reclaimapp.domain.notification.ReminderValidationException
import com.example.reclaimapp.domain.repository.EventRepository
import com.example.reclaimapp.domain.widget.runSuspend
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class EventReminderEditorTest {
    private val hour = 3_600_000L
    private val day = 24 * hour
    private val now = 10_000_000_000L

    private class Repo(initial: List<Event>) : EventRepository {
        val state = MutableStateFlow(initial)
        var updates = 0
        override fun observeEvents(): Flow<List<Event>> = state
        override suspend fun create(title: String, category: EventCategory, targetAtEpochMillis: Long, reminderLeadMillis: List<Long>): Long = error("unused")
        override suspend fun update(id: Long, title: String, category: EventCategory, targetAtEpochMillis: Long, reminderLeadMillis: List<Long>) {
            updates++
            state.value = state.value.map { if (it.id == id) it.copy(title = title, category = category, targetAtEpochMillis = targetAtEpochMillis, reminderLeadMillis = reminderLeadMillis) else it }
        }
        override suspend fun delete(id: Long) = error("unused")
        fun leads(id: Long) = state.value.first { it.id == id }.reminderLeadMillis
    }

    private fun editor(vararg leads: Long, target: Long = now + 3 * day): Pair<EventReminderEditor, Repo> {
        val repo = Repo(listOf(Event(1, "Trip", EventCategory.TRAVEL, target, leads.toList(), 0L)))
        return EventReminderEditor(repo, object : Clock { override fun nowEpochMillis() = now }) to repo
    }

    @Test
    fun addStoresSortedReminder() = runSuspend {
        val (editor, repo) = editor(day)
        assertTrue(editor.add(1, hour).isSuccess)
        assertEquals(listOf(hour, day), repo.leads(1))
    }

    @Test
    fun addRejectsDuplicateAndPastWithoutWriting() = runSuspend {
        val (editor, repo) = editor(hour)
        val dup = editor.add(1, hour).exceptionOrNull()
        assertIs<ReminderValidationException>(dup)
        assertEquals(ReminderError.DUPLICATE, dup.error)
        val past = editor.add(1, 5 * day).exceptionOrNull()
        assertIs<ReminderValidationException>(past)
        assertEquals(ReminderError.IN_PAST, past.error)
        assertEquals(0, repo.updates)
        assertEquals(listOf(hour), repo.leads(1))
    }

    @Test
    fun editReplacesTheOldLeadAndNeverLeavesBoth() = runSuspend {
        val (editor, repo) = editor(hour, day)
        assertTrue(editor.edit(1, hour, 2 * hour).isSuccess)
        assertEquals(listOf(2 * hour, day), repo.leads(1))
    }

    @Test
    fun editToItsOwnValueIsNotADuplicate() = runSuspend {
        val (editor, repo) = editor(hour, day)
        assertTrue(editor.edit(1, hour, hour).isSuccess)
        assertEquals(listOf(hour, day), repo.leads(1))
    }

    @Test
    fun editRejectsCollisionWithAnotherReminder() = runSuspend {
        val (editor, repo) = editor(hour, day)
        val error = editor.edit(1, hour, day).exceptionOrNull()
        assertIs<ReminderValidationException>(error)
        assertEquals(ReminderError.DUPLICATE, error.error)
        assertEquals(listOf(hour, day), repo.leads(1))
    }

    @Test
    fun removeThenRestoreBringsTheReminderBack() = runSuspend {
        val (editor, repo) = editor(hour, day)
        assertTrue(editor.remove(1, hour).isSuccess)
        assertEquals(listOf(day), repo.leads(1))
        assertTrue(editor.restore(1, hour).isSuccess)
        assertEquals(listOf(hour, day), repo.leads(1))
    }

    @Test
    fun restoreWorksEvenIfItsTriggerHasPassedMeanwhile() = runSuspend {
        // Event in 1 hour with a 2-hour reminder that already passed: Undo must still be able to bring it back.
        val (editor, repo) = editor(2 * hour, target = now + hour)
        assertTrue(editor.remove(1, 2 * hour).isSuccess)
        assertTrue(editor.restore(1, 2 * hour).isSuccess)
        assertEquals(listOf(2 * hour), repo.leads(1))
    }

    @Test
    fun missingEventFailsInsteadOfThrowing() = runSuspend {
        val (editor, _) = editor(hour)
        assertTrue(editor.remove(99, hour).isFailure)
    }
}
