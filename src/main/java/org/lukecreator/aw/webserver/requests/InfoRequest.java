package org.lukecreator.aw.webserver.requests;

import com.google.gson.JsonObject;
import org.lukecreator.aw.webserver.PendingRequest;
import org.lukecreator.aw.webserver.PendingRequestType;

public class InfoRequest extends PendingRequest {
    private final long userId;

    /**
     * Creates a new InfoRequest.
     *
     * @param requestId The ID of the request. Use {@link PendingRequest#getNextRequestId()}
     * @param userId    The Roblox ID of the player to get info for.
     */
    public InfoRequest(long requestId, long userId) {
        super(requestId, PendingRequestType.INFO);
        this.userId = userId;
    }

    @Override
    protected JsonObject _getJsonRepresentation() {
        JsonObject output = new JsonObject();
        output.addProperty("user", this.userId);
        return output;
    }
}
