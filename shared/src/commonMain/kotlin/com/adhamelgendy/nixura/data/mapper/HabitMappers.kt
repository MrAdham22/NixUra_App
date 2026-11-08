package com.adhamelgendy.nixura.data.mapper

import com.adhamelgendy.nixura.data.local.entity.CoachMessageEntity
import com.adhamelgendy.nixura.data.local.entity.EventEntity
import com.adhamelgendy.nixura.data.local.entity.GoodHabitEntity
import com.adhamelgendy.nixura.data.local.entity.GoodHabitLogEntity
import com.adhamelgendy.nixura.data.local.entity.HabitEntity
import com.adhamelgendy.nixura.data.local.entity.HabitEventEntity
import com.adhamelgendy.nixura.domain.model.CoachMessage
import com.adhamelgendy.nixura.domain.model.CoachRole
import com.adhamelgendy.nixura.domain.model.Event
import com.adhamelgendy.nixura.domain.model.EventCategory
import com.adhamelgendy.nixura.domain.model.EventSource
import com.adhamelgendy.nixura.domain.model.GoodHabit
import com.adhamelgendy.nixura.domain.model.GoodHabitLog
import com.adhamelgendy.nixura.domain.model.Habit
import com.adhamelgendy.nixura.domain.model.HabitEvent
import com.adhamelgendy.nixura.domain.model.HabitEventType
import com.adhamelgendy.nixura.domain.money.CostUnit
import com.adhamelgendy.nixura.domain.money.HabitCost

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
