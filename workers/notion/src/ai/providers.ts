/**
 * The free AI services the coach can use, behind one small interface so the chain can try them in order.
 * Every call has a time limit; any failure (limit reached, timeout, bad answer) just means "try the next one".
 * Nothing here logs prompts, answers or audio.
 */

export interface ChatTurn {
  role: 'user' | 'assistant';
  text: string;
}

export interface ChatRequest {
  system: string;
  turns: ChatTurn[];
  maxTokens: number;
  temperature: number;
  timeoutMs: number;
}

export interface AudioClip {
  /** Raw bytes, base64 (no data: prefix). */
  base64: string;
  mime: string;
}

export interface Provider {
  readonly name: string;
  /** Answers with the model's text, asked to be one JSON object. */
  chat(req: ChatRequest): Promise<string>;
  /** Speech to text. */
  transcribe?(clip: AudioClip, timeoutMs: number): Promise<string>;
  /** Listens to the clip and answers [system] directly, in one call (models that take audio). */
  listen?(clip: AudioClip, system: string, timeoutMs: number): Promise<string>;
}

export class ProviderError extends Error {
  constructor(
    readonly provider: string,
    readonly status: number,
  ) {
    super(`${provider} ${status}`);
  }
}

type Fetch = typeof fetch;

/** Minimal shape of Cloudflare's Workers AI binding. */
export interface AiBinding {
  run(model: string, input: unknown): Promise<unknown>;
}

async function postJson(http: Fetch, provider: string, url: string, headers: Record<string, string>, body: unknown, timeoutMs: number): Promise<any> {
  const res = await http(url, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', ...headers },
    body: JSON.stringify(body),
    signal: AbortSignal.timeout(timeoutMs),
  });
  if (!res.ok) throw new ProviderError(provider, res.status);
  return res.json();
}

function need(provider: string, text: unknown): string {
  if (typeof text !== 'string' || text.trim() === '') throw new ProviderError(provider, 204);
  return text;
}

/** Google Gemini (AI Studio key). Takes audio directly. */
export function gemini(apiKey: string, model: string, http: Fetch = (i, o) => fetch(i, o)): Provider {
  const name = 'gemini';
  const url = `https://generativelanguage.googleapis.com/v1beta/models/${encodeURIComponent(model)}:generateContent`;
  const read = (json: any) =>
    need(name, (json?.candidates?.[0]?.content?.parts ?? []).filter((p: any) => !p?.thought).map((p: any) => p?.text ?? '').join(''));
  return {
    name,
    async chat(req) {
      const json = await postJson(http, name, url, { 'x-goog-api-key': apiKey }, {
        systemInstruction: { parts: [{ text: req.system }] },
        contents: req.turns.map((t) => ({ role: t.role === 'assistant' ? 'model' : 'user', parts: [{ text: t.text }] })),
        generationConfig: { responseMimeType: 'application/json', maxOutputTokens: req.maxTokens * 3, temperature: req.temperature },
      }, req.timeoutMs);
      return read(json);
    },
    async listen(clip, system, timeoutMs) {
      const json = await postJson(http, name, url, { 'x-goog-api-key': apiKey }, {
        systemInstruction: { parts: [{ text: system }] },
        contents: [{ role: 'user', parts: [{ inlineData: { mimeType: clip.mime, data: clip.base64 } }, { text: 'Listen to this voice note and answer as instructed.' }] }],
        generationConfig: { responseMimeType: 'application/json', maxOutputTokens: 2048, temperature: 0.1 },
      }, timeoutMs);
      return read(json);
    },
  };
}

/** Any service that speaks the OpenAI chat format: Groq, Mistral, OpenRouter. */
export function openAiCompatible(
  name: string,
  baseUrl: string,
  apiKey: string,
  model: string,
  opts: { whisperModel?: string; jsonMode?: boolean } = {},
  http: Fetch = (i, o) => fetch(i, o),
): Provider {
  const auth = { Authorization: `Bearer ${apiKey}` };
  const provider: Provider = {
    name,
    async chat(req) {
      const json = await postJson(http, name, `${baseUrl}/chat/completions`, auth, {
        model,
        messages: [{ role: 'system', content: req.system }, ...req.turns.map((t) => ({ role: t.role, content: t.text }))],
        max_tokens: req.maxTokens,
        temperature: req.temperature,
        ...(opts.jsonMode === false ? {} : { response_format: { type: 'json_object' } }),
      }, req.timeoutMs);
      return need(name, json?.choices?.[0]?.message?.content);
    },
  };
  const whisperModel = opts.whisperModel;
  if (whisperModel) {
    provider.transcribe = async (clip, timeoutMs) => {
      const form = new FormData();
      form.append('file', new Blob([fromBase64(clip.base64)], { type: clip.mime }), `note.${extension(clip.mime)}`);
      form.append('model', whisperModel);
      form.append('response_format', 'json');
      const res = await http(`${baseUrl}/audio/transcriptions`, { method: 'POST', headers: auth, body: form, signal: AbortSignal.timeout(timeoutMs) });
      if (!res.ok) throw new ProviderError(name, res.status);
      return need(name, ((await res.json()) as any)?.text);
    };
  }
  return provider;
}

/** Cloudflare Workers AI: no key, it comes with the account (free daily allowance). */
export function workersAi(ai: AiBinding, model: string, whisperModel: string): Provider {
  const name = 'cloudflare';
  const timed = <T>(p: Promise<T>, ms: number): Promise<T> =>
    Promise.race([p, new Promise<T>((_, no) => setTimeout(() => no(new ProviderError(name, 408)), ms))]);
  return {
    name,
    async chat(req) {
      const out: any = await timed(
        ai.run(model, {
          messages: [{ role: 'system', content: req.system }, ...req.turns.map((t) => ({ role: t.role, content: t.text }))],
          max_tokens: req.maxTokens,
          temperature: req.temperature,
        }),
        req.timeoutMs,
      ).catch((e) => { throw e instanceof ProviderError ? e : new ProviderError(name, 500); });
      const text = typeof out?.response === 'string' ? out.response : out?.response ? JSON.stringify(out.response) : out?.choices?.[0]?.message?.content;
      return need(name, text);
    },
    async transcribe(clip, timeoutMs) {
      const out: any = await timed(ai.run(whisperModel, { audio: clip.base64 }), timeoutMs)
        .catch((e) => { throw e instanceof ProviderError ? e : new ProviderError(name, 500); });
      return need(name, out?.text);
    },
  };
}

function fromBase64(s: string): Uint8Array {
  const bin = atob(s);
  return Uint8Array.from(bin, (c) => c.charCodeAt(0));
}

function extension(mime: string): string {
  if (mime.includes('mp4') || mime.includes('m4a') || mime.includes('aac')) return 'm4a';
  if (mime.includes('webm')) return 'webm';
  if (mime.includes('ogg')) return 'ogg';
  if (mime.includes('wav')) return 'wav';
  if (mime.includes('mpeg') || mime.includes('mp3')) return 'mp3';
  return 'm4a';
}
