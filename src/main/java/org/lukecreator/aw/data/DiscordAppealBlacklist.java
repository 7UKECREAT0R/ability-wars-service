package org.lukecreator.aw.data;

import net.dv8tion.jda.api.entities.User;
import org.jetbrains.annotations.Nullable;
import org.lukecreator.aw.AWDatabase;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * DAO interface for loading/applying appeal blacklists for Discord users.
 * <p>
 * For appeal-blacklisting Roblox users, see the {@link AWPlayer#setBlacklist(String, User)} and {@link AWPlayer#removeBlacklist()} APIs.
 */
public class DiscordAppealBlacklist {
    /**
     * The Discord ID of the user blacklisted.
     */
    public final long discordId;
    /**
     * The Discord ID of the moderator that issued the blacklist.
     */
    public final long issuerId;
    /**
     * Can be null. The reason the blacklist was issued.
     */
    @Nullable
    public final String reason;
    /**
     * The date of the blacklist.
     */
    public final long date;

    public DiscordAppealBlacklist(long discordId, long issuerId, @Nullable String reason, long date) {
        this.discordId = discordId;
        this.issuerId = issuerId;
        this.reason = reason;
        this.date = date;
    }

    /**
     * Checks if a Discord user is blacklisted in the database.
     *
     * @param discordId The unique identifier of the Discord user being checked.
     * @return {@code true} if the user is blacklisted, otherwise {@code false}.
     * @throws SQLException If a database access error occurs or the query fails.
     */
    public static boolean isBlacklisted(long discordId) throws SQLException {
        var statement = AWDatabase.connection.prepareStatement("""
                SELECT 1 FROM discord_appeal_blacklists WHERE discord_id = ? LIMIT 1""");
        statement.setLong(1, discordId);
        try (ResultSet results = statement.executeQuery()) {
            return results.next();
        }
    }

    /**
     * Adds or updates a blacklist entry for a Discord user in the database.
     * If the user is already blacklisted, the existing entry is updated with the provided data.
     *
     * @param discordId The unique identifier of the Discord user being blacklisted.
     * @param issuerId  The unique identifier of the user or system issuing the blacklist.
     * @param reason    The reason for blacklisting the Discord user. Can be {@code null}.
     * @param date      The timestamp of when the blacklist action occurred, represented as a long.
     * @throws SQLException If a database access error occurs or the operation fails.
     */
    public static void push(long discordId, long issuerId, @Nullable String reason, long date) throws SQLException {
        new DiscordAppealBlacklist(discordId, issuerId, reason, date).pushToDatabase();
    }

    /**
     * Removes a Discord user from the appeal blacklist in the database.
     *
     * @param discordId The unique identifier of the Discord user to be removed from the appeal blacklist.
     * @throws SQLException If a database access error occurs or the operation fails.
     */
    public static void remove(long discordId) throws SQLException {
        var statement = AWDatabase.connection.prepareStatement("""
                DELETE FROM discord_appeal_blacklists WHERE discord_id = ?""");
        statement.setLong(1, discordId);
        statement.executeUpdate();
    }

    /**
     * Retrieves a DiscordAppealBlacklist instance for the given Discord user ID by querying the database. Returns {@code null} if the user is not blacklisted.
     *
     * @param discordId The unique identifier of the Discord user to retrieve the appeal blacklist for.
     * @return A DiscordAppealBlacklist instance populated with the retrieved data, or {@code null} if no entry is found for the given Discord user ID.
     * @throws SQLException If a database access error occurs or the query fails.
     */
    public static DiscordAppealBlacklist get(long discordId) throws SQLException {
        var statement = AWDatabase.connection.prepareStatement("""
                SELECT appeal_blacklist_reason, appeal_blacklist_date, appeal_blacklist_issuer
                FROM discord_appeal_blacklists
                WHERE discord_id = ?""");
        statement.setLong(1, discordId);
        try (var results = statement.executeQuery()) {
            if (!results.next()) {
                return null;
            }
            return new DiscordAppealBlacklist(discordId,
                    results.getLong(3),
                    results.getString(1),
                    results.getLong(2)
            );
        }
    }

    /**
     * Pushes the current appeal blacklist entry to the database.
     * If an entry for the specified Discord user already exists, it updates the existing record with the new data.
     * If no entry exists, it inserts a new record into the database.
     * <p>
     * The method uses a SQL "ON CONFLICT" statement to handle updates in the case of duplicates, ensuring
     * that the record remains consistent with the provided details for the Discord user.
     *
     * @throws SQLException If an error occurs while executing the SQL upsert command.
     */
    public void pushToDatabase() throws SQLException {
        var statement = AWDatabase.connection.prepareStatement("""
                INSERT INTO discord_appeal_blacklists (discord_id, appeal_blacklist_reason, appeal_blacklist_date, appeal_blacklist_issuer)
                VALUES (?, ?, ?, ?)
                ON CONFLICT (discord_id) DO UPDATE SET
                                                       appeal_blacklist_reason = excluded.appeal_blacklist_reason,
                                                       appeal_blacklist_date = excluded.appeal_blacklist_date,
                                                       appeal_blacklist_issuer = excluded.appeal_blacklist_issuer""");
        statement.setLong(1, this.discordId);
        statement.setString(2, this.reason);
        statement.setLong(3, this.date);
        statement.setLong(4, this.issuerId);
        statement.executeUpdate();
    }
}
