package com.example.reclaimapp.domain.usecase

import com.example.reclaimapp.domain.model.GoodHabitWithLogs
import com.example.reclaimapp.domain.repository.GoodHabitRepository
import com.example.reclaimapp.domain.validation.HabitNameValidator
import kotlinx.coroutines.flow.Flow

const val MAX_DAILY_TARGET = 99
const val MAX_DAILY_COUNT = 999

class ObserveGoodHabitsUseCase(private val repository: GoodHabitRepository) {
    operator fun invoke(): Flow<List<GoodHabitWithLogs>> = repository.observeGoodHabits()
}

class CreateGoodHabitUseCase(private val repository: GoodHabitRepository) {
    suspend operator fun invoke(name: String, dailyTarget: Int, colorArgb: Long?): Result<Long> =
        HabitNameValidator.validate(name).mapCatching { valid ->
            require(dailyTarget in 1..MAX_DAILY_TARGET) { "Daily target must be between 1 and $MAX_DAILY_TARGET" }
            repository.create(valid, dailyTarget, colorArgb)
        }
}

class UpdateGoodHabitUseCase(private val repository: GoodHabitRepository) {
    suspend operator fun invoke(id: Long, name: String, dailyTarget: Int, colorArgb: Long?): Result<Unit> =
        HabitNameValidator.validate(name).mapCatching { valid ->
            require(id > 0) { "Invalid habit id" }
            require(dailyTarget in 1..MAX_DAILY_TARGET) { "Daily target must be between 1 and $MAX_DAILY_TARGET" }
            repository.update(id, valid, dailyTarget, colorArgb)
        }
}

class ArchiveGoodHabitUseCase(private val repository: GoodHabitRepository) {
    suspend operator fun invoke(id: Long): Result<Unit> = runCatching {
        require(id > 0) { "Invalid habit id" }
        repository.archive(id)
    }
}

class SetGoodHabitCountUseCase(private val repository: GoodHabitRepository) {
    suspend operator fun invoke(id: Long, dayIndex: Long, count: Int): Result<Unit> = runCatching {
        require(id > 0) { "Invalid habit id" }
        repository.setCount(id, dayIndex, count.coerceIn(0, MAX_DAILY_COUNT))
    }
}
