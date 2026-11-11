import { createRemoteJWKSet, jwtVerify, type JWTVerifyGetKey } from 'jose';

/** Google's public keys for Firebase ID tokens (rotated by Google; jose caches them per Worker instance). */
const FIREBASE_JWKS = 'https://www.googleapis.com/service_accounts/v1/jwk/securetoken@system.gserviceaccount.com';

let googleKeys: JWTVerifyGetKey | undefined;

export function firebaseKeys(): JWTVerifyGetKey {
  googleKeys ??= createRemoteJWKSet(new URL(FIREBASE_JWKS));
  return googleKeys;
}

/**
 * Checks a Firebase ID token the way Firebase documents it (RS256 signed by Google, issuer and audience = this
 * project, not expired, signed in at a time in the past) and returns the user id, or null for any bad token.
 * The token itself is never logged or stored.
 */
export async function verifyFirebaseIdToken(token: string, projectId: string, keys: JWTVerifyGetKey, now: Date = new Date()): Promise<string | null> {
  if (!token || token.length > 4096) return null;
  try {
    const { payload } = await jwtVerify(token, keys, {
      issuer: `https://securetoken.google.com/${projectId}`,
      audience: projectId,
      algorithms: ['RS256'],
      currentDate: now,
      clockTolerance: 30,
    });
    const uid = payload.sub;
    if (typeof uid !== 'string' || uid.length === 0 || uid.length > 128) return null;
    const authTime = payload['auth_time'];
    if (typeof authTime !== 'number' || authTime * 1000 > now.getTime() + 30_000) return null;
    return uid;
  } catch {
    return null;
  }
}
