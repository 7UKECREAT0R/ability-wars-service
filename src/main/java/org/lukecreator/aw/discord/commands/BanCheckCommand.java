package org.lukecreator.aw.discord.commands;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.requests.restaction.WebhookMessageEditAction;
import org.jetbrains.annotations.NotNull;
import org.lukecreator.aw.AWDatabase;
import org.lukecreator.aw.RobloxAPI;
import org.lukecreator.aw.data.AWBan;
import org.lukecreator.aw.data.AWEvidence;
import org.lukecreator.aw.data.AWPlayer;
import org.lukecreator.aw.data.Links;
import org.lukecreator.aw.discord.AbilityWarsBot;
import org.lukecreator.aw.discord.BotCommand;
import org.lukecreator.aw.discord.StaffRoles;
import org.lukecreator.aw.webserver.PendingRequest;
import org.lukecreator.aw.webserver.PendingRequests;
import org.lukecreator.aw.webserver.requests.InfoRequest;

import java.awt.*;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class BanCheckCommand extends BotCommand {
    public static final DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE;

    public BanCheckCommand() {
        super("aw-ban-status", "Checks if a user is banned from Ability Wars, along with additional information.");
    }

    /**
     * Generates a descriptive message summarizing the details of an array of bans
     * and appends it to the provided EmbedBuilder.
     *
     * @param eb                  The EmbedBuilder to which the generated ban record description will be appended.
     * @param bans                A non-null array of AWBan objects representing the ban records to describe.
     * @param includeLegacyBans   If bans using the legacy system should be included in the list.
     * @param highlightActiveBans If any bans (usually only one) should be marked if it's currently active.
     */
    public static void generateBanRecordDescription(EmbedBuilder eb, @NotNull AWBan[] bans, boolean includeLegacyBans, boolean highlightActiveBans) {
        final ZoneId UTC = ZoneId.of("UTC");
        final Instant currentInstant = Instant.now();

        for (int i = bans.length - 1; i >= 0; i--) {
            AWBan ban = bans[i];

            StringBuilder banEntry = new StringBuilder("- ");

            if (ban.isLegacy()) {
                if (!includeLegacyBans)
                    continue;
                banEntry.append("`⚠ old` `");
            } else
                banEntry.append("`");

            LocalDate startDate = Instant.ofEpochMilli(ban.starts())
                    .atZone(UTC)
                    .toLocalDate();
            banEntry.append(formatter.format(startDate));

            if (ban.ends() != null) {
                LocalDate endDate = Instant.ofEpochMilli(ban.ends())
                        .atZone(UTC)
                        .toLocalDate();
                boolean isExpired = Instant.ofEpochMilli(ban.ends()).isBefore(currentInstant);
                String tense = isExpired ? "ended" : "ends";
                banEntry.append("` - ")
                        .append(tense)
                        .append(" on `")
                        .append(formatter.format(endDate));
                if (!isExpired && highlightActiveBans)
                    banEntry.append("` `⇦ ACTIVE");
            }

            banEntry.append("`\n-# ")
                    .append(ban.displayReasonOrDefault());
            if (i > 0)
                banEntry.append("\n");

            eb.appendDescription(banEntry.toString());
        }
    }

    @Override
    public SlashCommandData constructCommand() {
        return Commands.slash(this.name, this.description)
                .addOption(OptionType.STRING, "target", "The Roblox username, ID, or Discord user to check.", true)
                .addOption(OptionType.BOOLEAN, "get-evidence", "(staff only) Include evidence URLs in the response.", false);
    }

    /**
     * Builds an embed message for this command.
     *
     * @param awPlayer        The Ability Wars database instance to show the information for.
     * @param robloxAccount   The Roblox account associated with {@code awPlayer}.
     * @param includeEvidence Include evidence URLs associated with each ban, if applicable. Only let staff members use this.
     * @return An embed message describing the player's bans.
     */
    private MessageEmbed buildFromPlayer(AWPlayer awPlayer, RobloxAPI.User robloxAccount, boolean includeEvidence) {
        boolean isBanned = awPlayer.bans.isCurrentlyBanned();
        EmbedBuilder eb = new EmbedBuilder()
                .setTitle("Bans for " + robloxAccount.username(), robloxAccount.getProfileURL())
                .setColor(isBanned ? Color.RED : Color.GREEN);

        if (isBanned) {
            AWBan ban = awPlayer.bans.currentBan();
            String banDurationString = ban.durationString();
            String remainingString = ban.remainingString();
            eb.appendDescription("Currently banned " + banDurationString + ". " + remainingString);
            eb.addField("Reason", ban.displayReasonOrDefault(), true);

            long started = ban.starts();
            long now = System.currentTimeMillis();

            LocalDate startedDateTime = Instant.ofEpochMilli(started)
                    .atZone(ZoneId.of("UTC")).toLocalDate();
            String timeAgoDescriptorString = getDurationDescriptor(started, now);

            String startedDate = formatter.format(startedDateTime) + " (" + timeAgoDescriptorString + ")";
            eb.addField("Banned", startedDate, true);

            if (includeEvidence) {
                AWEvidence[] evidence;
                try {
                    evidence = Links.BanEvidenceLinks.getEvidenceLinkedToBan(ban.userId(), ban.starts());
                } catch (SQLException e) {
                    evidence = new AWEvidence[0];
                }
                if (evidence.length == 0)
                    eb.addField("Evidence", "No evidence is internally linked to this ban. You'll have to do a manual search in Discord.", false);
                else {
                    StringBuilder evidenceString = new StringBuilder();
                    evidenceString.append("Found ").append(evidence.length).append(" evidence links:");
                    for (AWEvidence ev : evidence) {
                        String details = ev.details == null ? null : ev.details
                                .replaceAll("\n{2,}", "\n") // remove multiple newlines back-to-back
                                .replace("\n", "\n-# ");    // make each extra line smaller using Markdown

                        if (ev.hasTimestamp())
                            evidenceString.append("\n- (").append(ev.timestampString()).append(") ").append(ev.url);
                        else
                            evidenceString.append("\n- ").append(ev.url);

                        if (details != null && !details.isBlank())
                            evidenceString.append("\n-# ").append(details);
                    }
                    eb.addField("Evidence", evidenceString.toString(), false);
                }
            }

            if (awPlayer.bans.size() > 1) {
                AWBan[] bans = awPlayer.bans.getBans();
                eb.appendDescription("\n\nPast bans on record:\n");
                generateBanRecordDescription(eb, bans, true, false);
            }
        } else {
            if (awPlayer.bans.size() == 0)
                eb.appendDescription("Not currently banned - no bans on record!");
            else {

                AWBan[] bans = awPlayer.bans.getBans();
                eb.appendDescription("Not currently banned; past bans:\n");
                generateBanRecordDescription(eb, bans, true, false);
            }
        }

        if (includeEvidence)
            eb.setFooter("This message is hidden to conceal the evidence URLs. To send this publicly, don't include `get-evidence` in the command.");
        return eb.build();
    }

    @Override
    public void execute(SlashCommandInteractionEvent e) throws SQLException {
        var usernameOption = e.getOption("target");
        var getEvidenceOption = e.getOption("get-evidence");
        final boolean getEvidence = getEvidenceOption != null && getEvidenceOption.getAsBoolean();

        if (getEvidence) {
            if (!StaffRoles.isStaff(e.getMember())) {
                e.reply("To use `get-evidence` in the command, you must be a staff member.").setEphemeral(true).queue();
                return;
            }
        }

        if (usernameOption == null) {
            e.reply("You must specify a username to check.").setEphemeral(true).queue();
            return;
        }

        String username = usernameOption.getAsString();
        RobloxAPI.User user = RobloxAPI.getUserByInput(username, true);

        if (user == null) {
            e.reply(getUnknownUsernameDescriptor(username)).queue();
            return;
        }

        long robloxId = user.userId();

        // start "thinking" as the bot, since the request could take multiple seconds
        e.deferReply(getEvidence).queue();

        // make a request for user information
        PendingRequest request = new InfoRequest(PendingRequest.getNextRequestId(), robloxId)
                .onFulfilled(f -> {
                    AWPlayer player = AWDatabase.loadPlayer(robloxId, false, true, true, false);
                    MessageEmbed embed = this.buildFromPlayer(player, user, getEvidence);
                    WebhookMessageEditAction<Message> edit = e.getInteraction().getHook().editOriginalEmbeds(embed);
                    if (!getEvidence && !player.bans.isCurrentlyBanned())
                        edit.setActionRow(Button.secondary(AbilityWarsBot.BUTTON_ID_EXPLAIN_IP_BAN, "I still can't join"));
                    edit.queue();
                });
        PendingRequests.add(request);
    }
}