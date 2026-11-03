import { fileURLToPath } from 'node:url';
import { PGlite } from '@electric-sql/pglite';
import type { FastifyInstance } from 'fastify';
import { loadConfig, type Config } from '../src/config.js';
import type { Db } from '../src/db/db.js';
import { migrate } from '../src/db/migrate.js';
import { createArgon2Hasher } from '../src/auth/crypto.js';
import { createAccessTokenService } from '../src/auth/tokens.js';
import { AuthService } from '../src/auth/authService.js';
import { CapturingEmailSender } from '../src/email/senders.js';
import { buildApp } from '../src/http/app.js';
import { MemoryRateLimiter } from '../src/http/rateLimiter.js';
import { SyncService } from '../src/sync/syncService.js';
import { NotionService } from '../src/integrations/notionService.js';
import { TokenCipher } from '../src/integrations/tokenCipher.js';

/** Adapts PGlite (real Postgres compiled to WASM) to the app's [Db] port. */
export class PgliteDb implements Db {
  constructor(private readonly pg: PGlite | { query: PGlite['query']; exec: PGlite['exec'] }) {}
  async query<T>(sql: string, params?: unknown[]): Promise<T[]> {
    if (params === undefined) {
      const results = await this.pg.exec(sql);
      return (results[results.length - 1]?.rows ?? []) as T[];
    }
    return (await this.pg.query<T>(sql, params)).rows;
  }
  async tx<T>(fn: (db: Db) => Promise<T>): Promise<T> {
    if (!(this.pg instanceof PGlite)) return fn(this);
    return this.pg.transaction(async (t) => fn(new PgliteDb(t as never)));
  }
  async close(): Promise<void> {
    if (this.pg instanceof PGlite) await this.pg.close();
  }
}

export const testEnv = {
  NODE_ENV: 'test',
  DATABASE_URL: 'postgres://unused',
  JWT_SECRET: 'test-jwt-secret-test-jwt-secret-test-jwt-secret',
  OTP_PEPPER: 'test-otp-pepper-test-otp-pepper-test-otp-pepper',
  ARGON2_MEMORY_KIB: '8192', // minimum allowed; keeps tests fast, still real Argon2id
  ARGON2_TIME_COST: '2', // argon2 rejects anything lower
  OTP_RESEND_COOLDOWN_SECONDS: '60',
} satisfies Record<string, string>;

export interface TestCtx {
  app: FastifyInstance;
  db: PgliteDb;
  mail: CapturingEmailSender;
  cfg: Config;
  advance(ms: number): void;
  now(): Date;
  close(): Promise<void>;
  post(url: string, body?: unknown, token?: string, ip?: string): Promise<Res>;
  get(url: string, token?: string): Promise<Res>;
  del(url: string, token?: string): Promise<Res>;
  /** register + verify, returns tokens */
  signUp(email: string, password: string): Promise<{ accessToken: string; refreshToken: string; sessionId: string; userId: string }>;
}

export interface Res {
  status: number;
  body: any;
  headers: Record<string, unknown>;
}

/** 32 zero bytes, base64: a test-only token key. */
export const TEST_TOKEN_KEY = Buffer.alloc(32, 7).toString('base64');

export async function makeCtx(
  env: Record<string, string> = {},
  breachCheck?: (p: string) => Promise<boolean>,
  notionFetch?: typeof fetch,
): Promise<TestCtx> {
  const cfg = loadConfig({ ...testEnv, ...env });
  const pg = new PGlite();
  const db = new PgliteDb(pg);
  await migrate(db, fileURLToPath(new URL('../migrations', import.meta.url)));
  let t = new Date('2030-01-01T12:00:00Z').getTime();
  const clock = () => new Date(t);
  const mail = new CapturingEmailSender();
  const auth = new AuthService({ db, cfg, hasher: createArgon2Hasher(cfg), tokens: createAccessTokenService(cfg), email: mail, clock, breachCheck });
  const notion = new NotionService(
    db,
    { clientId: 'cid', clientSecret: 'csecret', redirectUri: 'https://api.test/v1/integrations/notion/callback', appReturnUrl: 'nixura://integrations/notion' },
    new TokenCipher(TEST_TOKEN_KEY),
    notionFetch ?? (async () => new Response('{}', { status: 500 })),
    clock,
    async () => {},
  );
  const app = await buildApp({ cfg, auth, sync: new SyncService(db, clock), notion, limiter: new MemoryRateLimiter(() => t), ping: async () => void (await db.query('SELECT 1')) });

  const send = async (method: 'GET' | 'POST' | 'DELETE', url: string, body?: unknown, token?: string, ip = '10.0.0.1'): Promise<Res> => {
    const r = await app.inject({
      method,
      url,
      payload: body as object | undefined,
      remoteAddress: ip,
      headers: token ? { authorization: `Bearer ${token}` } : {},
    });
    return { status: r.statusCode, body: r.body ? safeJson(r.body) : undefined, headers: r.headers };
  };
  const ctx: TestCtx = {
    app, db, mail, cfg,
    advance: (ms) => void (t += ms),
    now: () => new Date(t),
    close: async () => {
      await app.close();
      await db.close();
    },
    post: (u, b, tok, ip) => send('POST', u, b ?? {}, tok, ip),
    get: (u, tok) => send('GET', u, undefined, tok),
    del: (u, tok) => send('DELETE', u, undefined, tok),
    async signUp(email, password) {
      const reg = await ctx.post('/v1/auth/register', { email, password });
      if (reg.status !== 202) throw new Error(`register failed: ${reg.status} ${JSON.stringify(reg.body)}`);
      const code = mail.lastCode(email, 'verify_email')!;
      const v = await ctx.post('/v1/auth/verify-email', { email, code, platform: 'android', deviceName: 'Test phone' });
      if (v.status !== 200) throw new Error(`verify failed: ${v.status} ${JSON.stringify(v.body)}`);
      return { ...v.body.tokens, userId: v.body.user.id };
    },
  };
  return ctx;
}

function safeJson(s: string): unknown {
  try {
    return JSON.parse(s);
  } catch {
    return s;
  }
}

export const GOOD_PASSWORD = 'correct horse battery staple';
