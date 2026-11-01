# Agent 2 plan: accounts (backend, login, email OTP, password security)

Written before coding, from `NixUra_Agent_Tasks.md` section 6 and the repo docs (README, AGENTS, PROJECT_CONTEXT, Agent 1 handoff).

## Environment honesty
This session has **no network, no Gradle, no Docker, no emulator**. `npm install` cannot run, so the backend and its tests are
written carefully but **not executed**; the Kotlin client is **not compiled**. Everything is marked unverified in the handoff,
with the exact commands for the owner. Nothing is claimed as passing unless it ran.

## Conflicts between the task file and the repo (surfaced, not silently resolved)
| Task file says | Repo reality | Decision |
|---|---|---|
| Retrofit/OkHttp + Authenticator | KMP `:shared` (Android + iOS), Koin, Compose Multiplatform, no Retrofit | Use **Ktor client** in `commonMain` (works on both platforms; Retrofit is JVM-only). Same behavior: transparent refresh, single-flight, queued concurrent requests. |
| Android Keystore encrypted storage | iOS is also a target (AGENTS.md: feature parity) | `SecureTokenStore` as `expect`/Koin port: Android = Keystore AES-GCM key + app-private file; iOS = Keychain. |
| `strings_auth.xml` resources | All UI text lives in `presentation/strings/Strings.kt` (plain `open class`, never constructor params) | New strings go in a **separate additive file** `AuthStrings` extension block is not possible without touching Strings.kt, so one additive `open val` block is appended to `Strings.kt`, same as Agent 1 did. |
| `AGENTS.md`: "offline only, no accounts" | Owner explicitly requested accounts in the task file | Treated as approval. App stays fully usable offline; AGENTS.md/README scope line updated. |
| Room untouched by me | Agent 3 owns schema | No Room change. Guest-data hand-over uses a flag in secure storage (contract below). |

## Backend (`/backend`)
Node 22 + TypeScript + **Fastify** + **PostgreSQL** (`pg`), `argon2` (Argon2id), `jose` (JWT access tokens), `zod` validation,
`@fastify/helmet`, `@fastify/cors`, `@fastify/rate-limit`, `@fastify/swagger` (OpenAPI). Email behind `EmailSender`
(Resend implementation + console implementation for dev/tests). Tests: `node:test` + `tsx`, DB via **PGlite** (real Postgres in WASM, no Docker needed).
Plain SQL migrations (`migrations/*.sql`) with a tiny runner (additive; Agent 3/4/5 add their own files).

Tables: `users`, `otp_codes`, `sessions` (device sessions), `refresh_tokens` (hashed, family id, rotated_at), `audit_log`, `login_attempts`.

Endpoints (`/v1/auth/*`): register, verify-email, resend-otp, login, refresh, logout, logout-all, forgot-password, reset-password,
change-password, delete-account, sessions (list), sessions/:id (revoke), me (get/patch profile name+age), plus `/health`.

Security choices: Argon2id (m=19 MiB, t=2, p=1 default, env-tunable); password policy (min 10, max 128, common-password denylist, not equal to email);
OTP = `crypto.randomInt` 6 digits, stored as HMAC-SHA256 (pepper from env), 10 min expiry, 5 attempts, 60 s resend cooldown, 5/day cap,
single use, `timingSafeEqual`; access token JWT 15 min (HS256 secret from env, `jti`, `sid`); refresh = 256-bit random, stored SHA-256, rotated on each use,
reuse of a rotated token revokes the whole family; generic responses to prevent enumeration (register for existing email returns the same shape and mails the owner a notice);
per-IP and per-account rate limits + progressive lockout; audit log of security events without PII beyond user id.

## Android/iOS client (KMP `:shared`)
- Domain: `AuthRepository`, `SessionManager` (`currentUserId: Flow<String?>`, `sessionState`), `AuthResult`/typed `AuthError`, use cases, pure validators (email, password policy, strength meter, OTP format) with tests.
- Data: `AuthApi` (Ktor), `AuthRepositoryImpl`, `TokenRefresher` (mutex single-flight), `SecureTokenStore` (expect/actual or Koin port).
- Presentation: `AuthViewModel`s + screens: Welcome, Register, Verify OTP (6 boxes, auto-advance, paste, resend timer), Login, Forgot/Reset, Change password, Delete account; Legal screens (Privacy/Terms) and links from Settings + Register.
- **Root gate**: `AppRoot` gets one small commit: after onboarding, if no session and the user has not chosen "continue offline" the Welcome flow shows; the app remains usable offline (guest mode) so existing users never lose access to local data.
- Profile name/email/age connected to the account (`/me`); email read-only.
- Manifest: `allowBackup=false` + `dataExtractionRules`, network security config (no cleartext, debug override only for emulator host), `INTERNET` permission.
- Guest-data contract for Agent 3: on first successful login/register `SessionManager` stores `guestDataPending=true` + `guestDataOwnerUserId` in the secure store; Agent 3 reads and clears it after uploading. Documented in the handoff.

## Docs
`SECURITY.md`, `docs/legal/PRIVACY_POLICY.md`, `docs/legal/TERMS_OF_USE.md`, `docs/legal/ACCOUNT_DELETION.md`, `docs/legal/PLAY_DATA_SAFETY.md`, `backend/README.md`, `docs/agents/agent-2-handoff.md`, README docs-index line.

## Risks
- Nothing compiled/ran here (see above). Highest-risk spots: Gradle dependency additions (Ktor versions vs Kotlin 2.4.20), `argon2` native build on the owner's host, PGlite/pg compatibility in tests.
- Large scope; order of work: backend + tests, docs, client domain/data, client UI, gate/manifest, handoff.

## Testing
Backend: unit tests (password policy, OTP, tokens, hashing) + integration tests over Fastify `inject` with PGlite and a capturing `EmailSender`: every flow incl. expired/used/wrong OTP, rate limits, refresh rotation + reuse detection.
Client: `commonTest` for validators, strength meter, token refresher single-flight logic (with fakes), session manager state machine.
