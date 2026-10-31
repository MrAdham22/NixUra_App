package com.example.reclaimapp.domain.widget

import com.example.reclaimapp.domain.model.Event

/** Picks the event the "nearest event" widget should show. */
object NearestEventSelector {
    /**
     * The soonest event strictly after [nowEpochMillis]; if none is upcoming, the most recent past event;
     * `null` when there are no events. An event at exactly `now` counts as just passed (matches the widget state).
     */
    fun select(events: List<Event>, nowEpochMillis: Long): Event? {
        val upcoming = events.filter { it.targetAtEpochMillis > nowEpochMillis }
            .minWithOrNull(compareBy<Event> { it.targetAtEpochMillis }.thenBy { it.id })
        if (upcoming != null) return upcoming
        return events.filter { it.targetAtEpochMillis <= nowEpochMillis }
            .maxWithOrNull(compareBy<Event> { it.targetAtEpochMillis }.thenBy { it.id })
    }
}
