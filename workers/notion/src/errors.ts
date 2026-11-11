/** An answer the app understands: `{"error":{"code","message"}}` with an HTTP status (same shape as the old backend). */
export class AppError extends Error {
  constructor(
    readonly code: string,
    readonly status: number,
    message: string,
  ) {
    super(message);
  }
}
