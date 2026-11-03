package com.example.reclaimapp.domain.usecase

import com.example.reclaimapp.domain.clock.Clock
import com.example.reclaimapp.domain.repository.DataMaintenanceRepository
import com.example.reclaimapp.domain.repository.EventRepository

/** Something the user can delete from a card. */
sealed interface DeletableItem {
    val id: Long

    data class BadHabit(override val id: Long) : DeletableItem
    data class GoodHabit(override val id: Long) : DeletableItem
    data class Event(override val id: Long) : DeletableItem
}

/**
 * Deletes one item with everything that belongs to it: a habit's history (start, relapses, logged days) or an event.
 * Rows are marked deleted, so the next sync removes them from the account and other devices too. Reminders and
 * widgets need no extra step: both follow the data (NotificationSyncService re-plans, widgets show "removed").
 */
class DeleteItemUseCase(
    private val maintenance: DataMaintenanceRepository,
    private val events: EventRepository,
    private val clock: Clock,
) {
    suspend operator fun invoke(item: DeletableItem): Result<Unit> = runCatching {
        require(item.id > 0) { "Invalid id" }
        val now = clock.nowEpochMillis()
        when (item) {
            is DeletableItem.BadHabit -> maintenance.deleteBadHabit(item.id, now)
            is DeletableItem.GoodHabit -> maintenance.deleteGoodHabit(item.id, now)
            is DeletableItem.Event -> events.delete(item.id)
        }
    }
}
