export type UsageKind = 'chat' | 'voice';

export interface Usage {
  chat: number;
  voice: number;
}

/** Counts each user's AI uses per UTC day. */
export interface UsageStore {
  get(uid: string, day: string): Promise<Usage>;
  /** Takes one use if the user is under [limit]; returns the new counts, or null when the limit is reached. */
  reserve(uid: string, day: string, kind: UsageKind, limit: number): Promise<Usage | null>;
  /** Gives a use back (the AI could not answer, so it should not count). */
  refund(uid: string, day: string, kind: UsageKind): Promise<void>;
  prune(beforeDay: string): Promise<void>;
}

export class D1UsageStore implements UsageStore {
  constructor(private readonly db: D1Database) {}

  async get(uid: string, day: string): Promise<Usage> {
    const row = await this.db.prepare('SELECT chat, voice FROM ai_usage WHERE uid = ? AND day = ?').bind(uid, day).first<Usage>();
    return row ?? { chat: 0, voice: 0 };
  }

  async reserve(uid: string, day: string, kind: UsageKind, limit: number): Promise<Usage | null> {
    if (limit < 1) return null;
    // One statement: check and count together, so two requests at once cannot both slip under the limit.
    const sql = kind === 'chat'
      ? `INSERT INTO ai_usage (uid, day, chat, voice) VALUES (?, ?, 1, 0)
         ON CONFLICT (uid, day) DO UPDATE SET chat = chat + 1 WHERE chat < ? RETURNING chat, voice`
      : `INSERT INTO ai_usage (uid, day, chat, voice) VALUES (?, ?, 0, 1)
         ON CONFLICT (uid, day) DO UPDATE SET voice = voice + 1 WHERE voice < ? RETURNING chat, voice`;
    return (await this.db.prepare(sql).bind(uid, day, limit).first<Usage>()) ?? null;
  }

  async refund(uid: string, day: string, kind: UsageKind): Promise<void> {
    const sql = kind === 'chat'
      ? 'UPDATE ai_usage SET chat = MAX(chat - 1, 0) WHERE uid = ? AND day = ?'
      : 'UPDATE ai_usage SET voice = MAX(voice - 1, 0) WHERE uid = ? AND day = ?';
    await this.db.prepare(sql).bind(uid, day).run();
  }

  async prune(beforeDay: string): Promise<void> {
    await this.db.prepare('DELETE FROM ai_usage WHERE day < ?').bind(beforeDay).run();
  }
}
