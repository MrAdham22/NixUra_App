package com.adhamelgendy.nixura.domain.coach

import com.adhamelgendy.nixura.domain.model.CoachMessage
import com.adhamelgendy.nixura.domain.model.CoachRole

/**
 * The quit habit a chat is about, read from the chat itself: the newest message the user wrote that names one of
 * their quit habits. So reopening a chat (or restarting the app) keeps "Today's focus" on what the chat discusses,
 * without storing anything new. Null when no message names a habit.
 */
object ChatFocus {
    fun inferred(messages: List<CoachMessage>, habits: List<BadHabitSnapshot>): Long? {
        if (habits.isEmpty()) return null
        // Longer names first, so "junk food" wins over a habit called "food".
        val byLength = habits.filter { it.name.isNotBlank() }.sortedByDescending { it.name.length }
        for (message in messages.sortedByDescending { it.createdAtEpochMillis }) {
            if (message.role != CoachRole.USER) continue
            val text = message.text.lowercase()
            byLength.firstOrNull { text.contains(it.name.trim().lowercase()) }?.let { return it.id }
        }
        return null
    }
}
