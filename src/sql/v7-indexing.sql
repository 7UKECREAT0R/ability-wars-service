CREATE INDEX IF NOT EXISTS idx_bans_user_starts  ON bans (user_id, starts);
CREATE INDEX IF NOT EXISTS idx_bans_mod_starts   ON bans (responsible_moderator, starts);
CREATE INDEX IF NOT EXISTS idx_unbans_user_date  ON unbans (user_id, date);