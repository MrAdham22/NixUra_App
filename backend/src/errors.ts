export type ErrorCode =
  | 'invalid_input'
  | 'weak_password'
  | 'invalid_credentials'
  | 'email_not_verified'
  | 'invalid_code'
  | 'account_locked'
  | 'rate_limited'
  | 'unauthorized'
  | 'session_revoked'
  | 'not_found'
  | 'integration_unavailable'
  | 'integration_not_connected'
  | 'integration_failed'
  | 'internal';

/** Typed, user-safe error. `message` is shown to clients, so it must never reveal whether an email exists. */
export class AppError extends Error {
  constructor(
    readonly code: ErrorCode,
    readonly status: number,
    message: string,
    readonly extra: Record<string, unknown> = {},
  ) {
    super(message);
  }
}

export const Errors = {
  invalidCredentials: () => new AppError('invalid_credentials', 401, 'Email or password is incorrect.'),
  invalidCode: () => new AppError('invalid_code', 400, 'That code is invalid or has expired. Request a new one.'),
  unauthorized: () => new AppError('unauthorized', 401, 'Please sign in again.'),
  sessionRevoked: () => new AppError('session_revoked', 401, 'This session has ended. Please sign in again.'),
  notVerified: () => new AppError('email_not_verified', 403, 'Please verify your email to continue.'),
  locked: (retryAfterSeconds: number) =>
    new AppError('account_locked', 429, 'Too many attempts. Try again later.', { retryAfterSeconds }),
  weakPassword: (problems: string[]) =>
    new AppError('weak_password', 422, 'Choose a stronger password.', { problems }),
};
