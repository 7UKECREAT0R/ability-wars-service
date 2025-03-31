package org.lukecreator.aw.webserver.fulfillments;

import com.google.gson.JsonObject;
import org.lukecreator.aw.data.AWPlayer;
import org.lukecreator.aw.data.AWPunchUpdate;
import org.lukecreator.aw.webserver.Fulfillment;
import org.lukecreator.aw.webserver.PendingRequest;
import org.lukecreator.aw.webserver.PendingRequestType;

import java.sql.SQLException;

public class SetPunchesFulfillment extends Fulfillment {
    public final long userId;
    public final String username;
    public final Long responsibleModerator;

    public final long newPunches;
    public final long oldPunches;

    protected SetPunchesFulfillment(boolean hasRequestId, long requestId,
                                    long userId, String username, Long responsibleModerator,
                                    long newPunches, long oldPunches) {
        super(hasRequestId, requestId, PendingRequestType.SET_PUNCHES);
        this.userId = userId;
        this.username = username;
        this.responsibleModerator = responsibleModerator;
        this.newPunches = newPunches;
        this.oldPunches = oldPunches;
    }

    /**
     * Parses a {@link JsonObject} and constructs a new {@link SetPunchesFulfillment} object.
     *
     * @param json A {@link JsonObject} containing the data required to create a {@link SetPunchesFulfillment}.
     *             The JSON must include a "user" key with a long value for the user's ID, "new_punches"
     *             and "old_punches" keys with long values. Optionally, it may include an "id" key for the
     *             request ID, a "username" key with a string value, and a "responsible_user" key with a long value.
     * @return A {@link SetPunchesFulfillment} object populated with the data extracted from the provided JSON.
     */
    public static SetPunchesFulfillment parse(JsonObject json) {
        boolean hasRequestId = json.has("id");
        long requestId = json.has("id") ? json.get("id").getAsLong() : -1;

        long userId = json.get("user").getAsLong();
        String username = json.has("username") ? json.get("username").getAsString() : null;
        Long responsibleModerator = json.has("responsible_user") ? json.get("responsible_user").getAsLong() : null;

        long newPunches = json.get("new_punches").getAsLong();
        long oldPunches = json.get("old_punches").getAsLong();

        return new SetPunchesFulfillment(hasRequestId, requestId, userId, username, responsibleModerator, newPunches, oldPunches);
    }

    @Override
    public void process(PendingRequest request) throws SQLException {
        AWPlayer player = AWPlayer.loadFromDatabase(this.userId, true, false, false, true);
        player.stats.setPunches(this.newPunches);
        player.punchUpdates.addRecord(
                new AWPunchUpdate(this.userId, this.responsibleModerator, System.currentTimeMillis(),
                        this.oldPunches, this.newPunches)
        );
    }
}
