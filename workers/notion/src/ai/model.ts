import { AppError } from '../errors.js';

/** What the app tells the coach about the user. No name, email or account id is ever part of it. */
export interface CoachContext {
  /** The user's local date, weekday and time, so "next Monday" can be worked out. */
  today: string;
  weekday: string;
  time: string;
  quit: { ref: string; name: string; streakHours: number; bestHours: number; relapses: number }[];
  build: { ref: string; name: string; today: number; target: number; streak: number; weekDone?: number; weekGoal?: number }[];
  events: { title: string; date: string; time?: string }[];
}

export type CoachAction =
  | { type: 'quit'; name: string }
  | { type: 'build'; name: string; perDay: number; daysPerWeek: number | null }
  | { type: 'event'; title: string; date: string; time: string | null };

export interface ChatBody {
  messages: { role: 'user' | 'assistant'; text: string }[];
  context: CoachContext;
  focus?: string;
}

export interface VoiceBody {
  audio: string;
  mime: string;
  context: CoachContext;
}

const MAX_MESSAGE = 500;
const MAX_TURNS = 12;
const MAX_ITEMS = 30;
const MAX_ACTIONS = 8;
/** About 90 seconds of 32 kbit/s audio, base64 encoded. */
export const MAX_AUDIO_BASE64 = 600_000;
const AUDIO_MIMES = ['audio/mp4', 'audio/m4a', 'audio/aac', 'audio/mpeg', 'audio/wav', 'audio/webm', 'audio/ogg'];

const invalid = () => new AppError('invalid_request', 400, 'The request is not valid.');

const isObj = (v: unknown): v is Record<string, unknown> => typeof v === 'object' && v !== null && !Array.isArray(v);
const str = (v: unknown, max: number): string => (typeof v === 'string' ? v.replace(/[\u0000-\u001f\u007f]+/g, ' ').trim().slice(0, max) : '');
const int = (v: unknown, min: number, max: number, fallback: number): number =>
  typeof v === 'number' && Number.isFinite(v) ? Math.min(max, Math.max(min, Math.round(v))) : fallback;
const list = (v: unknown): unknown[] => (Array.isArray(v) ? v.slice(0, MAX_ITEMS) : []);

const DATE = /^\d{4}-\d{2}-\d{2}$/;
const TIME = /^([01]\d|2[0-3]):[0-5]\d$/;

function realDate(s: string): boolean {
  if (!DATE.test(s)) return false;
  const d = new Date(`${s}T00:00:00Z`);
  return !Number.isNaN(d.getTime()) && d.toISOString().slice(0, 10) === s;
}

export function parseContext(v: unknown): CoachContext {
  if (!isObj(v)) throw invalid();
  const today = str(v.today, 10);
  if (!realDate(today)) throw invalid();
  const time = str(v.time, 5);
  return {
    today,
    weekday: str(v.weekday, 12),
    time: TIME.test(time) ? time : '12:00',
    quit: list(v.quit).filter(isObj).map((q, i) => ({
      ref: `q${i + 1}`,
      name: str(q.name, 60),
      streakHours: int(q.streakHours, 0, 1_000_000, 0),
      bestHours: int(q.bestHours, 0, 1_000_000, 0),
      relapses: int(q.relapses, 0, 100_000, 0),
    })).filter((q) => q.name),
    build: list(v.build).filter(isObj).map((b, i) => ({
      ref: `b${i + 1}`,
      name: str(b.name, 80),
      today: int(b.today, 0, 999, 0),
      target: int(b.target, 1, 99, 1),
      streak: int(b.streak, 0, 100_000, 0),
      ...(typeof b.weekGoal === 'number' ? { weekDone: int(b.weekDone, 0, 7, 0), weekGoal: int(b.weekGoal, 1, 7, 1) } : {}),
    })).filter((b) => b.name),
    events: list(v.events).filter(isObj).map((e) => {
      const t = str(e.time, 5);
      return { title: str(e.title, 80), date: str(e.date, 10), ...(TIME.test(t) ? { time: t } : {}) };
    }).filter((e) => e.title && realDate(e.date)),
  };
}

export function parseChatBody(v: unknown): ChatBody {
  if (!isObj(v) || !Array.isArray(v.messages)) throw invalid();
  const messages = v.messages.slice(-MAX_TURNS).filter(isObj).map((m) => ({
    role: m.role === 'assistant' ? ('assistant' as const) : ('user' as const),
    text: str(m.text, m.role === 'assistant' ? 1200 : MAX_MESSAGE),
  })).filter((m) => m.text);
  if (messages.length === 0 || messages[messages.length - 1]!.role !== 'user') throw invalid();
  const focus = str(v.focus, 4);
  return { messages, context: parseContext(v.context), ...(/^q\d{1,2}$/.test(focus) ? { focus } : {}) };
}

export function parseVoiceBody(v: unknown): VoiceBody {
  if (!isObj(v) || typeof v.audio !== 'string') throw invalid();
  const audio = v.audio;
  if (audio.length < 200 || audio.length > MAX_AUDIO_BASE64 || !/^[A-Za-z0-9+/]+={0,2}$/.test(audio)) throw invalid();
  const mime = str(v.mime, 30).toLowerCase();
  if (!AUDIO_MIMES.includes(mime)) throw invalid();
  return { audio, mime, context: parseContext(v.context) };
}

/** Pulls the first JSON object out of a model's answer (some models wrap it in text or a code fence). */
export function extractJson(text: string): Record<string, unknown> | null {
  const start = text.indexOf('{');
  const end = text.lastIndexOf('}');
  if (start < 0 || end <= start) return null;
  try {
    const v = JSON.parse(text.slice(start, end + 1));
    return isObj(v) ? v : null;
  } catch {
    return null;
  }
}

/** Keeps only well-formed things to add; a model's answer is never trusted as is. */
export function cleanActions(v: unknown): CoachAction[] {
  const out: CoachAction[] = [];
  for (const a of Array.isArray(v) ? v : []) {
    if (!isObj(a) || out.length >= MAX_ACTIONS) continue;
    if (a.type === 'quit') {
      const name = str(a.name, 60);
      if (name) out.push({ type: 'quit', name });
    } else if (a.type === 'build') {
      const name = str(a.name, 80);
      const week = typeof a.daysPerWeek === 'number' ? int(a.daysPerWeek, 1, 7, 7) : null;
      if (name) out.push({ type: 'build', name, perDay: week !== null && week < 7 ? 1 : int(a.perDay, 1, 99, 1), daysPerWeek: week !== null && week < 7 ? week : null });
    } else if (a.type === 'event') {
      const title = str(a.title, 80);
      const date = str(a.date, 10);
      const time = str(a.time, 5);
      if (title && realDate(date)) out.push({ type: 'event', title, date, time: TIME.test(time) ? time : null });
    }
  }
  return out;
}

export interface CoachAnswer {
  reply: string;
  show: string[];
  actions: CoachAction[];
}

/** Reads a chat answer; plain text (no JSON) is accepted as the reply itself. */
export function readChatAnswer(text: string, ctx: CoachContext): CoachAnswer | null {
  const json = extractJson(text);
  if (!json) {
    const reply = str(text, 1200);
    return reply && !reply.startsWith('{') ? { reply, show: [], actions: [] } : null;
  }
  const reply = typeof json.reply === 'string' ? json.reply.trim().slice(0, 1200) : '';
  if (!reply) return null;
  const known = new Set(['overview', ...ctx.quit.map((q) => q.ref), ...ctx.build.map((b) => b.ref)]);
  const show = [...new Set((Array.isArray(json.show) ? json.show : []).filter((r): r is string => typeof r === 'string' && known.has(r)))].slice(0, 2);
  return { reply, show, actions: cleanActions(json.actions) };
}

export interface VoiceAnswer {
  transcript: string;
  actions: CoachAction[];
}

export function readVoiceAnswer(text: string, transcript?: string): VoiceAnswer | null {
  const json = extractJson(text);
  if (!json) return null;
  const heard = transcript ?? (typeof json.transcript === 'string' ? json.transcript.trim().slice(0, 1000) : '');
  return { transcript: heard, actions: cleanActions(json.actions) };
}
