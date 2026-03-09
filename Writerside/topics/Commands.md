# Commands

The "Ability Warden" Discord bot is loaded with various commands related to the game, mainly moderation. Some commands
interact with the game's API as well. This is a table-of-contents for all the available commands.

Each page includes the syntax of the command and description of its parameters and what they do.
Parameters marked with **&lt;angle&gt;** brackets are required, and parameters with **[square]** brackets are optional.

## Public Commands

- [`/aw-ban-status <target: Player> [get-evidence: Boolean]`](Get-Ban-Status.md)
- [`/aw-discord-ban-status <user: Discord User|Player>`](Get-Discord-Ban-Status.md)
- [`/aw-stats <target: Player>`](Get-Stats.md)

## Staff Commands

### Actions

- [`/aw-ban <target: Player> <reason: Text>`](Ban-Player.md)
- [`/aw-tempban <target: Player> <days: Integer> <reason: Text>`](Ban-Player-Temporarily.md)
- [`/aw-unban <user: Player>`](Unban-Player.md)

### Blacklist-Related

- [`/appeal-blacklist discord <user: Discord User> [reason: Text]`](Blacklist-User.md#subcommand-discord)
- [`/appeal-blacklist roblox <user: Player> [reason: Text]`](Blacklist-User.md#subcommand-roblox)
- [`/appeal-blacklist-remove discord <user: Discord User>`](Blacklist-Remove.md#subcommand-discord)
- [`/appeal-blacklist-remove roblox <user: Player>`](Blacklist-Remove.md#subcommand-roblox)
- [`/appeal-blacklist-info discord <user: Discord User>`](Get-Blacklist-Info.md#subcommand-discord)
- [`/appeal-blacklist-info roblox <user: Player>`](Get-Blacklist-Info.md#subcommand-roblox)

### Ticket-Specific

- [`/ticket blacklist [reason: Text]`](Manage-Ticket.md#subcommand-blacklist)
- [`/ticket close [reason: Text]`](Manage-Ticket.md#subcommand-close)
- [`/ticket modify <property: Ticket Property> <value: Text>`](Manage-Ticket.md#subcommand-modify)
- [`/ticket nickname <name: Text>`](Manage-Ticket.md#subcommand-nickname)
- [`/ticket top`](Manage-Ticket.md#subcommand-top)

### Ticket-Related

- [`/ticket cleanup`](Manage-Ticket.md#subcommand-cleanup)
- [`/ticket history <user: Discord User> [type-filter: Ticket Type]`](Manage-Ticket.md#subcommand-history)
- [
  `/ticket history-by-closer <closer: Discord User> [type-filter: Ticket Type]`](Manage-Ticket.md#subcommand-history-by-closer)
- [`/ticket history-by-recent <limit: Integer>`](Manage-Ticket.md#subcommand-history-by-recent)
- [`/ticket recall <ticket-id: Integer>`](Manage-Ticket.md#subcommand-recall)

## Administrative Commands

- [`/create-ticket-button <type: Ticket Type>`](Create-Ticket-Button.md)
