import { test } from 'node:test';
import assert from 'node:assert/strict';
import { generateKeyPair, SignJWT, type KeyLike } from 'jose';
import { handle, type Deps } from '../src/app.js';
import { TokenCipher } from '../src/cipher.js';
import { verifyFirebaseIdToken } from '../src/firebaseAuth.js';
import { NotionService, toItem, zonedToEpoch } from '../src/notion.js';
import type { LinkStore } from '../src/store.js';

const KEY = btoa(String.fromCharCode(...new Uint8Array(32).fill(7)));
const PROJECT = 'nixura';
const BASE = 'https://w.example';

class MemoryStore implements LinkStore {
  states = new Map<string, { uid: string; expiresAt: number }>();
  links = new Map<string, { tokenEnc: string; workspace: string }>();
  async putState(state: string, uid: string, expiresAt: number) { this.states.set(state, { uid, expiresAt }); }
  async takeState(state: string) { const r = this.states.get(state) ?? null; this.states.delete(state); return r; }
  async pruneStates(now: number) { for (const [k, v] of this.states) if (v.expiresAt < now) this.states.delete(k); }
  async putLink(uid: string, tokenEnc: string, workspace: string) { this.links.set(uid, { tokenEnc, workspace }); }
  async getLink(uid: string) { return this.links.get(uid) ?? null; }
  async deleteLink(uid: string) { this.links.delete(uid); }
}

/** A fake Notion API: records calls and answers like Notion does. */
function fakeNotion(opts: { revoked?: boolean } = {}) {
  const calls: { url: string; body: any; auth: string }[] = [];
  const fn = (async (input: string | URL | Request, init?: RequestInit) => {
    const url = String(input);
    const body = init?.body ? JSON.parse(String(init.body)) : undefined;
    calls.push({ url, body, auth: String((init?.headers as Record<string, string>)?.Authorization ?? '') });
    const json = (o: unknown, status = 200) => new Response(JSON.stringify(o), { status, headers: { 'content-type': 'application/json' } });
    if (url.endsWith('/oauth/token')) return json({ access_token: 'secret-notion-token', workspace_name: 'Home' });
    if (opts.revoked) return json({ object: 'error' }, 401);
    if (url.endsWith('/search')) {
      return json({
        results: [{ id: 'db1', title: [{ plain_text: 'Plans' }], properties: { Name: { type: 'title' }, When: { type: 'date' }, Notes: { type: 'rich_text' } } }],
        has_more: false,
        next_cursor: null,
      });
    }
    if (url.includes('/databases/db1/query')) {
      return json({
        results: [
          { id: 'p1', last_edited_time: '2030-01-01T10:00:00.000Z', properties: { Name: { type: 'title', title: [{ plain_text: 'Dentist' }] }, When: { type: 'date', date: { start: '2030-01-05T09:30:00.000+02:00' } } } },
          { id: 'p2', last_edited_time: '2030-01-01T11:00:00.000Z', properties: { Name: { type: 'title', title: [{ plain_text: 'Trip' }] }, When: { type: 'date', date: { start: '2030-02-01' } } } },
        ],
        has_more: false,
        next_cursor: null,
      });
    }
    return json({}, 404);
  }) as typeof fetch;
  return { fn, calls };
}

let keys: { privateKey: KeyLike; publicKey: KeyLike } | undefined;
async function signingKeys() {
  keys ??= await generateKeyPair('RS256');
  return keys;
}

async function idToken(uid: string, over: { iss?: string; aud?: string; exp?: number; authTime?: number; key?: KeyLike } = {}): Promise<string> {
  const k = await signingKeys();
  const now = Math.floor(Date.now() / 1000);
  return new SignJWT({ auth_time: over.authTime ?? now - 60 })
    .setProtectedHeader({ alg: 'RS256', kid: 'test' })
    .setSubject(uid)
    .setIssuer(over.iss ?? `https://securetoken.google.com/${PROJECT}`)
    .setAudience(over.aud ?? PROJECT)
    .setIssuedAt(now - 60)
    .setExpirationTime(over.exp ?? now + 3600)
    .sign(over.key ?? k.privateKey);
}

async function makeCtx(notion = fakeNotion(), opts: { limited?: boolean; configured?: boolean } = {}) {
  const store = new MemoryStore();
  const cipher = await TokenCipher.create(KEY);
  const configured = opts.configured ?? true;
  const service = new NotionService(
    store,
    configured ? { clientId: 'cid', clientSecret: 'csecret', redirectUri: `${BASE}/v1/integrations/notion/callback`, appReturnUrl: 'nixura://integrations/notion' } : null,
    configured ? cipher : null,
    notion.fn,
    () => Date.now(),
    async () => {},
  );
  const { publicKey } = await signingKeys();
  const deps: Deps = {
    notion: service,
    verify: (t) => verifyFirebaseIdToken(t, PROJECT, async () => publicKey),
    limit: async () => !opts.limited,
  };
  const call = async (method: string, path: string, body?: unknown, token?: string) => {
    const headers: Record<string, string> = {};
    if (token) headers.Authorization = `Bearer ${token}`;
    if (body !== undefined) headers['Content-Type'] = 'application/json';
    const res = await handle(new Request(`${BASE}${path}`, { method, headers, body: body === undefined ? undefined : JSON.stringify(body) }), deps);
    const text = await res.text();
    return { status: res.status, headers: res.headers, body: text ? JSON.parse(text) : undefined };
  };
  return { store, cipher, call, notion };
}

async function connect(ctx: Awaited<ReturnType<typeof makeCtx>>, token: string) {
  const start = await ctx.call('POST', '/v1/integrations/notion/start', {}, token);
  assert.equal(start.status, 200);
  const state = new URL(start.body.url).searchParams.get('state')!;
  const cb = await ctx.call('GET', `/v1/integrations/notion/callback?code=abc&state=${state}`);
  return { start, cb };
}

test('Notion OAuth round trip stores the token encrypted and returns to the app', async () => {
  const ctx = await makeCtx();
  const t = await idToken('user-1');
  const { start, cb } = await connect(ctx, t);
  assert.match(start.body.url, /^https:\/\/api\.notion\.com\/v1\/oauth\/authorize\?client_id=cid&response_type=code&owner=user/);
  assert.equal(cb.status, 302);
  assert.equal(cb.headers.get('location'), 'nixura://integrations/notion?result=connected');
  // the client secret only went to Notion, as HTTP Basic
  assert.equal(ctx.notion.calls[0]!.auth, `Basic ${btoa('cid:csecret')}`);
  const link = ctx.store.links.get('user-1')!;
  assert.ok(!link.tokenEnc.includes('secret-notion-token'), 'token is not stored in plain text');
  assert.equal(await ctx.cipher.decrypt(link.tokenEnc), 'secret-notion-token');
  const st = await ctx.call('GET', '/v1/integrations', undefined, t);
  assert.deepEqual(st.body, { notion: { connected: true, workspace: 'Home' } });
});

test('a state is single use and bound to its user', async () => {
  const ctx = await makeCtx();
  const t = await idToken('user-2');
  const start = await ctx.call('POST', '/v1/integrations/notion/start', {}, t);
  const state = new URL(start.body.url).searchParams.get('state')!;
  const cb = (q: string) => ctx.call('GET', `/v1/integrations/notion/callback?${q}`).then((r) => r.headers.get('location'));
  assert.equal(await cb(`code=abc&state=${state}`), 'nixura://integrations/notion?result=connected');
  assert.equal(await cb(`code=abc&state=${state}`), 'nixura://integrations/notion?result=error');
  assert.equal(await cb('code=abc&state=forged'), 'nixura://integrations/notion?result=error');
  assert.equal(await cb('code=abc'), 'nixura://integrations/notion?result=error');
  const s2 = await ctx.call('POST', '/v1/integrations/notion/start', {}, t);
  const st2 = new URL(s2.body.url).searchParams.get('state')!;
  assert.equal(await cb(`error=access_denied&state=${st2}`), 'nixura://integrations/notion?result=denied');
});

test('an expired state is refused', async () => {
  const ctx = await makeCtx();
  ctx.store.states.set('old', { uid: 'user-x', expiresAt: Date.now() - 1 });
  const r = await ctx.call('GET', '/v1/integrations/notion/callback?code=abc&state=old');
  assert.equal(r.headers.get('location'), 'nixura://integrations/notion?result=error');
  assert.equal(ctx.store.links.size, 0);
});

test('databases and query map Notion pages to events, per user', async () => {
  const ctx = await makeCtx();
  const t = await idToken('user-3');
  const other = await idToken('user-4');
  await connect(ctx, t);
  const dbs = await ctx.call('GET', '/v1/integrations/notion/databases', undefined, t);
  assert.deepEqual(dbs.body.databases, [{ id: 'db1', title: 'Plans', titleProperties: ['Name'], dateProperties: ['When'] }]);
  const q = await ctx.call('POST', '/v1/integrations/notion/query', { databaseId: 'db1', titleProperty: 'Name', dateProperty: 'When' }, t);
  assert.equal(q.status, 200);
  assert.equal(q.body.complete, true);
  assert.deepEqual(q.body.items[0], { id: 'p1', title: 'Dentist', start: Date.parse('2030-01-05T07:30:00Z'), allDayDate: null, lastEditedAt: Date.parse('2030-01-01T10:00:00Z'), archived: false });
  assert.equal(q.body.items[1].allDayDate, '2030-02-01');
  // the app sends "since": null on a full read
  assert.equal((await ctx.call('POST', '/v1/integrations/notion/query', { databaseId: 'db1', titleProperty: 'Name', dateProperty: 'When', since: null }, t)).status, 200);
  // another account has no Notion link
  assert.equal((await ctx.call('GET', '/v1/integrations/notion/databases', undefined, other)).status, 409);
  // disconnect forgets the token
  assert.equal((await ctx.call('DELETE', '/v1/integrations/notion', undefined, t)).status, 204);
  assert.equal((await ctx.call('GET', '/v1/integrations', undefined, t)).body.notion.connected, false);
});

test('incremental query sends Notion a last_edited_time filter', async () => {
  const ctx = await makeCtx();
  const t = await idToken('user-5');
  await connect(ctx, t);
  await ctx.call('POST', '/v1/integrations/notion/query', { databaseId: 'db1', titleProperty: 'Name', dateProperty: 'When', since: Date.parse('2030-01-01T00:00:00Z') }, t);
  const sent = ctx.notion.calls.at(-1)!.body;
  assert.deepEqual(sent.filter, { timestamp: 'last_edited_time', last_edited_time: { on_or_after: '2030-01-01T00:00:00.000Z' } });
});

test('access removed in Notion ends the link with a clear answer', async () => {
  const ctx = await makeCtx(fakeNotion({ revoked: true }));
  const t = await idToken('user-6');
  await connect(ctx, t);
  const r = await ctx.call('GET', '/v1/integrations/notion/databases', undefined, t);
  assert.equal(r.status, 409);
  assert.equal(r.body.error.code, 'integration_not_connected');
  assert.equal((await ctx.call('GET', '/v1/integrations', undefined, t)).body.notion.connected, false);
});

test('signed-in routes need a valid Firebase ID token', async () => {
  const ctx = await makeCtx();
  const other = await generateKeyPair('RS256');
  const now = Math.floor(Date.now() / 1000);
  const bad = [
    undefined,
    'not-a-jwt',
    await idToken('u', { aud: 'some-other-project' }),
    await idToken('u', { iss: 'https://securetoken.google.com/some-other-project' }),
    await idToken('u', { exp: now - 3600 }),
    await idToken('u', { authTime: now + 3600 }),
    await idToken('u', { key: other.privateKey }),
    await idToken(''),
  ];
  for (const token of bad) {
    const r = await ctx.call('GET', '/v1/integrations', undefined, token);
    assert.equal(r.status, 401, `token ${bad.indexOf(token)} should be refused`);
    assert.equal(r.body.error.code, 'unauthorized');
  }
  assert.equal((await ctx.call('GET', '/v1/integrations', undefined, await idToken('fine'))).status, 200);
});

test('bad input, unknown routes, limits and a missing setup get clear answers', async () => {
  const ctx = await makeCtx();
  const t = await idToken('user-7');
  assert.equal((await ctx.call('POST', '/v1/integrations/notion/query', { databaseId: '' , titleProperty: 'a', dateProperty: 'b' }, t)).status, 400);
  assert.equal((await ctx.call('POST', '/v1/integrations/notion/query', { databaseId: 'db1', titleProperty: 'a', dateProperty: 'b', since: -5 }, t)).status, 400);
  assert.equal((await ctx.call('POST', '/v1/integrations/notion/query', { databaseId: 'x'.repeat(101), titleProperty: 'a', dateProperty: 'b' }, t)).status, 400);
  assert.equal((await ctx.call('GET', '/v1/integrations/notion/callback?state=' + 'x'.repeat(101))).status, 400);
  assert.equal((await ctx.call('GET', '/v1/nope')).status, 404);
  assert.equal((await ctx.call('GET', '/health')).body.ok, true);

  const limited = await makeCtx(fakeNotion(), { limited: true });
  assert.equal((await limited.call('POST', '/v1/integrations/notion/start', {}, t)).status, 429);
  assert.equal((await limited.call('GET', '/v1/integrations/notion/callback?code=a&state=b')).status, 429);

  const unset = await makeCtx(fakeNotion(), { configured: false });
  const r = await unset.call('POST', '/v1/integrations/notion/start', {}, t);
  assert.equal(r.status, 503);
  assert.equal(r.body.error.code, 'integration_unavailable');
  assert.equal((await unset.call('GET', '/v1/integrations', undefined, t)).body.notion.connected, false);
});

test('Notion dates: whole days, offsets, named time zones, archived pages', () => {
  const base = { id: 'x', last_edited_time: '2030-01-01T00:00:00Z' };
  const date = (d: { start: string; time_zone?: string | null }) => ({ ...base, properties: { T: { type: 'title', title: [{ plain_text: ' A ' }] }, D: { type: 'date', date: d } } });
  assert.equal(toItem(date({ start: '2030-03-10' }), 'T', 'D').allDayDate, '2030-03-10');
  assert.equal(toItem(date({ start: '2030-03-10T08:00:00.000Z' }), 'T', 'D').start, Date.parse('2030-03-10T08:00:00Z'));
  // 09:00 in Cairo (UTC+2 in January) is 07:00 UTC
  assert.equal(toItem(date({ start: '2030-01-10T09:00:00.000', time_zone: 'Africa/Cairo' }), 'T', 'D').start, Date.parse('2030-01-10T07:00:00Z'));
  assert.equal(zonedToEpoch('2030-07-01T12:00:00', 'Europe/London'), Date.parse('2030-07-01T11:00:00Z')); // BST
  assert.equal(toItem({ ...date({ start: '2030-03-10' }), archived: true }, 'T', 'D').archived, true);
  assert.equal(toItem(date({ start: '2030-03-10' }), 'T', 'D').title, 'A');
  assert.equal(toItem({ ...base, properties: {} }, 'T', 'D').start, null);
});

test('the token cipher rejects tampering and wrong keys, and reads the old backend format', async () => {
  const c = await TokenCipher.create(KEY);
  const enc = await c.encrypt('abc');
  assert.notEqual(enc, await c.encrypt('abc'), 'a fresh nonce each time');
  assert.equal(await c.decrypt(enc), 'abc');
  const parts = enc.split('.');
  parts[3] = btoa('xyz').replace(/=+$/, '');
  await assert.rejects(c.decrypt(parts.join('.')));
  const wrong = await TokenCipher.create(btoa(String.fromCharCode(...new Uint8Array(32).fill(1))));
  await assert.rejects(wrong.decrypt(enc));
  await assert.rejects(TokenCipher.create('short'));

  // Same layout as the Node backend's TokenCipher (node:crypto aes-256-gcm), so old rows stay readable.
  const { createCipheriv } = await import('node:crypto');
  const nonce = Buffer.alloc(12, 3);
  const nodeCipher = createCipheriv('aes-256-gcm', Buffer.from(KEY, 'base64'), nonce);
  const data = Buffer.concat([nodeCipher.update('from-node', 'utf8'), nodeCipher.final()]);
  const legacy = ['v1', nonce.toString('base64url'), nodeCipher.getAuthTag().toString('base64url'), data.toString('base64url')].join('.');
  assert.equal(await c.decrypt(legacy), 'from-node');
});
