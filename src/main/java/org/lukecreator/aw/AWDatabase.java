package org.lukecreator.aw;

import org.lukecreator.aw.data.AWPlayer;

import java.sql.Connection;
import java.sql.DriverManager;

/**
 * Manages the SQLite backend.
 */
public class AWDatabase {
    private static final String DB_URL = System.getenv("AW_DB_URL");

    /**
     * The active database connection.
     */
    public static Connection connection = null;

    public static void init() throws Exception {
        connection = DriverManager.getConnection(DB_URL);
        connection.setAutoCommit(true);
        if (connection == null)
            throw new Exception("Something happened and the database couldn't be connected to.");
        System.out.println("Connected to database.");
    }

    /**
     * Loads an Ability Wars player from the database, including optional data like stats, bans, unbans,
     * and punch updates.
     *
     * @param userId The Roblox user ID of the player to load.
     * @return An instance of AWPlayer representing the loaded player with optional extras.
     */
    public static AWPlayer loadPlayer(long userId) {
        return AWPlayer.loadFromDatabase(userId, true, true, true, true);
    }

    /**
     * Loads an Ability Wars player by their Roblox username.
     *
     * @param username The Roblox username of the player to load.
     * @return An instance of AWPlayer representing the loaded player, or null if the username is not found or
     * if the player cannot otherwise be loaded from the database.
     */
    public static AWPlayer loadPlayerByName(String username) {
        RobloxAPI.User user = RobloxAPI.getUserByCurrentUsername(username);
        if (user == null)
            return null;
        return loadPlayer(user.userId());
    }

    /**
     * Loads an Ability Wars player from the database, including optional data like stats, bans, unbans,
     * and punch updates based on the specified parameters.
     *
     * @param userId           The Roblox user ID of the player to load.
     * @param loadStats        Whether to load the stats for the player.
     * @param loadBans         Whether to load the ban records for the player.
     * @param loadUnbans       Whether to load the unban records for the player.
     * @param loadPunchUpdates Whether to load the punch update records for the player.
     * @return An instance of AWPlayer representing the loaded player with optional extras or null if an
     * error occurs or the player does not exist in the database.
     */
    public static AWPlayer loadPlayer(long userId, boolean loadStats, boolean loadBans, boolean loadUnbans, boolean loadPunchUpdates) {
        return AWPlayer.loadFromDatabase(userId, loadStats, loadBans, loadUnbans, loadPunchUpdates);
    }

    /**
     * Loads an Ability Wars player from the database by their Roblox username, including optional data like stats, bans, unbans,
     * and punch updates based on the specified parameters.
     *
     * @param username         The current username of the player to load.
     * @param loadStats        Whether to load the stats for the player.
     * @param loadBans         Whether to load the ban records for the player.
     * @param loadUnbans       Whether to load the unban records for the player.
     * @param loadPunchUpdates Whether to load the punch update records for the player.
     * @return An instance of AWPlayer representing the loaded player with optional extras or null if an
     * error occurs or the player does not exist in the database.
     */
    public static AWPlayer loadPlayerByName(String username, boolean loadStats, boolean loadBans, boolean loadUnbans, boolean loadPunchUpdates) {
        RobloxAPI.User user = RobloxAPI.getUserByCurrentUsername(username);
        if (user == null)
            return null;
        return loadPlayer(user.userId(), loadStats, loadBans, loadUnbans, loadPunchUpdates);
    }
}
