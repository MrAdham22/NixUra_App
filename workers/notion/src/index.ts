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
    const limiters: Record<LimitKind, RateLimit> = { notion: env.NOTION_LIMIT, start: env.START_LIMIT, callback: env.CALLBACK_LIMIT };
    return handle(request, {
      notion,
      verify: (token) => verifyFirebaseIdToken(token, env.FIREBASE_PROJECT_ID, firebaseKeys()),
      limit: async (kind, key) => (await limiters[kind].limit({ key })).success,
    });
  },
} satisfies ExportedHandler<Env>;
