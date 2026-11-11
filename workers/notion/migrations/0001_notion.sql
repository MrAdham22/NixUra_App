-- Notion links, keyed by the Firebase user id (uid). The Notion access token is stored encrypted
-- (AES-256-GCM, key INTEGRATION_TOKEN_KEY), never in plain text and never sent to the app.
CREATE TABLE notion_links (
  uid         TEXT PRIMARY KEY,
  token_enc   TEXT NOT NULL,
  workspace   TEXT NOT NULL DEFAULT '',
  created_at  INTEGER NOT NULL
);

-- One-time "state" values for the OAuth round trip through the browser (CSRF protection), valid for 10 minutes.
CREATE TABLE oauth_states (
  state       TEXT PRIMARY KEY,
  uid         TEXT NOT NULL,
  expires_at  INTEGER NOT NULL
);
CREATE INDEX oauth_states_expires ON oauth_states (expires_at);
