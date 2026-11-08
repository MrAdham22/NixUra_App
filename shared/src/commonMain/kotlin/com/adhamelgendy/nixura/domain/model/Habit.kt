package com.adhamelgendy.nixura.domain.model

import com.adhamelgendy.nixura.domain.money.HabitCost

data class Habit(
    val id: Long,
    val name: String,
    val createdAtEpochMillis: Long,
    val archivedAtEpochMillis: Long? = null,
    /** User-chosen card base color (ARGB). `null` means the default NixUra brand progression. */
    val colorArgb: Long? = null,
    /** What the habit used to cost, for the money-saved tracker; `null` = not tracked. */
    val cost: HabitCost? = null,
) {
    val isArchived: Boolean get() = archivedAtEpochMillis != null
}
