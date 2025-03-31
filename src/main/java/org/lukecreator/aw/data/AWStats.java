package org.lukecreator.aw.data;

import org.lukecreator.aw.AWDatabase;
import org.lukecreator.aw.RobloxAPI;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A record of an Ability Wars player's stats.
 * <p>
 * Specifically, this is an interface for interacting with the internal database.
 * No method here directly affects anything in the game, only the database.
 */
public final class AWStats {
    /**
     * The ID of the user whom these stats belong to.
     */
    private final long userId;
    /**
     * The identifiers of the gamepasses this player owns.
     */
    private long[] gamepasses;
    /**
     * The number of "punches" this player has.
     */
    private long punches;

    public AWStats(long userId, long punches, long[] gamepasses) {
        this.userId = userId;
        this.punches = punches;
        this.gamepasses = gamepasses;
    }

    public static AWStats loadFromDatabase(long userId) {
        try (var statement = AWDatabase.connection.prepareStatement("""
                SELECT user_id, punches, gamepasses
                FROM stats
                WHERE user_id = ?""")) {
            statement.setLong(1, userId);
            try (var results = statement.executeQuery()) {
                if (!results.next()) {
                    return new AWStats(userId, 0, new long[0]);
                }
                String gamepassesString = results.getString("gamepasses");
                List<Long> _gamepasses = (gamepassesString == null || gamepassesString.isBlank()) ?
                        new ArrayList<>() :
                        Arrays.stream(gamepassesString.split(","))
                                .map(Long::parseLong)
                                .toList();
                long[] gamepasses = _gamepasses.stream().mapToLong(Long::longValue).toArray();

                return new AWStats(
                        results.getLong("user_id"),
                        results.getLong("punches"),
                        gamepasses
                );
            }
        } catch (SQLException e) {
            System.err.println("Failed to load stats for user " + userId + ":\n\n" + e);
            return new AWStats(userId, 0, new long[0]);
        }
    }

    public static AWStats empty(long userId) {
        return new AWStats(userId, 0, new long[0]);
    }

    public long userId() {
        return this.userId;
    }

    public long punches() {
        return this.punches;
    }

    public long[] gamepasses() {
        return this.gamepasses;
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
     * Returns the names of the gamepasses associated with the current user.
     *
     * @return An array of strings containing the names of the user's gamepasses.
     * If the user has no gamepasses, an empty array is returned.
     */
    public String[] gamepassNames() {
        return Arrays.stream(this.resolveGamepasses())
                .map(RobloxAPI.Gamepass::name)
                .toList().toArray(new String[0]);
    }

    public void ensureDefaultStats() throws SQLException {
        var statement = AWDatabase.connection.prepareStatement("""
                INSERT INTO stats (user_id, punches, gamepasses)
                VALUES (?, 0, NULL)
                ON CONFLICT (user_id) DO NOTHING""");
        statement.setLong(1, this.userId);
        statement.executeUpdate();
    }

    /**
     * Updates the number of punches for the current user in the database and updates the internal state.
     *
     * @param punches The new number of punches to be set for the user.
     * @throws SQLException If an error occurs while updating the database.
     */
    public void setPunches(long punches) throws SQLException {
        this.ensureDefaultStats();
        var statement = AWDatabase.connection.prepareStatement("""
                UPDATE stats
                SET punches = ?
                WHERE user_id = ?"""
        );
        statement.setLong(1, punches);
        statement.setLong(2, this.userId);
        statement.executeUpdate();
        this.punches = punches;
    }

    /**
     * Adds a specified number of punches to the current user's total and updates the database.
     *
     * @param amount The number of punches to add to the user's total.
     * @throws SQLException If an error occurs while updating the database.
     */
    public void addPunches(long amount) throws SQLException {
        this.setPunches(this.punches + amount);
    }

    /**
     * Subtracts a specified number of punches from the current user's total and updates the database.
     *
     * @param amount The number of punches to subtract from the user's total.
     * @throws SQLException If an error occurs while updating the database.
     */
    public void subtractPunches(long amount) throws SQLException {
        this.setPunches(this.punches - amount);
    }

    /**
     * Updates the gamepasses associated with the current user in the database and updates the internal state.
     *
     * @param gamepasses An array of gamepasses (as Strings) to be associated with the user. If the array is null
     *                   or empty, the gamepasses will be set to null in the database.
     * @throws SQLException If an error occurs while updating the database.
     */
    public void setGamepasses(long[] gamepasses) throws SQLException {
        this.ensureDefaultStats();
        var statement = AWDatabase.connection.prepareStatement("""
                UPDATE stats
                SET gamepasses = ?
                WHERE user_id = ?"""
        );
        String gamepassesString = (gamepasses == null || gamepasses.length == 0) ? null : String.join(",",
                Arrays.stream(gamepasses).mapToObj(String::valueOf).toArray(String[]::new));
        statement.setString(1, gamepassesString);
        statement.setLong(2, this.userId);
        statement.executeUpdate();
        this.gamepasses = gamepasses;
    }
}
