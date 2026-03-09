# Create Ticket Button

<primary-label ref="bot-command"/>
<secondary-label ref="staff-only-command"/>

Creates a message with a button attached to it that will create a ticket when clicked. Restricted to staff only, but
is not inherently dangerous; the button does the same thing as the [`/open`](Open-Ticket.md) command.

The title and description that show up in the message embed are hard-coded into the ticket type itself and will be
automatically assigned.

![An example showing what the result of this command looks like.](ticket-button-example.png)

## Usage

- `/create-ticket-button <type: Ticket Type>`

### Parameter Details:

- [`<type: Ticket Type>`](Parameter-Types.md#ticket-type) The type of ticket to create the button/message for.