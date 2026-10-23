package com.example.reclaimapp.domain.usecase

import com.example.reclaimapp.domain.model.Event
import com.example.reclaimapp.domain.model.EventCategory
import com.example.reclaimapp.domain.repository.EventRepository
import kotlinx.coroutines.flow.Flow

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
        val leads = reminderLeadMillis.filter { it >= 0 }
        if (id == null) {
            repository.create(clean, category, targetAtEpochMillis, leads)
        } else {
            repository.update(id, clean, category, targetAtEpochMillis, leads)
            id
        }
    }
}

class DeleteEventUseCase(private val repository: EventRepository) {
    suspend operator fun invoke(id: Long): Result<Unit> = runCatching { repository.delete(id) }
}
