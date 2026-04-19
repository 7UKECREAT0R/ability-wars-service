package org.lukecreator.aw.discord.commands;

import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import org.lukecreator.aw.RobloxAPI;
import org.lukecreator.aw.data.AWPlayer;
import org.lukecreator.aw.data.AWTicket;
import org.lukecreator.aw.data.AWTicketsManager;
import org.lukecreator.aw.data.DiscordRobloxLinks;
import org.lukecreator.aw.data.tickets.AWPlayerReportTicket;
import org.lukecreator.aw.discord.BotCommand;
import org.lukecreator.aw.discord.StaffRoles;
import org.lukecreator.aw.webserver.PendingRequest;
import org.lukecreator.aw.webserver.PendingRequests;
import org.lukecreator.aw.webserver.requests.BanRequest;

import java.sql.SQLException;

public class TempbanCommand extends BotCommand {
    public TempbanCommand() {
        super("aw-tempban", "(staff only) Ban a user temporarily from Ability Wars.");
    }

    @Override
    public SlashCommandData constructCommand() {
        return Commands.slash(this.name, this.description)
                .addOption(OptionType.STRING, "target", "The Roblox username, ID, or Discord user to tempban.", true)
                .addOption(OptionType.INTEGER, "days", "The duration of the ban in days.", true)
                .addOption(OptionType.STRING, "reason", "The reason for the ban.", true);
    }

    @Override
    public void execute(SlashCommandInteractionEvent e) throws SQLException {
        var usernameOption = e.getOption("target");
        var daysOption = e.getOption("days");
        var reasonOption = e.getOption("reason");

        if (usernameOption == null || daysOption == null || reasonOption == null)
            return;

        String targetUsername = usernameOption.getAsString();
        String reason = reasonOption.getAsString();

        int days = daysOption.getAsInt();
        long durationMs = days * 24 * 60 * 60 * 1000L;

        if (StaffRoles.blockIfNotStaff(e))
            return;

        RobloxAPI.User targetUser = RobloxAPI.getUserByInput(targetUsername, true);
        Long responsibleModerator = DiscordRobloxLinks.robloxIdFromDiscordId(e.getUser().getIdLong());

        if (targetUser == null) {
            e.reply(getUnknownUsernameDescriptor(targetUsername)).queue();
            return;
        }
        if (responsibleModerator == null) {
            e.reply("Couldn't get your Roblox ID from your Discord ID. If you haven't already, get your accounts linked with an administrator.").setEphemeral(false).queue();
            return;
        }

        e.deferReply().queue();

        AWPlayer playerToBan = AWPlayer.loadFromDatabase(targetUser.userId(), false, true, true, false);
        if (playerToBan.bans.isCurrentlyBanned()) {
            e.getInteraction().getHook().editOriginal("The user [%s](%d) is already banned.".formatted(targetUser.username(), targetUser.userId())).queue();
            return;
        }

        AWPlayerReportTicket _ticket = null;
        if (e.getChannelType() == ChannelType.TEXT) {
            TextChannel channel = e.getChannel().asTextChannel();
            AWTicket __ticket = AWTicketsManager.getTicketFromCacheByDiscordChannel(channel);
            if (__ticket instanceof AWPlayerReportTicket t)
                _ticket = t;
        }
        final AWPlayerReportTicket ticket = _ticket;

        final Long ticketId = ticket == null ? null : ticket.id;
        final Long evidenceId = ticket == null ? null : ticket.getEvidenceId();

        // build the #in-game-punishments message
        final String[] evidenceURLs = ticket == null ? null : ticket.retrieveEvidenceURLs();
        final String report = evidenceURLs == null ? null : AWPlayerReportTicket.buildInGamePunishmentsRecord
                (e.getUser(), targetUser, reason, "Manually temp-banned for " + days + " days", evidenceURLs);

        PendingRequest request = new BanRequest(PendingRequest.getNextRequestId(), targetUser.userId(), responsibleModerator, reason, false, durationMs, evidenceId, ticketId)
                .onFulfilled(ignored -> {
                    String successMessage = report != null ?
                            "Successfully temp-banned user [%s](%s). Filing report in <#%d>.".formatted(targetUser.username(), targetUser.getProfileURL(), AWPlayerReportTicket.IN_GAME_PUNISHMENTS_CHANNEL) :
                            "Successfully temp-banned user [%s](%s). Unable to auto-file report.".formatted(targetUser.username(), targetUser.getProfileURL());
                    e.getInteraction().getHook().editOriginal(successMessage).queue();

                    if (report != null)
                        AWPlayerReportTicket.sendInGamePunishmentsMessage(e.getJDA(), report).queue();
                })
                .onNoPermission(() -> e.getInteraction().getHook().editOriginal("You don't have permission to ban users in-game.").queue());
        PendingRequests.add(request);
    }
}
