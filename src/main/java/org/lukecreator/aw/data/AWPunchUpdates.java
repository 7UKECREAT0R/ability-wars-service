package org.lukecreator.aw.data;

import org.lukecreator.aw.AWDatabase;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;

/**
 * Represents an updatable list of punch updates for a particular player.
 * Which user the punch updates belong to is not enforced in code but should be using logic.
 * <p>
 * Specifically, this is an interface for interacting with the internal database.
 * No method here directly affects anything in the game, only the database.
 */
public class AWPunchUpdates {
    /**
     * the ID of the user associated with each ban in this list.
     */
    public final long userId;
    private final ArrayList<AWPunchUpdate> punchUpdates;

    /**
     * @param userId       The ID of the user associated with each punch update.
     * @param punchUpdates The list of punch updates to pre-populate this list with.
     */
    public AWPunchUpdates(long userId, AWPunchUpdate... punchUpdates) {
        this.userId = userId;
        this.punchUpdates = new ArrayList<>();
        if (punchUpdates != null) {
            Collections.addAll(this.punchUpdates, punchUpdates);
        }
    }

    /**
     * Loads a list of punch updates associated with a specific user ID from the database.
     *
     * @param userId The ID of the user whose punch updates are to be loaded.
     * @return An {@link AWPunchUpdates} object containing the user's punch updates. If an issue occurs during the
     * database operation, an empty {@link AWPunchUpdates} object associated with the given user ID is returned.
     */
    public static AWPunchUpdates loadFromDatabase(long userId) {
        try (var statement = AWDatabase.connection.prepareStatement(
                """
                        SELECT user_id, responsible_moderator, date, old_punches, new_punches
                        FROM punch_update_records
                        WHERE user_id = ?
                        ORDER BY date""")) {
            statement.setLong(1, userId);
            try (var resultSet = statement.executeQuery()) {
                ArrayList<AWPunchUpdate> punchUpdates = new ArrayList<>();
                while (resultSet.next()) {
                    punchUpdates.add(new AWPunchUpdate(
                            resultSet.getLong("user_id"),
                            resultSet.getLong("responsible_moderator"),
                            resultSet.getLong("date"),
                            resultSet.getLong("old_punches"),
                            resultSet.getLong("new_punches")
                    ));
                }
                return new AWPunchUpdates(userId, punchUpdates.toArray(new AWPunchUpdate[0]));
            }
        } catch (Exception e) {
            System.err.println("Issue occurred when loading punch update records (" + userId + ") from database: " + e);
            return new AWPunchUpdates(userId);
        }
    }

    public static AWPunchUpdates empty(long userId) {
        return new AWPunchUpdates(userId);
    }

    /**
     * Clears all punch update records associated with the user from both the internal list
     * and the database.
     *
     * @throws SQLException If an issue occurs while clearing the records from the database.
     */
    public void clearRecords() throws SQLException {
        var statement = AWDatabase.connection.prepareStatement("DELETE FROM punch_update_records WHERE user_id = ?");
        statement.setLong(1, this.userId);
        statement.execute();
        this.punchUpdates.clear();
    }

    /**
     * Adds a punch update record to the database and the internal list of punch update records.
     *
     * @param record The {@link AWPunchUpdate} object representing the punch update to be added. It contains details such as
     *               the affected user ID, the responsible moderator (if any), the date of the update, the old punch value,
     *               and the new punch value.
     * @throws SQLException If an issue occurs while inserting the record into the database.
     */
    public void addRecord(AWPunchUpdate record) throws SQLException {
        var statement = AWDatabase.connection.prepareStatement("INSERT INTO punch_update_records VALUES (?, ?, ?, ?, ?)");
        statement.setLong(1, record.userId());

        if (record.responsibleModerator() == null)
            statement.setNull(2, java.sql.Types.INTEGER);
        else
            statement.setLong(2, record.responsibleModerator());

        statement.setLong(3, record.date());
        statement.setLong(4, record.oldPunches());
        statement.setLong(5, record.newPunches());

        statement.execute();
        this.punchUpdates.add(record);
    }
}
