# Manage Ticket

<primary-label ref="bot-command"/>
<secondary-label ref="staff-only-command"/>

This is the master command for managing tickets. It has lots of subcommands, some of which require being executed
inside a ticket channel to work. All the subcommands can only by run by staff members.

## Usages

### Ticket-Specific

- [`/ticket blacklist [reason: Text]`](#subcommand-blacklist)
- [`/ticket close [reason: Text]`](#subcommand-close)
- [`/ticket modify <property: Ticket Property> <value: Text>`](#subcommand-modify)
- [`/ticket nickname <name: Text>`](#subcommand-nickname)
- [`/ticket top`](#subcommand-top)

### Global (can be run anywhere)

- [`/ticket cleanup`](#subcommand-cleanup)
- [`/ticket history <user: Discord User> [type-filter: Ticket Type]`](#subcommand-history)
- [`/ticket history-by-closer <closer: Discord User> [type-filter: Ticket Type]`](#subcommand-history-by-closer)
- [`/ticket history-by-recent <limit: Integer>`](#subcommand-history-by-recent)
- [`/ticket recall <ticket-id: Integer>`](#subcommand-recall)

## Subcommand: `blacklist` {collapsible="true" default-state="collapsed"}

Blacklists the ticket's creator depending on the type of ticket. This does *not* close the ticket, it's simply a
context-aware shortcut for the regular blacklisting process.

If the ticket is an [appeal](Ban-Appeal.md)/[dispute](Ban-Dispute.md), the user will be blacklisted from creating future
appeals/disputes. If the ticket was for a Roblox ban, they'll be blacklisted by Roblox user. If the ticket was for a
Discord server ban, they'll be blacklisted by Discord user.

If the ticket is a [player report](Player-Report.md), the user will be given the blacklist role and prevented from
creating future reports.

### Usage

- `/ticket blacklist [reason: Text]`

#### Parameter Details:

- [`[reason: Text]`](Parameter-Types.md#text) The reason for the blacklist.

## Subcommand: `close` {collapsible="true" default-state="collapsed"}

Closes the ticket with an optional reason attached. This reason will be messaged to the user privately.

### Usage {id="usage_1"}

- `/ticket close [reason: Text]`

#### Parameter Details: {id="parameter-details_1"}

- [`[reason: Text]`](Parameter-Types.md#text) The reason for closing the ticket. This _will_ be visible to the user.

## Subcommand: `modify` {collapsible="true" default-state="collapsed"}

Modifies a property of the current ticket. The properties available depend on the type of ticket; Discord will generally
show you all the available options automatically.

Properties aren't just values that can be changed, they can perform actions as well. More information about specific
properties is available on the [pages dedicated to each type of ticket](Ticket-Types.md).

### Usage {id="usage_2"}

- `/ticket modify <property: Ticket Property> <value: Text>`

#### Parameter Details: {id="parameter-details_2"}

- [`<property: Ticket Property>`](Parameter-Types.md#ticket-property) The property to modify. Dependent on the current
  ticket type.
- [`<value: Text>`](Parameter-Types.md#text) The new value for the property.

## Subcommand: `nickname` {collapsible="true" default-state="collapsed"}

Gives the current ticket a nickname. You don't have to include anything besides the nickname you want; the channel will
be renamed with the correct format for you. You can also use spaces in the nickname, if you'd like.

### Usage {id="usage_3"}

- `/ticket nickname [name: Text]`

#### Parameter Details: {id="parameter-details_3"}

- [`[name: Text]`](Parameter-Types.md#text) The new nickname for the ticket. Spaces are allowed.

## Subcommand: `top` {collapsible="true" default-state="collapsed"}

Creates a jump link to the top of the ticket, where the <tooltip term="ticket-panel">control panel</tooltip> is.

### Usage {id="usage_4"}

- `/ticket top`

## Subcommand: `cleanup` {collapsible="true" default-state="collapsed"}

Runs a cleanup routine to remove any bugged tickets and any bugged channels that are no longer attached to a
valid ticket.

### Usage {id="usage_5"}

- `/ticket cleanup`

## Subcommand: `history` {collapsible="true" default-state="collapsed"}

Gets ticket history for the given Discord user. A type filter can be specified to only show tickets of a certain type.
The history is limited to 25 results due to Discord's limitation on embeds-per-message.

### Usage {id="usage_6"}

- `/ticket history <user: Discord User> [type-filter: Ticket Type]`

#### Parameter Details: {id="parameter-details_4"}

- [`<user: Discord User>`](Parameter-Types.md#discord-user) The user whose history to get.
- [`[type-filter: Ticket Type]`](Parameter-Types.md#ticket-type) The type of ticket to filter by. Defaults to none,
  gathering tickets of any type.

## Subcommand: `history-by-closer` {collapsible="true" default-state="collapsed"}

Gets history of tickets closed by a given moderator. A type filter can be specified to only show tickets of a certain
type.
The history is limited to 25 results due to Discord's limitation on embeds-per-message.

### Usage {id="usage_7"}

- `/ticket history-by-closer <closer: Discord User> [type-filter: Ticket Type]`

#### Parameter Details: {id="parameter-details_5"}

- [`<closer: Discord User>`](Parameter-Types.md#discord-user) The moderator that closed the tickets.
- [`[type-filter: Ticket Type]`](Parameter-Types.md#ticket-type) The type of ticket to filter by. Defaults to none,
  gathering tickets of any type.

## Subcommand: `history-by-recent` {collapsible="true" default-state="collapsed"}

Gets history of recently closed tickets. The limit can be, at most, 25 due to a Discord limitation.

### Usage {id="usage_8"}

- `/ticket history-by-recent <limit: Integer>`

#### Parameter Details: {id="parameter-details_6"}

- [`<limit: Integer>`](Parameter-Types.md#integer) The number of recent tickets to retrieve. Must be at least 1, but at
  most 25.

## Subcommand: `recall` {collapsible="true" default-state="collapsed"}

Recall information about a previously closed ticket. This will include the original answers the user gave in the
ticket creation modal. This is good for fact-checking user's claims about past tickets or otherwise figuring out what
happened during an incident.

### Usage {id="usage_9"}

- `/ticket recall <ticket-id: Integer>`

#### Parameter Details: {id="parameter-details_7"}

- [`<ticket-id: Integer>`](Parameter-Types.md#integer) The numeric ID of the ticket to recall.