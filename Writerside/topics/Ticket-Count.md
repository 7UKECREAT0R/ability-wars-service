# Ticket Count

<primary-label ref="bot-command"/>
<secondary-label ref="staff-only-command"/>

Counts the number of bans done by the given staff member for the current/last pay period.

## Usage

- `/ticketcount <staff: Discord User> [lastweek: Boolean]`

### Parameter Details:

- [`<staff: Discord User>`](Parameter-Types.md#discord-user) The staff member to count the tickets for.
- [`[lastweek: Boolean]`](Parameter-Types.md#boolean) If true, the count will be run for the last pay period instead of
  the current one. Defaults to `False`.
