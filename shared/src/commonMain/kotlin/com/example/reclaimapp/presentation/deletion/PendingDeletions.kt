package com.example.reclaimapp.presentation.deletion

import com.example.reclaimapp.domain.usecase.DeletableItem
import com.example.reclaimapp.domain.usecase.DeleteItemUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** A delete the user can still undo. */
data class PendingDeletion(val item: DeletableItem, val name: String, val undoable: Boolean = true)

/**
 * Delete with undo. The item disappears from every list at once, but is only deleted for real once the undo window
 * has passed, so Undo never has to resurrect synced rows, reminders or widgets. If the app is killed inside the
 * window the delete simply does not happen (the safe side). App-wide singleton: it outlives the screen it came from.
 */
class PendingDeletions(
    private val delete: DeleteItemUseCase,
    private val scope: CoroutineScope,
    private val undoWindowMillis: Long = UNDO_WINDOW_MILLIS,
) {
    private val _pending = MutableStateFlow<List<PendingDeletion>>(emptyList())

    /** Deletes waiting for their undo window, oldest first. The newest one is offered in the snackbar. */
    val pending: StateFlow<List<PendingDeletion>> = _pending.asStateFlow()

    private val jobs = HashMap<DeletableItem, Job>()

    fun schedule(item: DeletableItem, name: String) {
        if (jobs.containsKey(item)) return
        _pending.update { it + PendingDeletion(item, name) }
        jobs[item] = scope.launch {
            delay(undoWindowMillis)
            commit(item)
        }
    }

    fun undo(item: DeletableItem) {
        jobs.remove(item)?.cancel()
        _pending.update { list -> list.filterNot { it.item == item } }
    }

    /** Ends the undo window now (the snackbar was dismissed). */
    fun confirm(item: DeletableItem) {
        val job = jobs.remove(item) ?: return
        job.cancel()
        scope.launch { commit(item) }
    }

    private suspend fun commit(item: DeletableItem) {
        jobs.remove(item)
        _pending.update { list -> list.map { if (it.item == item) it.copy(undoable = false) else it } }
        val ok = delete(item).isSuccess
        // Stay hidden until the lists have re-read the database, so the card does not flash back for a frame.
        if (ok) delay(SETTLE_MILLIS)
        _pending.update { list -> list.filterNot { it.item == item } }
    }

    companion object {
        const val UNDO_WINDOW_MILLIS = 5_000L
        private const val SETTLE_MILLIS = 1_500L
    }
}

/** True when [item] is waiting to be deleted, so lists hide it right away. */
fun List<PendingDeletion>.hides(item: DeletableItem): Boolean = any { it.item == item }
