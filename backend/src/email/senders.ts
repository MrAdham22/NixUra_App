import type { EmailMessage, EmailSender } from './emailSender.js';
import { render } from './templates.js';

/** Dev-only: prints the mail (including the code) to stdout. Refused in production by config validation. */
export class ConsoleEmailSender implements EmailSender {
  async send(message: EmailMessage): Promise<void> {
    console.log(`[email:dev] to=${message.to} kind=${message.kind}${message.code ? ` code=${message.code}` : ''}`);
  }
}

/** Resend (https://resend.com) over HTTPS with a timeout and one retry on 5xx/network errors. */
export class ResendEmailSender implements EmailSender {
  constructor(
    private readonly apiKey: string,
    private readonly from: string,
    private readonly supportEmail: string,
    private readonly fetchImpl: typeof fetch = fetch,
  ) {}

  async send(message: EmailMessage): Promise<void> {
    const { subject, text, html } = render(message, this.supportEmail);
    const body = JSON.stringify({ from: this.from, to: [message.to], subject, text, html });
    let lastError: unknown;
    for (let attempt = 0; attempt < 2; attempt++) {
      try {
        const res = await this.fetchImpl('https://api.resend.com/emails', {
          method: 'POST',
          headers: { Authorization: `Bearer ${this.apiKey}`, 'Content-Type': 'application/json' },
          body,
          signal: AbortSignal.timeout(8000),
        });
        if (res.ok) return;
        lastError = new Error(`Email provider responded ${res.status}`);
        if (res.status < 500) break; // 4xx will not succeed on retry
      } catch (e) {
        lastError = e;
      }
    }
    throw lastError instanceof Error ? lastError : new Error('Email delivery failed');
  }
}

/** Test double that records what would have been sent. */
export class CapturingEmailSender implements EmailSender {
  readonly sent: EmailMessage[] = [];
  async send(message: EmailMessage): Promise<void> {
    this.sent.push(message);
  }
  lastCode(to: string, kind: EmailMessage['kind']): string | undefined {
    return [...this.sent].reverse().find((m) => m.to === to && m.kind === kind)?.code;
  }
}
