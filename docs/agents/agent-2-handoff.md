# Agent 2 handoff: accounts (backend, sign-in, email OTP, password security)

## Verification state: read this first
**Update 2026-11-02:** the backend and Kotlin client have since been built and tested, and first-run bugs were fixed. See "First real build of Wave 1" in `docs/DEVELOPMENT_STATUS.md`. The table below describes the original session.

This session had **no network, no Gradle/kotlinc, no Docker, no emulator**. Therefore:

| Part | State |
|---|---|
| Backend `npm install`, `tsc`, `npm test` (43 tests written) | **Never run.** Dependencies could not be installed. |
| Backend modules with no dependencies (`passwordPolicy`, `templates`, `openapi`, `migrate`) | **Loaded and exercised** with Node 22: the password-policy, breach-check (k-anonymity request shape) and email-template assertions from `test/unit.test.ts` passed. |
| Kotlin client (all of it) | **Not compiled.** Read through for signature/visibility problems only. Unit tests written (`AuthValidationTest`, `AuthorizedApiTest`, `SessionManagerTest`, `AuthErrorMapperTest`) but never run. |
| iOS Keychain code | Highest-risk file (`iosMain/.../IosAuthPlatform.kt`, CoreFoundation casts). Needs a Mac. |
| Gradle: Ktor 3.3.1, kotlinx-serialization 1.9.0 | Versions chosen from memory of a recent stable line; **check them against Kotlin 2.4.20** and bump to the latest 3.x if resolution fails. |

First commands for the owner, in order:
```
cd backend && npm install && npm run typecheck && npm test
./gradlew :shared:testAndroidHostTest
./gradlew :androidApp:assembleDebug
```
Expect a few first-compile fixes. The most likely ones: Ktor/serialization versions; `ContentType` autofill API (only in `presentation/auth/AutofillSupport.kt`: if it does not compile, delete the body of `autofill()`); `KeyboardType.NumberPassword`; the iOS Keychain casts.

## What was built

### Backend (`/backend`, Node 22 + TypeScript + Fastify + PostgreSQL)
Register, verify email (OTP), resend, login, refresh, logout, logout-all, forgot/reset password, change password, delete account, list/revoke device sessions, `GET/PATCH /v1/me`, `/health`, `/openapi.json`. Dockerfile + compose, `.env.example`, SQL migrations (`migrations/001_accounts.sql`), `backend/README.md`.
- **Passwords:** Argon2id (env-tunable, OWASP minimum default), transparent rehash, policy (10-128 chars, common-password list, not the email, optional HIBP k-anonymity check).
- **OTP:** CSPRNG 6 digits, stored as HMAC-SHA256 (pepper, bound to user + purpose), 10 min, 5 attempts (counter committed even on failure), 60 s cooldown, 5/day, single use, constant-time compare.
- **Sessions:** 15 min HS256 JWT checked against a live server-side session; rotating 256-bit refresh tokens stored as SHA-256; **reuse of a rotated token revokes the session** and writes an audit event.
- **Abuse:** anti-enumeration (same responses; unknown emails lock out identically via hashed-email `login_attempts`; hashing work done for unknown emails), progressive lockout (30 s doubling to 15 min after 5 failures), per-IP and per-email limits, 16 KB body limit, helmet, closed CORS, redacted logs, audit log with no PII.
- **Takeover defence:** a pending (unverified) registration is overwritten by the next one, so squatting a victim's address does not keep the squatter's password.
- **Deletion:** removes the user (cascade), erases their audit rows, leaves one anonymous `account_deleted` event.

### Client (KMP `:shared`, Android + iOS)
- Domain (`domain/auth`): `AuthRepository`, `SessionManager`, typed `AuthResult`/`AuthError`, validators mirroring the server policy, strength meter, `AccountProfileSync`.
- Data (`data/auth`): Ktor transport (https-only unless debug cleartext), error mapper, `SessionManagerImpl`, **`AuthorizedApi` = single-flight token refresh** (parallel 401s share one refresh; a wrong-password 401 does not refresh; refresh rejected = clean local logout; network failure keeps the session), `AuthRepositoryImpl`.
- Secure storage: Android Keystore AES-256-GCM (`AndroidSecureStore`), iOS Keychain (`KeychainSecureStore`).
- UI: Welcome, Register, Verify OTP (6 boxes, auto-advance, digits-only paste/IME suggestion, resend timer), Login, Forgot, Reset, Account hub, Change password, Delete account (password + confirm), Devices, Privacy/Terms viewers. Show/hide password, strength meter, inline errors, 48 dp targets, `Role.Button` semantics, passwords never in `rememberSaveable`.
- Entry points: first-launch **gate** after onboarding (with "Continue without an account"), Settings > Account. Profile name/age sync to the account; email is read-only while signed in.
- Android hardening: INTERNET only, `network_security_config` (no cleartext; debug-only override for `10.0.2.2`/`localhost`), backup rules excluding the encrypted token file, ProGuard rules for serialization/Ktor.
- Legal: `SECURITY.md`, `docs/legal/{PRIVACY_POLICY,TERMS_OF_USE,ACCOUNT_DELETION,PLAY_DATA_SAFETY}.md`; in-app text is generated from the markdown by `python3 tools/gen_legal_text.py` (re-run after editing).

## Decisions that differ from the task file (and why)
1. **Ktor instead of Retrofit/OkHttp:** the app is Kotlin Multiplatform with an iOS target (AGENTS.md); Retrofit is JVM-only. The Authenticator behaviour is reimplemented in `AuthorizedApi` with unit tests.
2. **Strings** live in `Strings.kt` (repo rule), as one additive block, not `strings_auth.xml`.
3. **Guest mode kept.** The app stays usable without an account, so nobody with existing local data is locked out on update.
4. **`allowBackup` stays true** (habits keep backing up); only the encrypted token file is excluded. Disabling backup wholesale would silently drop users' habit backups.
5. **Own limiter/OpenAPI** instead of `@fastify/rate-limit`/`@fastify/swagger` (fewer dependencies, injectable clock for tests). The limiter is per process (see limitations).
6. **No Room change.** Contract below uses the secure store.

## Contract for other agents
- Consume only `AuthRepository`, `SessionManager` (`state`, `currentUserId`) from Koin.
- **Agent 3 (sync): guest-data handoff.** After a sign-in, `SessionManager.pendingGuestHandoff()` returns `GuestDataHandoff(userId, previousUserId)`. `previousUserId == null`: nobody was signed in on this device before, local data is guest data: upload/adopt it. `previousUserId != null` (different account): clear local data **before** loading the new account; never upload it. Then call `markGuestHandoffHandled()`. The same user signing back in returns null.
- Authenticated backend calls from the app should go through `AuthorizedApi` (internal to `:shared`); expose it via a public wrapper if Agent 3/4 need it, rather than copying token logic.
- Backend: add routes in `backend/src/http/app.ts` (or register a plugin), protect with the `requireAuth` pre-handler, add numbered files in `backend/migrations/`. `users.id` (UUID) is the owner key for every per-user table; enforce `user_id` in every query.
- Agent 6: analytics events `login_completed`, `account_deleted` should be logged from the success paths (`AuthViewModels.kt`, `AppShell` gate); Privacy Policy and Play checklist have `<!-- AGENT-n -->` anchors for your additions.

## Owner setup
1. Provision Postgres; set `DATABASE_URL`, `JWT_SECRET`, `OTP_PEPPER` (`openssl rand -base64 48`), `NODE_ENV=production` (see `backend/README.md`).
2. Email: create a Resend account, verify a sending domain, set `EMAIL_PROVIDER=resend`, `RESEND_API_KEY`, `EMAIL_FROM`.
3. Deploy behind HTTPS; set `TRUST_PROXY_HOPS`.
4. Point the apps at it: Android `-PnixuraApiBaseUrl=https://api.yourdomain.com` (release; default is a placeholder that will not resolve), debug uses `http://10.0.2.2:8080` (`-PnixuraDebugApiBaseUrl` to change); iOS `NixUraApiBaseUrl` in `iosApp/iosApp/Info.plist`.
5. Fill the bracketed fields in `docs/legal/*.md` (name/address, dates, retention, governing law), have a lawyer review, re-run `tools/gen_legal_text.py`, host `PRIVACY_POLICY.md` and `ACCOUNT_DELETION.md` at public URLs, and enter them in Play Console (privacy policy, account deletion link, Data safety form using `PLAY_DATA_SAFETY.md`). Provide a verified test account for Play review.
6. Real devices: test the OTP email arriving (spam folder, domain SPF/DKIM) before release.

## Known limitations
- Rate limiting is per process; scale-out needs a shared store.
- Strict reuse detection: if a refresh *response* is lost in transit, that device is signed out at its next refresh.
- Logging out while offline ends the session locally only; the server session expires with its refresh token (60 days) or via Devices.
- Keychain items survive an iOS reinstall, so a reinstalled app may still be signed in.
- Habit names stay on-device (no sync yet), so the Privacy Policy states exactly that; Agent 3 must update it.
- English only (strings are centralised; Arabic/RTL not done in these screens' layouts beyond Compose defaults).
- No 2FA/passkeys, no Credential Manager integration (autofill hints only), no instrumented/UI tests (repo has no Compose test setup).
- The `docs/legal` texts are drafts, not legal advice.

## Branch note
`agent-2/accounts` starts with `chore: snapshot of uncommitted work already in the project zip (not Agent 2)`. That commit holds ~78 files (Agent 1's work and earlier edits) that were uncommitted in the zip; it exists only so Agent 2's commits are reviewable. Drop or rebase it if Agent 1's branch lands first.
