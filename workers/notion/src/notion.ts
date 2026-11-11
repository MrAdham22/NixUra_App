import type { TokenCipher } from './cipher.js';
import { toBase64Url } from './cipher.js';
import { AppError } from './errors.js';
import type { LinkStore } from './store.js';

export interface NotionConfig {
  clientId: string;
  clientSecret: string;
  /** This Worker's callback URL, exactly as registered in the Notion integration. */
  redirectUri: string;
  /** Where the browser goes back to the app, e.g. nixura://integrations/notion */
  appReturnUrl: string;
}

export interface NotionDatabaseInfo {
  id: string;
  title: string;
  titleProperties: string[];
  dateProperties: string[];
}

export interface NotionEventItem {
  id: string;
  title: string;
  /** Epoch millis for a date with a time; null for a whole-day date (see allDayDate). */
  start: number | null;
  /** YYYY-MM-DD for a whole-day date. */
  allDayDate: string | null;
  lastEditedAt: number;
  archived: boolean;
}

type Fetch = typeof fetch;

const NOTION = 'https://api.notion.com/v1';
const NOTION_VERSION = '2022-06-28';
const STATE_TTL_MS = 10 * 60_000;
const MAX_PAGES = 20;

/**
 * Notion public-integration OAuth and database reads, done for the app (ported from the old Node backend). The
 * client secret and the user's access token stay here (the token encrypted at rest); the app only sees event data.
 * Notion limits apps to about three requests a second, so 429 answers are retried after the time Notion asks for.
 */
export class NotionService {
  constructor(
    private readonly store: LinkStore,
    private readonly cfg: NotionConfig | null,
    private readonly cipher: TokenCipher | null,
    private readonly http: Fetch = (input, init) => fetch(input, init),
    private readonly clock: () => number = () => Date.now(),
    private readonly sleep: (ms: number) => Promise<void> = (ms) => new Promise((r) => setTimeout(r, ms)),
  ) {}

  private ready(): { cfg: NotionConfig; cipher: TokenCipher } {
    if (!this.cfg || !this.cipher) throw new AppError('integration_unavailable', 503, 'Notion is not set up on this server.');
    return { cfg: this.cfg, cipher: this.cipher };
  }

  /** URL to open in the browser; the one-time state ties Notion's answer to this user. */
  async startUrl(uid: string): Promise<string> {
    const { cfg } = this.ready();
    const state = toBase64Url(crypto.getRandomValues(new Uint8Array(24)));
    await this.store.pruneStates(this.clock());
    await this.store.putState(state, uid, this.clock() + STATE_TTL_MS);
    const q = new URLSearchParams({ client_id: cfg.clientId, response_type: 'code', owner: 'user', redirect_uri: cfg.redirectUri, state });
    return `${NOTION}/oauth/authorize?${q.toString()}`;
  }

  /** Handles Notion's redirect; returns where to send the browser (back into the app). Never throws to the browser. */
  async callback(code: string | undefined, state: string | undefined, error: string | undefined): Promise<string> {
    const { cfg, cipher } = this.ready();
    const back = (result: string) => `${cfg.appReturnUrl}?result=${result}`;
    if (!state) return back('error');
    const row = await this.store.takeState(state);
    if (!row || row.expiresAt < this.clock()) return back('error');
    if (error || !code) return back('denied');
    const res = await this.http(`${NOTION}/oauth/token`, {
      method: 'POST',
      headers: {
        Authorization: `Basic ${btoa(`${cfg.clientId}:${cfg.clientSecret}`)}`,
        'Content-Type': 'application/json',
        'Notion-Version': NOTION_VERSION,
      },
      body: JSON.stringify({ grant_type: 'authorization_code', code, redirect_uri: cfg.redirectUri }),
    });
    if (!res.ok) return back('error');
    const body = (await res.json()) as { access_token?: string; workspace_name?: string };
    if (!body.access_token) return back('error');
    await this.store.putLink(row.uid, await cipher.encrypt(body.access_token), (body.workspace_name ?? '').slice(0, 100), this.clock());
    return back('connected');
  }

  async status(uid: string): Promise<{ connected: boolean; workspace: string }> {
    const link = await this.store.getLink(uid);
    return { connected: link !== null, workspace: link?.workspace ?? '' };
  }

  /** Forgets the token. Notion has no revoke call for public integrations: the user removes NixUra in Notion's settings. */
  async disconnect(uid: string): Promise<void> {
    await this.store.deleteLink(uid);
  }

  async databases(uid: string): Promise<NotionDatabaseInfo[]> {
    const out: NotionDatabaseInfo[] = [];
    let cursor: string | undefined;
    for (let i = 0; i < MAX_PAGES; i++) {
      const page = await this.call<{ results: NotionDatabaseObject[]; has_more: boolean; next_cursor: string | null }>(uid, '/search', {
        filter: { property: 'object', value: 'database' },
        page_size: 100,
        ...(cursor ? { start_cursor: cursor } : {}),
      });
      for (const d of page.results) {
        const props = Object.entries(d.properties ?? {});
        out.push({
          id: d.id,
          title: plain(d.title).slice(0, 120),
          titleProperties: props.filter(([, p]) => p.type === 'title').map(([n]) => n),
          dateProperties: props.filter(([, p]) => p.type === 'date').map(([n]) => n),
        });
      }
      if (!page.has_more || !page.next_cursor) break;
      cursor = page.next_cursor;
    }
    return out;
  }

  /** Pages of a database as events. [since] = only pages edited at or after that instant (incremental sync). */
  async query(uid: string, databaseId: string, titleProperty: string, dateProperty: string, since?: number): Promise<{ items: NotionEventItem[]; complete: boolean }> {
    const items: NotionEventItem[] = [];
    let cursor: string | undefined;
    for (let i = 0; i < MAX_PAGES; i++) {
      const page = await this.call<{ results: NotionPageObject[]; has_more: boolean; next_cursor: string | null }>(uid, `/databases/${encodeURIComponent(databaseId)}/query`, {
        page_size: 100,
        ...(since !== undefined ? { filter: { timestamp: 'last_edited_time', last_edited_time: { on_or_after: new Date(since).toISOString() } } } : {}),
        ...(cursor ? { start_cursor: cursor } : {}),
      });
      for (const p of page.results) items.push(toItem(p, titleProperty, dateProperty));
      if (!page.has_more || !page.next_cursor) return { items, complete: true };
      cursor = page.next_cursor;
    }
    return { items, complete: false };
  }

  private async token(uid: string): Promise<string> {
    const { cipher } = this.ready();
    const link = await this.store.getLink(uid);
    if (!link) throw new AppError('integration_not_connected', 409, 'Notion is not connected.');
    return cipher.decrypt(link.tokenEnc);
  }

  private async call<T>(uid: string, path: string, body: unknown): Promise<T> {
    const token = await this.token(uid);
    for (let attempt = 0; ; attempt++) {
      const res = await this.http(`${NOTION}${path}`, {
        method: 'POST',
        headers: { Authorization: `Bearer ${token}`, 'Content-Type': 'application/json', 'Notion-Version': NOTION_VERSION },
        body: JSON.stringify(body),
      });
      if (res.ok) return (await res.json()) as T;
      if (res.status === 401) {
        // Access removed in Notion: forget the dead token so the app asks the user to connect again.
        await this.disconnect(uid);
        throw new AppError('integration_not_connected', 409, 'Notion access was removed. Connect again.');
      }
      if (res.status === 429 && attempt < 3) {
        const wait = Number(res.headers.get('retry-after') ?? '1');
        await this.sleep(Math.min(Math.max(Number.isFinite(wait) ? wait : 1, 1), 10) * 1000);
        continue;
      }
      if (res.status === 404) throw new AppError('not_found', 404, 'That Notion database is not shared with NixUra.');
      throw new AppError('integration_failed', 502, 'Notion did not answer. Try again later.');
    }
  }
}

interface RichText {
  plain_text?: string;
}
interface NotionDatabaseObject {
  id: string;
  title?: RichText[];
  properties?: Record<string, { type: string }>;
}
export interface NotionPageObject {
  id: string;
  archived?: boolean;
  in_trash?: boolean;
  last_edited_time: string;
  properties?: Record<
    string,
    { type: string; title?: RichText[]; rich_text?: RichText[]; date?: { start: string; end?: string | null; time_zone?: string | null } | null }
  >;
}

function plain(parts: RichText[] | undefined): string {
  return (parts ?? []).map((p) => p.plain_text ?? '').join('').trim();
}

export function toItem(p: NotionPageObject, titleProperty: string, dateProperty: string): NotionEventItem {
  const titleProp = p.properties?.[titleProperty];
  const title = plain(titleProp?.title ?? titleProp?.rich_text).slice(0, 200);
  const date = p.properties?.[dateProperty]?.date ?? null;
  let start: number | null = null;
  let allDayDate: string | null = null;
  if (date?.start) {
    if (/^\d{4}-\d{2}-\d{2}$/.test(date.start)) allDayDate = date.start;
    else start = date.time_zone && !/(Z|[+-]\d{2}:\d{2})$/.test(date.start) ? zonedToEpoch(date.start, date.time_zone) : Date.parse(date.start);
    if (start !== null && Number.isNaN(start)) start = null;
  }
  return {
    id: p.id,
    title,
    start,
    allDayDate,
    lastEditedAt: Date.parse(p.last_edited_time) || 0,
    archived: Boolean(p.archived || p.in_trash),
  };
}

/** "2026-11-05T09:00:00.000" in an IANA zone → epoch millis (two passes settle DST edges). */
export function zonedToEpoch(local: string, timeZone: string): number {
  const asUtc = Date.parse(`${local.replace(/\.\d+$/, '')}Z`);
  if (Number.isNaN(asUtc)) return NaN;
  const offsetAt = (t: number) => {
    const parts = new Intl.DateTimeFormat('en-US', {
      timeZone,
      hourCycle: 'h23',
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit',
    }).formatToParts(new Date(t));
    const v = (k: string) => Number(parts.find((x) => x.type === k)?.value);
    return Date.UTC(v('year'), v('month') - 1, v('day'), v('hour'), v('minute'), v('second')) - t;
  };
  try {
    let guess = asUtc - offsetAt(asUtc);
    guess = asUtc - offsetAt(guess);
    return guess;
  } catch {
    return Date.parse(`${local}Z`); // unknown zone: treat as UTC rather than drop the event
  }
}
