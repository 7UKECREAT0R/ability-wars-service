-- the is_legacy field will be used to give additional info for bans
-- which are being forward-ported to the modern ban system on the fly.
ALTER TABLE bans
    ADD COLUMN is_legacy INTEGER NOT NULL DEFAULT FALSE;

-- stores Discord bans tied to real timestamps so we can fetch ban times.
-- we have to do this since Discord doesn't provide dates of bans.
CREATE TABLE discord_ban_records
(
    user_id   INTEGER NOT NULL,
    timestamp INTEGER NOT NULL
);
