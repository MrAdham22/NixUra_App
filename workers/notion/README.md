# NixUra Notion Worker

The server half of "link your Notion calendar". It runs on Cloudflare Workers (free plan) and replaces the Notion part of the old Node `/backend`, with the same routes and answers, so the app only needed a new address.

Live at **https://nixura-notion.nixura-notion-worker.workers.dev** (deployed 2026-11-11).

## What it does

| Route | Who | What |
|---|---|---|
| `GET /health` | anyone | `{"ok":true}` |
| `GET /v1/integrations` | signed in | `{"notion":{"connected","workspace"}}` |
| `POST /v1/integrations/notion/start` | signed in | Notion's consent URL with a one-time state (10 min) |
| `GET /v1/integrations/notion/callback` | browser, from Notion | swaps the code for a token, stores it encrypted, redirects to `nixura://integrations/notion?result=connected/denied/error` |
| `DELETE /v1/integrations/notion` | signed in | forgets the token (the app also calls this before deleting an account) |
| `GET /v1/integrations/notion/databases` | signed in | the databases the user shared, with their title and date properties |
| `POST /v1/integrations/notion/query` | signed in | pages of one database as events; `since` = only pages edited after it |

**"Signed in"** means a valid Firebase ID token in `Authorization: Bearer …`. The Worker checks it with Google's public keys: RS256 signature, issuer and audience = project `nixura`, not expired, sign-in time in the past. A bad token gets `401`, and the app then retries once with a fresh token.

**Safety:**
- The Notion client secret and the users' Notion tokens never reach the phone.
- Tokens are stored with AES-256-GCM (key `INTEGRATION_TOKEN_KEY`) in the D1 table `notion_links`, keyed by the Firebase user id.
- Logs only ever contain an error's kind, never tokens, user ids or Notion data.
- Rate limits (Cloudflare's Rate Limiting binding, counted per location): 60 Notion calls a minute per user, 10 connects a minute per user, and 30 callbacks a minute per IP.

## Files

- `src/index.ts`: wires Cloudflare's bindings (D1, rate limits, secrets) to the code.
- `src/app.ts`: the routes, input checks and error answers (`{"error":{"code","message"}}`).
- `src/firebaseAuth.ts`: checks the Firebase ID token.
- `src/notion.ts`: the Notion OAuth flow and reads, ported from `backend/src/integrations/notionService.ts`.
- `src/cipher.ts`: AES-256-GCM in the same `v1.nonce.tag.data` format as the old backend.
- `src/store.ts`: the D1 queries. The one-time state is taken with a single `DELETE … RETURNING`, so it can't be used twice.
- `migrations/0001_notion.sql`: the tables `notion_links` and `oauth_states`.
- `test/worker.test.ts`: 10 tests (OAuth round trip, single-use state, per-user data, revoked access, token checks, bad input, limits, dates, cipher).

## Commands

```bash
npm install
npm test                    # node --test, no Cloudflare account needed
npm run typecheck
npx wrangler dev            # local copy on http://localhost:8787 (run npm run db:migrate:local first)
npx wrangler deploy         # publish (needs `npx wrangler login` once on this computer)
npm run db:migrate:remote   # apply a new migration to the live database
npx wrangler tail           # live logs
```

## Secrets

Set with `npx wrangler secret put NAME`. They are never kept in this folder or in git.

| Name | State |
|---|---|
| `INTEGRATION_TOKEN_KEY` | set (32 random bytes, generated straight into Cloudflare; nobody has seen it). Changing it makes the stored tokens unreadable, so everyone would have to connect Notion again. |
| `NOTION_CLIENT_ID`, `NOTION_CLIENT_SECRET` | set on 2026-11-11, from the Notion OAuth connection "NixUra"; see the main README, "What you need to do: Notion". Without them, connecting answers `503 integration_unavailable`. |
