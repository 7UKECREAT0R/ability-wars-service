package org.lukecreator.aw;

import com.google.gson.Gson;
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
    private static final String BLOXLINK_API_URL = "https://api.blox.link/v4/public/guilds/922921165373202463/discord-to-roblox/%d";
    private static final Gson gsonInstance = new Gson();

    public static Long lookupRobloxId(long discordId) {
        String url = String.format(BLOXLINK_API_URL, discordId);

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
}
