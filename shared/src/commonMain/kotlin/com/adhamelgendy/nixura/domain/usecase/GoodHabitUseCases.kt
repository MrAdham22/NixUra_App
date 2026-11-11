package com.adhamelgendy.nixura.domain.usecase

import com.adhamelgendy.nixura.domain.calculator.WeeklyGoalCalculator
import com.adhamelgendy.nixura.domain.model.GoodHabit
import com.adhamelgendy.nixura.domain.model.GoodHabitWithLogs
import com.adhamelgendy.nixura.domain.repository.GoodHabitRepository
import com.adhamelgendy.nixura.domain.validation.HabitNameValidator
import kotlinx.coroutines.flow.Flow

const val MAX_DAILY_TARGET = 99
const val MAX_DAILY_COUNT = 999

/** The most a single day can be logged: a weekly habit's day counts once. */
val GoodHabit.maxCountPerDay: Int get() = if (isWeekly) 1 else MAX_DAILY_COUNT

class ObserveGoodHabitsUseCase(private val repository: GoodHabitRepository) {
    operator fun invoke(): Flow<List<GoodHabitWithLogs>> = repository.observeGoodHabits()
}

/** [weeklyTarget] `null` = daily habit with [dailyTarget] times a day; otherwise 1..7 days a week. */
private fun checkTargets(dailyTarget: Int, weeklyTarget: Int?): Int {
    if (weeklyTarget != null) {
        require(weeklyTarget in 1..WeeklyGoalCalculator.MAX_DAYS_PER_WEEK) {
            "Weekly target must be between 1 and ${WeeklyGoalCalculator.MAX_DAYS_PER_WEEK} days"
        }
        return 1
    }
    require(dailyTarget in 1..MAX_DAILY_TARGET) { "Daily target must be between 1 and $MAX_DAILY_TARGET" }
    return dailyTarget
}

class CreateGoodHabitUseCase(private val repository: GoodHabitRepository) {
    suspend operator fun invoke(name: String, dailyTarget: Int, colorArgb: Long?, weeklyTarget: Int? = null): Result<Long> =
        HabitNameValidator.validate(name).mapCatching { valid ->
            repository.create(valid, checkTargets(dailyTarget, weeklyTarget), colorArgb, weeklyTarget)
        }
}

class UpdateGoodHabitUseCase(private val repository: GoodHabitRepository) {
    suspend operator fun invoke(id: Long, name: String, dailyTarget: Int, colorArgb: Long?, weeklyTarget: Int? = null): Result<Unit> =
        HabitNameValidator.validate(name).mapCatching { valid ->
            require(id > 0) { "Invalid habit id" }
            repository.update(id, valid, checkTargets(dailyTarget, weeklyTarget), colorArgb, weeklyTarget)
        }
}

class SetGoodHabitCountUseCase(private val repository: GoodHabitRepository) {
    /** [maxPerDay] is [GoodHabit.maxCountPerDay]: 1 for a weekly habit. */
    suspend operator fun invoke(id: Long, dayIndex: Long, count: Int, maxPerDay: Int = MAX_DAILY_COUNT): Result<Unit> = runCatching {
        require(id > 0) { "Invalid habit id" }
        repository.setCount(id, dayIndex, count.coerceIn(0, maxPerDay.coerceIn(1, MAX_DAILY_COUNT)))
    }
}
