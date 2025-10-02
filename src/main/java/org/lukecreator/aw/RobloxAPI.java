package org.lukecreator.aw;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.dv8tion.jda.api.entities.Message;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lukecreator.aw.data.DiscordRobloxLinks;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class RobloxAPI {
    private static final HttpClient client = HttpClient.newHttpClient();
    private static final String USERS_SEARCH_API = "https://users.roblox.com/v1/usernames/users";
    private static final String USER_INFO_API = "https://users.roblox.com/v1/users/";
    private static final String GAMEPASSES_API = "https://apis.roblox.com/game-passes/v1/game-passes/%d/product-info";
    private static final String AVATAR_BUST_API = "https://thumbnails.roblox.com/v1/users/avatar-bust?";
    private static final HashMap<Long, Gamepass> cachedGamepasses = new HashMap<>();
    private static final Pattern userMentionPattern = Message.MentionType.USER.getPattern();

    private static String getAvatarBustApiURL(long[] userIds, int size, boolean isCircular) {
        return "%suserIds=%s&size=%dx%d&format=Png&isCircular=%s".formatted(AVATAR_BUST_API,
                Stream.of(userIds).map(String::valueOf).collect(Collectors.joining(",")),
                size, size, String.valueOf(isCircular).toLowerCase());
    }

    private static String getAvatarBustApiURL(long userId, int size, boolean isCircular) {
        return "%suserIds=%d&size=%dx%d&format=Png&isCircular=%s".formatted(AVATAR_BUST_API, userId, size, size, String.valueOf(isCircular).toLowerCase());
    }

    private static long convertForeignDate(String d) {
        return Instant.parse(d).toEpochMilli();
    }

    /**
     * Retrieves a {@link User} object based on the input provided.
     * The input can be a Discord Mention, Roblox ID, or a username.
     * Attempts to determine the user by checking for Discord-Roblox links,
     * querying the Bloxlink API (if applicable), or searching Roblox records.
     *
     * @param input         The input string representing either a Discord ID (mention format),
     *                      Roblox ID, or username to identify the user.
     * @param allowBloxlink If true, the Bloxlink API may be used if a Discord mention is passed in.
     * @return A {@link User} object if a match is found, or null if no match exists.
     * @throws SQLException If a database access error occurs during the query.
     */
    public static User getUserByInput(String input, boolean allowBloxlink) throws SQLException {
        var matcher = userMentionPattern.matcher(input);
        if (matcher.matches()) {
            // unwrap ping into a Discord ID and then try to find a link
            long discordId = Long.parseLong(matcher.group(1));
            Long robloxId = DiscordRobloxLinks.robloxIdFromDiscordId(discordId);
            if (robloxId == null) {
                // no link, can't find the user based on this
                // try to use Bloxlink API
                if (!allowBloxlink)
                    return null;
                robloxId = BloxlinkAPI.lookupRobloxId(discordId);
                if (robloxId == null)
                    return null; // no bloxlink records, or out of free API usage
            }

            // robloxId is valid and not null here
            return getUserById(robloxId);
        }

        try {
            // id input
            long id = Long.parseLong(input);
            User byId = getUserById(id);
            if (byId != null)
                return byId;
            // fall back to the default, username input
            return getUserByCurrentUsername(input);
        } catch (NumberFormatException ignored) {
            // fall back to the default, username input
            return getUserByCurrentUsername(input);
        }
    }

    /**
     * Requests and returns information for the given user ID on Roblox.
     *
     * @param userId The Roblox ID of the user.
     * @return A {@link User} or null if the user couldn't be found.
     */
    public static User getUserById(long userId) {
        String url = USER_INFO_API + userId;
        HttpRequest request = HttpRequest
                .newBuilder(URI.create(url))
                .header("Accept", "application/json")
                .GET()
                .build();

        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200)
                return null;
            JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
            return new User(
                    userId,
                    json.get("name").getAsString(),
                    json.get("description").getAsString(),
                    convertForeignDate(json.get("created").getAsString())
            );
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Requests and returns a list of Roblox users that match the given username.
     * The method matches old and current usernames, so multiple users may be returned.
     *
     * @param username The username to search for. Case-insensitive.
     * @return An array of {@link User}s that match the given username or matched it in the past. Maybe empty!
     */
    public static User[] searchUsersByUsername(String username) {
        HttpRequest request = beginUsernameSearch(username);

        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200)
                return new User[0];
            JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
            JsonArray userList = json.getAsJsonArray("data").getAsJsonArray();
            User[] users = new User[userList.size()];
            for (int i = 0; i < userList.size(); i++) {
                JsonObject user = userList.get(i).getAsJsonObject();
                long userId = user.get("id").getAsLong();
                users[i] = getUserById(userId);
            }
            return users;
        } catch (Exception e) {
            return new User[0];
        }
    }

    /**
     * Requests and returns a single Roblox user which matches the given username currently.
     *
     * @param username The username to search for. Case-insensitive.
     * @return Null if the user couldn’t be found.
     */
    public static User getUserByCurrentUsername(String username) {
        HttpRequest request = beginUsernameSearch(username);

        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200)
                return null;
            JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
            JsonArray userList = json.getAsJsonArray("data").getAsJsonArray();
            long chosenUserId = 0L;

            for (int i = 0; i < userList.size(); i++) {
                JsonObject user = userList.get(i).getAsJsonObject();
                long currentUserId = user.get("id").getAsLong();
                String currentUserName = user.get("name").getAsString();
                if (currentUserName.equalsIgnoreCase(username)) {
                    chosenUserId = currentUserId;
                    break;
                }
            }

            if (chosenUserId == 0L)
                return null; // didn't find anyone with the requested username currently

            // get the full info of this user by ID
            return getUserById(chosenUserId);
        } catch (Exception e) {
            return null;
        }
    }

    private static HttpRequest beginUsernameSearch(String username) {
        JsonObject requestBody = new JsonObject();
        JsonArray usernames = new JsonArray();
        usernames.add(username);
        requestBody.add("usernames", usernames);
        requestBody.addProperty("excludeBannedUsers", false);

        return HttpRequest
                .newBuilder(URI.create(USERS_SEARCH_API))
                .header("Accept", "application/json")
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody.toString()))
                .build();
    }

    /**
     * Generates and returns the URL of the avatar bust image for the given Roblox user ID.
     * This method fetches the avatar image by making an external HTTP request to the Roblox API.
     * If the HTTP request fails or the response is invalid, this method returns null.
     *
     * @param userId The Roblox ID of the user whose avatar bust image URL is to be retrieved.
     * @return A string representing the URL of the user's avatar bust image, or null if unable to retrieve.
     */
    public static @Nullable String renderAvatarBustImageURL(long userId) {
        String requestURL = getAvatarBustApiURL(userId, 352, false);
        HttpRequest request = HttpRequest.newBuilder(URI.create(requestURL))
                .header("Accept", "application/json")
                .GET()
                .build();

        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200)
                return null;
            JsonObject uselessJson = JsonParser.parseString(response.body()).getAsJsonObject();
            JsonArray usefulJsonList = uselessJson.getAsJsonArray("data").getAsJsonArray();
            JsonObject usefulJson = usefulJsonList.get(0).getAsJsonObject();
            return usefulJson.get("imageUrl").getAsString();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Requests and returns information about a specific Roblox Gamepass given its ID.
     *
     * @param gamepassId The unique identifier of the Gamepass on Roblox.
     * @return A {@link Gamepass} object containing the Gamepass ID, its Robux cost, name,
     * and description, or null if the Gamepass couldn’t be retrieved or an error occurs.
     */
    public static Gamepass getGamepassById(long gamepassId) {
        if (cachedGamepasses.containsKey(gamepassId))
            return cachedGamepasses.get(gamepassId);

        URI uri = URI.create(String.format(GAMEPASSES_API, gamepassId));
        HttpRequest request = HttpRequest
                .newBuilder(uri)
                .headers("Accept", "application/json")
                .GET()
                .build();

        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200)
                return null;
            JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
            int robuxCost = json.get("PriceInRobux").getAsInt();
            String name = json.get("Name").getAsString();
            String description = json.get("Description").getAsString();
            Gamepass pass = new Gamepass(gamepassId, robuxCost, name, description);
            cachedGamepasses.put(gamepassId, pass);
            return pass;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Requests and returns an array of Roblox Gamepasses for the given Gamepass IDs.
     * Each Gamepass provides details such as ID, name, description, and Robux cost.
     * If a Gamepass can’t be retrieved for a given ID, it will be excluded from the returned array.
     *
     * @param gamepassIds The unique identifiers of the Gamepasses to retrieve.
     * @return An array of {@link Gamepass} objects corresponding to the provided IDs.
     * May be empty if none of the Gamepasses could be retrieved.
     */
    public static Gamepass[] getGamepassesById(long... gamepassIds) {
        List<Gamepass> gamepasses = new ArrayList<>();
        for (long gamepassId : gamepassIds) {
            Gamepass gamepass = getGamepassById(gamepassId);
            if (gamepass != null)
                gamepasses.add(gamepass);
        }
        return gamepasses.toArray(new Gamepass[0]);
    }


    public record User(long userId, String username, String bio, long creationDate) {
        @NotNull
        @Override
        public String toString() {
            return String.format("%d - %s (created %tB %<te, %<tY)", this.userId, this.username, this.creationDate);
        }

        public String getProfileURL() {
            return "https://www.roblox.com/users/" + this.userId + "/profile";
        }
    }

    public record Gamepass(long gamepassId, int robuxCost, String name, String description) {
        @NotNull
        @Override
        public String toString() {
            return String.format("%d - %s (%d robux)", this.gamepassId, this.name, this.robuxCost);
        }

        public String getURL() {
            return "https://www.roblox.com/game-pass/" + this.gamepassId + '/';
        }
    }
}