# Ban Appeal

<primary-label ref="ticket-type"/>

Ban appeals are requests from banned users to be unbanned via an apology. Users may generally appeal their bans after
six months have passed, but it's not extremely strict. They're only _able_ to open an appeal ticket if their account
has been banned for at least four months (for Roblox tickets).

Appeals can be either for in-game bans, or Discord server bans. The tickets will adapt depending on the type.

The name format is `appeal-######`

## Handling IP Bans

With Roblox's more recent implementation of IP bans, we tend to get a lot of appeals for them. While we technically
can override the ban and unban the user, our policy is to not do so. If a user is unable to play but is not banned *by
us*, they will be unable to open an appeal ticket at all. The rejection message has a button attached which will tell
them they're IP banned and how to get it resolved (_get all alternate accounts unbanned_).

## Questions

Where are you banned from?
: Discord or Roblox. This will determine how the ticket is formatted and what actions/properties will be available.

User ID
: If the user picked "Discord," this is their numeric Discord ID. If they picked "Roblox," this should be their numeric
Roblox ID. The user may get confused and enter a username instead of an ID, in which a failsafe will be provided for
both types.

Why were you banned?
: The self-proclaimed reason for the ban. It must be truthful and accurate to what occurred, and the user should not
"dance around" the edges of what happened. They must cleanly state what they did.

Why should you be unbanned?
: Why the user should be unbanned. This should always include an apology and assurance that the incident will
never happen again.

Anything else you'd like to tell us?
: Additional, unrequired information provided by the user that doesn't fit in the other two main questions.

## Action Buttons

The buttons which are present on the <tooltip term="ticket-panel">control panel</tooltip>. All of them are staff-only
to prevent the user from backpedaling or otherwise changing their story. Once the information is entered, it cannot be
changed by the opener.

### For Both Roblox and Discord Tickets

#### Close

Prompts the staff member to enter a reason for closing the ticket, then closes it. The user will see the reason
entered in a private message.

#### Close (keep waiting)

Used for cases where it hasn't been six months yet and the user is trying to appeal. Prompts the staff member to enter
the number of months left until the user can appeal. The user will see a pre-set reason in a private message.

#### Close (low effort)

Closes the ticket with a pre-set reason, letting them know their ticket was poorly written with no effort put into it.
The user will see the reason in a private message.

#### Blacklist (lying)

Closes the ticket and blacklists the user from creating future appeals and [disputes](Ban-Dispute.md). The reason for
the blacklist will be for lying about the circumstances surrounding their ban, and they'll be sent a private message
with a generic unrelated reason.

#### Blacklist (custom)

Prompts the staff member to enter both a blacklist reason and close reason. The user will be blacklisted, the ticket
will be closed, and the user will be sent a private message with the entered reason.

#### Unban

Unbans the user, closes the ticket, and sends them a private message letting them know. If the ban was from the Discord
server, they'll also be given a link to re-join. **This is basically the "Accept" button.**

#### Unban (custom)

Prompts the staff member to enter a close reason. Unbans the user, closes the ticket, and sends them a private message
with the given close reason. If the ban was from the Discord server, they'll also be given a link to re-join. **This is
basically the "Accept" button.**

### Roblox Ticket Specific

#### Blacklist (shared account)

Closes the ticket and blacklists the user from creating future appeals. The reason for the blacklist will be for sharing
their account with another person, and they'll be sent a private message with a generic unrelated reason.

#### Unban (unsure)

An alternative unban button which is used when the evidence isn't definitive. Unbans the user, closes the ticket, and
sends them a private message letting them know. This has its pre-set message tuned to be a bit more direct about the
circumstances surrounding the ban to _hopefully_ have them not repeat the offense.

#### Set Ban Length

Prompts the moderator for a number of days to ban the user for. This is an alternative to [Unban](#unban)ning the user
if the staff member believes they still need to serve a bit more time before they can play again, or if the user was
banned permanently for an offense which normally results in a temporary ban (e.g., bug abuse). This will change the
user's ban length and close the ticket. The user will be sent a private message letting them know.

## Properties

These are the properties that can be set on these tickets with [`/ticket modify`](Manage-Ticket.md#subcommand-modify).

`discord`
: Available if this ticket is for a Discord ban. Changes the user which this ticket is for. Accepts a numeric
Discord ID.

`roblox`
: Available if this ticket is for a Roblox ban. Changes the user which this ticket is for. Supports
standard [Player type](Parameter-Types.md#player) format.