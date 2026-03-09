# Ban Dispute

<primary-label ref="ticket-type"/>

Ban disputes are requests from banned users to be unbanned because their ban was invalid or otherwise unjust.
Disputes can be either for in-game bans, or Discord server bans. The tickets will adapt depending on the type.
If the ban was issued by the anti-cheat, a dispute cannot be opened and the decision is considered final.

The name format is `dispute-######`

### The Evidence Agreement

When a dispute is opened, a clickable message will be sent in the channel telling the user that we have video evidence
of the incident (it doesn't check, but 99% of the time this is correct). They will be asked if they give consent for us
to review the video. If they click the deny button, they will be asked to explain why.

> This agreement has no weight and is only there to make users incriminate themselves and/or reconsider telling the
> truth. An overwhelming majority of disputes we get are complete lies, so there are lots of silly layers in place to
> catch as many out as possible. It's foul play, yeah, but it works.

{style="warning"}

## Questions

Where are you banned from?
: Discord or Roblox. This will determine how the ticket is formatted and what actions/properties will be available.

User ID
: If the user picked "Discord," this is their numeric Discord ID. If they picked "Roblox," this should be their numeric
Roblox ID. The user may get confused and enter a username instead of an ID, in which a failsafe will be provided for
both types.

What were you false-banned for?
: What the user believes they were falsely banned for. This should be a direct and clear answer, and it should match
up with the evidence we have.

What do you think happened?
: How the user believes the false ban happened. Don't read too much into this but it can be a good indicator of if the
user is telling the truth or not.

Agreement
: The user must swear their answers are the full truth in writing. Unfortunately, most users who agree still lie
anyway.
But it's still a good way to make a few people rethink a decision to lie about their ban.

## Action Buttons

The buttons which are present on the <tooltip term="ticket-panel">control panel</tooltip>. All of them are staff-only
to prevent the user from backpedaling or otherwise changing their story. Once the information is entered, it cannot be
changed by the opener.

### For Both Roblox and Discord Tickets

#### Close

Prompts the staff member to enter a reason for closing the ticket, then closes it. The user will see the reason
entered in a private message.

#### Blacklist (lying)

Closes the ticket and blacklists the user from creating future [appeals](Ban-Appeal.md) and disputes. The reason for the
blacklist will be for lying about the circumstances surrounding their ban, and they'll be sent a private message with a
generic unrelated reason.

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