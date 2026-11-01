import { createHash } from 'node:crypto';

export const PASSWORD_MIN_LENGTH = 10;
export const PASSWORD_MAX_LENGTH = 128;

/** Small denylist of the most common passwords. The optional HIBP check covers the long tail. */
const COMMON = new Set([
  'password', 'password1', 'password12', 'password123', 'passw0rd', 'p@ssw0rd', 'qwertyuiop', 'qwerty123',
  'qwerty12345', '1234567890', '12345678910', '0123456789', '1q2w3e4r5t', '1qaz2wsx3edc', 'iloveyou12',
  'iloveyou123', 'letmein123', 'welcome123', 'welcome1234', 'admin12345', 'administrator', 'abc1234567',
  'abcdefghij', 'abcd123456', 'monkey1234', 'dragon1234', 'football123', 'baseball123', 'superman123',
  'trustno1234', 'changeme123', 'internet123', 'whatever123', 'sunshine123', 'princess123', 'nixura1234',
  'habbit1234', 'nixurapassword', 'passwordpassword', '1111111111', '0000000000', 'aaaaaaaaaa',
]);

export type PasswordProblem = 'too_short' | 'too_long' | 'too_common' | 'same_as_email' | 'low_variety' | 'breached';

/** Pure policy check. Returns every problem found (empty = acceptable). */
export function checkPasswordPolicy(password: string, email?: string): PasswordProblem[] {
  const problems: PasswordProblem[] = [];
  if ([...password].length < PASSWORD_MIN_LENGTH) problems.push('too_short');
  if (password.length > PASSWORD_MAX_LENGTH) problems.push('too_long');
  const lower = password.toLowerCase();
  if (COMMON.has(lower)) problems.push('too_common');
  if (email) {
    const e = email.toLowerCase();
    const local = e.split('@')[0] ?? '';
    if (lower === e || (local.length >= 4 && lower === local)) problems.push('same_as_email');
  }
  // One repeated character or a handful of distinct characters has no real entropy.
  if (new Set(password).size < 4) problems.push('low_variety');
  return problems;
}

/**
 * Optional breached-password check using the Pwned Passwords range API (k-anonymity: only the first 5 hex
 * chars of the SHA-1 leave the server). Fails open on network errors so an outage never blocks sign-up.
 */
export async function isBreached(password: string, fetchImpl: typeof fetch = fetch): Promise<boolean> {
  const sha1 = createHash('sha1').update(password).digest('hex').toUpperCase();
  const prefix = sha1.slice(0, 5);
  const suffix = sha1.slice(5);
  try {
    const res = await fetchImpl(`https://api.pwnedpasswords.com/range/${prefix}`, {
      headers: { 'Add-Padding': 'true' },
      signal: AbortSignal.timeout(2500),
    });
    if (!res.ok) return false;
    const body = await res.text();
    for (const line of body.split('\n')) {
      const [hash, count] = line.trim().split(':');
      if (hash === suffix && Number(count) > 0) return true;
    }
    return false;
  } catch {
    return false;
  }
}
