/** Minimal database port so production (pg) and tests (PGlite) share the same repositories. */
export interface Db {
  query<T = Record<string, unknown>>(sql: string, params?: unknown[]): Promise<T[]>;
  /** Runs [fn] inside a transaction; rolls back on throw. */
  tx<T>(fn: (db: Db) => Promise<T>): Promise<T>;
  close(): Promise<void>;
}
