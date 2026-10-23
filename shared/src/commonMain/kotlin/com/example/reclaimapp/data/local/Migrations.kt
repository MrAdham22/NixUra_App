package com.example.reclaimapp.data.local

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

/**
 * v1 → v2: custom habit card color, good habits (+ daily logs), events with reminders,
 * persisted coach chat, and a small key/value table for profile + settings.
 *
 * The SQL mirrors what Room generates for the v2 entities. If an entity changes, update this
 * migration (or add a new one) and keep the exported schema JSON in shared/schemas in sync.
 */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE `habits` ADD COLUMN `color_argb` INTEGER")
        connection.execSQL(
            "CREATE TABLE IF NOT EXISTS `good_habits` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`name` TEXT NOT NULL, `daily_target` INTEGER NOT NULL, `color_argb` INTEGER, " +
                "`created_at` INTEGER NOT NULL, `archived_at` INTEGER)",
        )
        connection.execSQL(
            "CREATE TABLE IF NOT EXISTS `good_habit_logs` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`good_habit_id` INTEGER NOT NULL, `day_index` INTEGER NOT NULL, `count` INTEGER NOT NULL)",
        )
        connection.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_good_habit_logs_good_habit_id_day_index` " +
                "ON `good_habit_logs` (`good_habit_id`, `day_index`)",
        )
        connection.execSQL(
            "CREATE TABLE IF NOT EXISTS `events` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`title` TEXT NOT NULL, `category` TEXT NOT NULL, `target_at` INTEGER NOT NULL, " +
                "`reminders` TEXT NOT NULL, `created_at` INTEGER NOT NULL)",
        )
        connection.execSQL(
            "CREATE TABLE IF NOT EXISTS `coach_messages` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`role` TEXT NOT NULL, `text` TEXT NOT NULL, `created_at` INTEGER NOT NULL)",
        )
        connection.execSQL(
            "CREATE TABLE IF NOT EXISTS `preferences` (`pref_key` TEXT NOT NULL, `pref_value` TEXT NOT NULL, " +
                "PRIMARY KEY(`pref_key`))",
        )
    }
}
