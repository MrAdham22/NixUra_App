import { AppError } from '../errors.js';
import { parseChatBody, parseVoiceBody, readChatAnswer, readVoiceAnswer, type CoachAction } from './model.js';
import { chatSystem, voiceSystem } from './prompts.js';
import { ProviderError, type Provider } from './providers.js';
import type { Usage, UsageKind, UsageStore } from './usage.js';

export interface CoachLimits {
  chatPerDay: number;
  voicePerDay: number;
}

export interface Remaining {
  chat: number;
  voice: number;
  chatLimit: number;
  voiceLimit: number;
}

const CHAT_BUDGET_MS = 20_000;
const VOICE_BUDGET_MS = 36_000;
const CHAT_CALL_MS = 11_000;
const VOICE_CALL_MS = 16_000;

/**
 * The AI coach. It tries the providers in order and moves on whenever one fails or is out of free quota. A use
 * only counts against the user's daily limit when an AI really answered.
 */
export class CoachService {
  constructor(
    private readonly providers: Provider[],
    private readonly usage: UsageStore,
    private readonly limits: CoachLimits,
    private readonly clock: () => number = () => Date.now(),
    private readonly note: (provider: string, status: number) => void = () => {},
  ) {}

  private day(): string {
    return new Date(this.clock()).toISOString().slice(0, 10);
  }

  private remaining(u: Usage): Remaining {
    return {
      chat: Math.max(this.limits.chatPerDay - u.chat, 0),
      voice: Math.max(this.limits.voicePerDay - u.voice, 0),
      chatLimit: this.limits.chatPerDay,
      voiceLimit: this.limits.voicePerDay,
    };
  }

  async quota(uid: string): Promise<{ remaining: Remaining; available: boolean }> {
    return { remaining: this.remaining(await this.usage.get(uid, this.day())), available: this.providers.length > 0 };
  }

  private async take(uid: string, kind: UsageKind): Promise<{ day: string; used: Usage }> {
    if (this.providers.length === 0) throw unavailable();
    const day = this.day();
    const limit = kind === 'chat' ? this.limits.chatPerDay : this.limits.voicePerDay;
    const used = await this.usage.reserve(uid, day, kind, limit);
    if (!used) throw new AppError('daily_limit', 429, kind === 'chat' ? 'You have used today\'s AI messages. They come back tomorrow.' : 'You have used today\'s voice notes. They come back tomorrow.');
    // The first use of the day is a cheap moment to drop old counters.
    if (used.chat + used.voice === 1) await this.usage.prune(new Date(this.clock() - 3 * 86_400_000).toISOString().slice(0, 10)).catch(() => {});
    return { day, used };
  }

  /** Runs [attempt] on each provider in turn until one gives a usable answer or the time budget is spent. */
  private async firstGood<T>(list: Provider[], budgetMs: number, attempt: (p: Provider, timeoutMs: number) => Promise<T | null>): Promise<{ value: T; provider: string } | null> {
    const deadline = this.clock() + budgetMs;
    for (const p of list) {
      const left = deadline - this.clock();
      if (left < 1_500) break;
      try {
        const value = await attempt(p, left);
        if (value !== null) return { value, provider: p.name };
        this.note(p.name, 422); // answered, but not in a usable shape
      } catch (e) {
        // 408 = it took too long; 599 = the call itself failed.
        this.note(p.name, e instanceof ProviderError ? e.status : (e as Error)?.name === 'TimeoutError' ? 408 : 599);
      }
    }
    return null;
  }

  async chat(uid: string, raw: unknown) {
    const body = parseChatBody(raw);
    const { day, used } = await this.take(uid, 'chat');
    const system = chatSystem(body.context, body.focus);
    const found = await this.firstGood(this.providers, CHAT_BUDGET_MS, async (p, left) => {
      const text = await p.chat({ system, turns: body.messages, maxTokens: 450, temperature: 0.6, timeoutMs: Math.min(CHAT_CALL_MS, left) });
      return readChatAnswer(text, body.context);
    });
    if (!found) {
      await this.usage.refund(uid, day, 'chat');
      throw unavailable();
    }
    return { ...found.value, provider: found.provider, remaining: this.remaining(used) };
  }

  async voice(uid: string, raw: unknown): Promise<{ transcript: string; actions: CoachAction[]; provider: string; remaining: Remaining }> {
    const body = parseVoiceBody(raw);
    const { day, used } = await this.take(uid, 'voice');
    const clip = { base64: body.audio, mime: body.mime };
    const deadline = this.clock() + VOICE_BUDGET_MS;
    const left = () => deadline - this.clock();

    // 1. A model that hears audio does it in one call.
    const direct = await this.firstGood(this.providers.filter((p) => p.listen), Math.min(VOICE_CALL_MS + 2_000, left()), async (p, ms) =>
      readVoiceAnswer(await p.listen!(clip, voiceSystem(body.context, true), Math.min(VOICE_CALL_MS, ms))));
    if (direct) return { ...direct.value, provider: direct.provider, remaining: this.remaining(used) };

    // 2. Otherwise: speech to text first, then any chat model picks out what to add.
    const heard = await this.firstGood(this.providers.filter((p) => p.transcribe), left() - 8_000, async (p, ms) => {
      const text = (await p.transcribe!(clip, Math.min(VOICE_CALL_MS, ms))).trim().slice(0, 1000);
      return text || null;
    });
    if (heard) {
      const system = voiceSystem(body.context, false);
      const picked = await this.firstGood(this.providers, left(), async (p, ms) =>
        readVoiceAnswer(await p.chat({ system, turns: [{ role: 'user', text: heard.value }], maxTokens: 500, temperature: 0.1, timeoutMs: Math.min(CHAT_CALL_MS, ms) }), heard.value));
      if (picked) return { ...picked.value, provider: `${heard.provider}+${picked.provider}`, remaining: this.remaining(used) };
    }
    await this.usage.refund(uid, day, 'voice');
    throw unavailable();
  }
}

const unavailable = () => new AppError('ai_unavailable', 503, 'The AI coach is resting right now. Try again later.');
