package com.adhamelgendy.nixura.domain.usecase

import com.adhamelgendy.nixura.domain.model.GoodHabit
import com.adhamelgendy.nixura.domain.model.GoodHabitWithLogs
import com.adhamelgendy.nixura.domain.repository.GoodHabitDayCount
import com.adhamelgendy.nixura.domain.repository.GoodHabitRepository
import com.adhamelgendy.nixura.domain.theme.HabitTheme
import com.adhamelgendy.nixura.domain.theme.WaterGoal
import com.adhamelgendy.nixura.domain.widget.DAY
import com.adhamelgendy.nixura.domain.widget.FixedClock
import com.adhamelgendy.nixura.domain.widget.HOUR
import com.adhamelgendy.nixura.domain.widget.utcDays
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Widget "+" / "-" taps arriving at the same time on real threads (Dispatchers.Default), with a repository that
 * suspends between the read and the write. Every tap must count exactly once: no lost update, no double write.
 */
class WidgetTapConcurrencyTest {
    private val today = 10L
    private val clock = FixedClock(10 * DAY + 9 * HOUR)

    /** Thread-safe in-memory store that pauses inside every read and write, so unsynchronized taps would interleave. */
    private class SlowRepo(vararg habits: Pair<GoodHabit, Int>) : GoodHabitRepository {
        private val guard = Mutex()
        private val habits = habits.associate { it.first.id to it.first }
        private val counts = habits.associate { it.first.id to it.second }.toMutableMap()
        var writes = 0
        var fullListReads = 0

        override fun observeGoodHabits(): Flow<List<GoodHabitWithLogs>> = flow {
            fullListReads++
            error("the tap path must not load every habit's history")
        }

        override suspend fun dayCountOf(id: Long, dayIndex: Long): GoodHabitDayCount? {
            val habit = habits[id] ?: return null
            val count = guard.withLock { counts.getValue(id) }
            delay(1) // a read that takes time: another tap could run here without the use case's lock
            return GoodHabitDayCount(habit, count)
        }

        override suspend fun setCount(id: Long, dayIndex: Long, count: Int) {
            delay(1)
            guard.withLock {
                writes++
                counts[id] = count.coerceAtLeast(0)
            }
        }

        suspend fun count(id: Long) = guard.withLock { counts.getValue(id) }

        override suspend fun create(name: String, dailyTarget: Int, colorArgb: Long?, weeklyTarget: Int?, theme: HabitTheme, water: WaterGoal?): Long = error("unused")
        override suspend fun update(id: Long, name: String, dailyTarget: Int, colorArgb: Long?, weeklyTarget: Int?, theme: HabitTheme, water: WaterGoal?) = error("unused")
        override suspend fun archive(id: Long) = error("unused")
    }

    private fun habit(id: Long) = GoodHabit(id, "Pray", 5, createdAtEpochMillis = 0L)
    private fun inc(r: SlowRepo) = IncrementGoodHabitTodayUseCase(r, clock, utcDays())
    private fun dec(r: SlowRepo) = DecrementGoodHabitTodayUseCase(r, clock, utcDays())

    /** Runs [taps] taps at once on real threads; [tap] gets the tap's index. */
    private suspend fun parallel(taps: Int, tap: suspend (Int) -> Result<Int>): List<Result<Int>> =
        withContext(Dispatchers.Default) { (0 until taps).map { i -> async { tap(i) } }.awaitAll() }

    @Test fun twoQuickTapsAddExactlyTwo() = runTest {
        val r = SlowRepo(habit(1) to 0)
        val use = inc(r)
        parallel(2) { use(1) }
        assertEquals(2, r.count(1))
        assertEquals(2, r.writes)
    }

    @Test fun fiftyParallelTapsAddExactlyFiftyAndWriteFiftyTimes() = runTest {
        val r = SlowRepo(habit(1) to 0)
        val use = inc(r)
        val results = parallel(50) { use(1) }
        assertEquals(50, r.count(1))
        assertEquals(50, r.writes) // one write per tap: none merged, none repeated
        // Each tap saw a different count: the returned values are exactly 1..50 (what each tap's redraw will show).
        assertEquals((1..50).toList(), results.map { it.getOrThrow() }.sorted())
    }

    @Test fun parallelPlusAndMinusNetOut() = runTest {
        val r = SlowRepo(habit(1) to 25)
        val plus = inc(r)
        val minus = dec(r)
        parallel(50) { i -> if (i < 30) plus(1) else minus(1) }
        assertEquals(35, r.count(1)) // 25 + 30 - 20, whatever the order (never touches 0 or the cap)
        assertEquals(50, r.writes)
    }

    @Test fun parallelTapsOnTwoHabitsKeepTheirOwnCounts() = runTest {
        val r = SlowRepo(habit(1) to 0, habit(2) to 3)
        val use = inc(r)
        parallel(20) { i -> use(if (i % 2 == 0) 1L else 2L) }
        assertEquals(10, r.count(1))
        assertEquals(13, r.count(2))
    }

    @Test fun parallelMinusTapsStopAtZeroWithoutExtraWrites() = runTest {
        val r = SlowRepo(habit(1) to 3)
        val use = dec(r)
        parallel(10) { use(1) }
        assertEquals(0, r.count(1))
        assertEquals(3, r.writes) // 3 -> 2 -> 1 -> 0, then nothing to change
    }

    @Test fun tapReadsOneRowNotTheWholeHistory() = runTest {
        val r = SlowRepo(habit(1) to 0)
        assertTrue(inc(r)(1).isSuccess)
        assertEquals(0, r.fullListReads)
    }

    @Test fun tapOnMissingHabitFailsWithoutWriting() = runTest {
        val r = SlowRepo(habit(1) to 0)
        assertTrue(inc(r)(7).isFailure)
        assertEquals(0, r.writes)
    }

    @Test fun defaultDayCountFindsTodayInTheList() = runTest {
        // Repositories that do not override dayCountOf (test fakes, future ones) still give the right count.
        val r = com.adhamelgendy.nixura.domain.widget.FakeGoodHabitRepository(
            listOf(com.adhamelgendy.nixura.domain.widget.goodHabit(1, 5, (today - 1) to 4, today to 2)),
        )
        assertEquals(2, r.dayCountOf(1, today)?.count)
        assertEquals(0, r.dayCountOf(1, today + 1)?.count)
        assertEquals(null, r.dayCountOf(9, today))
    }
}
