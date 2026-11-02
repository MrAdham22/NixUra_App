import Fastify, { type FastifyInstance, type FastifyReply, type FastifyRequest } from 'fastify';
import helmet from '@fastify/helmet';
import cors from '@fastify/cors';
import { ZodError, z } from 'zod';
import type { Config } from '../config.js';
import { AuthService, normalizeEmail, type DeviceInfo } from '../auth/authService.js';
import { AppError } from '../errors.js';
import { MemoryRateLimiter, type RateLimiter } from './rateLimiter.js';
import { openApiDocument } from './openapi.js';

export interface AppDeps {
  cfg: Config;
  auth: AuthService;
  limiter?: RateLimiter;
  /** Used by /health to verify the database answers. */
  ping: () => Promise<void>;
}

declare module 'fastify' {
  interface FastifyRequest {
    authUser?: { userId: string; sessionId: string };
  }
}

const email = z.string().trim().max(254).email().transform(normalizeEmail);
const device = z.object({ deviceName: z.string().max(80).optional(), platform: z.enum(['android', 'ios', 'web', 'other']).optional() }).partial();
const password = z.string().min(1).max(256); // policy is enforced by the service; this only bounds the input
const code = z.string().regex(/^\d{6}$/, 'The code has 6 digits.');

export async function buildApp(deps: AppDeps): Promise<FastifyInstance> {
  const { cfg, auth } = deps;
  const limiter = deps.limiter ?? new MemoryRateLimiter();
  const app = Fastify({
    logger: cfg.NODE_ENV === 'test' ? false : { level: 'info', redact: ['req.headers.authorization', 'req.headers.cookie'] },
    // Same as Fastify's numeric hop count, which its types do not accept.
    trustProxy: cfg.TRUST_PROXY_HOPS > 0 ? (_addr: string, hop: number) => hop < cfg.TRUST_PROXY_HOPS : false,
    bodyLimit: 16 * 1024,
    disableRequestLogging: false,
  });

  await app.register(helmet, { contentSecurityPolicy: { directives: { defaultSrc: ["'none'"], frameAncestors: ["'none'"] } }, hsts: cfg.NODE_ENV === 'production' });
  const origins = cfg.CORS_ORIGINS.split(',').map((s) => s.trim()).filter(Boolean);
  await app.register(cors, { origin: origins.length ? origins : false, methods: ['GET', 'POST', 'PATCH', 'DELETE'] });

  // Global per-IP ceiling; sensitive routes add their own tighter limits below.
  app.addHook('onRequest', async (req) => {
    const wait = limiter.hit(`ip:${req.ip}`, 120, 60);
    if (wait > 0) throw new AppError('rate_limited', 429, 'Too many requests. Slow down.', { retryAfterSeconds: wait });
  });

  app.setErrorHandler((err: unknown, req, reply) => {
    if (err instanceof AppError) {
      if (err.extra.retryAfterSeconds) void reply.header('Retry-After', String(err.extra.retryAfterSeconds));
      return reply.status(err.status).send({ error: { code: err.code, message: err.message, ...err.extra } });
    }
    if (err instanceof ZodError) {
      return reply.status(422).send({ error: { code: 'invalid_input', message: 'Some fields are invalid.', fields: err.issues.map((i) => ({ path: i.path.join('.'), message: i.message })) } });
    }
    const status = (err as { statusCode?: number }).statusCode;
    if (typeof status === 'number' && status >= 400 && status < 500) {
      return reply.status(status).send({ error: { code: 'invalid_input', message: 'The request could not be processed.' } });
    }
    req.log.error({ err: err instanceof Error ? err.message : 'unknown' }, 'unhandled error'); // message only: no stack with data, no PII
    return reply.status(500).send({ error: { code: 'internal', message: 'Something went wrong. Please try again.' } });
  });
  app.setNotFoundHandler((_req, reply) => reply.status(404).send({ error: { code: 'not_found', message: 'Not found.' } }));

  /** Per-IP and per-email limit for one action; both must pass. */
  const guard = (action: string, mail: string | undefined, ip: string, perIp: number, perEmail: number, windowSeconds: number) => {
    let wait = limiter.hit(`${action}:ip:${ip}`, perIp, windowSeconds);
    if (mail) wait = Math.max(wait, limiter.hit(`${action}:email:${mail}`, perEmail, windowSeconds));
    if (wait > 0) throw new AppError('rate_limited', 429, 'Too many attempts. Try again later.', { retryAfterSeconds: wait });
  };

  const requireAuth = async (req: FastifyRequest): Promise<void> => {
    const header = req.headers.authorization ?? '';
    const match = /^Bearer (.+)$/.exec(header);
    if (!match?.[1]) throw new AppError('unauthorized', 401, 'Please sign in again.');
    req.authUser = await auth.authenticate(match[1]);
  };
  const user = (req: FastifyRequest) => req.authUser!;
  const accepted = (reply: FastifyReply) => reply.status(202).send({ status: 'ok' });

  app.get('/health', async () => {
    await deps.ping();
    return { status: 'ok' };
  });
  app.get('/openapi.json', async () => openApiDocument);

  // -------------------------------------------------------------- auth (public)
  app.post('/v1/auth/register', async (req, reply) => {
    const b = z.object({ email, password, name: z.string().max(60).optional() }).parse(req.body);
    guard('register', b.email, req.ip, 10, 5, 3600);
    await auth.register(b.email, b.password, b.name);
    return reply.status(202).send({ status: 'verification_required' }); // identical for new and existing emails
  });

  app.post('/v1/auth/verify-email', async (req) => {
    const b = z.object({ email, code }).merge(device).parse(req.body);
    guard('verify', b.email, req.ip, 30, 10, 900);
    const r = await auth.verifyEmail(b.email, b.code, b as DeviceInfo);
    return r;
  });

  app.post('/v1/auth/resend-otp', async (req, reply) => {
    const b = z.object({ email }).parse(req.body);
    guard('resend', b.email, req.ip, 20, 5, 3600);
    await auth.resendVerification(b.email);
    return accepted(reply);
  });

  app.post('/v1/auth/login', async (req) => {
    const b = z.object({ email, password }).merge(device).parse(req.body);
    guard('login', undefined, req.ip, 30, 0, 900); // per-account protection is the lockout in the service
    return auth.login(b.email, b.password, b as DeviceInfo);
  });

  app.post('/v1/auth/refresh', async (req) => {
    const b = z.object({ refreshToken: z.string().min(20).max(200) }).parse(req.body);
    guard('refresh', undefined, req.ip, 120, 0, 900);
    return auth.refresh(b.refreshToken);
  });

  app.post('/v1/auth/forgot-password', async (req, reply) => {
    const b = z.object({ email }).parse(req.body);
    guard('forgot', b.email, req.ip, 20, 5, 3600);
    await auth.forgotPassword(b.email);
    return accepted(reply);
  });

  app.post('/v1/auth/reset-password', async (req, reply) => {
    const b = z.object({ email, code, newPassword: password }).parse(req.body);
    guard('reset', b.email, req.ip, 30, 10, 900);
    await auth.resetPassword(b.email, b.code, b.newPassword);
    return reply.status(204).send();
  });

  // -------------------------------------------------------------- auth (signed in)
  app.post('/v1/auth/logout', { preHandler: requireAuth }, async (req, reply) => {
    await auth.logout(user(req).userId, user(req).sessionId);
    return reply.status(204).send();
  });
  app.post('/v1/auth/logout-all', { preHandler: requireAuth }, async (req, reply) => {
    await auth.logoutAll(user(req).userId);
    return reply.status(204).send();
  });
  app.post('/v1/auth/change-password', { preHandler: requireAuth }, async (req, reply) => {
    const b = z.object({ currentPassword: password, newPassword: password }).parse(req.body);
    guard('change', user(req).userId, req.ip, 20, 10, 900);
    await auth.changePassword(user(req).userId, user(req).sessionId, b.currentPassword, b.newPassword);
    return reply.status(204).send();
  });
  app.post('/v1/auth/delete-account', { preHandler: requireAuth }, async (req, reply) => {
    const b = z.object({ password }).parse(req.body);
    guard('delete', user(req).userId, req.ip, 10, 5, 900);
    await auth.deleteAccount(user(req).userId, b.password);
    return reply.status(204).send();
  });
  app.get('/v1/auth/sessions', { preHandler: requireAuth }, async (req) => ({
    sessions: await auth.listSessions(user(req).userId, user(req).sessionId),
  }));
  app.delete('/v1/auth/sessions/:id', { preHandler: requireAuth }, async (req, reply) => {
    const { id } = z.object({ id: z.string().uuid() }).parse(req.params);
    await auth.revokeSessionById(user(req).userId, id);
    return reply.status(204).send();
  });

  app.get('/v1/me', { preHandler: requireAuth }, async (req) => ({ user: await auth.getMe(user(req).userId) }));
  app.patch('/v1/me', { preHandler: requireAuth }, async (req) => {
    const b = z.object({ name: z.string().max(60).optional(), age: z.number().int().min(5).max(120).nullable().optional() }).parse(req.body);
    return { user: await auth.updateMe(user(req).userId, b) };
  });

  return app;
}
