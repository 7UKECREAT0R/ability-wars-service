package org.lukecreator.aw.data;

import org.lukecreator.aw.AWDatabase;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Methods for accessing links between tickets/bans and evidence.
 */
@SuppressWarnings("unused") // I'll use these eventually
public class Links {
    /**
     * Methods for accessing links between tickets and evidence.
     */
    public static class TicketEvidenceLinks {
        /**
         * Retrieves an array of evidence entries linked to a specific ticket.
         * <p>
         * This method utilizes the {@code ticket_evidence_link} table to fetch the IDs of evidence
         * associated with the given ticket, then loads the corresponding evidence entries
         * from the database.
         *
         * @param ticketId The unique identifier of the ticket whose linked evidence is to be retrieved.
         * @return An array of {@code AWEvidence} objects representing the evidence linked to the given ticket.
         * If no evidence is linked to the specified ticket, an empty array is returned.
         * @throws SQLException If an error occurs while querying the database.
         */
        public static AWEvidence[] getEvidenceLinkedToTicket(long ticketId) throws SQLException {
            long[] evidenceIds = getEvidenceIDsLinkedToTicket(ticketId);
            List<AWEvidence> evidence = new ArrayList<>();

            for (long evidenceId : evidenceIds) {
                AWEvidence evidenceEntry = AWEvidence.loadFromDatabase(evidenceId);
                if (evidenceEntry != null)
                    evidence.add(evidenceEntry);
            }

            return evidence.toArray(new AWEvidence[0]);
        }

        /**
         * Retrieves an array of evidence entries linked to a specific ticket.
         * <p>
         * This method utilizes the {@code ticket_evidence_link} table to fetch the IDs of evidence
         * associated with the given ticket, then loads the corresponding evidence entries
         * from the database.
         *
         * @param ticketId The unique identifier of the ticket whose linked evidence is to be retrieved.
         * @return An array of {@code AWEvidence} objects representing the evidence linked to the given ticket.
         * If no evidence is linked to the specified ticket, an empty array is returned.
         * @throws SQLException If an error occurs while querying the database.
         */
        public static long[] getEvidenceIDsLinkedToTicket(long ticketId) throws SQLException {
            var statement = AWDatabase.connection.prepareStatement("""
                    SELECT evidence_id FROM ticket_evidence_link
                    WHERE ticket_id = ?""");
            statement.setLong(1, ticketId);

            try (ResultSet results = statement.executeQuery()) {
                List<Long> idList = new ArrayList<>();
                while (results.next()) {
                    long evidenceId = results.getLong("evidence_id");
                    idList.add(evidenceId);
                }

                return idList.stream().mapToLong(Long::longValue).toArray();
            }
        }

        /**
         * Links a specific piece of evidence to a ticket in the database.
         *
         * @param ticketId   The unique identifier of the ticket to which the evidence is to be linked.
         * @param evidenceId The unique identifier of the evidence to be linked to the ticket.
         * @throws SQLException If an error occurs while executing the database query.
         */
        public static void linkEvidenceToTicket(long ticketId, long evidenceId) throws SQLException {
            var statement = AWDatabase.connection.prepareStatement("INSERT INTO ticket_evidence_link VALUES (?, ?)");
            statement.setLong(1, ticketId);
            statement.setLong(2, evidenceId);
            statement.execute();
        }

        /**
         * Removes the link between a specific ticket and a piece of evidence in the database.
         *
         * @param ticketId   The unique identifier of the ticket from which the evidence should be unlinked.
         * @param evidenceId The unique identifier of the evidence to be unlinked from the ticket.
         * @throws SQLException If an error occurs while executing the database query.
         */
        public static void unlinkEvidenceFromTicket(long ticketId, long evidenceId) throws SQLException {
            var statement = AWDatabase.connection.prepareStatement("DELETE FROM ticket_evidence_link WHERE ticket_id = ? AND evidence_id = ?");
            statement.setLong(1, ticketId);
            statement.setLong(2, evidenceId);
            statement.execute();
        }

        /**
         * Removes all links in the database that link to the given evidence ID.
         *
         * @param evidenceId The unique identifier of the evidence to be deleted.
         * @throws SQLException If an error occurs while executing the database query.
         */
        public static void deleteEvidence(long evidenceId) throws SQLException {
            var statement = AWDatabase.connection.prepareStatement("DELETE FROM ticket_evidence_link WHERE evidence_id = ?");
            statement.setLong(1, evidenceId);
            statement.execute();
        }

        /**
         * Retrieves an array of tickets linked to a specific piece of evidence.
         * <p>
         * This method queries the {@code ticket_evidence_link} table to retrieve the ticket IDs
         * associated with the given evidence ID, then loads the corresponding ticket entries
         * from the database.
         *
         * @param evidenceId The unique identifier of the evidence whose linked tickets are to be retrieved.
         * @return An array of {@code AWTicket} objects representing the tickets linked to the specified evidence.
         * If no tickets are linked to the given evidence, an empty array is returned.
         * @throws SQLException If an error occurs while querying the database.
         */
        public static AWTicket[] getTicketsLinkedToEvidence(long evidenceId) throws SQLException {
            var statement = AWDatabase.connection.prepareStatement("""
                    SELECT ticket_id FROM ticket_evidence_link
                    WHERE evidence_id = ?""");
            statement.setLong(1, evidenceId);
            try (ResultSet results = statement.executeQuery()) {
                List<AWTicket> ticketList = new ArrayList<>();
                while (results.next()) {
                    long ticketId = results.getLong("ticket_id");
                    ticketList.add(AWTicket.loadFromDatabase(ticketId));
                }

                return ticketList.toArray(new AWTicket[0]);
            }
        }
    }

    /**
     * Methods for accessing links between bans and evidence.
     */
    public static class BanEvidenceLinks {
        /**
         * Retrieves all evidence entries linked to a specific Roblox user ID that resulted in a ban.
         *
         * @param robloxUserId The unique Roblox user ID for which the linked evidence is to be retrieved.
         * @return An array of {@code AWEvidence} objects containing all evidence entries linked to the specified
         * user ID. If no linked evidence exists, an empty array is returned.
         * @throws SQLException If an SQL error occurs while querying the database.
         */
        public static AWEvidence[] getEvidenceLinkedToUser(long robloxUserId) throws SQLException {
            var statement = AWDatabase.connection.prepareStatement("""
                    SELECT evidence_id FROM ban_evidence_link
                    WHERE user_id = ?""");
            statement.setLong(1, robloxUserId);

            try (ResultSet results = statement.executeQuery()) {
                List<AWEvidence> evidenceList = new ArrayList<>();
                while (results.next()) {
                    long evidenceId = results.getLong("evidence_id");
                    evidenceList.add(AWEvidence.loadFromDatabase(evidenceId));
                }

                return evidenceList.toArray(new AWEvidence[0]);
            }
        }

        /**
         * Retrieves all evidence entries linked to a specific Roblox user ID and ban start time.
         *
         * @param robloxUserId The unique Roblox user ID for which the linked evidence is to be retrieved.
         * @param banStartTime The timestamp representing the start time of the ban for which the linked evidence is to be retrieved.
         * @return An array of {@code AWEvidence} objects containing all evidence entries linked to the specified
         * Roblox user ID and ban start time. If no linked evidence exists, an empty array is returned.
         * @throws SQLException If an SQL error occurs while querying the database.
         */
        public static AWEvidence[] getEvidenceLinkedToBan(long robloxUserId, long banStartTime) throws SQLException {
            var statement = AWDatabase.connection.prepareStatement("""
                    SELECT evidence_id FROM ban_evidence_link
                    WHERE user_id = ? AND starts_timestamp = ?""");
            statement.setLong(1, robloxUserId);
            statement.setLong(2, banStartTime);

            try (ResultSet results = statement.executeQuery()) {
                List<AWEvidence> evidenceList = new ArrayList<>();
                while (results.next()) {
                    long evidenceId = results.getLong("evidence_id");
                    evidenceList.add(AWEvidence.loadFromDatabase(evidenceId));
                }

                return evidenceList.toArray(new AWEvidence[0]);
            }
        }

        /**
         * Links a specific evidence entry to a ban in the database.
         *
         * @param ban        An instance of {@code AWBan} representing the ban to which the evidence is being linked.
         *                   Contains information such as the user ID and the start timestamp of the ban.
         * @param evidenceId The unique identifier of the evidence to be linked to the ban.
         * @throws SQLException If there is an error while executing the SQL query to link the evidence.
         */
        public static void linkEvidenceToBan(AWBan ban, long evidenceId) throws SQLException {
            var statement = AWDatabase.connection.prepareStatement("INSERT INTO ban_evidence_link VALUES (?, ?, ?)");
            statement.setLong(1, ban.userId());
            statement.setLong(2, ban.starts());
            statement.setLong(3, evidenceId);
            statement.execute();
        }

        /**
         * Unlinks a specific evidence entry from a ban in the database.
         *
         * @param ban        An instance of {@code AWBan} representing the ban from which the evidence is to be unlinked.
         *                   Contains information such as the user ID and the start timestamp of the ban.
         * @param evidenceId The unique identifier of the evidence to be unlinked from the specified ban.
         * @throws SQLException If an error occurs while executing the SQL query to unlink the evidence.
         */
        public static void unlinkEvidenceFromBan(AWBan ban, long evidenceId) throws SQLException {
            var statement = AWDatabase.connection.prepareStatement("DELETE FROM ban_evidence_link WHERE user_id = ? AND starts_timestamp = ? AND evidence_id = ?");
            statement.setLong(1, ban.userId());
            statement.setLong(2, ban.starts());
            statement.setLong(3, evidenceId);
            statement.execute();
        }

        /**
         * Retrieves all bans associated with a specific piece of evidence.
         *
         * @param evidenceId The unique identifier of the evidence for which linked bans are to be retrieved.
         * @return An array of {@code AWBan} objects representing all bans linked to the specified evidence.
         * If no linked bans exist, an empty array is returned.
         * @throws SQLException If an SQL error occurs while querying the database.
         */
        public static AWBan[] getBansLinkedToEvidence(long evidenceId) throws SQLException {
            var statement = AWDatabase.connection.prepareStatement("""
                    SELECT starts_timestamp, user_id FROM ban_evidence_link
                    WHERE evidence_id = ?""");
            statement.setLong(1, evidenceId);

            try (ResultSet results = statement.executeQuery()) {
                List<AWBan> banList = new ArrayList<>();
                while (results.next()) {
                    long starts = results.getLong("starts_timestamp");
                    long userId = results.getLong("user_id");
                    AWPlayer player = AWPlayer.loadFromDatabase(userId, false, true, false, false);
                    for (AWBan ban : player.bans.iterateBans()) {
                        if (ban.starts() == starts)
                            banList.add(ban);
                    }
                }
                return banList.toArray(new AWBan[0]);
            }
        }
    }
}