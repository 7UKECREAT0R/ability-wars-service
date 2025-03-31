package org.lukecreator.aw.data;

import net.dv8tion.jda.api.entities.Message;
import org.lukecreator.aw.AWDatabase;
import org.lukecreator.aw.RobloxAPI;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Represents a piece of evidence in the form of a web resource with an ID, timestamp, and ID of the accused user.
 * <p>
 * Specifically, this is an interface for interacting with the internal database.
 */
public class AWEvidence {
    /**
     * The channel in Discord which should hold attachments. In this case, it's #mp4-storage inside Ability Wars.
     */
    public final static long VIDEO_FORWARD_CHANNEL = 1336072275673485433L;
    public final static String[] ALLOWED_EXTENSIONS = new String[]{"mp4", "mov", "png", "jpg", "jpeg"};
    public final long evidenceId;
    public final long timestamp;
    public Long accusedUserRobloxId;
    public String details;
    public String url;

    public AWEvidence(long evidenceId, long timestamp, Long accusedUserRobloxId, String details, String url) {
        this.evidenceId = evidenceId;
        this.timestamp = timestamp;
        this.accusedUserRobloxId = (accusedUserRobloxId == null || accusedUserRobloxId == 0L) ? null : accusedUserRobloxId;
        this.details = details == null ? "" : details;
        this.url = url;
    }

    /**
     * Checks if the provided filename has a valid extension based on a predefined list of allowed extensions.
     *
     * @param filename The name of the file to validate, including its extension.
     * @return {@code true} if the file has a valid extension, {@code false} otherwise.
     */
    public static boolean hasValidExtension(String filename) {
        int lastDot = filename.lastIndexOf('.');
        if (lastDot == -1)
            return false;
        String extension = filename.substring(lastDot + 1).toLowerCase();
        for (String allowedExtension : ALLOWED_EXTENSIONS) {
            if (allowedExtension.equals(extension))
                return true;
        }
        return false;
    }

    /**
     * Validates if the provided attachment has a valid file extension.
     * The validity is determined based on a predefined list of allowed file extensions.
     *
     * @param attachment The attachment to validate. It should contain the file's metadata, including its extension.
     * @return {@code true} if the attachment has a valid file extension, {@code false} otherwise.
     */
    public static boolean isValidAttachment(Message.Attachment attachment) {
        String extension = attachment.getFileExtension();
        if (extension == null)
            return false;
        extension = extension.toLowerCase();
        for (String allowedExtension : ALLOWED_EXTENSIONS) {
            if (allowedExtension.equals(extension))
                return true;
        }
        return false;
    }

    /**
     * Loads an evidence entry from the database using its unique identifier.
     *
     * @param id The unique identifier of the evidence entry to load.
     * @return An instance of {@code AWEvidence} representing the loaded evidence entry, or {@code null}
     * if no entry with the specified ID exists in the database.
     * @throws SQLException If an SQL error occurs while accessing the database.
     */
    public static AWEvidence loadFromDatabase(long id) throws SQLException {
        var statement = AWDatabase.connection.prepareStatement
                ("SELECT timestamp, accused_user, details, url FROM evidence WHERE evidence_id = ?");
        statement.setLong(1, id);
        try (var resultSet = statement.executeQuery()) {
            if (resultSet.next()) {
                long timestamp = resultSet.getLong("timestamp");
                Long accusedUserRobloxId = resultSet.getLong("accused_user");
                String details = resultSet.getString("details");
                String url = resultSet.getString("url");
                return new AWEvidence(id, timestamp, accusedUserRobloxId, details, url);
            }
        }
        return null;
    }

    /**
     * Removes a specific evidence entry from the database identified by its unique ID.
     *
     * @param id The unique identifier of the evidence entry to be removed.
     * @throws SQLException If an error occurs while executing the SQL statement.
     */
    public static void removeFromDatabase(long id) throws SQLException {
        // remove from the overall evidence database
        var statement = AWDatabase.connection.prepareStatement("DELETE FROM evidence WHERE evidence_id = ?");
        statement.setLong(1, id);
        statement.execute();

        // remove any ticket links to this evidence
        Links.TicketEvidenceLinks.deleteEvidence(id);
    }

    /**
     * Loads all evidence entries associated with a given user from the database.
     *
     * @param user The Roblox user for whom evidence records should be retrieved.
     *             If the user is null, an empty array is returned.
     * @return An array of {@code AWEvidence} instances containing all evidence associated
     * with the specified user. If no evidence is found, an empty array is returned.
     * @throws SQLException If an error occurs while querying the database.
     */
    public static AWEvidence[] loadEvidenceAgainstUser(RobloxAPI.User user) throws SQLException {
        if (user == null)
            return new AWEvidence[0];
        return loadEvidenceAgainstUserId(user.userId());
    }

    /**
     * Loads all evidence entries from the database associated with a given user ID.
     *
     * @param userId The Roblox user ID of the accused user for whom evidence records should be retrieved.
     * @return An array of {@code AWEvidence} instances containing all evidence entries associated with the given user ID.
     * Returns an empty array if no evidence records are found for the specified user.
     * @throws SQLException If an error occurs while querying the database.
     */
    public static AWEvidence[] loadEvidenceAgainstUserId(long userId) throws SQLException {
        var statement = AWDatabase.connection.prepareStatement("""
                SELECT evidence_id, timestamp, accused_user, details, url
                FROM evidence
                WHERE accused_user NOT NULL AND accused_user = ?""");
        statement.setLong(1, userId);
        try (var resultSet = statement.executeQuery()) {
            List<AWEvidence> evidenceList = new ArrayList<>();
            while (resultSet.next()) {
                long evidenceId = resultSet.getLong("evidence_id");
                long timestamp = resultSet.getLong("timestamp");
                Long accusedUserRobloxId = resultSet.getLong("accused_user");
                String details = resultSet.getString("details");
                String url = resultSet.getString("url");
                evidenceList.add(new AWEvidence(evidenceId, timestamp, accusedUserRobloxId, details, url));
            }

            return evidenceList.toArray(new AWEvidence[0]);
        }
    }

    /**
     * Tries to extract a millisecond timestamp from the given input.
     *
     * @param input The input string to search.
     * @return The extracted timestamp, or {@code 0L} if none could be automagically found.
     */
    public static long tryExtractTimestamp(String input) {
        if (input == null || input.isBlank()) {
            return 0L;
        }

        // convert to lowercase and remove any whitespace
        input = input.toLowerCase().trim();

        try {
            // handle MM:SS format (e.g., "6:29", "0:25")
            if (input.contains(":")) {
                String[] parts = input.split(":");
                if (parts.length == 2) {
                    int minutes = Integer.parseInt(parts[0]);
                    int seconds = Integer.parseInt(parts[1]);
                    return minutes * 60L + seconds * 1000L;
                }
            }

            // handle formats with "minutes"/"mins"/"m" and "seconds"/"secs"/"s"
            int totalSeconds = 0;

            // extract minutes
            Pattern minutePattern = Pattern.compile("(\\d+)\\s*(?:minutes?|mins?|m)");
            Matcher minuteMatcher = minutePattern.matcher(input);
            if (minuteMatcher.find()) {
                totalSeconds += Integer.parseInt(minuteMatcher.group(1)) * 60;
            }

            // extract seconds
            Pattern secondPattern = Pattern.compile("(\\d+)\\s*(?:seconds?|secs?|s)");
            Matcher secondMatcher = secondPattern.matcher(input);
            if (secondMatcher.find()) {
                totalSeconds += Integer.parseInt(secondMatcher.group(1));
            }

            // if we found any time components, return the total
            long ms = totalSeconds * 1000L;
            return Math.max(ms, 0);
        } catch (NumberFormatException e) {
            return 0L;
        }

    }

    public boolean hasTimestamp() {
        return this.timestamp > 0L;
    }

    /**
     * Converts the timestamp of this evidence into a formatted string representation.
     * The format is "m:ss", where "m" represents the minutes and "ss" represents the seconds.
     * If the timestamp is less than 1 millisecond, it defaults to "0:00".
     *
     * @return A string representing the timestamp in "m:ss" format, or "0:00" if the timestamp is invalid.
     */
    public String timestampString() {
        if (this.timestamp < 1L) {
            return "0:00";
        }
        long seconds = this.timestamp / 1000L;
        long minutes = seconds / 60L;
        seconds = seconds % 60L;
        return String.format("%d:%02d", minutes, seconds);
    }

    /**
     * Inserts the current evidence instance into the database. If a record with the same evidence ID
     * already exists, the operation does nothing. If the evidence overrides another piece of evidence,
     * the contents will be updated.
     *
     * @throws SQLException If an error occurs while preparing or executing the SQL statement.
     */
    public void pushToDatabase() throws SQLException {
        var statement = AWDatabase.connection.prepareStatement("""
                INSERT INTO evidence (evidence_id, timestamp, accused_user, details, url)
                VALUES (?, ?, ?, ?, ?)
                ON CONFLICT (evidence_id) DO UPDATE SET
                                                        timestamp = excluded.timestamp,
                                                        accused_user = excluded.accused_user,
                                                        details = excluded.details,
                                                        url = excluded.url""");
        statement.setLong(1, this.evidenceId);
        statement.setLong(2, this.timestamp);

        if (this.accusedUserRobloxId == null)
            statement.setNull(3, java.sql.Types.INTEGER);
        else
            statement.setLong(3, this.accusedUserRobloxId);

        statement.setString(4, this.details);
        statement.setString(5, this.url);
        statement.execute();
    }
}
