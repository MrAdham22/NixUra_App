package com.example.reclaimapp.data.mapper

import com.example.reclaimapp.data.local.entity.CoachMessageEntity
import com.example.reclaimapp.data.local.entity.EventEntity
import com.example.reclaimapp.data.local.entity.GoodHabitEntity
import com.example.reclaimapp.data.local.entity.GoodHabitLogEntity
import com.example.reclaimapp.data.local.entity.HabitEntity
import com.example.reclaimapp.data.local.entity.HabitEventEntity
import com.example.reclaimapp.domain.model.CoachMessage
import com.example.reclaimapp.domain.model.CoachRole
import com.example.reclaimapp.domain.model.Event
import com.example.reclaimapp.domain.model.EventCategory
import com.example.reclaimapp.domain.model.EventSource
import com.example.reclaimapp.domain.model.GoodHabit
import com.example.reclaimapp.domain.model.GoodHabitLog
import com.example.reclaimapp.domain.model.Habit
import com.example.reclaimapp.domain.model.HabitEvent
import com.example.reclaimapp.domain.model.HabitEventType
import com.example.reclaimapp.domain.money.CostUnit
import com.example.reclaimapp.domain.money.HabitCost

fun HabitEntity.toDomain(): Habit = Habit(
    id = id,
    name = name,
    createdAtEpochMillis = createdAtEpochMillis,
    archivedAtEpochMillis = archivedAtEpochMillis,
    colorArgb = colorArgb,
    cost = costOrNull(),
)

/** A half-written or unreadable tracker (unknown unit, zero amounts) counts as no tracker, never as a crash. */
fun HabitEntity.costOrNull(): HabitCost? {
    val unit = CostUnit.entries.firstOrNull { it.name == costUnit } ?: return null
    return runCatching {
        HabitCost(
            item = costItem.orEmpty(),
            unit = unit,
            unitsPerDay = costUnitsPerDay ?: return null,
            pricePerPackMinor = costPriceMinor ?: return null,
            unitsPerPack = costUnitsPerPack ?: 1,
            currency = costCurrency ?: return null,
            perDayInPacks = costPerDayInPacks ?: false,
        )
    }.getOrNull()
}

/** Writes [cost] into the entity's tracker columns (`null` clears them). */
fun HabitEntity.withCost(cost: HabitCost?): HabitEntity = copy(
    costItem = cost?.item,
    costUnit = cost?.unit?.name,
    costUnitsPerDay = cost?.unitsPerDay,
    costPriceMinor = cost?.pricePerPackMinor,
    costUnitsPerPack = cost?.unitsPerPack,
    costCurrency = cost?.currency,
    costPerDayInPacks = cost?.perDayInPacks,
)

fun HabitEventEntity.toDomain(): HabitEvent = HabitEvent(
    id = id,
    habitId = habitId,
    type = HabitEventType.valueOf(eventType),
    occurredAtEpochMillis = occurredAtEpochMillis,
)

fun HabitEventType.toStorageValue(): String = name

fun GoodHabitEntity.toDomain(): GoodHabit = GoodHabit(
    id = id,
    name = name,
    dailyTarget = dailyTarget,
    colorArgb = colorArgb,
    createdAtEpochMillis = createdAtEpochMillis,
    archivedAtEpochMillis = archivedAtEpochMillis,
)

fun GoodHabitLogEntity.toDomain(): GoodHabitLog = GoodHabitLog(goodHabitId, dayIndex, count)

fun EventEntity.toDomain(): Event = Event(
    id = id,
    title = title,
    category = EventCategory.entries.firstOrNull { it.name == category } ?: EventCategory.OTHER,
    targetAtEpochMillis = targetAtEpochMillis,
    reminderLeadMillis = decodeReminders(reminders),
    createdAtEpochMillis = createdAtEpochMillis,
    source = EventSource.entries.firstOrNull { it.name == source } ?: EventSource.LOCAL,
    allDay = allDay,
)

fun CoachMessageEntity.toDomain(): CoachMessage = CoachMessage(
    id = id,
    role = if (role == CoachRole.USER.name) CoachRole.USER else CoachRole.COACH,
    text = text,
    createdAtEpochMillis = createdAtEpochMillis,
)

fun encodeReminders(leads: List<Long>): String = leads.distinct().sortedDescending().joinToString(",")

fun decodeReminders(raw: String): List<Long> =
    raw.split(',').mapNotNull { it.trim().toLongOrNull() }.filter { it >= 0 }
