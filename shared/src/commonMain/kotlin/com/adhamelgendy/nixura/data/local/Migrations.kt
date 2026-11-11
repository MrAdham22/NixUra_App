package com.adhamelgendy.nixura.data.local

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

/**
 * v2 → v3: cloud sync. Additive only: every synced table gets a global id, a last-change time, a delete marker and a
 * "needs upload" flag. Existing rows keep their local ids and start as "needs upload". Written statement by statement
 * so tools/room_migration_check.py can run them.
 */
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE `habits` ADD COLUMN `sync_id` TEXT NOT NULL DEFAULT ''")
        connection.execSQL("ALTER TABLE `habits` ADD COLUMN `updated_at` INTEGER NOT NULL DEFAULT 0")
        connection.execSQL("ALTER TABLE `habits` ADD COLUMN `deleted_at` INTEGER")
        connection.execSQL("ALTER TABLE `habits` ADD COLUMN `dirty` INTEGER NOT NULL DEFAULT 1")
        connection.execSQL("ALTER TABLE `habit_events` ADD COLUMN `sync_id` TEXT NOT NULL DEFAULT ''")
        connection.execSQL("ALTER TABLE `habit_events` ADD COLUMN `updated_at` INTEGER NOT NULL DEFAULT 0")
        connection.execSQL("ALTER TABLE `habit_events` ADD COLUMN `deleted_at` INTEGER")
        connection.execSQL("ALTER TABLE `habit_events` ADD COLUMN `dirty` INTEGER NOT NULL DEFAULT 1")
        connection.execSQL("ALTER TABLE `good_habits` ADD COLUMN `sync_id` TEXT NOT NULL DEFAULT ''")
        connection.execSQL("ALTER TABLE `good_habits` ADD COLUMN `updated_at` INTEGER NOT NULL DEFAULT 0")
        connection.execSQL("ALTER TABLE `good_habits` ADD COLUMN `deleted_at` INTEGER")
        connection.execSQL("ALTER TABLE `good_habits` ADD COLUMN `dirty` INTEGER NOT NULL DEFAULT 1")
        connection.execSQL("ALTER TABLE `good_habit_logs` ADD COLUMN `sync_id` TEXT NOT NULL DEFAULT ''")
        connection.execSQL("ALTER TABLE `good_habit_logs` ADD COLUMN `updated_at` INTEGER NOT NULL DEFAULT 0")
        connection.execSQL("ALTER TABLE `good_habit_logs` ADD COLUMN `deleted_at` INTEGER")
        connection.execSQL("ALTER TABLE `good_habit_logs` ADD COLUMN `dirty` INTEGER NOT NULL DEFAULT 1")
        connection.execSQL("ALTER TABLE `events` ADD COLUMN `sync_id` TEXT NOT NULL DEFAULT ''")
        connection.execSQL("ALTER TABLE `events` ADD COLUMN `updated_at` INTEGER NOT NULL DEFAULT 0")
        connection.execSQL("ALTER TABLE `events` ADD COLUMN `deleted_at` INTEGER")
        connection.execSQL("ALTER TABLE `events` ADD COLUMN `dirty` INTEGER NOT NULL DEFAULT 1")

        connection.execSQL("UPDATE `habits` SET `sync_id` = lower(hex(randomblob(16))), `updated_at` = `created_at`")
        connection.execSQL("UPDATE `habit_events` SET `sync_id` = lower(hex(randomblob(16))), `updated_at` = `occurred_at`")
        connection.execSQL("UPDATE `good_habits` SET `sync_id` = lower(hex(randomblob(16))), `updated_at` = `created_at`")
        connection.execSQL(
            "UPDATE `good_habit_logs` SET `updated_at` = CAST(strftime('%s','now') AS INTEGER) * 1000, " +
                "`sync_id` = COALESCE((SELECT g.`sync_id` FROM `good_habits` g WHERE g.`id` = `good_habit_id`), lower(hex(randomblob(16)))) || ':' || `day_index`",
        )
        connection.execSQL("UPDATE `events` SET `sync_id` = lower(hex(randomblob(16))), `updated_at` = `created_at`")

        connection.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_habits_sync_id` ON `habits` (`sync_id`)")
        connection.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_habit_events_sync_id` ON `habit_events` (`sync_id`)")
        connection.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_good_habits_sync_id` ON `good_habits` (`sync_id`)")
        connection.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_good_habit_logs_sync_id` ON `good_habit_logs` (`sync_id`)")
        connection.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_events_sync_id` ON `events` (`sync_id`)")
    }
}

/**
 * v3 → v4: separate coach chats, so there can be a history. Additive only: the one existing chat, if any, becomes the
 * first conversation, titled with the user's first message. Written statement by statement for tools/room_migration_check.py.
 */
val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL(
            "CREATE TABLE IF NOT EXISTS `coach_conversations` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`title` TEXT NOT NULL, `created_at` INTEGER NOT NULL, `updated_at` INTEGER NOT NULL)",
        )
        connection.execSQL("ALTER TABLE `coach_messages` ADD COLUMN `conversation_id` INTEGER NOT NULL DEFAULT 0")
        connection.execSQL(
            "INSERT INTO `coach_conversations` (`title`, `created_at`, `updated_at`) " +
                "SELECT COALESCE((SELECT substr(u.`text`, 1, 60) FROM `coach_messages` u WHERE u.`role` = 'USER' ORDER BY u.`created_at`, u.`id` LIMIT 1), ''), " +
                "MIN(`created_at`), MAX(`created_at`) FROM `coach_messages` HAVING COUNT(*) > 0",
        )
        connection.execSQL("UPDATE `coach_messages` SET `conversation_id` = (SELECT MAX(`id`) FROM `coach_conversations`)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_coach_messages_conversation_id` ON `coach_messages` (`conversation_id`)")
    }
}

/**
 * v4 → v5, additive only: the money-saved tracker on quit habits (all columns null = no tracker) and the source of an
 * event (LOCAL for existing ones, or an imported Google Calendar / Notion event with its id there).
 */
val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE `habits` ADD COLUMN `cost_item` TEXT")
        connection.execSQL("ALTER TABLE `habits` ADD COLUMN `cost_unit` TEXT")
        connection.execSQL("ALTER TABLE `habits` ADD COLUMN `cost_units_per_day` REAL")
        connection.execSQL("ALTER TABLE `habits` ADD COLUMN `cost_price_minor` INTEGER")
        connection.execSQL("ALTER TABLE `habits` ADD COLUMN `cost_units_per_pack` INTEGER")
        connection.execSQL("ALTER TABLE `habits` ADD COLUMN `cost_currency` TEXT")
        connection.execSQL("ALTER TABLE `habits` ADD COLUMN `cost_per_day_in_packs` INTEGER")
        connection.execSQL("ALTER TABLE `events` ADD COLUMN `source` TEXT NOT NULL DEFAULT 'LOCAL'")
        connection.execSQL("ALTER TABLE `events` ADD COLUMN `external_id` TEXT")
        connection.execSQL("ALTER TABLE `events` ADD COLUMN `external_updated_at` INTEGER")
        connection.execSQL("ALTER TABLE `events` ADD COLUMN `all_day` INTEGER NOT NULL DEFAULT 0")
        connection.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_events_source_external_id` ON `events` (`source`, `external_id`)")
    }
}

/** v5 → v6, additive only: weekly good habits ("N days a week"). Existing habits stay daily (`weekly_target` null). */
val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE `good_habits` ADD COLUMN `weekly_target` INTEGER")
    }
}

/** v6 → v7, additive only: progress cards the AI coach attaches to a reply. Existing messages have none (null). */
val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE `coach_messages` ADD COLUMN `visuals` TEXT")
    }
}
