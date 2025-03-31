-- v2 adds blacklisting for specific players.
ALTER TABLE players
    ADD COLUMN is_appeal_blacklisted INTEGER DEFAULT FALSE;