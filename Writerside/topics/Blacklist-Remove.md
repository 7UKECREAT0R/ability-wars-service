# Blacklist Remove

<primary-label ref="bot-command"/>
<secondary-label ref="staff-only-command"/>

Removes a blacklist from a user, allowing them to create a [appeal](Ban-Appeal.md) or [dispute](Ban-Dispute.md) again.
Blacklists can either be applied on a per-roblox-user basis, or on a per-discord-account basis.

### Usages

- [`/appeal-blacklist-remove discord <user: Discord User>`](#subcommand-discord)
- [`/appeal-blacklist-remove roblox <user: Player>`](#subcommand-roblox)

## Subcommand: `discord` {collapsible="true" default-state="collapsed"}

Removes a blacklist from a Discord user, allowing them to create a [appeal](Ban-Appeal.md) or [dispute](Ban-Dispute.md)
again.

### Usage {id="usage_1"}

- `/appeal-blacklist-remove discord <user: Discord User>`

#### Parameter Details:

- [`<user: Discord User>`](Parameter-Types.md#discord-user) The Discord user to remove the blacklist from.

## Subcommand: `roblox` {collapsible="true" default-state="collapsed"}

Removes a blacklist from a Roblox user, allowing them to create a [appeal](Ban-Appeal.md) or [dispute](Ban-Dispute.md)
again.

### Usage {id="usage_2"}

- `/appeal-blacklist-remove roblox <user: Player>`

#### Parameter Details: {id="parameter-details_1"}

- [`<user: Player>`](Parameter-Types.md#player) The Roblox user to remove the blacklist from.