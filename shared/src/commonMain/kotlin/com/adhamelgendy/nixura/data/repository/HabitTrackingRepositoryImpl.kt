package com.adhamelgendy.nixura.data.repository

import com.adhamelgendy.nixura.data.local.dao.HabitDao
import com.adhamelgendy.nixura.data.local.dao.HabitEventDao
import com.adhamelgendy.nixura.data.local.dao.HabitWriteDao
import com.adhamelgendy.nixura.data.local.entity.HabitEntity
import com.adhamelgendy.nixura.data.local.entity.HabitEventEntity
import com.adhamelgendy.nixura.data.mapper.toDomain
import com.adhamelgendy.nixura.data.mapper.toStorageValue
import com.adhamelgendy.nixura.data.mapper.withCost
import com.adhamelgendy.nixura.domain.money.HabitCost
import com.adhamelgendy.nixura.domain.calculator.TrackingStateCalculator
import com.adhamelgendy.nixura.domain.clock.Clock
import com.adhamelgendy.nixura.domain.model.HabitEventType
import com.adhamelgendy.nixura.domain.model.HabitTracking
import com.adhamelgendy.nixura.domain.repository.HabitTrackingRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class HabitTrackingRepositoryImpl(
    private val habitDao: HabitDao,
    private val habitEventDao: HabitEventDao,
    private val habitWriteDao: HabitWriteDao,
    private val clock: Clock,
) : HabitTrackingRepository {

    override fun observeActiveHabitTracking(): Flow<List<HabitTracking>> {
        return habitDao.observeActiveHabits().combine(habitEventDao.observeAllEvents()) { habits, events ->
            val eventsByHabit = events.groupBy { it.habitId }
            val now = clock.nowEpochMillis()
            habits.map { habitEntity ->
                val habit = habitEntity.toDomain()
                val habitEvents = eventsByHabit[habit.id].orEmpty().map { it.toDomain() }
                val tracking = TrackingStateCalculator.compute(habitEvents, now)
                HabitTracking(habit, habitEvents, tracking)
            }
        }
    }

    override suspend fun createHabitAndStartTracking(name: String, startAtEpochMillis: Long, colorArgb: Long?, cost: HabitCost?): Long {
        val createdAt = clock.nowEpochMillis()
        return habitWriteDao.createHabitWithStartEvent(
            habit = HabitEntity(
                name = name,
                createdAtEpochMillis = createdAt,
                colorArgb = colorArgb,
            ).withCost(cost),
            startEvent = HabitEventEntity(
                habitId = 0,
                eventType = HabitEventType.TRACKING_START.toStorageValue(),
                occurredAtEpochMillis = startAtEpochMillis,
            ),
        )
    }

    override suspend fun recordRelapse(habitId: Long, atEpochMillis: Long) {
        val habit = habitDao.getById(habitId)
            ?: throw IllegalArgumentException("Habit not found")
        if (habit.archivedAtEpochMillis != null) {
            throw IllegalStateException("Cannot record relapse on archived habit")
        }
        val events = habitEventDao.getEventsForHabit(habitId)
        if (events.none { it.eventType == HabitEventType.TRACKING_START.name }) {
            throw IllegalStateException("Habit has no tracking start event")
        }
        habitWriteDao.insertRelapseEvent(
            HabitEventEntity(
                habitId = habitId,
                eventType = HabitEventType.RELAPSE.toStorageValue(),
                occurredAtEpochMillis = atEpochMillis,
            ),
        )
    }

    override suspend fun updateHabit(habitId: Long, name: String, colorArgb: Long?) {
        habitDao.getById(habitId) ?: throw IllegalArgumentException("Habit not found")
        habitDao.updateDetails(habitId, name, colorArgb, clock.nowEpochMillis())
    }

    override suspend fun updateCost(habitId: Long, cost: HabitCost?) {
        val c = HabitEntity(name = "", createdAtEpochMillis = 0).withCost(cost)
        habitDao.updateCost(
            habitId, c.costItem, c.costUnit, c.costUnitsPerDay, c.costPriceMinor, c.costUnitsPerPack, c.costCurrency,
            c.costPerDayInPacks, clock.nowEpochMillis(),
        )
    }

    override suspend fun archiveHabit(habitId: Long, atEpochMillis: Long) {
        habitDao.getById(habitId) ?: throw IllegalArgumentException("Habit not found")
        habitDao.archive(habitId, atEpochMillis)
    }
}
