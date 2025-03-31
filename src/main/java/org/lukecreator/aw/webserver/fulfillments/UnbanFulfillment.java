package org.lukecreator.aw.webserver.fulfillments;

import com.google.gson.JsonObject;
import org.lukecreator.aw.AWDatabase;
import org.lukecreator.aw.data.AWPlayer;
import org.lukecreator.aw.webserver.Fulfillment;
import org.lukecreator.aw.webserver.PendingRequest;
import org.lukecreator.aw.webserver.PendingRequestType;

import java.sql.SQLException;

public class UnbanFulfillment extends Fulfillment {
    public final long userId;
    public final String username;
    public final Long responsibleModerator;

    protected UnbanFulfillment(boolean hasRequestId, long requestId, long userId, String username, Long responsibleModerator) {
        super(hasRequestId, requestId, PendingRequestType.UNBAN);
        this.userId = userId;
        this.username = username;
        this.responsibleModerator = responsibleModerator;
    }

    /**
     * Parses a {@link JsonObject} and constructs a new {@link UnbanFulfillment} object.
     *
     * @param json A {@link JsonObject} containing the data required to create an {@link UnbanFulfillment}.
     *             The JSON should include a "user" key with a long value for the user's ID, and optionally an "id" key
     *             for the request ID. It may also contain optional keys such as "username" and "responsible_user".
     * @return A {@link UnbanFulfillment} object populated with the data extracted from the provided JSON.
     */
    public static UnbanFulfillment parse(JsonObject json) {
        boolean hasRequestId = json.has("id");
        long requestId = json.has("id") ? json.get("id").getAsLong() : -1;

        long userId = json.get("user").getAsLong();
        String username = json.has("username") ? json.get("username").getAsString() : null;
        Long responsibleModerator = json.has("responsible_user") ? json.get("responsible_user").getAsLong() : null;

        return new UnbanFulfillment(hasRequestId, requestId, userId, username, responsibleModerator);
    }

    @Override
    public void process(PendingRequest request) throws SQLException {
        AWPlayer player = AWDatabase.loadPlayer(this.userId, false, true, true, false);
        player.unban(this.responsibleModerator);
    }
}
