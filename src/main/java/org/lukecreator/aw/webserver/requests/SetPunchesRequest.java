package org.lukecreator.aw.webserver.requests;

import com.google.gson.JsonObject;
import org.lukecreator.aw.webserver.PendingRequest;
import org.lukecreator.aw.webserver.PendingRequestType;

public class SetPunchesRequest extends PendingRequest {
    /**
     * The ID of the player to set the punches of.
     */
    public long userIdToSetPunches;
    /**
     * The Roblox ID of the moderator responsible for this action. Required for authorization.
     */
    public long responsibleModerator;
    /**
     * The number of punches the player should have.
     */
    public long punches;

    /**
     * Represents a request to set the punch count of a player on the Ability Wars Roblox server.
     *
     * @param requestId            The ID of the request. Use {@link PendingRequest#getNextRequestId()}
     * @param userIdToSetPunches   The ID of the player whose punches are being set.
     * @param responsibleModerator The Roblox ID of the moderator responsible for this action. Required for authorization.
     *                             Can be null if the moderator is unknown.
     * @param punches              The number of punches to set for the player.
     */
    public SetPunchesRequest(long requestId, long userIdToSetPunches, long responsibleModerator, long punches) {
        super(requestId, PendingRequestType.SET_PUNCHES);
        this.userIdToSetPunches = userIdToSetPunches;
        this.responsibleModerator = responsibleModerator;
        this.punches = punches;
    }

    @Override
    protected JsonObject _getJsonRepresentation() {
        JsonObject output = new JsonObject();
        output.addProperty("user", this.userIdToSetPunches);
        output.addProperty("responsible_user", this.responsibleModerator);
        output.addProperty("punches", this.punches);
        return output;
    }
}