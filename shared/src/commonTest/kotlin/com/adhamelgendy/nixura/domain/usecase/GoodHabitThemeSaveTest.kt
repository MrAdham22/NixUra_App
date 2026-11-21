package com.adhamelgendy.nixura.domain.usecase

import com.adhamelgendy.nixura.domain.coach.HabitKnowledgeBase
import com.adhamelgendy.nixura.domain.model.GoodHabitWithLogs
import com.adhamelgendy.nixura.domain.repository.GoodHabitRepository
import com.adhamelgendy.nixura.domain.theme.HabitTheme
import com.adhamelgendy.nixura.domain.theme.HabitThemeDetector
import com.adhamelgendy.nixura.domain.theme.WaterGoal
import com.adhamelgendy.nixura.domain.theme.WaterUnit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class GoodHabitThemeSaveTest {

    private data class Saved(val dailyTarget: Int, val weeklyTarget: Int?, val theme: HabitTheme, val water: WaterGoal?)

    private class Repo : GoodHabitRepository {
        var last: Saved? = null
        override fun observeGoodHabits(): Flow<List<GoodHabitWithLogs>> = emptyFlow()
        override suspend fun create(name: String, dailyTarget: Int, colorArgb: Long?, weeklyTarget: Int?, theme: HabitTheme, water: WaterGoal?): Long {
            last = Saved(dailyTarget, weeklyTarget, theme, water); return 1
        }
        override suspend fun update(id: Long, name: String, dailyTarget: Int, colorArgb: Long?, weeklyTarget: Int?, theme: HabitTheme, water: WaterGoal?) {
            last = Saved(dailyTarget, weeklyTarget, theme, water)
        }
        override suspend fun archive(id: Long) = Unit
        override suspend fun setReminder(id: Long, reminder: com.adhamelgendy.nixura.domain.reminder.HabitReminderSettings) = error("unused")
        override suspend fun setCount(id: Long, dayIndex: Long, count: Int) = Unit
    }

    private val themes = HabitThemeDetector(HabitKnowledgeBase())
    private val repo = Repo()
    private val create = CreateGoodHabitUseCase(repo, themes)
    private val update = UpdateGoodHabitUseCase(repo, themes)

    @Test
    fun waterHabitStoresItsThemeAndADefaultGoalInCups() = runTest {
        create("Drink water", dailyTarget = 8, colorArgb = null)
        assertEquals(Saved(8, null, HabitTheme.WATER, WaterGoal(2_000, 250, WaterUnit.CUPS)), repo.last)
    }

    @Test
    fun aChosenGoalSetsTheDailyTargetInCups() = runTest {
        create("اشرب مياه", dailyTarget = 1, colorArgb = null, water = WaterGoal.fromLiters(3.0))
        assertEquals(12, repo.last?.dailyTarget)
        assertEquals(3_000, repo.last?.water?.goalMl)
    }

    @Test
    fun otherHabitsStoreTheirThemeAndNoWaterGoal() = runTest {
        create("صلاة", dailyTarget = 5, colorArgb = null)
        assertEquals(Saved(5, null, HabitTheme.PRAYER, null), repo.last)
        create("Read 10 pages", dailyTarget = 1, colorArgb = null)
        assertEquals(HabitTheme.READING, repo.last?.theme)
    }

    @Test
    fun renamingAwayFromWaterDropsTheGoal() = runTest {
        update(7, "Walk", dailyTarget = 1, colorArgb = null, water = WaterGoal.fromLiters(2.0))
        assertEquals(HabitTheme.WALK, repo.last?.theme)
        assertNull(repo.last?.water)
    }

    @Test
    fun weeklyWaterHabitHasNoDailyGoal() = runTest {
        create("Drink water", dailyTarget = 1, colorArgb = null, weeklyTarget = 3)
        assertEquals(Saved(1, 3, HabitTheme.WATER, null), repo.last)
    }
}
