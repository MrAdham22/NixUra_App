import { test } from 'node:test';
import assert from 'node:assert/strict';
import { handle, type Deps } from '../src/app.js';
import { CoachService } from '../src/ai/coach.js';
import { cleanActions, parseContext, readChatAnswer } from '../src/ai/model.js';
import { chatSystem, voiceSystem } from '../src/ai/prompts.js';
import { gemini, openAiCompatible, ProviderError, type Provider } from '../src/ai/providers.js';
import type { Usage, UsageKind, UsageStore } from '../src/ai/usage.js';
import { buildProviders, type Env } from '../src/index.js';

class MemoryUsage implements UsageStore {
  rows = new Map<string, Usage>();
  async get(uid: string, day: string) { return this.rows.get(`${uid}|${day}`) ?? { chat: 0, voice: 0 }; }
  async reserve(uid: string, day: string, kind: UsageKind, limit: number) {
    const u = { ...(await this.get(uid, day)) };
    if (u[kind] >= limit) return null;
    u[kind]++;
    this.rows.set(`${uid}|${day}`, u);
    return u;
  }
  async refund(uid: string, day: string, kind: UsageKind) {
    const u = await this.get(uid, day);
    this.rows.set(`${uid}|${day}`, { ...u, [kind]: Math.max(u[kind] - 1, 0) });
  }
  async prune() {}
}

const CTX = {
  today: '2026-11-11', weekday: 'Wednesday', time: '14:05',
  quit: [{ name: 'Smoking', streakHours: 52, bestHours: 300, relapses: 2 }],
  build: [{ name: 'Water', today: 3, target: 5, streak: 4 }, { name: 'Gym', today: 0, target: 1, streak: 2, weekDone: 1, weekGoal: 3 }],
  events: [{ title: 'Exam', date: '2026-11-14', time: '09:00' }],
};
const AUDIO = 'A'.repeat(400);

const good = (name: string, answer: string, extra: Partial<Provider> = {}): Provider & { calls: number } => {
  const p = { name, calls: 0, async chat() { p.calls++; return answer; }, ...extra };
  return p;
};
const broken = (name: string, status = 429): Provider & { calls: number } => {
  const p = { name, calls: 0, async chat(): Promise<string> { p.calls++; throw new ProviderError(name, status); } };
  return p;
};

function service(providers: Provider[], limits = { chatPerDay: 3, voicePerDay: 1 }) {
  const usage = new MemoryUsage();
  const notes: string[] = [];
  return { usage, notes, coach: new CoachService(providers, usage, limits, () => Date.parse('2026-11-11T12:00:00Z'), (p, s) => notes.push(`${p}:${s}`)) };
}

const chatBody = (text = 'How am I doing with smoking?') => ({ messages: [{ role: 'user', text }], context: CTX });

test('chat falls through the chain to the first service that answers', async () => {
  const a = broken('gemini');
  const b = good('groq', '{"show":[]}');
  const c = good('cloudflare', '```json\n{"reply":"52 hours clean. **Keep going.**","show":["q1","zz","b9"],"actions":[]}\n```');
  const d = good('mistral', '{"reply":"unused"}');
  const { coach, notes, usage } = service([a, b, c, d]);
  const r = await coach.chat('u1', chatBody());
  assert.equal(r.provider, 'cloudflare');
  assert.equal(r.reply, '52 hours clean. **Keep going.**');
  assert.deepEqual(r.show, ['q1'], 'unknown refs are dropped');
  assert.deepEqual(notes, ['gemini:429', 'groq:422']);
  assert.equal(d.calls, 0);
  assert.deepEqual(r.remaining, { chat: 2, voice: 1, chatLimit: 3, voiceLimit: 1 });
  assert.equal((await usage.get('u1', '2026-11-11')).chat, 1);
});

test('the daily limit is per user, and a failed answer does not use it up', async () => {
  const { coach, usage } = service([good('groq', '{"reply":"ok"}')], { chatPerDay: 2, voicePerDay: 1 });
  await coach.chat('u1', chatBody());
  await coach.chat('u1', chatBody());
  await assert.rejects(coach.chat('u1', chatBody()), (e: any) => e.code === 'daily_limit' && e.status === 429);
  assert.equal((await coach.chat('u2', chatBody())).remaining.chat, 1);

  const down = service([broken('gemini'), broken('groq', 500)]);
  await assert.rejects(down.coach.chat('u1', chatBody()), (e: any) => e.code === 'ai_unavailable' && e.status === 503);
  assert.equal((await down.usage.get('u1', '2026-11-11')).chat, 0, 'refunded');
  assert.equal((await usage.get('u1', '2026-11-11')).chat, 2);

  const none = service([]);
  await assert.rejects(none.coach.chat('u1', chatBody()), (e: any) => e.code === 'ai_unavailable');
  assert.equal((await none.coach.quota('u1')).available, false);
});

test('a voice note is understood directly, or by speech-to-text plus a chat model', async () => {
  const heard = '{"transcript":"I want to stop smoking and drink 5 cups of water a day, meeting next Monday 7pm","actions":[{"type":"quit","name":"Smoking"},{"type":"build","name":"Drink water","perDay":5,"daysPerWeek":null},{"type":"event","title":"Meeting","date":"2026-11-16","time":"19:00"}]}';
  const direct = service([good('gemini', '', { async listen() { return heard; } })]);
  const r = await direct.coach.voice('u1', { audio: AUDIO, mime: 'audio/mp4', context: CTX });
  assert.equal(r.provider, 'gemini');
  assert.equal(r.actions.length, 3);
  assert.deepEqual(r.actions[2], { type: 'event', title: 'Meeting', date: '2026-11-16', time: '19:00' });
  await assert.rejects(direct.coach.voice('u1', { audio: AUDIO, mime: 'audio/mp4', context: CTX }), (e: any) => e.code === 'daily_limit');

  const twoStep = service([
    good('gemini', '', { async listen(): Promise<string> { throw new ProviderError('gemini', 429); } }),
    good('groq', 'nope', { async transcribe() { return ' go to the gym three days a week '; } }),
    good('mistral', '{"actions":[{"type":"build","name":"Gym","perDay":4,"daysPerWeek":3}]}'),
  ]);
  const t = await twoStep.coach.voice('u1', { audio: AUDIO, mime: 'audio/mp4', context: CTX });
  assert.equal(t.provider, 'groq+mistral');
  assert.equal(t.transcript, 'go to the gym three days a week');
  assert.deepEqual(t.actions, [{ type: 'build', name: 'Gym', perDay: 1, daysPerWeek: 3 }]);

  const deaf = service([good('mistral', '{"actions":[]}')]);
  await assert.rejects(deaf.coach.voice('u1', { audio: AUDIO, mime: 'audio/mp4', context: CTX }), (e: any) => e.code === 'ai_unavailable');
  assert.equal((await deaf.usage.get('u1', '2026-11-11')).voice, 0);
});

test('requests and model answers are cleaned before use', () => {
  const ctx = parseContext({ ...CTX, quit: [{ name: 'x'.repeat(200), streakHours: -5 }, { name: '' }], events: [{ title: 'Bad', date: '2026-02-31' }, ...CTX.events], name: 'Ann', email: 'a@b.c' });
  assert.equal(ctx.quit.length, 1);
  assert.equal(ctx.quit[0]!.name.length, 60);
  assert.equal(ctx.quit[0]!.streakHours, 0);
  assert.equal(ctx.events.length, 1);
  assert.ok(!JSON.stringify(ctx).includes('Ann'), 'unknown fields such as a name never pass through');
  assert.throws(() => parseContext({ ...CTX, today: 'yesterday' }));

  assert.deepEqual(cleanActions([
    { type: 'quit', name: '  Vaping\n' }, { type: 'quit' }, { type: 'delete_all' }, 'x',
    { type: 'build', name: 'Read', perDay: 500 }, { type: 'build', name: 'Run', daysPerWeek: 7, perDay: 2 },
    { type: 'event', title: 'Trip', date: '2026-13-01' }, { type: 'event', title: 'Trip', date: '2026-12-01', time: '25:00' },
  ]), [
    { type: 'quit', name: 'Vaping' },
    { type: 'build', name: 'Read', perDay: 99, daysPerWeek: null }, { type: 'build', name: 'Run', perDay: 2, daysPerWeek: null },
    { type: 'event', title: 'Trip', date: '2026-12-01', time: null },
  ]);
  assert.equal(cleanActions(Array.from({ length: 20 }, () => ({ type: 'quit', name: 'A' }))).length, 8);
  assert.equal(readChatAnswer('Just keep going today.', parseContext(CTX))!.reply, 'Just keep going today.');
  assert.equal(readChatAnswer('{"show":["q1"]}', parseContext(CTX)), null);

  const sys = chatSystem(parseContext(CTX), 'q1');
  assert.match(sys, /q1 Smoking: 2d, best 12d, 2 relapses \[user is focused on this one\]/);
  assert.match(sys, /b2 Gym: 1\/3 days this week, not done today, 2 week streak/);
  assert.match(sys, /Exam: 2026-11-14 09:00/);
  assert.match(voiceSystem(parseContext(CTX), true), /Already in the app \(do not add again\): Smoking, Water, Gym\./);
});

test('providers send the right request and read the answer', async () => {
  const seen: { url: string; headers: any; body: any }[] = [];
  const http = (answer: unknown, status = 200) => (async (url: any, init: any) => {
    seen.push({ url: String(url), headers: init.headers, body: typeof init.body === 'string' ? JSON.parse(init.body) : init.body });
    return new Response(JSON.stringify(answer), { status });
  }) as typeof fetch;
  const req = { system: 'SYS', turns: [{ role: 'user' as const, text: 'hi' }, { role: 'assistant' as const, text: 'yo' }, { role: 'user' as const, text: 'ok' }], maxTokens: 100, temperature: 0.5, timeoutMs: 5000 };

  const g = gemini('KEY', 'gemini-flash-latest', http({ candidates: [{ content: { parts: [{ text: 'thinking', thought: true }, { text: '{"reply":"a"}' }] } }] }));
  assert.equal(await g.chat(req), '{"reply":"a"}');
  assert.match(seen[0]!.url, /models\/gemini-flash-latest:generateContent$/);
  assert.equal(seen[0]!.headers['x-goog-api-key'], 'KEY');
  assert.ok(!seen[0]!.url.includes('KEY'), 'the key is never in the URL');
  assert.deepEqual(seen[0]!.body.contents.map((c: any) => c.role), ['user', 'model', 'user']);
  await g.listen!({ base64: AUDIO, mime: 'audio/mp4' }, 'SYS', 5000);
  assert.deepEqual(seen[1]!.body.contents[0].parts[0], { inlineData: { mimeType: 'audio/mp4', data: AUDIO } });

  const o = openAiCompatible('groq', 'https://api.groq.com/openai/v1', 'K2', 'm', { whisperModel: 'w' }, http({ choices: [{ message: { content: 'x' } }], text: 'spoken' }));
  assert.equal(await o.chat(req), 'x');
  assert.equal(seen[2]!.headers.Authorization, 'Bearer K2');
  assert.equal(seen[2]!.body.messages[0].content, 'SYS');
  assert.deepEqual(seen[2]!.body.response_format, { type: 'json_object' });
  assert.equal(await o.transcribe!({ base64: AUDIO, mime: 'audio/mp4' }, 5000), 'spoken');
  assert.match(seen[3]!.url, /audio\/transcriptions$/);

  await assert.rejects(gemini('K', 'm', http({}, 429)).chat(req), (e: any) => e instanceof ProviderError && e.status === 429);
  await assert.rejects(gemini('K', 'm', http({ candidates: [] })).chat(req), ProviderError);
});

test('the chain follows AI_CHAIN and skips services without a key', () => {
  const env = { AI_CHAIN: 'mistral, groq,cloudflare,groq,unknown,gemini', GROQ_API_KEY: 'k', AI: { run: async () => ({}) }, GROQ_MODEL: 'm', GROQ_WHISPER_MODEL: 'w', CF_MODEL: 'c', CF_WHISPER_MODEL: 'cw' } as unknown as Env;
  assert.deepEqual(buildProviders(env).map((p) => p.name), ['groq', 'cloudflare']);
  assert.deepEqual(buildProviders({ ...env, AI_CHAIN: '' }).map((p) => p.name), []);
});

test('coach routes need a sign-in, respect the rate limit and answer over HTTP', async () => {
  const { coach } = service([good('groq', '{"reply":"hi","show":["overview"]}')]);
  const deps = (allowed: boolean): Deps => ({ notion: null as any, coach, verify: async (t) => (t === 'good' ? 'u9' : null), limit: async () => allowed });
  const call = async (path: string, body: unknown, token?: string, allowed = true) => {
    const res = await handle(new Request(`https://w.example${path}`, {
      method: body === undefined ? 'GET' : 'POST',
      headers: { ...(token ? { Authorization: `Bearer ${token}` } : {}), 'Content-Type': 'application/json' },
      body: body === undefined ? undefined : JSON.stringify(body),
    }), deps(allowed));
    return { status: res.status, body: await res.json() as any };
  };
  assert.equal((await call('/v1/coach/chat', chatBody())).status, 401);
  assert.equal((await call('/v1/coach/chat', chatBody(), 'good', false)).status, 429);
  assert.equal((await call('/v1/coach/chat', { messages: [] }, 'good')).status, 400);
  assert.equal((await call('/v1/coach/voice', { audio: 'x', mime: 'audio/mp4', context: CTX }, 'good')).status, 400);
  assert.equal((await call('/v1/coach/voice', { audio: AUDIO, mime: 'text/html', context: CTX }, 'good')).status, 400);
  const ok = await call('/v1/coach/chat', chatBody(), 'good');
  assert.equal(ok.status, 200);
  assert.deepEqual(ok.body.show, ['overview']);
  const q = await call('/v1/coach/quota', undefined, 'good');
  assert.deepEqual(q.body, { remaining: { chat: 2, voice: 1, chatLimit: 3, voiceLimit: 1 }, available: true });
});
