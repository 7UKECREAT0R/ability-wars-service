# Player Report

<primary-label ref="ticket-type"/>

Player report tickets make up the majority of the tickets staff deal with regularly. They are created by players who
have gathered evidence of another player committing a bannable offense, such as exploiting, abusing a bug, tab
glitching, etc.

The name format is `report-######`

## Questions

Username
: The username of the rule-breaker. Fully supports the [Player type](Parameter-Types.md#player). This must resolve to a
valid player not currently banned from the game.

Rule Broken
: A dropdown list which allows the user to select the rule that was broken, making it easier for the staff to determine
if the report is valid. The available options are:
: - Exploiting/Cheats
: - Bug Abuse
: - Tab Glitching
: - Inappropriate Engineer Build

Evidence
: A link to the video footage of the reported player. The video must clearly show the incident as-described, and it must
be public and accessible by the staff members.
: If the user wants to upload the video directly into Discord, this field can be left blank. In this case, the user will
be prompted after opening the ticket to upload the video; the prompt will continue sending until evidence is uploaded.

Timestamp and Extra Details
: Information about the incident and the video footage. If the video is longer than 10–20 seconds, the user must provide
a timestamp (`M:SS` format is fine) where the incident occurs in the video. The user can also provide any other details
they feel necessary.
: This information will be permanently attached to the video internally and can be recalled later in
the future, so it's relevant and useful.

## Action Buttons

The buttons which are present on the <tooltip term="ticket-panel">control panel</tooltip>. Most of them are staff-only.

### Cancel

Cancels the ticket. The ticket creator can also use this button to close their own ticket if they change their mind.
The user will be prompted to confirm this action before it's executed.

### Resolve

Prompts the staff member to enter a reason for resolving the ticket, then closes it. The user will see the reason
entered in a private message.

### Resolve (Bad Evidence)

Closes the ticket because the evidence provided is not sufficient.
This button has a pre-programmed reason for convenience.

### Resolve (Not Bannable)

Closes the ticket because the thing the user is reporting for is not a bannable offense.
This button has a pre-programmed reason for convenience.

### Ban

Bans the reported player and closes the ticket after the exploit name is entered for logging purposes.
A report is automatically created for the staff member.
This is basically a confirmation that the evidence is sufficient and the report should be taken care of.
This button has a pre-programmed reason for convenience.

### Ban (override close reason)

Bans the reported player and closes the ticket after the exploit name and reason is entered for logging purposes.
A report is automatically created for the staff member.
This is basically a confirmation that the evidence is sufficient and the report should be taken care of.

### Tempban

Temporarily bans the reported player and closes the ticket after the exploit name is entered for logging purposes, and
the ban duration is entered, in days.
A report is automatically created for the staff member.
This is basically a confirmation that the evidence is sufficient and the report should be taken care of.
This button has a pre-programmed reason for convenience.

### Tempban (override close reason)

Temporarily bans the reported player and closes the ticket after the exploit name and reason is entered for logging
purposes, and the ban duration is entered, in days.
A report is automatically created for the staff member.
This is basically a confirmation that the evidence is sufficient and the report should be taken care of.
This button has a pre-programmed reason for convenience.

## Properties

These are the properties that can be set on these tickets with [`/ticket modify`](Manage-Ticket.md#subcommand-modify).

`accused-user`
: Changes the accused user of the ticket; basically, the player who is being reported. This fully supports the
[Player type](Parameter-Types.md#player).

`add-extra-evidence`
: Adds a link to an extra piece of evidence to the ticket. This is useful for cases where the user has more evidence
and the bot only usually supports one link.
: **NOTE:** An easier way to do this is (and with support for Discord uploads) is to right-click the message with the
additional evidence and finding <ui-path>Apps | Ability Warden | Add as Evidence</ui-path>

`change-main-evidence`
: Changes the link to the main evidence of the ticket. After doing this, you will be prompted if you want to
delete/unregister the previous evidence as well.
: **NOTE:** An easier way to do this is (and with support for Discord uploads) is to right-click the message with the
new evidence and finding <ui-path>Apps | Ability Warden | Set as Main Evidence</ui-path>

`rule-broken`
: Changes the "rule broken" to a new value. This is useful for edge cases where the user has found something out of the
ordinary and you want to uphold correctness in our logging. Or, if the user straight-up picked the wrong option.