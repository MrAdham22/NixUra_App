package com.example.reclaimapp.data.local

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import com.example.reclaimapp.data.local.dao.CoachMessageDao
import com.example.reclaimapp.data.local.dao.EventDao
import com.example.reclaimapp.data.local.dao.GoodHabitDao
import com.example.reclaimapp.data.local.dao.GoodHabitLogDao
import com.example.reclaimapp.data.local.dao.HabitDao
import com.example.reclaimapp.data.local.dao.HabitEventDao
import com.example.reclaimapp.data.local.dao.HabitWriteDao
import com.example.reclaimapp.data.local.dao.MaintenanceDao
import com.example.reclaimapp.data.local.dao.PreferenceDao
import com.example.reclaimapp.data.local.entity.CoachMessageEntity
import com.example.reclaimapp.data.local.entity.EventEntity
import com.example.reclaimapp.data.local.entity.GoodHabitEntity
import com.example.reclaimapp.data.local.entity.GoodHabitLogEntity
import com.example.reclaimapp.data.local.entity.HabitEntity
import com.example.reclaimapp.data.local.entity.HabitEventEntity
import com.example.reclaimapp.data.local.entity.PreferenceEntity

@Database(
    entities = [
        HabitEntity::class,
        HabitEventEntity::class,
        GoodHabitEntity::class,
        GoodHabitLogEntity::class,
        EventEntity::class,
        CoachMessageEntity::class,
        PreferenceEntity::class,
    ],
    version = 2,
    exportSchema = true,
)
@ConstructedBy(NixUraDatabaseConstructor::class)
abstract class NixUraDatabase : RoomDatabase() {
    abstract fun habitDao(): HabitDao
    abstract fun habitEventDao(): HabitEventDao
    abstract fun habitWriteDao(): HabitWriteDao
    abstract fun goodHabitDao(): GoodHabitDao
    abstract fun goodHabitLogDao(): GoodHabitLogDao
    abstract fun eventDao(): EventDao
    abstract fun coachMessageDao(): CoachMessageDao
    abstract fun preferenceDao(): PreferenceDao
    abstract fun maintenanceDao(): MaintenanceDao
}

@Suppress("KotlinNoActualForExpect")
expect object NixUraDatabaseConstructor : RoomDatabaseConstructor<NixUraDatabase> {
    override fun initialize(): NixUraDatabase
}
