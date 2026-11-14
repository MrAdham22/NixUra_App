# Security

## Reporting a vulnerability
Email **adhamelgendydev@gmail.com** with "SECURITY" in the subject. Include steps to reproduce and the affected version.
Please do not open public issues for vulnerabilities and do not access other users' data. You will get an acknowledgement
within 5 working days. We ask for 90 days to fix before public disclosure.

## Threat model summary
| Asset | Threat | Mitigation |
|---|---|---|
| Passwords | Database leak, brute force, credential stuffing | Argon2id (m=19 MiB, t=2, p=1 by default, env-tunable), per-hash random salt, never logged or returned; policy (10+ chars, common-password denylist, optional breached-password check via k-anonymity); progressive lockout (30 s doubling to 15 min after 5 failures); per-IP and per-account rate limits. |
| Email ownership | Fake or pre-registered addresses, OTP guessing | 6-digit CSPRNG code, stored only as HMAC-SHA256 (server pepper, bound to user + purpose), 10 min expiry, 5 attempts, 60 s resend cooldown, 5 codes/day, single use, constant-time compare. Account unusable until verified. A pending registration is overwritten by a newer one, so a squatter cannot keep a victim's address. |
| Sessions | Token theft, replay | 15 min access JWT bound to a server-side session (revoking a session takes effect immediately); 256-bit refresh tokens stored as SHA-256, rotated on every use; replaying a rotated token revokes the session and is audited. Users can list and revoke devices; password change/reset signs out other devices. |
| Account enumeration | Probing which emails are registered | Register / resend / forgot return identical responses; unknown emails lock out like real ones; password hashing work is done for unknown emails. |
| Data in transit | Interception | HTTPS only (server behind TLS; HSTS in production; app network-security config forbids cleartext). |
| Data on device | Malware, backups, rooted devices | Tokens in Android Keystore-backed encrypted storage (iOS Keychain); app backup disabled for secrets (`allowBackup=false`, data-extraction rules). |
| Secrets | Leak via repo/APK | No secrets in the repo or app. Server secrets come from environment variables; production refuses placeholder secrets and the dev console mailer. |
| Logs | PII leakage | Structured logs without request bodies, emails, codes or tokens; `Authorization` redacted; audit log holds event names and user ids only. |
| Abuse | Flooding, oversized input | Zod validation on every body, 16 KB body limit, security headers (helmet), CORS closed by default. |

## Known limitations 
- Rate limiting is per server process. Running several instances needs a shared limiter (Redis) behind `RateLimiter`.
- Access tokens use a symmetric (HS256) secret: only this backend can verify them. Rotating `JWT_SECRET` signs everyone out.
- A refresh response lost in transit (client never saw the new token) will log that device out on the next refresh, because strict reuse detection cannot tell a replay from a retry.
- No 2FA/passkeys yet; email OTP proves ownership at sign-up and reset only.

## Operational checklist 
Managed Postgres with TLS and backups; strong random `JWT_SECRET` / `OTP_PEPPER`; verified sender domain; TLS-terminating proxy with `TRUST_PROXY_HOPS`; monitor audit events `refresh_token_reuse` and `login_failed` spikes; rotate secrets if exposed.
