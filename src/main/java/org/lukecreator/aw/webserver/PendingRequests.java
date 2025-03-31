package org.lukecreator.aw.webserver;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.lukecreator.aw.webserver.fulfillments.NoPermissionFulfillment;

import java.util.Collection;
import java.util.HashMap;
import java.util.stream.Collectors;

/**
 * Stores the queue of requests that need fulfillment from Ability Wars.
 */
public class PendingRequests {
    private static final HashMap<Long, PendingRequest> PENDING_REQUESTS = new HashMap<>();

    /**
     * Removes all expired requests from the pending requests queue.
     * <p>
     * This method iterates through all pending requests and determines whether each request
     * has expired based on its timestamp and a predefined expiration period: {@link PendingRequest#REQUEST_KEEP_ALIVE}.
     * The current system time is used for comparison to check
     * the expiration status of each request.
     */
    public static void removeExpiredRequests() {
        final long currentTime = System.currentTimeMillis();

        PENDING_REQUESTS.values().stream()
                .filter(request -> isRequestExpired(request, currentTime))
                .map(request -> request.requestId)
                .collect(Collectors.toSet())
                .forEach(PENDING_REQUESTS::remove);
    }

    private static boolean isRequestExpired(PendingRequest request, long currentTime) {
        return (request.timestamp + PendingRequest.REQUEST_KEEP_ALIVE) < currentTime;
    }

    /**
     * Retrieves a JSON representation of all pending requests in the queue.
     * Each pending request is represented as a JSON object and added to a JSON array
     * under the "requests" property.
     *
     * @return A JsonObject containing all pending requests, where each request is structured
     * as a JSON object within the "requests" array.
     */
    public static JsonObject getPendingRequestsJSON() {
        JsonObject output = new JsonObject();
        JsonArray array = new JsonArray();

        for (PendingRequest request : PENDING_REQUESTS.values()) {
            array.add(request.getJsonRepresentation());
        }

        output.add("requests", array);
        return output;
    }

    /**
     * Retrieves a pending request by its unique ID from the queue of pending requests.
     *
     * @param id The unique identifier of the pending request to retrieve.
     * @return The {@link PendingRequest} associated with the given ID, or null if no such request exists.
     */
    public static PendingRequest get(long id) {
        return PENDING_REQUESTS.getOrDefault(id, null);
    }

    /**
     * Adds a new request into the pending list. It should technically be responded to once the next poll request
     * from Ability Wars is received, and the callback should be called.
     *
     * @param request The request to add.
     */
    public static void add(PendingRequest request) {
        PENDING_REQUESTS.put(request.requestId, request);
    }

    /**
     * Processes the fulfillment of a pending request. This method handles different types of fulfillments,
     * including those that indicate no permission for the request or others that require additional processing.
     * Fulfilled requests are removed from the pending queue upon completion.
     *
     * @param fulfillment The fulfillment object containing the necessary details to satisfy a pending request.
     *                    If the fulfillment is of type {@link NoPermissionFulfillment}, a no-permission response
     *                    is sent for the associated request. Otherwise, the method processes the request using
     *                    the fulfillment data and removes it from the queue.
     */
    public static void fulfill(Fulfillment fulfillment) {
        if (fulfillment instanceof NoPermissionFulfillment) {
            PendingRequest r = PENDING_REQUESTS.remove(fulfillment.requestId);
            r.noPermission();
        } else {
            try {
                PendingRequest request = null;
                if (fulfillment.hasRequestId)
                    request = PENDING_REQUESTS.get(fulfillment.requestId);
                fulfillment.process(request);
            } catch (java.sql.SQLException e) {
                throw new RuntimeException(e);
            } finally {
                if (fulfillment.hasRequestId) {
                    PendingRequest request = PENDING_REQUESTS.remove(fulfillment.requestId);
                    if (request != null) {
                        request.fulfill(fulfillment);
                    }
                }
            }
        }

    }

    public static int size() {
        return PENDING_REQUESTS.size();
    }

    public static Collection<PendingRequest> values() {
        return PENDING_REQUESTS.values();
    }
}
