# NixUra backend (accounts API)

Fastify + TypeScript + PostgreSQL. Owned by Agent 2; later agents add endpoints/tables here (sync, AI, integrations)
with their own numbered files in `migrations/` and their own route modules registered in `src/http/app.ts`.

## Run locally
```bash
cp .env.example .env            # then edit secrets
docker compose up --build       # Postgres + API on :8080; verification codes are printed in the api logs
# or, without Docker (needs Postgres and DATABASE_URL):
npm install && npm run migrate && npm run dev
```
Android emulator reaches the host at `http://10.0.2.2:8080`.

## Test
```bash
npm install
npm run typecheck
npm test            # node:test + tsx; database is PGlite (real Postgres in WASM), no Docker needed
```

## Deploy (owner)
1. Provision managed PostgreSQL 14+ and put its URL in `DATABASE_URL` (TLS on).
2. Set `NODE_ENV=production`, `JWT_SECRET`, `OTP_PEPPER` (each `openssl rand -base64 48`), `EMAIL_PROVIDER=resend`,
   `RESEND_API_KEY`, `EMAIL_FROM` (a domain you verified in Resend). The server refuses to start in production with the
   console mailer or placeholder secrets.
3. Run behind HTTPS (reverse proxy / platform TLS) and set `TRUST_PROXY_HOPS` to the number of proxies so rate limits see client IPs.
4. Build and run the container (`Dockerfile`); it applies migrations on start and exposes `GET /health`.
5. Put the public base URL into the Android app (`NIXURA_API_BASE_URL`, see the Agent 2 handoff).

## Design notes
- **Passwords:** Argon2id (`ARGON2_*` env, defaults = OWASP minimum 19 MiB / t=2 / p=1). Hashes are upgraded transparently at login when parameters tighten. Policy: 10-128 chars, common-password denylist, not the email, optional Pwned Passwords k-anonymity check (`HIBP_CHECK=true`).
- **OTP:** `crypto.randomInt` 6 digits, stored as HMAC-SHA256 (server pepper, bound to user + purpose), 10 min, 5 attempts, 60 s resend cooldown, 5 codes/day, single use, constant-time compare.
- **Tokens:** 15 min HS256 JWT (`sid` = session id; every request also checks the session is not revoked) + 256-bit refresh tokens stored as SHA-256, rotated on each use. Replaying a rotated token revokes that session (family) and writes an audit event.
- **Anti-enumeration:** register / resend / forgot return the same response whether or not the email exists; unknown emails lock out like real ones; Argon2 work is done for unknown emails too.
- **Rate limits:** in-process fixed windows per IP and per email (`src/http/rateLimiter.ts`). For several instances replace it with a shared store behind the same interface.
- **Audit log:** event names + user id only (no emails/IPs/tokens). Deleting an account erases its audit rows and leaves one anonymous `account_deleted` event.
- **Logging:** pino with `Authorization`/`Cookie` redacted; error logs carry messages only, never bodies, codes or addresses.
- API reference: `GET /openapi.json`.
