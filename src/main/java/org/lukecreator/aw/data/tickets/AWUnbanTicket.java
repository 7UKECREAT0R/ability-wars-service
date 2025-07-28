package org.lukecreator.aw.data.tickets;

import com.google.gson.JsonObject;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;
import net.dv8tion.jda.api.interactions.modals.ModalMapping;
import net.dv8tion.jda.api.requests.restaction.MessageCreateAction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lukecreator.aw.RobloxAPI;
import org.lukecreator.aw.data.*;
import org.lukecreator.aw.discord.AbilityWarsBot;
import org.lukecreator.aw.discord.ActionModals;
import org.lukecreator.aw.discord.BotCommand;
import org.lukecreator.aw.discord.StaffRoles;
import org.lukecreator.aw.discord.commands.BanCheckCommand;
import org.lukecreator.aw.webserver.PendingRequest;
import org.lukecreator.aw.webserver.PendingRequests;
import org.lukecreator.aw.webserver.fulfillments.InfoFulfillment;
import org.lukecreator.aw.webserver.requests.InfoRequest;
import org.lukecreator.aw.webserver.requests.UnbanRequest;

import java.awt.*;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * A ticket in which a user is trying to be unbanned
 */
public abstract class AWUnbanTicket extends AWTicket {
    public final static long INFO_CHANNEL_ID = 982588177091022888L;
    public final static long TRANSCRIPTS_CHANNEL_ID = 978532026565132318L;
    protected final static String EMOJI_DISCORD = Emoji.fromCustom("discord", 1349347174701334528L, false).getAsMention();
    protected final static String EMOJI_IN_GAME = Emoji.fromCustom("ingame", 1349347143017693226L, false).getAsMention();
    protected final static String JSON_IS_FOR_DISCORD = "is_for_discord";
    protected final static String JSON_USER_ID = "user_id";
    private static final String[] PING_WARNING_MESSAGES = {
            "%s, please don't ping staff.",
            "%s, all of the staff are busy, please be patient and stop pinging them!",
            "Hey! %s! Please stop pinging people. We are volunteers, and we will respond to your ticket as soon as we can.",
            "%s, stop pinging staff, seriously. All of us are volunteers, and we will do your ticket once we have time available. We're not paid to do this. Please stop pinging us.",
            "Yoohoo! %s! I'm not trying to sound rude, but you're really bad at this waiting thing, aren't you? Please stop pinging staff, it's a disturbance.",
            "Okay, %s, if you ping staff again, this ticket will be closed and you'll be knocked back to the bottom of the queue. Please, for the love of all that is holy, be patient."
    };
    /**
     * Is this a ticket for a Discord unban?
     * <ul>
     *     <li>If {@code true} use {@link #discordIdToUnban}</li>
     *     <li>If {@code false} use {@link #robloxIdToUnban}</li>
     * </ul>
     */
    protected boolean isForDiscord;
    /**
     * If {@link #isForDiscord}, this is the ID of the Discord user to unban.
     */
    protected long discordIdToUnban;
    /**
     * If not {@link #isForDiscord}, this is the ID of the Roblox user to unban.
     */
    protected long robloxIdToUnban;
    /**
     * If not {@link #isForDiscord}, this is the Roblox user to unban.
     * This could be null for whatever reason, so be careful when using it.
     */
    @Nullable
    protected RobloxAPI.User robloxUserToUnban;
    /**
     * If not {@link #isForDiscord}, this is the AWPlayer database object attached to {@link #robloxIdToUnban}.
     */
    protected AWPlayer playerToUnban;
    /**
     * If {@link #isForDiscord}, this is the ban tied to the user {@link #discordIdToUnban}. May be null if the ban
     * doesn't exist, or this ticket has been reloaded via cache since being created. This field isn't loaded after a cache load (see {@link #afterCacheLoaded()}).
     */
    @Nullable
    protected Guild.Ban discordBan;
    /**
     * The number of times a ping warning has been sent to the user for this ticket.
     */
    private int numberOfPingWarnings = 0;

    public AWUnbanTicket(long id, long discordChannelId, long openedTimestamp,
                         boolean isOpen, String closeReason, long closedByDiscordId, JsonObject inputQuestions, long ownerDiscordId) {
        super(id, discordChannelId, openedTimestamp, isOpen, closeReason, closedByDiscordId, inputQuestions, ownerDiscordId);
    }

    /**
     * Sends a message to the #transcripts channel in the server using the provided JDA instance and message content.
     * Ensures the message does not allow mentions.
     *
     * @param jda     The JDA instance to interact with the Discord API.
     * @param message The text content of the message to send to the #transcripts channel.
     * @return A MessageCreateAction object representing the message action, which can be queued or modified.
     * @throws RuntimeException If the #transcripts channel cannot be found in the server.
     */
    public static MessageCreateAction sendTranscriptsMessageRaw(JDA jda, String message) {
        TextChannel channel = jda.getChannelById(TextChannel.class, TRANSCRIPTS_CHANNEL_ID);
        if (channel == null)
            throw new RuntimeException("Could not find the #transcripts channel.");

        return channel.sendMessage(message).setAllowedMentions(Collections.emptySet());
    }

    public boolean isForDiscord() {
        return this.isForDiscord;
    }

    public long getIdToUnban() {
        return this.isForDiscord ? this.discordIdToUnban : this.robloxIdToUnban;
    }

    @Override
    public void setProperty(@NotNull String key, @Nullable String value, SlashCommandInteractionEvent event) throws SQLException {
        if (value == null || value.isBlank()) {
            event.reply("Please specify a value for the property `%s`.".formatted(key)).setEphemeral(true).queue();
            return;
        }

        if (key.equalsIgnoreCase("discord")) {
            if (!this.isForDiscord) {
                event.reply("This is not a Discord unban ticket; you can't change the Discord user.").setEphemeral(true).queue();
                return;
            }

            try {
                long id = Long.parseLong(value);
                Guild abilityWars = event.getJDA().getGuildById(AbilityWarsBot.AW_GUILD_ID);
                if (abilityWars == null) {
                    event.reply("The Ability Wars server is not available right now. Please try again later.").queue();
                    return;
                }

                event.deferReply(false).queue();
                abilityWars.retrieveBan(UserSnowflake.fromId(id)).queue(ban -> {
                    this.discordIdToUnban = id;
                    this.discordBan = ban;
                    try {
                        this.updateInDatabase();
                    } catch (SQLException e) {
                        e.printStackTrace();
                    } finally {
                        event.getHook().editOriginal("Successfully changed the Discord user to be unbanned to <@%d>. ID: %1$d".formatted(id))
                                .mention(Collections.emptySet()).queue();
                    }
                }, failure -> {
                    event.getHook().editOriginal("The user <@%d> is not currently banned from the Discord Server, or doesn't exist.".formatted(id)).queue();
                });
            } catch (NumberFormatException ignored) {
                event.reply("The Discord ID `%s` is not a valid ID.".formatted(value)).setEphemeral(true).queue();
                return;
            }
        }
        if (key.equalsIgnoreCase("roblox")) {
            if (this.isForDiscord) {
                event.reply("This is not a Roblox unban ticket; you can't change the Roblox user.").setEphemeral(true).queue();
                return;
            }

            RobloxAPI.User tempUser;
            try {
                long id = Long.parseLong(value);
                tempUser = RobloxAPI.getUserById(id);
                if (tempUser == null) {
                    event.reply("Couldn't find a Roblox user with the ID `%d`".formatted(id)).setEphemeral(false).queue();
                    return;
                }
            } catch (NumberFormatException ignored) {
                tempUser = RobloxAPI.getUserByCurrentUsername(value);
                if (tempUser == null) {
                    event.reply("Couldn't find a Roblox user with the name `%s`".formatted(value.replace("`", ""))).setEphemeral(false).queue();
                    return;
                }
            }

            this.robloxIdToUnban = tempUser.userId();
            this.robloxUserToUnban = tempUser;
            this.playerToUnban = AWPlayer.loadFromDatabase(this.robloxIdToUnban, true, true, true, false);
            this.updateInDatabase();
            event.reply("Successfully changed the Roblox user to be unbanned to [%s](%s)".formatted(tempUser.username(), tempUser.getProfileURL())).setEphemeral(false).queue();
        }
    }

    @Override
    public String[] getPropertyChoices() {
        return new String[]{
                this.isForDiscord ? "discord" : "roblox",
        };
    }

    /**
     * Send a log in the #transcripts channel in the ward with the given action description.
     *
     * @param jda    The API instance to use.
     * @param action A description of what happened to the user. E.g., "blacklisted for lying in their appeal"
     */
    public void sendTranscriptsMessage(JDA jda, String action) {
        TextChannel channel = jda.getChannelById(TextChannel.class, TRANSCRIPTS_CHANNEL_ID);
        if (channel == null)
            throw new RuntimeException("Could not find the #transcripts channel.");

        String message = this.getTranscriptMessage(action);
        channel.sendMessage(message).setAllowedMentions(Collections.emptySet()).queue();
    }

    /**
     * Sends a warning message to a specified user in a given message channel.
     * The message warns the user about excessive pings and uses a preset sequence of warning messages.
     *
     * @param channel The {@link MessageChannel} where the warning message will be sent.
     * @param user    The {@link User} who will receive the warning message.
     */
    private void sendPingWarning(MessageChannel channel, User user) {
        String message = PING_WARNING_MESSAGES[this.numberOfPingWarnings++].formatted(user.getAsMention());
        channel.sendMessage(message).queue();
    }

    /**
     * Returns the message that should go in the #transcripts channel, given the input action.
     * Preferably one of these should be true.
     *
     * @param action A description of what happened to the user. e.g., "blacklisted for lying in their appeal"
     */
    private String getTranscriptMessage(String action) {
        StringBuilder sb = new StringBuilder();
        sb.append("Ticket #").append(this.id).append(" - ").append(this.isAppeal() ? "Appeal" : "Dispute").append('\n');
        if (this.isForDiscord) {
            sb.append("<@").append(this.discordIdToUnban).append(">, ").append(action);
        } else {
            if (this.robloxUserToUnban != null) {
                sb.append(this.robloxUserToUnban.username())
                        .append(", [Roblox Profile](")
                        .append(this.robloxUserToUnban.getProfileURL())
                        .append("), ").append(action);
            } else {
                sb.append("[Roblox Profile](")
                        .append(this.robloxIdToUnban)
                        .append("), ").append(action);
            }
        }

        return sb.toString();
    }

    @Override
    public JsonObject getInputQuestionsJSON() {
        JsonObject result = new JsonObject();
        result.addProperty("is_discord", this.isForDiscord);
        result.addProperty("id", this.isForDiscord ? this.discordIdToUnban : this.robloxIdToUnban);
        return result;
    }

    @Override
    public void processInputQuestionsJSON(JsonObject json) {
        this.isForDiscord = json.get("is_discord").getAsBoolean();
        long id = json.get("id").getAsLong();
        if (this.isForDiscord)
            this.discordIdToUnban = id;
        else
            this.robloxIdToUnban = id;
    }

    /**
     * Returns {@code true} if this ticket is a ban appeal (i.e., the user is guilty).
     * <p>
     * Returns {@code false} if this ticket is a ban dispute (i.e., the user is innocent).
     */
    protected abstract boolean isAppeal();

    @Override
    public void afterCacheLoaded() {
        if (!this.isForDiscord && this.robloxIdToUnban != 0) {
            this.robloxUserToUnban = RobloxAPI.getUserById(this.robloxIdToUnban);
            this.playerToUnban = AWPlayer.loadFromDatabase(this.robloxIdToUnban, true, true, true, false);
        }
    }

    /**
     * Parent implementation of {@link AWTicket#getInputQuestionsJSON()}.
     *
     * @return A new JSON object to add your input questions properties to. User information is already attached.
     */
    protected JsonObject getInputQuestionsStartJSON() {
        JsonObject json = new JsonObject();
        json.addProperty(JSON_IS_FOR_DISCORD, this.isForDiscord);
        json.addProperty(JSON_USER_ID, this.isForDiscord ? this.discordIdToUnban : this.robloxIdToUnban);
        return json;
    }

    /**
     * Parent implementation of {@link AWTicket#processInputQuestionsJSON(JsonObject)}.
     *
     * @param json The JSON object to parse from.
     */
    protected void processUserFromJSON(JsonObject json) {
        this.isForDiscord = json.get(JSON_IS_FOR_DISCORD).getAsBoolean();
        long id = json.get(JSON_USER_ID).getAsLong();

        if (this.isForDiscord)
            this.discordIdToUnban = id;
        else
            this.robloxIdToUnban = id;
    }

    @Override
    public void afterInitialMessageSent(TextChannel channel, Message message) {
        if (this.isForDiscord)
            return;

        final MessageEmbed initialEmbed = message.getEmbeds().get(0);
        if (initialEmbed == null)
            return;

        if (this.robloxUserToUnban == null) {
            message.editMessageEmbeds(initialEmbed,
                    new EmbedBuilder()
                            .setTitle("Failed to get info.")
                            .setDescription("Input user was invalid, doesn't exist, or roblox is down right now.")
                            .setColor(Color.RED)
                            .build()
            ).queueAfter(1, TimeUnit.SECONDS);
            return;
        }

        PendingRequest banInfoRequest = new InfoRequest(PendingRequest.getNextRequestId(), this.robloxIdToUnban)
                .onFulfilled(_fulfillment -> {
                    InfoFulfillment fulfillment = (InfoFulfillment) _fulfillment;
                    AWBan[] bansOnRecord = fulfillment.bans;
                    EmbedBuilder extraInfoEmbed;

                    if (bansOnRecord != null && bansOnRecord.length > 0) {
                        extraInfoEmbed = new EmbedBuilder()
                                .setTitle("Ban Information")
                                .setFooter("For user \"%s\"".formatted(this.robloxUserToUnban.username()))
                                .setColor(Color.RED);
                        BanCheckCommand.generateBanRecordDescription(extraInfoEmbed, bansOnRecord, true, true);
                        AWBan currentBan = fulfillment.bans[fulfillment.bans.length - 1];
                        long started = currentBan.starts();
                        long now = System.currentTimeMillis();
                        LocalDate startedDateTime = Instant.ofEpochMilli(started).atZone(ZoneId.of("UTC")).toLocalDate();
                        String timeAgoDescriptorString = BotCommand.getDurationDescriptor(started, now);
                        String startedDate = BanCheckCommand.formatter.format(startedDateTime) + " (" + timeAgoDescriptorString + ")";
                        extraInfoEmbed.addField("Banned", startedDate, true);
                    } else {
                        extraInfoEmbed = new EmbedBuilder()
                                .setTitle("No Bans on Record")
                                .setDescription("This user is not currently banned from Ability Wars, and has no bans on record. If you believe this is an error, please escalate to an administrator.")
                                .setFooter("For user \"%s\"".formatted(this.robloxUserToUnban.username()))
                                .setColor(Color.GREEN);
                    }
                    message.editMessageEmbeds(initialEmbed, extraInfoEmbed.build()).queue();
                });
        PendingRequests.add(banInfoRequest);
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        Message message = event.getMessage();
        User messageAuthor = message.getAuthor();
        Mentions mentions = message.getMentions();
        MessageChannel channel = event.getChannel();

        if (messageAuthor.getIdLong() == this.ownerDiscordId) {
            // message was sent by the ticket owner.

            // impatience
            List<Role> mentionedRoles = mentions.getRoles();
            List<Member> mentionedMembers = mentions.getMembers();

            boolean pingsStaff = !mentionedRoles.isEmpty() || mentionedMembers
                    .stream()
                    .filter(member -> !member.getUser().isBot())
                    .anyMatch(StaffRoles::hasStaffRole);

            if (pingsStaff) {
                if (this.numberOfPingWarnings >= PING_WARNING_MESSAGES.length) {
                    // close the ticket; this user has pinged staff way too many times
                    try {
                        this.close(event.getJDA(), messageAuthor, "Pinging staff repeatedly", null);
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                } else {
                    // send a warning to stop pinging staff
                    this.sendPingWarning(channel, messageAuthor);
                }
            }
        }
    }

    @Override
    public Modal createInputModal(long newTicketId) {
        TextInput discordOrRobloxInput = TextInput
                .create("discord-or-roblox", "For Discord or Roblox?", TextInputStyle.SHORT)
                .setPlaceholder("\"discord\" or \"roblox\", nothing else.")
                .setMinLength(5)
                .setMaxLength(8)
                .setMaxLength(MessageEmbed.VALUE_MAX_LENGTH)
                .build();
        TextInput usernameInput = TextInput
                .create("user-id", "User ID", TextInputStyle.SHORT)
                .setPlaceholder("Your Roblox/Discord ID.")
                .setMinLength(6)
                .setMaxLength(32)
                .build();

        Type type = this.type();
        String customId = type.getCreationModalCustomId() + "_" + newTicketId;
        return this.finishInputModal(Modal.create(customId, type.description)
                .addActionRow(discordOrRobloxInput)
                .addActionRow(usernameInput)
        ).build();
    }

    /**
     * Finish building the input modal; same as {@link #createInputModal(long)}.
     *
     * @param modal The modal that has been partially built.
     * @return The modal with up to <b>3</b> additional inputs.
     */
    public abstract Modal.Builder finishInputModal(Modal.Builder modal);

    @Override
    public List<MessageEmbed> getInitialMessageEmbeds(JDA jda) {
        String title = this.isAppeal() ? "Ban Appeal" : "Ban Dispute";
        String verb = this.isAppeal() ? "Appealing for:" : "Disputing for:";
        String description = this.isAppeal() ?
                "Thanks for opening your appeal to be unbanned. A staff member will review this as soon as possible. Please don't `@` mention any staff members during this time and be patient!" :
                "Thanks for opening your dispute, we'll be on this to help you as soon as we're available. Please don't `@` mention any staff members during this time.";
        Color color = this.isAppeal() ?
                new Color(63, 192, 235) :
                new Color(235, 206, 63);

        EmbedBuilder eb = new EmbedBuilder()
                .setTitle(title)
                .setDescription(description)
                .setColor(color);

        // now onto the important stuff
        eb.addField(verb, this.isForDiscord ?
                EMOJI_DISCORD + " Discord Server Ban" :
                EMOJI_IN_GAME + " In-Game Ban", true);
        if (this.isForDiscord) {
            String reason = this.discordBan == null ? "Not Banned?" : this.discordBan.getReason();
            if (reason == null)
                reason = "No reason specified.";

            eb.addField("User ID", "`%d` - <@%1$d>".formatted(this.discordIdToUnban), true);
            eb.addField("Ban Reason", reason, true);
        } else {
            if (this.robloxUserToUnban == null)
                eb.addField("Roblox User", "Username: `Unknown`\nUser ID: `%d`\n\n[Roblox Profile](https://www.roblox.com/users/%1$d/profile)".formatted(this.robloxIdToUnban), true);
            else
                eb.addField("Roblox User", "Username: `%s`\nUser ID: `%d`\n\n[Roblox Profile](%s)".formatted(this.robloxUserToUnban.username(), this.robloxIdToUnban, this.robloxUserToUnban.getProfileURL()), true);
        }
        eb.addField("Opened By", "<@%d> - Discord ID: %1$d".formatted(this.ownerDiscordId), true);

        eb = this.finishInitialMessageEmbed(eb);

        if (this.isForDiscord) {
            return List.of(eb.build());
        } else {
            return List.of(
                    eb.build(),
                    new EmbedBuilder()
                            .setTitle("Collecting info...")
                            .setDescription("-# Sent a request to Ability Wars to gather more information about the user in-game. If this message doesn't update in the next 5-10 seconds, use a command manually to look up their ban information instead.")
                            .setColor(Color.white)
                            .build()
            );
        }
    }

    /**
     * Complete the embed that was started by the parent implementation.
     *
     * @param eb The embed builder to append to.
     * @return The same passed-in embed builder, for easy chaining.
     */
    protected abstract EmbedBuilder finishInitialMessageEmbed(EmbedBuilder eb);

    @Override
    public TicketAction[] getAvailableActions() {
        List<TicketAction> actions = new ArrayList<>();
        actions.add(new TicketAction("close", "Close", ButtonStyle.PRIMARY, this.id));
        actions.add(new TicketAction("closefortime", "Close (keep waiting)", ButtonStyle.PRIMARY, this.id));

        if (this.isAppeal())
            actions.add(new TicketAction("closeloweffort", "Close (bad appeal)", ButtonStyle.PRIMARY, this.id));

        actions.add(new TicketAction("blacklistlying", "Blacklist (lying)", ButtonStyle.DANGER, this.id));

        if (!this.isForDiscord)
            actions.add(new TicketAction("blacklistshared", "Blacklist (shared account)", ButtonStyle.DANGER, this.id));

        actions.add(new TicketAction("blacklistcustom", "Blacklist (custom)", ButtonStyle.DANGER, this.id));
        actions.add(new TicketAction("unban", "Unban", ButtonStyle.SUCCESS, this.id));

        if (!this.isForDiscord)
            actions.add(new TicketAction("unbanunsure", "Unban (unsure)", ButtonStyle.SUCCESS, this.id));

        actions.add(new TicketAction("unbancustom", "Unban (custom)", ButtonStyle.SUCCESS, this.id));

        if (!this.isForDiscord)
            actions.add(new TicketAction("retime", "Set Ban Length", ButtonStyle.SUCCESS, this.id));

        return actions.toArray(new TicketAction[0]);
    }

    @Override
    public void handleAction(String actionId, ButtonInteractionEvent event) {
        User clickedUser = event.getUser();

        if (StaffRoles.blockIfNotStaff(event))
            return;

        switch (actionId) {
            case "close": {
                Modal modal = ActionModals.closeTicketWithCustomReason(this);
                event.replyModal(modal).queue();
                break;
            }
            case "closefortime": {
                Modal modal = ActionModals.closeTicketWithTemplatedReason(this, "Resolve (Time Waited)",
                        "Please open an appeal in {months} months as per <#" + INFO_CHANNEL_ID + ">.",
                        TextInput.create("months", "Months Until...", TextInputStyle.SHORT)
                                .setPlaceholder("The number of months until the user can appeal.")
                                .build());
                event.replyModal(modal).queue();

                break;
            }
            case "closeloweffort": {
                event.deferEdit().queue();
                try {
                    this.close(event.getJDA(), clickedUser, "Please try a little harder with writing your %s.".formatted(this.isAppeal() ? "appeal" : "dispute"), null);
                } catch (SQLException e) {
                    e.printStackTrace();
                }
                break;
            }
            case "blacklistlying": {
                try {
                    event.deferEdit().queue();
                    this.sendTranscriptsMessage(event.getJDA(), "Blacklisted for lying in their appeal");
                    this.closeAndBlacklist(event.getJDA(), clickedUser,
                            this.isForDiscord ?
                                    "Your Discord account <@%d> is ineligible for appeal.".formatted(this.discordIdToUnban) :
                                    this.robloxUserToUnban == null ?
                                            "Your Roblox account [%d](https://www.roblox.com/users/%1$d/profile) is ineligible for appeal.".formatted(this.robloxIdToUnban) :
                                            "Your Roblox account [%s](%s) is ineligible for appeal.".formatted(this.robloxUserToUnban.username(), this.robloxUserToUnban.getProfileURL()),
                            "lying in " + (this.isAppeal() ? "appeal" : "dispute"), null);
                } catch (SQLException e) {
                    e.printStackTrace();
                }
                break;
            }
            case "blacklistshared": {
                try {
                    event.deferEdit().queue();
                    this.sendTranscriptsMessage(event.getJDA(), "Blacklisted for shared account.");
                    this.closeAndBlacklist(event.getJDA(), clickedUser,
                            this.robloxUserToUnban == null ?
                                    "Your Roblox account [%d](https://www.roblox.com/users/%1$d/profile) is ineligible for appeal due to having shared the account with another person.".formatted(this.robloxIdToUnban) :
                                    "Your Roblox account [%s](%s) is ineligible for appeal due to having shared the account with another person.".formatted(this.robloxUserToUnban.username(), this.robloxUserToUnban.getProfileURL()),
                            "shared account", null);
                } catch (SQLException e) {
                    e.printStackTrace();
                }
                break;
            }
            case "blacklistcustom": {
                Modal modal = ActionModals.closeTicketAndBlacklistWithCustomReason(this);
                event.replyModal(modal).queue();
                break;
            }
            case "unban": {
                event.deferEdit().queue();
                this.sendTranscriptsMessage(event.getJDA(), "Unbanned");

                String reason = this.isForDiscord ?
                        "Unbanned, rejoin using `discord.gg/abilitywars`. Please remember to re-read the rules! :)" :
                        "Unbanned, have fun :)";
                try {
                    this.closeAndUnban(event.getJDA(), clickedUser, reason, null);
                } catch (SQLException e) {
                    e.printStackTrace();
                }
                break;
            }
            case "unbanunsure": {
                event.deferEdit().queue();
                this.sendTranscriptsMessage(event.getJDA(), "Unbanned due to poor/missing evidence");
                String reason = this.isForDiscord ?
                        "Unbanned because the evidence we do have isn't conclusive enough. Rejoin using `discord.gg/abilitywars`, and thanks for opening your " + (this.isAppeal() ? "appeal" : "dispute") + "!" :
                        "Unbanned because the evidence we have is lost/inconclusive. If you did exploit, please don't do it again :)";
                try {
                    this.closeAndUnban(event.getJDA(), clickedUser, reason, null);
                } catch (SQLException e) {
                    e.printStackTrace();
                }
                break;
            }
            case "unbancustom": {
                Modal modal = ActionModals.closeTicketAndUnbanWithCustomReason(this);
                event.replyModal(modal).queue();
                break;
            }
            case "retime": {
                Modal modal = ActionModals.closeTicketAndRetimeBan(this);
                event.replyModal(modal).queue();
                break;
            }
        }
    }

    /**
     * Calls {@link #close(JDA, User, String, Consumer)} but also unbans the user (depending on the ticket type) at the same time.
     *
     * @param jda          The API instance to use for Discord operations.
     * @param closedByUser The user that closed the ticket.
     * @param closeReason  The user-friendly description of why the ticket was closed.
     * @param onSuccess    If not null, the consumer to run when the ticket is fully, successfully closed.
     * @throws SQLException If something went wrong in the database.
     */
    public void closeAndUnban(JDA jda, User closedByUser, String closeReason, Consumer<JDA> onSuccess) throws SQLException {
        if (this.isForDiscord) {
            Guild abilityWars = jda.getGuildById(AbilityWarsBot.AW_GUILD_ID);
            if (abilityWars == null) {
                closeReason += "\n\nBot note: I wasn't able to unban you automagically, please reach out and have a moderator manually unban you.";
            } else {
                abilityWars.unban(UserSnowflake.fromId(this.discordIdToUnban)).queue();
            }
        } else {
            Long moderatorId = DiscordRobloxLinks.robloxIdFromDiscordId(closedByUser.getIdLong());
            if (moderatorId == null) {
                // couldn't fetch the user's Roblox account ID for some reason.
                // that, or the user somehow hacked and clicked the button without prior authentication...
                closeReason += "\n\nBot note: I wasn't able to unban you automagically, please reach out to a moderator and have them unban you!";
            } else {
                UnbanRequest unbanRequest = new UnbanRequest(PendingRequest.getNextRequestId(), this.robloxIdToUnban, moderatorId);
                PendingRequests.add(unbanRequest);
            }
        }

        this.close(jda, closedByUser, closeReason, onSuccess);
    }

    /**
     * Calls {@link #close(JDA, User, String, Consumer)} but also blacklists the user from appealing using that account (discord/roblox) in the future.
     *
     * @param jda             The API instance to use for Discord operations.
     * @param closedByUser    The user that closed the ticket.
     * @param closeReason     The user-friendly description of why the ticket was closed.
     * @param blacklistReason The reason to show for the blacklist.
     * @param onSuccess       If not null, the consumer to run when the ticket is fully, successfully closed.
     * @throws SQLException If something went wrong in the database.
     */
    public void closeAndBlacklist(JDA jda, User closedByUser, String closeReason, String blacklistReason, Consumer<JDA> onSuccess) throws SQLException {
        this.blacklist(closedByUser, blacklistReason);
        this.close(jda, closedByUser, closeReason, onSuccess);
    }

    /**
     * Blacklists a user from future appeals based on the current ticket type (Discord or in-game).
     * Performs either a Discord-based blacklist update or a Roblox user blacklist update.
     *
     * @param byUser The user initiating the blacklist operation.
     * @param reason The reason provided for blacklisting the user.
     * @throws SQLException If a database error occurs during the blacklist operation.
     */
    public void blacklist(User byUser, @Nullable String reason) throws SQLException {
        if (this.isForDiscord) {
            DiscordAppealBlacklist.push(this.discordIdToUnban, byUser.getIdLong(), reason, System.currentTimeMillis());
        } else {
            if (this.playerToUnban == null) {
                // load player
                this.playerToUnban = AWPlayer.loadFromDatabase(this.robloxIdToUnban, false, false, false, false);
                this.playerToUnban.ensureDefaultPlayer();
            }
            if (this.playerToUnban != null)
                this.playerToUnban.setBlacklist(reason, byUser);
        }
    }

    /**
     * Parses the provided input string to determine if it corresponds to the term "discord".
     * The method removes all non-alphabetic characters from the input string,
     * trims any leading or trailing whitespace, and performs a case-insensitive comparison with "discord".
     *
     * @param input The input string to be checked.
     * @return true if the processed input string matches "discord" (case-insensitive), otherwise false.
     */
    public boolean parseIsForDiscord(String input) {
        input = input.replaceAll("[^a-zA-Z]", "").strip().toLowerCase();
        return input.startsWith("d");
    }

    @Override
    public boolean loadFromModalResponse(ModalInteractionEvent event) throws SQLException {
        ModalMapping discordOrRobloxMapping = event.getValue("discord-or-roblox");
        ModalMapping userIdMapping = event.getValue("user-id");

        if (discordOrRobloxMapping == null || userIdMapping == null) {
            event.getHook().editOriginal("Something went wrong: discord sent us incomplete data??? Try again in a couple minutes maybe, or report to the developers if this continues happening.").queue();
            return false;
        }

        this.isForDiscord = this.parseIsForDiscord(discordOrRobloxMapping.getAsString());

        String userIdString = userIdMapping.getAsString();
        try {
            long userId = Long.parseLong(userIdString);
            if (this.isForDiscord)
                this.discordIdToUnban = userId;
            else {
                this.robloxIdToUnban = userId;
                this.robloxUserToUnban = RobloxAPI.getUserById(userId);

                if (this.robloxUserToUnban == null) {
                    event.getHook().editOriginal("Couldn't find any users on Roblox with the ID `" + userId + "`. Please try again.\n" +
                            "-# To get your Roblox ID, go to your profile on Roblox, and your ID is the set of numbers in the address. Example: `https://www.roblox.com/users/`**`3233825722`**`/profile`").queue();
                    return false;
                }
            }
        } catch (NumberFormatException ignored) {
            // the user is confused and/or doesn't know what an ID is; try to help them out by doing a string lookup.
            if (this.isForDiscord) {
                List<User> possibleDiscordUsers = event.getJDA().getUsersByName(userIdString, false);
                if (possibleDiscordUsers.isEmpty()) {
                    event.getHook().editOriginal("Your input for \"User ID\" was not valid. Please input your Discord ID.\n" +
                            "-# To get your Discord ID, right click your name and click \"Copy User ID\". If you don't see that option, go to `Settings` → `Advanced` → Turn on `Developer Mode`.").queue();
                    return false;
                }
                this.discordIdToUnban = possibleDiscordUsers.get(0).getIdLong();
            } else {
                RobloxAPI.User possibleRobloxUser = RobloxAPI.getUserByCurrentUsername(userIdString);
                if (possibleRobloxUser == null) {
                    event.getHook().editOriginal("Couldn't find any users on Roblox with the ID `" + userIdString.replace("`", "") + "`. Please try again.\n" +
                            "-# To get your Roblox ID, go to your profile on Roblox, and your ID is the set of numbers in the address. Example: `https://www.roblox.com/users/`**`3233825722`**`/profile`").queue();
                    return false;
                }
                this.robloxIdToUnban = possibleRobloxUser.userId();
                this.robloxUserToUnban = possibleRobloxUser;
            }
        }

        if (this.isForDiscord) {
            // make sure the user is banned from the Ability Wars server
            Guild abilityWars = event.getJDA().getGuildById(AbilityWarsBot.AW_GUILD_ID);
            if (abilityWars == null) {
                event.getHook().editOriginal("Something went wrong: The Ability Wars server is unavailable right now. Try again in a couple minutes.").queue();
                return false;
            }
            UserSnowflake discordToUnban = UserSnowflake.fromId(this.discordIdToUnban);
            this.discordBan = abilityWars.retrieveBan(discordToUnban)
                    .onErrorMap(throwable -> null)
                    .complete(); // evil synchronous statement
            if (this.discordBan == null) {
                event.getHook().editOriginal("The Discord user " + discordToUnban.getAsMention() + " is not currently banned from the Ability Wars Discord. You can rejoin using our invite link `discord.gg/abilitywars`.\n\n" +
                        "If you're still unable to join, please make sure to appeal for any alternate accounts you may be banned on.").queue();
                return false;
            }

            // check for blacklist
            DiscordAppealBlacklist appealBlacklist = DiscordAppealBlacklist.get(this.discordIdToUnban);
            if (appealBlacklist != null) {
                event.getHook().editOriginal("The Discord user " + discordToUnban.getAsMention() + " is blacklisted. This ticket cannot be opened.").queue();
                return false;
            }
        } else {
            if (this.robloxUserToUnban == null) {
                event.getHook().editOriginal("Couldn't find any users on Roblox with the ID `" + userIdString.replace("`", "") + "`. Please try again.\n" +
                        "-# To get your Roblox ID, go to your profile on Roblox, and your ID is the set of numbers in the address. Example: `https://www.roblox.com/users/`**`3233825722`**`/profile`").queue();
                return false;
            }
            // check for blacklist
            AWPlayer player = AWPlayer.loadFromDatabase(this.robloxIdToUnban,
                    true, true, true, false);
            if (player.isAppealBlacklisted()) {
                event.getHook().editOriginal("The Roblox user [%s](%s) is blacklisted. This ticket cannot be opened.".formatted(this.robloxUserToUnban.username(), this.robloxUserToUnban.getProfileURL())).queue();
                return false;
            }
        }
        return true;
    }
}
