package com.example.reclaimapp.domain.model

/** A habit the user wants to build, e.g. "Drink water" with a daily target of 8. */
data class GoodHabit(
    val id: Long,
    val name: String,
    /** How many times per day counts as "done". */
    val dailyTarget: Int,
    val colorArgb: Long? = null,
    val createdAtEpochMillis: Long,
    val archivedAtEpochMillis: Long? = null,
)

/** Number of times a good habit was done on one local day. [dayIndex] = local days since 1970-01-01. */
data class GoodHabitLog(
    val goodHabitId: Long,
    val dayIndex: Long,
    val count: Int,
)

data class GoodHabitWithLogs(
    val habit: GoodHabit,
    val logs: List<GoodHabitLog>,
)
