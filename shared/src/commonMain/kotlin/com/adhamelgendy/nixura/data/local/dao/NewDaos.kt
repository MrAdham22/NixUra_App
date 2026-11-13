package com.adhamelgendy.nixura.data.local.dao

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.adhamelgendy.nixura.data.local.entity.CoachConversationEntity
import com.adhamelgendy.nixura.data.local.entity.CoachMessageEntity
import com.adhamelgendy.nixura.data.local.entity.EventEntity
import com.adhamelgendy.nixura.data.local.entity.GoodHabitEntity
import com.adhamelgendy.nixura.data.local.entity.GoodHabitLogEntity
import com.adhamelgendy.nixura.data.local.entity.PreferenceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GoodHabitDao {
    @Query("SELECT * FROM good_habits WHERE archived_at IS NULL AND deleted_at IS NULL ORDER BY created_at ASC, id ASC")
    fun observeActive(): Flow<List<GoodHabitEntity>>

    @Insert
    suspend fun insert(habit: GoodHabitEntity): Long

    @Query("UPDATE good_habits SET name = :name, daily_target = :dailyTarget, color_argb = :colorArgb, weekly_target = :weeklyTarget, updated_at = :updatedAt, dirty = 1 WHERE id = :id")
    suspend fun updateDetails(id: Long, name: String, dailyTarget: Int, colorArgb: Long?, weeklyTarget: Int?, updatedAt: Long)

    @Query("UPDATE good_habits SET archived_at = :archivedAt, updated_at = :archivedAt, dirty = 1 WHERE id = :id")
    suspend fun archive(id: Long, archivedAt: Long)

    @Query("SELECT sync_id FROM good_habits WHERE id = :id LIMIT 1")
    suspend fun syncIdOf(id: Long): String?
}

@Dao
interface GoodHabitLogDao {
    @Query("SELECT * FROM good_habit_logs WHERE deleted_at IS NULL ORDER BY day_index ASC")
    fun observeAll(): Flow<List<GoodHabitLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(log: GoodHabitLogEntity)

    /** Clearing a day keeps a marked row, so the clear also reaches other devices. */
    @Query("UPDATE good_habit_logs SET count = 0, deleted_at = :at, updated_at = :at, dirty = 1 WHERE good_habit_id = :goodHabitId AND day_index = :dayIndex")
    suspend fun markDeleted(goodHabitId: Long, dayIndex: Long, at: Long)
}

/**
 * Stored in events.external_updated_at when the importer removed a row itself (not the user), so it can come back.
 * Real values are Google or Notion update times, which are never negative.
 */
const val REMOVED_BY_IMPORT: Long = -1L
private const val REMOVED_BY_IMPORT_SQL = "-1"

@Dao
interface EventDao {
    @Query("SELECT * FROM events WHERE deleted_at IS NULL ORDER BY target_at ASC, id ASC")
    fun observeAll(): Flow<List<EventEntity>>

    @Insert
    suspend fun insert(event: EventEntity): Long

    @Query("UPDATE events SET title = :title, category = :category, target_at = :targetAt, reminders = :reminders, updated_at = :updatedAt, dirty = 1 WHERE id = :id")
    suspend fun update(id: Long, title: String, category: String, targetAt: Long, reminders: String, updatedAt: Long)

    /** Deleting keeps a marked row, so the delete also reaches other devices. */
    @Query("UPDATE events SET deleted_at = :at, updated_at = :at, dirty = 1 WHERE id = :id")
    suspend fun markDeleted(id: Long, at: Long)

    // ---- imported events (Google Calendar, Notion) ----

    /** Deleted rows included: the importer must remember what the user deleted. */
    @Query("SELECT * FROM events WHERE source = :source AND external_id IS NOT NULL")
    suspend fun importedRows(source: String): List<EventEntity>

    @Query("SELECT * FROM events WHERE source = :source AND external_id = :externalId LIMIT 1")
    suspend fun importedRow(source: String, externalId: String): EventEntity?

    /** Updates an imported event from its source; the user's reminders and category stay. Clears a local delete. */
    @Query(
        "UPDATE events SET title = :title, target_at = :targetAt, all_day = :allDay, external_updated_at = :externalUpdatedAt, " +
            "category = CASE WHEN category = 'OTHER' AND :category IS NOT NULL THEN :category ELSE category END, " +
            "deleted_at = NULL, updated_at = :now, dirty = 1 WHERE id = :id",
    )
    /** [category] fills in a row still on Other; a category the user picked is kept. */
    suspend fun updateImported(id: Long, title: String, targetAt: Long, allDay: Boolean, externalUpdatedAt: Long, category: String?, now: Long)

    /** The importer's own delete: [REMOVED_BY_IMPORT] in external_updated_at tells it apart from the user's. */
    @Query("UPDATE events SET deleted_at = :at, external_updated_at = $REMOVED_BY_IMPORT_SQL, updated_at = :at, dirty = 1 WHERE id = :id")
    suspend fun markRemovedByImport(id: Long, at: Long)

    /** Disconnect: every row from [source] goes, and rows the user deleted are no longer remembered as such. */
    @Query(
        "UPDATE events SET deleted_at = COALESCE(deleted_at, :at), external_updated_at = $REMOVED_BY_IMPORT_SQL, " +
            "updated_at = :at, dirty = 1 WHERE source = :source AND external_id IS NOT NULL",
    )
    suspend fun markAllFromSourceRemovedByImport(source: String, at: Long)
}

/** A chat as the history list shows it. */
data class CoachConversationRow(
    val id: Long,
    val title: String,
    val preview: String?,
    @ColumnInfo(name = "updated_at") val updatedAtEpochMillis: Long,
)

@Dao
abstract class CoachMessageDao {
    @Query("SELECT * FROM coach_messages WHERE conversation_id = :conversationId ORDER BY created_at ASC, id ASC")
    abstract fun observeMessages(conversationId: Long): Flow<List<CoachMessageEntity>>

    /** Chats the user actually took part in, newest first. A chat that only holds the coach's greeting is not history. */
    @Query(
        "SELECT c.id, c.title, c.updated_at, " +
            "(SELECT m.text FROM coach_messages m WHERE m.conversation_id = c.id ORDER BY m.created_at DESC, m.id DESC LIMIT 1) AS preview " +
            "FROM coach_conversations c " +
            "WHERE EXISTS (SELECT 1 FROM coach_messages u WHERE u.conversation_id = c.id AND u.role = 'USER') " +
            "ORDER BY c.updated_at DESC, c.id DESC",
    )
    abstract fun observeConversations(): Flow<List<CoachConversationRow>>

    @Query("SELECT EXISTS (SELECT 1 FROM coach_conversations WHERE id = :conversationId)")
    abstract suspend fun conversationExists(conversationId: Long): Boolean

    @Query("SELECT id FROM coach_conversations ORDER BY updated_at DESC, id DESC LIMIT 1")
    abstract suspend fun latestConversationId(): Long?

    /** A chat with no message from the user yet; "New chat" reuses it instead of piling up empty ones. */
    @Query(
        "SELECT c.id FROM coach_conversations c " +
            "WHERE NOT EXISTS (SELECT 1 FROM coach_messages u WHERE u.conversation_id = c.id AND u.role = 'USER') " +
            "ORDER BY c.id DESC LIMIT 1",
    )
    abstract suspend fun unusedConversationId(): Long?

    @Query("SELECT COUNT(*) FROM coach_messages WHERE conversation_id = :conversationId")
    abstract suspend fun messageCount(conversationId: Long): Int

    /** Rewrites the coach's greeting while the user has not written in the chat yet; a used chat is never touched. */
    @Query(
        "UPDATE coach_messages SET text = :text WHERE conversation_id = :conversationId AND role = 'COACH' " +
            "AND NOT EXISTS (SELECT 1 FROM coach_messages u WHERE u.conversation_id = :conversationId AND u.role = 'USER')",
    )
    abstract suspend fun refreshUnusedGreeting(conversationId: Long, text: String): Int

    @Insert
    abstract suspend fun insertConversation(conversation: CoachConversationEntity): Long

    @Insert
    abstract suspend fun insert(message: CoachMessageEntity): Long

    /** Bumps the chat to the top of the history; [titleIfNew] names it unless it already has a title. */
    @Query("UPDATE coach_conversations SET updated_at = :at, title = CASE WHEN title = '' THEN :titleIfNew ELSE title END WHERE id = :conversationId")
    abstract suspend fun touchConversation(conversationId: Long, at: Long, titleIfNew: String)

    @Transaction
    open suspend fun append(message: CoachMessageEntity, titleIfNew: String): Long {
        val id = insert(message)
        touchConversation(message.conversationId, message.createdAtEpochMillis, titleIfNew)
        return id
    }

    @Query("DELETE FROM coach_messages WHERE conversation_id = :conversationId")
    abstract suspend fun deleteMessages(conversationId: Long)

    @Query("DELETE FROM coach_conversations WHERE id = :conversationId")
    abstract suspend fun deleteConversationRow(conversationId: Long)

    @Transaction
    open suspend fun deleteConversation(conversationId: Long) {
        deleteMessages(conversationId)
        deleteConversationRow(conversationId)
    }
}

@Dao
interface PreferenceDao {
    @Query("SELECT * FROM preferences")
    fun observeAll(): Flow<List<PreferenceEntity>>

    @Query("SELECT * FROM preferences")
    suspend fun getAll(): List<PreferenceEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<PreferenceEntity>)

    @Query("DELETE FROM preferences WHERE pref_key = :key")
    suspend fun delete(key: String)
}
