package org.lukecreator.aw.data;

import org.lukecreator.aw.AWDatabase;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.*;

/**
 * Represents an updatable list of bans for a particular user.
 * <p>
 * Specifically, this is an interface for interacting with the internal database.
 * No method here directly affects anything in the game, only the database.
 */
public class AWBans {
    /**
     * the ID of the user associated with each ban in this list.
     */
    public final long userId;
    private final ArrayList<AWBan> bans;

    /**
     * @param userId The ID of the user associated with each ban.
     * @param bans   The list of bans to pre-populate this list with.
     */
    public AWBans(long userId, AWBan... bans) {
        this.userId = userId;
        this.bans = new ArrayList<>();
        if (bans != null) {
            Collections.addAll(this.bans, bans);
            Arrays.sort(bans, Comparator.comparingLong(AWBan::starts));
        }
    }

    /**
     * Counts the number of distinct bans issued by a specific moderator (or staff member) within a specified time range.
     * Only bans that have not been overturned (unbanned) are considered.
     *
     * @param staffRobloxId The Roblox ID of the staff member responsible for issuing the bans.
     * @param weekStart     The start timestamp (inclusive) of the week-long time range in milliseconds.
     * @param weekEnd       The end timestamp (inclusive) of the week-long time range in milliseconds.
     * @return The count of distinct bans issued by the specified staff member within the given time range.
     * If there are no bans, returns 0.
     * @throws SQLException If an error occurs while querying the database.
     */
    public static int countBansByStaff(long staffRobloxId, long weekStart, long weekEnd) throws SQLException {
        final String query = """
                SELECT COUNT(DISTINCT ban.user_id)
                FROM bans ban
                LEFT JOIN unbans unban ON ban.user_id = unban.user_id
                                  AND unban.date > ban.starts
                WHERE ban.responsible_moderator = ?
                  AND ban.starts >= ? AND ban.starts <= ?
                  AND unban.user_id IS NULL
                """;

        try (PreparedStatement statement = AWDatabase.connection.prepareStatement(query)) {
            statement.setLong(1, staffRobloxId);
            statement.setLong(2, weekStart);
            statement.setLong(3, weekEnd);

            try (ResultSet result = statement.executeQuery()) {
                if (result.next()) {
                    return result.getInt(1);
                }
                return 0;
            }
        }

    }

    /**
     * Loads a list of bans associated with a specific user ID from the database.
     *
     * @param userId The ID of the user whose bans are to be loaded.
     * @return An {@link AWBans} object containing the user's bans. If an issue occurs during the database operation,
     * an empty {@link AWBans} object associated with the given user ID is returned.
     */
    public static AWBans loadFromDatabase(long userId) {
        try (var statement = AWDatabase.connection.prepareStatement(
                """
                        SELECT user_id, responsible_moderator, reason, starts, ends, linked_ticket, is_legacy
                        FROM bans
                        WHERE user_id = ?
                        ORDER BY starts""")) {
            statement.setLong(1, userId);
            try (var resultSet = statement.executeQuery()) {
                ArrayList<AWBan> bans = new ArrayList<>();
                while (resultSet.next()) {
                    bans.add(new AWBan(
                            resultSet.getLong("user_id"),
                            resultSet.getLong("responsible_moderator"),
                            resultSet.getString("reason"),
                            resultSet.getLong("starts"),
                            resultSet.getLong("ends"),
                            resultSet.getLong("linked_ticket"),
                            resultSet.getBoolean("is_legacy")
                    ));
                }
                return new AWBans(userId, bans.toArray(new AWBan[0]));
            }
        } catch (Exception e) {
            System.err.println("Issue occurred when loading bans (" + userId + ") from database: " + e);
            return new AWBans(userId);
        }
    }

    public static AWBans empty(long userId) {
        return new AWBans(userId);
    }


    /**
     * Determines whether the user is currently banned based on the list of bans.
     * A user is considered currently banned if their most recent ban has not ended yet
     * or if the ban's end time is unspecified.
     *
     * @return {@code true} if the user is currently banned; {@code false} otherwise.
     */
    public boolean isCurrentlyBanned() {
        if (this.bans.isEmpty())
            return false;
        AWBan lastBan = this.bans.get(this.bans.size() - 1);
        return lastBan.ends() == null || lastBan.ends() > System.currentTimeMillis();
    }

    /**
     * Returns the last (current) ban on record. This method DOESN'T guarantee that the user is currently banned.
     * Use {@link #isCurrentlyBanned()} to figure that part out.
     */
    public AWBan currentBan() {
        return this.bans.get(this.bans.size() - 1);
    }

    /**
     * Sets the last (current) ban to end at the given timestamp.
     *
     * @param ends The unix millisecond timestamp that the current ban ends.
     */
    public void setBanEnds(long ends) throws SQLException {
        var statement = AWDatabase.connection.prepareStatement("""
                UPDATE bans
                SET ends = ?
                WHERE user_id = ?
                AND starts = (
                    SELECT MAX(starts)
                    FROM bans
                    WHERE user_id = ?
                )""");

        statement.setLong(1, ends);
        statement.setLong(2, this.userId);
        statement.setLong(3, this.userId);
        statement.executeUpdate();

        if (this.bans.isEmpty())
            return; // do nothing, since player is not banned.

        // do the same thing with the local cache
        AWBan lastBan = this.bans.get(this.bans.size() - 1);
        this.bans.set(this.bans.size() - 1, new AWBan(
                lastBan.userId(),
                lastBan.responsibleModerator(),
                lastBan.reason(),
                lastBan.starts(),
                ends,
                lastBan.linkedTicketId,
                lastBan.isLegacy()
        ));
    }


    /**
     * Clears all bans associated with the user from both the internal list and the database.
     *
     * @throws SQLException If an issue occurs while clearing the banlist.
     */
    public void clearBans() throws SQLException {
        var statement = AWDatabase.connection.prepareStatement("DELETE FROM bans WHERE user_id = ?");
        statement.setLong(1, this.userId);
        statement.execute();
        this.bans.clear();
    }

    /**
     * Adds a ban to the database and the internal list of bans.
     * If any ban on record has the same start time as the input ban, this operation will do nothing (it's a duplicate).
     *
     * @param ban The {@link AWBan} object representing the ban to be added. It includes details such as the user ID,
     *            the responsible moderator (can be null), the reason for the ban, the start time, and the end time (can be null).
     * @throws SQLException If an issue occurs while inserting the ban into the database.
     */
    public void addBan(AWBan ban) throws SQLException {
        // check if this ban overlaps any other ban. if so, it doesn't need to be registered.
        if (!this.bans.isEmpty()) {
            for (AWBan sampleBan : this.bans) {
                if (sampleBan.starts() == ban.starts()) {
                    // remove any/all bans for this user that have the same starting timestamp
                    long userId = ban.userId();
                    long startsTimestamp = ban.starts();
                    var statement = AWDatabase.connection.prepareStatement("DELETE FROM bans WHERE user_id = ? AND starts = ?");
                    statement.setLong(1, userId);
                    statement.setLong(2, startsTimestamp);
                    statement.execute();
                }
            }
            // apply to the local cache too
            this.bans.removeIf(test -> test.starts() == ban.starts());
        }

        var statement = AWDatabase.connection.prepareStatement("INSERT INTO bans VALUES (?, ?, ?, ?, ?, ?, ?)");
        statement.setLong(1, ban.userId());

        if (ban.responsibleModerator() == null)
            statement.setNull(2, Types.INTEGER);
        else
            statement.setLong(2, ban.responsibleModerator());

        statement.setString(3, ban.reason());
        statement.setLong(4, ban.starts());

        if (ban.ends() == null)
            statement.setNull(5, Types.INTEGER);
        else
            statement.setLong(5, ban.ends());

        statement.setNull(6, Types.INTEGER);
        statement.setBoolean(7, ban.isLegacy());
        statement.execute();
        this.bans.add(ban);
    }

    public int size() {
        return this.bans.size();
    }

    public AWBan[] getBans() {
        return this.bans.toArray(new AWBan[0]);
    }

    public Collection<AWBan> iterateBans() {
        return Collections.unmodifiableCollection(this.bans);
    }
}
