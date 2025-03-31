package org.lukecreator.aw.webserver.requests;

import com.google.gson.JsonObject;
import org.lukecreator.aw.webserver.PendingRequest;
import org.lukecreator.aw.webserver.PendingRequestType;

public class UnbanRequest extends PendingRequest {
    /**
     * The ID of the player to unban.
     */
    private final long userIdToUnban;
    /**
     * The Roblox ID of the moderator responsible for this unban. Required for authorization.
     */
    private final long responsibleModerator;

    /**
     * Represents a request to unban a user in the Ability Wars Roblox server.
     *
     * @param requestId            The ID of the request. Use {@link PendingRequest#getNextRequestId()}
     * @param userIdToUnban        The ID of the player to be unbanned.
     * @param responsibleModerator The Roblox ID of the moderator responsible for this unban.
     *                             Required for authorization.
     */
    public UnbanRequest(long requestId, long userIdToUnban, long responsibleModerator) {
        super(requestId, PendingRequestType.UNBAN);
        this.userIdToUnban = userIdToUnban;
        this.responsibleModerator = responsibleModerator;
    }

    @Override
    protected JsonObject _getJsonRepresentation() {
        JsonObject output = new JsonObject();
        output.addProperty("user", this.userIdToUnban);
        output.addProperty("responsible_user", this.responsibleModerator);
        return output;
    }
}