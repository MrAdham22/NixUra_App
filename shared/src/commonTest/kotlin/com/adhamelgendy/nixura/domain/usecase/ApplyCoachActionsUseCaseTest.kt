package com.adhamelgendy.nixura.domain.usecase

import com.adhamelgendy.nixura.domain.clock.Clock
import com.adhamelgendy.nixura.domain.coach.ai.CoachAction
import com.adhamelgendy.nixura.domain.model.Event
import com.adhamelgendy.nixura.domain.model.EventCategory
import com.adhamelgendy.nixura.domain.model.GoodHabitWithLogs
import com.adhamelgendy.nixura.domain.model.HabitTracking
import com.adhamelgendy.nixura.domain.money.HabitCost
import com.adhamelgendy.nixura.domain.repository.EventRepository
import com.adhamelgendy.nixura.domain.repository.GoodHabitRepository
import com.adhamelgendy.nixura.domain.repository.HabitTrackingRepository
import com.adhamelgendy.nixura.domain.time.DayCalculator
import com.adhamelgendy.nixura.domain.time.TimeZoneOffsetProvider
import com.adhamelgendy.nixura.domain.widget.runSuspend
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlin.test.Test
import kotlin.test.assertEquals

/** The values the user corrected in the voice note sheet are the ones that get saved. */
class ApplyCoachActionsUseCaseTest {
    private val days = DayCalculator(TimeZoneOffsetProvider { 0L })
    private val now = days.epochMillisOf(2026, 11, 14, 12, 0)
    private val clock = object : Clock { override fun nowEpochMillis() = now }

    private class Quits : HabitTrackingRepository {
        val created = mutableListOf<Pair<String, Long>>()
        override fun observeActiveHabitTracking(): Flow<List<HabitTracking>> = emptyFlow()
        override suspend fun createHabitAndStartTracking(name: String, startAtEpochMillis: Long, colorArgb: Long?, cost: HabitCost?): Long {
            created += name to startAtEpochMillis
            return created.size.toLong()
        }
        override suspend fun recordRelapse(habitId: Long, atEpochMillis: Long) = error("unused")
        override suspend fun updateHabit(habitId: Long, name: String, colorArgb: Long?) = error("unused")
        override suspend fun updateCost(habitId: Long, cost: HabitCost?) = error("unused")
        override suspend fun archiveHabit(habitId: Long, atEpochMillis: Long) = error("unused")
    }

    private class Goods : GoodHabitRepository {
        val created = mutableListOf<Triple<String, Int, Int?>>()
        override fun observeGoodHabits(): Flow<List<GoodHabitWithLogs>> = emptyFlow()
        override suspend fun create(name: String, dailyTarget: Int, colorArgb: Long?, weeklyTarget: Int?): Long {
            created += Triple(name, dailyTarget, weeklyTarget)
            return created.size.toLong()
        }
        override suspend fun update(id: Long, name: String, dailyTarget: Int, colorArgb: Long?, weeklyTarget: Int?) = error("unused")
        override suspend fun archive(id: Long) = error("unused")
        override suspend fun setCount(id: Long, dayIndex: Long, count: Int) = error("unused")
    }

    private class Events : EventRepository {
        val created = mutableListOf<Triple<String, EventCategory, Long>>()
        override fun observeEvents(): Flow<List<Event>> = emptyFlow()
        override suspend fun create(title: String, category: EventCategory, targetAtEpochMillis: Long, reminderLeadMillis: List<Long>): Long {
            created += Triple(title, category, targetAtEpochMillis)
            return created.size.toLong()
        }
        override suspend fun update(id: Long, title: String, category: EventCategory, targetAtEpochMillis: Long, reminderLeadMillis: List<Long>) = error("unused")
        override suspend fun delete(id: Long) = error("unused")
    }

    private val quits = Quits()
    private val goods = Goods()
    private val events = Events()
    private val apply = ApplyCoachActionsUseCase(
        CreateHabitAndStartTrackingUseCase(quits),
        CreateGoodHabitUseCase(goods),
        SaveEventUseCase(events),
        clock,
        days,
    )

    @Test
    fun correctedValuesAreSaved() = runSuspend {
        // The AI heard "15 cups of water" and a meeting with no category; the user fixed both.
        val outcome = apply(
            listOf(
                CoachAction.Build("Drink water", perDay = 5, daysPerWeek = null),
                CoachAction.Build("Gym", perDay = 1, daysPerWeek = 3),
                CoachAction.Quit("Soda"),
                CoachAction.Event("Team meeting", 2026, 11, 16, 19, 0, EventCategory.WORK),
            ),
        )
        assertEquals(ApplyCoachActionsUseCase.Outcome(added = 4, failed = 0), outcome)
        assertEquals(listOf(Triple("Drink water", 5, null), Triple("Gym", 1, 3)), goods.created)
        assertEquals(listOf("Soda" to now), quits.created)
        assertEquals(listOf(Triple("Team meeting", EventCategory.WORK, days.epochMillisOf(2026, 11, 16, 19, 0))), events.created)
    }

    @Test
    fun eventWithoutTimeOrCategoryUsesTheDefaults() = runSuspend {
        apply(listOf(CoachAction.Event("Trip", 2026, 12, 1, null, null)))
        assertEquals(listOf(Triple("Trip", EventCategory.EVENT, days.epochMillisOf(2026, 12, 1, 9, 0))), events.created)
    }
}
