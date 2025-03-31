package org.lukecreator.aw.data.tickets;

import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;

/**
 * An action that can be taken on an open ticket.
 */
public class TicketAction {
    public final String displayName;
    public final ButtonStyle displayStyle;
    private final String id;
    private final long associatedTicketID;

    /**
     * Creates a new ticket action.
     *
     * @param id    The identifier of this action. An underscore is not allowed and will throw an {@link IllegalArgumentException}.
     * @param name  The display name of the button that calls this action.
     * @param style The style of the button that calls this action.
     */
    public TicketAction(String id, String name, ButtonStyle style, long associatedTicketID) {
        if (id.contains("_"))
            throw new IllegalArgumentException("Cannot have an underscore in the ID of a ticket action. (" + id + ")");
        this.id = id;
        this.displayName = name;
        this.displayStyle = style;
        this.associatedTicketID = associatedTicketID;
    }

    /**
     * Gets the ID of the Discord component attached to this action. You should probably use {@link #getButton()}.
     */
    public String getComponentId() {
        return "TA_" + this.id + "_" + this.associatedTicketID;
    }

    /**
     * Gets a Discord component that, when clicked, initiates this action.
     */
    public Button getButton() {
        return Button.of(this.displayStyle, this.getComponentId(), this.displayName);
    }
}
