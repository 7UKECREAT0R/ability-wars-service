CREATE TABLE tickets
(
    ticket_id          INTEGER NOT NULL PRIMARY KEY,
    discord_channel_id INTEGER NOT NULL UNIQUE,
    type               INTEGER NOT NULL, -- Uses an enum internally to determine the type of the ticket.
    opened_timestamp   INTEGER NOT NULL, -- The unix millisecond the ticket was opened at.

    -- Open/Closed status.
    is_open            INTEGER NOT NULL DEFAULT TRUE,
    close_reason       TEXT,             -- The reason the ticket was closed, if is_open = false and specified.
    closed_by          INTEGER,          -- Discord ID of the moderator that closed the ticket if is_open = false.

    -- Ticket Info
    input_questions    TEXT    NOT NULL, -- JSON representation of the user's answers to the form questions.
    owner_discord_id   INTEGER NOT NULL  -- Discord ID of the user that opened the ticket.
);

-- Links together tickets and evidence both ways.
CREATE TABLE ticket_evidence_link
(
    ticket_id   INTEGER NOT NULL,
    evidence_id INTEGER NOT NULL,
    PRIMARY KEY (ticket_id, evidence_id),
    FOREIGN KEY (ticket_id) REFERENCES tickets (ticket_id),
    FOREIGN KEY (evidence_id) REFERENCES evidence (evidence_id)
);
-- Links together bans and evidence both ways.
CREATE TABLE ban_evidence_link
(
    user_id          INTEGER NOT NULL,
    starts_timestamp INTEGER NOT NULL,
    evidence_id      INTEGER NOT NULL,
    PRIMARY KEY (user_id, starts_timestamp, evidence_id),
    FOREIGN KEY (user_id, starts_timestamp) REFERENCES bans (user_id, starts),
    FOREIGN KEY (evidence_id) REFERENCES evidence (evidence_id)
);

CREATE TABLE evidence
(
    evidence_id  INTEGER NOT NULL PRIMARY KEY,
    timestamp    INTEGER NOT NULL, -- The unix millisecond the evidence was created.
    accused_user INTEGER,          -- The Roblox ID of the user being accused by this evidence.
    details      TEXT,             -- If present, describes what the evidence shows. Example: "Engineer bug abuse at 9:40"
    url          TEXT,             -- Web URL to the evidence.

    FOREIGN KEY (accused_user) REFERENCES players (user_id)
);

ALTER TABLE bans
    ADD COLUMN linked_ticket INTEGER REFERENCES tickets (ticket_id) DEFAULT NULL