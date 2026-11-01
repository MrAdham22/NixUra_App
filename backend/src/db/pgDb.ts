import pg from 'pg';
import type { Db } from './db.js';

class PgClientDb implements Db {
  constructor(private readonly client: pg.PoolClient) {}
  async query<T>(sql: string, params: unknown[] = []): Promise<T[]> {
    return (await this.client.query(sql, params)).rows as T[];
  }
  async tx<T>(fn: (db: Db) => Promise<T>): Promise<T> {
    return fn(this); // already inside a transaction
  }
  async close(): Promise<void> {}
}

export class PgDb implements Db {
  private readonly pool: pg.Pool;
  constructor(connectionString: string) {
    this.pool = new pg.Pool({ connectionString, max: 10, idleTimeoutMillis: 30_000, statement_timeout: 10_000 });
  }
  async query<T>(sql: string, params: unknown[] = []): Promise<T[]> {
    return (await this.pool.query(sql, params)).rows as T[];
  }
  async tx<T>(fn: (db: Db) => Promise<T>): Promise<T> {
    const client = await this.pool.connect();
    try {
      await client.query('BEGIN');
      const result = await fn(new PgClientDb(client));
      await client.query('COMMIT');
      return result;
    } catch (e) {
      await client.query('ROLLBACK').catch(() => undefined);
      throw e;
    } finally {
      client.release();
    }
  }
  async close(): Promise<void> {
    await this.pool.end();
  }
}
