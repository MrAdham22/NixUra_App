import assert from 'node:assert/strict';
import { after, before, describe, test } from 'node:test';
import { GOOD_PASSWORD, makeCtx, type TestCtx } from './helpers.js';

const E = 'ann@example.com';
let n = 0;
const fresh = () => `user${++n}@example.com`;

describe('registration and email OTP', () => {
  let c: TestCtx;
  before(async () => (c = await makeCtx()));
  after(() => c.close());

  test('register sends a 6-digit code, never returns secrets, user unusable until verified', async () => {
    const r = await c.post('/v1/auth/register', { email: E, password: GOOD_PASSWORD, name: 'Ann' });
    assert.equal(r.status, 202);
    assert.deepEqual(r.body, { status: 'verification_required' });
    assert.match(c.mail.lastCode(E, 'verify_email')!, /^\d{6}$/);
    assert.ok(!JSON.stringify(r.body).includes(GOOD_PASSWORD));
    const login = await c.post('/v1/auth/login', { email: E, password: GOOD_PASSWORD });
    assert.equal(login.status, 403);
    assert.equal(login.body.error.code, 'email_not_verified');
  });

  test('stored OTP is a hash, password is Argon2id', async () => {
    const code = c.mail.lastCode(E, 'verify_email')!;
    const rows = await c.db.query<{ code_hash: string }>('SELECT code_hash FROM otp_codes');
    assert.ok(rows.length > 0 && rows.every((r) => r.code_hash !== code && r.code_hash.length === 64));
    const u = await c.db.query<{ password_hash: string }>('SELECT password_hash FROM users WHERE email=$1', [E]);
    assert.match(u[0]!.password_hash, /^\$argon2id\$/);
  });

  test('wrong code is rejected with a generic error; right code works once', async () => {
    const code = c.mail.lastCode(E, 'verify_email')!;
    const wrong = code === '000000' ? '000001' : '000000';
    const bad = await c.post('/v1/auth/verify-email', { email: E, code: wrong });
    assert.equal(bad.status, 400);
    assert.equal(bad.body.error.code, 'invalid_code');
    const ok = await c.post('/v1/auth/verify-email', { email: E, code, deviceName: 'Pixel', platform: 'android' });
    assert.equal(ok.status, 200);
    assert.equal(ok.body.user.emailVerified, true);
    assert.ok(ok.body.tokens.accessToken && ok.body.tokens.refreshToken);
    const again = await c.post('/v1/auth/verify-email', { email: E, code });
    assert.equal(again.status, 400, 'single use');
  });

  test('registering an existing verified email looks identical and mails a notice, not a code', async () => {
    const before = c.mail.sent.length;
    const r = await c.post('/v1/auth/register', { email: E, password: GOOD_PASSWORD });
    assert.equal(r.status, 202);
    assert.deepEqual(r.body, { status: 'verification_required' });
    const added = c.mail.sent.slice(before);
    assert.deepEqual(added.map((m) => m.kind), ['account_exists']);
    assert.equal(added[0]!.code, undefined);
  });

  test('weak passwords are rejected with reasons', async () => {
    const r = await c.post('/v1/auth/register', { email: fresh(), password: 'Password123' });
    assert.equal(r.status, 422);
    assert.equal(r.body.error.code, 'weak_password');
    assert.ok(r.body.error.problems.includes('too_common'));
    const short = await c.post('/v1/auth/register', { email: fresh(), password: 'abc' });
    assert.ok(short.body.error.problems.includes('too_short'));
  });

  test('invalid input is a 422 with field paths', async () => {
    const r = await c.post('/v1/auth/register', { email: 'not-an-email', password: GOOD_PASSWORD });
    assert.equal(r.status, 422);
    assert.equal(r.body.error.code, 'invalid_input');
    const r2 = await c.post('/v1/auth/verify-email', { email: E, code: '12ab56' });
    assert.equal(r2.status, 422);
  });

  test('email is case/whitespace-insensitive', async () => {
    const r = await c.post('/v1/auth/login', { email: '  ANN@Example.com ', password: GOOD_PASSWORD });
    assert.equal(r.status, 200);
  });
});

describe('OTP expiry, attempts, resend cooldown, daily cap', () => {
  let c: TestCtx;
  before(async () => (c = await makeCtx()));
  after(() => c.close());

  test('expired code fails', async () => {
    const e = fresh();
    await c.post('/v1/auth/register', { email: e, password: GOOD_PASSWORD });
    const code = c.mail.lastCode(e, 'verify_email')!;
    c.advance(10 * 60_000 + 1000);
    const r = await c.post('/v1/auth/verify-email', { email: e, code });
    assert.equal(r.status, 400);
  });

  test('after 5 wrong attempts even the right code is refused', async () => {
    const e = fresh();
    await c.post('/v1/auth/register', { email: e, password: GOOD_PASSWORD });
    const code = c.mail.lastCode(e, 'verify_email')!;
    const wrong = code === '111111' ? '222222' : '111111';
    for (let i = 0; i < 5; i++) assert.equal((await c.post('/v1/auth/verify-email', { email: e, code: wrong }, undefined, `10.1.0.${i}`)).status, 400);
    assert.equal((await c.post('/v1/auth/verify-email', { email: e, code }, undefined, '10.1.1.1')).status, 400);
  });

  test('resend honours the cooldown, then invalidates the old code', async () => {
    const e = fresh();
    await c.post('/v1/auth/register', { email: e, password: GOOD_PASSWORD });
    const first = c.mail.lastCode(e, 'verify_email')!;
    const count = () => c.mail.sent.filter((m) => m.to === e && m.kind === 'verify_email').length;
    assert.equal(count(), 1);
    assert.equal((await c.post('/v1/auth/resend-otp', { email: e })).status, 202);
    assert.equal(count(), 1, 'cooldown: no second mail');
    c.advance(61_000);
    assert.equal((await c.post('/v1/auth/resend-otp', { email: e })).status, 202);
    assert.equal(count(), 2);
    const second = c.mail.lastCode(e, 'verify_email')!;
    if (second !== first) assert.equal((await c.post('/v1/auth/verify-email', { email: e, code: first })).status, 400, 'old code invalidated');
    assert.equal((await c.post('/v1/auth/verify-email', { email: e, code: second })).status, 200);
  });

  test('daily cap stops further codes', async () => {
    const e = fresh();
    await c.post('/v1/auth/register', { email: e, password: GOOD_PASSWORD });
    for (let i = 0; i < 8; i++) {
      c.advance(61_000);
      await c.post('/v1/auth/resend-otp', { email: e }, undefined, `10.2.0.${i}`);
    }
    assert.equal(c.mail.sent.filter((m) => m.to === e && m.kind === 'verify_email').length, c.cfg.OTP_DAILY_CAP);
  });

  test('resend for unknown email answers the same and sends nothing', async () => {
    const before = c.mail.sent.length;
    const r = await c.post('/v1/auth/resend-otp', { email: 'nobody@example.com' });
    assert.equal(r.status, 202);
    assert.equal(c.mail.sent.length, before);
  });

  test('a pending registration is overwritten by the next one (pre-registration takeover is impossible)', async () => {
    const e = fresh();
    await c.post('/v1/auth/register', { email: e, password: 'attacker password 1' });
    await c.post('/v1/auth/register', { email: e, password: 'victim password 22' });
    c.advance(61_000);
    await c.post('/v1/auth/resend-otp', { email: e });
    const code = c.mail.lastCode(e, 'verify_email')!;
    assert.equal((await c.post('/v1/auth/verify-email', { email: e, code })).status, 200);
    assert.equal((await c.post('/v1/auth/login', { email: e, password: 'attacker password 1' })).status, 401);
    assert.equal((await c.post('/v1/auth/login', { email: e, password: 'victim password 22' })).status, 200);
  });
});

describe('login, lockout, enumeration', () => {
  let c: TestCtx;
  before(async () => (c = await makeCtx()));
  after(() => c.close());

  test('wrong password and unknown email give the same answer', async () => {
    const e = fresh();
    await c.signUp(e, GOOD_PASSWORD);
    const a = await c.post('/v1/auth/login', { email: e, password: 'wrong password!!' });
    const b = await c.post('/v1/auth/login', { email: 'ghost@example.com', password: 'wrong password!!' });
    assert.equal(a.status, 401);
    assert.deepEqual(a.body, b.body);
  });

  test('5 failures lock the account; unknown emails lock identically; lock expires', async () => {
    const real = fresh();
    await c.signUp(real, GOOD_PASSWORD);
    const ghost = 'ghost2@example.com';
    for (const email of [real, ghost]) {
      let last: any;
      for (let i = 0; i < 5; i++) last = await c.post('/v1/auth/login', { email, password: 'wrong password!!' }, undefined, `10.3.${email === real ? 1 : 2}.${i}`);
      assert.equal(last.status, 429);
      assert.equal(last.body.error.code, 'account_locked');
      assert.ok(Number(last.headers['retry-after']) > 0);
    }
    // even the correct password is refused while locked
    assert.equal((await c.post('/v1/auth/login', { email: real, password: GOOD_PASSWORD }, undefined, '10.3.9.9')).status, 429);
    c.advance(31_000);
    assert.equal((await c.post('/v1/auth/login', { email: real, password: GOOD_PASSWORD }, undefined, '10.3.9.9')).status, 200);
  });

  test('responses never contain the password or its hash', async () => {
    const e = fresh();
    const t = await c.signUp(e, GOOD_PASSWORD);
    const me = await c.get('/v1/me', t.accessToken);
    const blob = JSON.stringify(me.body);
    assert.ok(!blob.includes(GOOD_PASSWORD) && !blob.includes('argon2') && !blob.includes('password'));
  });

  test('audit log stores no email addresses', async () => {
    const rows = await c.db.query<{ event: string; detail: string }>('SELECT event, detail FROM audit_log');
    assert.ok(rows.length > 0);
    assert.ok(rows.every((r) => !/@/.test(r.event + r.detail)));
  });
});

describe('access tokens, refresh rotation and reuse detection', () => {
  let c: TestCtx;
  before(async () => (c = await makeCtx()));
  after(() => c.close());

  test('protected routes need a valid bearer token', async () => {
    assert.equal((await c.get('/v1/me')).status, 401);
    assert.equal((await c.get('/v1/me', 'garbage')).status, 401);
  });

  test('refresh rotates; replaying the old token revokes the whole session', async () => {
    const t = await c.signUp(fresh(), GOOD_PASSWORD);
    const r1 = await c.post('/v1/auth/refresh', { refreshToken: t.refreshToken });
    assert.equal(r1.status, 200);
    assert.notEqual(r1.body.refreshToken, t.refreshToken);
    assert.equal((await c.get('/v1/me', r1.body.accessToken)).status, 200);
    // attacker (or buggy client) replays the first token
    const replay = await c.post('/v1/auth/refresh', { refreshToken: t.refreshToken });
    assert.equal(replay.status, 401);
    // family is dead: the legitimate newest token and its access token stop working
    assert.equal((await c.post('/v1/auth/refresh', { refreshToken: r1.body.refreshToken })).status, 401);
    assert.equal((await c.get('/v1/me', r1.body.accessToken)).status, 401);
    const ev = await c.db.query<{ event: string }>("SELECT event FROM audit_log WHERE event='refresh_token_reuse'");
    assert.equal(ev.length, 1);
  });

  test('expired access token is refused; expired refresh token is refused', async () => {
    const t = await c.signUp(fresh(), GOOD_PASSWORD);
    c.advance(16 * 60_000);
    assert.equal((await c.get('/v1/me', t.accessToken)).status, 401);
    const r = await c.post('/v1/auth/refresh', { refreshToken: t.refreshToken });
    assert.equal(r.status, 200, 'refresh token outlives the access token');
    c.advance((c.cfg.REFRESH_TOKEN_TTL_DAYS + 1) * 86_400_000);
    assert.equal((await c.post('/v1/auth/refresh', { refreshToken: r.body.refreshToken })).status, 401);
  });

  test('unknown refresh token is refused', async () => {
    assert.equal((await c.post('/v1/auth/refresh', { refreshToken: 'x'.repeat(43) })).status, 401);
  });

  test('refresh tokens are stored hashed', async () => {
    const t = await c.signUp(fresh(), GOOD_PASSWORD);
    const rows = await c.db.query<{ token_hash: string }>('SELECT token_hash FROM refresh_tokens');
    assert.ok(rows.every((r) => r.token_hash !== t.refreshToken && r.token_hash.length === 64));
  });

  test('logout ends only this session; logout-all ends everything', async () => {
    const e = fresh();
    const a = await c.signUp(e, GOOD_PASSWORD);
    const bRes = await c.post('/v1/auth/login', { email: e, password: GOOD_PASSWORD, deviceName: 'Tablet', platform: 'android' });
    const b = bRes.body.tokens;
    assert.equal((await c.post('/v1/auth/logout', {}, a.accessToken)).status, 204);
    const gone = await c.get('/v1/me', a.accessToken);
    assert.equal(gone.status, 401);
    assert.equal(gone.body.error.code, 'session_revoked');
    assert.equal((await c.post('/v1/auth/refresh', { refreshToken: a.refreshToken })).status, 401);
    assert.equal((await c.get('/v1/me', b.accessToken)).status, 200);
    assert.equal((await c.post('/v1/auth/logout-all', {}, b.accessToken)).status, 204);
    assert.equal((await c.get('/v1/me', b.accessToken)).status, 401);
  });

  test('sessions can be listed and revoked, but never another user\'s', async () => {
    const e = fresh();
    const a = await c.signUp(e, GOOD_PASSWORD);
    const other = await c.signUp(fresh(), GOOD_PASSWORD);
    const bRes = await c.post('/v1/auth/login', { email: e, password: GOOD_PASSWORD, deviceName: 'Tablet', platform: 'ios' });
    const list = await c.get('/v1/auth/sessions', a.accessToken);
    assert.equal(list.body.sessions.length, 2);
    assert.equal(list.body.sessions.filter((s: any) => s.current).length, 1);
    assert.equal((await c.del(`/v1/auth/sessions/${other.sessionId}`, a.accessToken)).status, 404);
    assert.equal((await c.get('/v1/me', other.accessToken)).status, 200);
    assert.equal((await c.del(`/v1/auth/sessions/${bRes.body.tokens.sessionId}`, a.accessToken)).status, 204);
    assert.equal((await c.get('/v1/me', bRes.body.tokens.accessToken)).status, 401);
  });
});

describe('forgot / reset / change password', () => {
  let c: TestCtx;
  before(async () => (c = await makeCtx()));
  after(() => c.close());

  test('forgot answers 202 for unknown emails and sends nothing', async () => {
    const before = c.mail.sent.length;
    assert.equal((await c.post('/v1/auth/forgot-password', { email: 'nobody@example.com' })).status, 202);
    assert.equal(c.mail.sent.length, before);
  });

  test('reset with the emailed code sets the password and signs out every device', async () => {
    const e = fresh();
    const t = await c.signUp(e, GOOD_PASSWORD);
    assert.equal((await c.post('/v1/auth/forgot-password', { email: e })).status, 202);
    const code = c.mail.lastCode(e, 'reset_password')!;
    const wrong = code === '123123' ? '321321' : '123123';
    assert.equal((await c.post('/v1/auth/reset-password', { email: e, code: wrong, newPassword: 'brand new passphrase' })).status, 400);
    assert.equal((await c.post('/v1/auth/reset-password', { email: e, code, newPassword: 'Password123' })).status, 422, 'policy applies, code not burned by policy failure');
    assert.equal((await c.post('/v1/auth/reset-password', { email: e, code, newPassword: 'brand new passphrase' })).status, 204);
    assert.equal((await c.post('/v1/auth/reset-password', { email: e, code, newPassword: 'another new passphrase' })).status, 400, 'single use');
    assert.equal((await c.get('/v1/me', t.accessToken)).status, 401);
    assert.equal((await c.post('/v1/auth/login', { email: e, password: GOOD_PASSWORD })).status, 401);
    assert.equal((await c.post('/v1/auth/login', { email: e, password: 'brand new passphrase' })).status, 200);
    assert.ok(c.mail.sent.some((m) => m.to === e && m.kind === 'password_changed'));
  });

  test('a verify code cannot be used to reset a password (purpose binding)', async () => {
    const e = fresh();
    await c.post('/v1/auth/register', { email: e, password: GOOD_PASSWORD });
    const verifyCode = c.mail.lastCode(e, 'verify_email')!;
    assert.equal((await c.post('/v1/auth/reset-password', { email: e, code: verifyCode, newPassword: 'brand new passphrase' })).status, 400);
  });

  test('change password: needs current, enforces policy, keeps this device, signs out others, mails notice', async () => {
    const e = fresh();
    const a = await c.signUp(e, GOOD_PASSWORD);
    const b = (await c.post('/v1/auth/login', { email: e, password: GOOD_PASSWORD, deviceName: 'Tablet' })).body.tokens;
    assert.equal((await c.post('/v1/auth/change-password', { currentPassword: 'wrong wrong wrong', newPassword: 'brand new passphrase' }, a.accessToken)).status, 401);
    assert.equal((await c.post('/v1/auth/change-password', { currentPassword: GOOD_PASSWORD, newPassword: 'short' }, a.accessToken)).status, 422);
    const same = await c.post('/v1/auth/change-password', { currentPassword: GOOD_PASSWORD, newPassword: GOOD_PASSWORD }, a.accessToken);
    assert.equal(same.status, 422);
    assert.equal((await c.post('/v1/auth/change-password', { currentPassword: GOOD_PASSWORD, newPassword: 'brand new passphrase' }, a.accessToken)).status, 204);
    assert.equal((await c.get('/v1/me', a.accessToken)).status, 200, 'current device stays signed in');
    assert.equal((await c.get('/v1/me', b.accessToken)).status, 401, 'other device signed out');
    assert.ok(c.mail.sent.some((m) => m.to === e && m.kind === 'password_changed'));
    assert.equal((await c.post('/v1/auth/login', { email: e, password: 'brand new passphrase' })).status, 200);
  });
});

describe('account deletion and profile', () => {
  let c: TestCtx;
  before(async () => (c = await makeCtx()));
  after(() => c.close());

  test('profile name/age can be read and updated; invalid age rejected', async () => {
    const t = await c.signUp(fresh(), GOOD_PASSWORD);
    const patch = await c.app.inject({ method: 'PATCH', url: '/v1/me', payload: { name: ' Ann ', age: 29 }, headers: { authorization: `Bearer ${t.accessToken}` } });
    assert.equal(patch.statusCode, 200);
    assert.equal(JSON.parse(patch.body).user.name, 'Ann');
    const bad = await c.app.inject({ method: 'PATCH', url: '/v1/me', payload: { age: 3 }, headers: { authorization: `Bearer ${t.accessToken}` } });
    assert.equal(bad.statusCode, 422);
    assert.equal((await c.get('/v1/me', t.accessToken)).body.user.age, 29);
  });

  test('delete needs the password; then every trace of the account is gone', async () => {
    const e = fresh();
    const t = await c.signUp(e, GOOD_PASSWORD);
    assert.equal((await c.post('/v1/auth/delete-account', { password: 'wrong wrong wrong' }, t.accessToken)).status, 401);
    assert.equal((await c.post('/v1/auth/delete-account', { password: GOOD_PASSWORD }, t.accessToken)).status, 204);
    assert.equal((await c.get('/v1/me', t.accessToken)).status, 401);
    assert.equal((await c.post('/v1/auth/login', { email: e, password: GOOD_PASSWORD })).status, 401);
    assert.equal((await c.post('/v1/auth/refresh', { refreshToken: t.refreshToken })).status, 401);
    for (const table of ['users', 'sessions', 'refresh_tokens', 'otp_codes']) {
      const rows = await c.db.query<{ n: string }>(`SELECT count(*)::text AS n FROM ${table} WHERE ${table === 'users' ? 'id' : 'user_id'}=$1`, [t.userId]);
      assert.equal(rows[0]!.n, '0', `${table} cleared`);
    }
    const audit = await c.db.query<{ n: string }>('SELECT count(*)::text AS n FROM audit_log WHERE user_id=$1', [t.userId]);
    assert.equal(audit[0]!.n, '0');
    // the email can be registered again from scratch
    assert.equal((await c.post('/v1/auth/register', { email: e, password: GOOD_PASSWORD })).status, 202);
  });
});

describe('rate limits, headers, breach check, health', () => {
  test('register is limited per IP', async () => {
    const c = await makeCtx();
    let last: any;
    for (let i = 0; i < 11; i++) last = await c.post('/v1/auth/register', { email: fresh(), password: GOOD_PASSWORD }, undefined, '10.9.9.9');
    assert.equal(last.status, 429);
    assert.equal(last.body.error.code, 'rate_limited');
    assert.ok(Number(last.headers['retry-after']) > 0);
    await c.close();
  });

  test('per-email limit on code attempts holds across IPs', async () => {
    const c = await makeCtx();
    const e = fresh();
    await c.post('/v1/auth/register', { email: e, password: GOOD_PASSWORD });
    let last: any;
    for (let i = 0; i < 11; i++) last = await c.post('/v1/auth/verify-email', { email: e, code: '000000' }, undefined, `10.8.0.${i}`);
    assert.equal(last.status, 429);
    await c.close();
  });

  test('security headers present and CORS closed by default', async () => {
    const c = await makeCtx();
    const r = await c.app.inject({ method: 'GET', url: '/health', headers: { origin: 'https://evil.example' } });
    assert.equal(r.statusCode, 200);
    assert.ok(r.headers['x-content-type-options']);
    assert.equal(r.headers['access-control-allow-origin'], undefined);
    await c.close();
  });

  test('breached passwords are rejected when the check is on', async () => {
    const c = await makeCtx({ HIBP_CHECK: 'true' }, async (p) => p === 'leaked leaked leaked');
    const r = await c.post('/v1/auth/register', { email: fresh(), password: 'leaked leaked leaked' });
    assert.equal(r.status, 422);
    assert.ok(r.body.error.problems.includes('breached'));
    assert.equal((await c.post('/v1/auth/register', { email: fresh(), password: GOOD_PASSWORD })).status, 202);
    await c.close();
  });

  test('unknown routes 404 as JSON, oversized bodies refused, OpenAPI served', async () => {
    const c = await makeCtx();
    assert.equal((await c.get('/nope')).status, 404);
    const big = await c.app.inject({ method: 'POST', url: '/v1/auth/login', payload: { email: 'a@b.co', password: 'x'.repeat(20_000) } });
    assert.ok(big.statusCode >= 400 && big.statusCode < 500);
    const doc = await c.get('/openapi.json');
    assert.equal(doc.body.openapi, '3.1.0');
    assert.ok(doc.body.paths['/v1/auth/login']);
    await c.close();
  });
});
