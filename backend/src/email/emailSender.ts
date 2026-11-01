export type EmailKind = 'verify_email' | 'reset_password' | 'password_changed' | 'account_exists';

export interface EmailMessage {
  to: string;
  kind: EmailKind;
  /** Present for OTP mails. */
  code?: string;
  /** Minutes the code stays valid. */
  ttlMinutes?: number;
}

/** Transactional email port. Implementations must never log the code outside the dev console sender. */
export interface EmailSender {
  send(message: EmailMessage): Promise<void>;
}
