# Get Ban Status

<primary-label ref="bot-command"/>

Checks if a user is banned from the Ability Wars game, along with additional information. By request, the command can
also retrieve the evidence attached to the ban.

## Usage

- `/aw-ban-status <target: Player> [get-evidence: Boolean]`

### Parameter Details:

- [`<target: Player>`](Parameter-Types.md#player) The player to check the ban status of. No restrictions on who.
- [`[get-evidence: Boolean]`](Parameter-Types.md#boolean) Restricted to staff only. If true, the evidence (if
  any) attached to the ban will be retrieved and included with the response. Additionally, the response will be made
  ephemeral so nobody else can see it. Defaults to `False`.