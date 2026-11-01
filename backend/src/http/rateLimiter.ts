/**
 * Fixed-window counter limiter, per process. Good enough for a single instance; when the backend is scaled
 * out, swap [RateLimiter] for a shared-store implementation (Redis) behind the same interface.
 */
export interface RateLimiter {
  /** Returns 0 if allowed, else the seconds to wait. Counts the attempt. */
  hit(key: string, max: number, windowSeconds: number): number;
}

interface Bucket {
  count: number;
  resetAt: number;
}

export class MemoryRateLimiter implements RateLimiter {
  private readonly buckets = new Map<string, Bucket>();
  constructor(private readonly clock: () => number = Date.now) {}

  hit(key: string, max: number, windowSeconds: number): number {
    const now = this.clock();
    if (this.buckets.size > 20_000) this.sweep(now);
    let b = this.buckets.get(key);
    if (!b || b.resetAt <= now) {
      b = { count: 0, resetAt: now + windowSeconds * 1000 };
      this.buckets.set(key, b);
    }
    b.count += 1;
    return b.count > max ? Math.max(1, Math.ceil((b.resetAt - now) / 1000)) : 0;
  }

  private sweep(now: number): void {
    for (const [k, b] of this.buckets) if (b.resetAt <= now) this.buckets.delete(k);
  }
}
