package org.lukecreator.aw.data;

import net.dv8tion.jda.api.entities.UserSnowflake;
import org.jetbrains.annotations.Nullable;
import org.lukecreator.aw.AWDatabase;

import java.sql.SQLException;
import java.util.Objects;

/**
 * An Ability Wars player.
 * <p>
 * Specifically, this is an interface for interacting with the internal database.
 * No method here directly affects anything in the game, only the database.
 */
public final class AWPlayer {
    public static final String DEFAULT_USERNAME = "unknown";
    /**
     * The Roblox ID of this player.
     */
    public final long userId;

    /**
     * The stats of this player. Maybe null.
     */
    public final AWStats stats;

    /**
     * The ban records for this player. Maybe null.
     */
    public final AWBans bans;

    /**
     * The unban records for this player. Maybe null.
     */
    public final AWUnbans unbans;

    /**
     * The punch update record for this player. Maybe null.
     */
    public final AWPunchUpdates punchUpdates;

    /**
     * Indicates whether the player is appeal blacklisted (true) or not (false).
     */
    private boolean isAppealBlacklisted;
    /**
     * Can be null; The reason for the blacklist. (only valid if {@link #isAppealBlacklisted}.)
     */
    @Nullable
    private String appealBlacklistReason;
    /**
     * The unix millisecond time that the blacklist was issued. (only valid if {@link #isAppealBlacklisted}.)
     */
    private long appealBlacklistDate;
    /**
     * The Discord ID of the moderator who issued the blacklist. (only valid if {@link #isAppealBlacklisted}.)
     */
    private long appealBlacklistIssuer;

    /**
     * The Roblox username of this player.
     */
    private String username;

    public AWPlayer(long userId, String username,
                    boolean isAppealBlacklisted,
                    @Nullable String appealBlacklistReason,
                    long appealBlacklistDate,
                    long appealBlacklistIssuer,
                    AWStats stats,
                    AWBans bans,
                    AWUnbans unbans,
                    AWPunchUpdates punchUpdates) {
        this.userId = userId;
        this.username = username;
        this.isAppealBlacklisted = isAppealBlacklisted;
        this.appealBlacklistReason = appealBlacklistReason;
        this.appealBlacklistDate = appealBlacklistDate;
        this.appealBlacklistIssuer = appealBlacklistIssuer;
        this.stats = stats;
        this.bans = bans;
        this.unbans = unbans;
        this.punchUpdates = punchUpdates;
    }

    public static AWPlayer loadFromDatabase(long userId,
                                            boolean loadStats,
                                            boolean loadBans,
                                            boolean loadUnbans,
                                            boolean loadPunchUpdates) {
        try (var statement = AWDatabase.connection.prepareStatement("""
                SELECT username, is_appeal_blacklisted, appeal_blacklist_reason, appeal_blacklist_date, appeal_blacklist_issuer
                FROM players
                WHERE user_id = ?""")) {
            statement.setLong(1, userId);
            try (var results = statement.executeQuery()) {
                if (!results.next()) {
                    return AWPlayer.createWithOptionalExtras(userId, null, false,
                            null, 0L, 0L,
                            loadStats, loadBans, loadUnbans, loadPunchUpdates);
                }
                return AWPlayer.createWithOptionalExtras(
                        userId,
                        results.getString("username"),
                        results.getBoolean("is_appeal_blacklisted"),
                        results.getString("appeal_blacklist_reason"),
                        results.getLong("appeal_blacklist_date"),
                        results.getLong("appeal_blacklist_issuer"),
                        loadStats,
                        loadBans,
                        loadUnbans,
                        loadPunchUpdates
                );
            }
        } catch (SQLException e) {
            System.err.println("Failed to load stats for user " + userId + ":\n\n" + e);
            return new AWPlayer(userId, null, false,
                    null, 0L, 0L,
                    null, null, null, null);
        }
    }

    public static AWPlayer empty(long userId) {
        return new AWPlayer(userId, DEFAULT_USERNAME,
                false,
                null,
                0L,
                0L,
                AWStats.empty(userId),
                AWBans.empty(userId),
                AWUnbans.empty(userId),
                AWPunchUpdates.empty(userId));
    }

    private static AWPlayer createWithOptionalExtras(long userId, String username,
                                                     boolean isAppealBlacklisted,
                                                     @Nullable String appealBlacklistReason,
                                                     long appealBlacklistDate,
                                                     long appealBlacklistIssuer,
                                                     boolean loadStats,
                                                     boolean loadBans,
                                                     boolean loadUnbans,
                                                     boolean loadPunchUpdates) throws SQLException {
        AWStats stats = loadStats ? AWStats.loadFromDatabase(userId) : AWStats.empty(userId);
        AWBans bans = loadBans ? AWBans.loadFromDatabase(userId) : AWBans.empty(userId);
        AWUnbans unbans = loadUnbans ? AWUnbans.loadFromDatabase(userId) : AWUnbans.empty(userId);
        AWPunchUpdates punchUpdates = loadPunchUpdates ? AWPunchUpdates.loadFromDatabase(userId) : AWPunchUpdates.empty(userId);

        AWPlayer created = new AWPlayer(userId, username,
                isAppealBlacklisted, appealBlacklistReason, appealBlacklistDate, appealBlacklistIssuer,
                stats, bans, unbans, punchUpdates);

        created.ensureDefaultPlayer();
        return created;
    }

    public void ensureDefaultPlayer() throws SQLException {
        var statement = AWDatabase.connection.prepareStatement("""
                INSERT INTO players (user_id, username, is_appeal_blacklisted, appeal_blacklist_date, appeal_blacklist_reason, appeal_blacklist_issuer)
                VALUES (?, ?, FALSE, 0, NULL, 0)
                ON CONFLICT (user_id) DO NOTHING""");
        statement.setLong(1, this.userId);
        statement.setString(2, this.username);
        statement.executeUpdate();
    }


    /**
     * Updates the username of the player both in the database and in memory.
     *
     * @param newUsername The new username to assign to the player. This must be a non-null and valid string.
     * @throws SQLException If an error occurs while updating the username in the database.
     */
    public void setUsername(String newUsername) throws SQLException {
        this.ensureDefaultPlayer();
        var statement = AWDatabase.connection.prepareStatement("""
                UPDATE players
                SET username = ?
                WHERE user_id = ?"""
        );
        statement.setString(1, newUsername);
        statement.setLong(2, this.userId);
        statement.executeUpdate();
        this.username = newUsername;
    }

    public String username() {
        return this.username;
    }

    public boolean isAppealBlacklisted() {
        return this.isAppealBlacklisted;
    }

    @Nullable
    public String getAppealBlacklistReason() {
        return this.appealBlacklistReason;
    }

    public long getAppealBlacklistDate() {
        return this.appealBlacklistDate;
    }

    public long getAppealBlacklistIssuer() {
        return this.appealBlacklistIssuer;
    }

    /**
     * Removes the appeal blacklist status for this player in the database, updating the relevant fields to indicate
     * that the user is no longer appeal blacklisted. This includes setting the `is_appeal_blacklisted` field to `FALSE`,
     * clearing the `appeal_blacklist_date` and `appeal_blacklist_reason` fields, and resetting the `appeal_blacklist_issuer`.
     *
     * @throws SQLException If an error occurs while updating the database.
     */
    public void removeBlacklist() throws SQLException {
        var statement = AWDatabase.connection.prepareStatement("""
                UPDATE players
                SET (is_appeal_blacklisted, appeal_blacklist_date, appeal_blacklist_reason, appeal_blacklist_issuer) = (FALSE, 0, NULL, 0)
                WHERE user_id = ?"""
        );
        statement.setLong(1, this.userId);
        statement.executeUpdate();
    }

    /**
     * Sets the appeal blacklist status for this player in the database. The method updates the relevant database fields
     * to indicate that the user is now appeal blacklisted. It includes setting `is_appeal_blacklisted` to `TRUE`,
     * recording the current timestamp as the blacklist date, saving the reason for the blacklist, and identifying the
     * issuer responsible for the blacklist.
     *
     * @param reason The reason for blacklisting the player. Can be null.
     * @param issuer The user responsible for issuing the blacklist. Must not be null.
     * @throws SQLException If an error occurs while updating the database.
     */
    public void setBlacklist(@Nullable String reason, UserSnowflake issuer) throws SQLException {
        this.isAppealBlacklisted = true;
        this.appealBlacklistReason = reason;
        this.appealBlacklistDate = System.currentTimeMillis();
        this.appealBlacklistIssuer = issuer.getIdLong();

        this.ensureDefaultPlayer();
        var statement = AWDatabase.connection.prepareStatement("""
                UPDATE players
                SET (is_appeal_blacklisted, appeal_blacklist_date, appeal_blacklist_reason, appeal_blacklist_issuer) = (TRUE, ?, ?, ?)
                WHERE user_id = ?""");
        statement.setLong(1, this.appealBlacklistDate);
        statement.setString(2, reason);
        statement.setLong(3, issuer.getIdLong());
        statement.setLong(4, this.userId);
        statement.executeUpdate();
    }

    /**
     * Updates the internal database to indicate that this user is no longer banned, starting {@link System#currentTimeMillis()}.
     *
     * @param responsibleModerator The Roblox ID of the moderator responsible, if any.
     * @throws SQLException If something went wrong while updating the database.
     */
    public void unban(Long responsibleModerator) throws SQLException {
        this.ensureDefaultPlayer();

        long time = System.currentTimeMillis();

        // get the last ban on record and set its "ends" field.
        AWBans bans = (this.bans == null) ? AWBans.loadFromDatabase(this.userId) : this.bans;
        bans.setBanEnds(time);

        // add an unban record
        AWUnbans unbans = (this.unbans == null) ? AWUnbans.loadFromDatabase(this.userId) : this.unbans;
        unbans.addUnban(new AWUnban(this.userId, responsibleModerator, time));
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (AWPlayer) obj;
        return this.userId == that.userId &&
                Objects.equals(this.username, that.username) &&
                this.isAppealBlacklisted == that.isAppealBlacklisted &&
                Objects.equals(this.stats, that.stats) &&
                Objects.equals(this.bans, that.bans) &&
                Objects.equals(this.unbans, that.unbans) &&
                Objects.equals(this.punchUpdates, that.punchUpdates);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.userId, this.username, this.isAppealBlacklisted, this.stats, this.bans, this.unbans, this.punchUpdates);
    }

    @Override
    public String toString() {
        return "AWPlayer[" +
                "userId=" + this.userId + ", " +
                "username=" + this.username + ", " +
                "isAppealBlacklisted=" + this.isAppealBlacklisted + ", " +
                "stats=" + this.stats + ", " +
                "bans=" + this.bans + ", " +
                "unbans=" + this.unbans + ", " +
                "punchUpdates=" + this.punchUpdates + ']';
    }
}
