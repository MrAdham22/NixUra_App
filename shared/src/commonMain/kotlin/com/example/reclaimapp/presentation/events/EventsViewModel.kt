package com.example.reclaimapp.presentation.events

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.reclaimapp.domain.model.Event
import com.example.reclaimapp.domain.model.EventCategory
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
)

class EventsViewModel(
    observeEvents: ObserveEventsUseCase,
    private val saveEvent: SaveEventUseCase,
    private val deleteEvent: DeleteEventUseCase,
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
        transient.update { it.copy(errorMessage = null) }
    }
}
