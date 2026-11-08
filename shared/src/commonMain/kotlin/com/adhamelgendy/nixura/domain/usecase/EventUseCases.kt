package com.adhamelgendy.nixura.domain.usecase

import com.adhamelgendy.nixura.domain.clock.Clock
import com.adhamelgendy.nixura.domain.model.Event
import com.adhamelgendy.nixura.domain.model.EventCategory
import com.adhamelgendy.nixura.domain.notification.ReminderError
import com.adhamelgendy.nixura.domain.notification.ReminderRules
import com.adhamelgendy.nixura.domain.notification.ReminderValidationException
import com.adhamelgendy.nixura.domain.repository.EventRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

const val MAX_EVENT_TITLE = 60

class ObserveEventsUseCase(private val repository: EventRepository) {
    operator fun invoke(): Flow<List<Event>> = repository.observeEvents()
}

/** Creates a new event when [id] is null, otherwise updates it. */
class SaveEventUseCase(private val repository: EventRepository) {
    suspend operator fun invoke(
        id: Long?,
        title: String,
        category: EventCategory,
        targetAtEpochMillis: Long,
        reminderLeadMillis: List<Long>,
    ): Result<Long> = runCatching {
        val clean = title.trim()
        require(clean.isNotEmpty()) { "Event name is required" }
        require(clean.length <= MAX_EVENT_TITLE) { "Event name must be at most $MAX_EVENT_TITLE characters" }
        require(targetAtEpochMillis > 0) { "Choose a date and time" }
        val leads = ReminderRules.normalize(reminderLeadMillis)
        if (id == null) {
            repository.create(clean, category, targetAtEpochMillis, leads)
        } else {
            repository.update(id, clean, category, targetAtEpochMillis, leads)
            id
        }
    }
}

/**
 * Edits one reminder of an existing event and saves immediately, so the notification sync service
 * (which replaces the whole OS schedule on every change) cancels the old alarm and arms the new one.
 * Every change is validated by [ReminderRules]; read-modify-write steps are serialized.
 */
class EventReminderEditor(
    private val repository: EventRepository,
    private val clock: Clock,
) {
    private val lock = Mutex()

    suspend fun add(eventId: Long, leadMillis: Long): Result<Unit> = change(eventId) { event ->
        check(ReminderRules.validate(leadMillis, event.reminderLeadMillis, event.targetAtEpochMillis, clock.nowEpochMillis()))
        event.reminderLeadMillis + leadMillis
    }

    /** Puts a removed reminder back (Undo). Skips the "must be in the future" rule so a passed one can return too. */
    suspend fun restore(eventId: Long, leadMillis: Long): Result<Unit> = change(eventId) { event ->
        val error = when {
            leadMillis < 0L -> ReminderError.NEGATIVE
            leadMillis > ReminderRules.MAX_LEAD_MILLIS -> ReminderError.TOO_FAR_AHEAD
            leadMillis in event.reminderLeadMillis -> ReminderError.DUPLICATE
            event.reminderLeadMillis.size >= ReminderRules.MAX_REMINDERS_PER_EVENT -> ReminderError.TOO_MANY
            else -> null
        }
        check(error)
        event.reminderLeadMillis + leadMillis
    }

    suspend fun edit(eventId: Long, oldLeadMillis: Long, newLeadMillis: Long): Result<Unit> = change(eventId) { event ->
        val others = event.reminderLeadMillis - oldLeadMillis
        check(ReminderRules.validate(newLeadMillis, others, event.targetAtEpochMillis, clock.nowEpochMillis()))
        others + newLeadMillis
    }

    suspend fun remove(eventId: Long, leadMillis: Long): Result<Unit> = change(eventId) { event ->
        event.reminderLeadMillis - leadMillis
    }

    private fun check(error: ReminderError?) {
        if (error != null) throw ReminderValidationException(error)
    }

    private suspend fun change(eventId: Long, transform: (Event) -> List<Long>): Result<Unit> = lock.withLock {
        try {
            val event = repository.observeEvents().first().firstOrNull { it.id == eventId }
                ?: throw NoSuchElementException("Event $eventId no longer exists")
            val leads = ReminderRules.normalize(transform(event))
            repository.update(event.id, event.title, event.category, event.targetAtEpochMillis, leads)
            Result.success(Unit)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            Result.failure(error)
        }
    }
}
