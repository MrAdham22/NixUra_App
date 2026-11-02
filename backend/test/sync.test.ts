import assert from 'node:assert/strict';
import { after, before, describe, test } from 'node:test';
import { GOOD_PASSWORD, makeCtx, type TestCtx } from './helpers.js';

const habit = (id: string, updatedAt: number, name: string, deleted = false) => ({
  type: 'habit',
  id,
  updatedAt,
  deleted,
  payload: JSON.stringify({ name }),
});

describe('cloud sync', () => {
  let c: TestCtx;
  let ann: { accessToken: string; userId: string };
  let bob: { accessToken: string; userId: string };
  const base = new Date('2030-01-01T00:00:00Z').getTime();

  const push = (token: string, changes: unknown[]) => c.post('/v1/sync/push', { changes }, token);
  const pull = (token: string, cursor = '0', limit?: number) =>
    c.get(`/v1/sync/pull?cursor=${cursor}${limit ? `&limit=${limit}` : ''}`, token);

  before(async () => {
    c = await makeCtx();
    ann = await c.signUp('ann@example.com', GOOD_PASSWORD);
    bob = await c.signUp('bob@example.com', GOOD_PASSWORD);
  });
  after(() => c.close());

  test('sync needs a signed-in user', async () => {
    assert.equal((await push('', [])).status, 401);
    assert.equal((await pull('nonsense')).status, 401);
  });

  test('a change pushed by one device is pulled by another', async () => {
    const r = await push(ann.accessToken, [habit('h1', base, 'Smoking')]);
    assert.equal(r.status, 200);
    assert.deepEqual(r.body.results, [{ type: 'habit', id: 'h1', applied: true }]);
    const p = await pull(ann.accessToken);
    assert.equal(p.status, 200);
    assert.equal(p.body.items.length, 1);
    assert.equal(p.body.items[0].id, 'h1');
    assert.equal(p.body.items[0].updatedAt, base);
    assert.equal(JSON.parse(p.body.items[0].payload).name, 'Smoking');
  });

  test('a user can never read or overwrite another user\'s data, even with the same id', async () => {
    assert.equal((await pull(bob.accessToken)).body.items.length, 0);
    await push(bob.accessToken, [habit('h1', base + 500, 'Bob private')]);
    const annItems = (await pull(ann.accessToken)).body.items;
    assert.equal(JSON.parse(annItems[0].payload).name, 'Smoking');
    const bobItems = (await pull(bob.accessToken)).body.items;
    assert.equal(bobItems.length, 1);
    assert.equal(JSON.parse(bobItems[0].payload).name, 'Bob private');
  });

  test('the newest edit wins, in either arrival order', async () => {
    await push(ann.accessToken, [habit('h2', base + 2000, 'New name')]);
    const old = await push(ann.accessToken, [habit('h2', base + 1000, 'Old name')]);
    assert.equal(old.body.results[0].applied, false);
    const item = (await pull(ann.accessToken)).body.items.find((i: { id: string }) => i.id === 'h2');
    assert.equal(JSON.parse(item.payload).name, 'New name');
  });

  test('equal timestamps resolve to the same winner in either order', async () => {
    await push(ann.accessToken, [habit('t1', base, 'Alpha')]);
    await push(ann.accessToken, [habit('t1', base, 'Beta')]);
    await push(ann.accessToken, [habit('t2', base, 'Beta')]);
    await push(ann.accessToken, [habit('t2', base, 'Alpha')]);
    const items = (await pull(ann.accessToken)).body.items;
    const name = (id: string) => JSON.parse(items.find((i: { id: string }) => i.id === id).payload).name;
    assert.equal(name('t1'), name('t2'));
  });

  test('retrying the same push changes nothing and makes no new pull items', async () => {
    const first = await push(ann.accessToken, [habit('r1', base, 'Retry')]);
    assert.equal(first.body.results[0].applied, true);
    const cursor = (await pull(ann.accessToken)).body.cursor;
    const again = await push(ann.accessToken, [habit('r1', base, 'Retry')]);
    assert.equal(again.body.results[0].applied, false);
    assert.equal((await pull(ann.accessToken, cursor)).body.items.length, 0);
  });

  test('a delete reaches other devices, and an older edit cannot undo it', async () => {
    await push(ann.accessToken, [habit('d1', base, 'To delete')]);
    const cursor = (await pull(ann.accessToken)).body.cursor;
    await push(ann.accessToken, [habit('d1', base + 100, 'To delete', true)]);
    const next = (await pull(ann.accessToken, cursor)).body.items;
    assert.equal(next.length, 1);
    assert.equal(next[0].deleted, true);
    assert.equal((await push(ann.accessToken, [habit('d1', base + 50, 'Stale edit')])).body.results[0].applied, false);
  });

  test('a wrong phone clock cannot win forever: timestamps are capped at the server clock', async () => {
    const now = c.now().getTime();
    await push(ann.accessToken, [habit('f1', now + 10 * 365 * 86_400_000, 'From the future')]);
    const item = (await pull(ann.accessToken)).body.items.find((i: { id: string }) => i.id === 'f1');
    assert.equal(item.updatedAt, now);
    c.advance(1000);
    assert.equal((await push(ann.accessToken, [habit('f1', now + 1000, 'Later edit')])).body.results[0].applied, true);
  });

  test('pull pages through everything exactly once, in order', async () => {
    const carol = await c.signUp('carol@example.com', GOOD_PASSWORD);
    await push(carol.accessToken, Array.from({ length: 5 }, (_, i) => habit(`p${i}`, base + i, `Habit ${i}`)));
    const seen: string[] = [];
    let cursor = '0';
    for (;;) {
      const page = (await pull(carol.accessToken, cursor, 2)).body;
      seen.push(...page.items.map((i: { id: string }) => i.id));
      cursor = page.cursor;
      if (!page.hasMore) break;
    }
    assert.deepEqual(seen, ['p0', 'p1', 'p2', 'p3', 'p4']);
    assert.equal((await pull(carol.accessToken, cursor)).body.items.length, 0);
  });

  test('bad input is refused', async () => {
    assert.equal((await push(ann.accessToken, [{ ...habit('x', base, 'x'), type: 'not_a_type' }])).status, 422);
    assert.equal((await push(ann.accessToken, [{ ...habit('x', base, 'x'), payload: 'y'.repeat(9000) }])).status, 422);
    assert.equal((await push(ann.accessToken, Array.from({ length: 201 }, (_, i) => habit(`m${i}`, base, 'm')))).status, 422);
    assert.equal((await pull(ann.accessToken, 'abc')).status, 422);
  });

  test('a full push batch fits through the body limit', async () => {
    const big = Array.from({ length: 200 }, (_, i) => ({ ...habit(`b${i}`, base, 'b'), payload: 'x'.repeat(8000) }));
    assert.equal((await push(ann.accessToken, big)).status, 200);
  });

  test('deleting the account removes all of its synced data', async () => {
    const dave = await c.signUp('dave@example.com', GOOD_PASSWORD);
    await push(dave.accessToken, [habit('z1', base, 'Gone soon')]);
    assert.equal((await c.post('/v1/auth/delete-account', { password: GOOD_PASSWORD }, dave.accessToken)).status, 204);
    const left = await c.db.query('SELECT 1 FROM sync_items WHERE user_id = $1', [dave.userId]);
    assert.equal(left.length, 0);
  });
});
