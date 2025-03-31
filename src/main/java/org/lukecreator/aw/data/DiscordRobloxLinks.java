package org.lukecreator.aw.data;

import org.lukecreator.aw.AWDatabase;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;

/**
 * API for interfacing with the database for Discord >< Roblox links.
 */
public class DiscordRobloxLinks {
    /**
     * Creates a record linking a Discord ID to a Roblox ID in the database.
     * If a record with the given Discord ID and Roblox ID already exists, no action will be taken.
     *
     * @param discordId The Discord ID to be linked.
     * @param robloxId  The Roblox ID to be linked.
     * @throws SQLException If a database access error occurs or the SQL statement execution fails.
     */
    public static void createLink(long discordId, long robloxId) throws SQLException {
        var statement = AWDatabase.connection.prepareStatement("""
                INSERT INTO discord_roblox_links (discord_id, roblox_id) VALUES (?, ?) ON CONFLICT DO NOTHING""");
        statement.setLong(1, discordId);
        statement.setLong(2, robloxId);
        statement.executeUpdate();
    }

    /**
     * Removes the record associated with the given Discord ID from the discord_roblox_links database table.
     *
     * @param discordId The Discord ID whose associated record is to be removed.
     * @throws SQLException If a database access error occurs or the SQL statement execution fails.
     */
    public static void removeByDiscordId(long discordId) throws SQLException {
        var statement = AWDatabase.connection.prepareStatement("""
                DELETE FROM discord_roblox_links WHERE discord_id = ?""");
        statement.setLong(1, discordId);
        statement.executeUpdate();
    }

    /**
     * Removes the record associated with the given Roblox ID from the discord_roblox_links database table.
     *
     * @param robloxId The Roblox ID whose associated record is to be removed.
     * @throws SQLException If a database access error occurs or the SQL statement execution fails.
     */
    public static void removeByRobloxId(long robloxId) throws SQLException {
        var statement = AWDatabase.connection.prepareStatement("""
                DELETE FROM discord_roblox_links WHERE roblox_id = ?""");
        statement.setLong(1, robloxId);
    }

    /**
     * Retrieves the Roblox ID associated with a given Discord ID from the database.
     *
     * @param discordId The Discord ID for which the associated Roblox ID is being queried.
     * @return The Roblox ID associated with the provided Discord ID, or null if no association exists.
     * @throws SQLException If a database access error occurs or the query execution fails.
     */
    public static Long robloxIdFromDiscordId(long discordId) throws SQLException {
        var statement = AWDatabase.connection.prepareStatement("""
                SELECT roblox_id from discord_roblox_links where discord_id = ?""");
        statement.setLong(1, discordId);
        try (var results = statement.executeQuery()) {
            if (!results.next()) {
                return null;
            }
            return results.getLong(1);
        }
    }

    /**
     * Retrieves the Discord ID associated with a given Roblox ID from the database.
     *
     * @param robloxId The Roblox ID for which the associated Discord ID is being queried.
     * @return The Discord ID associated with the provided Roblox ID, or null if no association exists.
     * @throws SQLException If a database access error occurs or the query execution fails.
     */
    public static Long discordIdFromRobloxId(long robloxId) throws SQLException {
        var statement = AWDatabase.connection.prepareStatement("""
                SELECT discord_id from discord_roblox_links where roblox_id = ?""");
        statement.setLong(1, robloxId);
        try (var results = statement.executeQuery()) {
            if (!results.next()) {
                return null;
            }
            return results.getLong(1);
        }
    }

    /**
     * Retrieves all Discord-Roblox ID links from the database.
     * <p>
     * The method queries the `discord_roblox_links` table in the database
     * to collect all records linking Discord user IDs to Roblox user IDs,
     * and returns them as a map where the key is the Discord ID and the value
     * is the associated Roblox ID.
     *
     * @return A HashMap containing Discord IDs as keys and their associated Roblox IDs as values.
     * @throws SQLException If a database access error occurs or the query execution fails.
     */
    public static HashMap<Long, Long> getAllLinks() throws SQLException {
        PreparedStatement statement = AWDatabase.connection.prepareStatement("""
                SELECT discord_id, roblox_id
                FROM discord_roblox_links""");
        HashMap<Long, Long> links = new HashMap<>();

        try (statement) {
            try (ResultSet results = statement.executeQuery()) {
                while (results.next()) {
                    long discordId = results.getLong(1);
                    long robloxId = results.getLong(2);
                    links.put(discordId, robloxId);
                }
            }
        }

        return links;
    }
}
