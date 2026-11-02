import type { Db } from '../db/db.js';

export const SYNC_ENTITY_TYPES = ['habit', 'habit_event', 'good_habit', 'good_habit_log', 'event', 'settings'] as const;
export type SyncEntityType = (typeof SYNC_ENTITY_TYPES)[number];

export interface SyncChange {
  type: SyncEntityType;
  id: string;
  updatedAt: number;
  deleted: boolean;
  payload: string;
}

export interface SyncPushResult {
  type: SyncEntityType;
  id: string;
  /** False when the server already holds a newer version; the device gets that version on its next pull. */
  applied: boolean;
}

export interface SyncPullPage {
  items: SyncChange[];
  cursor: string;
  hasMore: boolean;
}

interface SyncRow {
  entity_type: SyncEntityType;
  entity_id: string;
  payload: string;
  deleted: boolean;
  updated_at: string | number | bigint;
  seq: string | number | bigint;
}

/**
 * Per-user change log behind the sync endpoints. Every query filters on user_id, so one account can never read or
 * overwrite another's rows.
 *
 * Conflicts: last write wins per item, by the device's timestamp, capped at the server's clock so a phone with a wrong
 * clock cannot win forever. Equal timestamps with different content pick the larger payload, so every device ends up
 * with the same winner. Re-sending the same change does nothing, so retries are safe.
 */
export class SyncService {
  constructor(private readonly db: Db, private readonly clock: () => Date = () => new Date()) {}

  async push(userId: string, changes: SyncChange[]): Promise<SyncPushResult[]> {
    const now = this.clock().getTime();
    return this.db.tx(async (tx) => {
      // Sequence numbers must be handed out in commit order per user, or a pull could skip a change that commits late.
      await tx.query('SELECT pg_advisory_xact_lock(hashtextextended($1, 0))', [userId]);
      const results: SyncPushResult[] = [];
      for (const c of changes) {
        const updated = await tx.query(
          `INSERT INTO sync_items (user_id, entity_type, entity_id, payload, deleted, updated_at, seq)
           VALUES ($1, $2, $3, $4, $5, $6, nextval('sync_seq'))
           ON CONFLICT (user_id, entity_type, entity_id) DO UPDATE
             SET payload = EXCLUDED.payload, deleted = EXCLUDED.deleted, updated_at = EXCLUDED.updated_at, seq = EXCLUDED.seq
             WHERE EXCLUDED.updated_at > sync_items.updated_at
                OR (EXCLUDED.updated_at = sync_items.updated_at AND EXCLUDED.payload > sync_items.payload)
           RETURNING entity_id`,
          [userId, c.type, c.id, c.payload, c.deleted, Math.min(c.updatedAt, now)],
        );
        results.push({ type: c.type, id: c.id, applied: updated.length > 0 });
      }
      return results;
    });
  }

  async pull(userId: string, cursor: string, limit: number): Promise<SyncPullPage> {
    const rows = await this.db.query<SyncRow>(
      `SELECT entity_type, entity_id, payload, deleted, updated_at, seq FROM sync_items
       WHERE user_id = $1 AND seq > $2::bigint ORDER BY seq ASC LIMIT $3`,
      [userId, cursor, limit + 1],
    );
    const hasMore = rows.length > limit;
    const page = hasMore ? rows.slice(0, limit) : rows;
    return {
      items: page.map((r) => ({
        type: r.entity_type,
        id: r.entity_id,
        updatedAt: Number(r.updated_at),
        deleted: r.deleted,
        payload: r.payload,
      })),
      cursor: page.length > 0 ? String(page[page.length - 1]!.seq) : cursor,
      hasMore,
    };
  }
}
