# Get Discord Ban Status

<primary-label ref="bot-command"/>

Gets information about a user's ban status from the official Ability Wars Discord. You can check by either their
Discord account, or their Roblox account via. [Bloxlink](https://blox.link/).

Unfortunately, Discord doesn't provide information about the *date* a user was banned, so that needs to be checked
manually through Discord search. Someday.

## Usage

- `/aw-discord-ban-status <user: Discord User|Player>`

### Parameter Details:

- [`[user: Discord User]`](Parameter-Types.md#discord-user) Checks ban information of the given Discord user.
- [`[user: Player]`](Parameter-Types.md#player) Checks ban information by looking up the given player's Discord account
  and then checking.
