import { AppError } from './errors.js';
import type { NotionService } from './notion.js';

export type LimitKind = 'notion' | 'start' | 'callback';

export interface Deps {
  notion: NotionService;
  /** Firebase ID token → user id, or null. */
  verify: (idToken: string) => Promise<string | null>;
  /** true = allowed, false = over the limit. */
  limit: (kind: LimitKind, key: string) => Promise<boolean>;
}

const MAX_BODY_BYTES = 8 * 1024;
const CALLBACK = '/v1/integrations/notion/callback';

/** The same routes and answers as the old backend's Notion part, so the app only needs the new address. */
export async function handle(req: Request, deps: Deps): Promise<Response> {
  const url = new URL(req.url);
  const route = `${req.method} ${url.pathname}`;
  try {
    if (route === 'GET /health') return json({ ok: true });

    // Public: the browser arrives here from Notion. The one-time state identifies the user; the answer is a redirect.
    if (route === `GET ${CALLBACK}`) {
      if (!(await deps.limit('callback', req.headers.get('cf-connecting-ip') ?? 'unknown'))) throw tooMany();
      const q = url.searchParams;
      const code = short(q.get('code'), 500);
      const state = short(q.get('state'), 100);
      const error = short(q.get('error'), 100);
      const target = await deps.notion.callback(code, state, error);
      return new Response(null, { status: 302, headers: { Location: target, 'Cache-Control': 'no-store', 'Referrer-Policy': 'no-referrer' } });
    }

    const known = ROUTES.has(route);
    if (!known) throw new AppError('not_found', 404, 'Not found.');
    const uid = await signedIn(req, deps);

    switch (route) {
      case 'GET /v1/integrations':
        return json({ notion: await deps.notion.status(uid) });
      case 'POST /v1/integrations/notion/start':
        if (!(await deps.limit('start', uid))) throw tooMany();
        return json({ url: await deps.notion.startUrl(uid) });
      case 'DELETE /v1/integrations/notion':
        await deps.notion.disconnect(uid);
        return new Response(null, { status: 204, headers: baseHeaders() });
      case 'GET /v1/integrations/notion/databases':
        if (!(await deps.limit('notion', uid))) throw tooMany();
        return json({ databases: await deps.notion.databases(uid) });
      case 'POST /v1/integrations/notion/query': {
        const b = parseQuery(await readJson(req));
        if (!(await deps.limit('notion', uid))) throw tooMany();
        return json(await deps.notion.query(uid, b.databaseId, b.titleProperty, b.dateProperty, b.since));
      }
    }
    throw new AppError('not_found', 404, 'Not found.');
  } catch (e) {
    if (e instanceof AppError) return json({ error: { code: e.code, message: e.message } }, e.status);
    // Only the error's kind is logged: never tokens, user ids or Notion data.
    console.error('unhandled', e instanceof Error ? e.name : typeof e);
    return json({ error: { code: 'internal', message: 'Something went wrong. Try again later.' } }, 500);
  }
}

const ROUTES = new Set([
  'GET /v1/integrations',
  'POST /v1/integrations/notion/start',
  'DELETE /v1/integrations/notion',
  'GET /v1/integrations/notion/databases',
  'POST /v1/integrations/notion/query',
]);

async function signedIn(req: Request, deps: Deps): Promise<string> {
  const auth = req.headers.get('authorization') ?? '';
  const m = /^Bearer (\S+)$/.exec(auth);
  const uid = m?.[1] ? await deps.verify(m[1]) : null;
  if (!uid) throw new AppError('unauthorized', 401, 'Please sign in again.');
  return uid;
}

async function readJson(req: Request): Promise<unknown> {
  const declared = Number(req.headers.get('content-length') ?? '0');
  if (declared > MAX_BODY_BYTES) throw new AppError('too_large', 413, 'Request too large.');
  const text = await req.text();
  if (text.length > MAX_BODY_BYTES) throw new AppError('too_large', 413, 'Request too large.');
  try {
    return JSON.parse(text);
  } catch {
    throw invalid();
  }
}

function parseQuery(v: unknown): { databaseId: string; titleProperty: string; dateProperty: string; since?: number } {
  if (typeof v !== 'object' || v === null) throw invalid();
  const o = v as Record<string, unknown>;
  const text = (x: unknown, max: number) => (typeof x === 'string' && x.length >= 1 && x.length <= max ? x : null);
  const databaseId = text(o.databaseId, 100);
  const titleProperty = text(o.titleProperty, 200);
  const dateProperty = text(o.dateProperty, 200);
  if (!databaseId || !titleProperty || !dateProperty) throw invalid();
  const since = o.since;
  if (since !== undefined && since !== null && !(typeof since === 'number' && Number.isInteger(since) && since >= 0)) throw invalid();
  return { databaseId, titleProperty, dateProperty, ...(typeof since === 'number' ? { since } : {}) };
}

function short(v: string | null, max: number): string | undefined {
  if (v === null) return undefined;
  if (v.length > max) throw invalid();
  return v;
}

const invalid = () => new AppError('invalid_request', 400, 'The request is not valid.');
const tooMany = () => new AppError('rate_limited', 429, 'Too many requests. Try again in a minute.');

function baseHeaders(): Record<string, string> {
  return { 'Cache-Control': 'no-store', 'X-Content-Type-Options': 'nosniff', 'Referrer-Policy': 'no-referrer' };
}

function json(body: unknown, status = 200): Response {
  return new Response(JSON.stringify(body), { status, headers: { ...baseHeaders(), 'Content-Type': 'application/json; charset=utf-8' } });
}
