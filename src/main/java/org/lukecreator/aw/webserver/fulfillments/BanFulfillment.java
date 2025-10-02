package org.lukecreator.aw.webserver.fulfillments;

import com.google.gson.JsonObject;
import org.lukecreator.aw.data.AWBan;
import org.lukecreator.aw.data.AWBans;
import org.lukecreator.aw.data.Links;
import org.lukecreator.aw.webserver.Fulfillment;
import org.lukecreator.aw.webserver.PendingRequest;
import org.lukecreator.aw.webserver.PendingRequestType;
import org.lukecreator.aw.webserver.requests.BanRequest;

import java.sql.SQLException;
import java.util.Arrays;

public class BanFulfillment extends Fulfillment {
    public final AWBan ban;

    protected BanFulfillment(boolean hasRequestId, long requestId, AWBan ban) {
        super(hasRequestId, requestId, PendingRequestType.BAN);
        this.ban = ban;
    }

    /**
     * Parses a {@link JsonObject} and constructs a new {@link BanFulfillment} object.
     *
     * @param json A {@link JsonObject} containing the data required to create a {@link BanFulfillment}.
     *             The JSON should include a "user" key with a long value for the user's ID, and optionally an "id" key
     *             for the request ID. The content for the {@link AWBan} object should also be present in the JSON.
     * @return A {@link BanFulfillment} object populated with the data extracted from the provided JSON.
     */
    public static BanFulfillment parse(JsonObject json) {
        boolean hasRequestId = json.has("id");
        long requestId = json.has("id") ?
                json.get("id").getAsLong() :
                -1;

        AWBan ban = AWBan.fromFulfillmentJSON(json.get("user").getAsLong(), json);
        return new BanFulfillment(hasRequestId, requestId, ban);
    }

    @Override
    public void process(PendingRequest request) throws SQLException {
        long[] evidenceIds = null;
        if (request instanceof BanRequest) {
            Long evidenceId = ((BanRequest) request).evidenceId;
            Long ticketId = ((BanRequest) request).ticketId;

            if (ticketId != null) {
                this.ban.linkedTicketId = ticketId;

                evidenceIds = Arrays.stream(Links.TicketEvidenceLinks.getEvidenceLinkedToTicket(ticketId))
                        .mapToLong(evidence -> evidence.evidenceId)
                        .toArray();
                if (evidenceId != null) {
                    boolean dupe = false;
                    for (long evidence : evidenceIds) {
                        if (evidence == evidenceId) {
                            dupe = true;
                            break;
                        }
                    }
                    if (!dupe) {
                        // add the last one to the end, something apparently went wrong internally
                        evidenceIds = Arrays.copyOf(evidenceIds, evidenceIds.length + 1);
                        evidenceIds[evidenceIds.length - 1] = evidenceId;
                    }
                }
            }
        } else {
            // no request, so this was very likely a ban issued in-game.

        }

        AWBans bans = AWBans.loadFromDatabase(this.ban.userId());
        bans.addBan(this.ban);

        if (evidenceIds != null) {
            // register ban/evidence link(s)
            for (long evidenceId : evidenceIds)
                Links.BanEvidenceLinks.linkEvidenceToBan(this.ban, evidenceId);
        }
    }
}
