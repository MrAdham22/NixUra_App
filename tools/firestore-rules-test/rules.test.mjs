// Checks firestore.rules on the local Firestore emulator with the same REST requests the app sends
// (data/sync/FirestoreSyncMapping.kt). No dependencies; Node 18+.
//
// Run from the repository root (needs Java 21 for the emulator; uses a demo project, never the real one):
//   firebase emulators:exec --only firestore --project demo-nixura "node --test tools/firestore-rules-test/rules.test.mjs"
import { test } from 'node:test';
import assert from 'node:assert/strict';

const HOST = process.env.FIRESTORE_EMULATOR_HOST ?? '127.0.0.1:8085';
const PROJECT = 'demo-nixura';
const DB = `projects/${PROJECT}/databases/(default)/documents`;
const BASE = `http://${HOST}/v1`;

// The emulator accepts unsigned tokens; it reads the uid from them.
function token(uid) {
  const b64 = (o) => Buffer.from(JSON.stringify(o)).toString('base64url');
  const now = Math.floor(Date.now() / 1000);
  return `${b64({ alg: 'none', typ: 'JWT' })}.${b64({
    iss: `https://securetoken.google.com/${PROJECT}`, aud: PROJECT, iat: now, exp: now + 3600, auth_time: now,
    sub: uid, user_id: uid, firebase: { sign_in_provider: 'password', identities: {} },
  })}.`;
}

async function call(uid, path, body) {
  const res = await fetch(`${BASE}/${path}`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', ...(uid ? { Authorization: `Bearer ${uid === 'owner' ? 'owner' : token(uid)}` } : {}) },
    body: JSON.stringify(body),
  });
  return { status: res.status, json: await res.json().catch(() => null) };
}

const str = (v) => ({ stringValue: v });
const int = (v) => ({ integerValue: String(v) });
const bool = (v) => ({ booleanValue: v });
const map = (fields) => ({ mapValue: { fields } });
const serverTime = [{ fieldPath: 'serverTime', setToServerValue: 'REQUEST_TIME' }];

function item(uid, type, id, updatedAt, data = {}, extra = {}) {
  return {
    update: {
      name: `${DB}/users/${uid}/sync/${type}~${id}`,
      fields: { type: str(type), id: str(id), updatedAt: int(updatedAt), deleted: bool(false), data: map(data), ...extra },
    },
    updateTransforms: serverTime,
  };
}

function day(uid, habitId, month, dayKey, updatedAt, count = 1, extraDays = {}) {
  return {
    update: {
      name: `${DB}/users/${uid}/sync/goodlog~${habitId}~${month}`,
      fields: {
        type: str('good_habit_log_month'), habitId: str(habitId), month: str(month), lastDay: str(dayKey),
        days: map({ [dayKey]: map({ id: str(`${habitId}:1`), count: int(count), updatedAt: int(updatedAt), deleted: bool(false) }), ...extraDays }),
      },
    },
    updateMask: { fieldPaths: ['type', 'habitId', 'month', 'lastDay', `days.${dayKey}`, ...Object.keys(extraDays).map((k) => `days.${k}`)] },
    updateTransforms: serverTime,
  };
}

/** Commits each write on its own (as the app does after a refusal) and returns 0 (saved) or 7 (refused by the rules). */
async function write(uid, ...writes) {
  const codes = [];
  for (const w of writes) {
    const r = await call(uid, `${DB}:commit`, { writes: [w] });
    assert.ok(r.status === 200 || r.json?.error?.status === 'PERMISSION_DENIED', JSON.stringify(r.json));
    codes.push(r.status === 200 ? 0 : 7);
  }
  return codes;
}

const now = () => Date.now();

test('a user writes and reads only their own documents', async () => {
  assert.deepEqual(await write('alice', item('alice', 'habit', 'h1', now(), { name: str('Smoking') })), [0]);
  assert.deepEqual(await write('bob', item('alice', 'habit', 'h2', now())), [7]);
  assert.deepEqual(await write(null, item('alice', 'habit', 'h3', now())), [7]);
  const mine = await call('alice', `${DB}/users/alice:runQuery`, { structuredQuery: { from: [{ collectionId: 'sync' }] } });
  assert.equal(mine.status, 200);
  const theirs = await call('bob', `${DB}/users/alice:runQuery`, { structuredQuery: { from: [{ collectionId: 'sync' }] } });
  assert.equal(theirs.status, 403);
});

test('the pull query ordered by server time and name is allowed', async () => {
  const r = await call('alice', `${DB}/users/alice:runQuery`, {
    structuredQuery: {
      from: [{ collectionId: 'sync' }],
      orderBy: [{ field: { fieldPath: 'serverTime' }, direction: 'ASCENDING' }, { field: { fieldPath: '__name__' }, direction: 'ASCENDING' }],
      startAt: { values: [{ timestampValue: '2020-01-01T00:00:00.000000Z' }], before: true },
      limit: 300,
    },
  });
  assert.equal(r.status, 200, JSON.stringify(r.json));
  assert.ok(r.json.some((row) => row.document?.fields?.serverTime?.timestampValue));
});

test('an older edit never overwrites a newer one; equal or newer is accepted', async () => {
  const t = now();
  assert.deepEqual(await write('carol', item('carol', 'good_habit', 'g1', t, { weeklyTarget: int(3) })), [0]);
  assert.deepEqual(await write('carol', item('carol', 'good_habit', 'g1', t - 1)), [7]);
  assert.deepEqual(await write('carol', item('carol', 'good_habit', 'g1', t)), [0]);
  assert.deepEqual(await write('carol', item('carol', 'good_habit', 'g1', t + 1)), [0]);
});

test('times far in the future, wrong ids, unknown types and extra fields are refused', async () => {
  assert.deepEqual(await write('dave', item('dave', 'habit', 'h1', now() + 3_600_000)), [7]);
  const wrongId = item('dave', 'habit', 'h1', now());
  wrongId.update.fields.id = str('other');
  assert.deepEqual(await write('dave', wrongId), [7]);
  assert.deepEqual(await write('dave', item('dave', 'journal', 'j1', now())), [7]);
  assert.deepEqual(await write('dave', item('dave', 'habit', 'h2', now(), {}, { admin: bool(true) })), [7]);
  const noServerTime = item('dave', 'habit', 'h3', now());
  delete noServerTime.updateTransforms;
  noServerTime.update.fields.serverTime = { timestampValue: '2020-01-01T00:00:00Z' };
  assert.deepEqual(await write('dave', noServerTime), [7]);
});

test('good-habit months: one day per write, older day edits lose, other days are kept', async () => {
  const t = now();
  assert.deepEqual(await write('erin', day('erin', 'g1', '2026-11', 'd14', t, 1)), [0]);
  assert.deepEqual(await write('erin', day('erin', 'g1', '2026-11', 'd13', t - 5000, 2)), [0]);
  assert.deepEqual(await write('erin', day('erin', 'g1', '2026-11', 'd14', t - 1, 0)), [7]);
  assert.deepEqual(await write('erin', day('erin', 'g1', '2026-11', 'd14', t + 1, 3)), [0]);
  // Two days in one write cannot be checked day by day, so it is refused.
  const two = day('erin', 'g1', '2026-11', 'd20', t, 1, { d21: map({ id: str('x'), count: int(1), updatedAt: int(t), deleted: bool(false) }) });
  assert.deepEqual(await write('erin', two), [7]);
  // A month name that does not match the document is refused.
  const wrong = day('erin', 'g1', '2026-11', 'd01', t);
  wrong.update.name = `${DB}/users/erin/sync/goodlog~g1~2026-12`;
  assert.deepEqual(await write('erin', wrong), [7]);

  const got = await call('erin', `${DB}:batchGet`, { documents: [`${DB}/users/erin/sync/goodlog~g1~2026-11`] });
  const days = got.json[0].found.fields.days.mapValue.fields;
  assert.equal(days.d14.mapValue.fields.count.integerValue, '3');
  assert.equal(days.d13.mapValue.fields.count.integerValue, '2');
});

test('a commit with one stale item is refused as a whole (the app then splits it)', async () => {
  const t = now();
  await write('ivy', item('ivy', 'habit', 'h1', t));
  const r = await call('ivy', `${DB}:commit`, { writes: [item('ivy', 'habit', 'h2', t), item('ivy', 'habit', 'h1', t - 1)] });
  assert.equal(r.status, 403);
  assert.equal(r.json.error.status, 'PERMISSION_DENIED');
  assert.deepEqual(await write('ivy', item('ivy', 'habit', 'h2', t)), [0]);
});

test('the owner can delete their documents, nobody else can', async () => {
  await write('frank', item('frank', 'event', 'e1', now()));
  assert.deepEqual(await write('gina', { delete: `${DB}/users/frank/sync/event~e1` }), [7]);
  assert.deepEqual(await write('frank', { delete: `${DB}/users/frank/sync/event~e1` }), [0]);
});

test('nothing outside users/{uid}/sync is reachable', async () => {
  const r = await write('hank', { update: { name: `${DB}/users/hank`, fields: { a: int(1) } } });
  assert.deepEqual(r, [7]);
});

// ---- bugReports (UX A2, data/support/FirestoreBugReportRemote.kt) ----

function report(uid, id, overrides = {}) {
  return {
    update: {
      name: `${DB}/bugReports/${id}`,
      fields: {
        uid: str(uid), category: str('crash'), message: str('The app closed when I tapped +'), steps: str(''),
        appVersion: str('1.0 (1)'), platform: str('android'), osVersion: str('15 (API 35)'),
        deviceModel: str('OPPO CPH2531'), language: str('en'), ...overrides,
      },
    },
    currentDocument: { exists: false },
    updateTransforms: [{ fieldPath: 'createdAt', setToServerValue: 'REQUEST_TIME' }],
  };
}

test('a signed-in user can create a bug report as themselves', async () => {
  assert.deepEqual(await write('rita', report('rita', 'r1')), [0]);
});

test('guests, other uids, bad fields and oversized text cannot create a report', async () => {
  assert.deepEqual(await write(null, report('rita', 'r2')), [7]);
  assert.deepEqual(await write('sam', report('rita', 'r3')), [7]);
  assert.deepEqual(await write('sam', report('sam', 'r4', { category: str('spam') })), [7]);
  assert.deepEqual(await write('sam', report('sam', 'r5', { message: str('') })), [7]);
  assert.deepEqual(await write('sam', report('sam', 'r6', { message: str('x'.repeat(2001)) })), [7]);
  assert.deepEqual(await write('sam', report('sam', 'r7', { email: str('a@b.c') })), [7]);
  const noTime = report('sam', 'r8');
  delete noTime.updateTransforms;
  assert.deepEqual(await write('sam', noTime), [7]);
});

test('nobody can read, change or delete a bug report, not even its author', async () => {
  await write('tom', report('tom', 'r9'));
  const read = await call('tom', `${DB}:batchGet`, { documents: [`${DB}/bugReports/r9`] });
  assert.equal(read.status, 403);
  const list = await call('tom', `${DB}:runQuery`, { structuredQuery: { from: [{ collectionId: 'bugReports' }] } });
  assert.equal(list.status, 403);
  const overwrite = report('tom', 'r9', { message: str('changed text here') });
  delete overwrite.currentDocument;
  assert.deepEqual(await write('tom', overwrite), [7]);
  assert.deepEqual(await write('tom', { delete: `${DB}/bugReports/r9` }), [7]);
});
