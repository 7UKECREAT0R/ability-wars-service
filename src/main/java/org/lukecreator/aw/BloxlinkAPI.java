package org.lukecreator.aw;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class BloxlinkAPI {
    private static final HttpClient client = HttpClient.newHttpClient();
    private static final String BLOXLINK_API_KEY = System.getenv("BLOXLINK_API_KEY");
    private static final String API_DISCORD_TO_ROBLOX = "https://api.blox.link/v4/public/guilds/922921165373202463/discord-to-roblox/%d";
    private static final String API_ROBLOX_TO_DISCORD = "https://api.blox.link/v4/public/guilds/922921165373202463/roblox-to-discord/%d";
    private static final Gson gsonInstance = new Gson();

    /**
     * Looks up the Roblox user ID associated with a given Discord user ID.
     *
     * @param discordId The Discord user ID to be looked up.
     * @return The associated Roblox user ID if found; otherwise, returns null.
     * May also return null if there is an issue with the request.
     */
    public static Long lookupRobloxId(long discordId) {
        String url = String.format(API_DISCORD_TO_ROBLOX, discordId);

        try {
            URI uri = new URI(url);
            HttpRequest request = HttpRequest.newBuilder(uri)
                    .headers("Authorization", BLOXLINK_API_KEY)
                    .GET().build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200)
                return null; // out of free requests or couldn't find anything.

            JsonObject json = gsonInstance.fromJson(response.body(), JsonObject.class);
            if (!json.has("robloxID"))
                return null;
            return Long.parseLong(json.get("robloxID").getAsString());
        } catch (URISyntaxException | NumberFormatException e) {
            throw new RuntimeException(e);
        } catch (IOException | InterruptedException ignored) {
            return null; // wut
        }
    }

    /**
     * Looks up the Discord user ID associated with a given Roblox user ID.
     *
     * @param robloxId The Roblox user ID to be looked up.
     * @return The associated Discord user ID if found; otherwise, returns null.
     */
    public static Long lookupDiscordId(long robloxId) {
        String url = String.format(API_ROBLOX_TO_DISCORD, robloxId);

        try {
            URI uri = new URI(url);
            HttpRequest request = HttpRequest.newBuilder(uri)
                    .headers("Authorization", BLOXLINK_API_KEY)
                    .GET().build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200)
                return null; // out of free requests or couldn't find anything.

            JsonObject json = gsonInstance.fromJson(response.body(), JsonObject.class);
            if (!json.has("discordIDs"))
                return null;
            JsonArray discordIds = json.getAsJsonArray("discordIDs");
            if (discordIds.isEmpty())
                return null;
            return discordIds.get(0).getAsLong();
        } catch (URISyntaxException | NumberFormatException e) {
            throw new RuntimeException(e);
        } catch (IOException | InterruptedException ignored) {
            return null; // wut
        }
    }
}
