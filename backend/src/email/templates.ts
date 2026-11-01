import type { EmailMessage } from './emailSender.js';

export interface Rendered {
  subject: string;
  text: string;
  html: string;
}

const esc = (s: string) => s.replace(/[&<>"']/g, (c) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' })[c]!);

function wrap(title: string, bodyHtml: string): string {
  return `<div style="font-family:system-ui,sans-serif;background:#1D1A39;color:#E8BCB9;padding:24px">
<div style="max-width:480px;margin:auto;background:#451952;border-radius:16px;padding:24px">
<h2 style="margin:0 0 12px;color:#F39F5A">${esc(title)}</h2>${bodyHtml}
<p style="font-size:12px;opacity:.7;margin-top:24px">NixUra. If this was not you, you can ignore this email.</p></div></div>`;
}

export function render(m: EmailMessage, supportEmail: string): Rendered {
  const ttl = m.ttlMinutes ?? 10;
  switch (m.kind) {
    case 'verify_email':
      return {
        subject: `Your NixUra verification code: ${m.code}`,
        text: `Your NixUra verification code is ${m.code}. It expires in ${ttl} minutes. Never share this code with anyone.`,
        html: wrap('Verify your email', `<p>Your code is</p><p style="font-size:32px;letter-spacing:8px;font-weight:700;color:#fff">${esc(m.code ?? '')}</p><p>It expires in ${ttl} minutes. Never share it with anyone.</p>`),
      };
    case 'reset_password':
      return {
        subject: `Your NixUra password reset code: ${m.code}`,
        text: `Your NixUra password reset code is ${m.code}. It expires in ${ttl} minutes. If you did not ask for it, ignore this email: your password is unchanged.`,
        html: wrap('Reset your password', `<p>Your code is</p><p style="font-size:32px;letter-spacing:8px;font-weight:700;color:#fff">${esc(m.code ?? '')}</p><p>It expires in ${ttl} minutes. If you did not ask for it, ignore this email: your password is unchanged.</p>`),
      };
    case 'password_changed':
      return {
        subject: 'Your NixUra password was changed',
        text: `Your NixUra password was just changed and you were signed out of your other devices. If this was not you, reset your password and contact ${supportEmail}.`,
        html: wrap('Password changed', `<p>Your password was just changed and you were signed out of your other devices.</p><p>If this was not you, reset your password and contact ${esc(supportEmail)}.</p>`),
      };
    case 'account_exists':
      return {
        subject: 'Someone tried to create a NixUra account with your email',
        text: `Someone tried to register a NixUra account with this email, but one already exists. If it was you, just sign in (or use "Forgot password"). If not, no action is needed.`,
        html: wrap('Account already exists', `<p>Someone tried to register with this email, but an account already exists.</p><p>If it was you, sign in or use "Forgot password". If not, no action is needed.</p>`),
      };
  }
}
