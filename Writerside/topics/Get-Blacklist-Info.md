# Get Blacklist Info

<primary-label ref="bot-command"/>
<secondary-label ref="staff-only-command"/>

Gets information about a user's blacklist status from [appealing](Ban-Appeal.md)/[disputing](Ban-Dispute.md).
Blacklists can either be applied on a per-roblox-user basis, or on a per-discord-account basis.

### Usages

- [`/appeal-blacklist-info discord <user: Discord User>`](#subcommand-discord)
- [`/appeal-blacklist-info roblox <user: Player>`](#subcommand-roblox)

## Subcommand: `discord` {collapsible="true" default-state="collapsed"}

Gets information about a Discord user's blacklist status from [appealing](Ban-Appeal.md)/[disputing](Ban-Dispute.md).

### Usage {id="usage_1"}

- `/appeal-blacklist-info discord <user: Discord User>`

#### Parameter Details:

- [`<user: Discord User>`](Parameter-Types.md#discord-user) The Discord user to get the blacklist status of.

## Subcommand: `roblox` {collapsible="true" default-state="collapsed"}

Gets information about a Roblox user's blacklist status from [appealing](Ban-Appeal.md)/[disputing](Ban-Dispute.md).

### Usage {id="usage_2"}

- `/appeal-blacklist-info roblox <user: Player>`

#### Parameter Details: {id="parameter-details_1"}

- [`<user: Player>`](Parameter-Types.md#player) The Roblox user to get the blacklist status of.