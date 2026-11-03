import { createCipheriv, createDecipheriv, randomBytes } from 'node:crypto';

/**
 * Encrypts third-party tokens at rest with AES-256-GCM. The key is 32 bytes given as base64 (INTEGRATION_TOKEN_KEY);
 * each value gets its own random 12-byte nonce, and the auth tag makes any tampering fail to decrypt.
 * Format: v1.<nonce>.<tag>.<ciphertext>, all base64url.
 */
export class TokenCipher {
  private readonly key: Buffer;

  constructor(keyBase64: string) {
    const key = Buffer.from(keyBase64, 'base64');
    if (key.length !== 32) throw new Error('INTEGRATION_TOKEN_KEY must be 32 bytes, base64 encoded');
    this.key = key;
  }

  encrypt(plain: string): string {
    const nonce = randomBytes(12);
    const cipher = createCipheriv('aes-256-gcm', this.key, nonce);
    const data = Buffer.concat([cipher.update(plain, 'utf8'), cipher.final()]);
    return ['v1', nonce.toString('base64url'), cipher.getAuthTag().toString('base64url'), data.toString('base64url')].join('.');
  }

  decrypt(stored: string): string {
    const [v, nonce, tag, data] = stored.split('.');
    if (v !== 'v1' || !nonce || !tag || data === undefined) throw new Error('unreadable token');
    const decipher = createDecipheriv('aes-256-gcm', this.key, Buffer.from(nonce, 'base64url'));
    decipher.setAuthTag(Buffer.from(tag, 'base64url'));
    return Buffer.concat([decipher.update(Buffer.from(data, 'base64url')), decipher.final()]).toString('utf8');
  }
}
