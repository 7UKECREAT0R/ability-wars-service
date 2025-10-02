package org.lukecreator.aw.discord;

import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;
import net.dv8tion.jda.api.interactions.modals.ModalMapping;
import org.lukecreator.aw.RobloxAPI;
import org.lukecreator.aw.data.AWTicket;
import org.lukecreator.aw.data.DiscordRobloxLinks;
import org.lukecreator.aw.data.tickets.AWPlayerReportTicket;
import org.lukecreator.aw.data.tickets.AWUnbanTicket;
import org.lukecreator.aw.webserver.PendingRequest;
import org.lukecreator.aw.webserver.PendingRequests;
import org.lukecreator.aw.webserver.requests.BanRequest;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;

/**
 * Contains methods for generating modals associated with tickets that perform pre-set actions when submitted.
 */
public class ActionModals {
    private static final String ACTION_ID_PREFIX = "TICKET-ACTION_";

    /**
     * Action to close the ticket with a custom reason provided by the user
     */
    private static final String ACTION_CLOSE_WITH_REASON = "CWR";
    /**
     * Action to close the ticket with a templated reason that uses user inputs
     */
    private static final String ACTION_CLOSE_WITH_TEMPLATED_REASON = "CWTR";
    /**
     * Action to close the ticket and ban a user with a preset reason
     */
    private static final String ACTION_CLOSE_AND_BAN = "CB";
    /**
     * Action to close the ticket and ban a user with a custom reason provided by the user
     */
    private static final String ACTION_CLOSE_AND_BAN_WITH_REASON = "CBR";
    /**
     * Action to close the ticket and temporarily ban a user with a preset reason
     */
    private static final String ACTION_CLOSE_AND_TEMPBAN = "CTB";
    /**
     * Action to close the ticket and temporarily ban a user with a custom reason provided by the user
     */
    private static final String ACTION_CLOSE_AND_TEMPBAN_WITH_REASON = "CTBR";
    /**
     * Action to close the ticket and change the user's ban length.
     */
    private static final String ACTION_CLOSE_AND_RETIME_BAN = "CRT";
    /**
     * Action to close the ticket and unban a user with a custom reason provided by the user
     */
    private static final String ACTION_CLOSE_AND_UNBAN_WITH_REASON = "CUBR";
    /**
     * Action to close the ticket and blacklist a user from appeals with a custom reason
     */
    private static final String ACTION_CLOSE_AND_APPEAL_BLACKLIST_WITH_REASON = "CABR";


    private static final HashMap<String, Long> stringCache = new HashMap<>();

    private static long encodeString(String text) {
        if (!stringCache.containsKey(text))
            stringCache.put(text, System.currentTimeMillis());
        return stringCache.get(text);
    }

    private static String decodeString(long id) {
        for (var entry : stringCache.entrySet())
            if (entry.getValue() == id)
                return entry.getKey();
        return null;
    }

    public static String createId(String actionId, long ticketId, String... args) {
        if (args.length > 0)
            return ACTION_ID_PREFIX + actionId + "_" + ticketId + "_" + String.join("_", args);
        return ACTION_ID_PREFIX + actionId + "_" + ticketId;
    }

    public static void sendToImplementation(String actionId, String[] args, ModalInteractionEvent event, AWTicket ticket) throws SQLException {
        switch (actionId) {
            case ACTION_CLOSE_WITH_REASON:
                impl_closeTicketWithCustomReason(event, ticket);
                break;
            case ACTION_CLOSE_WITH_TEMPLATED_REASON:
                impl_closeTicketWithTemplatedReason(event, ticket, Long.parseLong(args[0]));
                break;
            case ACTION_CLOSE_AND_BAN:
                impl_closeTicketAndBanWithPresetReason(event, ticket, Long.parseLong(args[0]));
                break;
            case ACTION_CLOSE_AND_BAN_WITH_REASON:
                impl_closeTicketAndBanWithCustomReason(event, ticket);
                break;
            case ACTION_CLOSE_AND_TEMPBAN:
                impl_closeTicketAndTempbanWithPresetReason(event, ticket, Long.parseLong(args[0]));
                break;
            case ACTION_CLOSE_AND_TEMPBAN_WITH_REASON:
                impl_closeTicketAndTempbanWithCustomReason(event, ticket);
                break;
            case ACTION_CLOSE_AND_RETIME_BAN:
                impl_closeTicketAndRetimeBan(event, ticket);
            case ACTION_CLOSE_AND_UNBAN_WITH_REASON:
                impl_closeTicketAndUnbanWithCustomReason(event, ticket);
                break;
            case ACTION_CLOSE_AND_APPEAL_BLACKLIST_WITH_REASON:
                impl_closeTicketAndBlacklistWithCustomReason(event, ticket);
                break;
        }
    }

    /**
     * Create a modal which allows the opener to close the ticket with a custom reason.
     *
     * @param ticket The ticket to close when this modal is submitted.
     */
    public static Modal closeTicketWithCustomReason(AWTicket ticket) {
        return Modal
                .create(createId(ACTION_CLOSE_WITH_REASON, ticket.id), "Resolve Ticket with Reason")
                .addActionRow(TextInput.create("reason", "Reason", TextInputStyle.PARAGRAPH)
                        .setPlaceholder("e.g., please only open one ticket at a time!")
                        .setRequired(true)
                        .build())
                .build();
    }

    private static void impl_closeTicketWithCustomReason(ModalInteractionEvent event, AWTicket ticket) throws SQLException {
        ModalMapping reasonMapping = event.getValue("reason");
        if (reasonMapping == null) {
            event.reply("Strange error: missing field in the modal response?").queue();
            return;
        }

        String reason = reasonMapping.getAsString();
        if (reason.isBlank())
            reason = "No reason provided.";

        // cancel the interaction, ticket is about to close
        event.deferEdit().queue();

        // close the ticket
        User closedBy = event.getUser();
        ticket.close(event.getJDA(), closedBy, reason, null, null);

        if (ticket.type().isAppealsServer() && ticket instanceof AWUnbanTicket unbanTicket) {
            // create transcript
            String actionDescription = "Closed without action for reason: `%s`".formatted(reason);
            unbanTicket.sendTranscriptsMessage(event.getJDA(), closedBy, actionDescription);
        }
    }

    /**
     * Creates a modal that allows the opener to close a ticket with a pre-set templated reason.
     * <p>
     * The {@code templateInputs} will be passed into the created modal directly. The ID of each input will be used
     * as a replacement key (surrounded by {curly braces}) inside the template string once the modal is submitted.
     * <p>
     * For example, if an input has the ID {@code name}, you can place its submitted value into the template string like so:
     * <ul>
     *     <li>{@code "Hey, {name}! We couldn't do anything about this ticket!"}</li>
     * </ul>
     *
     * @param ticket         The ticket to close when this modal is submitted.
     * @param modalTitle     The title of the modal displayed to the user. Max length {@value Modal#MAX_TITLE_LENGTH}.
     * @param templateString The string template to be used for generating the reason.
     * @param templateInputs Input fields to populate the template string. At least one and at most five inputs are required.
     * @return A constructed modal with the specified parameters and input fields.
     */
    public static Modal closeTicketWithTemplatedReason(AWTicket ticket, String modalTitle, String templateString, TextInput... templateInputs) {
        assert templateInputs.length > 0 && templateInputs.length <= 5;

        long templateStringEncoded = encodeString(templateString);
        Modal.Builder modal = Modal.create(createId(ACTION_CLOSE_WITH_TEMPLATED_REASON, ticket.id, String.valueOf(templateStringEncoded)), modalTitle);
        for (TextInput templateInput : templateInputs)
            modal.addActionRow(templateInput);
        return modal.build();
    }

    public static void impl_closeTicketWithTemplatedReason(ModalInteractionEvent event, AWTicket ticket, long templateStringEncoded) throws SQLException {
        String templateString = decodeString(templateStringEncoded);
        if (templateString == null) {
            event.reply("Strange error: lost the template string?").queue();
            return;
        }

        List<ModalMapping> inputs = event.getValues();

        for (ModalMapping input : inputs) {
            String replacementKey = "{" + input.getId() + '}';
            String value = input.getAsString();
            templateString = templateString.replace(replacementKey, value);
        }

        // cancel the interaction, ticket is about to close
        event.deferEdit().queue();

        // close the ticket
        User closedBy = event.getUser();
        ticket.close(event.getJDA(), closedBy, templateString, null, null);

        if (ticket.type().isAppealsServer() && ticket instanceof AWUnbanTicket unbanTicket) {
            // create transcript
            String actionDescription = "Closed without action for reason: `%s`".formatted(templateString);
            unbanTicket.sendTranscriptsMessage(event.getJDA(), closedBy, actionDescription);
        }
    }

    public static Modal closeTicketAndBanWithPresetReason(AWPlayerReportTicket ticket, String presetReason) {
        long reasonEncoded = encodeString(presetReason);
        return Modal
                .create(createId(ACTION_CLOSE_AND_BAN, ticket.id, String.valueOf(reasonEncoded)), "Close Ticket and Ban")
                .addActionRow(TextInput.create("exploit", "Exploit Name", TextInputStyle.SHORT)
                        .setPlaceholder("killaura, fly, speed, teleport, etc...")
                        .setRequired(true)
                        .build())
                .build();
    }

    private static void impl_closeTicketAndBanWithPresetReason(ModalInteractionEvent event, AWTicket _ticket, long presetReasonEncoded) throws SQLException {
        final AWPlayerReportTicket ticket = (AWPlayerReportTicket) _ticket;
        ModalMapping exploitMapping = event.getValue("exploit");
        if (exploitMapping == null) {
            event.reply("Strange error: missing field in the modal response?").queue();
            return;
        }
        final String exploit = exploitMapping.getAsString();
        String presetReason = decodeString(presetReasonEncoded);
        if (presetReason == null) {
            event.reply("Strange error: lost the pre-set reason?").queue();
            return;
        }
        User closedBy = event.getUser();
        Long _closedByRobloxId = DiscordRobloxLinks.robloxIdFromDiscordId(closedBy.getIdLong());

        if (_closedByRobloxId == null) {
            event.reply("Moderator: failed to get your linked Roblox ID.").queue();
            return;
        }

        final long toBanId = ticket.getAccusedUser().userId();
        final long closedByRobloxId = _closedByRobloxId;

        // cancel the interaction
        event.deferEdit().queue();

        // ban the user in-game after the next poll
        BanRequest banRequest = new BanRequest(
                PendingRequest.getNextRequestId(),
                toBanId,
                closedByRobloxId,
                exploit,
                true,
                0L,
                ticket.getEvidenceId(),
                ticket.id
        );

        PendingRequests.add(banRequest);

        // close the ticket
        ticket.close(event.getJDA(), closedBy, presetReason, null, ticket.returnEmbedIfExternalEvidence());
        ticket.closeRelated(event.getJDA(), closedBy, presetReason, null);

        // file a physical report in the #in-game-punishments
        if (ticket.hasEvidence()) {
            String physicalReport = AWPlayerReportTicket.buildInGamePunishmentsRecord(event.getUser(), ticket.getAccusedUser(), ticket.getRuleBroken(), exploit, ticket.getEvidenceURL());
            AWPlayerReportTicket.sendInGamePunishmentsMessage(event.getJDA(), physicalReport).queue();
        }
    }

    public static Modal closeTicketAndBanWithCustomReason(AWPlayerReportTicket ticket) {
        return Modal
                .create(createId(ACTION_CLOSE_AND_BAN_WITH_REASON, ticket.id), "Close Ticket and Ban")
                .addActionRow(TextInput.create("exploit", "Ban Reason", TextInputStyle.SHORT)
                        .setPlaceholder("killaura, fly, speed, teleport, etc...")
                        .setRequired(true)
                        .build())
                .addActionRow(TextInput.create("reason", "Ticket Close Reason", TextInputStyle.SHORT)
                        .setPlaceholder("e.g.: Banned, but please try to record in a higher resolution next time!")
                        .setRequired(true)
                        .build())
                .build();
    }

    private static void impl_closeTicketAndBanWithCustomReason(ModalInteractionEvent event, AWTicket _ticket) throws SQLException {
        final AWPlayerReportTicket ticket = (AWPlayerReportTicket) _ticket;
        ModalMapping exploitMapping = event.getValue("exploit");
        ModalMapping reasonMapping = event.getValue("reason");
        if (exploitMapping == null || reasonMapping == null) {
            event.reply("Strange error: missing field in the modal response?").queue();
            return;
        }

        final String exploit = exploitMapping.getAsString();
        final String reason = reasonMapping.getAsString();

        User closedBy = event.getUser();
        Long _closedByRobloxId = DiscordRobloxLinks.robloxIdFromDiscordId(closedBy.getIdLong());

        if (_closedByRobloxId == null) {
            event.reply("Moderator: failed to get your linked Roblox ID.").queue();
            return;
        }

        final long toBanId = ticket.getAccusedUser().userId();
        final long closedByRobloxId = _closedByRobloxId;

        // cancel the interaction
        event.deferEdit().queue();

        // ban the user in-game after the next poll
        BanRequest banRequest = new BanRequest(
                PendingRequest.getNextRequestId(),
                toBanId,
                closedByRobloxId,
                exploit,
                true,
                0L,
                ticket.getEvidenceId(),
                ticket.id
        );
        PendingRequests.add(banRequest);

        // close the ticket
        ticket.close(event.getJDA(), closedBy, reason, null, ticket.returnEmbedIfExternalEvidence());
        ticket.closeRelated(event.getJDA(), closedBy, reason, null);

        // file a physical report in the #in-game-punishments
        if (ticket.hasEvidence()) {
            StringBuilder physicalReport = new StringBuilder();
            RobloxAPI.User accused = ticket.getAccusedUser();
            physicalReport.append(accused.username());
            physicalReport.append(" - (").append(accused.userId()).append(") - ");
            physicalReport.append("[Roblox Profile](").append(accused.getProfileURL()).append(") - Banned by ").append(event.getUser().getAsMention()).append('\n');
            physicalReport.append(ticket.getRuleBroken()).append(" - ").append(exploit).append('\n');
            physicalReport.append(ticket.getEvidenceURL());
            AWPlayerReportTicket.sendInGamePunishmentsMessage(event.getJDA(), physicalReport.toString()).queue();
        }
    }

    public static Modal closeTicketAndTempbanWithPresetReason(AWPlayerReportTicket ticket, String presetReason) {
        long reasonEncoded = encodeString(presetReason);
        return Modal
                .create(createId(ACTION_CLOSE_AND_TEMPBAN, ticket.id, String.valueOf(reasonEncoded)), "Close Ticket and Temporarily Ban")
                .addActionRow(TextInput.create("exploit", "Exploit Name", TextInputStyle.SHORT)
                        .setPlaceholder("killaura, fly, speed, teleport, etc...")
                        .setRequired(true)
                        .build())
                .addActionRow(TextInput.create("duration", "Duration (days)", TextInputStyle.SHORT)
                        .setRequired(true)
                        .build())
                .build();
    }

    private static void impl_closeTicketAndTempbanWithPresetReason(ModalInteractionEvent event, AWTicket _ticket, long presetReasonEncoded) throws SQLException {
        final AWPlayerReportTicket ticket = (AWPlayerReportTicket) _ticket;
        ModalMapping exploitMapping = event.getValue("exploit");
        ModalMapping durationMapping = event.getValue("duration");
        if (exploitMapping == null || durationMapping == null) {
            event.reply("Strange error: missing field in the modal response?").queue();
            return;
        }
        final String exploit = exploitMapping.getAsString();
        final String duration = durationMapping.getAsString();

        try {
            long daysDuration = Long.parseLong(duration);
            String presetReason = decodeString(presetReasonEncoded);
            if (presetReason == null) {
                event.reply("Strange error: lost the pre-set reason?").queue();
                return;
            }
            User closedBy = event.getUser();
            Long _closedByRobloxId = DiscordRobloxLinks.robloxIdFromDiscordId(closedBy.getIdLong());

            if (_closedByRobloxId == null) {
                event.reply("Moderator: failed to get your linked Roblox ID.").queue();
                return;
            }

            final long toBanId = ticket.getAccusedUser().userId();
            final long closedByRobloxId = _closedByRobloxId;

            // cancel the interaction
            event.deferEdit().queue();

            // ban the user in-game after the next poll
            BanRequest banRequest = new BanRequest(
                    PendingRequest.getNextRequestId(),
                    toBanId,
                    closedByRobloxId,
                    exploit,
                    false,
                    daysDuration * 86400000L,
                    ticket.getEvidenceId(),
                    ticket.id
            );
            PendingRequests.add(banRequest);

            // close the ticket
            ticket.close(event.getJDA(), closedBy, presetReason, null, ticket.returnEmbedIfExternalEvidence());
            ticket.closeRelated(event.getJDA(), closedBy, presetReason, null);

            // file a physical report in the #in-game-punishments
            if (ticket.hasEvidence()) {
                StringBuilder physicalReport = new StringBuilder();
                RobloxAPI.User accused = ticket.getAccusedUser();
                physicalReport.append(accused.username());
                physicalReport.append(" - (").append(accused.userId()).append(") - ");
                physicalReport.append("[Roblox Profile](").append(accused.getProfileURL()).append(") - Temp-Banned by ").append(event.getUser().getAsMention()).append('\n');
                physicalReport.append(ticket.getRuleBroken()).append(" - ").append(exploit).append('\n');
                physicalReport.append("-# Temporary ban, duration: ").append(daysDuration).append(" days").append('\n');
                physicalReport.append(ticket.getEvidenceURL());
                AWPlayerReportTicket.sendInGamePunishmentsMessage(event.getJDA(), physicalReport.toString()).queue();
            }
        } catch (NumberFormatException ignored) {
            event.reply("Duration in days must be a valid number. (got \"%s\")".formatted(duration)).queue();
            return;
        }
    }

    public static Modal closeTicketAndTempbanWithCustomReason(AWPlayerReportTicket ticket) {
        return Modal
                .create(createId(ACTION_CLOSE_AND_TEMPBAN_WITH_REASON, ticket.id), "Close Ticket and Ban")
                .addActionRow(TextInput.create("exploit", "Ban Reason", TextInputStyle.SHORT)
                        .setPlaceholder("killaura, fly, speed, teleport, etc...")
                        .setRequired(true)
                        .build())
                .addActionRow(TextInput.create("reason", "Ticket Close Reason", TextInputStyle.SHORT)
                        .setPlaceholder("e.g.: Banned, but please try to record in a higher resolution next time!")
                        .setRequired(true)
                        .build())
                .addActionRow(TextInput.create("duration", "Duration (days)", TextInputStyle.SHORT)
                        .setRequired(true)
                        .build())
                .build();
    }

    private static void impl_closeTicketAndTempbanWithCustomReason(ModalInteractionEvent event, AWTicket _ticket) throws SQLException {
        final AWPlayerReportTicket ticket = (AWPlayerReportTicket) _ticket;
        ModalMapping exploitMapping = event.getValue("exploit");
        ModalMapping reasonMapping = event.getValue("reason");
        ModalMapping durationMapping = event.getValue("duration");
        if (exploitMapping == null || reasonMapping == null || durationMapping == null) {
            event.reply("Strange error: missing field in the modal response?").queue();
            return;
        }
        final String exploit = exploitMapping.getAsString();
        final String reason = reasonMapping.getAsString();
        final String duration = durationMapping.getAsString();

        try {
            long daysDuration = Long.parseLong(duration);
            User closedBy = event.getUser();
            Long _closedByRobloxId = DiscordRobloxLinks.robloxIdFromDiscordId(closedBy.getIdLong());

            if (_closedByRobloxId == null) {
                event.reply("Moderator: failed to get your linked Roblox ID.").queue();
                return;
            }

            final long toBanId = ticket.getAccusedUser().userId();
            final long closedByRobloxId = _closedByRobloxId;

            // cancel the interaction
            event.deferEdit().queue();

            // ban the user in-game after the next poll
            BanRequest banRequest = new BanRequest(
                    PendingRequest.getNextRequestId(),
                    toBanId,
                    closedByRobloxId,
                    exploit,
                    false,
                    daysDuration * 86400000L,
                    ticket.getEvidenceId(),
                    ticket.id
            );
            PendingRequests.add(banRequest);

            // close the ticket
            ticket.close(event.getJDA(), closedBy, reason, null, ticket.returnEmbedIfExternalEvidence());
            ticket.closeRelated(event.getJDA(), closedBy, reason, null);

            // file a physical report in the #in-game-punishments
            if (ticket.hasEvidence()) {
                StringBuilder physicalReport = new StringBuilder();
                RobloxAPI.User accused = ticket.getAccusedUser();
                physicalReport.append(accused.username());
                physicalReport.append(" - (").append(accused.userId()).append(") - ");
                physicalReport.append("[Roblox Profile](").append(accused.getProfileURL()).append(") - Temp-Banned by ").append(event.getUser().getAsMention()).append('\n');
                physicalReport.append(ticket.getRuleBroken()).append(" - ").append(exploit).append('\n');
                physicalReport.append("-# Temporary ban, duration: ").append(daysDuration).append(" days").append('\n');
                physicalReport.append(ticket.getEvidenceURL());
                AWPlayerReportTicket.sendInGamePunishmentsMessage(event.getJDA(), physicalReport.toString()).queue();
            }
        } catch (NumberFormatException ignored) {
            event.reply("Duration in days must be a valid number. (got \"%s\")".formatted(duration)).queue();
            return;
        }
    }

    public static Modal closeTicketAndRetimeBan(AWUnbanTicket ticket) {
        return Modal
                .create(createId(ACTION_CLOSE_AND_RETIME_BAN, ticket.id), "Close Ticket and Retime Ban")
                .addActionRow(TextInput.create("duration", "New Duration (days)", TextInputStyle.SHORT)
                        .setPlaceholder("Type -1 to make the ban permanent.")
                        .setRequired(true)
                        .build())
                .build();
    }

    private static void impl_closeTicketAndRetimeBan(ModalInteractionEvent event, AWTicket _ticket) throws SQLException {
        final AWUnbanTicket ticket = (AWUnbanTicket) _ticket;

        if (ticket.isForDiscord()) {
            event.reply("This action only works on tickets for Roblox bans.").setEphemeral(true).queue();
            return;
        }

        ModalMapping durationMapping = event.getValue("duration");
        if (durationMapping == null) {
            event.reply("Strange error: missing field in the modal response?").setEphemeral(true).queue();
            return;
        }
        final String duration = durationMapping.getAsString();

        try {
            long durationLong = Long.parseLong(duration);

            if (durationLong < -1) {
                event.reply("Duration must be a valid positive number, or -1 for a permanent ban. (got `%d`)".formatted(durationLong)).setEphemeral(true).queue();
                return;
            }

            long durationMs = durationLong * (24 * 60 * 60 * 1000);
            boolean isPermanent = durationLong == -1;
            long userIdToBan = ticket.getIdToUnban();
            User closedBy = event.getUser();
            Long moderatorId = DiscordRobloxLinks.robloxIdFromDiscordId(closedBy.getIdLong());

            if (moderatorId == null) {
                event.reply("Moderator: failed to get your linked Roblox ID.").setEphemeral(true).queue();
                return;
            }

            String reason = isPermanent ?
                    "Ban length corrected, thanks for letting us know :)" :
                    "Ban length corrected to %d days, thanks for letting us know :)".formatted(durationLong);

            // stop the interaction here
            event.deferEdit().queue();

            // send a new ban request to correct the duration
            PendingRequest request = new BanRequest(PendingRequest.getNextRequestId(), userIdToBan, moderatorId,
                    "Ban length correction", isPermanent, isPermanent ? 0L : durationMs, null, ticket.id);
            PendingRequests.add(request);

            // close the ticket with no further action
            ticket.close(event.getJDA(), closedBy, reason, null, null);

            // create transcript
            String actionDescription = "Ban duration changed to %d day(s)".formatted(durationLong);
            ticket.sendTranscriptsMessage(event.getJDA(), closedBy, actionDescription);
        } catch (NumberFormatException ignored) {
            event.reply("Duration must be a valid positive number, or -1 for a permanent ban. (got \"%s\")".formatted(duration)).setEphemeral(true).queue();
            return;
        }
    }

    public static Modal closeTicketAndUnbanWithCustomReason(AWUnbanTicket ticket) {
        return Modal
                .create(createId(ACTION_CLOSE_AND_UNBAN_WITH_REASON, ticket.id), "Close Ticket and Unban")
                .addActionRow(TextInput.create("reason", "Ticket Close Reason", TextInputStyle.PARAGRAPH)
                        .setPlaceholder("e.g.: Unbanned, but seriously, re-read the rules.")
                        .setRequired(true)
                        .build())
                .build();
    }

    public static void impl_closeTicketAndUnbanWithCustomReason(ModalInteractionEvent event, AWTicket _ticket) throws SQLException {
        final AWUnbanTicket ticket = (AWUnbanTicket) _ticket;
        ModalMapping reasonMapping = event.getValue("reason");
        if (reasonMapping == null) {
            event.reply("Strange error: missing field in the modal response?").queue();
            return;
        }
        final String reason = reasonMapping.getAsString();

        User closedBy = event.getUser();

        // cancel the interaction
        event.deferEdit().queue();

        // ban the user in-game after the next poll
        ticket.closeAndUnban(event.getJDA(), closedBy, reason, null);

        // file a physical report in the #transcripts channel
        String actionDescription = "Unbanned for `%s`".formatted(reason.replace("`", ""));
        ticket.sendTranscriptsMessage(event.getJDA(), closedBy, actionDescription);
    }

    public static Modal closeTicketAndBlacklistWithCustomReason(AWUnbanTicket ticket) {
        return Modal
                .create(createId(ACTION_CLOSE_AND_APPEAL_BLACKLIST_WITH_REASON, ticket.id), "Close Ticket and Blacklist")
                .addActionRow(TextInput.create("blacklist-reason", "Blacklist Reason", TextInputStyle.PARAGRAPH)
                        .setPlaceholder("This will not show to the user.")
                        .setRequired(true)
                        .build())
                .addActionRow(TextInput.create("close-reason", "Ticket Close Reason", TextInputStyle.PARAGRAPH)
                        .setPlaceholder("The reason to show to the user.")
                        .setRequired(true)
                        .build())
                .build();
    }

    public static void impl_closeTicketAndBlacklistWithCustomReason(ModalInteractionEvent event, AWTicket _ticket) throws SQLException {
        final AWUnbanTicket ticket = (AWUnbanTicket) _ticket;
        ModalMapping blacklistReasonMapping = event.getValue("blacklist-reason");
        ModalMapping closeReasonMapping = event.getValue("close-reason");
        if (blacklistReasonMapping == null || closeReasonMapping == null) {
            event.reply("Strange error: missing field in the modal response?").queue();
            return;
        }
        final String blacklistReason = blacklistReasonMapping.getAsString();
        String _closeReason = closeReasonMapping.getAsString();

        if (blacklistReason.isBlank()) {
            event.reply("Blacklist reason cannot be blank.").queue();
            return;
        }

        String closeReason = _closeReason.isBlank() ? "Blacklisted" : _closeReason;
        User closedBy = event.getUser();
        Long _closedByRobloxId = DiscordRobloxLinks.robloxIdFromDiscordId(closedBy.getIdLong());

        if (_closedByRobloxId == null) {
            event.reply("Moderator: failed to get your linked Roblox ID.").queue();
            return;
        }

        // cancel the interaction
        event.deferEdit().queue();

        // blacklist the user
        ticket.closeAndBlacklist(event.getJDA(), closedBy, closeReason, blacklistReason, null);

        // file a physical report in the #transcripts channel
        String actionDescription = "Blacklisted for `%s`".formatted(blacklistReason.replace("`", ""));
        ticket.sendTranscriptsMessage(event.getJDA(), closedBy, actionDescription);
    }
}
