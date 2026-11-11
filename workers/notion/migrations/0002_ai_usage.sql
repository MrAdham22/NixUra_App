-- AI coach: how many chat messages and voice notes each user used per UTC day (for the free daily limit).
CREATE TABLE ai_usage (
  uid    TEXT NOT NULL,
  day    TEXT NOT NULL,
  chat   INTEGER NOT NULL DEFAULT 0,
  voice  INTEGER NOT NULL DEFAULT 0,
  PRIMARY KEY (uid, day)
);
CREATE INDEX ai_usage_day ON ai_usage (day);
