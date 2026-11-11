/**
 * Encrypts third-party tokens at rest with AES-256-GCM (Web Crypto). The key is 32 bytes given as base64
 * (INTEGRATION_TOKEN_KEY); each value gets its own random 12-byte nonce, and the auth tag makes tampering fail.
 * Format: v1.<nonce>.<tag>.<ciphertext>, all base64url (the same format the old Node backend wrote).
 */
export class TokenCipher {
  private constructor(private readonly key: CryptoKey) {}

  static async create(keyBase64: string): Promise<TokenCipher> {
    let raw: Uint8Array;
    try {
      raw = fromBase64(keyBase64);
    } catch {
      throw new Error('INTEGRATION_TOKEN_KEY must be base64');
    }
    if (raw.length !== 32) throw new Error('INTEGRATION_TOKEN_KEY must be 32 bytes, base64 encoded');
    const key = await crypto.subtle.importKey('raw', raw, 'AES-GCM', false, ['encrypt', 'decrypt']);
    return new TokenCipher(key);
  }

  async encrypt(plain: string): Promise<string> {
    const nonce = crypto.getRandomValues(new Uint8Array(12));
    const out = new Uint8Array(await crypto.subtle.encrypt({ name: 'AES-GCM', iv: nonce }, this.key, new TextEncoder().encode(plain)));
    const data = out.slice(0, out.length - TAG_BYTES);
    const tag = out.slice(out.length - TAG_BYTES);
    return ['v1', toBase64Url(nonce), toBase64Url(tag), toBase64Url(data)].join('.');
  }

  async decrypt(stored: string): Promise<string> {
    const [v, nonce, tag, data] = stored.split('.');
    if (v !== 'v1' || !nonce || !tag || data === undefined) throw new Error('unreadable token');
    const body = concat(fromBase64Url(data), fromBase64Url(tag));
    const plain = await crypto.subtle.decrypt({ name: 'AES-GCM', iv: fromBase64Url(nonce) }, this.key, body);
    return new TextDecoder().decode(plain);
  }
}

const TAG_BYTES = 16;

function concat(a: Uint8Array, b: Uint8Array): Uint8Array {
  const out = new Uint8Array(a.length + b.length);
  out.set(a);
  out.set(b, a.length);
  return out;
}

export function fromBase64(s: string): Uint8Array {
  const bin = atob(s);
  return Uint8Array.from(bin, (c) => c.charCodeAt(0));
}

export function toBase64Url(bytes: Uint8Array): string {
  let bin = '';
  for (const b of bytes) bin += String.fromCharCode(b);
  return btoa(bin).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '');
}

export function fromBase64Url(s: string): Uint8Array {
  const b64 = s.replace(/-/g, '+').replace(/_/g, '/');
  return fromBase64(b64 + '='.repeat((4 - (b64.length % 4)) % 4));
}
