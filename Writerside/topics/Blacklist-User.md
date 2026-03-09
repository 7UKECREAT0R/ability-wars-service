# Blacklist User

<primary-label ref="bot-command"/>
<secondary-label ref="staff-only-command"/>

Blacklists the given user from opening an [appeal](Ban-Appeal.md)/[dispute](Ban-Dispute.md) in the future.
Blacklists can either be applied on a per-roblox-user basis, or on a per-discord-account basis.

### Usages

- [`/appeal-blacklist discord <user: Discord User> [reason: Text]`](#subcommand-discord)
- [`/appeal-blacklist roblox <user: Player> [reason: Text]`](#subcommand-roblox)

## Subcommand: `discord` {collapsible="true" default-state="collapsed"}

Blacklists the given Discord user from opening a [appeal](Ban-Appeal.md)/[dispute](Ban-Dispute.md) in the future.

### Usage {id="usage_1"}

- `/appeal-blacklist discord <user: Discord User> [reason: Text]`

#### Parameter Details:

- [`<user: Discord User>`](Parameter-Types.md#discord-user) The Discord user to blacklist.
- [`<reason: Text>`](Parameter-Types.md#text) The reason for the blacklist. This is generally good to include so that
  future staff know why it is there and why it shouldn't be removed.

## Subcommand: `roblox` {collapsible="true" default-state="collapsed"}

Blacklists the given Roblox user from opening a [appeal](Ban-Appeal.md)/[dispute](Ban-Dispute.md) in the future.

### Usage {id="usage_2"}

- `/appeal-blacklist roblox <user: Player> [reason: Text]`

#### Parameter Details: {id="parameter-details_1"}

- [`<user: Player>`](Parameter-Types.md#player) The Roblox user to blacklist.
- [`<reason: Text>`](Parameter-Types.md#text) The reason for the blacklist. This is generally good to include so that
  future staff know why it is there and why it shouldn't be removed.