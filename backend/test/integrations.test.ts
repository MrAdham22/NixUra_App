import { test } from 'node:test';
import assert from 'node:assert/strict';
import { makeCtx, GOOD_PASSWORD, TEST_TOKEN_KEY } from './helpers.js';
import { TokenCipher } from '../src/integrations/tokenCipher.js';
import { toItem, zonedToEpoch } from '../src/integrations/notionService.js';

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
        has_more: false, next_cursor: null,
      });
    }
    if (url.includes('/databases/db1/query')) {
      return json({
        results: [
          { id: 'p1', last_edited_time: '2030-01-01T10:00:00.000Z', properties: { Name: { type: 'title', title: [{ plain_text: 'Dentist' }] }, When: { type: 'date', date: { start: '2030-01-05T09:30:00.000+02:00' } } } },
          { id: 'p2', last_edited_time: '2030-01-01T11:00:00.000Z', properties: { Name: { type: 'title', title: [{ plain_text: 'Trip' }] }, When: { type: 'date', date: { start: '2030-02-01' } } } },
        ],
        has_more: false, next_cursor: null,
      });
    }
    return json({}, 404);
  }) as typeof fetch;
  return { fn, calls };
}

async function connect(ctx: Awaited<ReturnType<typeof makeCtx>>, access: string) {
  const start = await ctx.post('/v1/integrations/notion/start', {}, access);
  assert.equal(start.status, 200);
  const state = new URL(start.body.url).searchParams.get('state')!;
  const cb = await ctx.get(`/v1/integrations/notion/callback?code=abc&state=${state}`);
  return { start, cb };
}

test('Notion OAuth round trip stores the token encrypted and returns to the app', async () => {
  const notion = fakeNotion();
  const ctx = await makeCtx({}, undefined, notion.fn);
  try {
    const u = await ctx.signUp('n1@example.com', GOOD_PASSWORD);
    const { start, cb } = await connect(ctx, u.accessToken);
    assert.match(start.body.url, /^https:\/\/api\.notion\.com\/v1\/oauth\/authorize\?client_id=cid&response_type=code&owner=user/);
    assert.equal(cb.status, 302);
    assert.equal(cb.headers.location, 'nixura://integrations/notion?result=connected');
    // the client secret only went to Notion, as HTTP Basic
    assert.equal(notion.calls[0]!.auth, `Basic ${Buffer.from('cid:csecret').toString('base64')}`);
    const rows = await ctx.db.query<{ token_enc: string }>('SELECT token_enc FROM integration_links');
    assert.equal(rows.length, 1);
    assert.ok(!rows[0]!.token_enc.includes('secret-notion-token'), 'token is not stored in plain text');
    assert.equal(new TokenCipher(TEST_TOKEN_KEY).decrypt(rows[0]!.token_enc), 'secret-notion-token');
    const st = await ctx.get('/v1/integrations', u.accessToken);
    assert.deepEqual(st.body, { notion: { connected: true, workspace: 'Home' } });
  } finally {
    await ctx.close();
  }
});

test('a state is single use and bound to its user', async () => {
  const ctx = await makeCtx({}, undefined, fakeNotion().fn);
  try {
    const u = await ctx.signUp('n2@example.com', GOOD_PASSWORD);
    const start = await ctx.post('/v1/integrations/notion/start', {}, u.accessToken);
    const state = new URL(start.body.url).searchParams.get('state')!;
    assert.equal((await ctx.get(`/v1/integrations/notion/callback?code=abc&state=${state}`)).headers.location, 'nixura://integrations/notion?result=connected');
    assert.equal((await ctx.get(`/v1/integrations/notion/callback?code=abc&state=${state}`)).headers.location, 'nixura://integrations/notion?result=error');
    assert.equal((await ctx.get('/v1/integrations/notion/callback?code=abc&state=forged')).headers.location, 'nixura://integrations/notion?result=error');
    const s2 = await ctx.post('/v1/integrations/notion/start', {}, u.accessToken);
    const st2 = new URL(s2.body.url).searchParams.get('state')!;
    assert.equal((await ctx.get(`/v1/integrations/notion/callback?error=access_denied&state=${st2}`)).headers.location, 'nixura://integrations/notion?result=denied');
  } finally {
    await ctx.close();
  }
});

test('databases and query map Notion pages to events, per user', async () => {
  const ctx = await makeCtx({}, undefined, fakeNotion().fn);
  try {
    const u = await ctx.signUp('n3@example.com', GOOD_PASSWORD);
    const other = await ctx.signUp('n4@example.com', GOOD_PASSWORD);
    await connect(ctx, u.accessToken);
    const dbs = await ctx.get('/v1/integrations/notion/databases', u.accessToken);
    assert.deepEqual(dbs.body.databases, [{ id: 'db1', title: 'Plans', titleProperties: ['Name'], dateProperties: ['When'] }]);
    const q = await ctx.post('/v1/integrations/notion/query', { databaseId: 'db1', titleProperty: 'Name', dateProperty: 'When' }, u.accessToken);
    assert.equal(q.status, 200);
    assert.equal(q.body.complete, true);
    assert.deepEqual(q.body.items[0], { id: 'p1', title: 'Dentist', start: Date.parse('2030-01-05T07:30:00Z'), allDayDate: null, lastEditedAt: Date.parse('2030-01-01T10:00:00Z'), archived: false });
    assert.equal(q.body.items[1].allDayDate, '2030-02-01');
    // another account has no Notion link
    assert.equal((await ctx.get('/v1/integrations/notion/databases', other.accessToken)).status, 409);
    // disconnect forgets the token
    assert.equal((await ctx.del('/v1/integrations/notion', u.accessToken)).status, 204);
    assert.equal((await ctx.get('/v1/integrations', u.accessToken)).body.notion.connected, false);
  } finally {
    await ctx.close();
  }
});

test('access removed in Notion ends the link with a clear answer', async () => {
  const ctx = await makeCtx({}, undefined, fakeNotion({ revoked: true }).fn);
  try {
    const u = await ctx.signUp('n5@example.com', GOOD_PASSWORD);
    await connect(ctx, u.accessToken);
    const r = await ctx.get('/v1/integrations/notion/databases', u.accessToken);
    assert.equal(r.status, 409);
    assert.equal(r.body.error.code, 'integration_not_connected');
    assert.equal((await ctx.get('/v1/integrations', u.accessToken)).body.notion.connected, false);
  } finally {
    await ctx.close();
  }
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

test('the token cipher rejects tampering and wrong keys', () => {
  const c = new TokenCipher(TEST_TOKEN_KEY);
  const enc = c.encrypt('abc');
  assert.notEqual(enc, c.encrypt('abc'), 'a fresh nonce each time');
  assert.equal(c.decrypt(enc), 'abc');
  const parts = enc.split('.');
  parts[3] = Buffer.from('xyz').toString('base64url');
  assert.throws(() => c.decrypt(parts.join('.')));
  assert.throws(() => new TokenCipher(Buffer.alloc(32, 1).toString('base64')).decrypt(enc));
  assert.throws(() => new TokenCipher('short'));
});
