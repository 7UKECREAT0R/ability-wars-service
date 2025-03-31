package org.lukecreator.aw.webserver;

import org.lukecreator.aw.webserver.fulfillments.*;
import org.lukecreator.aw.webserver.requests.BanRequest;
import org.lukecreator.aw.webserver.requests.InfoRequest;
import org.lukecreator.aw.webserver.requests.SetPunchesRequest;
import org.lukecreator.aw.webserver.requests.UnbanRequest;

import java.util.Arrays;

/**
 * A type of request that can be made to the Ability Wars Roblox server.
 */
public enum PendingRequestType {
    /**
     * Requesting info about a particular player by ID.
     */
    INFO("info", InfoRequest.class, InfoFulfillment.class),
    /**
     * Requesting the banning of a player.
     */
    BAN("ban", BanRequest.class, BanFulfillment.class),
    /**
     * Requesting the unbanning of a player.
     */
    UNBAN("unban", UnbanRequest.class, UnbanFulfillment.class),
    /**
     * Requesting a player's punch count be set.
     */
    SET_PUNCHES("setpunches", SetPunchesRequest.class, SetPunchesFulfillment.class),
    NO_PERMISSION("nopermission", null, NoPermissionFulfillment.class);

    public final String identifier;
    public final Class<? extends PendingRequest> requestClass;
    public final Class<? extends Fulfillment> fullfillmentClass;

    PendingRequestType(String identifier, Class<? extends PendingRequest> requestClass, Class<? extends Fulfillment> fullfillmentClass) {
        this.identifier = identifier;
        this.requestClass = requestClass;
        this.fullfillmentClass = fullfillmentClass;
    }

    public static PendingRequestType byIdentifier(String identifier) {
        return Arrays.stream(PendingRequestType.values())
                .filter(type -> type.identifier.equalsIgnoreCase(identifier))
                .findFirst()
                .orElse(null);
    }
}
