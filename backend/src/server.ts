import { loadConfig } from './config.js';
import { PgDb } from './db/pgDb.js';
import { createArgon2Hasher } from './auth/crypto.js';
import { createAccessTokenService } from './auth/tokens.js';
import { AuthService } from './auth/authService.js';
import { ConsoleEmailSender, ResendEmailSender } from './email/senders.js';
import { buildApp } from './http/app.js';
import { SyncService } from './sync/syncService.js';

const cfg = loadConfig();
const db = new PgDb(cfg.DATABASE_URL);
const email =
  cfg.EMAIL_PROVIDER === 'resend'
    ? new ResendEmailSender(cfg.RESEND_API_KEY, cfg.EMAIL_FROM, cfg.SUPPORT_EMAIL)
    : new ConsoleEmailSender();

const holder: { log?: { error: (o: unknown, m?: string) => void } } = {};
const auth = new AuthService({
  db,
  cfg,
  hasher: createArgon2Hasher(cfg),
  tokens: createAccessTokenService(cfg),
  email,
  log: { error: (o, m) => holder.log?.error(o, m) },
});

const app = await buildApp({ cfg, auth, sync: new SyncService(db), ping: async () => void (await db.query('SELECT 1')) });
holder.log = app.log;

const shutdown = async (signal: string) => {
  app.log.info({ signal }, 'shutting down');
  await app.close();
  await db.close();
  process.exit(0);
};
process.on('SIGTERM', () => void shutdown('SIGTERM'));
process.on('SIGINT', () => void shutdown('SIGINT'));

await app.listen({ port: cfg.PORT, host: cfg.HOST });
