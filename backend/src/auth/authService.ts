import { randomUUID } from 'node:crypto';
import type { Config } from '../config.js';
import type { Db } from '../db/db.js';
import type { EmailSender } from '../email/emailSender.js';
import { AppError, Errors } from '../errors.js';
import { generateOtp, hashOtp, newOpaqueToken, safeEqualHex, sha256Hex, type PasswordHasher } from './crypto.js';
import { checkPasswordPolicy, isBreached } from './passwordPolicy.js';
import type { AccessTokenService } from './tokens.js';

export type OtpPurpose = 'verify_email' | 'reset_password';

export interface DeviceInfo {
  deviceName?: string;
  platform?: string;
}

export interface TokenPair {
  accessToken: string;
  accessTokenExpiresInSeconds: number;
  refreshToken: string;
  sessionId: string;
}

export interface PublicUser {
  id: string;
  email: string;
  name: string;
  age: number | null;
  emailVerified: boolean;
}

export interface SessionInfo {
  id: string;
  deviceName: string;
  platform: string;
  createdAt: string;
  lastUsedAt: string;
  current: boolean;
}

export interface AuthDeps {
  db: Db;
  cfg: Config;
  hasher: PasswordHasher;
  tokens: AccessTokenService;
  email: EmailSender;
  clock?: () => Date;
  /** Injected for tests; defaults to the HIBP range API when cfg.HIBP_CHECK is on. */
  breachCheck?: (password: string) => Promise<boolean>;
  log?: { error: (obj: unknown, msg?: string) => void };
}

interface UserRow {
  id: string;
  email: string;
  email_verified: boolean;
  password_hash: string;
  name: string;
  age: number | null;
}

type OtpOutcome = { ok: true } | { ok: false };

const MAX_NAME = 60;
const FAILURE_FREE = 5; // failed logins allowed before the first lock
const MAX_LOCK_SECONDS = 15 * 60;

export function normalizeEmail(raw: string): string {
  return raw.normalize('NFKC').trim().toLowerCase();
}

export class AuthService {
  private readonly db: Db;
  private readonly cfg: Config;
  private readonly now: () => Date;
  /** A real Argon2id hash of a random string; verified against when the email is unknown so timing matches. */
  private dummyHash: Promise<string> | undefined;

  constructor(private readonly deps: AuthDeps) {
    this.db = deps.db;
    this.cfg = deps.cfg;
    this.now = deps.clock ?? (() => new Date());
  }

  // ---------------------------------------------------------------- registration

  /**
   * Always resolves the same way for new and existing emails (anti-enumeration). A pending (unverified)
   * registration is overwritten by a newer one: only the mailbox owner can verify, so whoever verifies
   * is the owner, and an attacker who pre-registered a victim's address cannot keep their password.
   */
  async register(rawEmail: string, password: string, name: string | undefined): Promise<void> {
    const email = normalizeEmail(rawEmail);
    await this.assertPasswordAcceptable(password, email);
    const cleanName = (name ?? '').trim().slice(0, MAX_NAME);
    const hash = await this.deps.hasher.hash(password); // always hashed, so timing does not leak existence

    const existing = await this.findByEmail(email);
    if (existing?.email_verified) {
      this.fireAndForget(this.deps.email.send({ to: email, kind: 'account_exists' }));
      await this.audit(existing.id, 'register_existing_email');
      return;
    }
    let userId: string;
    if (existing) {
      userId = existing.id;
      await this.db.query('UPDATE users SET password_hash=$2, name=$3, updated_at=$4 WHERE id=$1', [userId, hash, cleanName, this.now()]);
    } else {
      userId = randomUUID();
      try {
        await this.db.query('INSERT INTO users (id,email,password_hash,name,created_at,updated_at) VALUES ($1,$2,$3,$4,$5,$5)', [
          userId, email, hash, cleanName, this.now(),
        ]);
      } catch (e) {
        if (isUniqueViolation(e)) return; // concurrent registration of the same email: the other request sends the code
        throw e;
      }
      await this.audit(userId, 'register');
    }
    await this.issueOtp(userId, email, 'verify_email');
  }

  async verifyEmail(rawEmail: string, code: string, device: DeviceInfo): Promise<{ tokens: TokenPair; user: PublicUser }> {
    const email = normalizeEmail(rawEmail);
    const user = await this.findByEmail(email);
    if (!user) {
      await this.deps.hasher.verify(await this.getDummyHash(), 'x'); // equalize timing
      throw Errors.invalidCode();
    }
    const outcome = await this.consumeOtp(user.id, 'verify_email', code);
    if (!outcome.ok) throw Errors.invalidCode();
    await this.db.query('UPDATE users SET email_verified=TRUE, updated_at=$2 WHERE id=$1', [user.id, this.now()]);
    await this.audit(user.id, 'email_verified');
    const tokens = await this.createSession(user.id, device);
    return { tokens, user: toPublic({ ...user, email_verified: true }) };
  }

  /** Always resolves (anti-enumeration). Sends a code only to an existing unverified account. */
  async resendVerification(rawEmail: string): Promise<void> {
    const email = normalizeEmail(rawEmail);
    const user = await this.findByEmail(email);
    if (user && !user.email_verified) await this.issueOtp(user.id, email, 'verify_email');
  }

  // ---------------------------------------------------------------- login & sessions

  async login(rawEmail: string, password: string, device: DeviceInfo): Promise<{ tokens: TokenPair; user: PublicUser }> {
    const email = normalizeEmail(rawEmail);
    const key = sha256Hex(email);
    const retry = await this.lockRemainingSeconds(key);
    if (retry > 0) throw Errors.locked(retry);

    const user = await this.findByEmail(email);
    const hash = user?.password_hash ?? (await this.getDummyHash());
    const valid = await this.deps.hasher.verify(hash, password);
    if (!user || !valid) {
      const lock = await this.recordFailure(key);
      if (user) await this.audit(user.id, 'login_failed');
      if (lock > 0) throw Errors.locked(lock);
      throw Errors.invalidCredentials();
    }
    await this.db.query('DELETE FROM login_attempts WHERE key=$1', [key]);

    if (!user.email_verified) {
      await this.issueOtp(user.id, email, 'verify_email'); // let them finish verification
      throw Errors.notVerified();
    }
    if (this.deps.hasher.needsRehash(user.password_hash)) {
      await this.db.query('UPDATE users SET password_hash=$2 WHERE id=$1', [user.id, await this.deps.hasher.hash(password)]);
    }
    await this.audit(user.id, 'login');
    return { tokens: await this.createSession(user.id, device), user: toPublic(user) };
  }

  /**
   * Rotating refresh tokens. Presenting an already-rotated token means it leaked (or the client replayed it):
   * the whole session (token family) is revoked and an audit event is written.
   */
  async refresh(refreshToken: string): Promise<TokenPair> {
    const hash = sha256Hex(refreshToken);
    type Outcome = { kind: 'ok'; userId: string; sessionId: string; token: string } | { kind: 'fail'; reuseOf?: string };
    const outcome: Outcome = await this.db.tx(async (t) => {
      const rows = await t.query<{ id: string; session_id: string; user_id: string; expires_at: Date; rotated_at: Date | null; revoked_at: Date | null }>(
        `SELECT r.id, r.session_id, r.user_id, r.expires_at, r.rotated_at, s.revoked_at
           FROM refresh_tokens r JOIN sessions s ON s.id = r.session_id
          WHERE r.token_hash = $1 FOR UPDATE OF r`,
        [hash],
      );
      const row = rows[0];
      if (!row) return { kind: 'fail' };
      const now = this.now();
      if (row.revoked_at) return { kind: 'fail' };
      if (row.rotated_at) {
        await t.query('UPDATE sessions SET revoked_at=$2 WHERE id=$1 AND revoked_at IS NULL', [row.session_id, now]);
        await t.query('DELETE FROM refresh_tokens WHERE session_id=$1', [row.session_id]);
        return { kind: 'fail', reuseOf: row.user_id };
      }
      if (new Date(row.expires_at) <= now) return { kind: 'fail' };
      const next = newOpaqueToken();
      await t.query('UPDATE refresh_tokens SET rotated_at=$2 WHERE id=$1', [row.id, now]);
      await t.query('INSERT INTO refresh_tokens (id,session_id,user_id,token_hash,expires_at,created_at) VALUES ($1,$2,$3,$4,$5,$6)', [
        randomUUID(), row.session_id, row.user_id, sha256Hex(next), this.refreshExpiry(), now,
      ]);
      await t.query('UPDATE sessions SET last_used_at=$2 WHERE id=$1', [row.session_id, now]);
      return { kind: 'ok', userId: row.user_id, sessionId: row.session_id, token: next };
    });
    if (outcome.kind === 'fail') {
      if (outcome.reuseOf) await this.audit(outcome.reuseOf, 'refresh_token_reuse', 'session revoked');
      throw Errors.unauthorized();
    }
    const access = await this.deps.tokens.issue({ userId: outcome.userId, sessionId: outcome.sessionId }, this.now());
    return {
      accessToken: access.token,
      accessTokenExpiresInSeconds: access.expiresInSeconds,
      refreshToken: outcome.token,
      sessionId: outcome.sessionId,
    };
  }

  /** Validates a bearer token AND that its session is still alive (so logout/revoke is immediate). */
  async authenticate(accessToken: string): Promise<{ userId: string; sessionId: string }> {
    const claims = await this.deps.tokens.verify(accessToken, this.now());
    if (!claims) throw Errors.unauthorized();
    const rows = await this.db.query('SELECT 1 FROM sessions WHERE id=$1 AND user_id=$2 AND revoked_at IS NULL', [claims.sessionId, claims.userId]);
    if (rows.length === 0) throw Errors.sessionRevoked();
    return claims;
  }

  async logout(userId: string, sessionId: string): Promise<void> {
    await this.revokeSession(userId, sessionId);
    await this.audit(userId, 'logout');
  }

  async logoutAll(userId: string): Promise<void> {
    await this.revokeAllSessions(userId, undefined);
    await this.audit(userId, 'logout_all');
  }

  async listSessions(userId: string, currentSessionId: string): Promise<SessionInfo[]> {
    const rows = await this.db.query<{ id: string; device_name: string; platform: string; created_at: Date; last_used_at: Date }>(
      'SELECT id, device_name, platform, created_at, last_used_at FROM sessions WHERE user_id=$1 AND revoked_at IS NULL ORDER BY last_used_at DESC',
      [userId],
    );
    return rows.map((r) => ({
      id: r.id,
      deviceName: r.device_name,
      platform: r.platform,
      createdAt: new Date(r.created_at).toISOString(),
      lastUsedAt: new Date(r.last_used_at).toISOString(),
      current: r.id === currentSessionId,
    }));
  }

  async revokeSessionById(userId: string, sessionId: string): Promise<void> {
    const rows = await this.db.query('SELECT 1 FROM sessions WHERE id=$1 AND user_id=$2 AND revoked_at IS NULL', [sessionId, userId]);
    if (rows.length === 0) throw new AppError('not_found', 404, 'Session not found.');
    await this.revokeSession(userId, sessionId);
    await this.audit(userId, 'session_revoked');
  }

  // ---------------------------------------------------------------- password flows

  /** Always resolves (anti-enumeration). Only a verified account receives a reset code. */
  async forgotPassword(rawEmail: string): Promise<void> {
    const email = normalizeEmail(rawEmail);
    const user = await this.findByEmail(email);
    if (user?.email_verified) await this.issueOtp(user.id, email, 'reset_password');
  }

  async resetPassword(rawEmail: string, code: string, newPassword: string): Promise<void> {
    const email = normalizeEmail(rawEmail);
    await this.assertPasswordAcceptable(newPassword, email);
    const user = await this.findByEmail(email);
    if (!user) {
      await this.deps.hasher.verify(await this.getDummyHash(), 'x');
      throw Errors.invalidCode();
    }
    const outcome = await this.consumeOtp(user.id, 'reset_password', code);
    if (!outcome.ok) throw Errors.invalidCode();
    const hash = await this.deps.hasher.hash(newPassword);
    await this.db.query('UPDATE users SET password_hash=$2, email_verified=TRUE, updated_at=$3 WHERE id=$1', [user.id, hash, this.now()]);
    await this.db.query('DELETE FROM login_attempts WHERE key=$1', [sha256Hex(email)]);
    await this.revokeAllSessions(user.id, undefined);
    await this.audit(user.id, 'password_reset');
    this.fireAndForget(this.deps.email.send({ to: email, kind: 'password_changed' }));
  }

  async changePassword(userId: string, currentSessionId: string, currentPassword: string, newPassword: string): Promise<void> {
    const user = await this.requireUser(userId);
    const key = sha256Hex(user.email);
    const retry = await this.lockRemainingSeconds(key);
    if (retry > 0) throw Errors.locked(retry);
    if (!(await this.deps.hasher.verify(user.password_hash, currentPassword))) {
      const lock = await this.recordFailure(key);
      await this.audit(userId, 'change_password_failed');
      throw lock > 0 ? Errors.locked(lock) : Errors.invalidCredentials();
    }
    await this.assertPasswordAcceptable(newPassword, user.email);
    if (await this.deps.hasher.verify(user.password_hash, newPassword)) {
      throw Errors.weakPassword(['same_as_current']);
    }
    const hash = await this.deps.hasher.hash(newPassword);
    await this.db.query('UPDATE users SET password_hash=$2, updated_at=$3 WHERE id=$1', [userId, hash, this.now()]);
    await this.db.query('DELETE FROM login_attempts WHERE key=$1', [key]);
    await this.revokeAllSessions(userId, currentSessionId); // keep this device signed in
    await this.audit(userId, 'password_changed');
    this.fireAndForget(this.deps.email.send({ to: user.email, kind: 'password_changed' }));
  }

  /** Permanently deletes the account and every row that belongs to it (Google Play account-deletion requirement). */
  async deleteAccount(userId: string, password: string): Promise<void> {
    const user = await this.requireUser(userId);
    const key = sha256Hex(user.email);
    const retry = await this.lockRemainingSeconds(key);
    if (retry > 0) throw Errors.locked(retry);
    if (!(await this.deps.hasher.verify(user.password_hash, password))) {
      const lock = await this.recordFailure(key);
      await this.audit(userId, 'delete_account_failed');
      throw lock > 0 ? Errors.locked(lock) : Errors.invalidCredentials();
    }
    await this.db.tx(async (t) => {
      // Cascades remove sessions, refresh tokens and OTP codes. Audit rows about this user are erased too,
      // then one anonymous event records that a deletion happened.
      await t.query('DELETE FROM audit_log WHERE user_id=$1', [userId]);
      await t.query('DELETE FROM login_attempts WHERE key=$1', [key]);
      await t.query('DELETE FROM users WHERE id=$1', [userId]);
      await t.query("INSERT INTO audit_log (user_id, event, created_at) VALUES (NULL, 'account_deleted', $1)", [this.now()]);
    });
  }

  // ---------------------------------------------------------------- profile

  async getMe(userId: string): Promise<PublicUser> {
    return toPublic(await this.requireUser(userId));
  }

  async updateMe(userId: string, patch: { name?: string; age?: number | null }): Promise<PublicUser> {
    const user = await this.requireUser(userId);
    const name = patch.name !== undefined ? patch.name.trim().slice(0, MAX_NAME) : user.name;
    const age = patch.age !== undefined ? patch.age : user.age;
    await this.db.query('UPDATE users SET name=$2, age=$3, updated_at=$4 WHERE id=$1', [userId, name, age, this.now()]);
    return toPublic({ ...user, name, age });
  }

  // ---------------------------------------------------------------- internals

  private async assertPasswordAcceptable(password: string, email: string): Promise<void> {
    const problems: string[] = checkPasswordPolicy(password, email);
    if (problems.length === 0 && this.cfg.HIBP_CHECK) {
      const breached = await (this.deps.breachCheck ?? ((p: string) => isBreached(p)))(password);
      if (breached) problems.push('breached');
    }
    if (problems.length > 0) throw Errors.weakPassword(problems);
  }

  private async findByEmail(email: string): Promise<UserRow | undefined> {
    const rows = await this.db.query<UserRow>(
      'SELECT id, email, email_verified, password_hash, name, age FROM users WHERE lower(email)=$1',
      [email],
    );
    return rows[0];
  }

  private async requireUser(userId: string): Promise<UserRow> {
    const rows = await this.db.query<UserRow>('SELECT id, email, email_verified, password_hash, name, age FROM users WHERE id=$1', [userId]);
    if (!rows[0]) throw Errors.unauthorized();
    return rows[0];
  }

  private getDummyHash(): Promise<string> {
    this.dummyHash ??= this.deps.hasher.hash(newOpaqueToken());
    return this.dummyHash;
  }

  private refreshExpiry(): Date {
    return new Date(this.now().getTime() + this.cfg.REFRESH_TOKEN_TTL_DAYS * 86_400_000);
  }

  private async createSession(userId: string, device: DeviceInfo): Promise<TokenPair> {
    const sessionId = randomUUID();
    const refresh = newOpaqueToken();
    const now = this.now();
    await this.db.tx(async (t) => {
      await t.query('INSERT INTO sessions (id,user_id,device_name,platform,created_at,last_used_at) VALUES ($1,$2,$3,$4,$5,$5)', [
        sessionId, userId, (device.deviceName ?? '').slice(0, 80), (device.platform ?? '').slice(0, 20), now,
      ]);
      await t.query('INSERT INTO refresh_tokens (id,session_id,user_id,token_hash,expires_at,created_at) VALUES ($1,$2,$3,$4,$5,$6)', [
        randomUUID(), sessionId, userId, sha256Hex(refresh), this.refreshExpiry(), now,
      ]);
    });
    const access = await this.deps.tokens.issue({ userId, sessionId }, now);
    return { accessToken: access.token, accessTokenExpiresInSeconds: access.expiresInSeconds, refreshToken: refresh, sessionId };
  }

  private async revokeSession(userId: string, sessionId: string): Promise<void> {
    await this.db.tx(async (t) => {
      await t.query('UPDATE sessions SET revoked_at=$3 WHERE id=$1 AND user_id=$2 AND revoked_at IS NULL', [sessionId, userId, this.now()]);
      await t.query('DELETE FROM refresh_tokens WHERE session_id=$1', [sessionId]);
    });
  }

  private async revokeAllSessions(userId: string, exceptSessionId: string | undefined): Promise<void> {
    const except = exceptSessionId ?? '00000000-0000-0000-0000-000000000000';
    await this.db.tx(async (t) => {
      await t.query('UPDATE sessions SET revoked_at=$3 WHERE user_id=$1 AND id<>$2 AND revoked_at IS NULL', [userId, except, this.now()]);
      await t.query('DELETE FROM refresh_tokens WHERE user_id=$1 AND session_id<>$2', [userId, except]);
    });
  }

  /**
   * Creates and emails a new code unless a resend cooldown or the daily cap applies (then it silently does nothing,
   * so callers can stay constant-shape). Any previous unused code is invalidated.
   */
  private async issueOtp(userId: string, email: string, purpose: OtpPurpose): Promise<void> {
    const now = this.now();
    const code = generateOtp();
    const issued = await this.db.tx(async (t) => {
      const last = await t.query<{ created_at: Date }>(
        'SELECT created_at FROM otp_codes WHERE user_id=$1 AND purpose=$2 ORDER BY created_at DESC LIMIT 1',
        [userId, purpose],
      );
      if (last[0] && now.getTime() - new Date(last[0].created_at).getTime() < this.cfg.OTP_RESEND_COOLDOWN_SECONDS * 1000) return false;
      const dayAgo = new Date(now.getTime() - 86_400_000);
      const count = await t.query<{ n: string }>('SELECT count(*)::text AS n FROM otp_codes WHERE user_id=$1 AND purpose=$2 AND created_at > $3', [userId, purpose, dayAgo]);
      if (Number(count[0]?.n ?? 0) >= this.cfg.OTP_DAILY_CAP) return false;
      await t.query('UPDATE otp_codes SET consumed_at=$3 WHERE user_id=$1 AND purpose=$2 AND consumed_at IS NULL', [userId, purpose, now]);
      await t.query('INSERT INTO otp_codes (id,user_id,purpose,code_hash,expires_at,created_at) VALUES ($1,$2,$3,$4,$5,$6)', [
        randomUUID(), userId, purpose, hashOtp(this.cfg.OTP_PEPPER, userId, purpose, code),
        new Date(now.getTime() + this.cfg.OTP_TTL_SECONDS * 1000), now,
      ]);
      return true;
    });
    if (!issued) return;
    this.fireAndForget(this.deps.email.send({ to: email, kind: purpose, code, ttlMinutes: Math.round(this.cfg.OTP_TTL_SECONDS / 60) }));
  }

  /**
   * Checks a code against the newest unused one. The attempt counter is committed even when the code is wrong
   * (the result is returned, not thrown, so the transaction is not rolled back). Single use; constant-time compare.
   */
  private async consumeOtp(userId: string, purpose: OtpPurpose, code: string): Promise<OtpOutcome> {
    const now = this.now();
    return this.db.tx(async (t) => {
      const rows = await t.query<{ id: string; code_hash: string; attempts: number; expires_at: Date }>(
        `SELECT id, code_hash, attempts, expires_at FROM otp_codes
          WHERE user_id=$1 AND purpose=$2 AND consumed_at IS NULL
          ORDER BY created_at DESC LIMIT 1 FOR UPDATE`,
        [userId, purpose],
      );
      const row = rows[0];
      if (!row || new Date(row.expires_at) <= now || row.attempts >= this.cfg.OTP_MAX_ATTEMPTS) return { ok: false } as const;
      await t.query('UPDATE otp_codes SET attempts = attempts + 1 WHERE id=$1', [row.id]);
      const candidate = hashOtp(this.cfg.OTP_PEPPER, userId, purpose, code);
      if (!safeEqualHex(candidate, row.code_hash)) return { ok: false } as const;
      await t.query('UPDATE otp_codes SET consumed_at=$2 WHERE id=$1', [row.id, now]);
      return { ok: true } as const;
    });
  }

  private async lockRemainingSeconds(key: string): Promise<number> {
    const rows = await this.db.query<{ locked_until: Date | null }>('SELECT locked_until FROM login_attempts WHERE key=$1', [key]);
    const until = rows[0]?.locked_until;
    if (!until) return 0;
    const ms = new Date(until).getTime() - this.now().getTime();
    return ms > 0 ? Math.ceil(ms / 1000) : 0;
  }

  /** Records a failure; from the 5th on the account locks for 30 s doubling up to 15 min. Returns lock seconds (0 = none). */
  private async recordFailure(key: string): Promise<number> {
    const now = this.now();
    const rows = await this.db.query<{ failures: number }>(
      `INSERT INTO login_attempts (key, failures, updated_at) VALUES ($1, 1, $2)
       ON CONFLICT (key) DO UPDATE SET failures = login_attempts.failures + 1, updated_at = $2
       RETURNING failures`,
      [key, now],
    );
    const failures = rows[0]?.failures ?? 1;
    if (failures < FAILURE_FREE) return 0;
    const seconds = Math.min(MAX_LOCK_SECONDS, 30 * 2 ** (failures - FAILURE_FREE));
    await this.db.query('UPDATE login_attempts SET locked_until=$2 WHERE key=$1', [key, new Date(now.getTime() + seconds * 1000)]);
    return seconds;
  }

  private async audit(userId: string | null, event: string, detail = ''): Promise<void> {
    await this.db.query('INSERT INTO audit_log (user_id, event, detail, created_at) VALUES ($1,$2,$3,$4)', [userId, event, detail, this.now()]);
  }

  /** Email delivery must not change response timing/shape; failures are logged without addresses or codes. */
  private fireAndForget(p: Promise<void>): void {
    p.catch((e) => this.deps.log?.error({ err: e instanceof Error ? e.message : 'unknown' }, 'email delivery failed'));
  }
}

function toPublic(u: Pick<UserRow, 'id' | 'email' | 'name' | 'age' | 'email_verified'>): PublicUser {
  return { id: u.id, email: u.email, name: u.name, age: u.age, emailVerified: u.email_verified };
}

function isUniqueViolation(e: unknown): boolean {
  return typeof e === 'object' && e !== null && (e as { code?: string }).code === '23505';
}
