/** Where Notion links and one-time OAuth states live. D1 in production; an in-memory copy in the tests. */
export interface LinkStore {
  putState(state: string, uid: string, expiresAt: number): Promise<void>;
  /** Removes the state and returns who it belonged to; a second call with the same state returns null. */
  takeState(state: string): Promise<{ uid: string; expiresAt: number } | null>;
  pruneStates(now: number): Promise<void>;
  putLink(uid: string, tokenEnc: string, workspace: string, now: number): Promise<void>;
  getLink(uid: string): Promise<{ tokenEnc: string; workspace: string } | null>;
  deleteLink(uid: string): Promise<void>;
}

export class D1LinkStore implements LinkStore {
  constructor(private readonly db: D1Database) {}

  async putState(state: string, uid: string, expiresAt: number): Promise<void> {
    await this.db.prepare('INSERT INTO oauth_states (state, uid, expires_at) VALUES (?, ?, ?)').bind(state, uid, expiresAt).run();
  }

  async takeState(state: string): Promise<{ uid: string; expiresAt: number } | null> {
    // One statement, so two callbacks racing with the same state cannot both get it.
    const row = await this.db
      .prepare('DELETE FROM oauth_states WHERE state = ? RETURNING uid, expires_at')
      .bind(state)
      .first<{ uid: string; expires_at: number }>();
    return row ? { uid: row.uid, expiresAt: row.expires_at } : null;
  }

  async pruneStates(now: number): Promise<void> {
    await this.db.prepare('DELETE FROM oauth_states WHERE expires_at < ?').bind(now).run();
  }

  async putLink(uid: string, tokenEnc: string, workspace: string, now: number): Promise<void> {
    await this.db
      .prepare(
        `INSERT INTO notion_links (uid, token_enc, workspace, created_at) VALUES (?, ?, ?, ?)
         ON CONFLICT (uid) DO UPDATE SET token_enc = excluded.token_enc, workspace = excluded.workspace, created_at = excluded.created_at`,
      )
      .bind(uid, tokenEnc, workspace, now)
      .run();
  }

  async getLink(uid: string): Promise<{ tokenEnc: string; workspace: string } | null> {
    const row = await this.db.prepare('SELECT token_enc, workspace FROM notion_links WHERE uid = ?').bind(uid).first<{ token_enc: string; workspace: string }>();
    return row ? { tokenEnc: row.token_enc, workspace: row.workspace } : null;
  }

  async deleteLink(uid: string): Promise<void> {
    await this.db.prepare('DELETE FROM notion_links WHERE uid = ?').bind(uid).run();
  }
}
