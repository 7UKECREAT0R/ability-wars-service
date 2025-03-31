package org.lukecreator.aw.webserver.fulfillments;

import com.google.gson.JsonObject;
import org.lukecreator.aw.webserver.Fulfillment;
import org.lukecreator.aw.webserver.PendingRequest;
import org.lukecreator.aw.webserver.PendingRequestType;

import java.sql.SQLException;

public class NoPermissionFulfillment extends Fulfillment {
    protected NoPermissionFulfillment(boolean hasRequestId, long requestId) {
        super(hasRequestId, requestId, PendingRequestType.NO_PERMISSION);
    }

    public static NoPermissionFulfillment parse(JsonObject json) {
        boolean hasRequestId = json.has("id");
        long requestId = json.has("id") ? json.get("id").getAsLong() : -1;
        return new NoPermissionFulfillment(hasRequestId, requestId);
    }

    @Override
    public void process(PendingRequest request) throws SQLException {
        // No-op
    }
}
