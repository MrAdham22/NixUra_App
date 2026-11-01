const err = { description: 'Error', content: { 'application/json': { schema: { $ref: '#/components/schemas/Error' } } } };
const body = (props: Record<string, unknown>, required: string[]) => ({
  required: true,
  content: { 'application/json': { schema: { type: 'object', required, properties: props } } },
});
const s = { type: 'string' };
const secured = [{ bearerAuth: [] }];

/** Hand-maintained OpenAPI 3.1 description of the public API (served at /openapi.json). */
export const openApiDocument = {
  openapi: '3.1.0',
  info: { title: 'NixUra API', version: '1.0.0', description: 'Accounts API. All responses are JSON; errors use {error:{code,message}}.' },
  servers: [{ url: '/' }],
  components: {
    securitySchemes: { bearerAuth: { type: 'http', scheme: 'bearer', bearerFormat: 'JWT' } },
    schemas: {
      Error: { type: 'object', properties: { error: { type: 'object', properties: { code: s, message: s } } } },
      User: { type: 'object', properties: { id: s, email: s, name: s, age: { type: ['integer', 'null'] }, emailVerified: { type: 'boolean' } } },
      Tokens: { type: 'object', properties: { accessToken: s, accessTokenExpiresInSeconds: { type: 'integer' }, refreshToken: s, sessionId: s } },
      AuthResult: { type: 'object', properties: { tokens: { $ref: '#/components/schemas/Tokens' }, user: { $ref: '#/components/schemas/User' } } },
    },
  },
  paths: {
    '/health': { get: { summary: 'Liveness + database check', responses: { 200: { description: 'ok' } } } },
    '/v1/auth/register': { post: { summary: 'Create an account and email a 6-digit code (same response for existing emails)', requestBody: body({ email: s, password: s, name: s }, ['email', 'password']), responses: { 202: { description: 'verification_required' }, 422: err, 429: err } } },
    '/v1/auth/verify-email': { post: { summary: 'Verify the email with the code; returns a session', requestBody: body({ email: s, code: s, deviceName: s, platform: s }, ['email', 'code']), responses: { 200: { description: 'AuthResult' }, 400: err, 429: err } } },
    '/v1/auth/resend-otp': { post: { summary: 'Resend the verification code (always 202)', requestBody: body({ email: s }, ['email']), responses: { 202: { description: 'ok' }, 429: err } } },
    '/v1/auth/login': { post: { summary: 'Sign in', requestBody: body({ email: s, password: s, deviceName: s, platform: s }, ['email', 'password']), responses: { 200: { description: 'AuthResult' }, 401: err, 403: err, 429: err } } },
    '/v1/auth/refresh': { post: { summary: 'Rotate the refresh token; reuse of an old token revokes the session', requestBody: body({ refreshToken: s }, ['refreshToken']), responses: { 200: { description: 'Tokens' }, 401: err } } },
    '/v1/auth/forgot-password': { post: { summary: 'Email a reset code (always 202)', requestBody: body({ email: s }, ['email']), responses: { 202: { description: 'ok' }, 429: err } } },
    '/v1/auth/reset-password': { post: { summary: 'Set a new password with the emailed code; signs out every device', requestBody: body({ email: s, code: s, newPassword: s }, ['email', 'code', 'newPassword']), responses: { 204: { description: 'done' }, 400: err, 422: err } } },
    '/v1/auth/change-password': { post: { summary: 'Change password; other devices are signed out', security: secured, requestBody: body({ currentPassword: s, newPassword: s }, ['currentPassword', 'newPassword']), responses: { 204: { description: 'done' }, 401: err, 422: err } } },
    '/v1/auth/logout': { post: { summary: 'End this session', security: secured, responses: { 204: { description: 'done' } } } },
    '/v1/auth/logout-all': { post: { summary: 'End every session', security: secured, responses: { 204: { description: 'done' } } } },
    '/v1/auth/delete-account': { post: { summary: 'Permanently delete the account and its data (password required)', security: secured, requestBody: body({ password: s }, ['password']), responses: { 204: { description: 'deleted' }, 401: err } } },
    '/v1/auth/sessions': { get: { summary: 'List active device sessions', security: secured, responses: { 200: { description: 'sessions' } } } },
    '/v1/auth/sessions/{id}': { delete: { summary: 'Revoke one session', security: secured, parameters: [{ name: 'id', in: 'path', required: true, schema: s }], responses: { 204: { description: 'done' }, 404: err } } },
    '/v1/me': {
      get: { summary: 'Current user', security: secured, responses: { 200: { description: 'user' } } },
      patch: { summary: 'Update name / age', security: secured, requestBody: body({ name: s, age: { type: ['integer', 'null'] } }, []), responses: { 200: { description: 'user' }, 422: err } },
    },
  },
} as const;
