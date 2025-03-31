package org.lukecreator.aw.webserver.requests;

import com.google.gson.JsonObject;
import org.lukecreator.aw.webserver.Fulfillment;
import org.lukecreator.aw.webserver.PendingRequest;
import org.lukecreator.aw.webserver.PendingRequestType;

public class BanRequest extends PendingRequest {
    /**
     * If present, the ID of the evidence to link to the ban once it's fulfilled.
     * <p>
     * Not included in the JSON payload, it's just passed to the {@link Fulfillment#process(PendingRequest)} method on fulfillment.
     */
    public final Long evidenceId;
    /**
     * If present, the ID of the ticket to link to the ban once it's fulfilled.
     * <p>
     * Not included in the JSON payload, it's just passed to the {@link Fulfillment#process(PendingRequest)} method on fulfillment.
     */
    public final Long ticketId;

    /**
     * The ID of the player to ban.
     */
    private final long userIdToBan;
    /**
     * The Roblox ID of the moderator responsible for this ban. Required for authorization.
     */
    private final long responsibleModerator;
    /**
     * The reason for the ban. Can be null if no reason is specified.
     */
    private final String reason;
    /**
     * If the ban is permanent. If {@code true}, the field {@link #duration} will be ignored.
     */
    private final boolean permanent;
    /**
     * The duration of the ban, in milliseconds. Ignored if {@link #permanent} is true.
     */
    private final long duration; // if not permanent

    /**
     * Represents a request to ban a user in the Ability Wars Roblox server.
     *
     * @param requestId            The ID of the request. Use {@link PendingRequest#getNextRequestId()}
     * @param userIdToBan          The ID of the player to be banned.
     * @param responsibleModerator The Roblox ID of the moderator responsible for the ban. Required for authorization.
     * @param reason               The reason for the ban. Can be null if no reason is specified.
     * @param permanent            Indicates whether the ban is permanent. If true, the {@code duration} parameter is ignored.
     * @param duration             The duration of the ban in milliseconds. Ignored if {@code permanent} is true. Just use {@code 0L} instead.
     */
    public BanRequest(long requestId,
                      long userIdToBan, long responsibleModerator, String reason,
                      boolean permanent, long duration,
                      Long evidenceId, Long ticketId) {
        super(requestId, PendingRequestType.BAN);
        this.userIdToBan = userIdToBan;
        this.responsibleModerator = responsibleModerator;
        this.reason = reason;
        this.permanent = permanent;
        this.duration = duration;
        this.evidenceId = evidenceId;
        this.ticketId = ticketId;
    }

    @Override
    protected JsonObject _getJsonRepresentation() {
        JsonObject output = new JsonObject();
        output.addProperty("user", this.userIdToBan);
        output.addProperty("responsible_user", this.responsibleModerator);
        if (this.reason != null && !this.reason.isEmpty())
            output.addProperty("reason", this.reason);
        output.addProperty("length", this.permanent ? 0L : this.duration);
        return output;
    }
}