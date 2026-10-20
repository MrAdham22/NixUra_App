package com.example.reclaimapp.data.local

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import com.example.reclaimapp.data.local.dao.HabitDao
import com.example.reclaimapp.data.local.dao.HabitEventDao
import com.example.reclaimapp.data.local.dao.HabitWriteDao
import com.example.reclaimapp.data.local.entity.HabitEntity
import com.example.reclaimapp.data.local.entity.HabitEventEntity

@Database(
    entities = [HabitEntity::class, HabitEventEntity::class],
    version = 1,
    exportSchema = true,
)
@ConstructedBy(NixUraDatabaseConstructor::class)
abstract class NixUraDatabase : RoomDatabase() {
    abstract fun habitDao(): HabitDao
    abstract fun habitEventDao(): HabitEventDao
    abstract fun habitWriteDao(): HabitWriteDao
}

@Suppress("KotlinNoActualForExpect")
expect object NixUraDatabaseConstructor : RoomDatabaseConstructor<NixUraDatabase> {
    override fun initialize(): NixUraDatabase
}
