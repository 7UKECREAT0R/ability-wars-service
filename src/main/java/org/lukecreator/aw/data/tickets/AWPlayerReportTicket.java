package org.lukecreator.aw.data.tickets;

import com.google.gson.JsonObject;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;
import net.dv8tion.jda.api.interactions.modals.ModalMapping;
import net.dv8tion.jda.api.requests.restaction.MessageCreateAction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lukecreator.aw.AWDatabase;
import org.lukecreator.aw.BloxlinkAPI;
import org.lukecreator.aw.RobloxAPI;
import org.lukecreator.aw.data.*;
import org.lukecreator.aw.discord.ActionModals;
import org.lukecreator.aw.discord.BotCommand;
import org.lukecreator.aw.discord.StaffRoles;
import org.lukecreator.aw.discord.commands.BanCheckCommand;
import org.lukecreator.aw.webserver.PendingRequest;
import org.lukecreator.aw.webserver.PendingRequests;
import org.lukecreator.aw.webserver.fulfillments.InfoFulfillment;
import org.lukecreator.aw.webserver.requests.InfoRequest;

import java.awt.*;
import java.sql.SQLException;
import java.util.*;
import java.util.List;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AWPlayerReportTicket extends AWTicket {
    public static final long IN_GAME_PUNISHMENTS_CHANNEL = 1329630658640875575L;
    public static final long BLACKLIST_ROLE = 1329649113368760354L;
    private static final Map<Pattern, String> SUPPORTED_SERVICES = Map.of(
            Pattern.compile("https?://(?:www\\.)?outplayed\\.tv/roblox/\\w+"), "Outplayed",
            Pattern.compile("https?://(?:www\\.)?medal\\.tv/(\\w{2}/)?games/roblox/clips/[A-z0-9_-]+(\\?invite=[A-z0-9_-]+)?"), "Medal",
            Pattern.compile("https?://(?:www\\.)?youtube\\.com/shorts/[A-z0-9_-]+(\\?si=[A-z0-9_-]+)?([?&]t=\\d+s?)?([?&]feature=shared)?"), "YouTube",
            Pattern.compile("https?://(?:www\\.|m\\.)?youtube\\.com/watch\\?v=[A-z0-9_&=-]+"), "YouTube",
            Pattern.compile("https?://(?:www\\.)?youtu\\.be/[A-z0-9_-]+(\\?si=[A-z0-9_-]+)?([?&]t=\\d+s?)?"), "YouTube",
            Pattern.compile("https?://(?:www\\.)?gyazo\\.com/[a-z0-9]+(\\.\\w{3})?"), "Gyazo"
    );
    private static final Map<Pattern, String> UNSUPPORTED_SERVICES = Map.of(
            Pattern.compile("https?://(?:www\\.)?twitch\\.tv/.+"), "Twitch",
            Pattern.compile("https?://(?:www\\.)?vimeo\\.com/.+"), "Vimeo",
            Pattern.compile("https?://drive?\\.google\\.com/.+"), "Google Drive",
            Pattern.compile("https?://media?\\.discordapp\\.net/attachments/.+"), "Discord Attachment Link",
            Pattern.compile("file:///[A-Z]:/.+\\.\\w+"), "Local File"
    );
    /**
     * A list of tickets which also report the same user.
     * When using this to mass close tickets, make sure to check that the ticket hasn't already been closed, since it won't be automatically removed.
     * <ul>
     * <li>If this ticket is resolved <b>with</b> the reported user being banned, all of these tickets should be closed too.</li>
     * <li>If this ticket is resolved <b>without</b> the reporting user being banned, then nothing should happen.</li>
     * </ul>
     */
    private final List<AWPlayerReportTicket> relatedTickets = new ArrayList<>();
    private String accusedUsername;
    private String ruleBroken;
    private String evidenceURL;
    private String evidenceDetails;
    private Long evidenceId;
    private RobloxAPI.User accusedUser;

    public AWPlayerReportTicket(long id,
                                long discordChannelId, long openedTimestamp,
                                boolean isOpen, String closeReason,
                                long closedByDiscordId, JsonObject inputQuestions,
                                long ownerDiscordId) {
        super(id, discordChannelId, openedTimestamp, isOpen,
                closeReason, closedByDiscordId, inputQuestions, ownerDiscordId);
    }

    /**
     * Builds a string message that will go in the #in-game-punishments channel in Discord.
     *
     * @param bannedBy       The Discord of the moderator that the user was banned by.
     * @param userBanned     The user that was banned.
     * @param ruleBrokenName The name of the rule that was broken.
     * @param exploitName    The name of the exploit that was used.
     * @param evidenceURL    The URL to the evidence.
     * @return A string organizing all the input information into a nicely formatted string.
     */
    public static String buildInGamePunishmentsRecord(User bannedBy, RobloxAPI.User userBanned, String ruleBrokenName, String exploitName, String evidenceURL) {
        return userBanned.username() +
                " - (" + userBanned.userId() + ") - " +
                "[Roblox Profile](" + userBanned.getProfileURL() + ") - Banned by " + bannedBy.getAsMention() + '\n' +
                ruleBrokenName + " - " + exploitName + '\n' +
                evidenceURL;
    }

    /**
     * Send a message in the #in-game-punishments channel in Discord.
     *
     * @param jda     The API instance to use.
     * @param message The message to send.
     * @return An action to be queued or modified.
     */
    public static MessageCreateAction sendInGamePunishmentsMessage(JDA jda, String message) {
        TextChannel channel = jda.getChannelById(TextChannel.class, IN_GAME_PUNISHMENTS_CHANNEL);
        if (channel == null)
            throw new RuntimeException("Could not find the #in-game-punishments channel.");
        return channel.sendMessage(message).setAllowedMentions(Collections.emptySet());
    }

    /**
     * Returns if the given URL is part of our supported service list.
     *
     * @param url The URL to check. It doesn't have to be a valid URL, just anything really.
     * @return {@code true} if the URL is valid and in our supported service list.
     */
    private static boolean isSupportedService(String url) {
        for (Pattern pattern : SUPPORTED_SERVICES.keySet()) {
            if (pattern.matcher(url).matches())
                return true;
        }
        return false;
    }

    /**
     * Extracts the first occurrence of a URL from the input string that matches one of the
     * patterns defined in the supported services map.
     *
     * @param input The input string to search for a supported service URL.
     * @return The matched URL if a supported service URL is found, or {@code null} if no match is found.
     */
    private static String extractSupportedServiceUrl(String input) {
        for (Map.Entry<Pattern, String> entry : SUPPORTED_SERVICES.entrySet()) {
            Matcher matcher = entry.getKey().matcher(input);
            if (matcher.find()) {
                return matcher.group(0);
            }
        }
        return null;
    }

    private static String getSupportedServicesList() {
        Set<String> uniqueServiceNames = new HashSet<>(SUPPORTED_SERVICES.values());
        return "**" + String.join("**, **", uniqueServiceNames) + "**";
    }

    /**
     * Returns the name of the service associated with the given URL, if any.
     *
     * @param url The URL to get the service name of. It doesn't have to be a valid URL, just anything really.
     * @return {@code null} if a service name isn't hardcoded for the input URL, or the input URL is otherwise not valid.
     */
    @Nullable
    private static String getKnownServiceName(String url) {
        for (Map.Entry<Pattern, String> entry : UNSUPPORTED_SERVICES.entrySet()) {
            if (entry.getKey().matcher(url).matches())
                return entry.getValue();
        }
        for (Map.Entry<Pattern, String> entry : SUPPORTED_SERVICES.entrySet()) {
            if (entry.getKey().matcher(url).matches()) {
                return entry.getValue();
            }
        }
        return null;
    }

    private void setPropertyAccusedUser(@Nullable String username, SlashCommandInteractionEvent event) throws SQLException {
        if (username == null || username.isBlank()) {
            event.reply("Please provide a valid username/user ID.").setEphemeral(true).queue();
            return;
        }

        RobloxAPI.User tempUser;

        try {
            long id = Long.parseLong(username);
            tempUser = RobloxAPI.getUserById(id);
            if (tempUser == null) {
                event.reply("Couldn't find a Roblox user with the ID `%d`".formatted(id)).setEphemeral(false).queue();
                return;
            }
        } catch (NumberFormatException ignored) {
            tempUser = RobloxAPI.getUserByCurrentUsername(username);
            if (tempUser == null) {
                event.reply("Couldn't find a Roblox user with the name `%s`".formatted(username.replace("`", ""))).setEphemeral(false).queue();
                return;
            }
        }

        AWEvidence evidence = AWEvidence.loadFromDatabase(this.evidenceId);
        if (evidence != null) {
            AWEvidence.removeFromDatabase(this.evidenceId);
            evidence.accusedUserRobloxId = tempUser.userId();
            evidence.pushToDatabase();
        }

        this.accusedUser = tempUser;
        this.accusedUsername = tempUser.username();
        this.updateInDatabase();
        event.reply(("Successfully changed the accused Roblox user to [%s](%s).\n" +
                "-# Note: the message at the top won't change, but the buttons will work as intended.").formatted(tempUser.username(), tempUser.getProfileURL())).setEphemeral(false).queue();
    }

    private void setPropertyAddExtraEvidence(@Nullable String evidence, SlashCommandInteractionEvent event) throws SQLException {
        if (!this.hasEvidence()) {
            this.setPropertyChangeMainEvidence(evidence, event);
            return;
        }
        if (evidence == null || evidence.isBlank()) {
            event.reply("`add-evidence` requires a value.").setEphemeral(true).queue();
            return;
        }

        String evidenceURL = extractSupportedServiceUrl(evidence);

        if (evidenceURL == null) {
            event.reply("Please provide a valid link to the evidence.").setEphemeral(true).queue();
            return;
        }

        String evidenceDetails = null;

        if (!evidenceURL.equals(evidence)) // if it's more than the link.
            evidenceDetails = evidence.replace(evidence, "").trim();

        long id = System.currentTimeMillis();
        long timestamp = AWEvidence.tryExtractTimestamp(evidenceDetails);

        AWEvidence newEvidence = new AWEvidence(id, timestamp, this.accusedUser.userId(), this.evidenceDetails, this.evidenceURL);
        newEvidence.pushToDatabase();
        Links.TicketEvidenceLinks.linkEvidenceToTicket(this.id, id);

        event.reply("Added evidence successfully. (evidence ID: `%d`)".formatted(id)).setEphemeral(false).queue();
    }

    private void setPropertyChangeMainEvidence(@Nullable String evidenceURL, SlashCommandInteractionEvent event) throws SQLException {
        if (evidenceURL == null || evidenceURL.isBlank()) {
            event.reply("`change-evidence` requires a value.").setEphemeral(true).queue();
            return;
        }

        String evidence = extractSupportedServiceUrl(evidenceURL);

        if (evidence == null) {
            event.reply("Please provide a valid link to the evidence.").setEphemeral(true).queue();
            return;
        }

        this.evidenceURL = evidence;
        if (!evidenceURL.equals(evidence)) // if it's more than the link.
        {
            if (this.evidenceDetails == null || this.evidenceDetails.isBlank())
                this.evidenceDetails = evidenceURL.replace(evidence, "").trim();
            else
                this.evidenceDetails = this.evidenceDetails + "\n\n" + (evidenceURL.replace(evidence, "").trim());
        }

        long id = System.currentTimeMillis();
        long previousEvidenceId = this.evidenceId;
        this.evidenceId = id;
        long timestamp = AWEvidence.tryExtractTimestamp(this.evidenceDetails);

        AWEvidence newEvidence = new AWEvidence(id, timestamp, this.accusedUser.userId(), this.evidenceDetails, this.evidenceURL);
        newEvidence.pushToDatabase();
        Links.TicketEvidenceLinks.linkEvidenceToTicket(this.id, id);
        event.reply("Changed the evidence successfully. Do you want to unregister the previous evidence?\n-# Note: the message at the top won't change.").mention(event.getUser()).setEphemeral(false)
                .addActionRow(Button.of(ButtonStyle.DANGER, "delete-evidence_" + previousEvidenceId, "Unregister It")).queue();
    }

    private void setPropertyRuleBroken(@Nullable String newRuleBroken, SlashCommandInteractionEvent event) throws SQLException {
        if (newRuleBroken == null || newRuleBroken.isBlank()) {
            event.reply("`rule-broken` requires a value.").setEphemeral(true).queue();
            return;
        }

        this.ruleBroken = newRuleBroken;
        this.updateInDatabase();
        event.reply("Successfully changed the rule-broken message to `%s`.\n-# Note: the message at the top won't change.".formatted(newRuleBroken)).setEphemeral(false).queue();
    }

    @Override
    public void setProperty(@NotNull String key, @Nullable String value, SlashCommandInteractionEvent event) throws SQLException {
        switch (key) {
            case "accused-user" -> this.setPropertyAccusedUser(value, event);
            case "add-extra-evidence" -> this.setPropertyAddExtraEvidence(value, event);
            case "change-main-evidence" -> this.setPropertyChangeMainEvidence(value, event);
            case "rule-broken" -> this.setPropertyRuleBroken(value, event);
            default -> event.reply("Invalid property name: `%s`".formatted(key)).setEphemeral(true).queue();
        }
    }

    @Override
    public String[] getPropertyChoices() {
        return new String[]{
                "accused-user",
                "add-extra-evidence",
                "change-main-evidence",
                "rule-broken"
        };
    }

    /**
     * Adds additional evidence from a given message to the ticket. If the ticket already has evidence,
     * it attempts to change the main evidence. If no evidence is present in the ticket and the message
     * does not contain valid attachments or URLs, a suitable error message is returned.
     * <p>
     * The method handles different scenarios such as messages with attachments, supported URLs, and
     * invalid inputs. If valid evidence is provided, it creates a new evidence entry and links it to
     * the current ticket.
     *
     * @param message The message object containing the evidence information, such as text,
     *                attachments, or links.
     * @return A string message indicating the result of the operation, such as success or an error
     * description if the evidence could not be added.
     * @throws SQLException If there is an error interacting with the database while adding evidence.
     */
    public String addExtraEvidenceFromMessage(Message message) throws SQLException {
        if (!this.hasEvidence()) {
            return this.changeMainEvidenceFromMessage(message);
        }

        List<Message.Attachment> attachments = message.getAttachments();
        if (attachments.isEmpty()) {
            String evidenceURL = extractSupportedServiceUrl(message.getContentRaw());
            if (evidenceURL == null) {
                return "Please provide a valid link to the evidence.";
            }

            long id = System.currentTimeMillis();
            String evidenceDetails = message.getContentRaw().replace(evidenceURL, "").trim();
            long timestamp = AWEvidence.tryExtractTimestamp(evidenceDetails);

            AWEvidence newEvidence = new AWEvidence(id, timestamp, this.accusedUser.userId(), evidenceDetails, evidenceURL);
            newEvidence.pushToDatabase();
            Links.TicketEvidenceLinks.linkEvidenceToTicket(this.id, id);

            return "Added evidence successfully. (evidence ID: `%d`)".formatted(id);
        }

        for (Message.Attachment attachment : attachments) {
            if (!AWEvidence.isValidAttachment(attachment)) {
                String extension = attachment.getFileExtension();
                if (extension == null) {
                    return "We can't accept attachments with that file type. For videos, we support `.mp4`, and for images, we support all popular file types.";
                }
                return "We can't accept attachments with the file type `." + extension.replace("`", "") + "`. For videos, we support `.mp4`, and for images, we support all popular file types.";
            }
        }

        MessageChannel forwardChannel = message.getGuild()
                .getChannelById(TextChannel.class, AWEvidence.VIDEO_FORWARD_CHANNEL);
        if (forwardChannel == null) {
            return "I couldn't find the `mp4-storage` channel, please contact the developers.";
        }

        final String messageContent = message.getContentStripped().trim();
        final long timestamp = AWEvidence.tryExtractTimestamp(messageContent);

        message.forwardTo(forwardChannel).queue(newMsg -> {
            this.evidenceURL = newMsg.getJumpUrl();
            long id = System.currentTimeMillis();

            if (!messageContent.isBlank()) {
                if (this.evidenceDetails == null || this.evidenceDetails.isBlank())
                    this.evidenceDetails = messageContent;
                else
                    this.evidenceDetails = this.evidenceDetails + "\n\n" + messageContent;
            }

            try {
                AWEvidence evidence = new AWEvidence(id, timestamp, this.accusedUser.userId(), messageContent, this.evidenceURL);
                evidence.pushToDatabase();
                Links.TicketEvidenceLinks.linkEvidenceToTicket(this.id, id);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });

        return "Added evidence successfully.";
    }

    /**
     * Attempts to change the main evidence associated with the ticket based on the provided
     * message. The evidence can be a URL or valid attachments in the message. If no valid
     * evidence is found, an appropriate error message is returned.
     * <p>
     * The method processes various scenarios, such as:
     * - Messages containing URLs that match supported services.
     * - Messages with valid attachments (specific file formats supported).
     * - Error handling for unsupported file types or missing evidence.
     * <p>
     * If valid evidence is provided, it replaces the current evidence, updates the database,
     * and links the new evidence to the ticket.
     *
     * @param message The message containing the evidence in the form of a URL, attachments,
     *                or textual details.
     * @return A string message indicating the result of the operation, such as success or a
     * descriptive error message if the evidence could not be changed.
     * @throws SQLException If an error occurs while interacting with the database.
     */
    public String changeMainEvidenceFromMessage(Message message) throws SQLException {
        List<Message.Attachment> attachments = message.getAttachments();
        long previousEvidenceId = this.evidenceId;

        if (attachments.isEmpty()) {
            String evidenceURL = extractSupportedServiceUrl(message.getContentRaw());
            if (evidenceURL == null) {
                return "Please provide a valid link to the evidence.";
            }

            long id = System.currentTimeMillis();
            String evidenceDetails = message.getContentRaw().replace(evidenceURL, "").trim();
            long timestamp = AWEvidence.tryExtractTimestamp(evidenceDetails);

            this.evidenceURL = evidenceURL;
            if (!message.getContentRaw().equals(evidenceURL)) {
                if (this.evidenceDetails == null || this.evidenceDetails.isBlank())
                    this.evidenceDetails = evidenceDetails;
                else
                    this.evidenceDetails = this.evidenceDetails + "\n\n" + evidenceDetails;
            }

            this.evidenceId = id;

            AWEvidence newEvidence = new AWEvidence(id, timestamp, this.accusedUser.userId(), this.evidenceDetails, this.evidenceURL);
            newEvidence.pushToDatabase();
            Links.TicketEvidenceLinks.linkEvidenceToTicket(this.id, id);

            return "Changed the evidence successfully. Previous evidence ID: `%d`".formatted(previousEvidenceId);
        }

        for (Message.Attachment attachment : attachments) {
            if (!AWEvidence.isValidAttachment(attachment)) {
                String extension = attachment.getFileExtension();
                if (extension == null) {
                    return "We can't accept attachments with that file type. For videos, we support `.mp4`, and for images, we support all popular file types.";
                }
                return "We can't accept attachments with the file type `." + extension.replace("`", "") + "`. For videos, we support `.mp4`, and for images, we support all popular file types.";
            }
        }

        MessageChannel forwardChannel = message.getGuild()
                .getChannelById(TextChannel.class, AWEvidence.VIDEO_FORWARD_CHANNEL);
        if (forwardChannel == null) {
            return "I couldn't find the `mp4-storage` channel, please contact the developers.";
        }

        final String messageContent = message.getContentStripped().trim();
        final long timestamp = AWEvidence.tryExtractTimestamp(messageContent);

        message.forwardTo(forwardChannel).queue(newMsg -> {
            this.evidenceURL = newMsg.getJumpUrl();
            long id = System.currentTimeMillis();

            if (!messageContent.isBlank()) {
                if (this.evidenceDetails == null || this.evidenceDetails.isBlank())
                    this.evidenceDetails = messageContent;
                else
                    this.evidenceDetails = this.evidenceDetails + "\n\n" + messageContent;
            }

            try {
                this.evidenceId = id;
                AWEvidence newEvidence = new AWEvidence(id, timestamp, this.accusedUser.userId(), this.evidenceDetails, this.evidenceURL);
                newEvidence.pushToDatabase();
                Links.TicketEvidenceLinks.linkEvidenceToTicket(this.id, id);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });

        return "Changed the evidence successfully. Previous evidence ID: `%d`".formatted(previousEvidenceId);
    }


    public RobloxAPI.User getAccusedUser() {
        return this.accusedUser;
    }

    public String getRuleBroken() {
        return this.ruleBroken;
    }

    public String getEvidenceURL() {
        return this.evidenceURL;
    }

    public String getEvidenceDetails() {
        return this.evidenceDetails;
    }

    /**
     * The ID of the evidence attached to this ticket, if any.
     */
    public Long getEvidenceId() {
        return this.evidenceId;
    }

    public boolean hasEvidence() {
        return this.evidenceURL != null && !this.evidenceURL.isEmpty();
    }

    public boolean hasEvidenceDetails() {
        return this.evidenceDetails != null && !this.evidenceDetails.isEmpty();
    }

    @Override
    public Type type() {
        return Type.PlayerReport;
    }

    @Override
    public JsonObject getInputQuestionsJSON() {
        JsonObject result = new JsonObject();
        result.addProperty("username", this.accusedUsername);
        result.addProperty("rule", this.ruleBroken);
        if (this.evidenceURL != null && !this.evidenceURL.isEmpty())
            result.addProperty("evidence", this.evidenceURL);
        if (this.evidenceDetails != null && !this.evidenceDetails.isEmpty())
            result.addProperty("details", this.evidenceDetails);
        return result;
    }

    @Override
    public void processInputQuestionsJSON(JsonObject json) {
        this.accusedUsername = json.get("username").getAsString();
        this.ruleBroken = json.get("rule").getAsString();
        if (json.has("evidence"))
            this.evidenceURL = json.get("evidence").getAsString();
        if (json.has("details"))
            this.evidenceDetails = json.get("details").getAsString();
    }

    @Override
    public void afterCacheLoaded() {
        // check if any other tickets are actively open reporting the same user and link them together
        Collection<AWTicket> openTickets = AWTicketsManager.getOpenTickets();
        this.relatedTickets.clear();
        for (AWTicket _ticket : openTickets) {
            if (!(_ticket instanceof AWPlayerReportTicket ticket))
                continue;
            if (ticket.accusedUsername.equalsIgnoreCase(this.accusedUsername)) {
                // if one is resolved, related ones should be resolved too
                ticket.relatedTickets.add(this);
                this.relatedTickets.add(ticket);
            }
        }

        // load player
        this.accusedUser = RobloxAPI.getUserByCurrentUsername(this.accusedUsername);

        // try to load evidence ID
        try {
            AWEvidence[] evidences = Links.TicketEvidenceLinks.getEvidenceLinkedToTicket(this.id);
            if (evidences.length > 0)
                this.evidenceId = evidences[evidences.length - 1].evidenceId;
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void afterTicketChannelCreated(TextChannel channel) {
        if (!this.hasEvidence()) {
            // the user still needs to attach evidence
            EmbedBuilder eb = new EmbedBuilder();
            eb.setColor(Color.orange);
            eb.setTitle("Awaiting evidence...");
            eb.setDescription("Please attach/upload video evidence of the rule-break in this channel, along with any additional information.\n\nIf the video is long, please include the timestamp of when the rule-break occurred as well.");
            channel.sendMessageEmbeds(eb.build())
                    .mention(UserSnowflake.fromId(this.ownerDiscordId))
                    .queue();
        }
    }

    @Override
    public void afterInitialMessageSent(TextChannel channel, Message message) {
        if (this.accusedUser == null)
            return;

        final MessageEmbed initialEmbed = message.getEmbeds().get(0);
        if (initialEmbed == null)
            return; // something bad must have happened?

        PendingRequest extraInfoRequest = new InfoRequest(PendingRequest.getNextRequestId(), this.accusedUser.userId())
                .onFulfilled(_response -> {
                    InfoFulfillment response = (InfoFulfillment) _response;
                    AWBan[] bans = response.bans;
                    EmbedBuilder extraInfoEmbed = new EmbedBuilder()
                            .setTitle("Ability Wars Info")
                            .setFooter("For user \"%s\"".formatted(this.accusedUser.username()))
                            .setColor(Color.RED);
                    if (bans != null && bans.length > 0) {
                        extraInfoEmbed.setDescription("Bans on record:\n");
                        BanCheckCommand.generateBanRecordDescription(extraInfoEmbed, bans, true, false);
                    } else
                        extraInfoEmbed.setDescription("No bans on record.");
                    extraInfoEmbed.addField("Punches", String.valueOf(response.punches), true);
                    if (response.gamepasses != null && response.gamepasses.length > 0)
                        extraInfoEmbed.addField("Gamepasses", String.join(", ", response.gamepassNames()), true);
                    message.editMessageEmbeds(initialEmbed, extraInfoEmbed.build()).queue();
                });
        PendingRequests.add(extraInfoRequest);
    }

    private void onReceivedEvidenceAttachment(MessageReceivedEvent event) {
        Message message = event.getMessage();
        MessageChannel forwardChannel = event.getGuild()
                .getChannelById(TextChannel.class, AWEvidence.VIDEO_FORWARD_CHANNEL);
        if (forwardChannel == null) {
            event.getChannel().sendMessage("I couldn't find the `mp4-storage` channel, please contact the developers.").queue();
            return;
        }

        // check to make sure all attachments are in MP4 format
        List<Message.Attachment> attachments = message.getAttachments();
        for (Message.Attachment attachment : attachments) {
            if (!AWEvidence.isValidAttachment(attachment)) {
                String extension = attachment.getFileExtension();
                if (extension == null) {
                    event.getChannel().sendMessage("We can't accept attachments with that file type. For videos, we support `.mp4`, and for images, we support all popular file types.").queue();
                    return;
                }
                extension = extension.replace("`", ""); // prevent pinging via injection
                event.getChannel().sendMessage("We can't accept attachments with the file type `." + extension + "`. For videos, we support `.mp4`, and for images, we support all popular file types.").queue();
                return;
            }
        }

        final String messageContent = message.getContentStripped().trim();
        final long timestamp = AWEvidence.tryExtractTimestamp(messageContent);

        if (!messageContent.isBlank()) {
            if (this.evidenceDetails == null || this.evidenceDetails.isBlank())
                this.evidenceDetails = messageContent;
            else
                this.evidenceDetails = this.evidenceDetails + "\n\n" + messageContent;
        }

        message.forwardTo(forwardChannel).queue(newMsg -> {
            this.evidenceURL = newMsg.getJumpUrl();
            long id = System.currentTimeMillis();
            this.evidenceId = id;

            AWEvidence evidence = new AWEvidence(id, timestamp, this.accusedUser.userId(), messageContent, this.evidenceURL);

            try {
                evidence.pushToDatabase();
                Links.TicketEvidenceLinks.linkEvidenceToTicket(this.id, id);

                MessageEmbed me = new EmbedBuilder()
                        .setColor(Color.green)
                        .setTitle("Got Evidence")
                        .setDescription("Saved your evidence video down. (id: `%s`)".formatted(id))
                        .addField("Evidence URL", this.evidenceURL, false)
                        .addField("Evidence Info", (this.evidenceDetails == null || this.evidenceDetails.isBlank()) ? "No additional info provided." : this.evidenceDetails, false)
                        .build();
                event.getChannel().sendMessageEmbeds(me).queue();
            } catch (SQLException e) {
                event.getChannel().sendMessage("Something went wrong when trying to save the evidence down to the database. This ticket may still work, but this incident should be reported to the developers.").queue();
                e.printStackTrace();
                return;
            }
        });
    }

    private void onReceivedEvidenceURL(String url, MessageReceivedEvent event) {
        if (url.isEmpty()) {
            event.getChannel().sendMessage("Please provide a valid link to your evidence. Besides uploading directly in Discord, services we support: " + getSupportedServicesList()).queue();
            return;
        }

        String evidence = extractSupportedServiceUrl(url);

        if (evidence == null) {
            event.getChannel().sendMessage("Please provide a valid link to your evidence. Besides uploading directly in Discord, services we support: " + getSupportedServicesList()).queue();
            return;
        }

        this.evidenceURL = evidence;
        if (!url.equals(evidence)) // if it's more than the link.
        {
            if (this.evidenceDetails == null || this.evidenceDetails.isBlank())
                this.evidenceDetails = url.replace(evidence, "").trim();
            else
                this.evidenceDetails = this.evidenceDetails + "\n\n" + (url.replace(evidence, "").trim());
        }

        long id = System.currentTimeMillis();
        this.evidenceId = id;
        long timestamp = AWEvidence.tryExtractTimestamp(this.evidenceDetails);

        AWEvidence newEvidence = new AWEvidence(id, timestamp, this.accusedUser.userId(), this.evidenceDetails, this.evidenceURL);

        try {
            newEvidence.pushToDatabase();
            Links.TicketEvidenceLinks.linkEvidenceToTicket(this.id, id);
            MessageEmbed me = new EmbedBuilder()
                    .setColor(Color.green)
                    .setTitle("Got Evidence")
                    .setDescription("Successfully got your evidence video. (id: `%s`)".formatted(id))
                    .addField("Evidence URL", this.evidenceURL, false)
                    .addField("Evidence Info", (this.evidenceDetails == null || this.evidenceDetails.isBlank()) ? "No additional info provided." : this.evidenceDetails, false)
                    .build();
            event.getChannel().sendMessageEmbeds(me).queue();
        } catch (SQLException e) {
            event.getChannel().sendMessage("Something went wrong when trying to save the evidence down to the database. This ticket may still work, but this incident should be reported to the developers.").queue();
            e.printStackTrace();
            return;
        }
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        Message message = event.getMessage();
        User author = event.getAuthor();

        if (!this.hasEvidence() && author.getIdLong() == this.ownerDiscordId) {
            // assume the user is trying to attach evidence
            List<Message.Attachment> attachments = message.getAttachments();
            if (attachments.isEmpty())
                this.onReceivedEvidenceURL(event.getMessage().getContentRaw(), event);
            else
                this.onReceivedEvidenceAttachment(event);
            return;
        }
    }

    @Override
    public boolean loadFromModalResponse(ModalInteractionEvent event) throws SQLException {
        ModalMapping usernameMapping = event.getValue("username");
        ModalMapping ruleMapping = event.getValue("rule");
        ModalMapping evidenceMapping = event.getValue("evidence");
        ModalMapping evidenceDetailsMapping = event.getValue("details");

        if (usernameMapping == null || ruleMapping == null || evidenceMapping == null || evidenceDetailsMapping == null) {
            event.getHook().editOriginal("Something went wrong: discord sent us incomplete data??? Try again in a couple minutes maybe, or report to the developers if this continues happening.").queue();
            return false;
        }

        this.accusedUsername = usernameMapping.getAsString();
        this.ruleBroken = ruleMapping.getAsString();
        this.evidenceURL = evidenceMapping.getAsString();
        this.evidenceDetails = evidenceDetailsMapping.getAsString();

        // check username
        this.accusedUser = RobloxAPI.getUserByInput(this.accusedUsername, true);

        if (this.accusedUser == null) {
            String errorMessage = BotCommand.getUnknownUsernameDescriptor(this.accusedUsername);
            event.getHook().editOriginal(errorMessage).queue();
            return false;
        }

        // try to get the roblox username of the reporter, so we can make sure they're not reporting themselves
        Long reporterRobloxId = BloxlinkAPI.lookupRobloxId(this.ownerDiscordId);
        if (reporterRobloxId != null) {
            if (this.accusedUser.userId() == reporterRobloxId) {
                event.getHook().editOriginal("You can't report yourself!").queue();
                return false;
            }
        }


        // check evidence (if present)
        if (!this.evidenceURL.isEmpty()) {
            if (!isSupportedService(this.evidenceURL)) {
                String serviceName = getKnownServiceName(this.evidenceURL);
                if (serviceName == null)
                    event.getHook().editOriginal("We don't support the video host you provided ([this one](" + this.evidenceURL + ")) for video evidence! Please upload your video to YouTube, Gyazo, or leave the field empty and upload the video directly into Discord later.").queue();
                else
                    event.getHook().editOriginal("We don't support " + serviceName + " for video evidence! Please upload your video to YouTube, Gyazo, or leave the field empty and upload the video directly into Discord later.").queue();
                return false;
            }
            // supported service. create an evidence entry for it
            long id = System.currentTimeMillis(); // is probably unique
            this.evidenceId = id;
            long timestamp = this.hasEvidenceDetails() ? AWEvidence.tryExtractTimestamp(this.evidenceDetails) : 0L;

            AWEvidence evidence = new AWEvidence(id, timestamp, this.accusedUser.userId(), this.evidenceDetails, this.evidenceURL);
            evidence.pushToDatabase();

            // link the ticket and evidence together in the database
            Links.TicketEvidenceLinks.linkEvidenceToTicket(this.id, id);
        }

        this.accusedUsername = this.accusedUser.username();
        long reportedUserId = this.accusedUser.userId();
        AWPlayer player = AWDatabase.loadPlayer(reportedUserId, false, true, false, false);

        // if the user is currently banned, cancel and let the reporter know
        if (player.bans.isCurrentlyBanned()) {
            event.getHook().editOriginal("The user you tried to report has already been banned! Thanks for your efforts!").queue();
            return false;
        }

        // check if any other tickets are actively open reporting the same user and link them together
        this.afterCacheLoaded();

        return true;
    }

    @Override
    public Modal createInputModal(long newTicketId) {
        TextInput usernameInput = TextInput
                .create("username", "Username", TextInputStyle.SHORT)
                .setRequired(true)
                .setRequiredRange(2, 20)
                .setPlaceholder("The username of the rule-breaker.")
                .build();
        TextInput ruleBrokenInput = TextInput
                .create("rule", "Rule Broken", TextInputStyle.SHORT)
                .setRequired(true)
                .setRequiredRange(-1, 128)
                .setPlaceholder("exploiting")
                .build();
        TextInput evidenceInput = TextInput
                .create("evidence", "Evidence", TextInputStyle.SHORT)
                .setRequired(false)
                .setRequiredRange(-1, 1024)
                .setPlaceholder("Video evidence link; leave empty to upload in Discord.")
                .build();
        TextInput evidenceDetails = TextInput
                .create("details", "Timestamp and Extra Details", TextInputStyle.PARAGRAPH)
                .setRequired(false)
                .setRequiredRange(-1, 1024)
                .setPlaceholder("The time in the video of the rule-break, if applicable, and other details.")
                .build();

        Type type = this.type();
        String customId = type.getCreationModalCustomId() + "_" + newTicketId;
        return Modal.create(customId, type.description)
                .addActionRow(usernameInput)
                .addActionRow(ruleBrokenInput)
                .addActionRow(evidenceInput)
                .addActionRow(evidenceDetails)
                .build();
    }

    @Override
    public TicketAction[] getAvailableActions() {
        return new TicketAction[]{
                new TicketAction("cancel", "Cancel", ButtonStyle.SECONDARY, this.id),
                new TicketAction("close", "Resolve", ButtonStyle.PRIMARY, this.id),
                new TicketAction("closebadevidence", "Resolve (Bad Evidence)", ButtonStyle.PRIMARY, this.id),
                new TicketAction("closenotbannable", "Resolve (Not Bannable)", ButtonStyle.PRIMARY, this.id),
                new TicketAction("ban", "Ban", ButtonStyle.DANGER, this.id),
                new TicketAction("banreason", "Ban (override close reason)", ButtonStyle.DANGER, this.id),
                new TicketAction("tempban", "Tempban", ButtonStyle.DANGER, this.id),
                new TicketAction("tempbanreason", "Tempban (override close reason)", ButtonStyle.DANGER, this.id)
        };
    }

    @Override
    public List<MessageEmbed> getInitialMessageEmbeds(JDA jda) {
        User ticketOwner = jda.getUserById(this.ownerDiscordId);

        EmbedBuilder eb = new EmbedBuilder();
        eb.setColor(new Color(103, 110, 233));
        eb.setTitle("Ticket " + this.id);
        if (ticketOwner == null)
            eb.setDescription("Player report, opened by <@" + this.ownerDiscordId + ">");
        else
            eb.setDescription("Player report, opened by %s (%s)".formatted(ticketOwner.getAsMention(), ticketOwner.getName()));

        eb.addField("Reported User", "`%s`, [Roblox Profile](%s)".formatted(
                this.accusedUser.username(),
                this.accusedUser.getProfileURL()
        ), false);

        eb.addField("Rule Broken", this.getRuleBroken(), false);
        if (this.hasEvidence())
            eb.addField("Evidence URL", this.getEvidenceURL(), false);
        if (this.hasEvidenceDetails())
            eb.addField("Evidence Details", this.getEvidenceDetails(), false);

        if (!this.relatedTickets.isEmpty()) {
            long[] ticketIds = this.relatedTickets.stream()
                    .mapToLong(AWTicket::getDiscordChannelId)
                    .toArray();
            StringJoiner joiner = new StringJoiner(", ");
            for (long ticketId : ticketIds)
                joiner.add("<#" + ticketId + ">");
            String relatedTicketsString = joiner.toString();
            eb.addField("Related Ticket(s)", relatedTicketsString, false);
        }

        List<MessageEmbed> result = new ArrayList<>();
        result.add(eb.build());
        result.add(new EmbedBuilder()
                .setTitle("Collecting info...")
                .setDescription("-# Sent a request to Ability Wars to gather more information about the user. If this message doesn't update in the next 5-10 seconds, use a command manually to look up their information instead.")
                .setColor(Color.white)
                .build()
        );

        return result;
    }

    @Override
    public void handleAction(String actionId, ButtonInteractionEvent event) {
        User clickedUser = event.getUser();
        long clickedUserId = clickedUser.getIdLong();

        if (actionId.equals("cancel")) {
            // must be ticket opener or staff
            if (this.ownerDiscordId == clickedUserId || StaffRoles.isStaff(event.getMember())) {
                try {
                    this.close(event.getJDA(), clickedUser, "Cancelled manually", null);
                } catch (SQLException e) {
                    e.printStackTrace();
                }
                return;
            }
            event.reply("You must be the ticket opener or staff to cancel this ticket.").setEphemeral(true).queue();
            return;
        }

        if (StaffRoles.blockIfNotStaff(event))
            return;

        switch (actionId) {
            case "close": { // no action, close with reason
                Modal modal = ActionModals.closeTicketWithCustomReason(this);
                event.replyModal(modal).queue();
                break;
            }
            case "closebadevidence": { // no action, close (bad evidence)
                try {
                    event.deferEdit().queue();
                    this.tryRemoveEvidence(event.getJDA());
                    this.close(event.getJDA(), clickedUser, "We couldn't punish the user using your evidence, sorry!", null);
                } catch (SQLException e) {
                    e.printStackTrace();
                }
                break;
            }
            case "closenotbannable": { // no action, close (not bannable)
                try {
                    event.deferEdit().queue();
                    this.tryRemoveEvidence(event.getJDA());
                    this.close(event.getJDA(), clickedUser, "Unfortunately, what you've reported (\"" + this.ruleBroken + "\") isn't bannable. Thanks for submitting a report, though.", null);
                } catch (SQLException e) {
                    e.printStackTrace();
                }
                break;
            }
            case "ban": { // ban
                Modal banModal = ActionModals.closeTicketAndBanWithPresetReason(this, "Banned, thanks for reporting!");
                event.replyModal(banModal).queue();
                break;
            }
            case "banreason": { // ban with reason override
                Modal banModal = ActionModals.closeTicketAndBanWithCustomReason(this);
                event.replyModal(banModal).queue();
                break;
            }
            case "tempban": { // tempban
                Modal banModal = ActionModals.closeTicketAndTempbanWithPresetReason(this, "Banned, thanks for reporting!");
                event.replyModal(banModal).queue();
                break;
            }
            case "tempbanreason": { // tempban with reason override
                Modal banModal = ActionModals.closeTicketAndTempbanWithCustomReason(this);
                event.replyModal(banModal).queue();
                break;
            }
        }
    }

    /**
     * If this ticket has any evidence attached to it, try to unregister/remove that evidence.
     *
     * @param jda The API to use.
     * @throws SQLException If something goes wrong with the database.
     */
    public void tryRemoveEvidence(JDA jda) throws SQLException {
        if (this.evidenceId != null && this.evidenceId != 0L) {
            AWEvidence evidence = AWEvidence.loadFromDatabase(this.evidenceId);
            if (evidence != null) {
                Links.TicketEvidenceLinks.unlinkEvidenceFromTicket(this.id, this.evidenceId);
                String url = evidence.url;
                Matcher jumpUrlMatcher = Message.JUMP_URL_PATTERN.matcher(url);
                if (jumpUrlMatcher.matches()) {
                    // it's a jump URL
                    String guildId = jumpUrlMatcher.group(1);
                    String channelId = jumpUrlMatcher.group(2);
                    String messageId = jumpUrlMatcher.group(3);
                    Guild guild = jda.getGuildById(guildId);
                    if (guild == null)
                        return;
                    MessageChannel channel = guild.getTextChannelById(channelId);
                    if (channel == null)
                        return;
                    channel.deleteMessageById(messageId).queue();
                }
            }
        }
    }

    public void closeRelated(JDA jda, User closedByUsed, String closeReason, Consumer<JDA> onSuccess) throws SQLException {
        if (this.relatedTickets.isEmpty())
            return;
        for (AWPlayerReportTicket ticket : this.relatedTickets) {
            ticket.close(jda, closedByUsed, closeReason, onSuccess);
        }
    }
}
