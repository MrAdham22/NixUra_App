package com.example.reclaimapp.presentation.events

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.reclaimapp.domain.model.Event
import com.example.reclaimapp.domain.clock.Clock
import com.example.reclaimapp.domain.model.EventCategory
import com.example.reclaimapp.domain.notification.ReminderError
import com.example.reclaimapp.domain.notification.ReminderRules
import com.example.reclaimapp.domain.notification.ReminderValidationException
import com.example.reclaimapp.domain.usecase.EventReminderEditor
import com.example.reclaimapp.domain.usecase.DeleteEventUseCase
import com.example.reclaimapp.domain.usecase.ObserveEventsUseCase
import com.example.reclaimapp.domain.usecase.SaveEventUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class EventsUiState(
    /** Sorted by target time ascending. */
    val events: List<Event> = emptyList(),
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    /** Last reminder edit that broke a rule; the UI shows a localized message. */
    val reminderError: ReminderError? = null,
    /** True when a reminder change could not be saved for another reason (event gone, database error). */
    val reminderSaveFailed: Boolean = false,
)

class EventsViewModel(
    observeEvents: ObserveEventsUseCase,
    private val saveEvent: SaveEventUseCase,
    private val deleteEvent: DeleteEventUseCase,
    private val reminderEditor: EventReminderEditor,
    private val clock: Clock,
) : ViewModel() {

    private val transient = MutableStateFlow(EventsUiState())

    val uiState: StateFlow<EventsUiState> = combine(observeEvents(), transient) { events, extra ->
        extra.copy(events = events.sortedBy { it.targetAtEpochMillis }, isLoading = false)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = EventsUiState(),
    )

    fun onSave(
        id: Long?,
        title: String,
        category: EventCategory,
        targetAtEpochMillis: Long,
        reminderLeadMillis: List<Long>,
        onSuccess: () -> Unit,
    ) {
        viewModelScope.launch {
            transient.update { it.copy(isSaving = true, errorMessage = null) }
            val result = saveEvent(id, title, category, targetAtEpochMillis, reminderLeadMillis)
            transient.update { it.copy(isSaving = false, errorMessage = result.exceptionOrNull()?.message) }
            if (result.isSuccess) onSuccess()
        }
    }

    fun onDelete(id: Long, onSuccess: () -> Unit) {
        viewModelScope.launch {
            deleteEvent(id).fold(
                onSuccess = { onSuccess() },
                onFailure = { error -> transient.update { it.copy(errorMessage = error.message ?: "Could not delete event") } },
            )
        }
    }

    fun onDismissError() {
        transient.update { it.copy(errorMessage = null, reminderError = null, reminderSaveFailed = false) }
    }

    /**
     * Live validation for the reminder dialog and quick-add chips. [otherLeads] are the event's other reminders
     * (leave the one being edited out); [targetAtEpochMillis] is the event time the user currently sees.
     */
    fun validateReminder(leadMillis: Long, otherLeads: Collection<Long>, targetAtEpochMillis: Long): ReminderError? =
        ReminderRules.validate(leadMillis, otherLeads, targetAtEpochMillis, clock.nowEpochMillis())

    /** Adds a reminder to a saved event and persists at once (the alarm schedule follows from the database). */
    fun onAddReminder(eventId: Long, leadMillis: Long) = runReminderChange { reminderEditor.add(eventId, leadMillis) }

    /** Replaces [oldLeadMillis] by [newLeadMillis] on a saved event. */
    fun onEditReminder(eventId: Long, oldLeadMillis: Long, newLeadMillis: Long) =
        runReminderChange { reminderEditor.edit(eventId, oldLeadMillis, newLeadMillis) }

    /** Removes a reminder from a saved event. Pair with [onRestoreReminder] for Undo. */
    fun onRemoveReminder(eventId: Long, leadMillis: Long) = runReminderChange { reminderEditor.remove(eventId, leadMillis) }

    /** Undo of a removal; allowed even when the restored reminder's time has passed meanwhile. */
    fun onRestoreReminder(eventId: Long, leadMillis: Long) = runReminderChange { reminderEditor.restore(eventId, leadMillis) }

    private fun runReminderChange(change: suspend () -> Result<Unit>) {
        viewModelScope.launch {
            transient.update { it.copy(reminderError = null, reminderSaveFailed = false) }
            val failure = change().exceptionOrNull() ?: return@launch
            transient.update {
                if (failure is ReminderValidationException) it.copy(reminderError = failure.error)
                else it.copy(reminderSaveFailed = true)
            }
        }
    }
}
