-- Can be null; The reason for the blacklist.
ALTER TABLE players
    ADD COLUMN appeal_blacklist_reason TEXT DEFAULT NULL;
-- The unix millisecond time that the blacklist was issued.
ALTER TABLE players
    ADD COLUMN appeal_blacklist_date INTEGER NOT NULL DEFAULT 0;
-- The Discord ID of the moderator who issued the blacklist.
ALTER TABLE players
    ADD COLUMN appeal_blacklist_issuer INTEGER NOT NULL DEFAULT 0;

-- A Discord user being present in this table implicitly confirms that they are blacklisted from appealing.
CREATE TABLE discord_appeal_blacklists
(
    -- The Discord ID of the user who's blacklisted
    discord_id              INTEGER NOT NULL PRIMARY KEY,
    -- Can be null; The reason for the blacklist.
    appeal_blacklist_reason TEXT DEFAULT NULL,
    -- The unix millisecond time that the blacklist was issued.
    appeal_blacklist_date   INTEGER NOT NULL,
    -- The Discord ID of the moderator who issued the blacklist.
    appeal_blacklist_issuer INTEGER NOT NULL
);