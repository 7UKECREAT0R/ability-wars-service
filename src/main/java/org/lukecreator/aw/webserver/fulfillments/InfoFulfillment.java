package org.lukecreator.aw.webserver.fulfillments;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.lukecreator.aw.AWDatabase;
import org.lukecreator.aw.RobloxAPI;
import org.lukecreator.aw.data.AWBan;
import org.lukecreator.aw.data.AWPlayer;
import org.lukecreator.aw.webserver.Fulfillment;
import org.lukecreator.aw.webserver.PendingRequest;
import org.lukecreator.aw.webserver.PendingRequestType;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;

public class InfoFulfillment extends Fulfillment {
    public final long userId;
    public final String username;
    public final long punches;
    public final long[] gamepasses;
    public final AWBan[] bans;


    private InfoFulfillment(boolean hasRequestId, long requestId,
                            long userId, String username, long punches, long[] gamepasses, AWBan[] bans) {
        super(hasRequestId, requestId, PendingRequestType.INFO);
        this.userId = userId;
        this.username = username;
        this.punches = punches;
        this.gamepasses = gamepasses;
        this.bans = bans;
        Arrays.sort(bans, Comparator.comparingLong(AWBan::starts));
    }

    public static InfoFulfillment parse(JsonObject json) {
        boolean hasRequestId = json.has("id");
        long requestId = json.has("id") ? json.get("id").getAsLong() : -1;

        long userId = json.get("user").getAsLong();
        String username = json.get("username").getAsString();
        long punches = json.get("punches").getAsLong();

        ArrayList<Long> _gamepassesList = new ArrayList<>();
        if (json.has("gamepasses")) {
            JsonElement gamepassesElement = json.get("gamepasses");
            if (!gamepassesElement.isJsonNull() && gamepassesElement.isJsonArray()) {
                JsonArray gamepassesJson = gamepassesElement.getAsJsonArray();
                for (int i = 0; i < gamepassesJson.size(); i++) {
                    JsonElement element = gamepassesJson.get(i);
                    if (!element.isJsonPrimitive()) {
                        continue;
                    }
                    _gamepassesList.add(element.getAsLong());
                }
            }
        }
        long[] gamepasses = _gamepassesList
                .stream()
                .distinct()
                .mapToLong(Long::longValue)
                .toArray();

        List<AWBan> bans = new ArrayList<>();
        final Consumer<JsonObject> addBan = ban -> bans.add(AWBan.fromFulfillmentJSON(userId, ban));

        if (json.has("ban")) {
            JsonElement banElement = json.get("ban");
            if (!banElement.isJsonNull()) {
                if (banElement.isJsonObject()) {
                    addBan.accept(banElement.getAsJsonObject());
                } else if (banElement.isJsonArray()) {
                    JsonArray banArray = banElement.getAsJsonArray();
                    for (JsonElement singleBan : banArray) {
                        if (singleBan.isJsonObject()) {
                            addBan.accept(singleBan.getAsJsonObject());
                        }
                    }
                }
            }
        }

        return new InfoFulfillment(hasRequestId, requestId,
                userId, username, punches, gamepasses, bans.toArray(new AWBan[0]));
    }

    @Override
    public void process(PendingRequest request) throws SQLException {
        // commit all this info to the database
        AWPlayer player = AWDatabase.loadPlayer(this.userId, true, true, true, false);

        if (this.username != null && !this.username.isBlank() && !this.username.equalsIgnoreCase(player.username()))
            player.setUsername(this.username);

        if (this.bans != null) {
            for (AWBan ban : this.bans)
                player.bans.addBan(ban);
        }

        if (this.punches != player.stats.punches()) {
            player.stats.setPunches(this.punches);
        }

        if (!Arrays.equals(this.gamepasses, player.stats.gamepasses())) {
            player.stats.setGamepasses(this.gamepasses);
        }
    }


    /**
     * Resolves the gamepasses associated with the current user into an array of
     * {@code RobloxAPI.Gamepass} objects. If the user has no associated gamepasses,
     * an empty array is returned.
     *
     * @return An array of {@code RobloxAPI.Gamepass} objects representing the
     * gamepasses associated with the current user, or an empty array
     * if no gamepasses are associated.
     */
    public RobloxAPI.Gamepass[] resolveGamepasses() {
        if (this.gamepasses == null || this.gamepasses.length == 0)
            return new RobloxAPI.Gamepass[0];
        return RobloxAPI.getGamepassesById(this.gamepasses);
    }

    /**
     * Returns the names of the gamepasses associated with the returned user info.
     *
     * @return An array of strings containing the names of the user's gamepasses.
     * If the user has no gamepasses, an empty array is returned.
     */
    public String[] gamepassNames() {
        return Arrays.stream(this.resolveGamepasses())
                .map(RobloxAPI.Gamepass::name)
                .toList().toArray(new String[0]);
    }
}
