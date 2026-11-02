import assert from 'node:assert/strict';
import { test } from 'node:test';
import { loadConfig } from '../src/config.js';
import { checkPasswordPolicy, isBreached } from '../src/auth/passwordPolicy.js';
import { createArgon2Hasher, generateOtp, hashOtp, safeEqualHex, sha256Hex } from '../src/auth/crypto.js';
import { createAccessTokenService } from '../src/auth/tokens.js';
import { MemoryRateLimiter } from '../src/http/rateLimiter.js';
import { render } from '../src/email/templates.js';
import { testEnv } from './helpers.js';

test('password policy: length, common, same-as-email, variety', () => {
  assert.deepEqual(checkPasswordPolicy('correct horse battery staple'), []);
  assert.ok(checkPasswordPolicy('short1').includes('too_short'));
  assert.ok(checkPasswordPolicy('Password123').includes('too_common'));
  assert.ok(checkPasswordPolicy('a'.repeat(129)).includes('too_long'));
  assert.ok(checkPasswordPolicy('adham.elgendy@x.com', 'adham.elgendy@x.com').includes('same_as_email'));
  assert.ok(checkPasswordPolicy('aaaaaaaaaaaa').includes('low_variety'));
  assert.ok(checkPasswordPolicy('ababababab').includes('low_variety'));
  // 10 emoji count as 10 characters, not 20 UTF-16 units
  assert.ok(!checkPasswordPolicy('😀😁😂🤣😃😄😅😆😉😊').includes('too_short'));
});

test('breached check sends only a 5-char hash prefix and finds the suffix', async () => {
  const sha1 = (await import('node:crypto')).createHash('sha1').update('hunter2hunter2').digest('hex').toUpperCase();
  let requested = '';
  const fake: typeof fetch = async (input) => {
    requested = String(input);
    return new Response(`${sha1.slice(5)}:42\nABCDEF0123456789ABCDEF0123456789ABC:1\n`, { status: 200 });
  };
  assert.equal(await isBreached('hunter2hunter2', fake), true);
  assert.equal(requested, `https://api.pwnedpasswords.com/range/${sha1.slice(0, 5)}`);
  assert.equal(await isBreached('definitely-not-listed-xyz', async () => new Response('0000000000000000000000000000000000A:3', { status: 200 })), false);
  assert.equal(await isBreached('x', async () => { throw new Error('offline'); }), false, 'fails open');
});

test('OTP: always 6 digits, hash is bound to user and purpose, constant-time compare', () => {
  for (let i = 0; i < 500; i++) assert.match(generateOtp(), /^\d{6}$/);
  const a = hashOtp('pepper', 'u1', 'verify_email', '123456');
  assert.notEqual(a, hashOtp('pepper', 'u2', 'verify_email', '123456'));
  assert.notEqual(a, hashOtp('pepper', 'u1', 'reset_password', '123456'));
  assert.notEqual(a, hashOtp('other', 'u1', 'verify_email', '123456'));
  assert.ok(!a.includes('123456'));
  assert.equal(safeEqualHex(a, a), true);
  assert.equal(safeEqualHex(a, hashOtp('pepper', 'u1', 'verify_email', '123457')), false);
  assert.equal(safeEqualHex(a, 'abcd'), false);
  assert.equal(safeEqualHex('', ''), false);
});

test('Argon2id hasher: PHC string, verify, rehash when parameters tighten', async () => {
  const weak = createArgon2Hasher({ ARGON2_MEMORY_KIB: 8192, ARGON2_TIME_COST: 2, ARGON2_PARALLELISM: 1 });
  const h = await weak.hash('s3cret-pass-phrase');
  assert.match(h, /^\$argon2id\$/);
  assert.ok(!h.includes('s3cret'));
  assert.equal(await weak.verify(h, 's3cret-pass-phrase'), true);
  assert.equal(await weak.verify(h, 'wrong'), false);
  assert.equal(await weak.verify('not-a-hash', 'x'), false);
  assert.equal(weak.needsRehash(h), false);
  const strong = createArgon2Hasher({ ARGON2_MEMORY_KIB: 16384, ARGON2_TIME_COST: 2, ARGON2_PARALLELISM: 1 });
  assert.equal(strong.needsRehash(h), true);
});

test('access tokens: round trip, expiry, tampering, wrong secret', async () => {
  const cfg = { JWT_SECRET: testEnv.JWT_SECRET, ACCESS_TOKEN_TTL_SECONDS: 900 };
  const svc = createAccessTokenService(cfg);
  const now = new Date('2030-01-01T00:00:00Z');
  const { token, expiresInSeconds } = await svc.issue({ userId: 'u', sessionId: 's' }, now);
  assert.equal(expiresInSeconds, 900);
  assert.deepEqual(await svc.verify(token, now), { userId: 'u', sessionId: 's' });
  assert.equal(await svc.verify(token, new Date(now.getTime() + 901_000)), null);
  assert.equal(await svc.verify(token.slice(0, -2) + 'xx', now), null);
  const other = createAccessTokenService({ ...cfg, JWT_SECRET: 'x'.repeat(40) });
  assert.equal(await other.verify(token, now), null);
  assert.equal(await svc.verify('garbage', now), null);
});

test('rate limiter: blocks after max in window and resets', () => {
  let t = 0;
  const l = new MemoryRateLimiter(() => t);
  for (let i = 0; i < 3; i++) assert.equal(l.hit('k', 3, 60), 0);
  assert.ok(l.hit('k', 3, 60) > 0);
  assert.equal(l.hit('other', 3, 60), 0);
  t += 61_000;
  assert.equal(l.hit('k', 3, 60), 0);
});

test('config: production refuses console mail and placeholder secrets; short secrets rejected', () => {
  assert.throws(() => loadConfig({ ...testEnv, NODE_ENV: 'production', EMAIL_PROVIDER: 'console' }), /console/);
  assert.throws(() => loadConfig({ ...testEnv, NODE_ENV: 'production', EMAIL_PROVIDER: 'resend' }), /RESEND_API_KEY/);
  assert.throws(() => loadConfig({ ...testEnv, NODE_ENV: 'production', EMAIL_PROVIDER: 'resend', RESEND_API_KEY: 'k', JWT_SECRET: 'change-me'.repeat(5) }), /Placeholder/);
  assert.throws(() => loadConfig({ ...testEnv, JWT_SECRET: 'short' }), /JWT_SECRET/);
  assert.equal(loadConfig(testEnv).OTP_MAX_ATTEMPTS, 5);
});

test('email templates escape HTML and carry the code', () => {
  const r = render({ to: 'a@b.c', kind: 'verify_email', code: '123456', ttlMinutes: 10 }, 'help@x.y');
  assert.ok(r.text.includes('123456') && r.html.includes('123456') && r.subject.includes('123456'));
  const w = render({ to: 'a@b.c', kind: 'password_changed' }, '<script>@x.y');
  assert.ok(!w.html.includes('<script>'));
  assert.equal(sha256Hex('a').length, 64);
});
