package org.lukecreator.aw.webserver;

import com.google.gson.JsonObject;
import org.lukecreator.aw.webserver.fulfillments.*;

import java.sql.SQLException;

/**
 * Fulfillment of a {@link PendingRequest}.
 */
public abstract class Fulfillment {
    /**
     * If true, then {@link Fulfillment#requestId} is the ID of the {@link PendingRequest} that's being fulfilled.
     * <p>
     * If false, then this is just general info and {@link Fulfillment#requestId} is meaningless.
     */
    public final boolean hasRequestId;
    /**
     * If {@link Fulfillment#hasRequestId}, then this is the ID of the {@link PendingRequest} that's being fulfilled.
     */
    public final long requestId;
    /**
     * The type of request this is fulfilling. See {@link PendingRequestType}
     */
    public final PendingRequestType type;

    protected Fulfillment(boolean hasRequestId, long requestId, PendingRequestType type) {
        this.hasRequestId = hasRequestId;
        this.requestId = requestId;
        this.type = type;
    }

    /**
     * Parses a {@link JsonObject} to construct a specific type of {@link Fulfillment} object.
     * The type of fulfillment is determined by the "type" field present in the provided JSON.
     *
     * @param json A {@link JsonObject} containing the data required to create a {@link Fulfillment}.
     *             The JSON must include a "type" key to indicate the type of fulfillment and
     *             additional fields specific to the respective fulfillment type.
     * @return A {@link Fulfillment} object corresponding to the "type" specified in the JSON.
     * Possible return types include {@link InfoFulfillment}, {@link BanFulfillment},
     * {@link UnbanFulfillment}, and {@link SetPunchesFulfillment}.
     * @throws Exception If the "type" field is missing or contains an unsupported value.
     */
    public static Fulfillment parse(JsonObject json) throws Exception {
        String type = json.get("type").getAsString();

        return switch (type.toUpperCase()) {
            case "INFO" -> InfoFulfillment.parse(json);
            case "BAN" -> BanFulfillment.parse(json);
            case "UNBAN" -> UnbanFulfillment.parse(json);
            case "SETPUNCHES" -> SetPunchesFulfillment.parse(json);
            case "NOPERMISSION" -> NoPermissionFulfillment.parse(json);
            default -> throw new Exception("Unknown fulfillment type: " + type);
        };
    }

    /**
     * Process this fulfillment. Called after it's been parsed.
     *
     * @param request Can be null. If not, this will be the request that's being responded to with this fulfillment.
     * @throws SQLException If something goes wrong with the SQL queries.
     */
    public abstract void process(PendingRequest request) throws SQLException;
}
