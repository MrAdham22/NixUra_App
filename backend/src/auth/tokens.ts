import { randomUUID } from 'node:crypto';
import { SignJWT, jwtVerify } from 'jose';
import type { Config } from '../config.js';

export interface AccessClaims {
  userId: string;
  sessionId: string;
}

export interface AccessTokenService {
  issue(claims: AccessClaims, now: Date): Promise<{ token: string; expiresInSeconds: number }>;
  /** Returns null for any invalid, expired or tampered token. */
  verify(token: string, now: Date): Promise<AccessClaims | null>;
}

const ISSUER = 'nixura-backend';
const AUDIENCE = 'nixura-app';

export function createAccessTokenService(cfg: Pick<Config, 'JWT_SECRET' | 'ACCESS_TOKEN_TTL_SECONDS'>): AccessTokenService {
  const key = new TextEncoder().encode(cfg.JWT_SECRET);
  const ttl = cfg.ACCESS_TOKEN_TTL_SECONDS;
  return {
    async issue({ userId, sessionId }, now) {
      const iat = Math.floor(now.getTime() / 1000);
      const token = await new SignJWT({ sid: sessionId })
        .setProtectedHeader({ alg: 'HS256', typ: 'JWT' })
        .setSubject(userId)
        .setIssuer(ISSUER)
        .setAudience(AUDIENCE)
        .setJti(randomUUID())
        .setIssuedAt(iat)
        .setExpirationTime(iat + ttl)
        .sign(key);
      return { token, expiresInSeconds: ttl };
    },
    async verify(token, now) {
      try {
        const { payload } = await jwtVerify(token, key, {
          issuer: ISSUER,
          audience: AUDIENCE,
          algorithms: ['HS256'],
          currentDate: now,
        });
        if (typeof payload.sub !== 'string' || typeof payload.sid !== 'string') return null;
        return { userId: payload.sub, sessionId: payload.sid };
      } catch {
        return null;
      }
    },
  };
}
