# Parameter Types

All commands and some form text fields accept certain kinds of parameters.
This page details each one and some examples of their usage.

## Text {collapsible="true" default-state="collapsed"}

Generic text, can be anything without any restrictions, except length when specified. Example:
`I don't like this user very much.`

## Integer {collapsible="true" default-state="collapsed"}

A whole number. Example: `500`, `34`, `7183`

## Boolean {collapsible="true" default-state="collapsed"}

A yes/no answer, denoted by either `True` or `False`. Discord will usually let you choose between these two options in
the UI.

## Player {collapsible="true" default-state="collapsed"}

An Ability Wars player. It accepts either a player's current Roblox username, Roblox ID, or Discord ping.
Generally, if your input is a valid roblox user, regardless of what it is, it should work.

### Roblox IDs

If the input is _fully_ a number, it will always be interpreted as a Roblox ID. An example could be `3233825722`.
This is how _any_ numbers are processed regardless of their length, so `123` will also be interpreted as a Roblox ID.

> If someone's username _is_ a number, you can prefix your input with an `@` symbol to have it be interpreted as a
> username instead. Example: `@8008135` will grab the player with the name "8008135", but with a different Roblox ID.

{style="note"}

### Discord Pings

If you ping a Discord user for this type, it will send a request to [Bloxlink](https://blox.link/) to attempt to get
their Roblox ID. If the user doesn't have their account linked in the **main** Ability Wars server, this will fail.

### Username

This is the fallback type, but the most commonly used. This will attempt to find a player by their current Roblox
username.
If you prefix your input with an `@` symbol, it will always be interpreted as a username, regardless of format.

## Ticket Type {collapsible="true" default-state="collapsed"}

The name of any type of ticket that exists, such as [`Player Report`](Player-Report.md), [
`Ban Appeal`](Ban-Appeal.md), [`Ban Dispute`](Ban-Dispute.md), etc. Discord will
usually let you choose between these options in the UI.

## Ticket Property {collapsible="true" default-state="collapsed"}

The name of a ticket-specific property. Commands that have this kind of parameter can only be used inside ticket
channels, and the kind of inputs allowed is dependent on the [ticket type](Ticket-Types.md). Discord will let you choose
between a list of available properties in the UI. Example: [`change-main-evidence`](Player-Report.md#properties),
[`rule-broken`](Player-Report.md#properties), etc.

## Discord User {collapsible="true" default-state="collapsed"}

A Discord user, backed by the Discord UI. Alternatively, you can supply the ID of a Discord user, which is helpful if
they're not showing up in the user picker. Example: `@lukecreator`