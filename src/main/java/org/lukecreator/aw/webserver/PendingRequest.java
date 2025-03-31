package org.lukecreator.aw.webserver;

import com.google.gson.JsonObject;

import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

public abstract class PendingRequest {
    /**
     * How long to keep new requests alive until they're removed prematurely from the list, presumably because the game
     * is having an internal error due to its presence.
     */
    public static final long REQUEST_KEEP_ALIVE = 1000 * 60 * 3; // 3 minutes
    private static final AtomicLong CURRENT_REQUEST_ID = new AtomicLong(0);
    /**
     * The ID of this request. Fulfillments of this request will include this ID.
     */
    public final long requestId;
    /**
     * The unix timestamp that this request was created at.
     */
    public final long timestamp = System.currentTimeMillis();
    /**
     * The type of this request. See {@link PendingRequestType}
     */
    public final PendingRequestType type;
    /**
     * If not null, the callback to call when this request is fulfilled.
     */
    private Consumer<Fulfillment> onFulfilledCallback;
    /**
     * If not null, the callback to call when this request is denied because of no permission.
     */
    private Runnable onNoPermissionCallback;

    protected PendingRequest(long requestId, PendingRequestType type) {
        this.requestId = requestId;
        this.type = type;
        this.onFulfilledCallback = null;
    }

    /**
     * Gets the next unused request ID. Thread-safe.
     *
     * @return A long representing a request ID that is yet unused.
     */
    public static long getNextRequestId() {
        return CURRENT_REQUEST_ID.incrementAndGet();
    }

    /**
     * Sets the callback that should run once this request is fulfilled.
     *
     * @param onFulfilledCallback The callback, with a parameter containing the {@link Fulfillment}
     *                            object returned by Ability Wars.
     * @return {@code this} object for method chaining.
     */
    public PendingRequest onFulfilled(Consumer<Fulfillment> onFulfilledCallback) {
        this.onFulfilledCallback = onFulfilledCallback;
        return this;
    }

    /**
     * Sets the callback that should be run if this request is denied due to missing permissions.
     *
     * @param onNoPermissionCallback The callback.
     * @return {@code this} object for method chaining.
     */
    public PendingRequest onNoPermission(Runnable onNoPermissionCallback) {
        this.onNoPermissionCallback = onNoPermissionCallback;
        return this;
    }

    /**
     * Represents the request as JSON to be sent to Ability Wars.
     *
     * @return A JSON object containing this request's information.
     */
    public JsonObject getJsonRepresentation() {
        JsonObject json = this._getJsonRepresentation();
        json.addProperty("id", this.requestId);
        json.addProperty("type", this.type.identifier);
        return json;
    }

    /**
     * Implemented for each request type.
     *
     * @return The JSON representation of this pending request.
     * The ID and type don't need to be added, those are added automatically.
     */
    protected abstract JsonObject _getJsonRepresentation();

    /**
     * Call the {@link #onFulfilledCallback} if it's not null.
     *
     * @param fulfillment The data returned by the Ability Wars server in response to this request.
     */
    public void fulfill(Fulfillment fulfillment) {
        if (this.onFulfilledCallback != null)
            this.onFulfilledCallback.accept(fulfillment);
    }

    /**
     * Call the {@link #onNoPermissionCallback} if it's not null.
     */
    public void noPermission() {
        if (this.onNoPermissionCallback != null)
            this.onNoPermissionCallback.run();
    }
}
