package org.lukecreator.aw.data;

import org.lukecreator.aw.AWDatabase;

import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collections;

/**
 * Represents an updatable list of unbans for a particular user.
 * Which user the unbans belong to is not enforced in code but should be using logic.
 * <p>
 * Specifically, this is an interface for interacting with the internal database.
 * No method here directly affects anything in the game, only the database.
 */
public class AWUnbans {
    /**
     * the ID of the user associated with each unban in this list.
     */
    public final long userId;
    private final ArrayList<AWUnban> unbans;

    /**
     * @param userId The ID of the user associated with each unban.
     * @param bans   The list of unbans to pre-populate this list with.
     */
    public AWUnbans(long userId, AWUnban... bans) {
        this.userId = userId;
        this.unbans = new ArrayList<>();
        if (bans != null) {
            Collections.addAll(this.unbans, bans);
        }
    }

    /**
     * Loads a list of unbans associated with a specific user ID from the database.
     *
     * @param userId The ID of the user whose unbans are to be loaded.
     * @return An {@link AWUnbans} object containing the user's unbans. If an issue occurs during the database operation,
     * an empty {@link AWUnbans} object associated with the given user ID is returned.
     */
    public static AWUnbans loadFromDatabase(long userId) {
        try (var statement = AWDatabase.connection.prepareStatement(
                """
                        SELECT user_id, responsible_moderator, date
                        FROM unbans
                        WHERE user_id = ?
                        ORDER BY date""")) {
            statement.setLong(1, userId);
            try (var resultSet = statement.executeQuery()) {
                ArrayList<AWUnban> unbans = new ArrayList<>();
                while (resultSet.next()) {
                    unbans.add(new AWUnban(
                            resultSet.getLong("user_id"),
                            resultSet.getLong("responsible_moderator"),
                            resultSet.getLong("date")
                    ));
                }
                return new AWUnbans(userId, unbans.toArray(new AWUnban[0]));
            }
        } catch (Exception e) {
            System.err.println("Issue occurred when loading unbans (" + userId + ") from database: " + e);
            return new AWUnbans(userId);
        }
    }

    public static AWUnbans empty(long userId) {
        return new AWUnbans(userId);
    }

    /**
     * Clears all unbans associated with the user from both the internal list and the database.
     *
     * @throws SQLException If an issue occurs while clearing the unban list.
     */
    public void clearBans() throws SQLException {
        var statement = AWDatabase.connection.prepareStatement("DELETE FROM unbans WHERE user_id = ?");
        statement.setLong(1, this.userId);
        statement.execute();
        this.unbans.clear();
    }


    /**
     * Adds an unban record to the database and the internal list of unbans.
     *
     * @param unban The {@link AWUnban} object representing the unban to be added. It includes details such as the user ID,
     *              the responsible moderator (can be null), and the date of the unban.
     * @throws SQLException If an issue occurs while inserting the unban into the database.
     */
    public void addUnban(AWUnban unban) throws SQLException {
        var statement = AWDatabase.connection.prepareStatement("INSERT INTO unbans VALUES (?, ?, ?)");
        statement.setLong(1, unban.userId());

        if (unban.responsibleModerator() == null)
            statement.setNull(2, Types.INTEGER);
        else
            statement.setLong(2, unban.responsibleModerator());

        statement.setLong(3, unban.date());

        statement.execute();
        this.unbans.add(unban);
    }
}
