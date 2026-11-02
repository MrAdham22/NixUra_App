-- Cloud sync: one row per synced thing (habit, relapse, good-habit day, event, settings), owned by a user.
-- payload is opaque JSON text written by the app; the server never looks inside it.
-- seq increases on every accepted change, so a device can ask for "everything after the last seq I saw".
CREATE SEQUENCE sync_seq;

CREATE TABLE sync_items (
  user_id     UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  entity_type TEXT NOT NULL,
  entity_id   TEXT NOT NULL,
  payload     TEXT NOT NULL,
  deleted     BOOLEAN NOT NULL DEFAULT FALSE,
  updated_at  BIGINT NOT NULL,
  seq         BIGINT NOT NULL,
  PRIMARY KEY (user_id, entity_type, entity_id)
);

CREATE INDEX sync_items_user_seq ON sync_items (user_id, seq);
