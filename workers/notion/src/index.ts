import { CoachService } from './ai/coach.js';
import { gemini, openAiCompatible, workersAi, type AiBinding, type Provider } from './ai/providers.js';
import { D1UsageStore } from './ai/usage.js';
import { handle, type LimitKind } from './app.js';
import { TokenCipher } from './cipher.js';
import { firebaseKeys, verifyFirebaseIdToken } from './firebaseAuth.js';
import { NotionService } from './notion.js';
import { D1LinkStore } from './store.js';

export interface Env {
  DB: D1Database;
  NOTION_LIMIT: RateLimit;
  START_LIMIT: RateLimit;
  CALLBACK_LIMIT: RateLimit;
  FIREBASE_PROJECT_ID: string;
  APP_RETURN_URL: string;
  NOTION_CLIENT_ID?: string;
  NOTION_CLIENT_SECRET?: string;
  INTEGRATION_TOKEN_KEY?: string;
  // AI coach
  AI: AiBinding;
  AI_LIMIT: RateLimit;
  /** Order to try the AI services in, e.g. "gemini,groq,cloudflare,mistral,openrouter". A name without its key is skipped. */
  AI_CHAIN: string;
  AI_CHAT_PER_DAY: string;
  AI_VOICE_PER_DAY: string;
  GEMINI_MODEL: string;
  GROQ_MODEL: string;
  GROQ_WHISPER_MODEL: string;
  CF_MODEL: string;
  CF_WHISPER_MODEL: string;
  MISTRAL_MODEL: string;
  OPENROUTER_MODEL: string;
  GEMINI_API_KEY?: string;
  GROQ_API_KEY?: string;
  MISTRAL_API_KEY?: string;
  OPENROUTER_API_KEY?: string;
}

/** The AI services that are switched on (named in AI_CHAIN and, where one is needed, given a key), in order. */
export function buildProviders(env: Env): Provider[] {
  const make: Record<string, () => Provider | null> = {
    gemini: () => (env.GEMINI_API_KEY ? gemini(env.GEMINI_API_KEY, env.GEMINI_MODEL) : null),
    groq: () => (env.GROQ_API_KEY ? openAiCompatible('groq', 'https://api.groq.com/openai/v1', env.GROQ_API_KEY, env.GROQ_MODEL, { whisperModel: env.GROQ_WHISPER_MODEL, jsonMode: false }) : null),
    cloudflare: () => (env.AI ? workersAi(env.AI, env.CF_MODEL, env.CF_WHISPER_MODEL) : null),
    mistral: () => (env.MISTRAL_API_KEY ? openAiCompatible('mistral', 'https://api.mistral.ai/v1', env.MISTRAL_API_KEY, env.MISTRAL_MODEL) : null),
    openrouter: () => (env.OPENROUTER_API_KEY ? openAiCompatible('openrouter', 'https://openrouter.ai/api/v1', env.OPENROUTER_API_KEY, env.OPENROUTER_MODEL, { jsonMode: false }) : null),
  };
  const seen = new Set<string>();
  const out: Provider[] = [];
  for (const name of (env.AI_CHAIN ?? '').split(',').map((s) => s.trim().toLowerCase())) {
    if (!name || seen.has(name)) continue;
    seen.add(name);
    const p = make[name]?.();
    if (p) out.push(p);
  }
  return out;
}

function perDay(v: string | undefined, fallback: number): number {
  const n = Number(v);
  return Number.isInteger(n) && n >= 0 && n <= 1000 ? n : fallback;
}

const CALLBACK_PATH = '/v1/integrations/notion/callback';

let cipherFor: { key: string; cipher: Promise<TokenCipher> } | undefined;

function cipher(env: Env): Promise<TokenCipher> | null {
  const key = env.INTEGRATION_TOKEN_KEY;
  if (!key) return null;
  if (cipherFor?.key !== key) cipherFor = { key, cipher: TokenCipher.create(key) };
  return cipherFor.cipher;
}

export default {
  async fetch(request: Request, env: Env): Promise<Response> {
    const ready = env.NOTION_CLIENT_ID && env.NOTION_CLIENT_SECRET ? cipher(env) : null;
    const notion = new NotionService(
      new D1LinkStore(env.DB),
      ready
        ? {
            clientId: env.NOTION_CLIENT_ID!,
            clientSecret: env.NOTION_CLIENT_SECRET!,
            // Cloudflare routes by host, so the Worker's own origin is the callback Notion has registered.
            redirectUri: new URL(CALLBACK_PATH, request.url).toString(),
            appReturnUrl: env.APP_RETURN_URL,
          }
        : null,
      ready ? await ready : null,
    );
    const limiters: Record<LimitKind, RateLimit> = { notion: env.NOTION_LIMIT, start: env.START_LIMIT, callback: env.CALLBACK_LIMIT, ai: env.AI_LIMIT };
    const coach = new CoachService(
      buildProviders(env),
      new D1UsageStore(env.DB),
      { chatPerDay: perDay(env.AI_CHAT_PER_DAY, 10), voicePerDay: perDay(env.AI_VOICE_PER_DAY, 2) },
      () => Date.now(),
      // Which service failed and how: the only thing the AI path ever logs.
      (provider, status) => console.warn('ai_fallback', provider, status),
    );
    return handle(request, {
      notion,
      coach,
      verify: (token) => verifyFirebaseIdToken(token, env.FIREBASE_PROJECT_ID, firebaseKeys()),
      limit: async (kind, key) => (await limiters[kind].limit({ key })).success,
    });
  },
} satisfies ExportedHandler<Env>;
