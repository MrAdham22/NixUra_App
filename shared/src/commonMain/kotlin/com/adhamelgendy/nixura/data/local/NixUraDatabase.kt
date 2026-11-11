package com.adhamelgendy.nixura.data.local

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import com.adhamelgendy.nixura.data.local.dao.CoachMessageDao
import com.adhamelgendy.nixura.data.local.dao.EventDao
import com.adhamelgendy.nixura.data.local.dao.GoodHabitDao
import com.adhamelgendy.nixura.data.local.dao.GoodHabitLogDao
import com.adhamelgendy.nixura.data.local.dao.HabitDao
import com.adhamelgendy.nixura.data.local.dao.HabitEventDao
import com.adhamelgendy.nixura.data.local.dao.HabitWriteDao
import com.adhamelgendy.nixura.data.local.dao.MaintenanceDao
import com.adhamelgendy.nixura.data.local.dao.PreferenceDao
import com.adhamelgendy.nixura.data.local.dao.SyncDao
import com.adhamelgendy.nixura.data.local.entity.CoachConversationEntity
import com.adhamelgendy.nixura.data.local.entity.CoachMessageEntity
import com.adhamelgendy.nixura.data.local.entity.EventEntity
import com.adhamelgendy.nixura.data.local.entity.GoodHabitEntity
import com.adhamelgendy.nixura.data.local.entity.GoodHabitLogEntity
import com.adhamelgendy.nixura.data.local.entity.HabitEntity
import com.adhamelgendy.nixura.data.local.entity.HabitEventEntity
import com.adhamelgendy.nixura.data.local.entity.PreferenceEntity

@Database(
    entities = [
        HabitEntity::class,
        HabitEventEntity::class,
        GoodHabitEntity::class,
        GoodHabitLogEntity::class,
        EventEntity::class,
        CoachConversationEntity::class,
        CoachMessageEntity::class,
        PreferenceEntity::class,
    ],
    version = 7,
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
    abstract fun syncDao(): SyncDao
}

@Suppress("KotlinNoActualForExpect")
expect object NixUraDatabaseConstructor : RoomDatabaseConstructor<NixUraDatabase> {
    override fun initialize(): NixUraDatabase
}
