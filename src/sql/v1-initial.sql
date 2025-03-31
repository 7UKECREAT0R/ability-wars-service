-- Basic user information, tied to the rest of the tables
CREATE TABLE players
(
    user_id  INTEGER PRIMARY KEY NOT NULL,
    username TEXT UNIQUE
);

-- Player-specific stats
CREATE TABLE stats
(
    user_id    INTEGER PRIMARY KEY NOT NULL,
    punches    INTEGER,
    gamepasses TEXT,

    FOREIGN KEY (user_id) REFERENCES players (user_id)
);

-- Player ban records.
CREATE TABLE bans
(
    user_id               INTEGER NOT NULL,
    responsible_moderator INTEGER,
    reason                TEXT,
    starts                INTEGER NOT NULL,
    ends                  INTEGER,

    FOREIGN KEY (user_id) REFERENCES players (user_id)
);

-- Player unban records.
CREATE TABLE unbans
(
    user_id               INTEGER NOT NULL,
    responsible_moderator INTEGER,
    date                  INTEGER NOT NULL,

    FOREIGN KEY (user_id) REFERENCES players (user_id)
);

-- Records of players' punches being manually set by moderators.
CREATE TABLE punch_update_records
(
    user_id               INTEGER NOT NULL,
    responsible_moderator INTEGER,
    date                  INTEGER NOT NULL,

    old_punches           INTEGER,
    new_punches           INTEGER,

    FOREIGN KEY (user_id) REFERENCES players (user_id)
);