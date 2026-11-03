-- Linked calendars. The provider's access token is stored encrypted (AES-256-GCM, key INTEGRATION_TOKEN_KEY),
-- never in plain text and never sent to the app. Deleting the account deletes these rows.
CREATE TABLE integration_links (
  user_id     UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  provider    TEXT NOT NULL,
  token_enc   TEXT NOT NULL,
  workspace   TEXT NOT NULL DEFAULT '',
  created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
  PRIMARY KEY (user_id, provider)
);

-- One-time "state" values for the OAuth round trip through the browser (CSRF protection), valid for 10 minutes.
CREATE TABLE oauth_states (
  state       TEXT PRIMARY KEY,
  user_id     UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  provider    TEXT NOT NULL,
  expires_at  TIMESTAMPTZ NOT NULL
);
