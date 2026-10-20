package com.example.reclaimapp.data.local

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase

fun getDatabaseBuilder(context: Context): RoomDatabase.Builder<NixUraDatabase> {
    val appContext = context.applicationContext
    val dbFile = appContext.getDatabasePath("nixura.db")
    return Room.databaseBuilder<NixUraDatabase>(
        context = appContext,
        name = dbFile.absolutePath,
    )
}
