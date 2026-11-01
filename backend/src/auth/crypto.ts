import { createHash, createHmac, randomBytes, randomInt, timingSafeEqual } from 'node:crypto';
import argon2 from 'argon2';
import type { Config } from '../config.js';

export interface PasswordHasher {
  hash(password: string): Promise<string>;
  verify(hash: string, password: string): Promise<boolean>;
  /** True when the stored hash was made with weaker parameters than the current config. */
  needsRehash(hash: string): boolean;
}

export function createArgon2Hasher(cfg: Pick<Config, 'ARGON2_MEMORY_KIB' | 'ARGON2_TIME_COST' | 'ARGON2_PARALLELISM'>): PasswordHasher {
  const options = {
    type: argon2.argon2id,
    memoryCost: cfg.ARGON2_MEMORY_KIB,
    timeCost: cfg.ARGON2_TIME_COST,
    parallelism: cfg.ARGON2_PARALLELISM,
  } as const;
  return {
    hash: (password) => argon2.hash(password, options),
    async verify(hash, password) {
      try {
        return await argon2.verify(hash, password);
      } catch {
        return false;
      }
    },
    needsRehash: (hash) => argon2.needsRehash(hash, options),
  };
}

/** Uniformly random 6-digit code from the OS CSPRNG (randomInt rejects values that would cause modulo bias). */
export function generateOtp(): string {
  return randomInt(0, 1_000_000).toString().padStart(6, '0');
}

/** HMAC-SHA256 of an OTP bound to its user and purpose; the pepper never leaves the server. */
export function hashOtp(pepper: string, userId: string, purpose: string, code: string): string {
  return createHmac('sha256', pepper).update(`${userId}\u0000${purpose}\u0000${code}`).digest('hex');
}

/** Constant-time comparison of two hex digests of equal length. */
export function safeEqualHex(a: string, b: string): boolean {
  const ba = Buffer.from(a, 'hex');
  const bb = Buffer.from(b, 'hex');
  if (ba.length !== bb.length || ba.length === 0) return false;
  return timingSafeEqual(ba, bb);
}

/** 256-bit opaque token, URL-safe. */
export function newOpaqueToken(): string {
  return randomBytes(32).toString('base64url');
}

export function sha256Hex(value: string): string {
  return createHash('sha256').update(value).digest('hex');
}
